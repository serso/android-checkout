package org.solovyev.android.checkout;

import com.android.vending.billing.InAppBillingService;

import org.json.JSONException;

import android.os.Bundle;
import android.os.RemoteException;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

abstract class BasePurchasesRequest extends Request<Purchases> {
    @Nonnull
    protected final String mProduct;
    @Nullable
    protected final String mContinuationToken;

    protected BasePurchasesRequest(@Nonnull RequestType type, int apiVersion, @Nonnull String product, @Nullable String continuationToken) {
        super(type, apiVersion);
        mProduct = product;
        mContinuationToken = continuationToken;
    }

    protected BasePurchasesRequest(@Nonnull BasePurchasesRequest request, @Nonnull String continuationToken) {
        super(request);
        mProduct = request.mProduct;
        mContinuationToken = continuationToken;
    }

    @Nonnull
    String getProduct() {
        return mProduct;
    }

    @Nullable
    String getContinuationToken() {
        return mContinuationToken;
    }

    @Override
    final void start(@Nonnull InAppBillingService service, @Nonnull String packageName) throws RemoteException {
        final Bundle bundle = request(service, packageName);
        if (handleError(bundle)) {
            return;
        }
        try {
            final String continuationToken = Purchases.getContinuationTokenFromBundle(bundle);
            final List<Purchase> purchases = Purchases.getListFromBundle(bundle);
            if (purchases.isEmpty()) {
                onSuccess(new Purchases(mProduct, purchases, continuationToken));
                return;
            }
            processPurchases(purchases, continuationToken);
        } catch (JSONException e) {
            onError(e);
        }
    }

    @Nullable
    protected abstract Bundle request(@Nonnull InAppBillingService service, @Nonnull String packageName) throws RemoteException;

    protected abstract void processPurchases(@Nonnull List<Purchase> purchases, @Nullable String continuationToken);

    @Nullable
    @Override
    protected String getCacheKey() {
        if (mContinuationToken != null) {
            return mProduct + "_" + mContinuationToken;
        } else {
            return mProduct;
        }
    }
}
