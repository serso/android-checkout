package org.solovyev.android.checkout;

import com.android.vending.billing.InAppBillingService;

import android.app.PendingIntent;
import android.os.Bundle;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class ChangePurchaseRequest extends Request<PendingIntent> {

    @Nonnull
    private final String mProduct;

    @Nonnull
    private final List<String> mOldSkus;

    @Nonnull
    private final String mNewSku;

    @Nullable
    private final String mPayload;

    ChangePurchaseRequest(@Nonnull String product, @Nonnull List<String> oldSkus, @Nonnull String newSku, @Nullable String payload) {
        super(RequestType.CHANGE_PURCHASE, Billing.V5);
        Check.isTrue(!oldSkus.isEmpty(), "There must be at least one old SKU to be changed");
        mProduct = product;
        mOldSkus = new ArrayList<>(oldSkus);
        mNewSku = newSku;
        mPayload = payload;
    }

    @Override
    void start(@Nonnull InAppBillingService service, @Nonnull String packageName) throws
            RemoteException, RequestException {
        final Bundle bundle = service.getBuyIntentToReplaceSkus(mApiVersion, packageName, mOldSkus, mNewSku, mProduct, mPayload == null ? "" : mPayload);
        if (handleError(bundle)) {
            return;
        }
        final PendingIntent pendingIntent = bundle.getParcelable("BUY_INTENT");
        Check.isNotNull(pendingIntent);
        onSuccess(pendingIntent);
    }

    @Nullable
    @Override
    protected String getCacheKey() {
        return null;
    }
}
