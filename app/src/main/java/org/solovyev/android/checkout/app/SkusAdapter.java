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
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class SkusAdapter extends ArrayAdapter<SkuUi> {

	@Nonnull
	private final SkusAdapter.OnChangeSubClickListener onChangeSubClickListener;

	interface OnChangeSubClickListener {
		void onClick(@Nonnull SkuUi skuUi);
	}

	private static final class ViewHolder implements View.OnClickListener {

		@Nonnull
		private final SkusAdapter.OnChangeSubClickListener onChangeSubClickListener;

		@Nonnull
		private ImageView icon;

		@Nonnull
		private ImageView changeSub;

		@Nonnull
		private TextView title;

		@Nonnull
		private TextView description;

		@Nonnull
		private TextView price;

		@Nullable
		private SkuUi skuUi;

		public ViewHolder(@Nonnull OnChangeSubClickListener onChangeSubClickListener) {
			this.onChangeSubClickListener = onChangeSubClickListener;
		}

		@Nonnull
		private static ViewHolder from(@Nonnull View view, @Nonnull  OnChangeSubClickListener onChangeSubClickListener) {
			final ViewHolder vh = new ViewHolder(onChangeSubClickListener);
			vh.icon = (ImageView) view.findViewById(R.id.sku_icon);
			vh.changeSub = (ImageView) view.findViewById(R.id.sku_change_sub);
			vh.title = (TextView) view.findViewById(R.id.sku_title);
			vh.description = (TextView) view.findViewById(R.id.sku_description);
			vh.price = (TextView) view.findViewById(R.id.sku_price);
			return vh;
		}

		public void fill(@Nonnull SkuUi skuUi) {
			this.skuUi = skuUi;
			final int iconResId = SkuUi.getIconResId(skuUi.sku.id);
			icon.setImageResource(iconResId);
			title.setText(SkuUi.getTitle(skuUi.sku));
			description.setText(skuUi.sku.description);
			price.setText(skuUi.sku.price);

			lineThrough(title, skuUi.isPurchased());
			lineThrough(description, skuUi.isPurchased());
			lineThrough(price, skuUi.isPurchased());

			if (skuUi.sku.isSubscription() && skuUi.isPurchased() && skuUi.canChangeSubs) {
				changeSub.setVisibility(View.VISIBLE);
				changeSub.setImageResource(skuUi.sku.id.equals("sub_01") ? R.drawable.ic_arrow_downward_black_24dp : R.drawable.ic_arrow_upward_black_24dp);
				changeSub.setOnClickListener(this);
			} else {
				changeSub.setVisibility(View.GONE);
				changeSub.setOnClickListener(null);
			}
		}

		private void lineThrough(TextView view, boolean lineThrough) {
			final int flags;
			if (lineThrough) {
				flags = view.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG;
			} else {
				flags = view.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG;
			}
			view.setPaintFlags(flags);
		}

		@Override
		public void onClick(View view) {
			if (skuUi == null) {
				return;
			}
			onChangeSubClickListener.onClick(skuUi);
		}
	}

	public SkusAdapter(@Nonnull Context context, @Nonnull OnChangeSubClickListener onChangeSubClickListener) {
		super(context, 0);
		this.onChangeSubClickListener = onChangeSubClickListener;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final View view;
		final ViewHolder vh;
		if (convertView == null) {
			final LayoutInflater inflater = LayoutInflater.from(getContext());
			view = inflater.inflate(R.layout.sku, parent, false);
			vh = ViewHolder.from(view, onChangeSubClickListener);
			view.setTag(vh);
		} else {
			view = convertView;
			vh = (ViewHolder) view.getTag();
		}
		vh.fill(getItem(position));
		return view;
	}
}
