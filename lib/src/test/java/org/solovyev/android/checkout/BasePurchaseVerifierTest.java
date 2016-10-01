package org.solovyev.android.checkout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BasePurchaseVerifierTest {

    @Test
    public void testVerifierIsCalledOnBackgroundThreadFromMainThread() throws Exception {
        final ThreadAwareVerifier verifier = new ThreadAwareVerifier();
        final CountDownLatchRequestListener listener = new CountDownLatchRequestListener();

        verifier.verify(new ArrayList<Purchase>(), listener);
        listener.mLatch.await(1, TimeUnit.SECONDS);

        assertEquals("TestThread", verifier.mThreadName);
    }

    @Test
    public void testVerifierIsCalledOnTheSameBackgroundThread() throws Exception {
        final ThreadAwareVerifier verifier = new ThreadAwareVerifier();
        final CountDownLatchRequestListener listener = new CountDownLatchRequestListener();

        new Thread(new Runnable() {
            @Override
            public void run() {
                verifier.verify(new ArrayList<Purchase>(), listener);
            }
        }, "BackgroundThread").start();
        listener.mLatch.await(1, TimeUnit.SECONDS);

        assertEquals("BackgroundThread", verifier.mThreadName);
    }

    @Test
    public void testVerifierCallsListenerMethodsOnMainThreadFromMainThread() throws Exception {
        final ThreadAwareVerifier verifier = new ThreadAwareVerifier();
        final ThreadAwareRequestListener listener = new ThreadAwareRequestListener();

        verifier.verify(new ArrayList<Purchase>(), listener);
        listener.mLatch.await(1, TimeUnit.SECONDS);

        // !!! Doesn't work now as we sleep on the main thread ^^^ while listener await executing on it
        //assertEquals(Thread.currentThread().getName(), verifier.mThreadName);
    }

    @Test
    public void testVerifierCallsListenerMethodsOnTheSameBackgroundThread() throws Exception {
        final ThreadAwareVerifier verifier = new ThreadAwareVerifier();
        final ThreadAwareRequestListener listener = new ThreadAwareRequestListener();

        new Thread(new Runnable() {
            @Override
            public void run() {
                verifier.verify(new ArrayList<Purchase>(), listener);
            }
        }, "BackgroundThread").start();
        listener.mLatch.await(1, TimeUnit.SECONDS);

        assertEquals("BackgroundThread", listener.mThreadName);
    }

    private static final class ThreadAwareVerifier extends BasePurchaseVerifier {
        @Nonnull
        private String mThreadName;

        public ThreadAwareVerifier() {
            super(new Handler(Looper.getMainLooper()), 1, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "TestThread");
                }
            });
        }

        @Override
        protected void doVerify(@Nonnull List<Purchase> purchases, @Nonnull RequestListener<List<Purchase>> listener) {
            mThreadName = Thread.currentThread().getName();
            listener.onSuccess(purchases);
        }
    }

    private static class ThreadAwareRequestListener extends CountDownLatchRequestListener {
        @Nonnull
        private String mThreadName;

        @Override
        public void onSuccess(@Nonnull List<Purchase> result) {
            mThreadName = Thread.currentThread().getName();
            super.onSuccess(result);
        }

        @Override
        public void onError(int response, @Nonnull Exception e) {
            mThreadName = Thread.currentThread().getName();
            super.onError(response, e);
        }
    }

    private static class CountDownLatchRequestListener implements RequestListener<List<Purchase>> {
        @Nonnull
        final CountDownLatch mLatch = new CountDownLatch(1);

        @Override
        public void onSuccess(@Nonnull List<Purchase> result) {
            mLatch.countDown();
        }

        @Override
        public void onError(int response, @Nonnull Exception e) {
            mLatch.countDown();
        }
    }
}