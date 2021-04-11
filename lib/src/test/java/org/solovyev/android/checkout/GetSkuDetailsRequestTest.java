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

import org.json.JSONObject;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetSkuDetailsRequestTest extends RequestTestBase {

    @Override
    protected Request newRequest() {
        return new GetSkuDetailsRequest("test", asList("sku"));
    }

    @Test
    public void testShouldHaveSameCacheKey() throws Exception {
        final List<String> skus = asList("1", "2", "3");
        final GetSkuDetailsRequest r1 = new GetSkuDetailsRequest("test", skus);
        Collections.reverse(skus);
        final GetSkuDetailsRequest r2 = new GetSkuDetailsRequest("test", skus);

        assertEquals(r1.getCacheKey(), r2.getCacheKey());
    }

    @Test
    public void testShouldContainAllSkusInCacheKey() throws Exception {
        final GetSkuDetailsRequest request = new GetSkuDetailsRequest("test", asList("1", "2", "3"));

        assertEquals("test_[1,2,3]", request.getCacheKey());
    }

    @Test
    public void testShouldHandleBigLists() throws Exception {
        final List<String> skus = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            skus.add("sku_" + i);
        }
        final InAppBillingService service = mock(InAppBillingServiceImpl.class);
        final GetSkuDetailsRequest request = new GetSkuDetailsRequest("test", skus);
        final RequestListenerSpy l = new RequestListenerSpy();
        request.setListener(l);
        when(service.getSkuDetails(anyInt(), anyString(), anyString(), any(Bundle.class))).thenAnswer(new Answer<Bundle>() {
            @Override
            public Bundle answer(InvocationOnMock invocation) throws Throwable {
                final Bundle bundle = (Bundle) invocation.getArguments()[3];
                final ArrayList<String> ids = bundle.getStringArrayList("ITEM_ID_LIST");
                final ArrayList<String> details = new ArrayList<String>();
                for (int i = 0; i < ids.size(); i++) {
                    final String id = ids.get(i);
                    final JSONObject skuDetail = new JSONObject();
                    skuDetail.put("productId", id);
                    skuDetail.put("price", String.valueOf(i));
                    skuDetail.put("title", id);
                    skuDetail.put("description", id);
                    details.add(skuDetail.toString());
                }
                final Bundle skuDetails = new Bundle();
                skuDetails.putStringArrayList("DETAILS_LIST", details);
                return skuDetails;
            }
        });

        request.start(service, "");

        assertNotNull(l.mSkus);
        for (String sku : skus) {
            assertTrue(l.mSkus.hasSku(sku));
        }
        assertTrue(l.mSkus.list.size() == 97);
    }

    private static class RequestListenerSpy implements RequestListener<Skus> {
        private Skus mSkus;

        @Override
        public void onSuccess(@Nonnull Skus skus) {
            mSkus = skus;
        }

        @Override
        public void onError(int response, @Nonnull Exception e) {
        }
    }
}
