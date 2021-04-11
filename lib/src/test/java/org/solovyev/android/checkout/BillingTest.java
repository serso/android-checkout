package org.solovyev.android.checkout;

import com.android.vending.billing.InAppBillingServiceImpl;
import com.android.vending.billing.InAppBillingService;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.os.Bundle;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.solovyev.android.checkout.RequestTestBase.newBundle;
import static org.solovyev.android.checkout.ResponseCodes.OK;

@SuppressWarnings("unchecked")
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BillingTest {

    @Nonnull
    private Billing mBilling;
    @Nonnull
    private Random mRandom;

    @Nonnull
    static Bundle newPurchasesBundle(long id, boolean withContinuationToken) throws JSONException {
        return newPurchasesBundle(id, withContinuationToken, false);
    }

    @Nonnull
    static Bundle newPurchasesBundle(long id, boolean withContinuationToken, boolean lite) throws JSONException {
        final Bundle bundle = newBundle(OK);
        final ArrayList<String> list = new ArrayList<String>();
        list.add(PurchaseTest.newJsonObject(id, Purchase.State.PURCHASED, lite).toString());
        bundle.putStringArrayList(Purchases.BUNDLE_DATA_LIST, list);
        if (withContinuationToken) {
            bundle.putString(Purchases.BUNDLE_CONTINUATION_TOKEN, String.valueOf(id + 1));
        }
        return bundle;
    }

    @Before
    public void setUp() throws Exception {
        mBilling = Tests.newSynchronousBilling();
        mRandom = new Random(currentTimeMillis());
    }

    @Test
    public void testShouldNotifyErrorIfCantConnect() throws Exception {
        final Billing.ServiceConnector connector = mock(Billing.ServiceConnector.class);
        when(connector.connect()).thenReturn(false);

        mBilling.setConnector(connector);
        final RequestListener<Object> l = mock(RequestListener.class);
        mBilling.getRequests().isBillingSupported("p", l);
        verify(l, times(1)).onError(eq(ResponseCodes.SERVICE_NOT_CONNECTED), any(BillingException.class));
        verify(l, times(0)).onSuccess(any());
    }

    @Test
    public void testShouldNotifyErrorIfConnectorReturnedNull() throws Exception {
        final Billing.ServiceConnector connector = mock(Billing.ServiceConnector.class);
        when(connector.connect()).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                mBilling.setService(null, true);
                return true;
            }
        });

        mBilling.setConnector(connector);
        final RequestListener<Object> l = mock(RequestListener.class);
        mBilling.getRequests().isBillingSupported("p", l);
        verify(l, times(1)).onError(eq(ResponseCodes.SERVICE_NOT_CONNECTED), any(BillingException.class));
        verify(l, times(0)).onSuccess(any());
    }

    @Test
    public void testShouldExecuteRequestIfConnected() throws Exception {
        final Billing.ServiceConnector connector = mock(Billing.ServiceConnector.class);
        final InAppBillingService service = mock(InAppBillingServiceImpl.class);
        when(service.isBillingSupported(anyInt(), anyString(), anyString())).thenReturn(OK);
        when(connector.connect()).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                mBilling.setService(service, true);
                return true;
            }
        });

        mBilling.setConnector(connector);
        final RequestListener<Object> l = mock(RequestListener.class);
        mBilling.getRequests().isBillingSupported("p", l);
        verify(l, times(0)).onError(anyInt(), any(BillingException.class));
        verify(l, times(1)).onSuccess(any());
    }

    @Test
    public void testStates() throws Exception {
        final Billing.ServiceConnector connector = mock(Billing.ServiceConnector.class);
        when(connector.connect()).thenReturn(true);
        mBilling.setConnector(connector);
        mBilling.connect();

        assertEquals(Billing.State.CONNECTING, mBilling.getState());
        mBilling.setService(mock(InAppBillingServiceImpl.class), true);
        assertEquals(Billing.State.CONNECTED, mBilling.getState());
        mBilling.disconnect();
        assertEquals(Billing.State.DISCONNECTING, mBilling.getState());
        mBilling.setService(null, false);
        assertEquals(Billing.State.DISCONNECTED, mBilling.getState());
    }

    @Test
    public void testShouldDisconnectOnlyIfNotInInitialState() throws Exception {
        mBilling.setState(Billing.State.INITIAL);
        mBilling.setService(null, false);

        assertEquals(Billing.State.INITIAL, mBilling.getState());

        mBilling.setState(Billing.State.CONNECTING);
        mBilling.setState(Billing.State.CONNECTED);

        mBilling.setState(Billing.State.DISCONNECTING);
        mBilling.setService(null, false);

        assertEquals(Billing.State.DISCONNECTED, mBilling.getState());

        mBilling.setState(Billing.State.CONNECTING);
        mBilling.setState(Billing.State.CONNECTED);
        mBilling.setService(null, false);

        assertEquals(Billing.State.DISCONNECTED, mBilling.getState());
    }

    @Test
    public void testShouldConnectOnlyIfConnecting() throws Exception {
        mBilling.setState(Billing.State.CONNECTING);
        mBilling.setState(Billing.State.FAILED);
        mBilling.setService(mock(InAppBillingServiceImpl.class), true);

        assertEquals(Billing.State.FAILED, mBilling.getState());

        mBilling.setState(Billing.State.CONNECTING);
        mBilling.setService(mock(InAppBillingServiceImpl.class), true);

        assertEquals(Billing.State.CONNECTED, mBilling.getState());
    }

    @Test
    public void testShouldJumpToDisconnectedStateIfWasConnecting() throws Exception {
        mBilling.setState(Billing.State.CONNECTING);
        mBilling.setService(null, false);

        assertEquals(Billing.State.FAILED, mBilling.getState());
    }

    @Test
    public void testShouldDisconnectServiceIfBillingIsInactive() throws Exception {
        final Billing.ServiceConnector connector = mock(Billing.ServiceConnector.class);
        mBilling.setConnector(connector);
        mBilling.setState(Billing.State.CONNECTING);
        mBilling.setState(Billing.State.DISCONNECTED);

        mBilling.setService(mock(InAppBillingServiceImpl.class), true);

        assertEquals(Billing.State.DISCONNECTED, mBilling.getState());
        verify(connector, times(1)).disconnect();
    }

    @Test
    public void testShouldGoToDisconnectedStateFromConnectingIfBillingDies() throws Exception {
        mBilling.setState(Billing.State.CONNECTING);
        mBilling.disconnect();

        assertEquals(Billing.State.DISCONNECTED, mBilling.getState());
    }

    @Test
    public void testShouldRunAllRequests() throws Exception {
        final int REQUESTS = 100;
        final int SLEEP = 10;

        final Billing b = Tests.newBilling(false);
        b.setMainThread(Tests.sameThreadExecutor());
        final AsyncServiceConnector c = new AsyncServiceConnector(b);
        b.setConnector(c);
        final CountDownLatch latch = new CountDownLatch(REQUESTS);
        final RequestListener l = new CountDownListener(latch);
        for (int i = 0; i < REQUESTS; i++) {
            if (i % 10 == 0) {
                if (mRandom.nextBoolean()) {
                    b.connect();
                } else {
                    // connector is called directly in order to avoid cancelling the pending
                    // requests
                    c.disconnect();
                }
            }
            b.runWhenConnected(new SleepingRequest(mRandom.nextInt(SLEEP)), l, null);
        }
        b.connect();
        assertTrue(latch.await(SLEEP * REQUESTS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testShouldCancelRequests() throws Exception {
        final int REQUESTS = 10;

        final Billing b = Tests.newBilling(false);
        final CountDownLatch latch = new CountDownLatch(REQUESTS / 2);
        final RequestListener l = new CountDownListener(latch);
        final List<Integer> requestIds = new ArrayList<Integer>();
        for (int i = 0; i < REQUESTS; i++) {
            requestIds.add(b.runWhenConnected(new SleepingRequest(100), l, null));
        }
        Thread.sleep(100 * (REQUESTS / 2 - 1));
        for (int i = REQUESTS / 2; i < REQUESTS; i++) {
            b.cancel(requestIds.get(i));
        }
        assertTrue(latch.await(1, SECONDS));
    }

    @Test
    public void testIsPurchasedShouldCollectAllThePurchases() throws Exception {
        checkIsPurchased("0", true);
        checkIsPurchased("1", true);
        checkIsPurchased("2", true);
        checkIsPurchased("3", true);
        checkIsPurchased("4", true);
        checkIsPurchased("-1", false);
        checkIsPurchased("5", false);
    }

    @Test
    public void testShouldLoadAllPurchases() throws Exception {
        final Billing billing = prepareMultiPurchasesBilling();

        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownListener l = new CountDownListener(latch);
        billing.getRequests().getAllPurchases(ProductTypes.IN_APP, l);

        assertTrue(latch.await(1, SECONDS));
        verify(l.listener).onSuccess(argThat(new PurchasesMatcher()));
    }

    @Test
    public void testShouldLoadWholePurchaseHistory() throws Exception {
        final Billing billing = prepareMultiPurchasesBilling();

        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownListener l = new CountDownListener(latch);
        billing.getRequests().getWholePurchaseHistory(ProductTypes.IN_APP, null, l);

        assertTrue(latch.await(1, SECONDS));
        verify(l.listener).onSuccess(argThat(new PurchasesMatcher()));
    }

    @Test
    public void testShouldCancelIsPurchasedListener() throws Exception {
        final Billing billing = Tests.newBilling(true);

        final CountDownLatch requestWaiter = new CountDownLatch(1);
        final CountDownLatch cancelWaiter = new CountDownLatch(1);

        final InAppBillingService service = mock(InAppBillingServiceImpl.class);
        when(service.getPurchases(anyInt(), anyString(), anyString(), isNull(String.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                requestWaiter.countDown();
                return newPurchasesBundle(0, true);
            }
        });
        when(service.getPurchases(anyInt(), anyString(), anyString(), eq("1"))).thenAnswer(new Answer<Bundle>() {
            @Override
            public Bundle answer(InvocationOnMock invocation) throws Throwable {
                cancelWaiter.await(1, SECONDS);
                return newPurchasesBundle(1, false);
            }
        });
        Tests.setService(billing, service);

        final RequestListener l = mock(RequestListener.class);
        final BillingRequests requests = billing.getRequests();
        requests.isPurchased(ProductTypes.IN_APP, "1", l);

        requestWaiter.await(1, SECONDS);
        requests.cancelAll();
        cancelWaiter.countDown();

        verify(l, never()).onSuccess(anyObject());
        verify(l, never()).onError(anyInt(), any(Exception.class));
    }

    private void checkIsPurchased(@Nonnull String id, boolean purchased) throws RemoteException, JSONException, InterruptedException {
        final Billing billing = prepareMultiPurchasesBilling();

        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownListener l = new CountDownListener(latch);

        billing.getRequests().isPurchased(ProductTypes.IN_APP, id, l);

        assertTrue(latch.await(1, SECONDS));
        verify(l.listener).onSuccess(eq(purchased));
        verify(l.listener, never()).onSuccess(eq(!purchased));
    }

    @Nonnull
    private Billing prepareMultiPurchasesBilling() throws RemoteException, JSONException {
        final Billing billing = Tests.newBilling(true);
        prepareMultiPurchasesService(billing);
        return billing;
    }

    private void prepareMultiPurchasesService(@Nonnull Billing billing) throws RemoteException, JSONException {
        final InAppBillingService service = mock(InAppBillingServiceImpl.class);

        when(service.getPurchases(anyInt(), anyString(), anyString(), isNull(String.class))).thenReturn(newPurchasesBundle(0, true));
        when(service.getPurchases(anyInt(), anyString(), anyString(), eq("1"))).thenReturn(newPurchasesBundle(1, true));
        when(service.getPurchases(anyInt(), anyString(), anyString(), eq("2"))).thenReturn(newPurchasesBundle(2, true));
        when(service.getPurchases(anyInt(), anyString(), anyString(), eq("3"))).thenReturn(newPurchasesBundle(3, true));
        when(service.getPurchases(anyInt(), anyString(), anyString(), eq("4"))).thenReturn(newPurchasesBundle(4, false));

        when(service.getPurchaseHistory(anyInt(), anyString(), anyString(), isNull(String.class), any(Bundle.class))).thenReturn(newPurchasesBundle(0, true, true));
        when(service.getPurchaseHistory(anyInt(), anyString(), anyString(), eq("1"), any(Bundle.class))).thenReturn(newPurchasesBundle(1, true, true));
        when(service.getPurchaseHistory(anyInt(), anyString(), anyString(), eq("2"), any(Bundle.class))).thenReturn(newPurchasesBundle(2, true, true));
        when(service.getPurchaseHistory(anyInt(), anyString(), anyString(), eq("3"), any(Bundle.class))).thenReturn(newPurchasesBundle(3, true, true));
        when(service.getPurchaseHistory(anyInt(), anyString(), anyString(), eq("4"), any(Bundle.class))).thenReturn(newPurchasesBundle(4, false, true));

        Tests.setService(billing, service);
    }

    @Test
    public void testShouldAutoDisconnect() throws Exception {
        final Billing billing = Tests.newBilling(true, true);
        assertTrue(billing.getState() == Billing.State.INITIAL);
        billing.onCheckoutStarted();
        assertTrue(billing.getState() == Billing.State.CONNECTED);
        billing.onCheckoutStarted();
        assertTrue(billing.getState() == Billing.State.CONNECTED);
        billing.onCheckoutStopped();
        assertTrue(billing.getState() == Billing.State.CONNECTED);
        billing.onCheckoutStopped();
        assertTrue(billing.getState() == Billing.State.DISCONNECTED);
    }

    private static class CountDownListener<R> implements RequestListener<R> {

        private final CountDownLatch latch;
        private final RequestListener<R> listener;

        public CountDownListener(CountDownLatch latch) {
            this.latch = latch;
            this.listener = mock(RequestListener.class);
        }

        @Override
        public void onSuccess(@Nonnull R result) {
            listener.onSuccess(result);
            onEnd();
        }

        private void onEnd() {
            latch.countDown();
        }

        @Override
        public void onError(int response, @Nonnull Exception e) {
            listener.onError(response, e);
            onEnd();
        }
    }

    private static class PurchasesMatcher extends BaseMatcher<Purchases> {
        @Override
        public boolean matches(Object item) {
            if (!(item instanceof Purchases)) {
                return false;
            }
            final Purchases purchases = (Purchases) item;
            for (Integer id : asList(0, 1, 2, 3, 4)) {
                if (!purchases.hasPurchaseInState(String.valueOf(id), Purchase.State.PURCHASED)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void describeTo(Description description) {
        }
    }

    private final class SleepingRequest extends Request {

        private final long sleep;

        private SleepingRequest(long sleep) {
            super(RequestType.BILLING_SUPPORTED);
            this.sleep = sleep;
        }

        @Override
        void start(@Nonnull InAppBillingService service, @Nonnull String packageName) throws RemoteException, RequestException {
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
