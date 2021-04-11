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

import com.android.vending.billing.InAppBillingService;

import android.app.PendingIntent;
import android.os.Bundle;
import android.os.RemoteException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class PurchaseRequest extends Request<PendingIntent> {

    @Nonnull
    private final String mProduct;
    @Nonnull
    private final String mSku;
    @Nullable
    private final String mPayload;
    @Nullable
    private final Bundle mExtraParams;

    PurchaseRequest(@Nonnull String product, @Nonnull String sku, @Nullable String payload) {
        this(product, sku, payload, null);
    }

    PurchaseRequest(@Nonnull String product, @Nonnull String sku, @Nullable String payload, @Nullable Bundle extraParams) {
        super(RequestType.PURCHASE, extraParams != null ? Billing.V6 : Billing.V3);
        mProduct = product;
        mSku = sku;
        mPayload = payload;
        mExtraParams = extraParams;
    }

    @Override
    void start(@Nonnull InAppBillingService service, @Nonnull String packageName) throws RemoteException, RequestException {
        final String payload = mPayload == null ? "" : mPayload;
        final Bundle bundle = mExtraParams != null ? service.getBuyIntentExtraParams(mApiVersion, packageName, mSku, mProduct, payload, mExtraParams) : service.getBuyIntent(mApiVersion, packageName, mSku, mProduct, payload);
        if (handleError(bundle)) {
            return;
        }
        final PendingIntent pendingIntent = bundle.getParcelable("BUY_INTENT");
        Check.isNotNull(pendingIntent);
        onSuccess(pendingIntent);
    }

    @Nullable
    @Override
    protected String getCacheKey() {
        return null;
    }
}
