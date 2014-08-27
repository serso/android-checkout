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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import javax.annotation.Nonnull;
import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.solovyev.android.checkout.PurchaseTest.verifyPurchase;

@RunWith(RobolectricTestRunner.class)
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
		list.add(PurchaseTest.newJson(3, Purchase.State.PURCHASED));
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
		assertTrue(purchases.hasPurchaseInState("3", Purchase.State.PURCHASED));
		assertFalse(purchases.hasPurchaseInState("3", Purchase.State.CANCELLED));
		assertFalse(purchases.hasPurchaseInState("1", Purchase.State.CANCELLED));
		assertFalse(purchases.hasPurchaseInState("4", Purchase.State.CANCELLED));

		final Purchase purchase3 = purchases.getPurchaseInState("3", Purchase.State.PURCHASED);
		assertNotNull(purchase3);
		assertEquals("3", purchase3.sku);
	}
}