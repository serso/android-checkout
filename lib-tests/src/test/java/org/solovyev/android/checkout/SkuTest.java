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
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
public class SkuTest {
	@Test
	public void testShouldBeCreatedFromJson() throws Exception {
		final Sku sku = Sku.fromJson(newJson("1"), "test");

		verifySku(sku, "1");
	}

	static void verifySku(@Nonnull Sku sku, @Nonnull String id) {
		assertEquals(id, sku.id);
		assertEquals("price_" + id, sku.price);
		assertEquals("description_" + id, sku.description);
		assertEquals("title_" + id, sku.title);
	}

	@Test
	public void testShouldNotCreateIfNoId() throws Exception {
		final JSONObject json = newJsonObject("2");
		json.remove("productId");
		try {
			Sku.fromJson(json.toString(), "test");
			fail();
		} catch (JSONException e) {
		}
	}

	@Test
	public void testShouldCreateWithoutDescription() throws Exception {
		final JSONObject json = newJsonObject("3");
		json.remove("description");
		final Sku sku = Sku.fromJson(json.toString(), "test");
		assertEquals("3", sku.id);
		assertEquals("price_3", sku.price);
		assertEquals("", sku.description);
		assertEquals("title_3", sku.title);
	}

	@Nonnull
	static String newJson(@Nonnull String id) throws JSONException {
		return newJsonObject(id).toString();
	}

	@Nonnull
	static JSONObject newJsonObject(String id) throws JSONException {
		final JSONObject json = new JSONObject();
		json.put("productId", id);
		json.put("price", "price_" + id);
		json.put("title", "title_" + id);
		json.put("description", "description_" + id);
		return json;
	}
}