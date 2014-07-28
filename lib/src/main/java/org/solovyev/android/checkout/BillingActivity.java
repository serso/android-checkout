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
import android.os.Bundle;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class BillingActivity extends Activity {

	@Nonnull
	private final ActivityCheckout checkout;

	private final boolean oneShotPurchaseFlow;

	/**
	 * @param billing  billing controller
	 * @param products list of products to be supported in this activity
	 */
	protected BillingActivity(@Nonnull Billing billing, @Nonnull List<String> products) {
		this(billing, products, false);
	}

	protected BillingActivity(@Nonnull Billing billing, @Nonnull List<String> products, boolean oneShotPurchaseFlow) {
		checkout = Checkout.forActivity(this, billing, products);
		this.oneShotPurchaseFlow = oneShotPurchaseFlow;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		checkout.start();
		if (!oneShotPurchaseFlow) {
			checkout.createPurchaseFlow(createPurchaseListener());
		}
	}

	/**
	 * @return listener to be used while purchasing products
	 */
	@Nonnull
	protected abstract RequestListener<Purchase> createPurchaseListener();

	@Override
	protected void onResume() {
		super.onResume();
		checkout.onReady(new Checkout.ListenerAdapter() {
			@Override
			public void onReady(@Nonnull BillingRequests requests, @Nonnull String product, boolean billingSupported) {
				onBillingReady(requests, product, billingSupported);
			}
		});
	}

	/**
	 * Called when billing is ready for <var>product</var>.
	 * Note: billing for other products might not be ready yet, check the <var>product</var> argument if you have more
	 * than one supported product.
	 *
	 * @param requests         Billing API
	 * @param product          product for which billing is ready
	 * @param billingSupported true if billing is supported for specified <var>product</var>
	 */
	protected abstract void onBillingReady(@Nonnull BillingRequests requests, @Nonnull String product, boolean billingSupported);

	/**
	 * Start purchase flow for item with SKU=<var>sku</var> and of <var>product</var> type.
	 * It's save to call this method before the billing for product is ready. In that case purchase will not start
	 * until {@link Checkout} is not ready.
	 * After the purchase is flow is finished this activity will be notified via call
	 * @param product item product
	 * @param sku item SKU
	 */
	protected void purchase(@Nonnull final String product, @Nonnull final String sku) {
		checkout.onReady(new Checkout.ListenerAdapter() {
			@Override
			public void onReady(@Nonnull BillingRequests requests) {
				final PurchaseFlow l;
				if (!oneShotPurchaseFlow) {
					l = checkout.getPurchaseFlow();
				} else {
					l = checkout.createOneShotPurchaseFlow(createPurchaseListener());
				}
				requests.purchase(product, sku, null, l);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		checkout.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onDestroy() {
		checkout.stop();
		super.onDestroy();
	}
}
