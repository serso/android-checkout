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

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.solovyev.android.checkout.Billing.DAY;

public class ConcurrentCacheTest extends CacheTestBase {

	@Test
	public void testShouldPutAllEntries() throws Exception {
		final Cache mockCache = mock(Cache.class);
		final ConcurrentCache cache = new ConcurrentCache(mockCache);

		final Cache.Entry entry = newEntry();
		cache.put(newKey(), entry);
		cache.put(newKey(), entry);
		cache.put(newKey(), entry);

		verify(mockCache, times(3)).put(any(Cache.Key.class), eq(entry));
	}

	@Test
	public void testShouldReturnNotExpiredValue() throws Exception {
		final ConcurrentCache cache = new ConcurrentCache(new MapCache());

		final Cache.Entry expected = newEntry();
		final Cache.Key key = newKey();

		cache.put(key, expected);

		final Cache.Entry actual = cache.get(key);
		Assert.assertSame(expected, actual);
	}

	@Test
	public void testShouldReturnNullIfValueExpired() throws Exception {
		final ConcurrentCache cache = new ConcurrentCache(new MapCache());

		final Cache.Entry entry = newEntry(-DAY);
		final Cache.Key key = newKey();

		cache.put(key, entry);

		final Cache.Entry actual = cache.get(key);
		Assert.assertNull(actual);
	}

	@Test
	public void testShouldPutOnlyIfNotExists() throws Exception {
		final ConcurrentCache cache = new ConcurrentCache(new MapCache());

		final Cache.Entry entry = newEntry();
		final Cache.Entry newEntry = newEntry();
		final Cache.Key key = newKey();

		cache.put(key, entry);
		cache.putIfNotExist(key, newEntry);

		Assert.assertSame(entry, cache.get(key));
	}

	@Test
	public void testShouldCallInit() throws Exception {
		final Cache mockCache = mock(Cache.class);
		final ConcurrentCache cache = new ConcurrentCache(mockCache);

		cache.init();

		verify(mockCache, times(1)).init();
	}
}