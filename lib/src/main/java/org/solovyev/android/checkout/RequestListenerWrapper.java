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

import javax.annotation.Nonnull;

class RequestListenerWrapper<R> implements CancellableRequestListener<R> {

	@Nonnull
	protected final RequestListener<R> listener;

	RequestListenerWrapper(@Nonnull RequestListener<R> listener) {
		this.listener = listener;
	}

	@Override
	public void onSuccess(@Nonnull R result) {
		listener.onSuccess(result);
	}

	@Override
	public void onError(int response, @Nonnull Exception e) {
		listener.onError(response, e);
	}

	public final void cancel() {
		onCancel();
		Billing.cancel(listener);
	}

	protected void onCancel() {
	}
}
