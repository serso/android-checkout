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

package org.solovyev.android.checkout.app;

import org.solovyev.android.checkout.Sku;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SkuUi {

    final Sku sku;

    @Nullable
    final String token;

    final boolean canChangeSubs;

    private SkuUi(Sku sku, @Nullable String token, boolean canChangeSubs) {
        this.sku = sku;
        this.token = token;
        this.canChangeSubs = canChangeSubs;
    }

    @Nonnull
    public static SkuUi create(Sku sku, @Nullable String token, boolean canChangeSubs) {
        return new SkuUi(sku, token, canChangeSubs);
    }

    static int getIconResId(@Nonnull String skuId) {
        final int iconResId;
        if (skuId.equals("cake")) {
            iconResId = R.drawable.ic_cake;
        } else if (skuId.equals("beer")) {
            iconResId = R.drawable.ic_beer;
        } else if (skuId.equals("hamburger")) {
            iconResId = R.drawable.ic_hamburger;
        } else if (skuId.equals("coffee")) {
            iconResId = R.drawable.ic_coffee;
        } else {
            iconResId = R.drawable.ic_shoppping_cart;
        }
        return iconResId;
    }

    @Nonnull
    static String getTitle(@Nonnull Sku sku) {
        if (sku.isSubscription()) {
            return sku.title;
        }
        final int i = sku.title.indexOf("(");
        if (i > 0) {
            if (sku.title.charAt(i - 1) == ' ') {
                return sku.title.substring(0, i - 1);
            } else {
                return sku.title.substring(0, i);
            }
        }
        return sku.title;
    }

    boolean isPurchased() {
        return token != null;
    }
}
