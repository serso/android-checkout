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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.solovyev.android.checkout.Sku;

import javax.annotation.Nonnull;

class SkusAdapter extends ArrayAdapter<Sku> {

	private static final class ViewHolder {

		@Nonnull
		private ImageView icon;

		@Nonnull
		private TextView title;

		@Nonnull
		private TextView description;

		@Nonnull
		private TextView price;

		@Nonnull
		private static ViewHolder from(@Nonnull View view) {
			final ViewHolder vh = new ViewHolder();
			vh.icon = (ImageView) view.findViewById(R.id.sku_icon);
			vh.title = (TextView) view.findViewById(R.id.sku_title);
			vh.description = (TextView) view.findViewById(R.id.sku_description);
			vh.price = (TextView) view.findViewById(R.id.sku_price);
			return vh;
		}

		public void fill(@Nonnull Sku sku) {
			title.setText(sku.title);
			description.setText(sku.description);
			price.setText(sku.price);
		}
	}

	public SkusAdapter(@Nonnull Context context) {
		super(context, 0);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final View view;
		final ViewHolder vh;
		if (convertView == null) {
			final LayoutInflater inflater = LayoutInflater.from(getContext());
			view = inflater.inflate(R.layout.sku, parent, false);
			vh = ViewHolder.from(view);
			view.setTag(R.id.sku_view_holder, vh);
		} else {
			view = convertView;
			vh = (ViewHolder) view.getTag(R.id.sku_view_holder);
		}
		vh.fill(getItem(position));
		return view;
	}
}
