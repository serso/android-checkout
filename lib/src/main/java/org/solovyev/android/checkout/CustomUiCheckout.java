package org.solovyev.android.checkout;

import javax.annotation.Nonnull;

final class CustomUiCheckout extends UiCheckout {
    @Nonnull
    private final IntentStarter mIntentStarter;

    CustomUiCheckout(@Nonnull IntentStarter intentStarter, @Nonnull Object tag, @Nonnull Billing billing) {
        super(tag, billing);
        mIntentStarter = intentStarter;
    }

    @Nonnull
    @Override
    protected IntentStarter makeIntentStarter() {
        return mIntentStarter;
    }
}
