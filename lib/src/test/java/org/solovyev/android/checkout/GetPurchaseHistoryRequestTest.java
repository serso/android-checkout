package org.solovyev.android.checkout;

import android.os.Bundle;

public class GetPurchaseHistoryRequestTest extends RequestTestBase {

    @Override
    protected Request newRequest() {
        final Bundle extraParams = new Bundle();
        extraParams.putString("extra", "42");
        return new GetPurchaseHistoryRequest(ProductTypes.SUBSCRIPTION, null, extraParams);
    }
}