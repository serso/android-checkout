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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import javax.annotation.Nonnull;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
public class PurchaseTest {

	@Test
	public void testShouldBeCreatedFromJson() throws Exception {
		final Purchase purchase = Purchase.fromJson(newJson(2, Purchase.State.REFUNDED), "signature");
		verifyPurchase(purchase, 2, Purchase.State.REFUNDED);
	}

	@Test
	public void testShouldNotBeCreatedIfProductIdIsMissing() throws Exception {
		final JSONObject json = newJsonObject(4, Purchase.State.PURCHASED);
		json.remove("productId");

		try {
			Purchase.fromJson(json.toString(), "signature");
			fail();
		} catch (JSONException e) {
		}
	}

	@Test
	public void testShouldBeCreatedWithoutUnnecessaryProperties() throws Exception {
		final JSONObject json = newJsonObject(3, Purchase.State.CANCELLED);
		json.remove("orderId");
		json.remove("packageName");
		json.remove("purchaseState");
		json.remove("developerPayload");
		json.remove("purchaseToken");

		final Purchase purchase = Purchase.fromJson(json.toString(), "signature");

		assertNotNull(purchase);
	}

	private void verifyPurchase(@Nonnull Purchase purchase, long id, Purchase.State state) {
		assertEquals(String.valueOf(id), purchase.sku);
		assertEquals("orderId_" + id, purchase.orderId);
		assertEquals("packageName_" + id, purchase.packageName);
		assertEquals(id, purchase.time);
		assertEquals(state, purchase.state);
		assertEquals("developerPayload_" + id, purchase.payload);
		assertEquals("purchaseToken_" + id, purchase.token);
	}

	@Nonnull
	static String newJson(long id, Purchase.State state) throws JSONException {
		return newJsonObject(id, state).toString();
	}

	@Nonnull
	static JSONObject newJsonObject(long id, Purchase.State state) throws JSONException {
		final JSONObject json = new JSONObject();
		json.put("productId", id);
		json.put("orderId", "orderId_" + id);
		json.put("packageName", "packageName_" + id);
		json.put("purchaseTime", id);
		json.put("purchaseState", state.id);
		json.put("developerPayload", "developerPayload_" + id);
		json.put("purchaseToken", "purchaseToken_" + id);
		return json;
	}
}