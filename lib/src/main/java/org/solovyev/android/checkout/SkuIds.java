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

package org.solovyev.android.checkout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

public final class SkuIds {
	private final Map<String, List<String>> map = new HashMap<>();

	private SkuIds() {
	}

	@Nonnull
	public static SkuIds create() {
		return new SkuIds();
	}

	@Nonnull
	SkuIds copy() {
		final SkuIds copy = new SkuIds();
		copy.map.putAll(map);
		return copy;
	}

	@Nonnull
	public SkuIds add(@Nonnull String product, @Nonnull List<String> skus) {
		for (String sku : skus) {
			add(product, sku);
		}
		return this;
	}

	@Nonnull
	public SkuIds add(@Nonnull String product, @Nonnull String sku) {
		Check.isNotEmpty(product);
		Check.isNotEmpty(sku);

		List<String> list = map.get(product);
		if (list == null) {
			list = new ArrayList<>();
			map.put(product, list);
		}
		Check.isTrue(!list.contains(sku), "Adding same SKU is not allowed");
		list.add(sku);
		return this;
	}

	@Nonnull
	public Collection<String> getProducts() {
		return Collections.unmodifiableCollection(map.keySet());
	}

	@Nonnull
	public List<String> getSkus(@Nonnull String product) {
		final List<String> list = map.get(product);
		return list == null ? Collections.<String>emptyList() : list;
	}

	public int getProductsCount() {
		return map.size();
	}

	@Override
	public int hashCode() {
		return map.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SkuIds)) return false;

		final SkuIds that = (SkuIds) o;

		return map.equals(that.map);

	}

	public boolean isEmpty() {
		return map.isEmpty();
	}
}
