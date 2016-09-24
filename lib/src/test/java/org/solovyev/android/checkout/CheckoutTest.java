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
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static java.util.Arrays.asList;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.solovyev.android.checkout.ProductTypes.IN_APP;
import static org.solovyev.android.checkout.ProductTypes.SUBSCRIPTION;
import static org.solovyev.android.checkout.ResponseCodes.OK;
import static org.solovyev.android.checkout.Tests.newBilling;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class CheckoutTest {

    @Nonnull
    private Billing billing;

    @Nonnull
    private Checkout checkout;

    @Before
    public void setUp() throws Exception {
        billing = newBilling();
        billing.connect();
        final IInAppBillingService service = ((TestServiceConnector) billing.getConnector()).service;
        when(service.isBillingSupported(eq(3), anyString(), eq(IN_APP))).thenReturn(OK);
        when(service.isBillingSupported(eq(3), anyString(), eq(SUBSCRIPTION))).thenReturn(OK);
        final Inventory.SkuIds skuIds = Inventory.SkuIds.create()
                .add(IN_APP, asList("1", "2", "3", "4", "6"))
                .add(SUBSCRIPTION, asList("sub1", "sub2", "sub3", "sub4"));
        checkout = Checkout.forApplication(billing, skuIds.getProducts());
    }

    @Test
    public void testAllProductsShouldBeSupported() throws Exception {
        final CountDownListener l = new CountDownListener();

        checkout.whenReady(l);
        checkout.start();

        l.waitWhileLoading();

        verify(l.listener, times(2)).onReady(any(BillingRequests.class), anyString(), eq(true));
        verify(l.listener, never()).onReady(any(BillingRequests.class), anyString(), eq(false));

        verify(l.listener).onReady(any(BillingRequests.class));
    }

    private final static class CountDownListener implements Checkout.Listener {

        @Nonnull
        private final CountDownLatch latch = new CountDownLatch(1);

        @Nonnull
        private final Checkout.Listener listener = mock(Checkout.Listener.class);

        @Override
        public void onReady(@Nonnull BillingRequests requests) {
            listener.onReady(requests);
            latch.countDown();
        }

        @Override
        public void onReady(@Nonnull BillingRequests requests, @Nonnull String product, boolean billingSupported) {
            listener.onReady(requests, product, billingSupported);
        }

        void waitWhileLoading() throws InterruptedException {
            if (!latch.await(1, TimeUnit.SECONDS)) {
                fail("Waiting too long");
            }
        }
    }
}