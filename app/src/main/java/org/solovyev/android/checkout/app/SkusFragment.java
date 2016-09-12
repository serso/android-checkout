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

import static org.solovyev.android.checkout.ProductTypes.IN_APP;
import static org.solovyev.android.checkout.ProductTypes.SUBSCRIPTION;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.solovyev.android.checkout.BillingRequests;
import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.Inventory;
import org.solovyev.android.checkout.Purchase;
import org.solovyev.android.checkout.RequestListener;
import org.solovyev.android.checkout.ResponseCodes;
import org.solovyev.android.checkout.Sku;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;

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
		adapter = new SkusAdapter(inflater.getContext(), new OnChangeSubClickListener());
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

	private class OnChangeSubClickListener implements SkusAdapter.OnChangeSubClickListener {
		@Override
		public void onClick(@Nonnull final SkuUi skuUi) {
			checkout.whenReady(new Checkout.ListenerAdapter() {
				@Override
				public void onReady(@Nonnull BillingRequests requests) {
					final List<String> oldSkus = Collections.singletonList(skuUi.sku.id);
					final String newSku = skuUi.sku.product.equals("sub_01") ? "sub_02" : "sub_01";
					requests.changeSubscription(oldSkus, newSku, null, checkout.getPurchaseFlow());
				}
			});
		}
	}

	private class PurchaseListener extends BaseRequestListener<Purchase> {
		@Override
		public void onSuccess(@Nonnull Purchase purchase) {
			onPurchased();
		}

		private void onPurchased() {
			// let's update purchase information in local inventory
			inventory.load(CheckoutApplication.skus).whenLoaded(new InventoryLoadedListener());
			Toast.makeText(getActivity(), R.string.msg_thank_you_for_purchase, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onError(int response, @Nonnull Exception e) {
			// it is possible that our data is not synchronized with data on Google Play => need to handle some errors
			if (response == ResponseCodes.ITEM_ALREADY_OWNED) {
				onPurchased();
			} else {
				super.onError(response, e);
			}
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
			consume(skuUi.token, new ConsumeListener());
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

	private static class SkuComparator implements Comparator<SkuUi> {
		@Override
		public int compare(@Nonnull SkuUi l, @Nonnull SkuUi r) {
			return l.sku.title.compareTo(r.sku.title);
		}
	}

	private class InventoryLoadedListener implements Inventory.Listener {
		@Override
		public void onLoaded(@Nonnull final Inventory.Products products) {
			checkout.whenReady(new Checkout.ListenerAdapter() {
				@Override
				public void onReady(@Nonnull BillingRequests requests) {
					requests.isChangeSubscriptionSupported(new RequestListener<Object>() {
						@Override
						public void onSuccess(@Nonnull Object result) {
							onProductsReady(products, true);
						}

						@Override
						public void onError(int response, @Nonnull Exception e) {
							onProductsReady(products, false);
						}
					});
				}
			});
		}

		private void onProductsReady(@Nonnull Inventory.Products products, boolean canChangeSubs) {
			adapter.setNotifyOnChange(false);
			adapter.clear();
			final Inventory.Product inApp = products.get(IN_APP);
			final Inventory.Product sub = products.get(SUBSCRIPTION);
			if (inApp.supported || sub.supported) {
				if (inApp.supported) {
					addSkus(inApp, canChangeSubs);
				}
				if (sub.supported) {
					addSkus(sub, canChangeSubs);
				}
				adapter.sort(new SkuComparator());
			} else {
				emptyView.setText(R.string.billing_not_supported);
			}
			adapter.notifyDataSetChanged();
			setListShown(true);
		}

		private void addSkus(Inventory.Product product, boolean canChangeSubs) {
			for (Sku sku : product.getSkus()) {
				final Purchase purchase = product.getPurchaseInState(sku, Purchase.State.PURCHASED);
				adapter.add(SkuUi.create(sku, purchase != null ? purchase.token : null, canChangeSubs));
			}
		}
	}

	private abstract class BaseRequestListener<R> implements RequestListener<R> {

		@Override
		public void onError(int response, @Nonnull Exception e) {
			Log.e("Checkout", "onError: response=" + response, e);
			final String message = e.getMessage();
			Toast.makeText(getActivity(), "Error (" + response + ")" + (TextUtils.isEmpty(message) ? "" : ": " + message), Toast.LENGTH_LONG).show();
		}
 	}

	private class ConsumeListener extends BaseRequestListener<Object> {
		@Override
		public void onSuccess(@Nonnull Object result) {
			onConsumed();
		}

		private void onConsumed() {
			inventory.load(CheckoutApplication.skus).whenLoaded(new InventoryLoadedListener());
			Toast.makeText(getActivity(), R.string.msg_item_consumed, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onError(int response, @Nonnull Exception e) {
			// it is possible that our data is not synchronized with data on Google Play => need to handle some errors
			if (response == ResponseCodes.ITEM_NOT_OWNED) {
				onConsumed();
			} else {
				super.onError(response, e);
			}
		}
	}
}
