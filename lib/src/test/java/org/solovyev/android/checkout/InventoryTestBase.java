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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import javax.annotation.Nonnull;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.solovyev.android.checkout.ProductTypes.IN_APP;
import static org.solovyev.android.checkout.ProductTypes.SUBSCRIPTION;
import static org.solovyev.android.checkout.Purchase.State.CANCELLED;
import static org.solovyev.android.checkout.Purchase.State.EXPIRED;
import static org.solovyev.android.checkout.Purchase.State.PURCHASED;
import static org.solovyev.android.checkout.Purchase.State.REFUNDED;
import static org.solovyev.android.checkout.PurchaseTest.verifyPurchase;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public abstract class InventoryTestBase {

    @Nonnull
    protected Billing mBilling;
    @Nonnull
    protected Checkout mCheckout;
    @Nonnull
    protected Inventory mInventory;
    @Nonnull
    protected Inventory.Request mRequest;

    @Before
    public void setUp() throws Exception {
        mBilling = newBilling();
        mRequest = Inventory.Request.create()
                .loadAllPurchases()
                .loadSkus(IN_APP, asList("1", "2", "3", "4", "6"))
                .loadSkus(SUBSCRIPTION, asList("sub1", "sub2", "sub3", "sub4"));
        mCheckout = Checkout.forApplication(mBilling);
        mInventory = newInventory(mCheckout);
    }

    @Nonnull
    protected Billing newBilling() {
        return Tests.newBilling();
    }

    @Nonnull
    protected abstract Inventory newInventory(@Nonnull Checkout checkout);

    @Test
    public void testShouldLoadPurchases() throws Exception {
        populatePurchases();
        populateSkus();

        final TestCallback listener = new TestCallback();
        mCheckout.start();
        mInventory.load(mRequest, listener);

        Tests.waitWhileLoading(mInventory);

        final boolean complete = shouldVerifyPurchaseCompletely();

        final Inventory.Product inApp = listener.mProducts.get(IN_APP);
        final List<Purchase> actualInApps = inApp.getPurchases();
        assertEquals(4, actualInApps.size());

        verifyPurchase(actualInApps.get(0), 4, EXPIRED, complete, false);
        verifyPurchase(actualInApps.get(1), 3, REFUNDED, complete, false);
        verifyPurchase(actualInApps.get(2), 2, CANCELLED, complete, false);
        verifyPurchase(actualInApps.get(3), 1, PURCHASED, complete, false);

        final Inventory.Product sub = listener.mProducts.get(SUBSCRIPTION);
        final List<Purchase> actualSubs = sub.getPurchases();
        assertEquals(4, actualSubs.size());

        verifyPurchase(actualSubs.get(0), 4, EXPIRED, complete, true);
        verifyPurchase(actualSubs.get(1), 3, REFUNDED, complete, true);
        verifyPurchase(actualSubs.get(2), 2, CANCELLED, complete, true);
        verifyPurchase(actualSubs.get(3), 1, PURCHASED, complete, true);
    }

    protected void populatePurchases() throws Exception {
        final List<Purchase> expectedInApps = asList(
                Purchase.fromJson(PurchaseTest.newJson(1, PURCHASED), ""),
                Purchase.fromJson(PurchaseTest.newJson(2, CANCELLED), ""),
                Purchase.fromJson(PurchaseTest.newJson(3, REFUNDED), ""),
                Purchase.fromJson(PurchaseTest.newJson(4, EXPIRED), "")
        );
        insertPurchases(IN_APP, expectedInApps);

        final List<Purchase> expectedSubs = asList(
                Purchase.fromJson(PurchaseTest.newJsonSubscription(1, PURCHASED), ""),
                Purchase.fromJson(PurchaseTest.newJsonSubscription(2, CANCELLED), ""),
                Purchase.fromJson(PurchaseTest.newJsonSubscription(3, REFUNDED), ""),
                Purchase.fromJson(PurchaseTest.newJsonSubscription(4, EXPIRED), "")
        );
        insertPurchases(SUBSCRIPTION, expectedSubs);
    }

    protected void populateSkus() throws Exception {
        final List<Sku> expectedInAppSkus = asList(Sku.fromJson(SkuTest.newInAppJson("1"), IN_APP),
                Sku.fromJson(SkuTest.newInAppJson("2"), IN_APP),
                Sku.fromJson(SkuTest.newInAppJson("3"), IN_APP),
                Sku.fromJson(SkuTest.newInAppJson("4"), IN_APP),
                Sku.fromJson(SkuTest.newInAppJson("6"), IN_APP));
        insertSkus(IN_APP, expectedInAppSkus);

        final List<Sku> expectedSubSkus = asList(
                Sku.fromJson(SkuTest.newSubscriptionJson("sub1"), SUBSCRIPTION),
                Sku.fromJson(SkuTest.newSubscriptionJson("sub2"), SUBSCRIPTION),
                Sku.fromJson(SkuTest.newSubscriptionJson("sub3"), SUBSCRIPTION),
                Sku.fromJson(SkuTest.newSubscriptionJson("sub4"), SUBSCRIPTION));
        insertSkus(SUBSCRIPTION, expectedSubSkus);
    }

    @Test
    public void testShouldLoadSeveralTasksAsynchronously() throws Exception {
        populatePurchases();
        populateSkus();

        mCheckout.start();

        final CountingCallback c = new CountingCallback();
        mInventory.load(Inventory.Request.create().loadPurchases(IN_APP), c);
        mInventory.load(Inventory.Request.create().loadPurchases(SUBSCRIPTION), c);
        mInventory.load(Inventory.Request.create().loadSkus(SUBSCRIPTION, asList("sub1", "sub2", "sub3", "sub4")), c);
        mInventory.load(Inventory.Request.create().loadSkus(IN_APP, asList("1", "2", "3", "4", "6")), c);
        mInventory.load(mRequest, c);

        Tests.waitWhileLoading(mInventory);

        assertEquals(5, c.mCount);
    }

    @Test
    public void testShouldCancelTaskById() throws Exception {
        mCheckout.start();
        final int task1 = mInventory.load(mRequest, mock(Inventory.Callback.class));
        final int task2 = mInventory.load(mRequest, mock(Inventory.Callback.class));
        Assert.assertTrue(mInventory.isLoading());
        mInventory.cancel(task1);
        Assert.assertTrue(mInventory.isLoading());
        mInventory.cancel(task2);
        Assert.assertFalse(mInventory.isLoading());
    }

    @Test
    public void testShouldCancelAllTasks() throws Exception {
        mCheckout.start();
        mInventory.load(mRequest, mock(Inventory.Callback.class));
        mInventory.load(mRequest, mock(Inventory.Callback.class));
        Assert.assertTrue(mInventory.isLoading());
        mInventory.cancel();
        Assert.assertFalse(mInventory.isLoading());
    }

    @Test
    public void testCanceledCallbackShouldNotGetCalled() throws Exception {
        mCheckout.start();
        final Inventory.Callback c1 = mock(Inventory.Callback.class);
        final Inventory.Callback c2 = mock(Inventory.Callback.class);
        final int task1 = mInventory.load(mRequest, c1);
        mInventory.load(mRequest, c2);

        mInventory.cancel(task1);

        Tests.waitWhileLoading(mInventory);
        verify(c1, never()).onLoaded(anyProducts());
        verify(c2, times(1)).onLoaded(anyProducts());
    }

    protected abstract boolean shouldVerifyPurchaseCompletely();

    protected abstract void insertPurchases(@Nonnull String product, @Nonnull List<Purchase> purchases) throws Exception;

    protected abstract void insertSkus(@Nonnull String product, @Nonnull List<Sku> skus) throws Exception;

    @Test
    public void testShouldCallListenerWhenLoaded() throws Exception {
        final Inventory.Callback c = mock(Inventory.Callback.class);

        mCheckout.start();
        mInventory.load(mRequest, c);
        Tests.waitWhileLoading(mInventory);

        verify(c, times(1)).onLoaded(anyProducts());
    }

    @Nonnull
    private Inventory.Products anyProducts() {
        return any(Inventory.Products.class);
    }

    @Test
    public void testListenerShouldBeCalledOnlyOnce() throws Exception {
        final Inventory.Callback l = mock(Inventory.Callback.class);
        mCheckout.start();
        mInventory.load(mRequest, mock(Inventory.Callback.class));
        mInventory.load(mRequest, mock(Inventory.Callback.class));
        mInventory.load(mRequest, l);
        Tests.waitWhileLoading(mInventory);
        mInventory.load(mRequest, mock(Inventory.Callback.class));
        mInventory.load(mRequest, mock(Inventory.Callback.class));
        verify(l, times(1)).onLoaded(anyProducts());
    }

    private static class CountingCallback implements Inventory.Callback {
        int mCount = 0;

        @Override
        public void onLoaded(@Nonnull Inventory.Products products) {
            mCount++;
        }
    }

    static class TestCallback implements Inventory.Callback {
        @Nonnull
        volatile Inventory.Products mProducts;

        @Override
        public void onLoaded(@Nonnull Inventory.Products products) {
            this.mProducts = products;
        }
    }
}