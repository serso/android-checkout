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

    @Override
    public boolean isLoading() {
        Check.isMainThread();
        synchronized (mLock) {
            return !mTasks.isEmpty();
        }
    }

    @Override
    public int load(@Nonnull Request request, @Nonnull Callback callback) {
        synchronized (mLock) {
            final Task task = new Task(request, callback);
            mTasks.add(task);
            task.run();
            return task.mId;
        }
    }

    protected final <R> RequestListener<R> synchronizedListener(@Nonnull final RequestListener<R> l) {
        return new SynchronizedRequestListener<>(l);
    }

    @Nonnull
    protected abstract Runnable createWorker(@Nonnull Task task);

    protected final class Task {

        private final int mId = mTaskIdGenerator.getAndIncrement();
        @Nonnull
        private final Request mRequest;
        @GuardedBy("mLock")
        @Nullable
        private Callback mCallback;
        @GuardedBy("mLock")
        private final Products mProducts = new Products();

        public Task(@Nonnull Request request, @Nonnull Callback callback) {
            mRequest = request.copy();
            mCallback = callback;
        }

        public boolean isCancelled() {
            synchronized (mLock) {
                return mCallback == null;
            }
        }

        private void cancel() {
            synchronized (mLock) {
                mCallback = null;
                mTasks.remove(this);
            }
        }

        public void run() {
            createWorker(this).run();
        }

        @Nonnull
        public Request getRequest() {
            return mRequest;
        }

        public void onDone(@Nonnull Products products) {
            synchronized (mLock) {
                mProducts.merge(products);
                onDone();
            }
        }

        public boolean onMaybeDone(@Nonnull Products products) {
            synchronized (mLock) {
                mProducts.merge(products);
                if (!existsUnsupported()) {
                    onDone();
                    return true;
                }
                return false;
            }
        }

        private void onDone() {
            Check.isTrue(Thread.holdsLock(mLock), "Must be synchronized");
            if (mCallback == null) {
                return;
            }
            mTasks.remove(this);
            mCallback.onLoaded(mProducts);
            mCallback = null;
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
    }

    private final class SynchronizedRequestListener<R> implements RequestListener<R> {
        private final RequestListener<R> mListener;

        public SynchronizedRequestListener(RequestListener<R> listener) {
            mListener = listener;
        }

        @Override
        public void onSuccess(@Nonnull R result) {
            synchronized (mLock) {
                mListener.onSuccess(result);
            }
        }

        @Override
        public void onError(int response, @Nonnull Exception e) {
            synchronized (mLock) {
                mListener.onError(response, e);
            }
        }
    }
}
