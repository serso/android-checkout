package org.solovyev.android.checkout;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;

import javax.annotation.Nonnull;

/**
 * <p>Callback invoked by {@link UiCheckout} when a {@link PurchaseFlow} starts. Implementor expects
 * to call one of the following methods:
 * {@link android.app.Activity#startIntentSenderForResult(IntentSender, int, Intent, int, int,
 * int)},
 * {@link android.app.Fragment#startIntentSenderForResult(IntentSender, int, Intent, int, int, int,
 * Bundle)}
 * or
 * {@link androidx.fragment.app.Fragment#startIntentSenderForResult(IntentSender, int, Intent, int,
 * int, int, Bundle)}.
 * </p>
 * <p>
 * The reason for this interface is to avoid a dependency between this library and the support
 * library.
 * </p>
 *
 * @see Checkout#forUi
 */
public interface IntentStarter {
    void startForResult(@Nonnull IntentSender intentSender, int requestCode, @Nonnull Intent intent) throws IntentSender.SendIntentException;
}
