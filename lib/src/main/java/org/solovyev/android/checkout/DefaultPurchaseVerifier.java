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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import static android.text.TextUtils.isEmpty;

/**
 * Checks the purchase signature with default Android implementation {@link Security}
 */
class DefaultPurchaseVerifier implements PurchaseVerifier {

    @Nonnull
    private final String publicKey;

    public DefaultPurchaseVerifier(@Nonnull String publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public void verify(@Nonnull List<Purchase> purchases, @Nonnull RequestListener<List<Purchase>> listener) {
        final List<Purchase> verifiedPurchases = new ArrayList<Purchase>(purchases.size());
        for (Purchase purchase : purchases) {
            if (Security.verifyPurchase(publicKey, purchase.data, purchase.signature)) {
                verifiedPurchases.add(purchase);
            } else {
                if (isEmpty(purchase.signature)) {
                    Billing.error("Cannot verify purchase: " + purchase + ". Signature is empty");
                } else {
                    Billing.error("Cannot verify purchase: " + purchase + ". Wrong signature");
                }
            }
        }
        listener.onSuccess(verifiedPurchases);
    }
}
