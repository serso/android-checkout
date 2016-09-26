package org.solovyev.android.checkout;

import javax.annotation.Nonnull;

final class EmptyLogger implements Logger {
    @Override
    public void v(@Nonnull String tag, @Nonnull String msg) {
    }

    @Override
    public void v(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
    }

    @Override
    public void d(@Nonnull String tag, @Nonnull String msg) {
    }

    @Override
    public void d(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
    }

    @Override
    public void i(@Nonnull String tag, @Nonnull String msg) {
    }

    @Override
    public void i(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
    }

    @Override
    public void w(@Nonnull String tag, @Nonnull String msg) {
    }

    @Override
    public void w(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
    }

    @Override
    public void e(@Nonnull String tag, @Nonnull String msg) {
    }

    @Override
    public void e(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
    }
}
