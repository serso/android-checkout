package org.solovyev.android.checkout;

public interface PlayStoreListener {
    /**
     * Called when the Play Store notifies us about the purchase changes.
     */
    void onPurchasesChanged();
}
