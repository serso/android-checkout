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

import android.app.PendingIntent;
import android.os.Bundle;
import android.os.RemoteException;
import com.android.vending.billing.IInAppBillingService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class PurchaseRequest extends Request<PendingIntent> {

	@Nonnull
	private final String product;

	@Nonnull
	private final String sku;

	@Nullable
	private final String payload;

	PurchaseRequest(@Nonnull String product, @Nonnull String sku, @Nullable String payload) {
		super(RequestType.PURCHASE);
		this.product = product;
		this.sku = sku;
		this.payload = payload;
	}

	@Override
	void start(@Nonnull IInAppBillingService service, @Nonnull String packageName) throws RemoteException, RequestException {
		final Bundle bundle = service.getBuyIntent(apiVersion, packageName, sku, product, payload == null ? "" : payload);
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
