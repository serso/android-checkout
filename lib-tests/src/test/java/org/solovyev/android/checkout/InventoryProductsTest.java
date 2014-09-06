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
import org.robolectric.RobolectricTestRunner;

import javax.annotation.Nonnull;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class InventoryProductsTest {

	@Nonnull
	private Inventory.Products products;

	@Before
	public void setUp() throws Exception {
		products = new Inventory.Products();
		products.add(new Inventory.Product("0", true));
		products.add(new Inventory.Product("1", false));
		products.add(new Inventory.Product("2", true));
	}

	@Test
	public void testShouldAddProduct() throws Exception {
		products.add(new Inventory.Product("2", true));

		assertEquals(3, products.size());
		assertEquals("0", products.get("0").id);
		assertEquals("1", products.get("1").id);
		assertEquals("2", products.get("2").id);
	}

	@Test
	public void testShouldIterateOverAllProducts() throws Exception {
		int count = 0;
		for (Inventory.Product product : products) {
			assertTrue(asList("0", "1", "2").contains(product.id));
			count++;
		}

		assertEquals(3, count);
	}
}