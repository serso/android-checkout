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

import android.os.Bundle;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class GetSkuDetailsRequest extends Request<Skus> {

    // unfortunately, Android has an undocumented limit on the size of the list in this request.
    // 20 is a number used in one of the Google samples, namely "Trivial Drive", source code of which
    // can be found here https://github.com/googlesamples/android-play-billing/blob/master/TrivialDrive/app/src/main/java/com/example/android/trivialdrivesample/util/IabHelper.java
    private static final int MAX_SIZE_PER_REQUEST = 20;

    @Nonnull
    private final String mProduct;

    @Nonnull
    private final ArrayList<String> mSkus;

    GetSkuDetailsRequest(@Nonnull String product, @Nonnull List<String> skus) {
        super(RequestType.GET_SKU_DETAILS);
        mProduct = product;
        mSkus = new ArrayList<>(skus);
        Collections.sort(mSkus);
    }

    @Override
    void start(@Nonnull InAppBillingService service, @Nonnull String packageName) throws RemoteException, RequestException {
        final List<Sku> allSkuDetails = new ArrayList<>();
        for (int start = 0; start < mSkus.size(); start += MAX_SIZE_PER_REQUEST) {
            final int end = Math.min(mSkus.size(), start + MAX_SIZE_PER_REQUEST);
            final ArrayList<String> skuBatch = new ArrayList<>(mSkus.subList(start, end));
            final Skus skuDetails = getSkuDetails(service, packageName, skuBatch);
            if (skuDetails != null) {
                allSkuDetails.addAll(skuDetails.list);
            } else {
                // error during the request, already handled
                return;
            }
        }
        onSuccess(new Skus(mProduct, allSkuDetails));
    }

    @Nullable
    private Skus getSkuDetails(@Nonnull InAppBillingService service, @Nonnull String packageName,
                               ArrayList<String> skuBatch) throws RemoteException, RequestException {
        Check.isTrue(skuBatch.size() <= MAX_SIZE_PER_REQUEST, "SKU list is too big");
        final Bundle skusBundle = new Bundle();
        skusBundle.putStringArrayList("ITEM_ID_LIST", skuBatch);
        final Bundle bundle = service.getSkuDetails(Billing.V3, packageName, mProduct, skusBundle);
        if (!handleError(bundle)) {
            return Skus.fromBundle(bundle, mProduct);
        }
        return null;
    }

    @Nullable
    @Override
    protected String getCacheKey() {
        if (mSkus.size() == 1) {
            return mProduct + "_" + mSkus.get(0);
        } else {
            final StringBuilder sb = new StringBuilder(5 * mSkus.size());
            sb.append("[");
            for (int i = 0; i < mSkus.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(mSkus.get(i));
            }
            sb.append("]");
            return mProduct + "_" + sb.toString();
        }
    }
}
