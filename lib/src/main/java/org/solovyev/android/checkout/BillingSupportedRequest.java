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

import android.os.Bundle;
import android.os.RemoteException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.solovyev.android.checkout.RequestType.BILLING_SUPPORTED;

final class BillingSupportedRequest extends Request<Object> {

    @Nonnull
    private final String mProduct;
    @Nullable
    private final Bundle mExtraParams;

    BillingSupportedRequest(@Nonnull String product) {
        this(product, Billing.V3, null);
    }

    BillingSupportedRequest(@Nonnull String product, int apiVersion, @Nullable Bundle extraParams) {
        super(BILLING_SUPPORTED, apiVersion);
        Check.isTrue(extraParams == null || apiVersion >= Billing.V7, "#isBillingSupportedExtraParams only works in Billing API v7 or higher");
        mProduct = product;
        mExtraParams = extraParams;
    }

    @Override
    public void start(@Nonnull InAppBillingService service, @Nonnull String packageName) throws RemoteException {
        final int response = mExtraParams != null ? service.isBillingSupportedExtraParams(mApiVersion, packageName, mProduct, mExtraParams) : service.isBillingSupported(mApiVersion, packageName, mProduct);
        if (!handleError(response)) {
            onSuccess(new Object());
        }
    }

    @Nullable
    @Override
    protected String getCacheKey() {
        if (mExtraParams != null) {
            // disable cache if there are any extra parameters
            return null;
        }
        if (mApiVersion == Billing.V3) {
            return mProduct;
        }
        return mProduct + "_" + mApiVersion;
    }
}
