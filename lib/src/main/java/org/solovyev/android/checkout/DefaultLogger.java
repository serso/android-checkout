package org.solovyev.android.checkout;


import android.util.Log;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Default logger implementation that logs to Android Log
 */
@ThreadSafe
public class DefaultLogger implements Logger {
    @Override
    public void e(String tag, String msg) {
        Log.e(tag,msg);
    }

    @Override
    public void w(@Nonnull String tag, String msg) {
        Log.w(tag,msg);
    }

    @Override
    public void i(@Nonnull String tag, String msg) {
        Log.i(tag,msg);
    }

    @Override
    public void d(@Nonnull String tag, String msg) {
        Log.d(tag, msg);
    }

    @Override
    public void v(@Nonnull String tag, String msg) {
        Log.v(tag,msg);
    }

    @Override
    public void e(@Nonnull String tag, String msg, Exception e) {
        Log.e(tag,msg,e);
    }

    @Override
    public void w(@Nonnull String tag, String msg, Exception e) {
        Log.w(tag, msg,e);
    }

    @Override
    public void i(@Nonnull String tag, String msg, Exception e) {
        Log.i(tag,msg,e);
    }

    @Override
    public void d(@Nonnull String tag, String msg, Exception e) {
        Log.d(tag, msg,e);
    }

    @Override
    public void v(@Nonnull String tag, String msg, Exception e) {
        Log.v(tag, msg,e);
    }
}
