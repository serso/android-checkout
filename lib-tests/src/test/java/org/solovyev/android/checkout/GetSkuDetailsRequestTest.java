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

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class GetSkuDetailsRequestTest extends RequestTestBase {

	@Override
	protected Request newRequest() {
		return new GetSkuDetailsRequest("test", asList("sku"));
	}

	@Test
	public void testShouldHaveSameCacheKey() throws Exception {
		final List<String> skus = asList("1", "2", "3");
		final GetSkuDetailsRequest r1 = new GetSkuDetailsRequest("test", skus);
		Collections.reverse(skus);
		final GetSkuDetailsRequest r2 = new GetSkuDetailsRequest("test", skus);

		assertEquals(r1.getCacheKey(), r2.getCacheKey());
	}

	@Test
	public void testShouldContainAllSkusInCacheKey() throws Exception {
		final GetSkuDetailsRequest request = new GetSkuDetailsRequest("test", asList("1", "2", "3"));

		assertEquals("test_[1,2,3]", request.getCacheKey());
	}
}