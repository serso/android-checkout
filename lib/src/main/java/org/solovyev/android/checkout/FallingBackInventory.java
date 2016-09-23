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
import javax.annotation.concurrent.GuardedBy;

/**
 * Inventory which falls back to fallback {@link Inventory} if one of the products is not
 * supported.
 */
class FallingBackInventory extends BaseInventory {

    @Nonnull
    private final CheckoutInventory mMainInventory;
    @Nonnull
    private final Inventory mFallbackInventory;
    @Nonnull
    private final MainCallback mMainCallback = new MainCallback();
    @Nonnull
    private final FallbackCallback mFallbackCallback = new FallbackCallback();

    public FallingBackInventory(@Nonnull Checkout checkout, @Nonnull Inventory fallbackInventory) {
        super(checkout);
        mMainInventory = new CheckoutInventory(checkout);
        mFallbackInventory = fallbackInventory;
    }

    @Nonnull
    @Override
    public Inventory load(@Nonnull SkuIds skus, @Nonnull Callback callback) {
        synchronized (mLock) {
            setSkus(skus, callback);
            mMainCallback.load(skus);
        }
        return this;
    }

    boolean isLoaded() {
        synchronized (mLock) {
            return mProducts == mMainCallback.mProducts;
        }
    }

    private void onProductsLoaded(@Nonnull Products products) {
        synchronized (mLock) {
            mMainCallback.mProducts = products;
            mProducts = products;
            onLoaded();
        }
    }

    private class MainCallback implements Callback {

        @GuardedBy("mLock")
        @Nullable
        private Products mProducts;

        @Override
        public void onLoaded(@Nonnull Products products) {
            synchronized (mLock) {
                if (mProducts != null) {
                    return;
                }
                if (!existsUnsupported(products)) {
                    onProductsLoaded(products);
                    return;
                }
                mFallbackCallback.load(products, getSkus());
            }
        }

        private boolean existsUnsupported(@Nonnull Products products) {
            for (Product product : products) {
                if (!product.supported) {
                    return true;
                }
            }

            return false;
        }

        public void load(SkuIds skus) {
            Check.isTrue(Thread.holdsLock(mLock), "Must be synchronized");
            mProducts = null;
            mMainInventory.load(skus, this);
        }
    }

    private class FallbackCallback implements Callback {

        @GuardedBy("mLock")
        @Nonnull
        private Products mProducts;

        @Override
        public void onLoaded(@Nonnull Products products) {
            synchronized (mLock) {
                if (mMainCallback.mProducts != null) {
                    return;
                }
                mProducts.merge(products);
                onProductsLoaded(mProducts);
            }
        }

        public void load(Products products, SkuIds skus) {
            Check.isTrue(Thread.holdsLock(mLock), "Must be synchronized");
            mProducts = products;
            mFallbackInventory.load(skus, this);
        }
    }
}
