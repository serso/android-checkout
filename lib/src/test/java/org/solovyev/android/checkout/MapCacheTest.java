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

public class MapCacheTest extends CacheTestBase {

    @Test
    public void testShouldAddEntries() throws Exception {
        final MapCache cache = new MapCache();
        final Cache.Entry expected = newEntry();
        final Cache.Key key = newKey();

        cache.put(key, expected);

        Assert.assertSame(expected, cache.get(key));
    }

    @Test
    public void testShouldRemoveEntries() throws Exception {
        final MapCache cache = new MapCache();
        final Cache.Entry expected = newEntry();
        final Cache.Key key = newKey();
        cache.put(key, expected);

        cache.remove(key);

        Assert.assertNull(cache.get(key));
    }

    @Test
    public void testShouldRemoveEntriesByType() throws Exception {
        final MapCache cache = new MapCache();
        final Cache.Key key = new Cache.Key(1, "test");
        final Cache.Key key1 = new Cache.Key(1, "test1");
        final Cache.Key key2 = new Cache.Key(1, "test2");
        final Cache.Key key3 = new Cache.Key(2, "test2");

        cache.put(key, newEntry());
        cache.put(key1, newEntry());
        cache.put(key2, newEntry());
        cache.put(key3, newEntry());

        Assert.assertNotNull(cache.get(key));
        Assert.assertNotNull(cache.get(key1));
        Assert.assertNotNull(cache.get(key2));
        Assert.assertNotNull(cache.get(key3));

        cache.removeAll(1);
        Assert.assertNull(cache.get(key));
        Assert.assertNull(cache.get(key1));
        Assert.assertNull(cache.get(key2));
        Assert.assertNotNull(cache.get(key3));
    }
}