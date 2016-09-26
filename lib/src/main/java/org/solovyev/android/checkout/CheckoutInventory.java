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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * Default Billing v.3 {@link Inventory}. Loads the purchase information from the {@link Checkout}.
 */
final class CheckoutInventory extends BaseInventory {

    @GuardedBy("mLock")
    @Nonnull
    private final Counter mCounter = new Counter();

    CheckoutInventory(@Nonnull Checkout checkout) {
        super(checkout);
    }

    @Nonnull
    @Override
    public Inventory load(@Nonnull Request request, @Nonnull Callback callback) {
        Check.isMainThread();

        synchronized (mLock) {
            setRequest(request, callback);
            // for each product we wait for:
            // 1. onReady to be called
            // 2. loadPurchased to be finished
            // 3. loadSkus to be finished
            int count = ProductTypes.ALL.size();
            for (String product : ProductTypes.ALL) {
                if (request.shouldLoadPurchases(product)) {
                    count++;
                }
                if (request.shouldLoadSkus(product)) {
                    count++;
                }
            }
            final long id = mCounter.newAttempt(count);

            // clear all previously loaded data
            mProducts = new Products();

            mCheckout.whenReady(new CheckoutListener(id));
        }

        return this;
    }

    private boolean onFinished(long id) {
        return onFinished(1, id);
    }

    private boolean onFinished(int count, long id) {
        synchronized (mLock) {
            final boolean loading = mCounter.countDown(count, id);
            if (loading && isLoaded()) {
                onLoaded();
            }
            return loading;
        }
    }

    @Override
    boolean isLoaded() {
        synchronized (mLock) {
            return mCounter.isFinished();
        }
    }

    private void loadPurchases(@Nonnull final BillingRequests requests, @Nonnull Product product, long id) {
        requests.getAllPurchases(product.id, new ProductRequestListener<Purchases>(product, id) {
            @Override
            public void onSuccess(@Nonnull Purchases purchases) {
                synchronized (mLock) {
                    if (isAlive()) {
                        mProduct.setPurchases(purchases.list);
                    }
                    onFinished(mId);
                }
            }
        });
    }

    private void loadSkus(@Nonnull BillingRequests requests, @Nonnull final Product product, long id) {
        final List<String> skuIds = getRequest().getSkus(product.id);
        if (!skuIds.isEmpty()) {
            requests.getSkus(product.id, skuIds, new ProductRequestListener<Skus>(product, id) {
                @Override
                public void onSuccess(@Nonnull Skus skus) {
                    synchronized (mLock) {
                        if (isAlive()) {
                            mProduct.setSkus(skus.list);
                        }
                        onFinished(mId);
                    }
                }
            });
        } else {
            Billing.warning("There are no SKUs for \"" + product.id + "\" product. No SKU information will be loaded");
            synchronized (mLock) {
                onFinished(id);
            }
        }
    }

    private static final class Counter {
        @Nonnull
        private final AtomicInteger mCount = new AtomicInteger(-1);

        @Nonnull
        private final AtomicLong mId = new AtomicLong();

        public long newAttempt(int count) {
            final long id = mId.incrementAndGet();
            mCount.set(count);
            return id;
        }

        /**
         * @param counts number of count downs
         * @param id     loading ID
         * @return true if current loading id is the same same as <var>mId</var>
         */
        public boolean countDown(int counts, long id) {
            if (mId.get() != id) {
                // new loading was requested => have to stop now
                return false;
            }
            Check.isFalse(isFinished(), "Inventory is already loaded. Loading id: " + id);
            mCount.addAndGet(-counts);
            return true;
        }

        public boolean isFinished() {
            return mCount.get() == 0L;
        }
    }

    private abstract class ProductRequestListener<R> implements RequestListener<R> {

        @Nonnull
        protected final Product mProduct;
        protected final long mId;

        protected ProductRequestListener(@Nonnull Product product, long id) {
            mProduct = product;
            mId = id;
        }

        boolean isAlive() {
            Check.isTrue(Thread.holdsLock(mLock), "Should be called from a synchronized block");
            final Product p = mProducts.get(mProduct.id);
            return p == mProduct;
        }

        @Override
        public final void onError(int response, @Nonnull Exception e) {
            onFinished(mId);
        }
    }

    private class CheckoutListener extends Checkout.ListenerAdapter {
        private final long mId;

        public CheckoutListener(long id) {
            mId = id;
        }

        @Override
        public void onReady(@Nonnull BillingRequests requests, @Nonnull String productId, boolean billingSupported) {
            final Product product = new Product(productId, billingSupported);
            synchronized (mLock) {
                final boolean loading = onFinished(mId);
                if (!loading) {
                    return;
                }
                mProducts.add(product);
                final Request request = getRequest();
                if (product.supported && request.shouldLoadPurchases(productId)) {
                    loadPurchases(requests, product, mId);
                } else {
                    onFinished(1, mId);
                }
                if (product.supported && request.shouldLoadSkus(productId)) {
                    loadSkus(requests, product, mId);
                } else {
                    onFinished(1, mId);
                }
            }
        }
    }
}
