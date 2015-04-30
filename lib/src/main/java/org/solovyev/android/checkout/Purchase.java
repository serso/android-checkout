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

import android.text.TextUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

@Immutable
public final class Purchase {

	@Nonnull
	public final String sku;
	@Nonnull
	public final String orderId;
	@Nonnull
	public final String packageName;
	public final long time;
	@Nonnull
	public final State state;
	@Nonnull
	public final String payload;
	@Nonnull
	public final String token;
    public final boolean autoRenewing;

	/**
	 * Raw data returned from {@link com.android.vending.billing.IInAppBillingService#getPurchases}
	 */
	@Nonnull
	public final String data;
	/**
	 * Signature of {@link #data}
	 */
	@Nonnull
	public final String signature;

	Purchase(@Nonnull String sku, @Nonnull String orderId, @Nonnull String packageName, long time, int state, @Nonnull String payload, @Nonnull String token, @Nonnull String data, @Nonnull String signature, @Nonnull boolean autoRenew) {
		this.sku = sku;
		this.orderId = orderId;
		this.packageName = packageName;
		this.time = time;
		this.state = State.valueOf(state);
		this.payload = payload;
		this.token = token;
		this.signature = signature;
		this.data = data;
        this.autoRenewing = autoRenew;
	}

	@Nonnull
	static Purchase fromJson(@Nonnull String data, @Nonnull String signature) throws JSONException {
		final JSONObject json = new JSONObject(data);
		final String sku = json.getString("productId");
		final String orderId = json.optString("orderId");
		final String packageName = json.optString("packageName");
		final long purchaseTime = json.getLong("purchaseTime");
		final int purchaseState = json.optInt("purchaseState", 0);
		final String payload = json.optString("developerPayload");
		final String token = json.optString("token", json.optString("purchaseToken"));
        final boolean autoRenew = json.optBoolean("autoRenewing", false);
		return new Purchase(sku, orderId, packageName, purchaseTime, purchaseState, payload, token, data, signature, autoRenew);
	}

	/**
	 * Same as {@link #toJson(boolean)} with {@code withSignature=false}.
	 * Note that this method returns JSON which is not the same as original JSON returned by Google. Original JSON is
	 * stored in {@link #data}, use it if you want to do a signature check (as {@link #signature} signs {@link #data})
	 * @return JSON representation of this object
	 */
	@Nonnull
	public String toJson() {
		return toJson(false);
	}

	/**
	 * It might be useful to get a JSON of {@link Purchase} with signature information (Android provides signature
	 * separately). In that case use {@code withSignature=true}.
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
            json.put("autoRenewing", autoRenewing);
			tryPut(json, "developerPayload", payload);
			tryPut(json, "token", token);
			if (withSignature) {
				tryPut(json, "signature", signature);
			}
		} catch (JSONException e) {
			// JSON exception should never happen in runtime
			throw new AssertionError(e);
		}
		return json;
	}

	private static void tryPut(JSONObject json, @Nonnull String key, @Nonnull String name) throws JSONException {
		if (!TextUtils.isEmpty(name)) {
			json.put(key, name);
		}
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
