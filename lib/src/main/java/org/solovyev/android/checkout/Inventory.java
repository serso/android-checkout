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

import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;

/**
 * Class which contains information about products, SKUs and purchases. This class can't be instantiated manually but only
 * through {@link Checkout#loadInventory()} method call.
 * Note that this class doesn't reflect real time billing information. It is not updated or notified if item was purchased
 * or cancelled.
 * This class lifecycle is bound to the lifecycle of {@link Checkout} in which it was created. If {@link Checkout}
 * stops this class loading also stops and no {@link Inventory.Listener#onLoaded(Inventory.Products)} method is called.
 */
public interface Inventory {

	@Nonnull
	Inventory load();

	void whenLoaded(@Nonnull Listener listener);

	/**
	 * Note that this method may return different instances of {@link Inventory.Products} with different contents.
	 * If you're reloading this inventory consider using {@link #whenLoaded(Inventory.Listener)} method.
	 *
	 * @return currently loaded set of products
	 */
	@Nonnull
	Inventory.Products getProducts();

	interface Listener {
		void onLoaded(@Nonnull Inventory.Products products);
	}

	/**
	 * Set of products in the inventory.
	 */
	@Immutable
	final class Products implements Iterable<Inventory.Product> {

		@Nonnull
		static final Products EMPTY = new Products();

		@Nonnull
		private final Map<String, Inventory.Product> map = new HashMap<String, Inventory.Product>();

		void add(@Nonnull Inventory.Product product) {
			map.put(product.id, product);
		}

		/**
		 * @param productId product id
		 * @return product by id
		 * @throws java.lang.RuntimeException if product doesn't exist
		 */
		@Nonnull
		public Inventory.Product get(@Nonnull String productId) {
			return map.get(productId);
		}

		/**
		 * @return unmodifiable iterator which iterates over all products
		 */
		@Override
		public Iterator<Inventory.Product> iterator() {
			return unmodifiableCollection(map.values()).iterator();
		}

		/**
		 * @return number of products
		 */
		public int size() {
			return map.size();
		}

		void merge(@Nonnull Products products) {
			for (Map.Entry<String, Product> entry : map.entrySet()) {
				if (!entry.getValue().supported) {
					final Product product = products.map.get(entry.getKey());
					if (product != null) {
						entry.setValue(product);
					}
				}
			}
		}
	}

	/**
	 * One product in the inventory. Contains list of purchases and optionally list of SKUs (if
	 * {@link org.solovyev.android.checkout.Products} contains information about SKUs)
	 */
	@Immutable
	final class Product {

		/**
		 * Product ID, see {@link org.solovyev.android.checkout.ProductTypes}
		 */
		@Nonnull
		public final String id;

		/**
		 * True if product is supported by {@link Inventory}. Note that Billing for this product might not be supported:
		 * this just indicates that {@link Inventory} loaded purchases/SKUs for the product.
		 */
		public final boolean supported;

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
			return Purchases.getPurchaseInState(purchases, sku, state);
		}

		@Nullable
		public Purchase getPurchaseInState(@Nonnull Sku sku, @Nonnull Purchase.State state) {
			return getPurchaseInState(sku.id, state);
		}

		/**
		 * This list doesn't contain duplicates, i.e. each element in the list has unique SKU
		 * @return unmodifiable list of purchases sorted by purchase date (latest first)
		 */
		@Nonnull
		public List<Purchase> getPurchases() {
			return unmodifiableList(purchases);
		}

		/**
		 * Note that this list might be empty if {@link org.solovyev.android.checkout.Inventory} doesn't contain
		 * information about SKUs
		 * @return unmodifiable list of SKUs
		 */
		@Nonnull
		public List<Sku> getSkus() {
			return unmodifiableList(skus);
		}

		void setSkus(@Nonnull List<Sku> skus) {
			Check.isTrue(this.skus.isEmpty(), "Must be called only once");
			this.skus.addAll(skus);
		}

		void setPurchases(@Nonnull List<Purchase> purchases) {
			Check.isTrue(this.purchases.isEmpty(), "Must be called only once");
			this.purchases.addAll(Purchases.neutralize(purchases));
			sort(this.purchases, PurchaseComparator.latestFirst());
		}
	}
}
