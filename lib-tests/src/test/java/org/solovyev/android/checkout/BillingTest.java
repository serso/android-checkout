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

import android.os.RemoteException;
import com.android.vending.billing.IInAppBillingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@RunWith(CheckoutTestRunner.class)
public class BillingTest {

	@Nonnull
	private Billing billing;

	@Before
	public void setUp() throws Exception {
		billing = Tests.newSynchronousBilling();
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

	@Test
	public void testStates() throws Exception {
		final Billing.ServiceConnector connector = mock(Billing.ServiceConnector.class);
		when(connector.connect()).thenReturn(true);
		billing.setConnector(connector);
		billing.connect();

		assertEquals(Billing.State.CONNECTING, billing.getState());
		billing.setService(mock(IInAppBillingService.class), true);
		assertEquals(Billing.State.CONNECTED, billing.getState());
		billing.disconnect();
		assertEquals(Billing.State.DISCONNECTING, billing.getState());
		billing.setService(null, false);
		assertEquals(Billing.State.DISCONNECTED, billing.getState());
	}

	@Test
	public void testShouldDisconnectOnlyIfDisconnecting() throws Exception {
		billing.setState(Billing.State.FAILED);
		billing.setService(null, false);

		assertEquals(Billing.State.FAILED, billing.getState());

		billing.setState(Billing.State.DISCONNECTING);
		billing.setService(null, false);

		assertEquals(Billing.State.DISCONNECTED, billing.getState());
	}

	@Test
	public void testShouldConnectOnlyIfConnecting() throws Exception {
		billing.setState(Billing.State.FAILED);
		billing.setService(mock(IInAppBillingService.class), true);

		assertEquals(Billing.State.FAILED, billing.getState());

		billing.setState(Billing.State.CONNECTING);
		billing.setService(mock(IInAppBillingService.class), true);

		assertEquals(Billing.State.CONNECTED, billing.getState());
	}

	@Test
	public void testShouldRunAllRequests() throws Exception {
		final int REQUESTS = 100;
		final int SLEEP = 10;

		final Billing b = Tests.newBilling(false);
		b.setConnector(new AsyncServiceConnector(b));
		final Random r = new Random(currentTimeMillis());
		final CountDownLatch latch = new CountDownLatch(REQUESTS);
		final RequestListener l = new CountDownListener(latch);
		for (int i = 0; i < REQUESTS; i++) {
			if (i % 10 == 0) {
				if (r.nextBoolean()) {
					b.connect();
				} else {
					b.disconnect();
				}
			}
			b.runWhenConnected(new SleepingRequest(r.nextInt(SLEEP)), l, null);
		}
		b.connect();
		assertTrue(latch.await(SLEEP * REQUESTS, TimeUnit.MILLISECONDS));
	}

	private static class CountDownListener implements RequestListener {

		private final CountDownLatch latch;

		public CountDownListener(CountDownLatch latch) {
			this.latch = latch;
		}

		@Override
		public void onSuccess(@Nonnull Object result) {
			onEnd();
		}

		private void onEnd() {
			latch.countDown();
		}

		@Override
		public void onError(int response, @Nonnull Exception e) {
			onEnd();
		}
	}

	private final class SleepingRequest extends Request {

		private final long sleep;

		private SleepingRequest(long sleep) {
			super(RequestType.BILLING_SUPPORTED);
			this.sleep = sleep;
		}

		@Override
		void start(@Nonnull IInAppBillingService service, int apiVersion, @Nonnull String packageName) throws RemoteException, RequestException {
			try {
				Thread.sleep(sleep);
				onSuccess(new Object());
			} catch (InterruptedException e) {
				throw new RequestException(e);
			}
		}

		@Nullable
		@Override
		String getCacheKey() {
			return null;
		}
	}
}