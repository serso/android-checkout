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

import android.os.RemoteException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class ConsumePurchaseRequest extends Request<Object> {

    @Nonnull
    private final String mToken;

    ConsumePurchaseRequest(@Nonnull String token) {
        super(RequestType.CONSUME_PURCHASE);
        mToken = token;
    }

    @Override
    void start(@Nonnull InAppBillingService service, @Nonnull String packageName) throws RemoteException, RequestException {
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
