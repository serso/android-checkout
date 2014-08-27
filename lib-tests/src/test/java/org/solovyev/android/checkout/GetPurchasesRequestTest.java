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
import com.android.vending.billing.IInAppBillingService;
import org.json.JSONException;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.solovyev.android.checkout.ResponseCodes.EXCEPTION;
import static org.solovyev.android.checkout.ResponseCodes.OK;

public class GetPurchasesRequestTest extends RequestTestBase {

	@Override
	protected GetPurchasesRequest newRequest() {
		return new GetPurchasesRequest("test", null);
	}

	@Test
	public void testShouldCreateFromOldRequest() throws Exception {
		final GetPurchasesRequest oldRequest = new GetPurchasesRequest("test", "first");
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
}