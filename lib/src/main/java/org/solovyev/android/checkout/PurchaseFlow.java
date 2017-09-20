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

import org.json.JSONException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static android.app.Activity.RESULT_OK;
import static java.util.Collections.singletonList;
import static org.solovyev.android.checkout.ResponseCodes.EXCEPTION;
import static org.solovyev.android.checkout.ResponseCodes.NULL_INTENT;
import static org.solovyev.android.checkout.ResponseCodes.OK;
import static org.solovyev.android.checkout.ResponseCodes.WRONG_SIGNATURE;

/**
 * <p>
 * Class that represents one purchase flow (process) from the moment when user requests a purchase
 * until the moment the purchase goes through. It is mainly used by {@link
 * BillingRequests#purchase(Sku, String, PurchaseFlow)}
 * in order to conduct a purchase. This class can only be instantiated in the context of
 * {@link Activity} as it is required by Billing API (to start a Google Play app).
 * </p>
 * There are three main steps in the purchase process:
 * <ol>
 * <li>Initial communication with the billing service and preparing the purchase (done in {@link
 * BillingRequests#purchase(Sku, String, PurchaseFlow)})</li>
 * <li>Starting Google Play app to conduct the purchase (done in this class, see {@link
 * PurchaseFlow#onSuccess(PendingIntent)}</li>
 * <li>Handling the result from Google Play app (done in this class, see {@link
 * PurchaseFlow#onActivityResult(int, int, Intent)})</li>
 * </ol>
 */
public final class PurchaseFlow implements CancellableRequestListener<PendingIntent> {

    static final String EXTRA_RESPONSE = "RESPONSE_CODE";
    static final String EXTRA_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
    static final String EXTRA_PURCHASE_SIGNATURE = "INAPP_DATA_SIGNATURE";
    @Nonnull
    private final IntentStarter mIntentStarter;
    private final int mRequestCode;
    @Nonnull
    private final PurchaseVerifier mVerifier;
    @Nullable
    private RequestListener<Purchase> mListener;

    PurchaseFlow(@Nonnull IntentStarter intentStarter, int requestCode, @Nonnull RequestListener<Purchase> listener, @Nonnull PurchaseVerifier verifier) {
        mIntentStarter = intentStarter;
        mRequestCode = requestCode;
        mListener = listener;
        mVerifier = verifier;
    }

    @Override
    public void onSuccess(@Nonnull PendingIntent purchaseIntent) {
        if (mListener == null) {
            // request was cancelled => stop here
            return;
        }
        try {
            mIntentStarter.startForResult(purchaseIntent.getIntentSender(), mRequestCode, new Intent());
        } catch (RuntimeException | IntentSender.SendIntentException e) {
            handleError(e);
        }
    }

    void onActivityResult(int requestCode, int resultCode, Intent intent) {
        try {
            Check.equals(mRequestCode, requestCode);
            if (intent == null) {
                // sometimes intent is null (it's not obvious when it happens but it happens from time to time)
                handleError(NULL_INTENT);
                return;
            }
            final int responseCode = intent.getIntExtra(EXTRA_RESPONSE, OK);
            if (resultCode != RESULT_OK || responseCode != OK) {
                handleError(responseCode);
                return;
            }
            final String data = intent.getStringExtra(EXTRA_PURCHASE_DATA);
            final String signature = intent.getStringExtra(EXTRA_PURCHASE_SIGNATURE);
            Check.isNotNull(data);
            Check.isNotNull(signature);

            final Purchase purchase = Purchase.fromJson(data, signature);
            mVerifier.verify(singletonList(purchase), new VerificationListener());
        } catch (RuntimeException | JSONException e) {
            handleError(e);
        }
    }

    private void handleError(int response) {
        Billing.error("Error response: " + response + " in Purchase/ChangePurchase request");
        onError(response, new BillingException(response));
    }

    private void handleError(@Nonnull Exception e) {
        Billing.error("Exception in Purchase/ChangePurchase request: ", e);
        onError(ResponseCodes.EXCEPTION, e);
    }

    @Override
    public void onError(int response, @Nonnull Exception e) {
        if (mListener == null) {
            return;
        }
        mListener.onError(response, e);
    }

    /**
     * Cancels this purchase flow.
     * Note that cancelling the purchase flow is not the same as cancelling the purchase process as
     * purchase process is not controlled by the app. This method only guarantees that there will be
     * no more calls of {@link RequestListener}'s methods.
     */
    @Override
    public void cancel() {
        if (mListener == null) {
            return;
        }
        Billing.cancel(mListener);
        mListener = null;
    }

    private class VerificationListener implements RequestListener<List<Purchase>> {
        @Override
        public void onSuccess(@Nonnull List<Purchase> verifiedPurchases) {
            Check.isMainThread();
            if (verifiedPurchases.isEmpty()) {
                handleError(WRONG_SIGNATURE);
                return;
            }
            if (mListener == null) {
                return;
            }
            mListener.onSuccess(verifiedPurchases.get(0));
        }

        @Override
        public void onError(int response, @Nonnull Exception e) {
            Check.isMainThread();
            if (response == EXCEPTION) {
                handleError(e);
            } else {
                handleError(response);
            }
        }
    }
}
