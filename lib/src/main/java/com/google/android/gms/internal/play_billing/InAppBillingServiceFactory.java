package com.google.android.gms.internal.play_billing;

import android.os.IBinder;

import com.android.vending.billing.InAppBillingService;
import com.android.vending.billing.InAppBillingServiceImpl;

import javax.annotation.Nonnull;

public class InAppBillingServiceFactory {

    @Nonnull
    public static InAppBillingService create(@Nonnull IBinder service) {
        return new InAppBillingServiceImpl(new zzc(service));
    }

}
