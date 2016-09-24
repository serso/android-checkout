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

import org.junit.Assert;
import org.junit.Test;

import android.os.Bundle;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.when;
import static org.solovyev.android.checkout.ProductTypes.IN_APP;
import static org.solovyev.android.checkout.ProductTypes.SUBSCRIPTION;
import static org.solovyev.android.checkout.RequestTestBase.newBundle;
import static org.solovyev.android.checkout.ResponseCodes.OK;

public class CheckoutInventoryTest extends InventoryTestBase {

    static void insertPurchases(@Nonnull Billing billing, @Nonnull String product, @Nonnull List<Purchase> purchases) throws RemoteException {
        final Bundle bundle = newBundle(OK);
        final ArrayList<String> list = new ArrayList<String>();
        for (Purchase purchase : purchases) {
            list.add(purchase.toJson());
        }
        bundle.putStringArrayList(Purchases.BUNDLE_DATA_LIST, list);
        final IInAppBillingService service = ((TestServiceConnector) billing.getConnector()).service;
        when(service.getPurchases(anyInt(), anyString(), eq(product), isNull(String.class))).thenReturn(bundle);
    }

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
        insertPurchases(billing, product, purchases);
    }

    protected boolean isLoaded(@Nonnull Inventory inventory) {
        return ((BaseInventory) inventory).isLoaded();
    }

    @Test
    public void testIsLoadedWithEmptySkusList() throws Exception {
        populatePurchases();

        final Inventory.SkuIds skuIds = Inventory.SkuIds.create()
                .add(IN_APP, "in_app_01")
                .add(SUBSCRIPTION, "sub_01");
        final Checkout checkout = Checkout.forApplication(billing, skuIds.getProducts());

        final CheckoutInventory inventory = new CheckoutInventory(checkout);
        final TestCallback listener = new TestCallback();
        checkout.start();
        inventory.load(skuIds, listener);

        waitWhileLoading(inventory);

        final Inventory.Product app = listener.products.get(IN_APP);
        Assert.assertTrue(app.getSkus().isEmpty());
        Assert.assertFalse(app.getPurchases().isEmpty());
        final Inventory.Product sub = listener.products.get(SUBSCRIPTION);
        Assert.assertTrue(sub.getSkus().isEmpty());
        Assert.assertFalse(sub.getPurchases().isEmpty());
    }

    @Test
    public void testShouldContinueAfterListenerException() throws Exception {
        populatePurchases();

        final Inventory.SkuIds skuIds = Inventory.SkuIds.create()
                .add(IN_APP, "in_app_01")
                .add(SUBSCRIPTION, "sub_01");
        final Checkout checkout = Checkout.forApplication(billing, skuIds.getProducts());

        final CrashingCallback listener = new CrashingCallback();
        final CheckoutInventory inventory = new CheckoutInventory(checkout);
        checkout.start();
        inventory.load(skuIds, listener);

        waitWhileLoading(inventory);

        Assert.assertTrue(listener.exceptionThrown);
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
