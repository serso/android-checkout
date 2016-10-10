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
import org.junit.Test;

import android.os.RemoteException;

import java.util.List;

import javax.annotation.Nonnull;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.solovyev.android.checkout.ProductTypes.IN_APP;
import static org.solovyev.android.checkout.ProductTypes.SUBSCRIPTION;

public class CheckoutInventoryTest extends InventoryTestBase {

    @Nonnull
    protected CheckoutInventory newInventory(@Nonnull Checkout checkout) {
        return new CheckoutInventory(checkout);
    }

    @Override
    protected boolean shouldVerifyPurchaseCompletely() {
        return true;
    }

    @Override
    protected void insertPurchases(@Nonnull String product, @Nonnull List<Purchase> purchases) throws RemoteException {
        Tests.mockGetPurchases(mBilling, product, purchases);
    }

    @Override
    protected void insertSkus(@Nonnull String product, @Nonnull List<Sku> skus) throws Exception {
        Tests.mockGetSkuDetails(mBilling, product, skus);
    }

    @Test
    public void testIsLoadedWithEmptySkusList() throws Exception {
        populatePurchases();
        populateSkus();

        final Inventory.Request request = Inventory.Request.create()
                .loadAllPurchases()
                .loadSkus(IN_APP, "in_app_01")
                .loadSkus(SUBSCRIPTION, "sub_01");
        final Checkout checkout = Checkout.forApplication(mBilling);

        final CheckoutInventory inventory = new CheckoutInventory(checkout);
        final TestCallback listener = new TestCallback();
        checkout.start();
        inventory.load(request, listener);

        Tests.waitWhileLoading(inventory);

        final Inventory.Product app = listener.mProducts.get(IN_APP);
        Assert.assertTrue(app.getSkus().isEmpty());
        Assert.assertFalse(app.getPurchases().isEmpty());
        final Inventory.Product sub = listener.mProducts.get(SUBSCRIPTION);
        Assert.assertTrue(sub.getSkus().isEmpty());
        Assert.assertFalse(sub.getPurchases().isEmpty());
    }

    @Test
    public void testShouldContinueAfterListenerException() throws Exception {
        populatePurchases();

        final Inventory.Request request = Inventory.Request.create()
                .loadSkus(IN_APP, "in_app_01")
                .loadSkus(SUBSCRIPTION, "sub_01");
        final Checkout checkout = Checkout.forApplication(mBilling);

        final CrashingCallback listener = new CrashingCallback();
        final CheckoutInventory inventory = new CheckoutInventory(checkout);
        checkout.start();
        inventory.load(request, listener);

        Tests.waitWhileLoading(inventory);

        Assert.assertTrue(listener.exceptionThrown);
    }

    @Test
    public void testShouldLoadSkus() throws Exception {
        populateSkus();

        mCheckout.start();

        final TestCallback c1 = new TestCallback();
        mInventory.load(Inventory.Request.create().loadSkus(SUBSCRIPTION, asList("subX", "sub2", "sub3")), c1);
        final TestCallback c2 = new TestCallback();
        mInventory.load(Inventory.Request.create().loadSkus(IN_APP, asList("1", "2", "5")), c2);

        Tests.waitWhileLoading(mInventory);

        assertEquals(2, c1.mProducts.get(SUBSCRIPTION).getSkus().size());
        assertEquals(2, c2.mProducts.get(IN_APP).getSkus().size());
    }

    private static final class CrashingCallback implements Inventory.Callback {

        private volatile boolean exceptionThrown;

        @Override
        public void onLoaded(@Nonnull Inventory.Products products) {
            exceptionThrown = true;
            throw new RuntimeException("Hello there!");
        }
    }
}
