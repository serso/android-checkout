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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.text.TextUtils;

import javax.annotation.Nonnull;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SkuTest {
    static void verifySku(@Nonnull Sku sku, @Nonnull String id) {
        assertEquals(id, sku.id.code);
        assertEquals("price_" + id, sku.price);
        assertEquals("description_" + id, sku.description);
        assertEquals("title_" + id, sku.title);

        if(sku.id.isInApp()) {
            assertTrue(TextUtils.isEmpty(sku.subscriptionPeriod));
            assertTrue(TextUtils.isEmpty(sku.freeTrialPeriod));
            assertTrue(TextUtils.isEmpty(sku.introductoryPricePeriod));
        } else if (sku.id.isSubscription()) {
            assertTrue(sku.subscriptionPeriod.equals("subscriptionPeriod_" + id));
            assertTrue(sku.freeTrialPeriod.equals("freeTrialPeriod_" + id));
            assertTrue(sku.introductoryPricePeriod.equals("introductoryPricePeriod_" + id));
        } else if (!sku.id.product.equals("dummy")) {
            Assert.fail();
        }
    }

    @Nonnull
    static String newSubscriptionJson(@Nonnull String id) throws JSONException {
        return newSubscriptionJsonObject(id).toString();
    }

    @Nonnull
    static String newInAppJson(@Nonnull String id) throws JSONException {
        return newInAppJsonObject(id).toString();
    }

    @Nonnull
    static JSONObject newSubscriptionJsonObject(String id) throws JSONException {
        final JSONObject json = newInAppJsonObject(id);
        json.put("subscriptionPeriod", "subscriptionPeriod_" + id);
        json.put("freeTrialPeriod", "freeTrialPeriod_" + id);
        json.put("introductoryPricePeriod", "introductoryPricePeriod_" + id);

        return json;
    }

    @Nonnull
    static JSONObject newInAppJsonObject(String id) throws JSONException {
        final JSONObject json = new JSONObject();
        json.put("productId", id);
        json.put("price", "price_" + id);
        json.put("title", "title_" + id);
        json.put("description", "description_" + id);

        return json;
    }

    @Test
    public void testInAppSkuShouldBeCreatedFromJson() throws Exception {
        final Sku sku = Sku.fromJson(newInAppJson("1"), ProductTypes.IN_APP);

        verifySku(sku, "1");
    }

    @Test
    public void testSubscriptionSkuShouldBeCreatedFromJson() throws Exception {
        final Sku sku = Sku.fromJson(newSubscriptionJson("1"), ProductTypes.SUBSCRIPTION);

        verifySku(sku, "1");
    }

    @Test
    public void testShouldNotCreateIfNoId() throws Exception {
        final JSONObject json = newInAppJsonObject("2");
        json.remove("productId");
        try {
            Sku.fromJson(json.toString(), "test");
            fail();
        } catch (JSONException e) {
        }
    }

    @Test
    public void testShouldCreateWithoutDescription() throws Exception {
        final JSONObject json = newInAppJsonObject("3");
        json.remove("description");
        final Sku sku = Sku.fromJson(json.toString(), "test");
        assertEquals("3", sku.id.code);
        assertEquals("price_3", sku.price);
        assertEquals("", sku.description);
        assertEquals("title_3", sku.title);
    }

    @Test
    public void testShouldHaveNotValidPriceIfNoDetailedDataAvailable() throws Exception {
        final Sku sku = Sku.fromJson(newInAppJson("1"), "test");

        assertFalse(sku.detailedPrice.isValid());
    }

    @Test
    public void testShouldHaveNotValidIntroductoryPriceIfNoDetailedDataAvailable() throws Exception {
        final Sku sku = Sku.fromJson(newSubscriptionJson("1"), "test");

        assertNotNull(sku.detailedIntroductoryPrice);
        assertFalse(sku.detailedIntroductoryPrice.isValid());
    }

    @Test
    public void testShouldHaveDetailedPrice() throws Exception {
        testDetailedPrice(Long.MAX_VALUE, "USD");
        testDetailedPrice(1, "RUB");
        testDetailedPrice(111, "руб");
    }

    @Test
    public void testShouldHaveIntroductoryPrice() throws Exception {
        testIntroductoryPrice(Long.MAX_VALUE, "USD");
        testIntroductoryPrice(1, "RUB");
        testIntroductoryPrice(111, "руб");
    }

    private void testDetailedPrice(long amount, @Nonnull String currency) throws JSONException {
        final JSONObject json = newInAppJsonObject("1");
        json.put("price_amount_micros", amount);
        json.put("price_currency_code", currency);
        final Sku sku = Sku.fromJson(json.toString(), "test");

        assertTrue(sku.detailedPrice.isValid());
        assertEquals(currency, sku.detailedPrice.currency);
        assertEquals(amount, sku.detailedPrice.amount);
    }

    private void testIntroductoryPrice(long amount, @Nonnull String currency) throws JSONException {
        final JSONObject json = newSubscriptionJsonObject("1");
        json.put("introductoryPriceAmountMicros", amount);
        json.put("price_currency_code", currency);
        final Sku sku = Sku.fromJson(json.toString(), "test");

        assertNotNull(sku.detailedIntroductoryPrice);
        assertTrue(sku.detailedIntroductoryPrice.isValid());
        assertEquals(currency, sku.detailedIntroductoryPrice.currency);
        assertEquals(amount, sku.detailedIntroductoryPrice.amount);
    }

    @Test
    public void testShouldStripSimpleAppNameFromTitle() throws Exception {
        final JSONObject json = newInAppJsonObject("1");
        json.put("title", "Test #1 (Test App name)");
        final Sku sku = Sku.fromJson(json.toString(), "test");

        final String displayTitle = sku.getDisplayTitle();

        assertEquals("Test #1", displayTitle);
    }

    @Test
    public void testShouldStripSimpleAppNameFromTitleWithBrackets() throws Exception {
        final JSONObject json = newInAppJsonObject("1");
        json.put("title", "Test #1 (test) (Test App name)");
        final Sku sku = Sku.fromJson(json.toString(), "test");

        final String displayTitle = sku.getDisplayTitle();

        assertEquals("Test #1 (test)", displayTitle);
    }

    @Test
    public void testShouldStripSimpleAppNameWithBracketsFromTitleWithBrackets() throws Exception {
        final JSONObject json = newInAppJsonObject("1");
        json.put("title", "Test #1 (test) (Test App name (test))");
        final Sku sku = Sku.fromJson(json.toString(), "test");

        final String displayTitle = sku.getDisplayTitle();

        assertEquals("Test #1 (test)", displayTitle);
    }

    @Test
    public void testShouldReadIntroductoryPrice() throws Exception {
        final JSONObject json = newInAppJsonObject("1");
        json.put("introductoryPrice", "1 000 000EUR");

        final Sku sku = Sku.fromJson(json.toString(), "test");

        assertEquals("1 000 000EUR", sku.introductoryPrice);
    }

    @Test
    public void testShouldNotPutEmptyFieldsInJson() throws Exception {
        final Sku sku = Sku.fromJson(newInAppJsonObject("1").toString(), "test");
        final JSONObject json = sku.toJsonObject();

        assertTrue(!json.has("subscriptionPeriod"));
        assertTrue(!json.has("freeTrialPeriod"));
        assertTrue(!json.has("introductoryPricePeriod"));
        assertTrue(!json.has("introductoryPriceAmountMicros"));
    }

    @Test
    public void testShouldReadIntroductoryPriceCycles() throws Exception {
        final JSONObject json = newInAppJsonObject("1");
        json.put("introductoryPriceCycles", 42);
        final Sku sku = Sku.fromJson(json.toString(), "test");

        assertEquals(sku.introductoryPriceCycles, 42);
    }
}