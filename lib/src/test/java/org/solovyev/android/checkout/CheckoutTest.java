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

import com.android.vending.billing.InAppBillingService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.solovyev.android.checkout.BillingTest.newPurchasesBundle;
import static org.solovyev.android.checkout.ProductTypes.IN_APP;
import static org.solovyev.android.checkout.ProductTypes.SUBSCRIPTION;
import static org.solovyev.android.checkout.ResponseCodes.BILLING_UNAVAILABLE;
import static org.solovyev.android.checkout.ResponseCodes.OK;
import static org.solovyev.android.checkout.Tests.newBilling;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class CheckoutTest {

    @Nonnull
    private InAppBillingService mService;
    @Nonnull
    private Checkout mCheckout;

    @Before
    public void setUp() throws Exception {
        Billing billing = newBilling();
        billing.connect();
        mService = ((TestServiceConnector) billing.getConnector()).mService;
        when(mService.isBillingSupported(eq(3), anyString(), eq(IN_APP))).thenReturn(OK);
        when(mService.isBillingSupported(eq(3), anyString(), eq(SUBSCRIPTION))).thenReturn(OK);
        mCheckout = Checkout.forApplication(billing);
    }

    @Test
    public void testAllProductsShouldBeSupported() throws Exception {
        final AwaitingListener l = new AwaitingListener();

        mCheckout.start(l);

        l.waitWhileLoading();

        verify(l.mListener, times(2)).onReady(any(BillingRequests.class), anyString(), eq(true));
        verify(l.mListener, never()).onReady(any(BillingRequests.class), anyString(), eq(false));

        verify(l.mListener).onReady(any(BillingRequests.class));
    }

    @Test
    public void testShouldLoadPurchasesWhenProductsBecameSupported() throws Exception {
        when(mService.isBillingSupported(eq(3), anyString(), eq(IN_APP))).thenReturn(BILLING_UNAVAILABLE);
        when(mService.isBillingSupported(eq(3), anyString(), eq(SUBSCRIPTION))).thenReturn(BILLING_UNAVAILABLE);
        when(mService.getPurchases(anyInt(), anyString(), anyString(), isNull(String.class))).thenReturn(newPurchasesBundle(0, false));

        mCheckout.start();
        final AwaitingCallback c1 = new AwaitingCallback();
        mCheckout.loadInventory(Inventory.Request.create().loadAllPurchases(), c1);
        c1.waitWhileLoading();

        assertFalse(c1.mProducts.get(IN_APP).supported);
        assertFalse(c1.mProducts.get(SUBSCRIPTION).supported);
        assertTrue(c1.mProducts.get(IN_APP).getPurchases().isEmpty());

        when(mService.isBillingSupported(eq(3), anyString(), eq(IN_APP))).thenReturn(OK);
        when(mService.isBillingSupported(eq(3), anyString(), eq(SUBSCRIPTION))).thenReturn(OK);

        final AwaitingCallback c2 = new AwaitingCallback();
        mCheckout.loadInventory(Inventory.Request.create().loadAllPurchases(), c2);
        c2.waitWhileLoading();

        assertTrue(c2.mProducts.get(IN_APP).supported);
        assertTrue(c2.mProducts.get(SUBSCRIPTION).supported);
        assertTrue(c2.mProducts.get(IN_APP).getPurchases().size() == 1);
    }

    private final static class AwaitingListener implements Checkout.Listener {

        @Nonnull
        private final CountDownLatch mLatch = new CountDownLatch(1);
        @Nonnull
        private final Checkout.Listener mListener = mock(Checkout.Listener.class);

        @Override
        public void onReady(@Nonnull BillingRequests requests) {
            mListener.onReady(requests);
            mLatch.countDown();
        }

        @Override
        public void onReady(@Nonnull BillingRequests requests, @Nonnull String product, boolean billingSupported) {
            mListener.onReady(requests, product, billingSupported);
        }

        void waitWhileLoading() throws InterruptedException {
            if (!mLatch.await(1, TimeUnit.SECONDS)) {
                fail("Waiting too long");
            }
        }
    }

    private static class AwaitingCallback implements Inventory.Callback {
        @Nonnull
        private final CountDownLatch mLatch = new CountDownLatch(1);
        @Nonnull
        private Inventory.Products mProducts = Inventory.Products.empty();

        @Override
        public void onLoaded(@Nonnull Inventory.Products products) {
            mProducts = products;
            mLatch.countDown();
        }

        void waitWhileLoading() throws InterruptedException {
            if (!mLatch.await(1, TimeUnit.SECONDS)) {
                fail("Waiting too long");
            }
        }
    }
}
