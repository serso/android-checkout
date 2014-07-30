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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.solovyev.android.checkout.ActivityCheckout;
import org.solovyev.android.checkout.Inventory;

import javax.annotation.Nonnull;
import java.util.Comparator;

import static android.view.animation.AnimationUtils.loadAnimation;
import static org.solovyev.android.checkout.ProductTypes.IN_APP;

public class PurchasesFragment extends DialogFragment {

	@Nonnull
	private ActivityCheckout checkout;

	private boolean listShown;

	@Nonnull
	private ArrayAdapter<Inventory.Purchases> adapter;

	@Nonnull
	private ListView listView;

	@Nonnull
	private ProgressBar progressBar;

	@Nonnull
	private TextView emptyView;

	@Nonnull
	private Inventory inventory;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		checkout = ((MainActivity) activity).getCheckout();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		inventory = checkout.loadInventory();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		getDialog().setTitle(R.string.purchased_items);
		final View view = inflater.inflate(R.layout.fragment_purchases_list, container, false);
		adapter = new PurchasesAdapter(inflater.getContext());
		listView = (ListView) view.findViewById(R.id.purchases_listview);
		listView.setAdapter(adapter);
		progressBar = (ProgressBar) view.findViewById(R.id.purchases_progressbar);
		emptyView = (TextView) view.findViewById(R.id.purchases_emptyview);

		inventory.whenLoaded(new Inventory.Listener() {
			@Override
			public void onLoaded(@Nonnull Inventory inventory) {
				final Inventory.Product product = inventory.getProduct(IN_APP);
				if (product.isSupported()) {
					for (Inventory.Purchases purchases : product.getPurchasesBySku()) {
						adapter.add(purchases);
					}
					adapter.sort(new PurchasesComparator());
					adapter.notifyDataSetChanged();
				} else {
					emptyView.setText(R.string.billing_not_supported);
				}
				setListShown(true);
			}
		});

		return view;
	}

	public void setListShown(boolean shown, boolean animate) {
		if (listShown == shown) {
			return;
		}
		listShown = shown;

		if (shown) {
			final View view = listView.getCount() > 0 ? listView : emptyView;
			if (animate) {
				progressBar.startAnimation(loadAnimation(getActivity(), android.R.anim.fade_out));
				view.startAnimation(loadAnimation(getActivity(), android.R.anim.fade_in));
			}
			progressBar.setVisibility(View.GONE);
			view.setVisibility(View.VISIBLE);
		} else {
			final View view = listView.getVisibility() == View.VISIBLE ? listView : emptyView;
			if (animate) {
				progressBar.startAnimation(loadAnimation(getActivity(), android.R.anim.fade_in));
				view.startAnimation(loadAnimation(getActivity(), android.R.anim.fade_out));
			}
			progressBar.setVisibility(View.VISIBLE);
			view.setVisibility(View.INVISIBLE);
		}
	}

	public void setListShown(boolean shown) {
		setListShown(shown, true);
	}

	public void setListShownNoAnimation(boolean shown) {
		setListShown(shown, false);
	}

	private class PurchasesComparator implements Comparator<Inventory.Purchases> {
		@Override
		public int compare(@Nonnull Inventory.Purchases l, @Nonnull Inventory.Purchases r) {
			return compare(l.size(), r.size());
		}

		public int compare(int lhs, int rhs) {
			return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
		}
	}
}
