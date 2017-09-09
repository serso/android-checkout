package org.solovyev.android.checkout;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Variant of {@link Checkout} that can initiate a purchase. {@link UiCheckout} lives in the
 * context of some UI (for example, {@link Activity} or {@link android.app.Fragment}) as it is
 * required by Billing API to conduct a purchase.
 * Usage example:
 * <pre>
 * {@code
 *
 *  protected final UiCheckout mCheckout = Checkout.forActivity(this, getBilling());
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
 * UiCheckout#createOneShotPurchaseFlow(int, RequestListener)}.
 *
 * @see Checkout#forFragment(Fragment, Billing)
 * @see Checkout#forUi(IntentStarter, Object, Billing)
 * @see Checkout#forActivity(Activity, Billing)
 */
public abstract class UiCheckout extends Checkout {

    static final int DEFAULT_REQUEST_CODE = 0XCAFE;// mm, coffee

    @Nonnull
    private final SparseArray<PurchaseFlow> mFlows = new SparseArray<>();

    protected UiCheckout(@Nonnull Object tag, @Nonnull Billing billing) {
        super(tag, billing);
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
     * is unregistered. Next purchase should be initiated with a new purchase flow. Instead of
     * calling
     * {@link UiCheckout#createPurchaseFlow(int, RequestListener)} in {@link
     * Activity#onCreate(android.os.Bundle)} (or {@link android.app.Fragment#onCreate(android.os.Bundle)})
     * and {@link UiCheckout#getPurchaseFlow()} while starting a purchase flow only this method
     * might be used:
     * <pre>
     * {@code
     *
     *  protected final UiCheckout mCheckout = Checkout.forActivity(this, getBilling());
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
     * See {@link UiCheckout#createPurchaseFlow(int, RequestListener)} for creating a
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
        flow = mBilling.createPurchaseFlow(makeIntentStarter(), requestCode, listener);
        mFlows.append(requestCode, flow);
        return flow;
    }

    @Nonnull
    protected abstract IntentStarter makeIntentStarter();

    /**
     * Creates a one-shot {@link PurchaseFlow} and tries starting it. If {@link Checkout} is not
     * ready the start is postponed.
     */
    public void startPurchaseFlow(final String product, final String sku, @Nullable final String payload, final RequestListener<Purchase> listener) {
        startPurchaseFlow(product, sku, payload, null, listener);
    }

    /**
     * Same as {@link #startPurchaseFlow(String, String, String, RequestListener)} but with extra
     * parameters. The extra parameters work only in the billing API v6.
     */
    public void startPurchaseFlow(final String product, final String sku, @Nullable final String payload, @Nullable final Bundle extraParams, final RequestListener<Purchase> listener) {
        createOneShotPurchaseFlow(listener);
        whenReady(new EmptyListener() {
            @Override
            public void onReady(@Nonnull BillingRequests requests) {
                requests.purchase(product, sku, payload, extraParams, getPurchaseFlow());
            }
        });
    }

    /**
     * @see #startPurchaseFlow(String, String, String, RequestListener)
     */
    public void startPurchaseFlow(Sku sku, @Nullable String payload, RequestListener<Purchase> listener) {
        startPurchaseFlow(sku, payload, null, listener);
    }

    public void startPurchaseFlow(Sku sku, @Nullable String payload, @Nullable Bundle extraParams, RequestListener<Purchase> listener) {
        startPurchaseFlow(sku.id.product, sku.id.code, payload, extraParams, listener);
    }

    /**
     * This method must be called from {@link Activity#onActivityResult(int, int, Intent)} (or
     * {@link android.app.Fragment#onActivityResult(int, int, Intent)}) in order to finish a started
     * purchase flow.
     *
     * @return true if activity result was handled (there existed a purchase flow for the given
     * <var>requestCode</var>)
     * @see Activity#onActivityResult(int, int, Intent)
     * @see android.app.Fragment#onActivityResult(int, int, Intent)
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
