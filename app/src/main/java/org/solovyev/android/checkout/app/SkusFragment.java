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
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.solovyev.android.checkout.*;

import javax.annotation.Nonnull;
import java.util.Comparator;

import static android.view.animation.AnimationUtils.loadAnimation;
import static org.solovyev.android.checkout.ProductTypes.IN_APP;

public class SkusFragment extends Fragment {

	@Nonnull
	private ActivityCheckout checkout;

	private boolean listShown;

	@Nonnull
	private ArrayAdapter<Sku> adapter;

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

		checkout.createPurchaseFlow(new PurchaseListener());
		inventory = checkout.loadInventory();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_skus_list, container, false);
		adapter = new SkusAdapter(inflater.getContext());
		listView = (ListView) view.findViewById(R.id.skus_listview);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnSkuClickListener());
		progressBar = (ProgressBar) view.findViewById(R.id.skus_progressbar);
		emptyView = (TextView) view.findViewById(R.id.skus_emptyview);

		inventory.whenLoaded(new Inventory.Listener() {
			@Override
			public void onLoaded(@Nonnull Inventory inventory) {
				final Inventory.Product product = inventory.getProduct(IN_APP);
				if (product.isSupported()) {
					for (Sku sku : product.getSkus()) {
						adapter.add(sku);
					}
					adapter.sort(new SkuComparator());
					adapter.notifyDataSetChanged();
				} else {
					emptyView.setText(R.string.skus_billing_not_supported);
				}
				setListShown(true);
			}
		});

		return view;
	}

	private static class PurchaseListener implements RequestListener<Purchase> {
		@Override
		public void onSuccess(@Nonnull Purchase purchase) {
			CheckoutApplication.get().getBus().post(new NewPurchaseEvent(purchase));
			Toast.makeText(CheckoutApplication.get(), R.string.thank_you_for_purchase, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onError(int response, @Nonnull Exception e) {
		}
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

	private class OnSkuClickListener implements AdapterView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			final Sku sku = adapter.getItem(position);
			checkout.whenReady(new Checkout.ListenerAdapter() {
				@Override
				public void onReady(@Nonnull BillingRequests requests) {
					requests.purchase(sku, null, checkout.getPurchaseFlow());
				}
			});
		}
	}

	private class SkuComparator implements Comparator<Sku> {
		@Override
		public int compare(@Nonnull Sku l, @Nonnull Sku r) {
			return l.title.compareTo(r.title);
		}
	}
}
