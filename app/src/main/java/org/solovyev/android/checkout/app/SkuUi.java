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

import org.solovyev.android.checkout.Sku;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SkuUi {

	final Sku sku;

	@Nullable
	final String token;

	private SkuUi(Sku sku, @Nullable String token) {
		this.sku = sku;
		this.token = token;
	}

	@Nonnull
	public static SkuUi create(Sku sku, @Nullable String token) {
		return new SkuUi(sku, token);
	}

	boolean isPurchased() {
		return token != null;
	}
}
