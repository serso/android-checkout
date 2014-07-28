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

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import org.solovyev.android.checkout.*;

import javax.annotation.Nonnull;

import static org.solovyev.android.checkout.ProductTypes.IN_APP;
import static org.solovyev.android.checkout.app.CheckoutApplication.IN_APP_SKUS;

public class SkusListFragment extends ListFragment {

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getCheckout().onReady(new Checkout.ListenerAdapter() {
			@Override
			public void onReady(@Nonnull BillingRequests requests) {
				requests.getSkus(IN_APP, IN_APP_SKUS, new RequestListener<Skus>() {
					@Override
					public void onSuccess(@Nonnull Skus skus) {
					}

					@Override
					public void onError(int response, @Nonnull Exception e) {
					}
				});
			}
		});
	}

	@Nonnull
	private ActivityCheckout getCheckout() {
		return ((MainActivity) getActivity()).getCheckout();
	}
}
