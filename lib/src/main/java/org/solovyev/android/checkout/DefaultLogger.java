package org.solovyev.android.checkout;


import android.util.Log;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Default logger implementation that logs to Android Log
 */
@ThreadSafe
class DefaultLogger implements Logger {

    private boolean enabled = BuildConfig.DEBUG;

    @Override
    public void e(@Nonnull String tag, @Nonnull String msg) {
        if (enabled) {
            Log.e(tag, msg);
        }
    }

    @Override
    public void w(@Nonnull String tag, @Nonnull String msg) {
        if (enabled) {
            Log.w(tag, msg);
        }
    }

    @Override
    public void i(@Nonnull String tag, @Nonnull String msg) {
        if (enabled) {
            Log.i(tag, msg);
        }
    }

    @Override
    public void d(@Nonnull String tag, @Nonnull String msg) {
        if (enabled) {
            Log.d(tag, msg);
        }
    }

    @Override
    public void v(@Nonnull String tag, @Nonnull String msg) {
        if (enabled) {
            Log.v(tag, msg);
        }
    }

    @Override
    public void e(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
        if (enabled) {
            Log.e(tag, msg, e);
        }
    }

    @Override
    public void w(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
        if (enabled) {
            Log.w(tag, msg, e);
        }
    }

    @Override
    public void i(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
        if (enabled) {
            Log.i(tag, msg, e);
        }
    }

    @Override
    public void d(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
        if (enabled) {
            Log.d(tag, msg, e);
        }
    }

    @Override
    public void v(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
        if (enabled) {
            Log.v(tag, msg, e);
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
