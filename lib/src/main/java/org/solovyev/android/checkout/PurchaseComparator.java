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
import java.util.Comparator;

final class PurchaseComparator implements Comparator<Purchase> {

	@Nonnull
	private static final Comparator<Purchase> EARLIEST_FIRST = new PurchaseComparator(true);

	@Nonnull
	private static final Comparator<Purchase> LATEST_FIRST = new PurchaseComparator(false);

	@Nonnull
	static Comparator<Purchase> earliestFirst() {
		return EARLIEST_FIRST;
	}

	@Nonnull
	static Comparator<Purchase> latestFirst() {
		return LATEST_FIRST;
	}

	private final int asc;

	private PurchaseComparator(boolean asc) {
		this.asc = asc ? 1 : -1;
	}

	@Override
	public int compare(@Nonnull Purchase l, @Nonnull Purchase r) {
		return asc * compare(l.time, r.time);
	}

	public static int compare(long l, long r) {
		return l < r ? -1 : (l == r ? 0 : 1);
	}
}
