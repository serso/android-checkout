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

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public final class Tests {

	private Tests() {
		throw new AssertionError();
	}

	@Nonnull
	static CancellableExecutor sameThreadExecutor() {
		return SameThreadExecutor.INSTANCE;
	}

	@Nonnull
	static Billing newBilling() {
		return newBilling(true);
	}

	@Nonnull
	static Billing newBilling(boolean cache) {
		return newBilling(cache, false);
	}

	@Nonnull
	static Billing newBilling(boolean cache, boolean autoConnect) {
		return newBilling(newConfiguration(cache, autoConnect));
	}

	@Nonnull
	static Billing newBilling(@Nonnull Billing.Configuration configuration) {
		final Billing billing = new Billing(RuntimeEnvironment.application, configuration);
		billing.setPurchaseVerifier(Tests.newMockVerifier(true));
		final IInAppBillingService service = mock(IInAppBillingService.class);
		setService(billing, service);
		return billing;
	}

	@Nonnull
	private static Billing.Configuration newConfiguration(final boolean cache, final boolean autoConnect) {
		return new Billing.Configuration() {
			@Nonnull
			@Override
			public String getPublicKey() {
				return "test";
			}

			@Nullable
			@Override
			public Cache getCache() {
				return cache ? Billing.newCache() : null;
			}

			@Nonnull
			@Override
			public PurchaseVerifier getPurchaseVerifier() {
				return Billing.newPurchaseVerifier(this.getPublicKey());
			}

			@Override
			public Inventory getFallbackInventory(@Nonnull Checkout checkout, @Nonnull Executor onLoadExecutor) {
				return null;
			}

			@Override
			public boolean isAutoConnect() {
				return autoConnect;
			}
		};
	}

	@Nonnull
	static Billing newSynchronousBilling() {
		final Billing billing = new Billing(RuntimeEnvironment.application, newConfiguration(true, false));
		billing.setPurchaseVerifier(Tests.newMockVerifier(true));
		final IInAppBillingService service = mock(IInAppBillingService.class);
		final CancellableExecutor sameThreadExecutor = sameThreadExecutor();
		billing.setBackground(sameThreadExecutor);
		billing.setMainThread(sameThreadExecutor);
		setService(billing, service);
		return billing;
	}

	static void setService(@Nonnull final Billing billing, @Nonnull final IInAppBillingService service) {
		if (billing.getState() != Billing.State.INITIAL) {
			billing.disconnect();
		}
		billing.setConnector(new TestServiceConnector(billing, service));
	}

	@Nonnull
	static PurchaseVerifier newMockVerifier(final boolean verified) {
		return mockVerifier(mock(PurchaseVerifier.class), verified);
	}

	@Nonnull
	static PurchaseVerifier mockVerifier(@Nonnull PurchaseVerifier verifier, final boolean verified) {
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				final List<Purchase> purchases = (List<Purchase>) invocation.getArguments()[0];
				final RequestListener<List<Purchase>> l = (RequestListener) invocation.getArguments()[1];
				l.onSuccess(verified ? new ArrayList<Purchase>(purchases) : Collections.<Purchase>emptyList());
				return null;
			}
		}).when(verifier).verify(anyList(), any(RequestListener.class));
		return verifier;
	}
}
