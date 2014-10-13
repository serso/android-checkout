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

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link org.solovyev.android.checkout.PurchaseVerifier} which verifies the purchases on the background thread.
 */
public abstract class BasePurchaseVerifier implements PurchaseVerifier {

	@Nonnull
	private final Executor background = Executors.newFixedThreadPool(2, new ThreadFactory() {

		@Nonnull
		private final AtomicInteger counter = new AtomicInteger();

		@Override
		public Thread newThread(@Nonnull Runnable r) {
			return new Thread(r, "PurchaseVerifierThread #" + counter.getAndIncrement());
		}
	});

	@Nonnull
	private final MainThread mainThread;

	protected BasePurchaseVerifier(@Nonnull Handler handler) {
		mainThread = new MainThread(handler);
	}

	protected BasePurchaseVerifier() {
		mainThread = new MainThread(new Handler());
	}

	@Override
	public final void verify(@Nonnull final List<Purchase> purchases, @Nonnull final RequestListener<List<Purchase>> listener) {
		final boolean mainThread = MainThread.isMainThread();
		if (mainThread) {
			background.execute(new Runnable() {
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
	 * Method verifies a list of <var>purchases</var> on background thread and delivers result by calling <var>listener</var>
	 * methods. Note that at the end of execution one and only one method of <var>listener</var> must be called. Not
	 * calling <var>listener</var> method might cause dangling request listeners. Listener's methods must be called on
	 * the thread on which current method was called.
	 *
	 * @param purchases purchases to be verified
	 * @param listener  callback listener
	 */
	protected abstract void doVerify(@Nonnull List<Purchase> purchases, @Nonnull RequestListener<List<Purchase>> listener);

	private final class MainThreadRequestListener implements RequestListener<List<Purchase>> {

		@Nonnull
		private final RequestListener<List<Purchase>> listener;

		private MainThreadRequestListener(@Nonnull RequestListener<List<Purchase>> listener) {
			this.listener = listener;
		}

		@Override
		public void onSuccess(@Nonnull final List<Purchase> result) {
			mainThread.execute(new Runnable() {
				@Override
				public void run() {
					listener.onSuccess(result);
				}
			});
		}

		@Override
		public void onError(final int response, @Nonnull final Exception e) {
			mainThread.execute(new Runnable() {
				@Override
				public void run() {
					listener.onError(response, e);
				}
			});
		}
	}
}
