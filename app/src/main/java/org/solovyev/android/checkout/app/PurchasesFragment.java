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
import android.widget.ArrayAdapter;
import org.solovyev.android.checkout.Inventory;

import javax.annotation.Nonnull;
import java.util.Comparator;

import static org.solovyev.android.checkout.ProductTypes.IN_APP;

public class PurchasesFragment extends BaseListFragment {
	@Nonnull
	private ArrayAdapter<Inventory.Purchases> adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View view = super.onCreateView(inflater, container, savedInstanceState);
		adapter = new PurchasesAdapter(getActivity());
		listView.setAdapter(adapter);
		titleView.setText(R.string.purchased_items);
		emptyView.setText(R.string.purchases_empty);

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
