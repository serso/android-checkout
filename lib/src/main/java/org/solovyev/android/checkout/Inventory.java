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
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class which contains information about products, SKUs and purchases. This class can't be instantiated manually but only
 * through {@link Checkout#loadInventory()} method call.
 * Note that this class doesn't reflect real time billing information. It is not updated or notified if item was purchased
 * or cancelled.
 * This class lifecycle is bound to the lifecycle of {@link Checkout} in which it was created. If {@link Checkout}
 * stops this class loading also stops and no {@link Inventory.Listener#onLoaded(Inventory.Products)} method is called.
 */
public final class Inventory {

	public static interface Listener {
		void onLoaded(@Nonnull Products products);
	}

	@Nonnull
	private final Checkout checkout;

	@Nonnull
	private Products products = new Products();

	@Nonnull
	private final Listeners listeners = new Listeners();

	@Nonnull
	private final AtomicInteger toBeFinished = new AtomicInteger();

	@Nonnull
	private final AtomicLong loadingId = new AtomicLong();

	Inventory(@Nonnull Checkout checkout) {
		this.checkout = checkout;
	}

	@Nonnull
	public Inventory load() {
		Check.isMainThread();
		// for each product we wait for:
		// 1. onReady to be called
		// 2. loadPurchased to be finished
		// 3. loadSkus to be finished
		final int size = checkout.getProducts().size();
		toBeFinished.set(size * 3);
		final long id = loadingId.incrementAndGet();

		// clear all previously loaded data
		products = new Products();

		checkout.whenReady(new Checkout.ListenerAdapter() {
			@Override
			public void onReady(@Nonnull BillingRequests requests, @Nonnull String productId, boolean billingSupported) {
				final Product product = new Product(productId, billingSupported);
				products.add(product);
				onFinished(id);
				if (product.supported) {
					loadPurchases(requests, product, id);
					loadSkus(requests, product, id);
				} else {
					onFinished(2, id);
				}
			}
		});
		return this;
	}

	/**
	 * Note that this method may return different instances of {@link Inventory.Products} with different contents.
	 * If you're reloading this inventory consider using {@link #whenLoaded(Inventory.Listener)} method.
	 * @return currently loaded set of products
	 */
	@Nonnull
	public Products getProducts() {
		Check.isTrue(isLoaded(), "Inventory is not loaded yet. Use Inventory#whenLoaded");
		return products;
	}

	private void onFinished(long id) {
		onFinished(1, id);
	}

	private void onFinished(int count, long id) {
		if (loadingId.get() != id) {
			// new loading was requested => have to stop now
			return;
		}
		Check.isFalse(isLoaded(), "Inventory is already loaded");
		toBeFinished.addAndGet(-count);
		if (isLoaded()) {
			listeners.onLoaded(products);
		}
	}

	private boolean isLoaded() {
		return toBeFinished.get() == 0;
	}

	public void whenLoaded(@Nonnull Listener listener) {
		Check.isMainThread();

		if (isLoaded()) {
			listener.onLoaded(products);
		} else {
			// still waiting
			listeners.add(listener);
		}
	}

	private void loadPurchases(@Nonnull final BillingRequests requests, @Nonnull Product product, long id) {
		requests.getAllPurchases(product.id, new ProductRequestListener<org.solovyev.android.checkout.Purchases>(product, id) {
			@Override
			public void onSuccess(@Nonnull org.solovyev.android.checkout.Purchases purchases) {
				if (isAlive()) {
					product.purchases.addAll(purchases.list);
				}
				onFinished(id);
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
					if (isAlive()) {
						product.skus.addAll(skus.list);
					}
					onFinished(id);
				}
			});
		}
	}

	/**
	 * Set of products in the inventory.
	 */
	@Immutable
	public static final class Products implements Iterable<Product> {
		final Map<String, Product> map = new HashMap<String, Product>();

		void add(@Nonnull Product product) {
			map.put(product.id, product);
		}

		@Nonnull
		public Product get(@Nonnull String productId) {
			return map.get(productId);
		}

		@Nonnull
		public Collection<Product> asList() {
			return Collections.unmodifiableCollection(map.values());
		}

		@Override
		public Iterator<Product> iterator() {
			return map.values().iterator();
		}
	}

	/**
	 * One product in the inventory. Contains list of purchases and optionally list of SKUs (if {@link Products})
	 * contains such information
	 */
	@Immutable
	public static final class Product {

		@Nonnull
		final String id;
		final boolean supported;

		@Nonnull
		final List<Purchase> purchases = new ArrayList<Purchase>();

		@Nonnull
		final List<Sku> skus = new ArrayList<Sku>();

		Product(@Nonnull String id, boolean supported) {
			this.id = id;
			this.supported = supported;
		}

		public boolean isPurchased(@Nonnull Sku sku) {
			return isPurchased(sku.id);
		}

		public boolean isPurchased(@Nonnull String sku) {
			return hasPurchaseInState(sku, Purchase.State.PURCHASED);
		}

		public boolean hasPurchaseInState(@Nonnull String sku, @Nonnull Purchase.State state) {
			return getPurchaseInState(sku, state) != null;
		}

		@Nullable
		public Purchase getPurchaseInState(@Nonnull String sku, @Nonnull Purchase.State state) {
			return org.solovyev.android.checkout.Purchases.getPurchaseInState(purchases, sku, state);
		}

		@Nullable
		public Purchase getPurchaseInState(@Nonnull Sku sku, @Nonnull Purchase.State state) {
			return getPurchaseInState(sku.id, state);
		}

		@Nonnull
		public String getId() {
			return id;
		}

		public boolean isSupported() {
			return supported;
		}

		@Nonnull
		public List<Purchase> getPurchases() {
			return Collections.unmodifiableList(purchases);
		}

		@Nonnull
		public List<Sku> getSkus() {
			return Collections.unmodifiableList(skus);
		}
	}

	private static final class Listeners implements Listener {
		@Nonnull
		private final List<Listener> list = new ArrayList<Listener>();

		public void add(@Nonnull Listener l) {
			if (!list.contains(l)) {
				list.add(l);
			}
		}

		@Override
		public void onLoaded(@Nonnull Products products) {
			final List<Listener> localList = new ArrayList<Listener>(list);
			list.clear();
			for (Listener listener : localList) {
				listener.onLoaded(products);
			}
		}
	}
}
