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

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Types of the products available in Billing API
 */
public final class ProductTypes {

    /**
     * Simple product. Might be purchased many times or only once depending on configuration. See
     * <a href="http://developer.android.com/google/play/billing/api.html#managed">Managed In-app
     * Products</a> docs
     */
    public static final String IN_APP = "inapp";
    /**
     * Subscription product. See <a href="http://developer.android.com/google/play/billing/api.html#subs">Subscriptions</a>
     * docs
     */
    public static final String SUBSCRIPTION = "subs";

    public static final List<String> ALL = Arrays.asList(IN_APP, SUBSCRIPTION);

    private ProductTypes() {
        throw new AssertionError();
    }

    static void checkSupported(@Nonnull String product) {
        Check.isTrue(ALL.contains(product), "Unsupported product: " + product);
    }
}
