package org.solovyev.android.checkout;

import com.android.vending.billing.IInAppBillingService;

import android.app.PendingIntent;
import android.os.Bundle;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class ChangePurchaseRequest extends Request<PendingIntent> {

    @Nonnull
    private final String product;

    @Nonnull
    private final List<String> oldSkus;

    @Nonnull
    private final String newSku;

    @Nullable
    private final String payload;

    ChangePurchaseRequest(@Nonnull String product, @Nonnull List<String> oldSkus, @Nonnull String newSku, @Nullable String payload) {
        super(RequestType.CHANGE_PURCHASE, Billing.V5);
        Check.isTrue(!oldSkus.isEmpty(), "There must be at least one old SKU to be changed");
        this.product = product;
        this.oldSkus = new ArrayList<>(oldSkus);
        this.newSku = newSku;
        this.payload = payload;
    }

    @Override
    void start(@Nonnull IInAppBillingService service, @Nonnull String packageName) throws
            RemoteException, RequestException {
        final Bundle bundle = service.getBuyIntentToReplaceSkus(apiVersion, packageName, oldSkus, newSku, product, payload == null ? "" : payload);
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
