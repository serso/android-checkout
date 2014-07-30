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
import com.squareup.otto.Bus;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.solovyev.android.checkout.Billing;
import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.Products;

import javax.annotation.Nonnull;

import static java.util.Arrays.asList;
import static org.solovyev.android.checkout.Billing.newInMemoryCache;
import static org.solovyev.android.checkout.ProductTypes.IN_APP;

@ReportsCrashes(formKey = "",
		mailTo = "se.solovyev@gmail.com",
		mode = ReportingInteractionMode.SILENT)
public class CheckoutApplication extends Application {

	@Nonnull
	private static final Products products = Products.create().add(IN_APP, asList("coffee", "beer", "cake", "hamburger"));

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
	private final Checkout checkout = Checkout.forApplication(billing, products);

	@Nonnull
	private final Bus bus = new Bus();

	@Nonnull
	private static CheckoutApplication instance;

	public CheckoutApplication() {
		instance = this;
	}

	static int getSkuIconResId(@Nonnull String skuId) {
		final int iconResId;
		if (skuId.equals("cake")) {
			iconResId = R.drawable.ic_agenda_birthday_color;
		} else if (skuId.equals("beer")) {
			iconResId = R.drawable.ic_agenda_birthday_color;
		} else if (skuId.equals("hamburger")) {
			iconResId = R.drawable.ic_agenda_birthday_color;
		} else if (skuId.equals("coffee")) {
			iconResId = R.drawable.ic_agenda_birthday_color;
		} else {
			iconResId = 0;
		}
		return iconResId;
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
	public Bus getBus() {
		return bus;
	}
}
