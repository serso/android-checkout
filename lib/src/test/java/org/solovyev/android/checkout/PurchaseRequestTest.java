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

import com.android.vending.billing.InAppBillingServiceImpl;
import com.android.vending.billing.InAppBillingService;

import org.junit.Test;

import android.os.Bundle;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PurchaseRequestTest extends RequestTestBase {

    @Override
    protected Request newRequest() {
        return new PurchaseRequest("test", "sku", null);
    }

    @Test
    public void testShouldUseExtraParams() throws Exception {
        final Bundle extraParams = new Bundle();
        extraParams.putString("extra", "test");
        final PurchaseRequest request = new PurchaseRequest("product", "sku", "payload", extraParams);
        final InAppBillingService service = mock(InAppBillingServiceImpl.class);

        request.start(service, "package");

        verify(service).getBuyIntentExtraParams(eq(Billing.V6), eq("package"), eq("sku"), eq("product"), eq("payload"), eq(extraParams));
    }
}
