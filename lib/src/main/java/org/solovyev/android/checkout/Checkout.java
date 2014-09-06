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

import android.app.Activity;
import android.app.Service;
import android.content.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * <p/>
 * Billing API helper class. Might be used in {@link android.app.Activity} or {@link android.app.Service} and then its
 * lifespan should be bound to the lifecycle of the activity/service. For example, {@link #start()} and {@link #stop()} methods
 * of this class should be called from appropriate methods of activity:<br/>
 * <pre>{@code
 * public class MainActivity extends Activity {
 * <p/>
 *    private final ActivityCheckout checkout = Checkout.forActivity(this, App.getCheckout());
 *    private final RequestListener<Purchase> purchaseListener = new BillingListener<Purchase>() {
 *        public void onSuccess(@Nonnull Purchase purchase) {
 *            // item was purchased
 *            // ...
 *        }
 *    };
 * <p/>
 *    protected void onCreate(Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *         // ...
 *         checkout.start(new Checkout.Listener() {
 *                  public void onReady(@Nonnull BillingRequests requests) {
 *                  }
 * <p/>
 *                  public void onReady(@Nonnull BillingRequests requests, @Nonnull String product, boolean billingSupported) {
 *                      if (billingSupported) {
 *                          // billing for product is supported
 *                          // ...
 *                      }
 *                  }
 *             });
 * <p/>
 *         // in case is Activity was recreated (screen rotation) we need to readd purchase listener
 *         checkout.createPurchaseFlow(purchaseListener);
 *     }
 * <p/>
 *     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
 *         super.onActivityResult(requestCode, resultCode, data);
 *         checkout.onActivityResult(requestCode, resultCode, data);
 *     }
 * <p/>
 *     protected void onDestroy() {
 *         checkout.stop();
 *         super.onDestroy();
 *     }
 * }
 * }</pre>
 * <br/>
 * It is possible to call {@link #start()}/{@link #stop()} several times in order to start/stop work with Checkout class. Be aware, though, that {@link #stop()} will cancel all pending requests and remove all listeners.
 * </p>
 * <p>
 * As soon as Billing API is ready for product
 * {@link Listener#onReady(BillingRequests, String, boolean)} is called. If all products are ready {@link Listener#onReady(BillingRequests)}
 * is called. In case of any error while executing the initial requests {@link Listener#onReady(BillingRequests, String, boolean)} is called with <code>billingSupported=false</code>
 * </p>
 * <p>
 * <b>Note</b>: currently this class can only be used on the main application thread
 * </p>
 */
public class Checkout {

	/**
	 * For activity {@link ActivityCheckout#onActivityResult(int, int, android.content.Intent)} must be called from appropriate
	 * activity method.
	 */
	@Nonnull
	public static ActivityCheckout forActivity(@Nonnull Activity activity, @Nonnull Billing billing, @Nonnull Products products) {
		return new ActivityCheckout(activity, billing, products);
	}

	@Nonnull
	public static ActivityCheckout forActivity(@Nonnull Activity activity, @Nonnull Checkout checkout) {
		return new ActivityCheckout(activity, checkout.billing, checkout.products);
	}

	@Nonnull
	public static Checkout forService(@Nonnull Service service, @Nonnull Billing billing, @Nonnull Products products) {
		return new Checkout(service, billing, products);
	}

	@Nonnull
	public static Checkout forApplication(@Nonnull Billing billing, @Nonnull Products products) {
		return new Checkout(null, billing, products);
	}

	@Nonnull
	Context getContext() {
		return billing.getContext();
	}

	/**
	 * Initial request listener, all methods are called on the main application thread
	 */
	public static interface Listener {
		/**
		 * Called when {@link BillingRequests#isBillingSupported(String, RequestListener)} finished for all products
		 *
		 * @param requests requests ready to use
		 */
		void onReady(@Nonnull BillingRequests requests);

		/**
		 * Called when {@link BillingRequests#isBillingSupported(String, RequestListener)} finished for <var>product</var>
		 * with <var>billingSupported</var> result
		 *
		 * @param requests         requests ready to use
		 * @param product          product for which check was done
		 * @param billingSupported true if billing is supported for <var>product</var>
		 */
		void onReady(@Nonnull BillingRequests requests, @Nonnull String product, boolean billingSupported);
	}

	/**
	 * This adapter class provides empty implementations of the methods from {@link org.solovyev.android.checkout.Checkout.Listener}.
	 * Any custom listener that cares only about a subset of the methods of this listener can
	 * simply subclass this adapter class instead of implementing the interface directly.
	 */
	public static abstract class ListenerAdapter implements Listener {
		@Override
		public void onReady(@Nonnull BillingRequests requests) {
		}

		@Override
		public void onReady(@Nonnull BillingRequests requests, @Nonnull String product, boolean billingSupported) {
		}
	}

	@Nullable
	protected final Context context;

	@Nonnull
	protected final Billing billing;

	@Nonnull
	private final Products products;

	@Nonnull
	final Object lock = new Object();

	@GuardedBy("lock")
	private Billing.Requests requests;

	@GuardedBy("lock")
	@Nonnull
	private State state = State.INITIAL;

	@GuardedBy("lock")
	@Nonnull
	private final Listeners listeners = new Listeners();

	@GuardedBy("lock")
	@Nonnull
	private final Map<String, Boolean> supportedProducts = new HashMap<String, Boolean>();

	@Nonnull
	private final OnLoadExecutor onLoadExecutor = new OnLoadExecutor();

	Checkout(@Nullable Context context, @Nonnull Billing billing, @Nonnull Products products) {
		this.billing = billing;
		Check.isNotEmpty(products.getIds());
		this.context = context;
		this.products = products.copy();
	}

	@Nonnull
	Products getProducts() {
		return products;
	}

	public void start() {
		start(null);
	}

	public void start(@Nullable final Listener listener) {
		Check.isMainThread();

		synchronized (lock) {
			Check.isFalse(state == State.STARTED, "Already started");
			Check.isNull(requests, "Already started");
			state = State.STARTED;
			requests = billing.getRequests(context);
			if (listener != null) {
				listeners.add(listener);
			}
			for (final String product : products.getIds()) {
				requests.isBillingSupported(product, new RequestListener<Object>() {
					@Override
					public void onSuccess(@Nonnull Object result) {
						onBillingSupported(product, true);
					}

					@Override
					public void onError(int response, @Nonnull Exception e) {
						onBillingSupported(product, false);
					}
				});
			}
		}
	}

	public void whenReady(@Nonnull Listener listener) {
		Check.isMainThread();

		synchronized (lock) {
			for (Map.Entry<String, Boolean> entry : supportedProducts.entrySet()) {
				listener.onReady(requests, entry.getKey(), entry.getValue());
			}

			if (isReady()) {
				checkIsNotStopped();
				Check.isNotNull(requests);
				listener.onReady(requests);
			} else {
				// still waiting
				listeners.add(listener);
			}
		}
	}

	private void checkIsNotStopped() {
		Check.isFalse(state == State.STOPPED, "Checkout is stopped");
	}

	private boolean isReady() {
		Check.isTrue(Thread.holdsLock(lock), "Should be called from synchronized block");
		return supportedProducts.size() == products.size();
	}

	private void onBillingSupported(@Nonnull String product, boolean supported) {
		synchronized (lock) {
			supportedProducts.put(product, supported);
			listeners.onReady(requests, product, supported);
			if (isReady()) {
				listeners.onReady(requests);
				listeners.clear();
			}
		}
	}

	@Nonnull
	public Inventory loadInventory() {
		Check.isMainThread();

		synchronized (lock) {
			checkIsNotStopped();
		}

		final Inventory inventory;
		final Inventory fallbackInventory = billing.getConfiguration().getFallbackInventory(this, onLoadExecutor);
		if (fallbackInventory == null) {
			inventory = new CheckoutInventory(this);
		} else {
			inventory = new FallingBackInventory(this, fallbackInventory);
		}
		inventory.load();
		return inventory;
	}

	/**
	 * Method clears all listeners and cancels all pending requests. After this method is called no more work can be
	 * done with this class unless {@link Checkout#start()} method is called again.
	 */
	public void stop() {
		Check.isMainThread();

		synchronized (lock) {
			supportedProducts.clear();
			listeners.clear();
			if (state != State.INITIAL) {
				state = State.STOPPED;
			}
			if (requests != null) {
				requests.cancelAll();
				requests = null;
			}
		}
	}

	public boolean isBillingSupported(@Nonnull String product) {
		Check.isTrue(products.getIds().contains(product), "Product should be added to the products list");
		Check.isTrue(supportedProducts.containsKey(product), "Billing information is not ready yet");
		return supportedProducts.get(product);
	}

	private static final class Listeners implements Listener {
		@Nonnull
		private final List<Listener> list = new ArrayList<Listener>();

		public void add(@Nonnull Listener l) {
			if (!list.contains(l)) {
				list.add(l);
			}
		}

		@Override
		public void onReady(@Nonnull BillingRequests requests) {
			final List<Listener> localList = new ArrayList<Listener>(list);
			list.clear();
			for (Listener listener : localList) {
				listener.onReady(requests);
			}
		}

		@Override
		public void onReady(@Nonnull BillingRequests requests, @Nonnull String product, boolean billingSupported) {
			for (Listener listener : list) {
				listener.onReady(requests, product, billingSupported);
			}
		}

		public void clear() {
			list.clear();
		}
	}

	private final class OnLoadExecutor implements Executor {
		@Override
		public void execute(Runnable command) {
			final Executor executor;
			synchronized (lock) {
				executor = requests != null ? requests.getDeliveryExecutor() : null;
			}

			if (executor != null) {
				executor.execute(command);
			} else {
				Billing.error("Trying to deliver result on stopped checkout.");
			}
		}
	}

	private enum State {
		INITIAL,
		STARTED,
		STOPPED
	}
}
