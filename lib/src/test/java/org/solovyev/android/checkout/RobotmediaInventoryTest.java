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

import org.junit.Before;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import javax.annotation.Nonnull;

import static org.solovyev.android.checkout.Tests.sameThreadExecutor;

public class RobotmediaInventoryTest extends InventoryTestBase {

    @Nonnull
    private BillingDB mDb;

    static void insertPurchases(@Nonnull BillingDB db, @Nonnull List<Purchase> purchases) {
        for (Purchase purchase : purchases) {
            db.insert(toTransaction(purchase));
        }
    }

    @Nonnull
    private static Transaction toTransaction(@Nonnull Purchase purchase) {
        Transaction.PurchaseState[] states = Transaction.PurchaseState.values();
        Transaction.PurchaseState state = states[purchase.state.id];
        return new Transaction(purchase.orderId, purchase.sku, purchase.packageName, state, null, purchase.time, purchase.payload);
    }

    @Override
    protected void insertSkus(@Nonnull String product, @Nonnull List<Sku> skus) throws Exception {
    }

    @Override
    @Before
    public void setUp() throws Exception {
        mDb = new BillingDB(RuntimeEnvironment.application);
        super.setUp();
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
    protected Inventory newInventory(@Nonnull Checkout checkout) {
        return new RobotmediaInventory(checkout, sameThreadExecutor());
    }

    @Override
    protected boolean shouldVerifyPurchaseCompletely() {
        return false;
    }

    @Override
    protected void insertPurchases(@Nonnull String product, @Nonnull List<Purchase> purchases) throws Exception {
        insertPurchases(mDb, purchases);
    }
}