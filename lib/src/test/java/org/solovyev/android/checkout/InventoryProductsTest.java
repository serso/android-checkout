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
import org.robolectric.annotation.Config;

import javax.annotation.Nonnull;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class InventoryProductsTest {

    @Nonnull
    private Inventory.Products mProducts;

    @Before
    public void setUp() throws Exception {
        mProducts = new Inventory.Products();
    }

    @Test
    public void testShouldAddProduct() throws Exception {
        mProducts.add(new Inventory.Product(ProductTypes.IN_APP, true));
        mProducts.add(new Inventory.Product(ProductTypes.SUBSCRIPTION, true));

        assertEquals(2, mProducts.size());
        assertEquals(ProductTypes.IN_APP, mProducts.get(ProductTypes.IN_APP).id);
        assertEquals(ProductTypes.SUBSCRIPTION, mProducts.get(ProductTypes.SUBSCRIPTION).id);
    }

    @Test
    public void testShouldIterateOverAllProducts() throws Exception {
        int count = 0;
        for (Inventory.Product product : mProducts) {
            assertTrue(ProductTypes.ALL.contains(product.id));
            count++;
        }

        assertEquals(2, count);
    }
}