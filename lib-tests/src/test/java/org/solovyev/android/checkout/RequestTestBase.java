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
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.solovyev.android.checkout.ResponseCodes.BILLING_UNAVAILABLE;

@RunWith(CheckoutTestRunner.class)
abstract class RequestTestBase {

	@Test
	public void testShouldErrorIfErrorResponse() throws Exception {
		final Request request = newRequest();
		final RequestListener l = mock(RequestListener.class);
		request.setListener(l);

		final Bundle bundle = new Bundle();
		bundle.putInt("RESPONSE_CODE", BILLING_UNAVAILABLE);

		final IInAppBillingService service = mock(IInAppBillingService.class);
		when(service.isBillingSupported(anyInt(), anyString(), anyString())).thenReturn(BILLING_UNAVAILABLE);
		when(service.consumePurchase(anyInt(), anyString(), anyString())).thenReturn(BILLING_UNAVAILABLE);
		when(service.getPurchases(anyInt(), anyString(), anyString(), anyString())).thenReturn(bundle);
		when(service.getSkuDetails(anyInt(), anyString(), anyString(), any(Bundle.class))).thenReturn(bundle);
		when(service.getBuyIntent(anyInt(), anyString(), anyString(), anyString(), anyString())).thenReturn(bundle);

		request.start(service, 3, "testse");

		verify(l).onError(eq(BILLING_UNAVAILABLE), any(Exception.class));
		verify(l, never()).onSuccess(any());
	}

	protected abstract Request newRequest();
}
