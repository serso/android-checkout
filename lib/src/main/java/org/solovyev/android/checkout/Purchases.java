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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

import com.android.vending.billing.InAppBillingServiceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * List of the purchases of the {@link #product} type as returned from
 * {@link InAppBillingServiceImpl#getPurchases(int, String, String,
 * String)}.
 */
@Immutable
public final class Purchases {

    static final String BUNDLE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST";
    static final String BUNDLE_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST";
    static final String BUNDLE_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN";
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
     * Token to be used to request more purchases, see <a href="http://developer.android.com/google/play/billing/billing_integrate.html#QueryPurchases">Query
     * Purchases</a> docs.
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
        final String continuationToken = getContinuationTokenFromBundle(bundle);
        final List<Purchase> purchases = getListFromBundle(bundle);
        return new Purchases(product, purchases, continuationToken);
    }

    @Nullable
    static String getContinuationTokenFromBundle(@Nonnull Bundle bundle) {
        return bundle.getString(BUNDLE_CONTINUATION_TOKEN);
    }

    @Nonnull
    static List<Purchase> getListFromBundle(@Nonnull Bundle bundle) throws JSONException {
        final List<String> datas = extractDatasList(bundle);
        final List<String> signatures = bundle.getStringArrayList(BUNDLE_SIGNATURE_LIST);

        final List<Purchase> purchases = new ArrayList<>(datas.size());
        for (int i = 0; i < datas.size(); i++) {
            final String data = datas.get(i);
            final String signature = signatures != null ? signatures.get(i) : "";
            purchases.add(Purchase.fromJson(data, signature));
        }
        return purchases;
    }

    @Nonnull
    private static List<String> extractDatasList(@Nonnull Bundle bundle) {
        final List<String> list = bundle.getStringArrayList(BUNDLE_DATA_LIST);
        return list != null ? list : Collections.<String>emptyList();
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

    @Nonnull
    static List<Purchase> neutralize(@Nonnull List<Purchase> purchases) {
        // probably, it's possible to avoid creation of temporary list. The reason for it is that we don't want to
        // modify original list
        purchases = new LinkedList<>(purchases);

        final List<Purchase> result = new ArrayList<>(purchases.size());

        Collections.sort(purchases, PurchaseComparator.earliestFirst());
        while (!purchases.isEmpty()) {
            final Purchase purchase = purchases.get(0);
            switch (purchase.state) {
                case PURCHASED:
                    if (!isNeutralized(purchases, purchase)) {
                        result.add(purchase);
                    }
                    break;
                case CANCELLED:
                case REFUNDED:
                case EXPIRED:
                    if (!isDangling(purchases, purchase)) {
                        result.add(purchase);
                    }
                    break;
            }
            purchases.remove(0);
        }

        // purchases were added earliest first but we want result to be latest first
        Collections.reverse(result);

        return result;
    }

    private static boolean isDangling(@Nonnull List<Purchase> purchases, @Nonnull Purchase purchase) {
        Check.isFalse(purchase.state == Purchase.State.PURCHASED, "Must not be PURCHASED");
        for (int i = 1; i < purchases.size(); i++) {
            final Purchase same = purchases.get(i);
            if (same.sku.equals(purchase.sku)) {
                // for not purchases transaction exists newer transaction => this transaction is dangling
                return true;
            }
        }

        return false;
    }

    private static boolean isNeutralized(@Nonnull List<Purchase> purchases, @Nonnull Purchase purchase) {
        Check.isTrue(purchase.state == Purchase.State.PURCHASED, "Must be PURCHASED");
        for (int i = 1; i < purchases.size(); i++) {
            final Purchase same = purchases.get(i);
            if (same.sku.equals(purchase.sku)) {
                switch (same.state) {
                    case PURCHASED:
                        // found same later purchase => obviously there is a bug somewhere as user can't own
                        // several purchases with same SKU. For now let's skip the item
                        Billing.warning("Two purchases with same SKU found: " + purchase + " and " + same);
                        break;
                    case CANCELLED:
                    case REFUNDED:
                    case EXPIRED:
                        // neutralization found => need to remove it
                        purchases.remove(i);
                        break;
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Same as {@link #toJson(boolean)}  with {@code withSignatures=false}
     *
     * @return JSON representation of this object
     */
    @Nonnull
    public String toJson() {
        return toJson(false);
    }

    /**
     * @param withSignatures if true then {@link Purchase} will include signature field
     * @return JSON representation of this object
     */
    @Nonnull
    public String toJson(boolean withSignatures) {
        return toJsonObject(withSignatures).toString();
    }

    @Nonnull
    JSONObject toJsonObject(boolean withSignatures) {
        final JSONObject json = new JSONObject();
        try {
            json.put("product", product);
            final JSONArray array = new JSONArray();
            for (int i = 0; i < list.size(); i++) {
                final Purchase purchase = list.get(i);
                array.put(i, purchase.toJsonObject(withSignatures));
            }
            json.put("list", array);
        } catch (JSONException e) {
            // should never happen
            throw new AssertionError(e);
        }
        return json;
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
     *
     * @param sku SKU of purchase to be found
     * @return true if purchase with specified <var>sku</var> exists
     */
    public boolean hasPurchase(@Nonnull String sku) {
        return getPurchase(sku) != null;
    }

    /**
     * @param sku   SKU of purchase to be found
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
}
