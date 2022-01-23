package com.android.vending.billing;

import android.os.Bundle;
import android.os.RemoteException;

public interface InAppBillingService {
    int isBillingSupported(int var1, String var2, String var3) throws RemoteException;

    Bundle getSkuDetails(int var1, String var2, String var3, Bundle var4) throws RemoteException;

    Bundle getBuyIntent(int var1, String var2, String var3, String var4, String var5) throws RemoteException;

    Bundle getPurchases(int var1, String var2, String var3, String var4) throws RemoteException;

    int consumePurchase(int var1, String var2, String var3) throws RemoteException;

    Bundle getBuyIntentExtraParams(int var1, String var2, String var3, String var4, String var5, Bundle var6) throws RemoteException;

    Bundle getPurchaseHistory(int var1, String var2, String var3, String var4, Bundle var5) throws RemoteException;

    int isBillingSupportedExtraParams(int var1, String var2, String var3, Bundle var4) throws RemoteException;

    Bundle consumePurchaseExtraParams(int var1, String var2, String var3, Bundle var4) throws RemoteException;
}
