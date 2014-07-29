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
import java.util.List;

/**
 * Available billing request methods.
 * Depending on configuration requests executed via methods might have different tags and results might be delivered
 * on different threads, see {@link org.solovyev.android.checkout.Billing.RequestsBuilder} for more
 * information.
 */
public interface BillingRequests {

	/**
	 * Checks if billing for specified <var>product</var> is supported
	 * @param product type of product, see {@link org.solovyev.android.checkout.ProductTypes}
	 * @return request id
	 *
	 * @see org.solovyev.android.checkout.ProductTypes
	 */
	int isBillingSupported(@Nonnull String product);

	/**
	 * Checks if billing for specified <var>product</var> is supported and returns result through <var>listener</var>
	 * @param product type of product, see {@link org.solovyev.android.checkout.ProductTypes}
	 * @param listener request listener, called asynchronously
	 * @return request id
	 *
	 * @see org.solovyev.android.checkout.ProductTypes
	 */
	int isBillingSupported(@Nonnull String product, @Nonnull RequestListener<Object> listener);

	/**
	 * Requests list of purchased items of <var>product</var> type.
	 * Note: In case if there are more than 700 items - continuation token is returned and might be used for further
	 * requests. See <a href="http://developer.android.com/google/play/billing/billing_integrate.html#QueryPurchases">Query Purchases</a>.
	 * for more information.
	 * @param product type of product, see {@link org.solovyev.android.checkout.ProductTypes}
	 * @param continuationToken token which is used for fetching more purchases
	 * @param listener request listener, called asynchronously
	 * @return request id
	 *
	 * @see org.solovyev.android.checkout.ProductTypes
	 */
	int getPurchases(@Nonnull String product, @Nullable String continuationToken, @Nonnull RequestListener<Purchases> listener);

	/**
	 * Same as {@link #getPurchases(String, String, RequestListener)} but will load all the purchases.
	 */
	int getAllPurchases(@Nonnull String product, @Nonnull RequestListener<Purchases> listener);

	/**
	 * Method checks if item with <var>sku</var> of <var>product</var> is purchased (i.e. there exists purchase with SKU=<var>sku</var> and state={@link org.solovyev.android.checkout.Purchase.State#PURCHASED}).
	 * This method checks ALL the purchases, even if there are more than 700 items.
	 * @param product type of product, see {@link org.solovyev.android.checkout.ProductTypes}
	 * @param sku SKU of item
	 * @param listener request listener, called asynchronously
	 * @return request id
	 */
	int isPurchased(@Nonnull String product, @Nonnull String sku, @Nonnull RequestListener<Boolean> listener);

	/**
	 * Requests list of available SKUs of <var>product</var> type.
	 * See <a href="http://developer.android.com/google/play/billing/billing_integrate.html#QueryDetails">Querying for Items Available for Purchase</a>
	 * @param product type of product
	 * @param skus list of SKUs to be loaded
	 * @param listener request listener, called asynchronously
	 * @return request id
	 */
	int getSkus(@Nonnull String product, @Nonnull List<String> skus, @Nonnull RequestListener<Skus> listener);

	/**
	 * Purchases an item with <var>sku</var> of <var>product</var>. This method only works in conjunction with
	 * {@link ActivityCheckout}. See {@link ActivityCheckout} for more information.
	 * See <a href="http://developer.android.com/google/play/billing/billing_integrate.html#Purchase">Purchase</a> docs.
	 * Note that cancelling of purchase process is not simple as this is a multi-step process. If {@link Request#cancel()}
	 * method is called before the request is executed then purchase flow stops. If it is called after - then only listener is
	 * cancelled (it won't receive any calls) but purchase process continues.
	 * @param product type of product
	 * @param sku SKU of item to be bought
	 * @param payload developer's payload
	 * @param purchaseFlow purchase flow associated with purchase process
	 * @return request id
	 */
	int purchase(@Nonnull String product, @Nonnull String sku, @Nullable String payload, @Nonnull PurchaseFlow purchaseFlow);

	int purchase(@Nonnull Sku sku, @Nullable String payload, @Nonnull PurchaseFlow purchaseFlow);

	/**
	 * Consumes previously purchased item.
	 * See <a href="http://developer.android.com/google/play/billing/billing_integrate.html#Consume">Consume</a> for more
	 * information.
	 * @param token token which was provided with purchase, see {@link Purchase#token}
	 * @param listener request listener, called asynchronously
	 * @return request id
	 */
	int consumePurchase(@Nonnull String token, @Nonnull RequestListener<Object> listener);

	void cancelAll();
}
