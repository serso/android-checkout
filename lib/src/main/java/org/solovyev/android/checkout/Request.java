/*
 * Copyright 2014 serso aka se.solovyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Contact details
 *
 * Email: se.solovyev@gmail.com
 * Site:  http://se.solovyev.org
 */

package org.solovyev.android.checkout;

import com.android.vending.billing.InAppBillingService;

import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import static org.solovyev.android.checkout.ResponseCodes.EXCEPTION;
import static org.solovyev.android.checkout.ResponseCodes.OK;

/**
 * Asynchronous operation which is done with connected billing service
 */
abstract class Request<R> {

    @Nonnull
    private static final AtomicInteger sCounter = new AtomicInteger(0);
    protected final int mApiVersion;
    private final int mId;
    @Nonnull
    private final RequestType mType;
    @Nullable
    private Object mTag;
    @GuardedBy("this")
    @Nullable
    private RequestListener<R> mListener;
    @GuardedBy("this")
    private boolean mListenerCalled;

    Request(@Nonnull RequestType type) {
        this(type, Billing.V3);
    }

    Request(@Nonnull RequestType type, int apiVersion) {
        mType = type;
        mApiVersion = apiVersion;
        mId = sCounter.getAndIncrement();
    }

    Request(@Nonnull Request<R> request) {
        mType = request.mType;
        mId = request.mId;
        mApiVersion = request.mApiVersion;
        synchronized (request) {
            mListener = request.mListener;
        }
    }

    /**
     * @return request id, unique identifier of the request in the application
     */
    protected int getId() {
        return mId;
    }

    abstract void start(@Nonnull InAppBillingService service, @Nonnull String packageName)
            throws RemoteException, RequestException;

    /**
     * @return request tag, object which is associated with this request
     */
    @Nullable
    Object getTag() {
        return mTag;
    }

    void setTag(@Nullable Object tag) {
        this.mTag = tag;
    }

    @Nonnull
    RequestType getType() {
        return mType;
    }

    /**
     * Cancels this request, after this method is called request listener method will not be called
     */
    void cancel() {
        synchronized (this) {
            if (mListener != null) {
                Billing.cancel(mListener);
            }
            mListener = null;
        }
    }

    /**
     * @return true if request is cancelled
     */
    boolean isCancelled() {
        synchronized (this) {
            return mListener == null;
        }
    }

    protected void onSuccess(@Nonnull R result) {
        final RequestListener<R> l = getListener();
        if (l != null) {
            if (checkListenerCalled()) return;
            l.onSuccess(result);
        }
    }

    private boolean checkListenerCalled() {
        synchronized (this) {
            if (mListenerCalled) {
                return true;
            }
            mListenerCalled = true;
        }
        return false;
    }

    protected void onError(int response) {
        String message = ResponseCodes.toString(response);
        Billing.error("Error response: " + message + " in " + this + " request");
        onError(response, new BillingException(response));
    }

    public void onError(@Nonnull Exception e) {
        Check.isFalse(e instanceof BillingException, "Use onError(int) instead");
        Billing.error("Exception in " + this + " request: ", e);
        onError(EXCEPTION, e);
    }

    private void onError(int response, @Nonnull Exception e) {
        Check.notEquals(OK, response);
        final RequestListener<R> l = getListener();
        if (l != null) {
            if (checkListenerCalled()) return;
            l.onError(response, e);
        }
    }

    protected final boolean handleError(@Nullable Bundle bundle) {
        final int response = bundle != null ? bundle.getInt("RESPONSE_CODE") : ResponseCodes.ERROR;
        return handleError(response);
    }

    protected final boolean handleError(int response) {
        if (response != OK) {
            onError(response);
            return true;
        }
        return false;
    }

    @Nullable
    RequestListener<R> getListener() {
        synchronized (this) {
            return mListener;
        }
    }

    void setListener(@Nullable RequestListener<R> listener) {
        synchronized (this) {
            Check.isNull(this.mListener);
            mListener = listener;
        }
    }

    @Override
    public String toString() {
        final String cacheKey = getCacheKey();
        if (!TextUtils.isEmpty(cacheKey)) {
            return getClass().getSimpleName() + "(" + cacheKey + ")";
        } else {
            return getClass().getSimpleName();
        }
    }

    /**
     * @return key to be used in the cache, null if request should not be cached
     */
    @Nullable
    abstract String getCacheKey();

}
