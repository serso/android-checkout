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
 * Base class for all {@link Inventory} implementations. Contains the last loaded
 * {@link Inventory.Products}.
 */
public abstract class BaseInventory implements Inventory {

    @Nonnull
    protected final Object mLock;
    @Nonnull
    protected final Checkout mCheckout;
    @GuardedBy("mLock")
    @Nonnull
    protected Products mProducts = Products.EMPTY;
    @GuardedBy("mLock")
    @Nonnull
    private SkuIds mSkus;
    @GuardedBy("mLock")
    @Nullable
    private Callback mCallback;

    protected BaseInventory(@Nonnull Checkout checkout) {
        mCheckout = checkout;
        mLock = checkout.mLock;
        mSkus = SkuIds.create();
    }

    protected final void setSkus(@Nonnull SkuIds skus, @Nonnull Callback callback) {
        Check.isTrue(Thread.holdsLock(mLock), "Must be locked");
        Check.isTrue(!skus.isEmpty(), "Skus must not be empty");
        mSkus = skus;
        mCallback = callback;
    }

    @Nonnull
    protected final SkuIds getSkus() {
        Check.isTrue(Thread.holdsLock(mLock), "Must be locked");
        return mSkus;
    }

    @Override
    public void cancel() {
        synchronized (mLock) {
            mCallback = null;
        }
    }

    @Override
    @Nonnull
    public final Products getProducts() {
        Check.isMainThread();
        synchronized (mLock) {
            if (!isLoaded()) {
                Billing.warning("Inventory is not loaded yet. Use Inventory#whenLoaded");
            }
            return mProducts;
        }
    }

    protected void onLoaded() {
        Check.isTrue(Thread.holdsLock(mLock), "Must be locked");
        Check.isTrue(isLoaded(), "Must be loaded");
        if (mCallback == null) {
            return;
        }
        mCallback.onLoaded(mProducts);
        mCallback = null;
    }

    /**
     * Note that this method should be called from inside of the synchronized block
     *
     * @return true if {@link #mProducts} are loaded
     */
    abstract boolean isLoaded();
}
