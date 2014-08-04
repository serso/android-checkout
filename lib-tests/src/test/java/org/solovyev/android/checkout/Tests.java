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

import com.android.vending.billing.IInAppBillingService;
import org.robolectric.Robolectric;

import javax.annotation.Nonnull;

import static org.mockito.Mockito.mock;

public final class Tests {

	private Tests() {
		throw new AssertionError();
	}

	@Nonnull
	static CancellableExecutor sameThreadExecutor() {
		return new CancellableExecutor() {
			@Override
			public void execute(@Nonnull Runnable runnable) {
				runnable.run();
			}

			@Override
			public void cancel(@Nonnull Runnable runnable) {
			}
		};
	}

	@Nonnull
	static Billing newBilling() {
		final Billing billing = new Billing(Robolectric.application, "test", null);
		final IInAppBillingService service = mock(IInAppBillingService.class);
		setService(billing, service);
		return billing;
	}

	@Nonnull
	static Billing newSynchronousBilling() {
		final Billing billing = new Billing(Robolectric.application, "test", null);
		final IInAppBillingService service = mock(IInAppBillingService.class);
		final CancellableExecutor sameThreadExecutor = sameThreadExecutor();
		billing.setBackground(sameThreadExecutor);
		billing.setMainThread(sameThreadExecutor);
		setService(billing, service);
		return billing;
	}

	static void setService(@Nonnull final Billing billing, @Nonnull final IInAppBillingService service) {
		billing.setConnector(new Billing.ServiceConnector() {
			@Override
			public boolean connect() {
				billing.setService(service, true);
				return true;
			}

			@Override
			public void disconnect() {
				billing.setService(null, false);
			}
		});
	}
}
