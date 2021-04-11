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

import javax.annotation.Nonnull;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BillingSupportedRequestTest extends RequestTestBase {

    @Test
    public void testShouldHaveDifferentCacheKeys() throws Exception {
        final BillingSupportedRequest r1 = newRequest("test1");
        final BillingSupportedRequest r2 = newRequest("test2");

        assertNotEquals(r1.getCacheKey(), r2.getCacheKey());
    }

    @Override
    protected BillingSupportedRequest newRequest() {
        return newRequest("test");
    }

    @Nonnull
    private BillingSupportedRequest newRequest(@Nonnull String product) {
        return new BillingSupportedRequest(product);
    }

    @Test
    public void testShouldNotBeCachedWithExtraParams() throws Exception {
        final BillingSupportedRequest request = new BillingSupportedRequest("test", Billing.V7, new Bundle());
        assertNull(request.getCacheKey());
    }

    @Test
    public void testShouldUseExtraParams() throws Exception {
        final Bundle extraParams = new Bundle();
        extraParams.putString("extra", "test");
        final BillingSupportedRequest request = new BillingSupportedRequest("product", Billing.V7, extraParams);
        final InAppBillingService service = mock(InAppBillingServiceImpl.class);

        request.start(service, "package");

        verify(service).isBillingSupportedExtraParams(eq(Billing.V7), eq("package"), eq("product"), eq(extraParams));
    }
}
