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

import android.content.Context;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class RobotmediaInventory extends BaseInventory {

	@Nonnull
	private final Executor background = Executors.newSingleThreadExecutor();

	@Nonnull
	private final Executor onLoadExecutor;

	@GuardedBy("lock")
	@Nonnull
	private State state = State.INITIAL;

	public RobotmediaInventory(@Nonnull Checkout checkout, @Nonnull Executor onLoadExecutor) {
		super(checkout);
		this.onLoadExecutor = onLoadExecutor;
	}

	@Nonnull
	@Override
	public Inventory load() {
		synchronized (lock) {
			if (state != State.INITIAL) {
				return this;
			}
			state = State.LOADING;
			if (RobotmediaDatabase.exists(checkout.getContext())) {
				background.execute(new Loader());
			} else {
				onLoaded(RobotmediaDatabase.toInventoryProducts(checkout.getProducts()));
			}
		}

		return this;
	}

	private void onLoaded(@Nonnull final Inventory.Products products) {
		synchronized (lock) {
			if(this.state == State.LOADED) {
				return;
			}
			this.state = State.LOADED;
			this.products = products;
		}
		onLoadExecutor.execute(new Runnable() {
			@Override
			public void run() {
				synchronized (lock) {
					if(state != State.LOADED) {
						return;
					}
					listeners.onLoaded(products);
				}
			}
		});
	}

	boolean isLoaded() {
		synchronized (lock) {
			return state == State.LOADED;
		}
	}

	private enum State {
		INITIAL,
		LOADING,
		LOADED
	}

	private class Loader implements Runnable {
		@Override
		public void run() {
			final Context context = checkout.getContext();
			final RobotmediaDatabase database = new RobotmediaDatabase(context);
			final Products products = database.load(checkout.getProducts());
			onLoaded(products);
		}
	}
}
