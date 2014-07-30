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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.Window;
import org.solovyev.android.checkout.ActivityCheckout;
import org.solovyev.android.checkout.Checkout;

import javax.annotation.Nonnull;

public class BaseActivity extends FragmentActivity {

	@Nonnull
	private final ActivityCheckout checkout = Checkout.forActivity(this, CheckoutApplication.get().getCheckout());

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		checkout.start();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		checkout.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onDestroy() {
		checkout.stop();
		super.onDestroy();
	}

	@Nonnull
	public ActivityCheckout getCheckout() {
		return checkout;
	}


	protected final void addFragment(@Nonnull Fragment fragment, int viewId, boolean retain) {
		fragment.setRetainInstance(retain);
		getSupportFragmentManager().beginTransaction()
				.add(viewId, fragment)
				.commit();
	}
}
