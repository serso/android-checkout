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

import static android.app.Activity.RESULT_OK;
import static java.util.Arrays.asList;
import static org.solovyev.android.checkout.ResponseCodes.EXCEPTION;
import static org.solovyev.android.checkout.ResponseCodes.NULL_INTENT;
import static org.solovyev.android.checkout.ResponseCodes.OK;
import static org.solovyev.android.checkout.ResponseCodes.WRONG_SIGNATURE;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;

import org.json.JSONException;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class which handles different events during the purchase process
 */
public final class PurchaseFlow implements CancellableRequestListener<PendingIntent> {

	static final String EXTRA_RESPONSE = "RESPONSE_CODE";
	static final String EXTRA_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
	static final String EXTRA_PURCHASE_SIGNATURE = "INAPP_DATA_SIGNATURE";

	@Nonnull
	private final Activity activity;

	private final int requestCode;

	@Nullable
	private RequestListener<Purchase> listener;

	@Nonnull
	private final PurchaseVerifier verifier;

	PurchaseFlow(@Nonnull Activity activity, int requestCode, @Nonnull RequestListener<Purchase> listener, @Nonnull PurchaseVerifier verifier) {
		this.activity = activity;
		this.requestCode = requestCode;
		this.listener = listener;
		this.verifier = verifier;
	}

	@Override
	public void onSuccess(@Nonnull PendingIntent purchaseIntent) {
		if (listener == null) {
			// request was cancelled => stop here
			return;
		}
		try {
			activity.startIntentSenderForResult(purchaseIntent.getIntentSender(), requestCode, new Intent(), 0, 0, 0);
		} catch (RuntimeException e) {
			handleError(e);
		} catch (IntentSender.SendIntentException e) {
			handleError(e);
		}
	}

	void onActivityResult(int requestCode, int resultCode, Intent intent) {
		try {
			Check.equals(this.requestCode, requestCode);
			if (intent == null) {
				// sometimes intent is null (it's not obvious when it happens but it happens from time to time)
				handleError(NULL_INTENT);
				return;
			}
			final int responseCode = intent.getIntExtra(EXTRA_RESPONSE, OK);
			if (resultCode == RESULT_OK && responseCode == OK) {
				final String data = intent.getStringExtra(EXTRA_PURCHASE_DATA);
				final String signature = intent.getStringExtra(EXTRA_PURCHASE_SIGNATURE);
				Check.isNotNull(data);
				Check.isNotNull(signature);

				final Purchase purchase = Purchase.fromJson(data, signature);
				verifier.verify(asList(purchase), new VerificationListener());
			} else {
				handleError(responseCode);
			}
		} catch (RuntimeException e) {
			handleError(e);
		} catch (JSONException e) {
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
		if (listener != null) {
			listener.onError(response, e);
		}
	}

	/**
	 * Note that cancelling the purchase flow is not the same as cancelling the purchase process as purchase process
	 * is not controlled by the app. This method only guarantees that there will be no more calls to {@link RequestListener}
	 */
	@Override
	public void cancel() {
		if (listener != null) {
			Billing.cancel(listener);
			listener = null;
		}
	}

	private class VerificationListener implements RequestListener<List<Purchase>> {
		@Override
		public void onSuccess(@Nonnull List<Purchase> verifiedPurchases) {
			Check.isMainThread();
			if (!verifiedPurchases.isEmpty()) {
				if (listener != null) {
					listener.onSuccess(verifiedPurchases.get(0));
				}
			} else {
				handleError(WRONG_SIGNATURE);
			}
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
