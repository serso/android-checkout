package org.solovyev.android.checkout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.text.TextUtils;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * Receives "com.android.vending.billing.PURCHASES_UPDATED" from the Play Store and notifies the
 * listener.
 */
class PlayStoreBroadcastReceiver extends BroadcastReceiver {

    private static final String ACTION = "com.android.vending.billing.PURCHASES_UPDATED";

    @Nonnull
    private final Context mContext;
    @Nonnull
    private final Object mLock;
    @GuardedBy("mLock")
    @Nonnull
    private final List<PlayStoreListener> mListeners = new ArrayList<>();

    PlayStoreBroadcastReceiver(@Nonnull Context context, @Nonnull Object lock) {
        mContext = context;
        mLock = lock;
    }

    void addListener(@Nonnull PlayStoreListener listener) {
        synchronized (mLock) {
            Check.isTrue(!mListeners.contains(listener), "Listener " + listener + " is already in the list");
            mListeners.add(listener);
            if (mListeners.size() == 1) {
                ContextCompat.registerReceiver(mContext, this, new IntentFilter(ACTION), ContextCompat.RECEIVER_EXPORTED);
            }
        }
    }

    void removeListener(@Nonnull PlayStoreListener listener) {
        synchronized (mLock) {
            Check.isTrue(mListeners.contains(listener), "Listener " + listener + " is not in the list");
            mListeners.remove(listener);
            if (mListeners.size() == 0) {
                mContext.unregisterReceiver(this);
            }
        }
    }

    boolean contains(@Nonnull PlayStoreListener listener) {
        synchronized (mLock) {
            return mListeners.contains(listener);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !TextUtils.equals(intent.getAction(), ACTION)) {
            return;
        }
        final List<PlayStoreListener> listeners;
        synchronized (mLock) {
            listeners = new ArrayList<>(mListeners);
        }
        for (PlayStoreListener listener : listeners) {
            listener.onPurchasesChanged();
        }
    }

}
