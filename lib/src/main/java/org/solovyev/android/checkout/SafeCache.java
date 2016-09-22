/*
 * Copyright 2015 serso aka se.solovyev
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
 * Crash-safe wrapper around the cache
 */
final class SafeCache implements Cache {
    @Nonnull
    private final Cache cache;

    SafeCache(@Nonnull Cache cache) {
        this.cache = cache;
    }

    @Override
    @Nullable
    public Entry get(@Nonnull Key key) {
        try {
            return cache.get(key);
        } catch (Exception e) {
            Billing.error(e);
            return null;
        }
    }

    @Override
    public void put(@Nonnull Key key, @Nonnull Entry entry) {
        try {
            cache.put(key, entry);
        } catch (Exception e) {
            Billing.error(e);
        }
    }

    @Override
    public void init() {
        try {
            cache.init();
        } catch (Exception e) {
            Billing.error(e);
        }
    }

    @Override
    public void remove(@Nonnull Key key) {
        try {
            cache.remove(key);
        } catch (Exception e) {
            Billing.error(e);
        }
    }

    @Override
    public void removeAll(int type) {
        try {
            cache.removeAll(type);
        } catch (Exception e) {
            Billing.error(e);
        }
    }

    @Override
    public void clear() {
        try {
            cache.clear();
        } catch (Exception e) {
            Billing.error(e);
        }
    }
}
