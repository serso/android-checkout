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

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.mock;

class AsyncServiceConnector implements Billing.ServiceConnector {

	@Nonnull
	private final Executor background;

	@Nonnull
	private final Billing billing;

	public AsyncServiceConnector(@Nonnull Billing billing) {
		this.billing = billing;
		background = Executors.newSingleThreadExecutor();
	}

	@Override
	public boolean connect() {
		setService(mock(IInAppBillingService.class));
		return true;
	}

	private void setService(final IInAppBillingService service) {
		background.execute(new Runnable() {
			@Override
			public void run() {
				billing.setService(service, service != null);
			}
		});
	}

	@Override
	public void disconnect() {
		setService(null);
	}
}
