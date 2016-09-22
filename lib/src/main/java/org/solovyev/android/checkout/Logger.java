package org.solovyev.android.checkout;

import javax.annotation.Nonnull;

/**
 * Interface to allow custom logger
 */
public interface Logger {

    void v(@Nonnull String tag, @Nonnull String msg);

    void v(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e);

    void d(@Nonnull String tag, @Nonnull String msg);

    void d(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e);

    void i(@Nonnull String tag, @Nonnull String msg);

    void i(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e);

    void w(@Nonnull String tag, @Nonnull String msg);

    void w(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e);

    void e(@Nonnull String tag, @Nonnull String msg);

    void e(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e);
}
