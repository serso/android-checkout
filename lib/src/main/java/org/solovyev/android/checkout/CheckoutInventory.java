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
import javax.annotation.concurrent.GuardedBy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default Billing v.3 {@link Inventory}. Loads its contents from the {@link Checkout}.
 */
final class CheckoutInventory extends BaseInventory {

	@GuardedBy("lock")
	@Nonnull
	private final Counter counter = new Counter();

	CheckoutInventory(@Nonnull Checkout checkout) {
		super(checkout);
	}

	@Override
	@Nonnull
	public final Inventory load() {
		Check.isMainThread();

		synchronized (lock) {
			// for each product we wait for:
			// 1. onReady to be called
			// 2. loadPurchased to be finished
			// 3. loadSkus to be finished
			final int size = checkout.getProducts().size();
			final long id = counter.newAttempt(size * 3);

			// clear all previously loaded data
			products = new Products();

			checkout.whenReady(new CheckoutListener(id));
		}

		return this;
	}

	protected final boolean onFinished(long id) {
		return onFinished(1, id);
	}

	protected final boolean onFinished(int count, long id) {
		synchronized (lock) {
			final boolean stillLoading = counter.countDown(count, id);
			if (stillLoading && isLoaded()) {
				listeners.onLoaded(products);
			}
			return stillLoading;
		}
	}

	@Override
	boolean isLoaded() {
		synchronized (lock) {
			return counter.isFinished();
		}
	}

	private void loadPurchases(@Nonnull final BillingRequests requests, @Nonnull Product product, long id) {
		requests.getAllPurchases(product.id, new ProductRequestListener<Purchases>(product, id) {
			@Override
			public void onSuccess(@Nonnull Purchases purchases) {
				synchronized (lock) {
					if (isAlive()) {
						product.setPurchases(purchases.list);
					}
					onFinished(id);
				}
			}
		});
	}

	private abstract class ProductRequestListener<R> implements RequestListener<R> {

		@Nonnull
		protected final Product product;
		protected final long id;

		protected ProductRequestListener(@Nonnull Product product, long id) {
			this.product = product;
			this.id = id;
		}

		boolean isAlive() {
			Check.isTrue(Thread.holdsLock(lock), "Should be called from synchronized block");
			final Product p = products.get(product.id);
			return p == product;
		}

		@Override
		public final void onError(int response, @Nonnull Exception e) {
			onFinished(id);
		}
	}

	private void loadSkus(@Nonnull BillingRequests requests, @Nonnull final Product product, long id) {
		final List<String> skuIds = checkout.getProducts().getSkuIds(product.id);
		if (!skuIds.isEmpty()) {
			requests.getSkus(product.id, skuIds, new ProductRequestListener<Skus>(product, id) {
				@Override
				public void onSuccess(@Nonnull Skus skus) {
					synchronized (lock) {
						if (isAlive()) {
							product.setSkus(skus.list);
						}
						onFinished(id);
					}
				}
			});
		}
	}

	private class CheckoutListener extends Checkout.ListenerAdapter {
		private final long id;

		public CheckoutListener(long id) {
			this.id = id;
		}

		@Override
		public void onReady(@Nonnull BillingRequests requests, @Nonnull String productId, boolean billingSupported) {
			final Product product = new Product(productId, billingSupported);
			synchronized (lock) {
				final boolean stillLoading = onFinished(id);
				if (stillLoading) {
					products.add(product);
					if (product.supported) {
						loadPurchases(requests, product, id);
						loadSkus(requests, product, id);
					} else {
						onFinished(2, id);
					}
				}
			}
		}
	}

	private static final class Counter {
		@Nonnull
		private final AtomicInteger counter = new AtomicInteger(-1);

		@Nonnull
		private final AtomicLong id = new AtomicLong();

		public long newAttempt(int counter) {
			final long id = this.id.incrementAndGet();
			this.counter.set(counter);
			return id;
		}

		/**
		 * @param counts number of count downs
		 * @param id loading ID
		 * @return true if current loading id is the same same as <var>id</var>
		 */
		public boolean countDown(int counts, long id) {
			if (this.id.get() != id) {
				// new loading was requested => have to stop now
				return false;
			}
			Check.isFalse(isFinished(), "Inventory is already loaded. Loading id: " + id);
			counter.addAndGet(-counts);
			return true;
		}

		public boolean isFinished() {
			return counter.get() == 0L;
		}
	}
}
