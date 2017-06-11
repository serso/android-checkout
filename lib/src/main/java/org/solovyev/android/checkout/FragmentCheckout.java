package org.solovyev.android.checkout;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;

import javax.annotation.Nonnull;

@TargetApi(Build.VERSION_CODES.N)
final class FragmentCheckout extends UiCheckout implements IntentStarter {
    @Nonnull
    private final Fragment mFragment;

    FragmentCheckout(@Nonnull Fragment fragment, @Nonnull Billing billing) {
        super(fragment, billing);
        mFragment = fragment;
    }

    @Nonnull
    @Override
    protected IntentStarter makeIntentStarter() {
        return this;
    }

    @Override
    public void startForResult(@Nonnull IntentSender intentSender, int requestCode, @Nonnull Intent intent) throws IntentSender.SendIntentException {
        mFragment.startIntentSenderForResult(intentSender, requestCode, intent, 0, 0, 0, null);
    }
}
