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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An interface defining a cache that is used for storing the results of some Billing requests.
 * A {@link Billing} instance can be configured to use a concrete cache (or no cache) via
 * {@link Billing.Configuration#getCache()} method. The default cache implementation is created
 * in {@link Billing#newCache()} factory method.
 *
 * @see Billing.Configuration#getCache()
 */
public interface Cache {

    /**
     * Retrieves an entry from the cache
     *
     * @param key cache key
     * @return an {@link Entry} or null in the event of a cache miss
     */
    @Nullable
    Entry get(@Nonnull Key key);

    /**
     * Adds/replaces an entry in/to the cache
     *
     * @param key   cache key
     * @param entry data to store
     */
    void put(@Nonnull Key key, @Nonnull Entry entry);

    /**
     * Performs any potentially long-running actions needed to initialize the cache;
     * will be called from a worker thread.
     */
    void init();

    /**
     * Removes an entry from the cache
     *
     * @param key Cache key
     */
    void remove(@Nonnull Key key);

    /**
     * Removes all entries from the cache with specified <var>type</var>
     *
     * @param type type of cache key, see {@link Key#type}
     */
    void removeAll(int type);

    /**
     * Empties the cache
     */
    void clear();

    /**
     * Cache key definition that includes a type of the request and some string that uniquely
     * identifies the request's parameters (f.e. list of SKUs, product type, etc).
     */
    final class Key {
        public final int type;
        @Nonnull
        public final String key;

        Key(int type, @Nonnull String key) {
            this.type = type;
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;

            Key that = (Key) o;

            if (type != that.type) return false;
            if (!key.equals(that.key)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = type;
            result = 31 * result + key.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return RequestType.getCacheKeyName(type) + "_" + key;
        }
    }

    /**
     * Cache entry definition that includes the cached data and its expiration date.
     */
    final class Entry {
        @Nonnull
        public final Object data;
        public final long expiresAt;

        Entry(@Nonnull Object data, long expiresAt) {
            this.data = data;
            this.expiresAt = expiresAt;
        }
    }

}
