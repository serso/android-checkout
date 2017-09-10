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
import org.robolectric.annotation.Config;
import org.skyscreamer.jsonassert.JSONAssert;

import javax.annotation.Nonnull;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PurchaseTest {

    static void verifyPurchase(@Nonnull Purchase purchase, long id, Purchase.State state) {
        verifyPurchase(purchase, id, state, true, false);
    }

    static void verifyPurchase(@Nonnull Purchase purchase, long id, Purchase.State state, boolean complete, boolean sub) {
        if (sub) {
            assertEquals("sub" + String.valueOf(id), purchase.sku);
        } else {
            assertEquals(String.valueOf(id), purchase.sku);
        }
        if (sub) {
            assertEquals("suborderId_" + id, purchase.orderId);
        } else {
            assertEquals("orderId_" + id, purchase.orderId);
        }
        if (complete) {
            assertEquals("packageName_" + id, purchase.packageName);
        }
        assertEquals(id, purchase.time);
        assertEquals(state, purchase.state);
        assertEquals("developerPayload_" + id, purchase.payload);
        if (complete) {
            assertEquals("purchaseToken_" + id, purchase.token);
        }
    }

    @Nonnull
    static String newJson(long id, Purchase.State state) throws JSONException {
        return newJsonObject(id, state).toString();
    }

    @Nonnull
    static String newJsonSubscription(long id, Purchase.State state) throws JSONException {
        return newJsonObjectSubscription(id, state).toString();
    }

    @Nonnull
    static JSONObject newJsonObjectSubscription(long id, Purchase.State state) throws JSONException {
        JSONObject json = newJsonObject(id, state);
        json.put("productId", "sub" + json.getString("productId"));
        json.put("orderId", "sub" + json.getString("orderId"));
        return json;
    }

    @Nonnull
    static JSONObject newJsonObject(long id, Purchase.State state) throws JSONException {
        return newJsonObject(id, state, false);
    }

    @Nonnull
    static JSONObject newJsonObject(long id, Purchase.State state, boolean lite) throws JSONException {
        final JSONObject json = new JSONObject();
        json.put("productId", String.valueOf(id));
        json.put("purchaseTime", id);
        json.put("developerPayload", "developerPayload_" + id);
        json.put("purchaseToken", "purchaseToken_" + id);
        if (!lite) {
            json.put("orderId", "orderId_" + id);
            json.put("packageName", "packageName_" + id);
            json.put("purchaseState", state.id);
        }
        return json;
    }

    @Test
    public void testShouldBeCreatedFromJson() throws Exception {
        final Purchase purchase = Purchase.fromJson(newJson(2, Purchase.State.REFUNDED), "signature");
        verifyPurchase(purchase, 2, Purchase.State.REFUNDED);
    }

    @Test
    public void testToJsonShouldReturnCorrectJson() throws Exception {
        final Purchase purchase = Purchase.fromJson(newJson(2, Purchase.State.REFUNDED), "signature");
        final String json = purchase.toJson();
        final Purchase actual = Purchase.fromJson(json, "signature");

        verifyPurchase(actual, 2, Purchase.State.REFUNDED);
    }

    @Test
    public void testToJsonShouldReturnCorrectJsonForPartiallyEmptyPurchase() throws Exception {
        final JSONObject expected = newJsonObject(3, Purchase.State.CANCELLED);
        expected.remove("orderId");
        expected.remove("packageName");
        expected.remove("purchaseState");
        expected.remove("developerPayload");
        expected.remove("purchaseToken");
        final Purchase purchase = Purchase.fromJson(expected.toString(), "signature");

        final JSONObject actual = new JSONObject(purchase.toJson());
        assertTrue(actual.has("purchaseState"));
        actual.remove("purchaseState");

        JSONAssert.assertEquals(expected, actual, false);
    }

    @Test
    public void testJsonShouldNotContainSignature() throws Exception {
        final Purchase purchase = Purchase.fromJson(newJson(2, Purchase.State.REFUNDED), "signature");
        final String json = purchase.toJson();
        final JSONObject jsonObject = new JSONObject(json);
        assertFalse(jsonObject.has("signature"));
    }

    @Test
    public void testJsonShouldContainSignature() throws Exception {
        final Purchase purchase = Purchase.fromJson(newJson(2, Purchase.State.REFUNDED), "signature");
        final String json = purchase.toJson(true);
        final JSONObject jsonObject = new JSONObject(json);
        assertTrue(jsonObject.has("signature"));
        assertEquals("signature", jsonObject.getString("signature"));
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
}