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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.squareup.otto.Subscribe;
import org.solovyev.android.checkout.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MainActivity extends BaseActivity {

	@Nonnull
	private TextView purchasesCounter;

	@Nullable
	private AlertDialog infoDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		CheckoutApplication.get().getBus().register(this);
		if (savedInstanceState == null) {
			addFragment(new SkusFragment(), R.id.fragment_skus, false);
		}
		purchasesCounter = (TextView) findViewById(R.id.purchases_counter);
		final View purchasesButton = findViewById(R.id.purchases_list_button);
		purchasesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, PurchasesActivity.class));
			}
		});
		final View infoButton = findViewById(R.id.info_button);
		infoButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showInfoDialog();
			}
		});
		purchasesCounter.setText(getString(R.string.purchased_items_count, 0));
		updateCounter();
	}

	private void showInfoDialog() {
		final AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setMessage(getString(R.string.info));
		b.setPositiveButton(R.string.join, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				CheckoutApplication.get().getBus().post(new JoinCommunityEvent());
			}
		});

		infoDialog = b.create();
		infoDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				infoDialog = null;
			}
		});
		infoDialog.show();
	}

	@Override
	protected void onDestroy() {
		if (infoDialog != null) {
			infoDialog.dismiss();
			infoDialog = null;
		}
		super.onDestroy();
	}

	private void updateCounter() {
		getCheckout().whenReady(new Checkout.ListenerAdapter() {
			@Override
			public void onReady(@Nonnull BillingRequests requests) {
				requests.getAllPurchases(ProductTypes.IN_APP, new RequestListenerAdapter<Purchases>() {
					@Override
					public void onSuccess(@Nonnull Purchases purchases) {
						purchasesCounter.setText(getString(R.string.purchased_items_count, purchases.list.size()));
					}
				});
			}
		});
	}

	@Subscribe
	public void onNewPurchased(@Nonnull NewPurchaseEvent e) {
		updateCounter();
	}

	@Subscribe
	public void onJoinCommunity(@Nonnull JoinCommunityEvent e) {
		CheckoutApplication.openUri(this, "https://plus.google.com/communities/115918337136768532130");
	}
}
