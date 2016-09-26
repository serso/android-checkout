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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * Default Billing v.3 {@link Inventory}. Loads the purchase information from the {@link Checkout}.
 */
final class CheckoutInventory extends BaseInventory {

    private class MyTask extends Task implements Checkout.Listener {

        @GuardedBy("mLock")
        private int mCount;

        MyTask(Request request, Callback callback) {
            super(request, callback);
            // for each product we wait for:
            // 1. onReady to be called
            // 2. loadPurchased to be finished
            // 3. loadSkus to be finished
            mCount = ProductTypes.ALL.size() * 3;
        }

        @Override
        public void run() {
            mCheckout.whenReady(this);
        }

        @Override
        public void onReady(@Nonnull BillingRequests requests) {
        }

        @Override
        public void onReady(@Nonnull BillingRequests requests, @Nonnull String productId,
                boolean billingSupported) {
            final Product product = new Product(productId, billingSupported);
            synchronized (mLock) {
                countDown();
                mProducts.add(product);
                if (!isCancelled() && product.supported && mRequest.shouldLoadPurchases(productId)) {
                    loadPurchases(requests, product);
                } else {
                    countDown(1);
                }
                if (!isCancelled() && product.supported && mRequest.shouldLoadSkus(productId)) {
                    loadSkus(requests, product);
                } else {
                    countDown(1);
                }
            }
        }

        private void countDown() {
            Check.isTrue(Thread.holdsLock(mLock), "Must be synchronized");
            countDown(1);
        }

        private void countDown(int count) {
            Check.isTrue(Thread.holdsLock(mLock), "Must be synchronized");
            mCount -= count;
            Check.isTrue(mCount >= 0, "Can't be negative");
            if (mCount == 0) {
                onDone();
            }
        }

        private void loadPurchases(@Nonnull final BillingRequests requests,
                @Nonnull final Product product) {
            requests.getAllPurchases(product.id, synchronizedListener(new RequestListener<Purchases>() {
                @Override
                public void onSuccess(@Nonnull Purchases purchases) {
                    product.setPurchases(purchases.list);
                    countDown();
                }

                @Override
                public void onError(int response, @Nonnull Exception e) {
                    countDown();
                }
            }));
        }

        private void loadSkus(@Nonnull BillingRequests requests, @Nonnull final Product product) {
            final List<String> skuIds = mRequest.getSkus(product.id);
            if (skuIds.isEmpty()) {
                Billing.warning("There are no SKUs for \"" + product.id
                        + "\" product. No SKU information will be loaded");
                synchronized (mLock) {
                    countDown();
                }
                return;
            }
            requests.getSkus(product.id, skuIds, synchronizedListener(new RequestListener<Skus>() {
                @Override
                public void onSuccess(@Nonnull Skus skus) {
                    product.setSkus(skus.list);
                    countDown();
                }

                @Override
                public void onError(int response, @Nonnull Exception e) {
                    countDown();
                }
            }));
        }
    }

    CheckoutInventory(@Nonnull Checkout checkout) {
        super(checkout);
    }

    @Override
    protected Task createTask(@Nonnull Request request, @Nonnull Callback callback) {
        return new MyTask(request, callback);
    }
}
