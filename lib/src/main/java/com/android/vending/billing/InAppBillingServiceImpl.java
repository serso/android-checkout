package com.android.vending.billing;

import android.os.Bundle;
import android.os.RemoteException;

import com.google.android.gms.internal.play_billing.zzc;

import javax.annotation.Nonnull;
import org.solovyev.android.checkout.ResponseCodes;

/**
 * {@link InAppBillingService} based on the internal implementation of the official
 * <a href="https://developer.android.com/google/play/billing">Android's billing client<a/>.
 */
public class InAppBillingServiceImpl implements InAppBillingService {

    // This field might need to be updated every time the billing client library is updated as
    // it points to the internal obfuscated class representing the billing interface.
    @Nonnull
    private final zzc api;

    public InAppBillingServiceImpl(@Nonnull zzc api) {
        this.api = api;
    }

    @Override
    public int isBillingSupported(int var1, String var2, String var3) throws RemoteException {
        int code = api.zza(var1, var2, var3);
        if (code == ResponseCodes.DEVELOPER_ERROR) return ResponseCodes.OK;
        return code;
    }

    @Override
    public int isBillingSupportedExtraParams(int var1, String var2, String var3, Bundle var4) throws RemoteException {
        int code = api.zzc(var1, var2, var3, var4);
        if (code == ResponseCodes.DEVELOPER_ERROR) return ResponseCodes.OK;
        return code;
    }

    @Override
    public Bundle getSkuDetails(int var1, String var2, String var3, Bundle var4) throws RemoteException {
        return api.zzk(var1, var2, var3, var4);
    }

    @Override
    public Bundle getBuyIntent(int var1, String var2, String var3, String var4, String var5) throws RemoteException {
        return api.zzf(var1, var2, var3, var4, var5);
    }

    @Override
    public Bundle getPurchases(int var1, String var2, String var3, String var4) throws RemoteException {
        return api.zzi(var1, var2, var3, var4);
    }

    @Override
    public int consumePurchase(int var1, String var2, String var3) throws RemoteException {
        return api.zza(var1, var2, var3);
    }

    @Override
    public Bundle getBuyIntentExtraParams(int var1, String var2, String var3, String var4, String var5, Bundle var6) throws RemoteException {
        return api.zzg(var1, var2, var3, var4, var5, var6);
    }

    @Override
    public Bundle getPurchaseHistory(int var1, String var2, String var3, String var4, Bundle var5) throws RemoteException {
        return api.zzh(var1, var2, var3, var4, var5);
    }

    @Override
    public Bundle consumePurchaseExtraParams(int var1, String var2, String var3, Bundle var4) throws RemoteException {
        return api.zze(var1, var2, var3, var4);
    }
}
