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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import org.solovyev.android.checkout.*;

import javax.annotation.Nonnull;
import java.util.Comparator;

import static org.solovyev.android.checkout.ProductTypes.IN_APP;

public class SkusFragment extends BaseListFragment {

	@Nonnull
	private ArrayAdapter<SkuUi> adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		checkout.createPurchaseFlow(new PurchaseListener());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View view = super.onCreateView(inflater, container, savedInstanceState);
		adapter = new SkusAdapter(inflater.getContext());
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnSkuClickListener());
		titleView.setText(R.string.items_for_purchase);
		emptyView.setText(R.string.skus_empty);

		inventory.whenLoaded(new InventoryLoadedListener());

		return view;
	}

	@Override
	public void onDestroy() {
		checkout.destroyPurchaseFlow();
		super.onDestroy();
	}

	private class PurchaseListener extends BaseRequestListener<Purchase> {
		@Override
		public void onSuccess(@Nonnull Purchase purchase) {
			// let's update purchase information in local inventory
			inventory.load().whenLoaded(new InventoryLoadedListener());
			Toast.makeText(CheckoutApplication.get(), R.string.msg_thank_you_for_purchase, Toast.LENGTH_SHORT).show();
		}
	}

	private class OnSkuClickListener implements AdapterView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			final SkuUi sku = adapter.getItem(position);
			purchase(sku);
		}
	}

	private void purchase(@Nonnull final SkuUi skuUi) {
		if (!skuUi.isPurchased()) {
			purchase(skuUi.sku);
		} else {
			consume(skuUi.token, new BaseRequestListener<Object>() {
				@Override
				public void onSuccess(@Nonnull Object result) {
					inventory.load();
				}
			});
		}
	}

	private void consume(@Nonnull final String token, @Nonnull final RequestListener<Object> onConsumed) {
		checkout.whenReady(new Checkout.ListenerAdapter() {
			@Override
			public void onReady(@Nonnull BillingRequests requests) {
				requests.consume(token, onConsumed);
			}
		});
	}

	private void purchase(@Nonnull final Sku sku) {
		checkout.whenReady(new Checkout.ListenerAdapter() {
			@Override
			public void onReady(@Nonnull BillingRequests requests) {
				requests.purchase(sku, null, checkout.getPurchaseFlow());
			}
		});
	}

	private class SkuComparator implements Comparator<SkuUi> {
		@Override
		public int compare(@Nonnull SkuUi l, @Nonnull SkuUi r) {
			return l.sku.title.compareTo(r.sku.title);
		}
	}

	private class InventoryLoadedListener implements Inventory.Listener {
		@Override
		public void onLoaded(@Nonnull Inventory.Products products) {
			final Inventory.Product product = products.get(IN_APP);
			adapter.setNotifyOnChange(false);
			adapter.clear();
			if (product.isSupported()) {
				for (Sku sku : product.getSkus()) {
					final Purchase purchase = product.getPurchaseInState(sku, Purchase.State.PURCHASED);
					adapter.add(SkuUi.create(sku, purchase != null ? purchase.token : null));
				}
				adapter.sort(new SkuComparator());
			} else {
				emptyView.setText(R.string.billing_not_supported);
			}
			adapter.notifyDataSetChanged();
			setListShown(true);
		}
	}

	private abstract class BaseRequestListener<R> implements RequestListener<R> {

		@Override
		public void onError(int response, @Nonnull Exception e) {
			// todo serso: add alert dialog or console
		}
	}
}
