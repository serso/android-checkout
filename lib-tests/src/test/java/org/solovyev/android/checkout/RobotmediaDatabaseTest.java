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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.solovyev.android.checkout.ProductTypes.IN_APP;
import static org.solovyev.android.checkout.RobotmediaDatabase.makeInClause;
import static org.solovyev.android.checkout.Tests.sameThreadExecutor;

@RunWith(RobolectricTestRunner.class)
public class RobotmediaDatabaseTest {

	@Nonnull
	private Checkout checkout;

	@Nonnull
	private BillingDB db;

	@Before
	public void setUp() throws Exception {
		final Billing billing = Tests.newBilling();
		billing.setMainThread(sameThreadExecutor());
		final Products products = Products.create().add(IN_APP, asList("sku_0", "sku_1", "sku_2", "sku_3", "sku_4", "sku_6"));
		checkout = Checkout.forApplication(billing, products);
		db = new BillingDB(Robolectric.application);
	}

	@Test
	public void testShouldCreateEmptyProductsIfError() throws Exception {
		db.close();
		Robolectric.application.deleteDatabase(RobotmediaDatabase.NAME);

		final RobotmediaInventory inventory = new RobotmediaInventory(checkout, sameThreadExecutor());
		final CountDownListener l = new CountDownListener();
		inventory.load().whenLoaded(l);

		final Inventory.Products products = l.waitProducts();
		final Inventory.Product product = products.get(IN_APP);
		assertNotNull(product);
		assertTrue(product.getPurchases().isEmpty());
	}

	@Test
	public void testShouldReadTransactions() throws Exception {
		db.insert(newTransaction(0));
		db.insert(newTransaction(1));
		db.insert(newTransaction(2));
		db.insert(newTransaction(3));
		db.insert(newTransaction(4));
		db.insert(newTransaction(5));

		final RobotmediaInventory inventory = new RobotmediaInventory(checkout, sameThreadExecutor());
		final CountDownListener l = new CountDownListener();
		inventory.load().whenLoaded(l);

		final Inventory.Products products = l.waitProducts();
		final Inventory.Product product = products.get(IN_APP);
		assertTrue(product.supported);
		final List<Purchase> purchases = product.getPurchases();
		assertEquals(5, purchases.size());
		verifyPurchase(purchases, 0);
		verifyPurchase(purchases, 1);
		verifyPurchase(purchases, 2);
		verifyPurchase(purchases, 3);
		verifyPurchase(purchases, 4);
	}

	@Test
	public void testShouldReadEmptyList() throws Exception {
		final RobotmediaInventory inventory = new RobotmediaInventory(checkout, sameThreadExecutor());
		final CountDownListener l = new CountDownListener();
		inventory.load().whenLoaded(l);

		final Inventory.Products products = l.waitProducts();
		final Inventory.Product product = products.get(IN_APP);
		assertTrue(product.supported);
		final List<Purchase> purchases = product.getPurchases();
		assertEquals(0, purchases.size());
	}

	@Test
	public void testShouldCreateValidInClause() throws Exception {
		assertEquals("(?)", makeInClause(1));
		assertEquals("(?,?)", makeInClause(2));
		assertEquals("(?,?,?)", makeInClause(3));
		assertEquals("(?,?,?,?)", makeInClause(4));

		try {
			makeInClause(0);
			fail();
		} catch (RuntimeException e) {
			// ok
		}

	}

	private void verifyPurchase(@Nonnull List<Purchase> purchases, int id) {
		final Purchase purchase = findPurchase(purchases, id);
		verifyPurchase(purchase, id);
	}

	private void verifyPurchase(@Nonnull Purchase purchase, int id) {
		final Transaction transaction = newTransaction(id);
		assertEquals(transaction.orderId, purchase.orderId);
		assertEquals(transaction.productId, purchase.sku);
		assertEquals(transaction.developerPayload, purchase.payload);
		assertEquals(transaction.purchaseTime, purchase.time);
		assertEquals(transaction.purchaseState.ordinal(), purchase.state.id);
	}

	@Nonnull
	private Purchase findPurchase(@Nonnull List<Purchase> purchases, int id) {
		for (Purchase purchase : purchases) {
			if(purchase.orderId.equals(String.valueOf(id))) {
				return purchase;
			}
		}
		throw new AssertionError("Purchase with id " + id + " was not found");
	}

	@Nonnull
	private Transaction newTransaction(int id) {
		final Transaction t = new Transaction();
		t.orderId = String.valueOf(id);
		t.productId = "sku_" + id;
		t.purchaseTime = id;
		final Transaction.PurchaseState[] states = Transaction.PurchaseState.values();
		t.purchaseState = states[id % states.length];
		t.developerPayload = id % 2 == 0 ? "payload_" + id : null;
		return t;
	}

	private static class CountDownListener implements Inventory.Listener {
		@Nonnull
		private final CountDownLatch latch = new CountDownLatch(1);
		private Inventory.Products products;

		@Override
		public void onLoaded(@Nonnull Inventory.Products products) {
			this.products = products;
			latch.countDown();
		}

		@Nonnull
		Inventory.Products waitProducts() throws InterruptedException {
			if (latch.await(5, TimeUnit.SECONDS)) {
				return products;
			}
			throw new AssertionError();
		}
	}
}