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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * Base class for all {@link Inventory} implementations. Contains the last loaded
 * {@link Inventory.Products}.
 */
public abstract class BaseInventory implements Inventory {

    @Nonnull
    protected final Object mLock;
    @Nonnull
    protected final Checkout mCheckout;
    @GuardedBy("mLock")
    @Nonnull
    private Products mLastProducts = Products.EMPTY;
    @GuardedBy("mLock")
    @Nonnull
    private final List<Task> mTasks = new ArrayList<>();
    private final AtomicInteger mTaskIdGenerator = new AtomicInteger();

    protected BaseInventory(@Nonnull Checkout checkout) {
        mCheckout = checkout;
        mLock = checkout.mLock;
    }

    @Override
    public void cancel() {
        for (Task task : getTasksCopy()) {
            task.cancel();
        }
    }

    @Override
    public void cancel(int id) {
        synchronized (mLock) {
            for (Task task : mTasks) {
                if (task.mId == id) {
                    task.cancel();
                    break;
                }
            }
        }
    }

    @Nonnull
    private List<Task> getTasksCopy() {
        synchronized (mLock) {
            return new ArrayList<>(mTasks);
        }
    }

    @Nonnull
    public final Products getLastLoadedProducts() {
        Check.isMainThread();
        synchronized (mLock) {
            if (!hasLastLoadedProducts()) {
                Billing.warning(
                        "Inventory is not loaded yet. Use Inventory#load(Request, Callback) instead");
            }
            return mLastProducts;
        }
    }

    @Override
    public boolean hasLastLoadedProducts() {
        Check.isMainThread();
        synchronized (mLock) {
            return mLastProducts != Products.EMPTY;
        }
    }

    @Override
    public int load(@Nonnull Request request, @Nonnull Callback callback) {
        synchronized (mLock) {
            final Task task = createTask(request, callback);
            mTasks.add(task);
            task.run();
            return task.mId;
        }
    }

    protected abstract Task createTask(@Nonnull Request request, @Nonnull Callback callback);

    protected abstract class Task implements Runnable {

        protected final int mId = mTaskIdGenerator.getAndIncrement();
        @Nonnull
        protected final Request mRequest;
        @GuardedBy("mLock")
        @Nullable
        private Callback mCallback;
        @GuardedBy("mLock")
        protected final Products mProducts = new Products();

        public Task(@Nonnull Request request, @Nonnull Callback callback) {
            mRequest = request.copy();
            mCallback = callback;
        }

        protected final void onDone() {
            synchronized (mLock) {
                mLastProducts = mProducts;
                if (mCallback == null) {
                    return;
                }
                mTasks.remove(this);
                mCallback.onLoaded(mProducts);
                mCallback = null;
            }
        }

        final boolean isCancelled() {
            synchronized (mLock) {
                return mCallback == null;
            }
        }

        final void cancel() {
            synchronized (mLock) {
                mCallback = null;
                mTasks.remove(this);
            }
        }
    }
}
