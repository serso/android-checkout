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

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.android.vending.billing.InAppBillingServiceImpl;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Purchase information as described <a href="http://developer.android.com/google/play/billing/billing_reference.html#purchase-data-table">here</a>
 */
@Immutable
public final class Purchase {

    // the item's product identifier. Every item has a product ID, which you must specify
    // in the application's product list on the Google Play Developer Console
    @Nonnull
    public final String sku;
    // a unique order identifier for the transaction. This identifier corresponds to the
    // Google Wallet Order ID
    @Nonnull
    public final String orderId;
    // the application package from which the purchase originated
    @Nonnull
    public final String packageName;
    // the time the product was purchased, in milliseconds since the epoch (Jan 1, 1970)
    public final long time;
    // the purchase state of the order
    @Nonnull
    public final State state;
    // a developer-specified string that contains supplemental information about an order.
    // You can specify a value for this field when you make a getBuyIntent request
    @Nonnull
    public final String payload;
    // a token that uniquely identifies a purchase for a given item and user pair
    @Nonnull
    public final String token;
    // Indicates whether the subscription renews automatically. If true, the subscription is active,
    // and will automatically renew on the next billing date. If false, indicates that the user has
    // canceled the subscription. The user has access to subscription content until the next billing
    // date and will lose access at that time unless they re-enable automatic renewal
    public final boolean autoRenewing;
    /**
     * Raw data returned from {@link InAppBillingServiceImpl#getPurchases}
     */
    @Nonnull
    public final String data;
    /**
     * Signature of {@link #data}
     */
    @Nonnull
    public final String signature;

    Purchase(@Nonnull String sku, @Nonnull String orderId, @Nonnull String packageName, long time, int state, @Nonnull String payload, @Nonnull String token, boolean autoRenewing, @Nonnull String data, @Nonnull String signature) {
        this.sku = sku;
        this.orderId = orderId;
        this.packageName = packageName;
        this.time = time;
        this.state = State.valueOf(state);
        this.payload = payload;
        this.token = token;
        this.autoRenewing = autoRenewing;
        this.signature = signature;
        this.data = data;
    }

    Purchase(@Nonnull String data, @Nonnull String signature) throws JSONException {
        final JSONObject json = new JSONObject(data);
        this.sku = json.getString("productId");
        this.orderId = json.optString("orderId");
        this.packageName = json.optString("packageName");
        this.time = json.getLong("purchaseTime");
        this.state = State.valueOf(json.optInt("purchaseState", 0));
        this.payload = json.optString("developerPayload");
        this.token = json.optString("token", json.optString("purchaseToken"));
        this.autoRenewing = json.optBoolean("autoRenewing");
        this.data = data;
        this.signature = signature;
    }

    @Nonnull
    static Purchase fromJson(@Nonnull String data, @Nonnull String signature) throws JSONException {
        return new Purchase(data, signature);
    }

    private static void tryPut(@Nonnull JSONObject json, @Nonnull String key, @Nonnull String name) throws JSONException {
        if (!TextUtils.isEmpty(name)) {
            json.put(key, name);
        }
    }

    /**
     * Same as {@link #toJson(boolean)} with {@code withSignature=false}.
     * Note that this method returns JSON which is not the same as original JSON returned by
     * Google.
     * Original JSON is
     * stored in {@link #data}, use it if you want to do a signature check (as {@link #signature}
     * signs {@link #data})
     *
     * @return JSON representation of this object
     */
    @Nonnull
    public String toJson() {
        return toJson(false);
    }

    /**
     * It might be useful to get a JSON of {@link Purchase} with signature information (Android
     * provides signature
     * separately). In that case use {@code withSignature=true}.
     *
     * @param withSignature if true then {@link #signature} will be included in the result
     * @return JSON representation of this object
     */
    @Nonnull
    public String toJson(boolean withSignature) {
        return toJsonObject(withSignature).toString();
    }

    @Nonnull
    JSONObject toJsonObject(boolean withSignature) {
        final JSONObject json = new JSONObject();
        try {
            json.put("productId", sku);
            tryPut(json, "orderId", orderId);
            tryPut(json, "packageName", packageName);
            json.put("purchaseTime", time);
            json.put("purchaseState", state.id);
            tryPut(json, "developerPayload", payload);
            tryPut(json, "token", token);
            if (autoRenewing) {
                json.put("autoRenewing", true);
            }
            if (withSignature) {
                tryPut(json, "signature", signature);
            }
        } catch (JSONException e) {
            // JSON exception should never happen in runtime
            throw new AssertionError(e);
        }
        return json;
    }

    @Override
    public String toString() {
        return "Purchase{" +
                "state=" + state +
                ", time=" + time +
                ", sku='" + sku + '\'' +
                '}';
    }

    public static enum State {
        PURCHASED(0),
        CANCELLED(1),
        REFUNDED(2),
        // billing v2 only
        EXPIRED(3);

        public final int id;

        State(int id) {
            this.id = id;
        }

        @Nonnull
        static State valueOf(int id) {
            switch (id) {
                case 0:
                    return PURCHASED;
                case 1:
                    return CANCELLED;
                case 2:
                    return REFUNDED;
                case 3:
                    return EXPIRED;
            }
            throw new IllegalArgumentException("Id=" + id + " is not supported");
        }
    }
}
