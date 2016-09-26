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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * List of requests to be executed when connection with service is established.
 */
final class PendingRequests implements Runnable {

    @GuardedBy("list")
    @Nonnull
    private final List<RequestRunnable> list = new ArrayList<RequestRunnable>();

    /**
     * Adds <var>runnable</var> to the end of waiting list.
     *
     * @param runnable runnable to be executed when connection is established
     */
    void add(@Nonnull RequestRunnable runnable) {
        synchronized (list) {
            Billing.debug("Adding pending request: " + runnable);
            list.add(runnable);
        }
    }

    /**
     * Method cancels all pending requests
     */
    void cancelAll() {
        synchronized (list) {
            Billing.debug("Cancelling all pending requests");
            final Iterator<RequestRunnable> iterator = list.iterator();
            while (iterator.hasNext()) {
                final RequestRunnable request = iterator.next();
                request.cancel();
                iterator.remove();
            }
        }
    }

    /**
     * Method cancels all pending requests with specified <var>tag</var>
     *
     * @param tag request tag
     */
    void cancelAll(@Nullable Object tag) {
        synchronized (list) {
            Billing.debug("Cancelling all pending requests with tag=" + tag);
            final Iterator<RequestRunnable> iterator = list.iterator();
            while (iterator.hasNext()) {
                final RequestRunnable request = iterator.next();
                final Object requestTag = request.getTag();
                if (requestTag == tag) {
                    request.cancel();
                    iterator.remove();
                    continue;
                }

                if (requestTag != null && tag == null) {
                    continue;
                }

                if (requestTag != null && requestTag.equals(tag)) {
                    request.cancel();
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Method cancels pending request with specified <var>requestId</var>
     *
     * @param requestId id of request to be cancelled
     */
    void cancel(int requestId) {
        synchronized (list) {
            Billing.debug("Cancelling pending request with id=" + requestId);
            final Iterator<RequestRunnable> iterator = list.iterator();
            while (iterator.hasNext()) {
                final RequestRunnable request = iterator.next();
                if (request.getId() == requestId) {
                    request.cancel();
                    iterator.remove();
                    break;
                }
            }
        }
    }

    /**
     * Method removes first element from the waiting list
     *
     * @return first list element or null if waiting list is empty
     */
    @Nullable
    RequestRunnable pop() {
        synchronized (list) {
            final RequestRunnable runnable = !list.isEmpty() ? list.remove(0) : null;
            if (runnable != null) {
                Billing.debug("Removing pending request: " + runnable);
            }
            return runnable;
        }
    }

    /**
     * Method gets first element from the waiting list
     *
     * @return first list element or null if waiting list is empty
     */
    @Nullable
    RequestRunnable peek() {
        synchronized (list) {
            return !list.isEmpty() ? list.get(0) : null;
        }
    }

    /**
     * Executes all pending runnable.
     * Note: this method must be called only on one thread.
     */
    @Override
    public void run() {
        RequestRunnable runnable = peek();
        while (runnable != null) {
            Billing.debug("Running pending request: " + runnable);
            if (runnable.run()) {
                remove(runnable);
                runnable = peek();
            } else {
                // request can't be run because service is not connected => no need to run other requests (they will be
                // executed when service is connected)
                break;
            }
        }
    }

    /**
     * Method removes instance of <var>runnable</var> from the waiting list
     *
     * @param runnable runnable to be removed from the waiting list
     */
    private void remove(@Nonnull RequestRunnable runnable) {
        synchronized (list) {
            final Iterator<RequestRunnable> iterator = list.iterator();
            while (iterator.hasNext()) {
                if (iterator.next() == runnable) {
                    Billing.debug("Removing pending request: " + runnable);
                    iterator.remove();
                    break;
                }
            }
        }
    }

    /**
     * Cancels all pending requests with {@link ResponseCodes#SERVICE_NOT_CONNECTED} error code.
     */
    void onConnectionFailed() {
        Check.isMainThread();
        RequestRunnable requestRunnable = pop();
        while (requestRunnable != null) {
            final Request request = requestRunnable.getRequest();
            if (request != null) {
                request.onError(ResponseCodes.SERVICE_NOT_CONNECTED);
                requestRunnable.cancel();
            }
            requestRunnable = pop();
        }
    }
}
