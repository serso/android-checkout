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
import com.android.vending.billing.IInAppBillingService;
import org.json.JSONException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class GetPurchasesRequest extends Request<Purchases> {

	@Nonnull
	private final String product;

	@Nullable
	private final String continuationToken;

	GetPurchasesRequest(@Nonnull String product, @Nullable String continuationToken) {
		super(RequestType.GET_PURCHASES);
		this.product = product;
		this.continuationToken = continuationToken;
	}

	GetPurchasesRequest(@Nonnull GetPurchasesRequest request, @Nonnull String continuationToken) {
		super(RequestType.GET_PURCHASES, request);
		this.product = request.product;
		this.continuationToken = continuationToken;
	}

	@Nonnull
	String getProduct() {
		return product;
	}

	@Override
	void start(@Nonnull IInAppBillingService service, int apiVersion, @Nonnull String packageName) throws RemoteException {
		final Bundle bundle = service.getPurchases(apiVersion, packageName, product, continuationToken);
		if (!handleError(bundle)) {
			try {
				onSuccess(Purchases.fromBundle(bundle, product));
			} catch (JSONException e) {
				onError(e);
			}
		}
	}

	@Nullable
	@Override
	protected String getCacheKey() {
		if (continuationToken != null) {
			return product + "_" + continuationToken;
		} else {
			return product;
		}
	}
}
