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
import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * Class which contains information about products, SKUs and purchases. This class can't be instantiated manually but only
 * through {@link Checkout#loadInventory()} method call.
 * Note that this class doesn't reflect real time billing information. It is not updated or notified if item was purchased
 * or cancelled.
 * This class lifecycle is bound to the lifecycle of {@link Checkout} in which it was created. If {@link Checkout}
 * stops this class loading also stops and no {@link Inventory.Listener#onLoaded(Inventory)} method is called.
 */
@Immutable
public final class Inventory {

	public static interface Listener {
		void onLoaded(@Nonnull Inventory inventory);
	}

	@Nonnull
	private final Checkout checkout;

	@Nonnull
	private final Map<String, Product> products = new HashMap<String, Product>();

	@Nonnull
	private final Listeners listeners = new Listeners();

	private int finished;

	Inventory(@Nonnull Checkout checkout) {
		this.checkout = checkout;
	}

	void load() {
		Check.isMainThread();
		finished = 0;
		checkout.whenReady(new Checkout.ListenerAdapter() {
			@Override
			public void onReady(@Nonnull BillingRequests requests, @Nonnull String productId, boolean billingSupported) {
				final Product product = new Product(productId, billingSupported);
				products.put(productId, product);
				onFinished();
				if (product.supported) {
					loadPurchases(requests, product);
					loadSkus(requests, product);
				} else {
					onFinished(2);
				}
			}
		});
	}

	@Nonnull
	public Collection<Product> getProducts() {
		Check.isTrue(isLoaded(), "Inventory is not loaded yet. Use Inventory#whenLoaded");
		return Collections.unmodifiableCollection(products.values());
	}

	@Nonnull
	public Product getProduct(@Nonnull String productId) {
		Check.isTrue(isLoaded(), "Inventory is not loaded yet. Use Inventory#whenLoaded");
		return products.get(productId);
	}

	private void onFinished() {
		onFinished(1);
	}

	private void onFinished(int count) {
		Check.isFalse(isLoaded(), "Inventory is already loaded");
		finished += count;
		if (isLoaded()) {
			listeners.onLoaded(this);
		}
	}

	private boolean isLoaded() {
		// for each product we wait for:
		// 1. onReady to be called
		// 2. loadPurchased to be finished
		// 3. loadSkus to be finished
		final int size = checkout.getProducts().size();
		final int toBeFinished = size * 3;
		return finished == toBeFinished;
	}

	public void whenLoaded(@Nonnull Listener listener) {
		Check.isMainThread();

		if (isLoaded()) {
			listener.onLoaded(this);
		} else {
			// still waiting
			listeners.add(listener);
		}
	}

	private void loadPurchases(@Nonnull final BillingRequests requests, @Nonnull final Product product) {
		requests.getAllPurchases(product.id, new ProductRequestListener<org.solovyev.android.checkout.Purchases>(product) {
			@Override
			public void onSuccess(@Nonnull org.solovyev.android.checkout.Purchases purchases) {
				if (isAlive()) {
					product.purchases.addAll(purchases.list);
				}
				onFinished();
			}
		});

	}

	private abstract class ProductRequestListener<R> implements RequestListener<R> {

		@Nonnull
		private final Product product;

		protected ProductRequestListener(@Nonnull Product product) {
			this.product = product;
		}

		boolean isAlive() {
			final Product p = products.get(product.id);
			return p == product;
		}

		@Override
		public final void onError(int response, @Nonnull Exception e) {
			onFinished();
		}
	}

	private void loadSkus(@Nonnull BillingRequests requests, @Nonnull final Product product) {
		final List<String> skuIds = checkout.getProducts().getSkuIds(product.id);
		if (!skuIds.isEmpty()) {
			requests.getSkus(product.id, skuIds, new ProductRequestListener<Skus>(product) {
				@Override
				public void onSuccess(@Nonnull Skus skus) {
					if (isAlive()) {
						product.skus.addAll(skus.list);
					}
					onFinished();
				}
			});
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

		public boolean isPurchased(@Nonnull String skuId) {
			for (Purchase purchase : purchases) {
				if (purchase.sku.equals(skuId)) {
					if (purchase.state == Purchase.State.PURCHASED) {
						return true;
					}
				}
			}

			return false;
		}

		@Nonnull
		public List<Purchases> getPurchasesBySku() {
			final List<Purchases> result = new ArrayList<Purchases>();
			for (Sku sku : skus) {
				final List<Purchase> list = new ArrayList<Purchase>();
				for (Purchase purchase : purchases) {
					if (purchase.sku.equals(sku.id)) {
						list.add(purchase);
					}
				}
				result.add(new Purchases(sku, list));
			}
			return result;
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


	/**
	 * List of purchases of SKU item
	 */
	public static final class Purchases implements Iterable<Purchase> {

		@Nonnull
		private final Sku sku;

		@Nonnull
		private final List<Purchase> list;

		Purchases(@Nonnull Sku sku, @Nonnull List<Purchase> list) {
			this.sku = sku;
			this.list = list;
		}

		@Nonnull
		public Sku getSku() {
			return sku;
		}

		@Nonnull
		public List<Purchase> asList() {
			return list;
		}

		public int size() {
			return list.size();
		}

		@Nonnull
		public Purchase get(int location) {
			return list.get(location);
		}

		@Override
		public Iterator<Purchase> iterator() {
			return list.iterator();
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

		public void clear() {
			list.clear();
		}

		@Override
		public void onLoaded(@Nonnull Inventory inventory) {
			for (Listener listener : list) {
				listener.onLoaded(inventory);
			}
			list.clear();
		}
	}
}
