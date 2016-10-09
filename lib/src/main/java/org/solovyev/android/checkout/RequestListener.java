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

/**
 * Listener associated with a request. Its {@link #onSuccess(Object)} method is called when the
 * result is ready and {@link #onError(int, Exception)} in case of any error.
 * Listener methods might be called either on a background thread or on the main application
 * thread. See {@link Billing} for more information.<p/>
 * <b>Note</b>: if a listener references an activity/context the associated request should
 * be cancelled through {@link Billing#cancel(int)} or {@link Billing#cancelAll()} methods when
 * the references activity/context is destroyed. Otherwise, the request will continue holding the
 * reference and the activity/context will leak.
 */
public interface RequestListener<R> {
    /**
     * Called when the request has finished successfully.
     *
     * @param result request result
     */
    void onSuccess(@Nonnull R result);

    /**
     * Called when the request has finished with an error (for example, exception was raised).
     *
     * @param response response code
     * @param e        raised exception
     */
    void onError(int response, @Nonnull Exception e);
}
