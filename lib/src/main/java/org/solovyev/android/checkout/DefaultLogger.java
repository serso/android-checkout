package org.solovyev.android.checkout;


import android.util.Log;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Default logger implementation that logs to Android Log
 */
@ThreadSafe
class DefaultLogger implements Logger {

    private boolean mEnabled = BuildConfig.DEBUG;

    @Override
    public void e(@Nonnull String tag, @Nonnull String msg) {
        if (mEnabled) {
            Log.e(tag, msg);
        }
    }

    @Override
    public void w(@Nonnull String tag, @Nonnull String msg) {
        if (mEnabled) {
            Log.w(tag, msg);
        }
    }

    @Override
    public void i(@Nonnull String tag, @Nonnull String msg) {
        if (mEnabled) {
            Log.i(tag, msg);
        }
    }

    @Override
    public void d(@Nonnull String tag, @Nonnull String msg) {
        if (mEnabled) {
            Log.d(tag, msg);
        }
    }

    @Override
    public void v(@Nonnull String tag, @Nonnull String msg) {
        if (mEnabled) {
            Log.v(tag, msg);
        }
    }

    @Override
    public void e(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
        if (mEnabled) {
            Log.e(tag, msg, e);
        }
    }

    @Override
    public void w(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
        if (mEnabled) {
            Log.w(tag, msg, e);
        }
    }

    @Override
    public void i(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
        if (mEnabled) {
            Log.i(tag, msg, e);
        }
    }

    @Override
    public void d(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
        if (mEnabled) {
            Log.d(tag, msg, e);
        }
    }

    @Override
    public void v(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
        if (mEnabled) {
            Log.v(tag, msg, e);
        }
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }
}
