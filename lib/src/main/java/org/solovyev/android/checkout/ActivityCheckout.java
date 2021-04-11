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

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;

import javax.annotation.Nonnull;

/**
 * <p>{@link UiCheckout} that lives in the context of {@link Activity}.</p>
 * <p>This class shouldn't be used from inside the {@link android.app.Fragment} as
 * {@link android.app.Fragment#onActivityResult} is *not* be called in that case - it's a
 * developers' responsibility to dispatch a {@link Activity#onActivityResult} call themselves to
 * an appropriate {@link android.app.Fragment} instance.</p>
 * <p>In order to conduct purchases inside a {@link android.app.Fragment} (or {@link
 * androidx.fragment.app.Fragment}) a special version of {@link UiCheckout} should be used - it can
 * be obtained via {@link Checkout#forFragment} or, for a more generic case, via
 * {@link Checkout#forUi}.</p>
 *
 * @see UiCheckout
 * @see Checkout#forFragment
 * @see Checkout#forUi
 * @see Checkout#forActivity
 */
public final class ActivityCheckout extends UiCheckout {
    @Nonnull
    private final Activity mActivity;
    @Nonnull
    private final IntentStarter mIntentStarter = new IntentStarter() {
        @Override
        public void startForResult(@Nonnull IntentSender intentSender, int requestCode, @Nonnull Intent intent) throws IntentSender.SendIntentException {
            mActivity.startIntentSenderForResult(intentSender, requestCode, intent, 0, 0, 0);
        }
    };

    ActivityCheckout(@Nonnull Activity activity, @Nonnull Billing billing) {
        super(activity, billing);
        mActivity = activity;
    }

    @Nonnull
    @Override
    protected IntentStarter makeIntentStarter() {
        return mIntentStarter;
    }
}
