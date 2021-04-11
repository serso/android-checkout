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

import android.os.Bundle;

import com.android.vending.billing.InAppBillingServiceImpl;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface that defines all billing methods available in the library. This includes not only the
 * methods that come directly from {@link InAppBillingServiceImpl}
 * (such as {@link #isBillingSupported(String)} and {@link #consume(String, RequestListener)}) but
 * also some wrapper or utility methods defined by the library (such as
 * {@link #getAllPurchases(String, RequestListener)} and {@link #cancelAll()}).
 * All methods defined in this interface are executed asynchronously. {@link Billing} creates a
 * request object for every method call. Any created request can be cancelled via
 * {@link BillingRequests#cancel(int)}. All requests can be cancelled simultaneously via
 * {@link BillingRequests#cancelAll()} call.
 * Depending on the configuration {@link RequestListener} methods might be called on different
 * threads, see {@link Billing.RequestsBuilder} for more information.
 */
public interface BillingRequests {

    /**
     * Checks if billing v3 for the specified <var>product</var> is supported
     *
     * @param product product type, see {@link ProductTypes}
     * @return request id
     * @see ProductTypes
     */
    int isBillingSupported(@Nonnull String product);

    /**
     * Same as {@link #isBillingSupported(String)} but with the explicit API version argument.
     *
     * @param apiVersion API version to check
     * @return request id
     */
    int isBillingSupported(@Nonnull String product, int apiVersion);

    /**
     * Checks if billing v3 for specified <var>product</var> is supported and returns result
     * through the passed <var>listener</var>
     *
     * @param product  product type, see {@link ProductTypes}
     * @param listener request listener, called asynchronously
     * @return request id
     * @see ProductTypes
     */
    int isBillingSupported(@Nonnull String product, @Nonnull RequestListener<Object> listener);

    /**
     * Same as {@link #isBillingSupported(String, RequestListener)} but with the explicit API
     * version argument.
     *
     * @param apiVersion API version to check
     */
    int isBillingSupported(@Nonnull String product, int apiVersion, @Nonnull RequestListener<Object> listener);

    /**
     * Checks whether billing for the specified <var>product</var>, <var>version</var> and <var>arguments</var>
     * is supported.
     *
     * @param extraParams bundle with extra arguments
     */
    int isBillingSupported(@Nonnull String product, int apiVersion, @Nonnull Bundle extraParams, @Nonnull RequestListener<Object> listener);

    /**
     * Requests a list of purchased items of the given <var>product</var> type.
     * Note: In case if there are more than 700 items - continuation token is returned and might be
     * used for further requests. See <a href="http://developer.android.com/google/play/billing/billing_integrate.html#QueryPurchases">Query
     * Purchases</a> for more information.
     *
     * @param product           product type, see {@link ProductTypes}
     * @param continuationToken token which is used for fetching more purchases
     * @param listener          request listener, called asynchronously
     * @return request id
     * @see ProductTypes
     */
    int getPurchases(@Nonnull String product, @Nullable String continuationToken, @Nonnull RequestListener<Purchases> listener);

    /**
     * Same as {@link #getPurchases(String, String, RequestListener)} but it automatically
     * loads all the purchases passing "continuationToken" recursively until there are no more items
     * to load.
     */
    int getAllPurchases(@Nonnull String product, @Nonnull RequestListener<Purchases> listener);

    /**
     * Requests a list of purchased items of the given <var>product</var> type with the given
     * <var>extraParams</var> bundle. In contrast to {@link #getPurchases(String, String, RequestListener)}
     * this method loads a purchase even if it is expired, canceled or consumed. The loaded {@link Purchase}
     * has only certain fields set such as productId, purchaseTime, developerPayload and purchaseToken.
     * Values of other fields in {@link Purchase} are *undefined* and should *not* be used.
     * Note: In case if there are more than 700 items - continuation token is returned that might be
     * used for further requests.
     *
     * @param product           product type, see {@link ProductTypes}
     * @param continuationToken token which is used for fetching more purchases
     * @param extraParams       extra arguments, see <a href="https://developer.android.com/google/play/billing/billing_reference.html#getPurchaseHistory">getPurchaseHistory</a> for details.
     * @param listener          request listener, called asynchronously
     * @return request id
     */
    int getPurchaseHistory(@Nonnull String product, @Nullable String continuationToken, @Nullable Bundle extraParams, @Nonnull RequestListener<Purchases> listener);

    /**
     * Same as {@link #getPurchaseHistory(String, String, Bundle, RequestListener)} but it automatically
     * loads all the purchases passing "continuationToken" recursively until there are no more items
     * to load.
     */
    int getWholePurchaseHistory(@Nonnull String product, @Nullable Bundle extraParams, @Nonnull RequestListener<Purchases> listener);

    /**
     * Checks whether it is possible to call {@link #getPurchaseHistory(String, String, Bundle, RequestListener)}
     * in this version of Billing API. It is equivalent of calling {@link #isBillingSupported(String, int)}
     * with {@code version=6}.
     */
    int isGetPurchaseHistorySupported(@Nonnull String product, @Nonnull RequestListener<Object> listener);

    /**
     * Method checks if an item with the given <var>sku</var> of <var>product</var> type is
     * purchased (i.e. there is a purchase with SKU=<var>sku</var> in {@link
     * Purchase.State#PURCHASED} state). This method checks ALL the purchases, even if there are
     * more than 700 items.
     *
     * @param product  product type, see {@link ProductTypes}
     * @param sku      SKU of item
     * @param listener request listener, called asynchronously
     * @return request id
     */
    int isPurchased(@Nonnull String product, @Nonnull String sku, @Nonnull RequestListener<Boolean> listener);

    /**
     * Requests a list of available SKUs of a <var>product</var> type.
     * See <a href="http://developer.android.com/google/play/billing/billing_integrate.html#QueryDetails">Querying
     * for Items Available for Purchase</a>
     *
     * @param product  product type
     * @param skus     list of SKUs to be loaded
     * @param listener request listener, called asynchronously
     * @return request id
     */
    int getSkus(@Nonnull String product, @Nonnull List<String> skus, @Nonnull RequestListener<Skus> listener);

    /**
     * Purchases an item with the given <var>sku</var> of a <var>product</var> type. This method
     * only works in conjunction with {@link UiCheckout}.
     * See {@link UiCheckout} for more information.
     * See also <a href="http://developer.android.com/google/play/billing/billing_integrate.html#Purchase">Purchase</a>
     * docs.
     * Note that cancelling of a purchase process is not simple as it is a multi-step process. If
     * {@link Request#cancel()} method is called before the request is executed then purchase flow
     * stops. If it is called after - then only the listener is cancelled (it won't receive any
     * calls) but the purchase process continues.
     *
     * @param product      product type
     * @param sku          SKU of item to be bought
     * @param payload      developer's payload
     * @param purchaseFlow purchase flow associated with purchase process
     * @return request id
     */
    int purchase(@Nonnull String product, @Nonnull String sku, @Nullable String payload, @Nonnull PurchaseFlow purchaseFlow);

    /**
     * Same as {@link #purchase(String, String, String, PurchaseFlow)} but with a bundle of extra
     * parameters.
     * This method is only supported in Billing API v.6.
     */
    int purchase(@Nonnull String product, @Nonnull String sku, @Nullable String payload, @Nullable Bundle extraParams, @Nonnull PurchaseFlow purchaseFlow);

    /**
     * Checks whether it is possible to call {@link #purchase(String, String, String, Bundle, PurchaseFlow)}
     * in this version of Billing API. It is equivalent of calling {@link #isBillingSupported(String, int)}
     * with {@code version=6}.
     */
    int isPurchaseWithExtraParamsSupported(@Nonnull String product, @Nonnull RequestListener<Object> listener);

    /**
     * @see #purchase(String, String, String, PurchaseFlow)
     */
    int purchase(@Nonnull Sku sku, @Nullable String payload, @Nonnull PurchaseFlow purchaseFlow);

    /**
     * Upgrades/downgrades a list of the current subscriptions to a given subscription. This method
     * only works in conjunction with {@link UiCheckout}
     * See {@link UiCheckout} for more information.
     * See also <a href="https://developer.android.com/google/play/billing/billing_reference.html#upgrade-getBuyIntentToReplaceSkus">getBuyIntentToReplaceSkus()</a>
     * docs.
     *
     * @param oldSkus      list of current subscriptions to be changed
     * @param newSku       new subscription
     * @param payload      developer's payload
     * @param purchaseFlow purchase flow associated with purchase process
     * @return request id
     */
    int changeSubscription(@Nonnull List<String> oldSkus,
                           @Nonnull String newSku, @Nullable String payload, @Nonnull PurchaseFlow purchaseFlow);

    /**
     * @see #changeSubscription(List, String, String, PurchaseFlow)
     */
    int changeSubscription(@Nonnull List<Sku> oldSkus,
                           @Nonnull Sku newSku, @Nullable String payload, @Nonnull PurchaseFlow purchaseFlow);

    /**
     * Checks whether it is possible to change the subscription in this version of Billing API. It
     * is equivalent of calling {@link #isBillingSupported(String, int)} with {@code
     * product=ProductTypes.SUBSCRIPTION} and {@code version=5}.
     */
    int isChangeSubscriptionSupported(@Nonnull RequestListener<Object> listener);

    /**
     * Consumes a previously purchased item.
     * See <a href="http://developer.android.com/google/play/billing/billing_integrate.html#Consume">Consume</a>
     * for more information.
     *
     * @param token    token which was provided with purchase, see {@link Purchase#token}
     * @param listener request listener, called asynchronously
     * @return request id
     */
    int consume(@Nonnull String token, @Nonnull RequestListener<Object> listener);

    /**
     * Cancels all pending requests created by this {@link BillingRequests} instance.
     */
    void cancelAll();

    /**
     * Cancels a pending request with the given <var>requestId</var>.
     * @param requestId request id
     */
    void cancel(int requestId);
}
