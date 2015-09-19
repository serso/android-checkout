package org.solovyev.android.checkout;

/**
 * Interface to allow custom logger
 */
public interface Logger {
    void e(String tag,String msg);
    void w(String tag,String msg);
    void i(String tag,String msg);
    void d(String tag,String msg);
    void v(String tag,String msg);

    void e(String tag,String msg,Exception e);
    void w(String tag,String msg,Exception e);
    void i(String tag,String msg,Exception e);
    void d(String tag,String msg,Exception e);
    void v(String tag,String msg,Exception e);
}
