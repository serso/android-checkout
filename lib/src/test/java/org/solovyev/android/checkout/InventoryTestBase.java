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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import javax.annotation.Nonnull;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public abstract class InventoryTestBase {

    private static class CountingCallback implements Inventory.Callback {
        int mCount = 0;

        @Override
        public void onLoaded(@Nonnull Inventory.Products products) {
            mCount++;
        }
    }

    @Nonnull
    protected Billing billing;

    @Nonnull
    protected Checkout checkout;

    @Nonnull
    protected Inventory inventory;

    @Nonnull
    protected Inventory.Request mRequest;

    @Before
    public void setUp() throws Exception {
        billing = newBilling();
        mRequest = Inventory.Request.create()
                .loadAllPurchases()
                .loadSkus(IN_APP, asList("1", "2", "3", "4", "6"))
                .loadSkus(SUBSCRIPTION, asList("sub1", "sub2", "sub3", "sub4"));
        checkout = Checkout.forApplication(billing);
        inventory = newInventory(checkout);
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
        checkout.start();
        inventory.load(mRequest, listener);

        Tests.waitWhileLoading(inventory);

        final boolean complete = shouldVerifyPurchaseCompletely();

        final Inventory.Product inApp = listener.products.get(IN_APP);
        final List<Purchase> actualInApps = inApp.getPurchases();
        assertEquals(4, actualInApps.size());

        verifyPurchase(actualInApps.get(0), 4, EXPIRED, complete, false);
        verifyPurchase(actualInApps.get(1), 3, REFUNDED, complete, false);
        verifyPurchase(actualInApps.get(2), 2, CANCELLED, complete, false);
        verifyPurchase(actualInApps.get(3), 1, PURCHASED, complete, false);

        final Inventory.Product sub = listener.products.get(SUBSCRIPTION);
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
        final List<Sku> expectedInAppSkus = asList(
                Sku.fromJson(SkuTest.newJson("1"), IN_APP),
                Sku.fromJson(SkuTest.newJson("2"), IN_APP),
                Sku.fromJson(SkuTest.newJson("3"), IN_APP),
                Sku.fromJson(SkuTest.newJson("4"), IN_APP),
                Sku.fromJson(SkuTest.newJson("6"), IN_APP)
        );
        insertSkus(IN_APP, expectedInAppSkus);

        final List<Sku> expectedSubSkus = asList(
                Sku.fromJson(SkuTest.newJson("sub1"), SUBSCRIPTION),
                Sku.fromJson(SkuTest.newJson("sub2"), SUBSCRIPTION),
                Sku.fromJson(SkuTest.newJson("sub3"), SUBSCRIPTION),
                Sku.fromJson(SkuTest.newJson("sub4"), SUBSCRIPTION)
        );
        insertSkus(SUBSCRIPTION, expectedSubSkus);
    }

    @Test
    public void testShouldLoadSeveralTasksAsynchronously() throws Exception {
        populatePurchases();
        populateSkus();

        checkout.start();

        final CountingCallback c = new CountingCallback();
        inventory.load(Inventory.Request.create().loadPurchases(IN_APP), c);
        inventory.load(Inventory.Request.create().loadPurchases(SUBSCRIPTION), c);
        inventory.load(Inventory.Request.create().loadSkus(SUBSCRIPTION, asList("sub1", "sub2", "sub3", "sub4")), c);
        inventory.load(Inventory.Request.create().loadSkus(IN_APP, asList("1", "2", "3", "4", "6")), c);
        inventory.load(mRequest, c);

        Tests.waitWhileLoading(inventory);

        assertEquals(5, c.mCount);
    }

    @Test
    public void testShouldCancelTaskById() throws Exception {
        checkout.start();
        final int task1 = inventory.load(mRequest, mock(Inventory.Callback.class));
        final int task2 = inventory.load(mRequest, mock(Inventory.Callback.class));
        Assert.assertTrue(inventory.isLoading());
        inventory.cancel(task1);
        Assert.assertTrue(inventory.isLoading());
        inventory.cancel(task2);
        Assert.assertFalse(inventory.isLoading());
    }

    @Test
    public void testShouldCancelAllTasks() throws Exception {
        checkout.start();
        inventory.load(mRequest, mock(Inventory.Callback.class));
        inventory.load(mRequest, mock(Inventory.Callback.class));
        Assert.assertTrue(inventory.isLoading());
        inventory.cancel();
        Assert.assertFalse(inventory.isLoading());
    }

    @Test
    public void testCanceledCallbackShouldNotGetCalled() throws Exception {
        checkout.start();
        final Inventory.Callback c1 = mock(Inventory.Callback.class);
        final Inventory.Callback c2 = mock(Inventory.Callback.class);
        final int task1 = inventory.load(mRequest, c1);
        inventory.load(mRequest, c2);

        inventory.cancel(task1);

        Tests.waitWhileLoading(inventory);
        verify(c1, never()).onLoaded(anyProducts());
        verify(c2, times(1)).onLoaded(anyProducts());
    }

    protected abstract boolean shouldVerifyPurchaseCompletely();

    protected abstract void insertPurchases(@Nonnull String product, @Nonnull List<Purchase> purchases) throws Exception;
    protected abstract void insertSkus(@Nonnull String product, @Nonnull List<Sku> skus) throws Exception;

    @Test
    public void testShouldCallListenerWhenLoaded() throws Exception {
        final Inventory.Callback c = mock(Inventory.Callback.class);

        checkout.start();
        inventory.load(mRequest, c);
        Tests.waitWhileLoading(inventory);

        verify(c, times(1)).onLoaded(anyProducts());
    }

    @Nonnull
    private Inventory.Products anyProducts() {
        return any(Inventory.Products.class);
    }

    @Test
    public void testListenerShouldBeCalledOnlyOnce() throws Exception {
        final Inventory.Callback l = mock(Inventory.Callback.class);
        checkout.start();
        inventory.load(mRequest, mock(Inventory.Callback.class));
        inventory.load(mRequest, mock(Inventory.Callback.class));
        inventory.load(mRequest, l);
        Tests.waitWhileLoading(inventory);
        inventory.load(mRequest, mock(Inventory.Callback.class));
        inventory.load(mRequest, mock(Inventory.Callback.class));
        verify(l, times(1)).onLoaded(anyProducts());
    }

    static class TestCallback implements Inventory.Callback {
        @Nonnull
        volatile Inventory.Products products;

        @Override
        public void onLoaded(@Nonnull Inventory.Products products) {
            this.products = products;
        }
    }
}