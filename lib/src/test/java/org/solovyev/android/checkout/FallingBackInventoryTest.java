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
import org.robolectric.RuntimeEnvironment;

import android.database.sqlite.SQLiteDatabase;

import java.util.List;

import javax.annotation.Nonnull;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.solovyev.android.checkout.ProductTypes.IN_APP;
import static org.solovyev.android.checkout.ProductTypes.SUBSCRIPTION;
import static org.solovyev.android.checkout.Tests.sameThreadExecutor;

public class FallingBackInventoryTest extends InventoryTestBase {

    @Override
    @Before
    public void setUp() throws Exception {
        SQLiteDatabase db = RuntimeEnvironment.application.openOrCreateDatabase(RobotmediaDatabase.NAME, 0, null);
        db.close();

        super.setUp();

        final InAppBillingService service = ((TestServiceConnector) mBilling.getConnector()).mService;
        when(service.isBillingSupported(anyInt(), anyString(), eq(SUBSCRIPTION))).thenReturn(ResponseCodes.ERROR);
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

        assertEquals(0, c1.mProducts.get(SUBSCRIPTION).getSkus().size());
        assertEquals(2, c2.mProducts.get(IN_APP).getSkus().size());
    }

    @Nonnull
    @Override
    protected Billing newBilling() {
        final Billing billing = super.newBilling();
        billing.setMainThread(sameThreadExecutor());
        return billing;
    }

    @Nonnull
    @Override
    protected FallingBackInventory newInventory(@Nonnull Checkout checkout) {
        return new FallingBackInventory(checkout, new RobotmediaInventory(checkout, sameThreadExecutor()));
    }

    @Override
    protected boolean shouldVerifyPurchaseCompletely() {
        return false;
    }

    @Override
    protected void insertPurchases(@Nonnull String product, @Nonnull List<Purchase> purchases) throws Exception {
        if (IN_APP.equals(product)) {
            Tests.mockGetPurchases(mBilling, product, purchases);
        } else {
            RobotmediaInventoryTest.insertPurchases(new BillingDB(RuntimeEnvironment.application), purchases);
        }
    }

    @Override
    protected void insertSkus(@Nonnull String product, @Nonnull List<Sku> skus) throws Exception {
        Tests.mockGetSkuDetails(mBilling, product, skus);
    }

    @Test
    public void testShouldFallbackIfProductIsNotSupported() throws Exception {
    }
}
