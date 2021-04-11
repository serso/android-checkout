package org.solovyev.android.checkout;

import com.android.vending.billing.InAppBillingService;

import android.os.Bundle;
import android.os.RemoteException;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class GetPurchaseHistoryRequest extends BasePurchasesRequest {

    @Nonnull
    private final Bundle mExtraParams;

    protected GetPurchaseHistoryRequest(@Nonnull String product, @Nullable String continuationToken, @Nullable Bundle extraParams) {
        super(RequestType.GET_PURCHASE_HISTORY, Billing.V6, product, continuationToken);
        mExtraParams = extraParams == null ? new Bundle() : extraParams;
    }

    public GetPurchaseHistoryRequest(@Nonnull GetPurchaseHistoryRequest request, @Nonnull String continuationToken) {
        super(request, continuationToken);
        mExtraParams = request.mExtraParams;
    }

    @Nullable
    @Override
    protected Bundle request(@Nonnull InAppBillingService service, @Nonnull String packageName) throws RemoteException {
        return service.getPurchaseHistory(mApiVersion, packageName, mProduct, mContinuationToken, mExtraParams);
    }

    @Override
    protected void processPurchases(@Nonnull List<Purchase> purchases, @Nullable String continuationToken) {
        onSuccess(new Purchases(mProduct, purchases, continuationToken));
    }
}
