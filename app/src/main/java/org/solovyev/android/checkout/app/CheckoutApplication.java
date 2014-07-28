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

package org.solovyev.android.checkout.app;

import android.app.Application;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.solovyev.android.checkout.Billing;
import org.solovyev.android.checkout.Checkout;

import javax.annotation.Nonnull;
import java.util.List;

import static java.util.Arrays.asList;
import static org.solovyev.android.checkout.Billing.newInMemoryCache;
import static org.solovyev.android.checkout.ProductTypes.IN_APP;
import static org.solovyev.android.checkout.ProductTypes.SUBSCRIPTION;

@ReportsCrashes(formKey = "",
		mailTo = "se.solovyev@gmail.com",
		mode = ReportingInteractionMode.SILENT)
public class CheckoutApplication extends Application {

	@Nonnull
	public static final List<String> IN_APP_SKUS = asList("coffee", "beer");

	@Nonnull
	public static final List<String> SUBSCRIPTION_SKUS = asList("coffee", "beer");

	@Nonnull
	public static final List<String> PRODUCTS = asList(IN_APP, SUBSCRIPTION);

	/**
	 * For better performance billing class should be used as singleton
	 */
	@Nonnull
	private final Billing billing = new Billing(this, "test", newInMemoryCache());

	/**
	 * Application wide {@link org.solovyev.android.checkout.Checkout} instance (can be used anywhere in the app).
	 * This instance contains all available products in the app.
	 */
	@Nonnull
	private final Checkout checkout = Checkout.forApplication(billing, PRODUCTS);

	@Nonnull
	private static CheckoutApplication instance;

	public CheckoutApplication() {
		instance = this;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		billing.connect();
	}

	@Nonnull
	public static CheckoutApplication get() {
		return instance;
	}

	@Nonnull
	public Checkout getCheckout() {
		return checkout;
	}

	@Nonnull
	public Billing getBilling() {
		return billing;
	}
}
