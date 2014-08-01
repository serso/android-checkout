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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;

import javax.annotation.Nonnull;

import static org.mockito.Mockito.*;

@RunWith(CheckoutTestRunner.class)
public class BillingTest {

	@Nonnull
	private Billing billing;

	@Before
	public void setUp() throws Exception {
		billing = new Billing(Robolectric.application, "test", null);
		final CancellableExecutor sameThreadExecutor = sameThreadExecutor();
		billing.setBackground(sameThreadExecutor);
		billing.setMainThread(sameThreadExecutor);
	}

	@Test
	public void testShouldNotifyErrorIfCantConnect() throws Exception {
		final Billing.ServiceConnector connector = mock(Billing.ServiceConnector.class);
		when(connector.connect()).thenReturn(false);

		billing.setConnector(connector);
		final RequestListener<Object> l = mock(RequestListener.class);
		billing.getRequests().isBillingSupported("p", l);
		verify(l, times(1)).onError(eq(ResponseCodes.SERVICE_NOT_CONNECTED), any(BillingException.class));
		verify(l, times(0)).onSuccess(any());
	}

	@Test
	public void testShouldNotifyErrorIfConnectorReturnedNull() throws Exception {
		final Billing.ServiceConnector connector = mock(Billing.ServiceConnector.class);
		when(connector.connect()).then(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				billing.setService(null, true);
				return true;
			}
		});

		billing.setConnector(connector);
		final RequestListener<Object> l = mock(RequestListener.class);
		billing.getRequests().isBillingSupported("p", l);
		verify(l, times(1)).onError(eq(ResponseCodes.SERVICE_NOT_CONNECTED), any(BillingException.class));
		verify(l, times(0)).onSuccess(any());
	}

	@Test
	public void testShouldExecuteRequestIfConnected() throws Exception {
		final Billing.ServiceConnector connector = mock(Billing.ServiceConnector.class);
		final IInAppBillingService service = mock(IInAppBillingService.class);
		when(service.isBillingSupported(anyInt(), anyString(), anyString())).thenReturn(ResponseCodes.OK);
		when(connector.connect()).then(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				billing.setService(service, true);
				return true;
			}
		});

		billing.setConnector(connector);
		final RequestListener<Object> l = mock(RequestListener.class);
		billing.getRequests().isBillingSupported("p", l);
		verify(l, times(0)).onError(anyInt(), any(BillingException.class));
		verify(l, times(1)).onSuccess(any());
	}

	@Nonnull
	private CancellableExecutor sameThreadExecutor() {
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
}