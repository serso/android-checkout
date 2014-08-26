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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import javax.annotation.Nonnull;

import java.util.ArrayList;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class SkusTest {

	@Nonnull
	private Skus skus;

	@Before
	public void setUp() throws Exception {
		skus = new Skus("test", asList(newSku("1"), newSku("2"), newSku("3")));
	}

	@Test
	public void testShouldReturnSkuById() throws Exception {
		assertNotNull(skus.getSku("2"));
	}

	@Test
	public void testShouldNotReturnSkuById() throws Exception {
		assertNull(skus.getSku("4"));
	}

	@Test
	public void testShouldHaveSku() throws Exception {
		assertTrue(skus.hasSku("2"));
	}

	@Test
	public void testShouldNotHaveSku() throws Exception {
		assertFalse(skus.hasSku("4"));
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
		list.add(SkuTest.newJson("1"));
		list.add(SkuTest.newJson("2"));
		list.add(SkuTest.newJson("3"));
		list.add(SkuTest.newJson("4"));

		final Bundle bundle = new Bundle();
		bundle.putStringArrayList(Skus.BUNDLE_LIST, list);
		final Skus skus = Skus.fromBundle(bundle, "test");
		assertEquals(4, skus.list.size());
		SkuTest.verifySku(skus.list.get(0), "1");
		SkuTest.verifySku(skus.list.get(3), "4");
	}

	private Sku newSku(String id) {
		return new Sku("test", id, id, id, id);
	}
}