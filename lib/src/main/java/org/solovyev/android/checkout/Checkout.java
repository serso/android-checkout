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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p/>
 * Billing API helper class. Might be used in {@link android.app.Activity} or {@link android.app.Service} and then its
 * lifespan should be bound to the lifecycle of the activity/service. For example, {@link #start()} and {@link #stop()} methods
 * of this class should be called from appropriate methods of activity:<br/>
 * <pre>{@code
 * public class MainActivity extends Activity {
 *
 *    private final ActivityCheckout checkout = Checkout.forActivity(this, App.getCheckout());
 *    private final RequestListener<Purchase> purchaseListener = new BillingListener<Purchase>() {
 *        public void onSuccess(@Nonnull Purchase purchase) {
 *            // item was purchased
 *            // ...
 *        }
 *    };
 *
 *    protected void onCreate(Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *         // ...
 *         checkout.start(new Checkout.Listener() {
 *                  public void onReady(@Nonnull BillingRequests requests) {
 *                  }
 *
 *                  public void onReady(@Nonnull BillingRequests requests, @Nonnull String product, boolean billingSupported) {
 *                      if (billingSupported) {
 *                          // billing for product is supported
 *                          // ...
 *                      }
 *                  }
 *             });
 *
 *         // in case is Activity was recreated (screen rotation) we need to readd purchase listener
 *         checkout.createPurchaseFlow(purchaseListener);
 *     }
 *
 *     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
 *         super.onActivityResult(requestCode, resultCode, data);
 *         checkout.onActivityResult(requestCode, resultCode, data);
 *     }
 *
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
	public static ActivityCheckout forActivity(@Nonnull Activity activity, @Nonnull Billing billing, @Nonnull List<String> products) {
		return new ActivityCheckout(activity, billing, products);
	}

	@Nonnull
	public static ActivityCheckout forActivity(@Nonnull Activity activity, @Nonnull Checkout checkout) {
		return new ActivityCheckout(activity, checkout.billing, checkout.products);
	}

	@Nonnull
	public static Checkout forService(@Nonnull Service service, @Nonnull Billing billing, @Nonnull List<String> products) {
		return new Checkout(service, billing, products);
	}

	@Nonnull
	public static Checkout forApplication(@Nonnull Billing billing, @Nonnull List<String> products) {
		return new Checkout(null, billing, products);
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
	private final List<String> products;

	private BillingRequests requests;

	@Nonnull
	private final Listeners listeners = new Listeners();

	@Nonnull
	private final Map<String, Boolean> supportedProducts = new HashMap<String, Boolean>();

	Checkout(@Nullable Context context, @Nonnull Billing billing, @Nonnull List<String> products) {
		this.billing = billing;
		Check.isNotEmpty(products);
		this.context = context;
		this.products = products;
	}

	public void start() {
		start(null);
	}

	public void start(@Nullable final Listener listener) {
		Check.isMainThread();
		Check.isNull(requests, "Already started");
		requests = createRequests();
		if (listener != null) {
			listeners.add(listener);
		}
		for (final String product : products) {
			requests.isBillingSupported(product, new RequestListener<Object>() {
				@Override
				public void onSuccess(@Nonnull Object result) {
					onBillingSupportedResult(product, true);
				}

				@Override
				public void onError(int response, @Nonnull Exception e) {
					onBillingSupportedResult(product, false);
				}
			});
		}
	}

	public void whenReady(@Nonnull Listener listener) {
		Check.isMainThread();
		for (Map.Entry<String, Boolean> entry : supportedProducts.entrySet()) {
			listener.onReady(requests, entry.getKey(), entry.getValue());
		}

		if (isReady()) {
			listener.onReady(requests);
		} else {
			// still waiting
			listeners.add(listener);
		}
	}

	private boolean isReady() {
		return supportedProducts.size() == products.size();
	}

	@Nonnull
	private BillingRequests createRequests() {
		if (context instanceof Activity) {
			return billing.getRequests((Activity) context);
		} else if (context instanceof Service) {
			return billing.getRequests((Service) context);
		} else {
			Check.isNull(context);
			return billing.getRequests();
		}
	}

	private void onBillingSupportedResult(@Nonnull String product, boolean supported) {
		supportedProducts.put(product, supported);
		listeners.onReady(requests, product, supported);
		if (isReady()) {
			listeners.onReady(requests);
			listeners.clear();
		}
	}

	/**
	 * Method clears all listeners and cancels all pending requests. After this method is called no more work can be
	 * done with this class unless {@link Checkout#start()} method is called again.
	 */
	public void stop() {
		Check.isMainThread();
		listeners.clear();
		if (requests != null) {
			requests.cancelAll();
			requests = null;
		}
	}

	public boolean isBillingSupported(@Nonnull String product) {
		Check.isTrue(product.contains(product), "Product should be added to the products list");
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
			for (Listener listener : list) {
				listener.onReady(requests);
			}
			list.clear();
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
}
