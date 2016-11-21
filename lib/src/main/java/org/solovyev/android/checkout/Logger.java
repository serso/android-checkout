package org.solovyev.android.checkout;

import javax.annotation.Nonnull;

/**
 * Logger interface that can be used in {@link Billing} via {@link Billing#setLogger(Logger)}.
 * Methods of this class can be invoked on any thread, use
 * {@link Billing#newMainThreadLogger(Logger)} to perform logging only on the main thread.
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
