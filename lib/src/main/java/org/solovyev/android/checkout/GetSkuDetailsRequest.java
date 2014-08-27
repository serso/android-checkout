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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class GetSkuDetailsRequest extends Request<Skus> {

	@Nonnull
	private final String product;

	@Nonnull
	private final ArrayList<String> skus;

	GetSkuDetailsRequest(@Nonnull String product, @Nonnull List<String> skus) {
		super(RequestType.GET_SKU_DETAILS);
		this.product = product;
		this.skus = new ArrayList<String>(skus);
		Collections.sort(this.skus);
	}

	@Override
	void start(@Nonnull IInAppBillingService service, int apiVersion, @Nonnull String packageName) throws RemoteException, RequestException {
		final Bundle skusBundle = new Bundle();
		skusBundle.putStringArrayList("ITEM_ID_LIST", skus);
		final Bundle bundle = service.getSkuDetails(apiVersion, packageName, product, skusBundle);
		if (!handleError(bundle)) {
			onSuccess(Skus.fromBundle(bundle, product));
		}
	}

	@Nullable
	@Override
	protected String getCacheKey() {
		if (skus.size() == 1) {
			return product + "_" + skus.get(0);
		} else {
			final StringBuilder sb = new StringBuilder(5 * skus.size());
			sb.append("[");
			for (int i = 0; i < skus.size(); i++) {
				if (i > 0) {
					sb.append(",");
				}
				sb.append(skus.get(i));
			}
			sb.append("]");
			return product + "_" + sb.toString();
		}
	}
}
