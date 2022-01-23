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

import android.os.Bundle;
import android.os.RemoteException;

import com.android.vending.billing.InAppBillingService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class ConsumePurchaseRequest extends Request<Object> {

    @Nonnull
    private final String mToken;
    @Nullable
    private final Bundle mExtraParams;

    ConsumePurchaseRequest(@Nonnull String token, @Nullable Bundle extraParams) {
        super(RequestType.CONSUME_PURCHASE, extraParams != null ? Billing.V7 : Billing.V3);
        mToken = token;
        mExtraParams = extraParams;
    }

    @Override
    void start(@Nonnull InAppBillingService service, @Nonnull String packageName) throws RemoteException, RequestException {
        if (mExtraParams != null) {
            final Bundle response = service.consumePurchaseExtraParams(mApiVersion, packageName, mToken, mExtraParams);
            if (!handleError(response)) {
                Billing.waitGooglePlay();
                onSuccess(new Object());
            }
            return;
        }
        final int response = service.consumePurchase(mApiVersion, packageName, mToken);
        if (!handleError(response)) {
            Billing.waitGooglePlay();
            onSuccess(new Object());
        }
    }

    @Nullable
    @Override
    protected String getCacheKey() {
        return null;
    }
}
