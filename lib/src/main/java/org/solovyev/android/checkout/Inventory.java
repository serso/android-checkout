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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;

/**
 * Class that can load information about products, SKUs and purchases. This class can't be
 * instantiated directly but only through {@link Checkout#loadInventory(Request, Callback)} or
 * {@link Checkout#makeInventory()} method calls.
 * This class lifecycle is bound to the lifecycle of {@link Checkout} in which it was created. If
 * {@link Checkout} stops this class loading also stops and no
 * {@link Callback#onLoaded(Inventory.Products)} method is called.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public interface Inventory {

    /**
     * Loads {@link Products} and asynchronously delivers it to the provided {@link Callback}. Data
     * to be loaded is defined by {@link Request} argument. For each load request a task is created
     * whose identifier is returned in this method. The task can be later cancelled via
     * {@link #cancel(int)} method.
     * @param request request definition
     * @return task identifier
     */
    int load(@Nonnull Request request, @Nonnull Callback callback);

    /**
     * Cancels all load tasks, if any.
     */
    void cancel();

    /**
     * Cancels a task by id. Id can be obtained from {@link #load(Request, Callback)} method.
     * @param id task id
     */
    void cancel(int id);

    /**
     * @return true if there is at least one task that is still running, false otherwise
     */
    boolean isLoading();

    /**
     * A callback of {@link #load(Request, Callback)} method.
     */
    interface Callback {
        /**
         * Called when all the products were loaded. Note that this method is called even if the
         * loading fails.
         * @param products loaded products
         */
        void onLoaded(@Nonnull Inventory.Products products);
    }

    /**
     * Set of products in the inventory.
     */
    @Immutable
    final class Products implements Iterable<Inventory.Product> {

        @Nonnull
        static final Products sEmpty = new Products();

        @Nonnull
        private final Map<String, Inventory.Product> mMap = new HashMap<>();

        Products() {
            for (String product : ProductTypes.ALL) {
                mMap.put(product, new Product(product, false));
            }
        }

        @Nonnull
        public static Products empty() {
            return sEmpty;
        }

        void add(@Nonnull Inventory.Product product) {
            mMap.put(product.id, product);
        }

        /**
         * @param productId product id
         * @return product by id
         */
        @Nonnull
        public Inventory.Product get(@Nonnull String productId) {
            ProductTypes.checkSupported(productId);
            return mMap.get(productId);
        }

        /**
         * @return unmodifiable iterator which iterates over all products
         */
        @Override
        public Iterator<Inventory.Product> iterator() {
            return unmodifiableCollection(mMap.values()).iterator();
        }

        /**
         * @return number of products
         */
        public int size() {
            return mMap.size();
        }

        void merge(@Nonnull Products products) {
            for (Map.Entry<String, Product> entry : mMap.entrySet()) {
                if (!entry.getValue().supported) {
                    final Product product = products.mMap.get(entry.getKey());
                    if (product != null) {
                        entry.setValue(product);
                    }
                }
            }
        }
    }

    /**
     * One product in the inventory. Contains list of purchases and optionally list of SKUs (if
     * {@link Request} contains information about SKUs)
     */
    @Immutable
    final class Product {

        /**
         * Product ID, see {@link org.solovyev.android.checkout.ProductTypes}
         */
        @Nonnull
        public final String id;

        /**
         * True if product is supported by {@link Inventory}. Note that Billing for this product
         * might not be supported:
         * this just indicates that {@link Inventory} loaded purchases/SKUs for the product.
         */
        public final boolean supported;

        /**
         * This list is loaded only if {@link Request#loadPurchases(String)} was called for this
         * product.
         */
        @Nonnull
        final List<Purchase> mPurchases = new ArrayList<>();

        /**
         * This list is loaded only if {@link Request#loadSkus(String, List)} was called for this
         * product and contains only SKUs listed in the original request.
         */
        @Nonnull
        final List<Sku> mSkus = new ArrayList<>();

        Product(@Nonnull String id, boolean supported) {
            ProductTypes.checkSupported(id);
            this.id = id;
            this.supported = supported;
        }

        public boolean isPurchased(@Nonnull Sku sku) {
            return isPurchased(sku.id.code);
        }

        public boolean isPurchased(@Nonnull String sku) {
            return hasPurchaseInState(sku, Purchase.State.PURCHASED);
        }

        public boolean hasPurchaseInState(@Nonnull String sku, @Nonnull Purchase.State state) {
            return getPurchaseInState(sku, state) != null;
        }

        @Nullable
        public Purchase getPurchaseInState(@Nonnull String sku, @Nonnull Purchase.State state) {
            return Purchases.getPurchaseInState(mPurchases, sku, state);
        }

        @Nullable
        public Purchase getPurchaseInState(@Nonnull Sku sku, @Nonnull Purchase.State state) {
            return getPurchaseInState(sku.id.code, state);
        }

        /**
         * This list doesn't contain duplicates, i.e. each element in the list has unique SKU
         * @return unmodifiable list of purchases sorted by purchase date (latest first)
         */
        @Nonnull
        public List<Purchase> getPurchases() {
            return unmodifiableList(mPurchases);
        }

        void setPurchases(@Nonnull List<Purchase> purchases) {
            Check.isTrue(mPurchases.isEmpty(), "Must be called only once");
            mPurchases.addAll(Purchases.neutralize(purchases));
            sort(mPurchases, PurchaseComparator.latestFirst());
        }

        /**
         * @return unmodifiable list of SKUs in the product
         */
        @Nonnull
        public List<Sku> getSkus() {
            return unmodifiableList(mSkus);
        }

        void setSkus(@Nonnull List<Sku> skus) {
            Check.isTrue(mSkus.isEmpty(), "Must be called only once");
            mSkus.addAll(skus);
        }

        @Nullable
        public Sku getSku(@Nonnull String sku) {
            for (Sku s : mSkus) {
                if (s.id.code.equals(sku)) {
                    return s;
                }
            }
            return null;
        }
    }

    /**
     * This class defines what data should be loaded into the {@link Products} after
     * {@link #load(Request, Callback)} finishes.
     */
    final class Request {
        // list of SKUs for which the details are loaded
        private final Map<String, List<String>> mSkus = new HashMap<>();
        // set of products for which purchase information is loaded
        private final Set<String> mProducts = new HashSet<>();

        private Request() {
            for (String product : ProductTypes.ALL) {
                mSkus.put(product, new ArrayList<String>(5));
            }
        }

        @Nonnull
        Request copy() {
            final Request copy = new Request();
            copy.mSkus.putAll(mSkus);
            copy.mProducts.addAll(mProducts);
            return copy;
        }

        /**
         * Creates an empty load request. Only {@link Product#supported} flag is set in
         * {@link #load(Request, Callback)} for all available for billing products.
         * @return empty request
         * @see ProductTypes
         */
        @Nonnull
        public static Request create() {
            return new Request();
        }

        /**
         * Makes {@link Inventory} to load purchases for all available for billing products,
         * see {@link ProductTypes#ALL}.
         * @return this request
         * @see BillingRequests#getAllPurchases(String, RequestListener)
         */
        @Nonnull
        public Request loadAllPurchases() {
            mProducts.addAll(ProductTypes.ALL);
            return this;
        }

        /**
         * Makes {@link Inventory} to load all purchases for the given <var>product</var>.
         * @param product product
         * @return this request
         * @see BillingRequests#getAllPurchases(String, RequestListener)
         */
        @Nonnull
        public Request loadPurchases(@Nonnull String product) {
            ProductTypes.checkSupported(product);
            mProducts.add(product);
            return this;
        }

        boolean shouldLoadPurchases(@Nonnull String product) {
            return mProducts.contains(product);
        }

        /**
         * Same as {@link #loadSkus(String, List)}.
         */
        @Nonnull
        public Request loadSkus(@Nonnull String product, @Nonnull String... skus) {
            Check.isTrue(skus.length > 0, "No SKUs listed, can't load them");
            return loadSkus(product, Arrays.asList(skus));
        }

        /**
         * Makes {@link Inventory} to load SKU details for the given list of <var>skus</var>. As
         * SKU identifier is unique only in product <var>product</var> id must be also provided to
         * this method.
         * @param product product
         * @param skus    list of SKU identifiers for which SKU details should be loaded
         * @return this request
         */
        @Nonnull
        public Request loadSkus(@Nonnull String product, @Nonnull List<String> skus) {
            for (String sku : skus) {
                loadSkus(product, sku);
            }
            return this;
        }

        /**
         * Same as {@link #loadSkus(String, List)} with one element in the list.
         */
        @Nonnull
        public Request loadSkus(@Nonnull String product, @Nonnull String sku) {
            ProductTypes.checkSupported(product);
            Check.isNotEmpty(sku);

            final List<String> list = mSkus.get(product);
            Check.isTrue(!list.contains(sku), "Adding same SKU is not allowed");
            list.add(sku);
            return this;
        }

        boolean shouldLoadSkus(@Nonnull String product) {
            ProductTypes.checkSupported(product);
            return !mSkus.get(product).isEmpty();
        }

        @Nonnull
        List<String> getSkus(@Nonnull String product) {
            return mSkus.get(product);
        }
    }
}
