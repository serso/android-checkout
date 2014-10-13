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

import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import com.android.vending.billing.IInAppBillingService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.solovyev.android.checkout.ResponseCodes.EXCEPTION;
import static org.solovyev.android.checkout.ResponseCodes.OK;

/**
 * Asynchronous operation which is done with connected billing service
 */
abstract class Request<R> {

	@Nonnull
	private static final AtomicInteger counter = new AtomicInteger(0);

	private final int id;

	@Nonnull
	private final RequestType type;

	@Nullable
	private Object tag;

	@GuardedBy("this")
	@Nullable
	private RequestListener<R> listener;

	Request(@Nonnull RequestType type) {
		this.type = type;
		this.id = counter.getAndIncrement();
	}

	Request(@Nonnull RequestType type, @Nonnull Request<R> request) {
		this.type = type;
		this.id = request.id;
		synchronized (request) {
			this.listener = request.listener;
		}
	}

	/**
	 * @return request id, unique identifier of the request in the application
	 */
	protected int getId() {
		return id;
	}

	abstract void start(@Nonnull IInAppBillingService service, int apiVersion, @Nonnull String packageName) throws RemoteException, RequestException;

	/**
	 * @return request tag, object which is associated with this request
	 */
	@Nullable
	Object getTag() {
		return tag;
	}

	void setTag(@Nullable Object tag) {
		this.tag = tag;
	}

	@Nonnull
	RequestType getType() {
		return type;
	}

	/**
	 * Cancels this request, after this method is called request listener method will not be called
	 */
	void cancel() {
		synchronized (this) {
			if (listener != null) {
				Billing.cancel(listener);
			}
			listener = null;
		}
	}

	/**
	 * @return true if request is cancelled
	 */
	boolean isCancelled() {
		synchronized (this) {
			return listener == null;
		}
	}

	void setListener(@Nullable RequestListener<R> listener) {
		synchronized (this) {
			Check.isNull(this.listener);
			this.listener = listener;
		}
	}

	protected void onSuccess(@Nonnull R result) {
		final RequestListener<R> l = getListener();
		if (l != null) {
			l.onSuccess(result);
		}
	}

	protected void onError(int response) {
		Billing.error("Error response: " + response + " in " + this + " request");
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
			return listener;
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
