package org.solovyev.android.checkout;

import android.os.Handler;
import android.os.Looper;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
class MainThreadLogger implements Logger {

    @Nonnull
    private final Logger mLogger;
    private final MainThread mMainThread;

    public MainThreadLogger(@Nonnull Logger logger) {
        mLogger = logger;
        mMainThread = new MainThread(new Handler(Looper.getMainLooper()));
    }

    @Override
    public void v(@Nonnull final String tag, @Nonnull final String msg) {
        mMainThread.execute(new Runnable() {
            @Override
            public void run() {
                mLogger.v(tag, msg);
            }
        });
    }

    @Override
    public void v(@Nonnull final String tag, @Nonnull final String msg, @Nonnull final Throwable e) {
        mMainThread.execute(new Runnable() {
            @Override
            public void run() {
                mLogger.v(tag, msg, e);
            }
        });
    }

    @Override
    public void d(@Nonnull final String tag, @Nonnull final String msg) {
        mMainThread.execute(new Runnable() {
            @Override
            public void run() {
                mLogger.d(tag, msg);
            }
        });
    }

    @Override
    public void d(@Nonnull final String tag, @Nonnull final String msg, @Nonnull final Throwable e) {
        mMainThread.execute(new Runnable() {
            @Override
            public void run() {
                mLogger.d(tag, msg, e);
            }
        });
    }

    @Override
    public void i(@Nonnull final String tag, @Nonnull final String msg) {
        mMainThread.execute(new Runnable() {
            @Override
            public void run() {
                mLogger.i(tag, msg);
            }
        });
    }

    @Override
    public void i(@Nonnull final String tag, @Nonnull final String msg, @Nonnull final Throwable e) {
        mMainThread.execute(new Runnable() {
            @Override
            public void run() {
                mLogger.i(tag, msg, e);
            }
        });
    }

    @Override
    public void w(@Nonnull final String tag, @Nonnull final String msg) {
        mMainThread.execute(new Runnable() {
            @Override
            public void run() {
                mLogger.w(tag, msg);
            }
        });
    }

    @Override
    public void w(@Nonnull final String tag, @Nonnull final String msg, @Nonnull final Throwable e) {
        mMainThread.execute(new Runnable() {
            @Override
            public void run() {
                mLogger.w(tag, msg, e);
            }
        });
    }

    @Override
    public void e(@Nonnull final String tag, @Nonnull final String msg) {
        mMainThread.execute(new Runnable() {
            @Override
            public void run() {
                mLogger.e(tag, msg);
            }
        });
    }

    @Override
    public void e(@Nonnull final String tag, @Nonnull final String msg, @Nonnull final Throwable e) {
        mMainThread.execute(new Runnable() {
            @Override
            public void run() {
                mLogger.e(tag, msg, e);
            }
        });
    }
}
