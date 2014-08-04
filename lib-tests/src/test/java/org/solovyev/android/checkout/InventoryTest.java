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

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(CheckoutTestRunner.class)
public class InventoryTest {

	@Nonnull
	private Inventory inventory;

	@Before
	public void setUp() throws Exception {
		final Billing billing = Tests.newBilling();
		final Products products = Products.create().add(ProductTypes.IN_APP, asList("test_01, test_02"));
		final Checkout checkout = Checkout.forApplication(billing, products);
		checkout.start();
		inventory = new Inventory(checkout);
	}

	@Test
	public void testShouldCallListenerWhenLoaded() throws Exception {
		final Inventory.Listener l1 = mock(Inventory.Listener.class);
		final Inventory.Listener l2 = mock(Inventory.Listener.class);
		final Inventory.Listener l3 = mock(Inventory.Listener.class);

		inventory.whenLoaded(l1);
		inventory.whenLoaded(l2);
		inventory.load();
		waitWhileLoading();
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
		inventory.load();
		waitWhileLoading();

		verify(l, times(1)).onLoaded(anyProducts());
	}

	@Test
	public void testShouldRunSameListenerIfLoaded() throws Exception {
		final Inventory.Listener l = mock(Inventory.Listener.class);

		inventory.load();
		waitWhileLoading();
		inventory.whenLoaded(l);
		inventory.whenLoaded(l);
		inventory.whenLoaded(l);

		verify(l, times(3)).onLoaded(anyProducts());
	}

	@Test
	public void testListenerShouldBeCalledOnlyOnce() throws Exception {
		final Inventory.Listener l = mock(Inventory.Listener.class);
		inventory.whenLoaded(l);
		inventory.load();
		inventory.load();
		inventory.load();
		waitWhileLoading();
		inventory.load();
		inventory.load();
		verify(l, times(1)).onLoaded(anyProducts());
	}

	private void waitWhileLoading() throws InterruptedException {
		while (!inventory.isLoaded()) {
			Thread.sleep(10L);
		}
	}
}