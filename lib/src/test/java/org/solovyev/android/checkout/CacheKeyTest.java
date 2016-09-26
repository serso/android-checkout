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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class CacheKeyTest {

    @Test
    public void testHashCodeAndEquals() throws Exception {
        final Cache.Key k1 = new Cache.Key(1, "test");
        final Cache.Key k2 = new Cache.Key(2, "test");
        final Cache.Key k3 = new Cache.Key(1, "test1");
        final Cache.Key k4 = new Cache.Key(3, "test1");
        final Cache.Key k5 = new Cache.Key(1, "test");

        assertNotEquals(k1.hashCode(), k2.hashCode());
        assertNotEquals(k1.hashCode(), k3.hashCode());
        assertNotEquals(k1.hashCode(), k4.hashCode());
        assertEquals(k1.hashCode(), k5.hashCode());

        assertNotEquals(k1, k2);
        assertNotEquals(k1, k3);
        assertNotEquals(k1, k4);
        assertEquals(k1, k5);

        assertNotEquals(k1.toString(), k2.toString());
        assertNotEquals(k1.toString(), k3.toString());
        assertNotEquals(k1.toString(), k4.toString());
        assertEquals(k1.toString(), k5.toString());
    }
}