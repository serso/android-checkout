/*
 * Copyright 2015 serso aka se.solovyev
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;
import static org.solovyev.android.checkout.ProductTypes.IN_APP;
import static org.solovyev.android.checkout.ProductTypes.SUBSCRIPTION;
import static org.solovyev.android.checkout.Purchase.State.CANCELLED;
import static org.solovyev.android.checkout.Purchase.State.EXPIRED;
import static org.solovyev.android.checkout.Purchase.State.PURCHASED;
import static org.solovyev.android.checkout.Purchase.State.REFUNDED;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class FailingCacheCheckoutInventoryTest {
    @Nonnull
    protected FailingCache mFailingCache;
    @Nonnull
    protected Billing mBilling;
    @Nonnull
    private Checkout mCheckout;
    @Nonnull
    private Inventory.Request mRequest;

    @Before
    public void setUp() throws Exception {
        mFailingCache = new FailingCache();
        mBilling = newBilling();
        mRequest = Inventory.Request.create()
                .loadAllPurchases()
                .loadSkus(IN_APP, asList("1", "2", "3", "4", "6"))
                .loadSkus(SUBSCRIPTION, asList("sub1", "sub2", "sub3", "sub4"));
        mCheckout = Checkout.forApplication(mBilling);
    }

    protected void populatePurchases() throws Exception {
        final List<Purchase> expectedInApps = asList(
                Purchase.fromJson(PurchaseTest.newJson(1, PURCHASED), ""),
                Purchase.fromJson(PurchaseTest.newJson(2, CANCELLED), ""),
                Purchase.fromJson(PurchaseTest.newJson(3, REFUNDED), ""),
                Purchase.fromJson(PurchaseTest.newJson(4, EXPIRED), "")
        );
        Tests.mockGetPurchases(mBilling, IN_APP, expectedInApps);

        final List<Purchase> expectedSubs = asList(
                Purchase.fromJson(PurchaseTest.newJsonSubscription(1, PURCHASED), ""),
                Purchase.fromJson(PurchaseTest.newJsonSubscription(2, CANCELLED), ""),
                Purchase.fromJson(PurchaseTest.newJsonSubscription(3, REFUNDED), ""),
                Purchase.fromJson(PurchaseTest.newJsonSubscription(4, EXPIRED), "")
        );
        Tests.mockGetPurchases(mBilling, SUBSCRIPTION, expectedSubs);
    }

    @Test
    public void testShouldContinueAfterCacheException() throws Exception {
        populatePurchases();

        final CheckoutInventory inventory = new CheckoutInventory(mCheckout);
        final InventoryTestBase.TestCallback listener = new InventoryTestBase.TestCallback();
        mCheckout.start();
        inventory.load(mRequest, listener);

        Tests.waitWhileLoading(inventory);

        assertTrue(mFailingCache.mExceptionThrown);
    }

    @Nonnull
    protected Billing newBilling() {
        return Tests.newBilling(new Billing.Configuration() {
            @Nonnull
            @Override
            public String getPublicKey() {
                return "test";
            }

            @Nullable
            @Override
            public Cache getCache() {
                return mFailingCache;
            }

            @Nonnull
            @Override
            public PurchaseVerifier getPurchaseVerifier() {
                return Billing.newPurchaseVerifier(this.getPublicKey());
            }

            @Nullable
            @Override
            public Inventory getFallbackInventory(@Nonnull Checkout checkout, @Nonnull Executor onLoadExecutor) {
                return null;
            }

            @Override
            public boolean isAutoConnect() {
                return true;
            }
        });
    }

    private class FailingCache implements Cache {
        boolean mExceptionThrown;

        @Nullable
        @Override
        public Entry get(@Nonnull Key key) {
            return null;
        }

        @Override
        public void put(@Nonnull Key key, @Nonnull Entry entry) {
            if (key.toString().startsWith("purchases_")) {
                throwException();
            }
        }

        private void throwException() {
            mExceptionThrown = true;
            throw new RuntimeException("Hello there!");
        }

        @Override
        public void init() {

        }

        @Override
        public void remove(@Nonnull Key key) {

        }

        @Override
        public void removeAll(int type) {

        }

        @Override
        public void clear() {

        }
    }
}
