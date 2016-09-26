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

import javax.annotation.Nonnull;

/**
 * Inventory which falls back to fallback {@link Inventory} if one of the products is not
 * supported.
 */
class FallingBackInventory extends BaseInventory {

    private class MyTask extends Task {

        @Nonnull
        private final MainCallback mMainCallback = new MainCallback();
        @Nonnull
        private final FallbackCallback mFallbackCallback = new FallbackCallback();

        MyTask(Request request, Callback callback) {
            super(request, callback);
        }

        @Override
        public void run() {
            mMainCallback.load();
        }

        private class MainCallback implements Callback {

            @Override
            public void onLoaded(@Nonnull Products products) {
                synchronized (mLock) {
                    mProducts.merge(products);
                    if (!existsUnsupported()) {
                        onDone();
                        return;
                    }
                    mFallbackCallback.load();
                }
            }

            private boolean existsUnsupported() {
                Check.isTrue(Thread.holdsLock(mLock), "Must be synchronized");
                for (Product product : mProducts) {
                    if (!product.supported) {
                        return true;
                    }
                }

                return false;
            }

            public void load() {
                mMainInventory.load(mRequest, this);
            }
        }

        private class FallbackCallback implements Callback {

            @Override
            public void onLoaded(@Nonnull Products products) {
                synchronized (mLock) {
                    mProducts.merge(products);
                    onDone();
                }
            }

            public void load() {
                mFallbackInventory.load(mRequest, this);
            }
        }
    }

    @Nonnull
    private final CheckoutInventory mMainInventory;
    @Nonnull
    private final Inventory mFallbackInventory;

    public FallingBackInventory(@Nonnull Checkout checkout, @Nonnull Inventory fallbackInventory) {
        super(checkout);
        mMainInventory = new CheckoutInventory(checkout);
        mFallbackInventory = fallbackInventory;
    }

    @Override
    protected Task createTask(@Nonnull Request request, @Nonnull Callback callback) {
        return new MyTask(request, callback);
    }
}
