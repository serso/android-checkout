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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.skyscreamer.jsonassert.JSONAssert;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nonnull;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.solovyev.android.checkout.Purchase.State.CANCELLED;
import static org.solovyev.android.checkout.Purchase.State.EXPIRED;
import static org.solovyev.android.checkout.Purchase.State.PURCHASED;
import static org.solovyev.android.checkout.Purchase.State.REFUNDED;
import static org.solovyev.android.checkout.PurchaseTest.verifyPurchase;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PurchasesTest {

    @Test
    public void testShouldReadEmptyList() throws Exception {
        final Bundle bundle = new Bundle();
        bundle.putStringArrayList(Purchases.BUNDLE_DATA_LIST, new ArrayList<String>());
        final Purchases purchases = Purchases.fromBundle(bundle, "test");
        assertTrue(purchases.list.isEmpty());
    }

    @Test
    public void testShouldReadNullList() throws Exception {
        final Bundle bundle = new Bundle();
        final Purchases purchases = Purchases.fromBundle(bundle, "test");
        assertTrue(purchases.list.isEmpty());
    }

    @Test
    public void testShouldReadList() throws Exception {
        final Purchases purchases = Purchases.fromBundle(prepareBundle(), "test");

        assertEquals(3, purchases.list.size());
        verifyPurchase(purchases.list.get(0), 1, Purchase.State.REFUNDED);
        verifyPurchase(purchases.list.get(1), 2, Purchase.State.CANCELLED);
        verifyPurchase(purchases.list.get(2), 3, Purchase.State.PURCHASED);
    }

    @Nonnull
    private Bundle prepareBundle() throws JSONException {
        final ArrayList<String> list = new ArrayList<String>();
        list.add(PurchaseTest.newJson(1, Purchase.State.REFUNDED));
        list.add(PurchaseTest.newJson(2, Purchase.State.CANCELLED));
        list.add(PurchaseTest.newJson(3, PURCHASED));
        final Bundle bundle = new Bundle();
        bundle.putStringArrayList(Purchases.BUNDLE_DATA_LIST, list);
        return bundle;
    }

    @Test
    public void testShouldReadListWithSignatures() throws Exception {
        final Bundle bundle = prepareBundle();
        final ArrayList<String> signatures = new ArrayList<String>();
        signatures.add("sig_1");
        signatures.add("sig_2");
        signatures.add("sig_3");
        bundle.putStringArrayList(Purchases.BUNDLE_SIGNATURE_LIST, signatures);

        final Purchases purchases = Purchases.fromBundle(bundle, "test");

        assertEquals(3, purchases.list.size());
        assertEquals("sig_1", purchases.list.get(0).signature);
        assertEquals("sig_2", purchases.list.get(1).signature);
        assertEquals("sig_3", purchases.list.get(2).signature);
    }

    @Test
    public void testShouldReadContinuationToken() throws Exception {
        final Bundle bundle = prepareBundle();
        bundle.putString(Purchases.BUNDLE_CONTINUATION_TOKEN, "token");
        final Purchases purchases = Purchases.fromBundle(bundle, "test");

        assertEquals("token", purchases.continuationToken);
    }

    @Test
    public void testShouldFindPurchase() throws Exception {
        final Purchases purchases = Purchases.fromBundle(prepareBundle(), "test");

        assertTrue(purchases.hasPurchase("1"));
        assertTrue(purchases.hasPurchase("2"));
        assertTrue(purchases.hasPurchase("3"));
        assertFalse(purchases.hasPurchase("4"));

        final Purchase purchase3 = purchases.getPurchase("3");
        assertNotNull(purchase3);
        assertEquals("3", purchase3.sku);

        final Purchase purchase1 = purchases.getPurchase("1");
        assertNotNull(purchase1);
        assertEquals("1", purchase1.sku);

        final Purchase noPurchase = purchases.getPurchase("4");
        assertNull(noPurchase);
    }

    @Test
    public void testShouldFindPurchaseInState() throws Exception {
        final Purchases purchases = Purchases.fromBundle(prepareBundle(), "test");

        assertTrue(purchases.hasPurchaseInState("1", Purchase.State.REFUNDED));
        assertTrue(purchases.hasPurchaseInState("2", Purchase.State.CANCELLED));
        assertTrue(purchases.hasPurchaseInState("3", PURCHASED));
        assertFalse(purchases.hasPurchaseInState("3", Purchase.State.CANCELLED));
        assertFalse(purchases.hasPurchaseInState("1", Purchase.State.CANCELLED));
        assertFalse(purchases.hasPurchaseInState("4", Purchase.State.CANCELLED));

        final Purchase purchase3 = purchases.getPurchaseInState("3", PURCHASED);
        assertNotNull(purchase3);
        assertEquals("3", purchase3.sku);
    }

    @Test
    public void testShouldIncludeSignaturesInJson() throws Exception {
        final Purchases purchases = new Purchases("test", asList(newPurchase(0), newPurchase(1), newPurchase(2)), null);

        final JSONObject json = purchases.toJsonObject(true);
        final JSONArray jsonArray = json.getJSONArray("list");

        assertEquals("signature0", jsonArray.getJSONObject(0).getString("signature"));
        assertEquals("signature1", jsonArray.getJSONObject(1).getString("signature"));
        assertEquals("signature2", jsonArray.getJSONObject(2).getString("signature"));
    }

    @Test
    public void testShouldReadAutoRenewing() throws Exception {
        JSONObject json = PurchaseTest.newJsonObject(0, PURCHASED);
        json.put("autoRenewing", true);

        Purchase purchase = Purchase.fromJson(json.toString(), "signature");
        Assert.assertTrue(purchase.autoRenewing);

        json.put("autoRenewing", false);
        purchase = Purchase.fromJson(json.toString(), "signature");
        Assert.assertFalse(purchase.autoRenewing);

        json.remove("autoRenewing");
        purchase = Purchase.fromJson(json.toString(), "signature");
        Assert.assertFalse(purchase.autoRenewing);
    }

    @Nonnull
    private Purchase newPurchase(long id) throws JSONException {
        return Purchase.fromJson(PurchaseTest.newJson(id, PURCHASED), "signature" + id);
    }

    @Test
    public void testShouldNotIncludeSignaturesInJson() throws Exception {
        final Purchases purchases = new Purchases("test", asList(newPurchase(0), newPurchase(1), newPurchase(2)), null);

        final JSONObject json = purchases.toJsonObject(false);
        final JSONArray jsonArray = json.getJSONArray("list");

        assertFalse(jsonArray.getJSONObject(0).has("signature"));
        assertFalse(jsonArray.getJSONObject(1).has("signature"));
        assertFalse(jsonArray.getJSONObject(2).has("signature"));
    }

    @Test
    public void testShouldJson() throws Exception {
        final JSONObject purchaseJson2 = PurchaseTest.newJsonObject(2, PURCHASED);
        purchaseJson2.put("autoRenewing", true);
        final Purchase purchase2 = Purchase.fromJson(purchaseJson2.toString(), "signature" + 2);
        final Purchases purchases = new Purchases("test", asList(newPurchase(0), newPurchase(1), purchase2), null);

        final JSONObject json = purchases.toJsonObject(false);
        final JSONArray jsonArray = json.getJSONArray("list");

        assertEquals("test", json.getString("product"));
        for (int i = 0; i < jsonArray.length(); i++) {
            final JSONObject o = jsonArray.getJSONObject(i);
            verifyPurchase(Purchase.fromJson(o.toString(), null), i, PURCHASED);
        }
        JSONAssert.assertEquals("{" +
                        "\"product\":\"test\"," +
                        "\"list\":" +
                        "[" +
                        "{\"developerPayload\":\"developerPayload_0\",\"packageName\":\"packageName_0\",\"token\":\"purchaseToken_0\",\"purchaseState\":0,\"orderId\":\"orderId_0\",\"purchaseTime\":0,\"productId\":\"0\"}," +
                        "{\"developerPayload\":\"developerPayload_1\",\"packageName\":\"packageName_1\",\"token\":\"purchaseToken_1\",\"purchaseState\":0,\"orderId\":\"orderId_1\",\"purchaseTime\":1,\"productId\":\"1\"}," +
                        "{\"developerPayload\":\"developerPayload_2\",\"autoRenewing\":true,\"packageName\":\"packageName_2\",\"token\":\"purchaseToken_2\",\"purchaseState\":0,\"orderId\":\"orderId_2\",\"purchaseTime\":2,\"productId\":\"2\"}" +
                        "]" +
                        "}",
                json, true);
    }

    @Test
    public void testShouldJsonEmptyList() throws Exception {
        final Purchases purchases = new Purchases("test", Collections.<Purchase>emptyList(), null);
        final JSONObject json = purchases.toJsonObject(true);
        final JSONArray jsonArray = json.getJSONArray("list");
        assertEquals(0, jsonArray.length());
    }

    @Test
    public void testShouldNeutralizePurchasesWithSameSkus() throws Exception {
        List<Purchase> purchases = Purchases.neutralize(asList(newPurchase("test", 100, PURCHASED),
                newPurchase("test", 120, CANCELLED),
                newPurchase("test", 135, PURCHASED)));

        assertEquals(1, purchases.size());
        final Purchase purchase = purchases.get(0);
        assertEquals(135, purchase.time);

        purchases = Purchases.neutralize(asList(newPurchase("test", 100, PURCHASED), newPurchase("test", 120, CANCELLED)));
        assertEquals(0, purchases.size());

        purchases = Purchases.neutralize(asList(newPurchase("test", 100, PURCHASED), newPurchase("test", 120, CANCELLED),
                newPurchase("test", 135, PURCHASED), newPurchase("test", 145, CANCELLED)));
        assertEquals(0, purchases.size());
    }

    @Test
    public void testShouldNotNeutralizePurchasesWithDifferentSkus() throws Exception {
        List<Purchase> purchases = Purchases.neutralize(asList(newPurchase("0", 100, PURCHASED),
                newPurchase("1", 120, CANCELLED),
                newPurchase("2", 122, EXPIRED),
                newPurchase("3", 123, REFUNDED),
                newPurchase("4", 135, PURCHASED),
                newPurchase("5", 137, CANCELLED),
                newPurchase("6", 138, REFUNDED),
                newPurchase("7", 140, EXPIRED),
                newPurchase("8", 141, PURCHASED)
        ));

        assertEquals(9, purchases.size());
    }

    @Test
    public void testShouldRemovePurchaseDuplicates() throws Exception {
        List<Purchase> purchases = Purchases.neutralize(asList(newPurchase("test", 1, PURCHASED),
                newPurchase("test", 2, PURCHASED),
                newPurchase("test", 3, PURCHASED)));

        assertEquals(1, purchases.size());
        Purchase purchase = purchases.get(0);
        assertEquals(3, purchase.time);

        purchases = Purchases.neutralize(asList(newPurchase("test", 1, PURCHASED),
                newPurchase("test", 2, PURCHASED)));

        assertEquals(1, purchases.size());
        purchase = purchases.get(0);
        assertEquals(2, purchase.time);
    }

    @Test
    public void testShouldNeutralizeDanglingPurchases() throws Exception {
        List<Purchase> purchases = Purchases.neutralize(asList(newPurchase("test", 1, EXPIRED),
                newPurchase("test", 2, PURCHASED)));

        assertEquals(1, purchases.size());
        final Purchase purchase = purchases.get(0);
        assertEquals(2, purchase.time);
    }

    @Test
    public void testSinglePurchaseCantBeNeutralized() throws Exception {
        List<Purchase> purchases = Purchases.neutralize(asList(newPurchase("test", 1, EXPIRED)));
        assertEquals(1, purchases.size());
        Purchase purchase = purchases.get(0);
        assertEquals(1, purchase.time);

        purchases = Purchases.neutralize(asList(newPurchase("test", 1, REFUNDED)));
        assertEquals(1, purchases.size());
        purchase = purchases.get(0);
        assertEquals(1, purchase.time);
    }

    @Test
    public void testShouldNotContainDuplicates() throws Exception {
        final List<Purchase> purchases = new ArrayList<Purchase>(1000);
        final Random r = new Random(currentTimeMillis());
        for (int i = 0; i < 1000; i++) {
            purchases.add(newPurchase(String.valueOf(i % 100), r.nextLong(), Purchase.State.valueOf(r.nextInt(4))));
        }

        final List<Purchase> actual = Purchases.neutralize(purchases);
        final Map<String, Integer> counters = new HashMap<String, Integer>();
        for (Purchase purchase : actual) {
            final Integer counter = counters.get(purchase.sku);
            assertNull("Several purchases with same SKU are in the neutralized list", counter);
            counters.put(purchase.sku, 1);
        }
    }

    @Nonnull
    private Purchase newPurchase(@Nonnull String sku, long time, @Nonnull Purchase.State state) {
        return new Purchase(sku, "", "", time, state.id, "", "", false, "", "");
    }
}