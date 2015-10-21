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
import android.content.Intent;
import android.util.SparseArray;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * Recommended usage of this class with {@link android.app.Activity}:
 * <pre>
 * {@code
 * 	protected void onCreate(Bundle savedInstanceState) {
 * 		super.onCreate(savedInstanceState);
 * 		// ...
 * 		checkout.start();
 * 		// purchase flow is creates here as activity might be destroyed during purchase process (and with it all the purchase flows are destroyed).
 * 		checkout.createPurchaseFlow(createPurchaseListener());
 * 	}
 *
 * 	protected void purchase(@Nonnull final String product, @Nonnull final String sku) {
 * 		checkout.whenReady(new Checkout.BaseListener() {
 * 			public void onReady(@Nonnull BillingRequests requests) {
 * 				requests.purchase(product, sku, null, checkout.getPurchaseFlow());
 * 			}
 * 		});
 * 	}
 * }
 * </pre>
 * Another usage with "one-shot" purchase flows can be found in documentation for {@link ActivityCheckout#createOneShotPurchaseFlow(int, RequestListener)}.
 */
public final class ActivityCheckout extends Checkout {

	static final int DEFAULT_REQUEST_CODE = 0XCAFE;// mm, coffee

	@Nonnull
	private final SparseArray<PurchaseFlow> purchaseFlows = new SparseArray<PurchaseFlow>();

	@Nonnull
	private final Set<Integer> oneShotPurchaseFlows = new HashSet<Integer>();

	ActivityCheckout(@Nonnull final Activity activity, @Nonnull Billing billing, @Nonnull Products products) {
		super(activity, billing, products);
	}

	@Override
	public void stop() {
		oneShotPurchaseFlows.clear();
		purchaseFlows.clear();
		super.stop();
	}

	/**
	 * Same as {@link #createPurchaseFlow(int, RequestListener)} but with default request code
	 */
	public void createPurchaseFlow(@Nonnull RequestListener<Purchase> listener) {
		createPurchaseFlow(DEFAULT_REQUEST_CODE, listener);
	}

	/**
	 * Creates a permanent purchase flow with purchase <var>listener</var> to wait for purchase updates. Listener will receive updates only from
	 * purchase which was started with <var>requestCode</var>.
	 * All purchase flows are automatically destroyed in {@link #stop()} method.
	 * Permanent purchase flows are not destroyed when are finished (comparing to "one-shot" purchase flows), thus,
	 * <var>listener</var> methods might be called several times if several purchases with same <var>requestCode</var>
	 * are done.
	 *
	 * @param requestCode request code associated with purchase
	 * @param listener    purchase listener
	 */
	public void createPurchaseFlow(int requestCode, @Nonnull RequestListener<Purchase> listener) {
		createPurchaseFlow(requestCode, listener, false);
	}

	/**
	 * Same as {@link #destroyPurchaseFlow(int)} but with default request code
	 */
	public void destroyPurchaseFlow() {
		destroyPurchaseFlow(DEFAULT_REQUEST_CODE);
	}

	/**
	 * Destroys previously created purchase flow. Nothing happens if flow has already been destroyed.
	 *
	 * @param requestCode request code associated with purchase
	 */
	public void destroyPurchaseFlow(int requestCode) {
		final PurchaseFlow flow = purchaseFlows.get(requestCode);
		if (flow != null) {
			purchaseFlows.delete(requestCode);
			oneShotPurchaseFlows.remove(requestCode);

			// instead of cancelling purchase request in `Billing` class (which we can't do as we don't have `requestId`)
			// let's just cancel it here
			flow.cancel();
		}
	}

	/**
	 * Same as {@link #getPurchaseFlow(int)} with default request code.
	 */
	@Nonnull
	public PurchaseFlow getPurchaseFlow() {
		return getPurchaseFlow(DEFAULT_REQUEST_CODE);
	}

	/**
	 * @param requestCode request request code associated with purchase
	 * @return previously created purchase flow associated with <var>requestCode</var>
	 * @throws IllegalArgumentException if purchase flow for <var>requestCode</var> doesn't exist
	 */
	@Nonnull
	public PurchaseFlow getPurchaseFlow(int requestCode) {
		final PurchaseFlow flow = purchaseFlows.get(requestCode);
		if (flow == null) {
			throw new IllegalArgumentException("Purchase flow doesn't exist. Have you forgotten to create it?");
		}
		return flow;
	}

	/**
	 * Same as {@link #createOneShotPurchaseFlow(int, RequestListener)} with default request code
	 */
	@Nonnull
	public PurchaseFlow createOneShotPurchaseFlow(@Nonnull RequestListener<Purchase> listener) {
		return createOneShotPurchaseFlow(DEFAULT_REQUEST_CODE, listener);
	}

	/**
	 * Creates a new "one-shot" purchase flow associated with <var>requestCode</var> with purchase <var>listener</var>. As soon as purchase flow
	 * is finished it is destroyed and <var>listener</var> is unregistered. Next purchase should be done with a new purchase flow. This might
	 * be useful if activity is never destroyed - then instead of calling {@link ActivityCheckout#createPurchaseFlow(int, RequestListener)} in {@link android.app.Activity#onCreate(android.os.Bundle)}
	 * and {@link ActivityCheckout#getPurchaseFlow()} while starting purchase flow only this method might be used:
	 * <pre>
	 * {@code
	 * 	protected void onCreate(Bundle savedInstanceState) {
	 * 		super.onCreate(savedInstanceState);
	 * 		// ...
	 * 		checkout.start();
	 * 		// NOTE: we don't need to create purchase flow here, it is created in `purchase` method
	 * 	}
	 *
	 * 	protected void purchase(@Nonnull final String product, @Nonnull final String sku) {
	 * 		checkout.whenReady(new Checkout.BaseListener() {
	 * 			public void onReady(@Nonnull BillingRequests requests) {
	 * 				// listener will be unregistered when the purchase flow finishes. If this method is called with the same requestCode during
	 * 				// the purchase process exception will be raised
	 * 				requests.purchase(product, sku, null, checkout.createOneShotPurchaseFlow(createPurchaseListener()));
	 * 			}
	 * 		});
	 * 	}
	 * }
	 * </pre>
	 * <p/>
	 * See {@link ActivityCheckout#createPurchaseFlow(int, RequestListener)} for adding the permanent purchase flows.
	 *
	 * @param requestCode request code associated with purchase
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
		PurchaseFlow flow = purchaseFlows.get(requestCode);
		if (flow == null) {
			if (oneShot) {
				listener = new RequestListenerWrapper<Purchase>(listener) {
					@Override
					public void onError(int response, @Nonnull Exception e) {
						destroyPurchaseFlow(requestCode);
						super.onError(response, e);
					}
					@Override
					public void onCancel() {
						destroyPurchaseFlow(requestCode);
					}
					@Override
					public void onSuccess(@Nonnull Purchase result) {
						destroyPurchaseFlow(requestCode);
						super.onSuccess(result);
					}
				};
			}
			//noinspection ConstantConditions
			flow = billing.createPurchaseFlow((Activity) context, requestCode, listener);
			purchaseFlows.append(requestCode, flow);
			if (oneShot) {
				oneShotPurchaseFlows.add(requestCode);
			}
		} else {
			throw new IllegalArgumentException("Purchase flow associated with requestCode=" + requestCode + " already exists");
		}
		return flow;
	}

	/**
	 * When used with activity this method must be called from {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
	 *
	 * @return true if activity result was handled (there exists purchase flow for <var>requestCode</var>)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
		final PurchaseFlow flow = purchaseFlows.get(requestCode);
		if (flow != null) {
			flow.onActivityResult(requestCode, resultCode, data);
			return true;
		} else {
			Billing.warning("Purchase flow doesn't exist for requestCode=" + requestCode + ". Have you forgotten to create it?");
			return false;
		}
	}
}
