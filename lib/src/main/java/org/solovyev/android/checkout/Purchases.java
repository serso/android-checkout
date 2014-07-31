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
import org.json.JSONException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * List of purchased items of <var>product</var> type.
 */
@Immutable
public final class Purchases {

	/**
	 * Product type
	 */
	@Nonnull
	public final String product;

	/**
	 * Purchased items
	 */
	@Nonnull
	public final List<Purchase> list;

	/**
	 * Token to be used to request more purchases, see <a href="http://developer.android.com/google/play/billing/billing_integrate.html#QueryPurchases">Query Purchases</a> docs.
	 *
	 * @see BillingRequests#getPurchases(String, String, RequestListener)
	 */
	@Nullable
	public final String continuationToken;

	Purchases(@Nonnull String product, @Nonnull List<Purchase> list, @Nullable String continuationToken) {
		this.product = product;
		this.list = Collections.unmodifiableList(list);
		this.continuationToken = continuationToken;
	}

	@Nonnull
	static Purchases fromBundle(@Nonnull Bundle bundle, @Nonnull String product) throws JSONException {
		final List<String> datas = bundle.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
		final List<String> signatures = bundle.getStringArrayList("INAPP_DATA_SIGNATURE");
		final String continuationToken = bundle.getString("INAPP_CONTINUATION_TOKEN");

		final List<Purchase> purchases = new ArrayList<Purchase>(datas.size());
		for (int i = 0; i < datas.size(); i++) {
			final String data = datas.get(i);
			final String signature = signatures != null ? signatures.get(i) : null;
			purchases.add(Purchase.fromData(data, signature));
		}
		return new Purchases(product, purchases, continuationToken);
	}

	@Nullable
	public Purchase getPurchase(@Nonnull String sku) {
		for (Purchase purchase : list) {
			if (purchase.sku.equals(sku)) {
				return purchase;
			}
		}
		return null;
	}

	/**
	 * <b>Note</b>: this method doesn't check state of the purchase
	 * @param sku SKU of purchase to be found
	 * @return true if purchase with specified <var>sku</var> exists
	 */
	public boolean hasPurchase(@Nonnull String sku) {
		return getPurchase(sku) != null;
	}

	/**
	 * @param sku SKU of purchase to be found
	 * @param state state of the purchase to be found
	 * @return true if purchase with specified <var>sku</var> and <var>state</var> exists
	 */
	public boolean hasPurchaseInState(@Nonnull String sku, @Nonnull Purchase.State state) {
		return getPurchaseInState(sku, state) != null;
	}

	@Nullable
	public Purchase getPurchaseInState(@Nonnull String sku, @Nonnull Purchase.State state) {
		return getPurchaseInState(list, sku, state);
	}

	@Nullable
	static Purchase getPurchaseInState(@Nonnull List<Purchase> purchases, @Nonnull String sku, @Nonnull Purchase.State state) {
		for (Purchase purchase : purchases) {
			if (purchase.sku.equals(sku)) {
				if (purchase.state == state) {
					return purchase;
				}
			}
		}
		return null;
	}
}
