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
        Looper.prepareMainLooper();

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
        }
    }

    private static class CountDownLatchRequestListener implements RequestListener<List<Purchase>> {
        @Nonnull
        private final CountDownLatch mLatch = new CountDownLatch(1);

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