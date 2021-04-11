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

import com.android.vending.billing.InAppBillingServiceImpl;
import com.android.vending.billing.InAppBillingService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;

import javax.annotation.Nonnull;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.solovyev.android.checkout.ResponseCodes.BILLING_UNAVAILABLE;
import static org.solovyev.android.checkout.ResponseCodes.OK;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
abstract class RequestTestBase {

    @Nonnull
    static Bundle newBundle(int response) {
        final Bundle bundle = new Bundle();
        bundle.putInt("RESPONSE_CODE", response);
        return bundle;
    }

    @Test
    public void testShouldError() throws Exception {
        final Request request = newRequest();
        final RequestListener l = mock(RequestListener.class);
        request.setListener(l);

        final Bundle bundle = newBundle(BILLING_UNAVAILABLE);

        final InAppBillingService service = mock(InAppBillingServiceImpl.class);
        when(service.isBillingSupported(anyInt(), anyString(), anyString())).thenReturn(BILLING_UNAVAILABLE);
        when(service.consumePurchase(anyInt(), anyString(), anyString())).thenReturn(BILLING_UNAVAILABLE);
        when(service.getPurchases(anyInt(), anyString(), anyString(), anyString())).thenReturn(bundle);
        when(service.getPurchaseHistory(anyInt(), anyString(), anyString(), anyString(), any(Bundle.class))).thenReturn(bundle);
        when(service.getSkuDetails(anyInt(), anyString(), anyString(), any(Bundle.class))).thenReturn(bundle);
        when(service.getBuyIntent(anyInt(), anyString(), anyString(), anyString(), anyString())).thenReturn(bundle);

        request.start(service, "testse");

        verify(l).onError(eq(BILLING_UNAVAILABLE), any(Exception.class));
        verify(l, never()).onSuccess(any());
    }

    @Test
    public void testShouldSuccess() throws Exception {
        final Request r = newRequest();
        final RequestListener l = mock(RequestListener.class);
        r.setListener(l);
        final InAppBillingService service = mock(InAppBillingServiceImpl.class);

        when(service.isBillingSupported(anyInt(), anyString(), anyString())).thenReturn(OK);
        when(service.consumePurchase(anyInt(), anyString(), anyString())).thenReturn(OK);
        final Bundle purchases = new Bundle();
        purchases.putStringArrayList("INAPP_PURCHASE_DATA_LIST", new ArrayList<String>());
        when(service.getPurchases(anyInt(), anyString(), anyString(), anyString())).thenReturn(purchases);
        when(service.getPurchaseHistory(anyInt(), anyString(), anyString(), anyString(), any(Bundle.class))).thenReturn(purchases);
        final Bundle skuDetails = new Bundle();
        skuDetails.putStringArrayList("DETAILS_LIST", new ArrayList<String>());
        when(service.getSkuDetails(anyInt(), anyString(), anyString(), any(Bundle.class))).thenReturn(skuDetails);
        final Bundle buyIntent = new Bundle();
        buyIntent.putParcelable("BUY_INTENT", PendingIntent.getActivity(RuntimeEnvironment.application, 100, new Intent(), 0));
        when(service.getBuyIntent(anyInt(), anyString(), anyString(), anyString(), anyString())).thenReturn(buyIntent);

        r.start(service, "");

        verify(l).onSuccess(anyObject());
        verify(l, never()).onError(anyInt(), any(Exception.class));
    }

    @Test
    public void testShouldHaveSameCacheKeys() throws Exception {
        final Request r1 = newRequest();
        final Request r2 = newRequest();

        assertEquals(r1.getCacheKey(), r2.getCacheKey());
    }

    protected abstract Request newRequest();
}
