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
import android.os.Bundle;
import android.view.View;

import javax.annotation.Nullable;

public class MainActivity extends BaseActivity {

	@Nullable
	private AlertDialog infoDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			addFragment(new SkusFragment(), R.id.fragment_skus, false);
		}
		final View infoButton = findViewById(R.id.info_button);
		infoButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showInfoDialog();
			}
		});
	}

	private void showInfoDialog() {
		final AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setMessage(getString(R.string.msg_info));
		b.setPositiveButton(R.string.join, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				CheckoutApplication.openUri(MainActivity.this, "https://plus.google.com/communities/115918337136768532130");
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
}
