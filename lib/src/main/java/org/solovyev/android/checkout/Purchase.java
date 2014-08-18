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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
	@Nonnull
	public final String signature;

	Purchase(@Nonnull String sku, @Nonnull String orderId, @Nonnull String packageName, long time, int state, @Nonnull String payload, @Nonnull String token, @Nonnull String signature) {
		this.sku = sku;
		this.orderId = orderId;
		this.packageName = packageName;
		this.time = time;
		this.state = State.valueOf(state);
		this.payload = payload;
		this.token = token;
		this.signature = signature;
	}

	@Nonnull
	static Purchase fromData(@Nonnull String data, @Nullable String signature) throws JSONException {
		final JSONObject json = new JSONObject(data);
		final String sku = json.optString("productId");
		final String orderId = json.optString("orderId");
		final String packageName = json.optString("packageName");
		final long purchaseTime = json.getLong("purchaseTime");
		final int purchaseState = json.optInt("purchaseState", 0);
		final String payload = json.optString("developerPayload");
		final String token = json.optString("token", json.optString("purchaseToken"));
		return new Purchase(sku, orderId, packageName, purchaseTime, purchaseState, payload, token, signature == null ? "" : signature);
	}

	@Override
	public String toString() {
		return "Purchase{" +
				"sku='" + sku + '\'' +
				'}';
	}

	public static enum State {
		PURCHASED,
		CANCELLED,
		REFUNDED;

		@Nonnull
		static State valueOf(int id) {
			switch (id) {
				case 0:
					return PURCHASED;
				case 1:
					return CANCELLED;
				case 2:
					return REFUNDED;
			}
			throw new IllegalArgumentException("Id=" + id + " is not supported");
		}
	}
}
