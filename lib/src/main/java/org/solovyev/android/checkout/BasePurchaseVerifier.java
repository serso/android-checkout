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

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

/**
 * Common base class of common implementation for {@link PurchaseVerifier} that verifies purchases
 * on a background thread.
 */
public abstract class BasePurchaseVerifier implements PurchaseVerifier {

    @Nonnull
    private final Executor mBackground;
    @Nonnull
    private final MainThread mMainThread;

    protected BasePurchaseVerifier() {
        this(new Handler(Looper.getMainLooper()));
    }

    protected BasePurchaseVerifier(@Nonnull Handler handler) {
        this(handler, 2, defaultThreadFactory());
    }

    protected BasePurchaseVerifier(@Nonnull Handler handler, int threadCount, @Nonnull ThreadFactory threadFactory) {
        mMainThread = new MainThread(handler);
        mBackground = Executors.newFixedThreadPool(threadCount, threadFactory);
    }

    @Nonnull
    private static ThreadFactory defaultThreadFactory() {
        return new ThreadFactory() {
            @Nonnull
            private final AtomicInteger mCount = new AtomicInteger();

            @Override
            public Thread newThread(@Nonnull Runnable r) {
                return new Thread(r, "PurchaseVerifierThread #" + mCount.getAndIncrement());
            }
        };
    }

    @Override
    public final void verify(@Nonnull final List<Purchase> purchases, @Nonnull final RequestListener<List<Purchase>> listener) {
        if (MainThread.isMainThread()) {
            mBackground.execute(new Runnable() {
                @Override
                public void run() {
                    doVerify(purchases, new MainThreadRequestListener(listener));
                }
            });
        } else {
            doVerify(purchases, listener);
        }
    }

    /**
     * Implementation should verify a list of <var>purchases</var> and deliver the result to the
     * passed <var>listener</var>. Note that at the end of the execution one and only one method of
     * <var>listener</var> must be called. Forgetting calling methods of <var>listener</var> causes
     * a memory leak as there is no other indication of that the work is done. Listener's methods
     * must be called on the same thread on which this method is called.
     *
     * @param purchases purchases to be verified
     * @param listener  callback listener
     */
    protected abstract void doVerify(@Nonnull List<Purchase> purchases, @Nonnull RequestListener<List<Purchase>> listener);

    private final class MainThreadRequestListener implements RequestListener<List<Purchase>> {

        @Nonnull
        private final RequestListener<List<Purchase>> mListener;

        private MainThreadRequestListener(@Nonnull RequestListener<List<Purchase>> listener) {
            mListener = listener;
        }

        @Override
        public void onSuccess(@Nonnull final List<Purchase> result) {
            mMainThread.execute(new Runnable() {
                @Override
                public void run() {
                    mListener.onSuccess(result);
                }
            });
        }

        @Override
        public void onError(final int response, @Nonnull final Exception e) {
            mMainThread.execute(new Runnable() {
                @Override
                public void run() {
                    mListener.onError(response, e);
                }
            });
        }
    }
}
