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
import android.support.annotation.Nullable;
import com.android.vending.billing.IInAppBillingService;
import org.json.JSONException;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.solovyev.android.checkout.Purchase.State.PURCHASED;
import static org.solovyev.android.checkout.ResponseCodes.EXCEPTION;
import static org.solovyev.android.checkout.ResponseCodes.OK;

public class GetPurchasesRequestTest extends RequestTestBase {

	@Override
	protected GetPurchasesRequest newRequest() {
		return new GetPurchasesRequest("test", null, Tests.newMockVerifier(true));
	}

	@Test
	public void testShouldCreateFromOldRequest() throws Exception {
		final GetPurchasesRequest oldRequest = new GetPurchasesRequest("test", "first", Tests.newMockVerifier(true));
		final GetPurchasesRequest newRequest = new GetPurchasesRequest(oldRequest, "second");

		assertEquals("second", newRequest.getContinuationToken());
		assertEquals("test", newRequest.getProduct());
		assertSame(oldRequest.getListener(), newRequest.getListener());
	}

	@Test
	public void testShouldHaveDifferentCacheKeys() throws Exception {
		final GetPurchasesRequest oldRequest = newRequest();
		final GetPurchasesRequest newRequest1 = new GetPurchasesRequest(oldRequest, "second");
		final GetPurchasesRequest newRequest2 = new GetPurchasesRequest(oldRequest, "third");

		assertNotEquals(oldRequest.getCacheKey(), newRequest1.getCacheKey());
		assertNotEquals(newRequest1.getCacheKey(), newRequest2.getCacheKey());
	}

	@Test
	public void testShouldErrorIfJsonException() throws Exception {
		final GetPurchasesRequest request = newRequest();
		final RequestListener l = mock(RequestListener.class);
		request.setListener(l);
		final IInAppBillingService service = mock(IInAppBillingService.class);
		final Bundle bundle = newBundle(OK);
		final ArrayList<String> datas = new ArrayList<String>();
		datas.add("test");
		bundle.putStringArrayList(Purchases.BUNDLE_DATA_LIST, datas);
		when(service.getPurchases(anyInt(), anyString(), anyString(), anyString())).thenReturn(bundle);

		request.start(service, 3, "test");

		verify(l, times(1)).onError(eq(EXCEPTION), any(JSONException.class));
	}

	@Test
	public void testShouldVerifyPurchasesAsynchronously() throws Exception {
		final GetPurchasesRequest request = new GetPurchasesRequest("test", null, new AsyncPurchaseVerifier());
		final PurchasesAwareRequestListener l = new PurchasesAwareRequestListener();
		request.setListener(l);
		final IInAppBillingService service = mock(IInAppBillingService.class);
		final ArrayList<String> list = new ArrayList<String>();
		list.add(PurchaseTest.newJson(0, Purchase.State.REFUNDED));
		list.add(PurchaseTest.newJson(1, Purchase.State.REFUNDED));
		list.add(PurchaseTest.newJson(2, Purchase.State.CANCELLED));
		list.add(PurchaseTest.newJson(3, PURCHASED));
		list.add(PurchaseTest.newJson(4, PURCHASED));
		list.add(PurchaseTest.newJson(5, PURCHASED));
		list.add(PurchaseTest.newJson(6, PURCHASED));
		final Bundle bundle = newBundle(OK);
		bundle.putStringArrayList(Purchases.BUNDLE_DATA_LIST, list);
		when(service.getPurchases(anyInt(), anyString(), anyString(), anyString())).thenReturn(bundle);

		request.start(service, 3, "test");

		assertNotNull(l.purchases);
		assertTrue(l.purchases.list.size() == 4);
	}

	private static class AsyncPurchaseVerifier implements PurchaseVerifier {
		@Nonnull
		private Executor executor = Executors.newSingleThreadExecutor();

		@Override
		public void verify(@Nonnull final List<Purchase> purchases, @Nonnull RequestListener<List<Purchase>> listener) {
			final CountDownLatch latch = new CountDownLatch(1);
			final List<Purchase> verifiedPurchases = new ArrayList<Purchase>(purchases.size());
			executor.execute(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < purchases.size(); i++) {
						if (i % 2 == 0) {
							verifiedPurchases.add(purchases.get(i));
						}
					}
					latch.countDown();
				}
			});
			try {
				if (latch.await(1L, TimeUnit.SECONDS)) {
					listener.onSuccess(verifiedPurchases);
				} else {
					listener.onError(ResponseCodes.EXCEPTION, new Exception());
				}
			} catch (InterruptedException e) {
				listener.onError(ResponseCodes.EXCEPTION, e);
			}
		}
	}

	private static class PurchasesAwareRequestListener implements RequestListener<Purchases> {
		@Nullable
		Purchases purchases;

		@Override
		public void onSuccess(@Nonnull Purchases result) {
			this.purchases = result;
		}

		@Override
		public void onError(int response, @Nonnull Exception e) {

		}
	}
}