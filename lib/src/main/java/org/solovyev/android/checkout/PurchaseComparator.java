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

import java.util.Comparator;

import javax.annotation.Nonnull;

final class PurchaseComparator implements Comparator<Purchase> {

    @Nonnull
    private static final Comparator<Purchase> EARLIEST_FIRST = new PurchaseComparator(true);

    @Nonnull
    private static final Comparator<Purchase> LATEST_FIRST = new PurchaseComparator(false);
    private final int mAsc;

    private PurchaseComparator(boolean asc) {
        mAsc = asc ? 1 : -1;
    }

    @Nonnull
    static Comparator<Purchase> earliestFirst() {
        return EARLIEST_FIRST;
    }

    @Nonnull
    static Comparator<Purchase> latestFirst() {
        return LATEST_FIRST;
    }

    public static int compare(long l, long r) {
        return l < r ? -1 : (l == r ? 0 : 1);
    }

    @Override
    public int compare(@Nonnull Purchase l, @Nonnull Purchase r) {
        return mAsc * compare(l.time, r.time);
    }
}
