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
import android.util.Base64;
import android.util.Log;

import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.solovyev.android.checkout.Billing;
import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.Inventory;
import org.solovyev.android.checkout.Products;
import org.solovyev.android.checkout.RobotmediaDatabase;
import org.solovyev.android.checkout.RobotmediaInventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static android.content.Intent.ACTION_VIEW;
import static org.solovyev.android.checkout.ProductTypes.IN_APP;

@ReportsCrashes(mailTo = CheckoutApplication.MAIL,
		mode = ReportingInteractionMode.SILENT)
public class CheckoutApplication extends Application {

	@Nonnull
	static final String MAIL = "se.solovyev@gmail.com";

	@Nonnull
	private static final Products products;

	static {
		final List<String> skus = new ArrayList<>();
		skus.addAll(Arrays.asList("coffee", "beer", "cake", "hamburger"));
		for (int i = 0; i < 20; i++) {
			final int id = i + 1;
			final String sku = id < 10 ? "item_0" + id : "item_" + id;
			skus.add(sku);
		}
		products = Products.create().add(IN_APP, skus);
	}

	/**
	 * For better performance billing class should be used as singleton
	 */
	@Nonnull
	private final Billing billing = new Billing(this, new Billing.DefaultConfiguration() {
		@Nonnull
		@Override
		public String getPublicKey() {
			// Note that this is not plain public key but public key encoded with CheckoutApplication.x() method where
			// key = MAIL. As symmetric ciphering is used in CheckoutApplication.x() the same method is used for both
			// ciphering and deciphering. Additionally result of the ciphering is converted to Base64 string => for
			// deciphering with need to convert it back. Generally, x(fromBase64(toBase64(x(PK, salt))), salt) == PK
			// To cipher use CheckoutApplication.toX(), to decipher - CheckoutApplication.fromX().
			// Note that you also can use plain public key and just write `return "Your public key"` but this
			// is not recommended by Google, see http://developer.android.com/google/play/billing/billing_best_practices.html#key
			// Also consider using your own ciphering/deciphering algorithm.
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
		public Inventory getFallbackInventory(@Nonnull Checkout checkout, @Nonnull Executor onLoadExecutor) {
			if (RobotmediaDatabase.exists(billing.getContext())) {
				return new RobotmediaInventory(checkout, onLoadExecutor);
			} else {
				return null;
			}
		}
	});

	/**
	 * Method deciphers previously ciphered message
	 * @param message ciphered message
	 * @param salt salt which was used for ciphering
	 * @return deciphered message
	 */
	@Nonnull
	static String fromX(@Nonnull String message, @Nonnull String salt) {
		return x(new String(Base64.decode(message, 0)), salt);
	}

	/**
	 * Method ciphers message. Later {@link #fromX} method might be used for deciphering
	 * @param message message to be ciphered
	 * @param salt salt to be used for ciphering
	 * @return ciphered message
	 */
	@Nonnull
	static String toX(@Nonnull String message, @Nonnull String salt) {
		return new String(Base64.encode(x(message, salt).getBytes(), 0));
	}

	/**
	 * Symmetric algorithm used for ciphering/deciphering. Note that in your application you probably want to modify
	 * algorithm used for ciphering/deciphering.
	 * @param message message
	 * @param salt salt
	 * @return ciphered/deciphered message
	 */
	@Nonnull
	static String x(@Nonnull String message, @Nonnull String salt) {
		final char[] m = message.toCharArray();
		final char[] s = salt.toCharArray();

		final int ml = m.length;
		final int sl = s.length;
		final char[] result = new char[ml];

		for (int i = 0; i < ml; i++) {
			result[i] = (char) (m[i] ^ s[i % sl]);
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
