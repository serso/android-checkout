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

import android.app.Activity;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.*;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.solovyev.android.checkout.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static android.content.Intent.ACTION_VIEW;
import static java.util.Arrays.asList;
import static org.solovyev.android.checkout.ProductTypes.IN_APP;

@ReportsCrashes(formKey = "",
		mailTo = CheckoutApplication.MAIL,
		mode = ReportingInteractionMode.SILENT)
public class CheckoutApplication extends Application {

	@Nonnull
	static final String MAIL = "se.solovyev@gmail.com";

	@Nonnull
	private static final Products products = Products.create().add(IN_APP, asList("coffee", "beer", "cake", "hamburger"));

	/**
	 * For better performance billing class should be used as singleton
	 */
	@Nonnull
	private final Billing billing = new Billing(this, new Billing.Configuration() {
		@Nonnull
		@Override
		public String getPublicKey() {
			final String s = "PixnMSYGLjg7Ah0xDwYILlVZUy0sIiBoMi4jLDcoXTcNLiQjKgtlIC48NiRcHxwKHEcYEyZrPyMWXFRpV10VES9ENz" +
					"g1Hj06HTV1MCAHJlpgEDcmOxFDEkA8OiQRKjEQDxhRWVVEMBYmNl1AJghcKUAYVT15KSQgBQABMgwqKSlqF1gZBA4fAw5rMyxKI" +
					"w9LJFc7AhxZGjoPATgRUiUjKSsOWyRKDi4nIA9lKgAGOhMLDF06CwoKGFR6Wj0hGwReS10NXzQTIREhKlkuMz4XDTwUQjRCJUA+" +
					"VjQVPUIoPicOLQJCLxs8RjZnJxY1OQNLKgQCPj83AyBEFSAJEk5UClYjGxVLNBU3FS4DCztENQMuOk5rFVclKz88AAApPgADGFx" +
					"EEV5eQAF7QBhdQEE+Bzc5MygCAwlEFzclKRB7FB0uFgwPKgAvLCk2OyFiKxkgIy8BBQYjFy4/E1ktJikrEVlKJVYIHh16NDwtDC" +
					"U0Vg8JNzoQBwQWOwk1GzZ4FT8fWicwITcRJi8=";
			return fromX(s, MAIL);
		}

		@Nullable
		@Override
		public Cache getCache() {
			return Billing.newCache();
		}
	});

	@Nonnull
	static String fromX(@Nonnull String message, @Nonnull String key) {
		return x(new String(Base64.decode(message, 0)), key);
	}

	@Nonnull
	static String toX(@Nonnull String message, @Nonnull String key) {
		return new String(Base64.encode(x(message, key).getBytes(), 0));
	}

	@Nonnull
	static String x(@Nonnull String message, @Nonnull String key) {
		final char[] m = message.toCharArray();
		final char[] k = key.toCharArray();

		final int ml = m.length;
		final int kl = k.length;
		final char[] result = new char[ml];

		for (int i = 0; i < ml; i++) {
			result[i] = (char) (m[i] ^ k[i % kl]);
		}
		return new String(result);
	}

	/**
	 * Application wide {@link org.solovyev.android.checkout.Checkout} instance (can be used anywhere in the app).
	 * This instance contains all available products in the app.
	 */
	@Nonnull
	private final Checkout checkout = Checkout.forApplication(billing, products);

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

	static boolean openUri(@Nonnull Activity activity, @Nonnull String uri) {
		try {
			activity.startActivity(new Intent(ACTION_VIEW, Uri.parse(uri)));
			return true;
		} catch (ActivityNotFoundException e) {
			Log.e("Checkout", e.getMessage(), e);
		}
		return false;
	}
}
