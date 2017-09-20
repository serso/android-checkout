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

import android.os.Bundle;

import java.util.ArrayList;

import javax.annotation.Nonnull;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SkusTest {

    @Nonnull
    private Skus mSkus;

    @Before
    public void setUp() throws Exception {
        mSkus = new Skus("test", asList(newSku("1"), newSku("2"), newSku("3")));
    }

    @Test
    public void testShouldReturnSkuById() throws Exception {
        assertNotNull(mSkus.getSku("2"));
    }

    @Test
    public void testShouldNotReturnSkuById() throws Exception {
        assertNull(mSkus.getSku("4"));
    }

    @Test
    public void testShouldHaveSku() throws Exception {
        assertTrue(mSkus.hasSku("2"));
    }

    @Test
    public void testShouldNotHaveSku() throws Exception {
        assertFalse(mSkus.hasSku("4"));
    }

    @Test
    public void testShouldReadNullList() throws Exception {
        final Skus skus = Skus.fromBundle(new Bundle(), "test");
        assertTrue(skus.list.isEmpty());
    }

    @Test
    public void testShouldReadEmptyList() throws Exception {
        final Bundle bundle = new Bundle();
        bundle.putStringArrayList(Skus.BUNDLE_LIST, new ArrayList<String>());
        final Skus skus = Skus.fromBundle(bundle, "test");
        assertTrue(skus.list.isEmpty());
    }

    @Test
    public void testShouldHaveCorrectProduct() throws Exception {
        final Skus skus = Skus.fromBundle(new Bundle(), "test");
        assertEquals("test", skus.product);
    }

    @Test
    public void testShouldReadSkus() throws Exception {
        final ArrayList<String> list = new ArrayList<String>();
        list.add(SkuTest.newInAppJson("1"));
        list.add(SkuTest.newInAppJson("2"));
        list.add(SkuTest.newSubscriptionJson("3"));
        list.add(SkuTest.newSubscriptionJson("4"));

        final Bundle bundle = new Bundle();
        bundle.putStringArrayList(Skus.BUNDLE_LIST, list);
        final Skus skus = Skus.fromBundle(bundle, "dummy");
        assertEquals(4, skus.list.size());
        SkuTest.verifySku(skus.list.get(0), "1");
        SkuTest.verifySku(skus.list.get(3), "4");
    }

    private Sku newSku(String id) {
        return new Sku("test", id, id, Sku.Price.EMPTY, id, id, id, Sku.Price.EMPTY, id, id, id, 0);
    }
}