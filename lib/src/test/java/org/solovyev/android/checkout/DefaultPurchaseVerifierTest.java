package org.solovyev.android.checkout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DefaultPurchaseVerifierTest {
    @Nonnull
    private DefaultPurchaseVerifier mVerifier;

    @Before
    public void setUp() throws Exception {
        mVerifier = new DefaultPurchaseVerifier("PK");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testShouldAutoVerifyTestPurchases() throws Exception {
        final List<Purchase> testPurchases = new ArrayList<>();
        for (String sku : DefaultPurchaseVerifier.TEST_SKUS) {
            testPurchases.add(new Purchase(sku, "", "", 0, 0, "", "", false, "", ""));
        }
        final RequestListener<List<Purchase>> listener = Mockito.mock(RequestListener.class);
        mVerifier.verify(testPurchases, listener);
        Mockito.verify(listener).onSuccess(Mockito.eq(testPurchases));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testShouldNotAutoVerifyRealPurchases() throws Exception {
        final List<Purchase> purchases = Collections.singletonList(
                new Purchase("android.test.boom", "", "", 0, 0, "", "", false, "", ""));
        final RequestListener<List<Purchase>> listener = Mockito.mock(RequestListener.class);
        mVerifier.verify(purchases, listener);
        Mockito.verify(listener).onSuccess(Mockito.eq(Collections.<Purchase>emptyList()));
    }
}