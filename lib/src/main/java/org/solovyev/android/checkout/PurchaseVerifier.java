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

import java.util.List;

import javax.annotation.Nonnull;

public interface PurchaseVerifier {
    /**
     * Verifies a list of <var>purchases</var> passing all the verified purchases to
     * <var>listener</var>. Note that this method might be called on any thread and methods of
     * <var>listener</var> must be called on the same thread (if this method was called on the main
     * thread listener methods should be also called on the main thread).
     * The actual verification though might use a background thread for communicating with a remote
     * server. {@link BasePurchaseVerifier} can be used as a base class for purchase verifiers that
     * should be executed on a background thread.
     *
     * @param purchases purchases to be verified
     * @param listener  callback which gets a list of verified purchases
     * @see BasePurchaseVerifier
     */
    void verify(@Nonnull List<Purchase> purchases, @Nonnull RequestListener<List<Purchase>> listener);
}
