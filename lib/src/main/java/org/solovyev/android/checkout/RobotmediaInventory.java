/*
 * Copyright 2014 serso aka se.solovyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Contact details
 *
 * Email: se.solovyev@gmail.com
 * Site:  http://se.solovyev.org
 */

package org.solovyev.android.checkout;

import android.content.Context;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

public final class RobotmediaInventory extends BaseInventory {

    @Nonnull
    private final Executor mBackground = Executors.newSingleThreadExecutor();
    @Nonnull
    private final Executor mOnLoadExecutor;
    @GuardedBy("mLock")
    @Nonnull
    private State mState = State.INITIAL;

    public RobotmediaInventory(@Nonnull Checkout checkout, @Nonnull Executor onLoadExecutor) {
        super(checkout);
        mOnLoadExecutor = onLoadExecutor;
    }

    @Nonnull
    @Override
    public Inventory load(@Nonnull Request request, @Nonnull Callback callback) {
        synchronized (mLock) {
            setRequest(request, callback);
            mState = State.LOADING;
            if (RobotmediaDatabase.exists(mCheckout.getContext())) {
                mBackground.execute(new Loader(request));
            } else {
                onLoaded(RobotmediaDatabase.toInventoryProducts(ProductTypes.ALL));
            }
        }

        return this;
    }

    private void onLoaded(@Nonnull final Inventory.Products products) {
        synchronized (mLock) {
            if (mState == State.LOADED) {
                return;
            }
            mState = State.LOADED;
            mProducts = products;
        }
        mOnLoadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (mLock) {
                    if (mState != State.LOADED) {
                        return;
                    }
                    onLoaded();
                }
            }
        });
    }

    boolean isLoaded() {
        synchronized (mLock) {
            return mState == State.LOADED;
        }
    }

    private enum State {
        INITIAL,
        LOADING,
        LOADED
    }

    private class Loader implements Runnable {
        private final Request mSkus;

        public Loader(Request skus) {
            this.mSkus = skus;
        }

        @Override
        public void run() {
            final Context context = mCheckout.getContext();
            final RobotmediaDatabase database = new RobotmediaDatabase(context);
            final Products products = database.load(mSkus);
            synchronized (mLock) {
                if (!getRequest().equals(mSkus)) {
                    return;
                }
            }
            onLoaded(products);
        }
    }
}
