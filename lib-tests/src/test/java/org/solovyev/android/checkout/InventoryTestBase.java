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

import javax.annotation.Nonnull;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.solovyev.android.checkout.ProductTypes.IN_APP;
import static org.solovyev.android.checkout.ProductTypes.SUBSCRIPTION;
import static org.solovyev.android.checkout.Purchase.State.*;
import static org.solovyev.android.checkout.PurchaseTest.verifyPurchase;

@RunWith(CheckoutTestRunner.class)
public abstract class InventoryTestBase {

	@Nonnull
	protected Billing billing;

	@Nonnull
	private Checkout checkout;

	@Nonnull
	private Inventory inventory;

	@Before
	public void setUp() throws Exception {
		billing = newBilling();
		final Products products = Products.create()
				.add(IN_APP, asList("1", "2", "3", "4", "6"))
				.add(SUBSCRIPTION, asList("sub1", "sub2", "sub3", "sub4"));
		checkout = Checkout.forApplication(billing, products);
		inventory = newInventory(checkout);
	}

	@Nonnull
	protected Billing newBilling() {
		return Tests.newBilling();
	}

	@Nonnull
	protected abstract Inventory newInventory(@Nonnull Checkout checkout);

	@Test
	public void testShouldLoadPurchases() throws Exception {
		populatePurchases();

		final TestListener listener = new TestListener();
		checkout.start();
		inventory.load().whenLoaded(listener);

		waitWhileLoading(inventory);

		final boolean complete = shouldVerifyPurchaseCompletely();

		final Inventory.Product inApp = listener.products.get(IN_APP);
		final List<Purchase> actualInApps = inApp.getPurchases();
		assertEquals(4, actualInApps.size());

		verifyPurchase(actualInApps.get(0), 4, EXPIRED, complete, false);
		verifyPurchase(actualInApps.get(1), 3, REFUNDED, complete, false);
		verifyPurchase(actualInApps.get(2), 2, CANCELLED, complete, false);
		verifyPurchase(actualInApps.get(3), 1, PURCHASED, complete, false);

		final Inventory.Product sub = listener.products.get(SUBSCRIPTION);
		final List<Purchase> actualSubs = sub.getPurchases();
		assertEquals(4, actualSubs.size());

		verifyPurchase(actualSubs.get(0), 4, EXPIRED, complete, true);
		verifyPurchase(actualSubs.get(1), 3, REFUNDED, complete, true);
		verifyPurchase(actualSubs.get(2), 2, CANCELLED, complete, true);
		verifyPurchase(actualSubs.get(3), 1, PURCHASED, complete, true);

		assertSame(listener.products, inventory.getProducts());
	}

	protected void populatePurchases() throws Exception {
		final List<Purchase> expectedInApps = asList(
				Purchase.fromJson(PurchaseTest.newJson(1, PURCHASED), null),
				Purchase.fromJson(PurchaseTest.newJson(2, CANCELLED), null),
				Purchase.fromJson(PurchaseTest.newJson(3, REFUNDED), null),
				Purchase.fromJson(PurchaseTest.newJson(4, EXPIRED), null)
		);
		insertPurchases(IN_APP, expectedInApps);

		final List<Purchase> expectedSubs = asList(
				Purchase.fromJson(PurchaseTest.newJsonSubscription(1, PURCHASED), null),
				Purchase.fromJson(PurchaseTest.newJsonSubscription(2, CANCELLED), null),
				Purchase.fromJson(PurchaseTest.newJsonSubscription(3, REFUNDED), null),
				Purchase.fromJson(PurchaseTest.newJsonSubscription(4, EXPIRED), null)
		);
		insertPurchases(SUBSCRIPTION, expectedSubs);
	}

	protected abstract boolean shouldVerifyPurchaseCompletely();

	protected abstract void insertPurchases(@Nonnull String product, @Nonnull List<Purchase> purchases) throws Exception;

	@Test
	public void testShouldCallListenerWhenLoaded() throws Exception {
		final Inventory.Listener l1 = mock(Inventory.Listener.class);
		final Inventory.Listener l2 = mock(Inventory.Listener.class);
		final Inventory.Listener l3 = mock(Inventory.Listener.class);

		inventory.whenLoaded(l1);
		inventory.whenLoaded(l2);
		checkout.start();
		inventory.load();
		waitWhileLoading(inventory);
		inventory.whenLoaded(l3);

		verify(l1, times(1)).onLoaded(anyProducts());
		verify(l2, times(1)).onLoaded(anyProducts());
		verify(l3, times(1)).onLoaded(anyProducts());
	}

	@Nonnull
	private Inventory.Products anyProducts() {
		return any(Inventory.Products.class);
	}

	@Test
	public void testShouldNotAddSameListener() throws Exception {
		final Inventory.Listener l = mock(Inventory.Listener.class);

		inventory.whenLoaded(l);
		inventory.whenLoaded(l);
		inventory.whenLoaded(l);
		checkout.start();
		inventory.load();
		waitWhileLoading(inventory);

		verify(l, times(1)).onLoaded(anyProducts());
	}

	@Test
	public void testShouldRunSameListenerIfLoaded() throws Exception {
		final Inventory.Listener l = mock(Inventory.Listener.class);

		checkout.start();
		inventory.load();
		waitWhileLoading(inventory);
		inventory.whenLoaded(l);
		inventory.whenLoaded(l);
		inventory.whenLoaded(l);

		verify(l, times(3)).onLoaded(anyProducts());
	}

	@Test
	public void testListenerShouldBeCalledOnlyOnce() throws Exception {
		final Inventory.Listener l = mock(Inventory.Listener.class);
		checkout.start();
		inventory.whenLoaded(l);
		inventory.load();
		inventory.load();
		inventory.load();
		waitWhileLoading(inventory);
		inventory.load();
		inventory.load();
		verify(l, times(1)).onLoaded(anyProducts());
	}

	void waitWhileLoading(@Nonnull Inventory inventory) throws InterruptedException {
		int sleeping = 0;
		while (!isLoaded(inventory)) {
			Thread.sleep(50L);
			sleeping += 50L;
			if (sleeping > 1000L) {
				fail("Too long wait!");
			}
		}
	}

	protected abstract boolean isLoaded(Inventory inventory);

	static class TestListener implements Inventory.Listener {
		@Nonnull
		volatile Inventory.Products products;

		@Override
		public void onLoaded(@Nonnull Inventory.Products products) {
			this.products = products;
		}
	}
}