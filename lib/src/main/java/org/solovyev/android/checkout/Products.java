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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * List of supported products in the {@link Checkout}. Additionally SKUs might be
 * provided. Then SKU details will be loaded in {@link Checkout#loadInventory()}.
 * Note that changes to this class won't affect {@link Checkout} or {@link Billing} after they are created.
 */
@Immutable
public final class Products {
	// key: productId, value: list of SKUs for product
	private final Map<String, List<String>> map = new HashMap<String, List<String>>();

	private Products() {
	}

	@Nonnull
	public static Products create() {
		return new Products();
	}

	@Nonnull
	Products copy() {
		final Products copy = new Products();
		copy.map.putAll(map);
		return copy;
	}

	@Nonnull
	public Products add(@Nonnull String product, @Nonnull List<String> skus) {
		Check.isNull(map.get(product), "Products can't be changed");
		map.put(product, Collections.unmodifiableList(new ArrayList<String>(skus)));
		return this;
	}

	@Nonnull
	public Products add(@Nonnull String product) {
		add(product, Collections.<String>emptyList());
		return this;
	}

	@Nonnull
	public Collection<String> getIds() {
		return Collections.unmodifiableCollection(map.keySet());
	}

	@Nonnull
	public List<String> getSkuIds(@Nonnull String product) {
		return map.get(product);
	}

	public int size() {
		return map.size();
	}
}
