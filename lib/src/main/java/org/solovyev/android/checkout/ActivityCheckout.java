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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.SparseArray;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Variant of {@link Checkout} that can initiate a purchase. {@link ActivityCheckout} lives in the
 * context of {@link Activity} as it is required by Billing API to conduct a purchase.
 * Usage example:
 * <pre>
 * {@code
 *
 *  protected final ActivityCheckout mCheckout = Checkout.forActivity(this, getBilling());
 *
 *  protected void onCreate(Bundle savedInstanceState) {
 *      super.onCreate(savedInstanceState);
 *      mCheckout.start();
 *  }
 *
 *  protected void purchase(final String product, final String sku) {
 *      mCheckout.whenReady(new Checkout.EmptyListener() {
 *          public void onReady(BillingRequests requests) {
 *              requests.purchase(product, sku, null, checkout.getPurchaseFlow());
 *          }
 *      });
 *  }
 *
 *  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
 *      super.onActivityResult(requestCode, resultCode, data);
 *      mCheckout.onActivityResult(requestCode, resultCode, data);
 *  }
 *
 *  protected void onDestroy() {
 *      mCheckout.stop();
 *      super.onDestroy();
 *  }
 * }
 * </pre>
 * Another usage with "one-shot" purchase flows can be found in documentation for {@link
 * ActivityCheckout#createOneShotPurchaseFlow(int, RequestListener)}.
 */
public final class ActivityCheckout extends Checkout {

    static final int DEFAULT_REQUEST_CODE = 0XCAFE;// mm, coffee

    @Nonnull
    private final SparseArray<PurchaseFlow> mFlows = new SparseArray<>();

    ActivityCheckout(@Nullable Context context, @Nonnull Billing billing) {
        super(context, billing);
    }

    @Override
    public void stop() {
        mFlows.clear();
        super.stop();
    }

    /**
     * Same as {@link #createPurchaseFlow(int, RequestListener)} but with the default request code
     */
    public void createPurchaseFlow(@Nonnull RequestListener<Purchase> listener) {
        createPurchaseFlow(DEFAULT_REQUEST_CODE, listener);
    }

    /**
     * Creates a permanent purchase flow with a <var>listener</var> that receives purchase
     * updates. Listener will receive updates only from the purchase marked with given
     * <var>requestCode</var>.
     * All flows are automatically destroyed in {@link #stop()} method.
     * Permanent purchase flows are not destroyed when they are finished (comparing to "one-shot"
     * flows), thus, <var>listener</var> methods might be called several times if several purchases
     * with the same <var>requestCode</var> were initiated.
     *
     * @param requestCode request code associated with a purchase
     * @param listener    purchase listener
     */
    public void createPurchaseFlow(int requestCode, @Nonnull RequestListener<Purchase> listener) {
        createPurchaseFlow(requestCode, listener, false);
    }

    /**
     * Same as {@link #destroyPurchaseFlow(int)} but with the default request code
     */
    public void destroyPurchaseFlow() {
        destroyPurchaseFlow(DEFAULT_REQUEST_CODE);
    }

    /**
     * Destroys previously created purchase flow. Nothing happens if flow has already been
     * destroyed.
     *
     * @param requestCode purchase request code
     */
    public void destroyPurchaseFlow(int requestCode) {
        final PurchaseFlow flow = mFlows.get(requestCode);
        if (flow == null) {
            return;
        }
        mFlows.delete(requestCode);
        // instead of cancelling purchase request in `Billing` class (which we can't do as we don't
        // have `requestId`) let's cancel it here
        flow.cancel();
    }

    /**
     * Same as {@link #getPurchaseFlow(int)} with the default request code.
     */
    @Nonnull
    public PurchaseFlow getPurchaseFlow() {
        return getPurchaseFlow(DEFAULT_REQUEST_CODE);
    }

    /**
     * @param requestCode request request code associated with a purchase
     * @return previously created purchase flow associated with <var>requestCode</var>
     * @throws IllegalArgumentException if purchase flow for <var>requestCode</var> doesn't exist
     */
    @Nonnull
    public PurchaseFlow getPurchaseFlow(int requestCode) {
        final PurchaseFlow flow = mFlows.get(requestCode);
        if (flow == null) {
            throw new IllegalArgumentException("Purchase flow doesn't exist. Have you forgotten to create it?");
        }
        return flow;
    }

    /**
     * Same as {@link #createOneShotPurchaseFlow(int, RequestListener)} with the default request
     * code.
     */
    @Nonnull
    public PurchaseFlow createOneShotPurchaseFlow(@Nonnull RequestListener<Purchase> listener) {
        return createOneShotPurchaseFlow(DEFAULT_REQUEST_CODE, listener);
    }

    /**
     * Creates a new "one-shot" purchase flow associated with the given <var>requestCode</var> and
     * <var>listener</var>. As soon as the flow is finished it is destroyed and <var>listener</var>
     * is unregistered. Next purchase should be initiated with a new purchase flow. This might
     * be useful if activity is never destroyed - then instead of calling {@link
     * ActivityCheckout#createPurchaseFlow(int, RequestListener)} in {@link
     * Activity#onCreate(android.os.Bundle)} and {@link ActivityCheckout#getPurchaseFlow()}
     * while starting a purchase flow only this method might be used:
     * <pre>
     * {@code
     *
     *  protected final ActivityCheckout mCheckout = Checkout.forActivity(this, getBilling());
     *
     *  protected void onCreate(Bundle savedInstanceState) {
     *      super.onCreate(savedInstanceState);
     *      // ...
     *      mCheckout.start();
     *      // NOTE: we don't need to create purchase flow here, it is created in `purchase` method
     *  }
     *
     *  protected void purchase(final String product, final String sku) {
     *      mCheckout.whenReady(new Checkout.EmptyListener() {
     *          public void onReady(BillingRequests requests) {
     *              // listener will be unregistered when the purchase flow finishes. If this
     *              // method is called several times with the same requestCode an exception is
     *              // raised
     *              requests.purchase(product, sku, null, checkout.createOneShotPurchaseFlow(createPurchaseListener()));
     *          }
     *      });
     * 	}
     * }
     * </pre>
     * <p/>
     * See {@link ActivityCheckout#createPurchaseFlow(int, RequestListener)} for creating a
     * permanent purchase flows.
     *
     * @param requestCode request code associated with a purchase
     * @param listener    purchase listener
     * @return newly created "one-shot" purchase flow
     * @throws IllegalArgumentException if purchase flow for <var>requestCode</var> already exists
     */
    @Nonnull
    public PurchaseFlow createOneShotPurchaseFlow(int requestCode, @Nonnull RequestListener<Purchase> listener) {
        return createPurchaseFlow(requestCode, listener, true);
    }

    @Nonnull
    private PurchaseFlow createPurchaseFlow(final int requestCode, @Nonnull RequestListener<Purchase> listener, boolean oneShot) {
        PurchaseFlow flow = mFlows.get(requestCode);
        if (flow != null) {
            throw new IllegalArgumentException("Purchase flow associated with requestCode=" + requestCode + " already exists");
        }
        if (oneShot) {
            listener = new OneShotRequestListener(listener, requestCode);
        }
        flow = mBilling.createPurchaseFlow(getActivity(), requestCode, listener);
        mFlows.append(requestCode, flow);
        return flow;
    }

    private Activity getActivity() {
        return (Activity) mContext;
    }

    /**
     * This method must be called from {@link Activity#onActivityResult(int, int, Intent)} in order
     * to finish a purchase flow.
     *
     * @return true if activity result was handled (there existed a purchase flow for the given
     * <var>requestCode</var>)
     * @see Activity#onActivityResult(int, int, Intent)
     */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        final PurchaseFlow flow = mFlows.get(requestCode);
        if (flow == null) {
            Billing.warning("Purchase flow doesn't exist for requestCode=" + requestCode + ". Have you forgotten to create it?");
            return false;
        }
        flow.onActivityResult(requestCode, resultCode, data);
        return true;
    }

    private class OneShotRequestListener extends RequestListenerWrapper<Purchase> {
        private final int mRequestCode;

        public OneShotRequestListener(RequestListener<Purchase> listener, int requestCode) {
            super(listener);
            mRequestCode = requestCode;
        }

        @Override
        public void onError(int response, @Nonnull Exception e) {
            destroyPurchaseFlow(mRequestCode);
            super.onError(response, e);
        }

        @Override
        public void onCancel() {
            destroyPurchaseFlow(mRequestCode);
        }

        @Override
        public void onSuccess(@Nonnull Purchase result) {
            destroyPurchaseFlow(mRequestCode);
            super.onSuccess(result);
        }
    }
}
