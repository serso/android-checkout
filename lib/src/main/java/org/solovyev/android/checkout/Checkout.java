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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * <p/>
 * Billing API helper class. Can be be used in the context of {@link android.app.Activity} or
 * {@link android.app.Service}. In such case its lifespan should be bound to the lifecycle of the
 * bound activity/service. For example, {@link #start()} and {@link #stop()} methods of this class
 * should be called from the appropriate methods of activity:<br/>
 * <pre>{@code
 * public class MainActivity extends Activity {
 *
 *    private final ActivityCheckout mCheckout = Checkout.forActivity(this, getCheckout());
 *
 *    private final RequestListener<Purchase> mPurchaseListener = new BillingListener<Purchase>() {
 *        public void onSuccess(Purchase purchase) {
 *            // item was purchased
 *            // ...
 *        }
 *    };
 *
 *    protected void onCreate(Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *         // ...
 *         mCheckout.start(new Checkout.ListenerAdapter() {
 *              public void onReady(BillingRequests requests, String product, boolean
 * billingSupported) {
 *                      if (billingSupported) {
 *                          // billing for a product is supported
 *                          // ...
 *                      }
 *                  }
 *              });
 *
 *         mCheckout.createPurchaseFlow(mPurchaseListener);
 *     }
 *
 *     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
 *         super.onActivityResult(requestCode, resultCode, data);
 *         mCheckout.onActivityResult(requestCode, resultCode, data);
 *     }
 *
 *     protected void onDestroy() {
 *         mCheckout.stop();
 *         super.onDestroy();
 *     }
 * }
 * }</pre>
 * <br/>
 * It is possible to call {@link #start()}/{@link #stop()} several times if you want know that in
 * some circumstances no Billing information is needed (internally the Billing service might be
 * unbound from the application). Be aware, though, that {@link #stop()} will cancel all pending
 * requests and remove all set listeners.
 * </p>
 * <p>
 * As soon as Billing API is ready for product
 * {@link Listener#onReady(BillingRequests, String, boolean)} is called. If all the products are
 * ready {@link Listener#onReady(BillingRequests)} is called. In case of any error while executing
 * the initial requests {@link Listener#onReady(BillingRequests, String, boolean)} is called with
 * <code>billingSupported=false</code>
 * </p>
 * <p>
 * <b>Note</b>: currently this class can only be used on the main application thread
 * </p>
 */
public class Checkout {

    @Nullable
    protected final Context mContext;
    @Nonnull
    protected final Billing mBilling;
    @Nonnull
    final Object mLock = new Object();
    @Nonnull
    private final List<String> mProducts;
    @GuardedBy("mLock")
    @Nonnull
    private final Map<String, Boolean> mSupportedProducts = new HashMap<>();
    @GuardedBy("mLock")
    @Nonnull
    private final Listeners mListeners = new Listeners();
    @Nonnull
    private final OnLoadExecutor mOnLoadExecutor = new OnLoadExecutor();
    @GuardedBy("mLock")
    private Billing.Requests mRequests;
    @GuardedBy("mLock")
    @Nonnull
    private State mState = State.INITIAL;

    Checkout(@Nullable Context context, @Nonnull Billing billing, @Nonnull Collection<String> products) {
        Check.isNotEmpty(products);
        mBilling = billing;
        mContext = context;
        mProducts = new ArrayList<>(products);
    }

    Checkout(@Nullable Context context, @Nonnull Billing billing) {
        this(context, billing, Arrays.asList(ProductTypes.IN_APP, ProductTypes.SUBSCRIPTION));
    }

    /**
     * {@link ActivityCheckout#onActivityResult(int, int, android.content.Intent)} must be called
     * from the appropriate activity method.
     */
    @Nonnull
    public static ActivityCheckout forActivity(@Nonnull Activity activity, @Nonnull Billing billing, @Nonnull Collection<String> products) {
        return new ActivityCheckout(activity, billing, products);
    }

    @Nonnull
    public static ActivityCheckout forActivity(@Nonnull Activity activity, @Nonnull Billing billing) {
        return new ActivityCheckout(activity, billing);
    }

    @Nonnull
    public static ActivityCheckout forActivity(@Nonnull Activity activity, @Nonnull Checkout checkout) {
        return new ActivityCheckout(activity, checkout.mBilling, checkout.mProducts);
    }

    @Nonnull
    public static Checkout forService(@Nonnull Service service, @Nonnull Billing billing, @Nonnull Collection<String> products) {
        return new Checkout(service, billing, products);
    }

    @Nonnull
    public static Checkout forService(@Nonnull Service service, @Nonnull Billing billing) {
        return new Checkout(service, billing);
    }

    @Nonnull
    public static Checkout forApplication(@Nonnull Billing billing, @Nonnull Collection<String> products) {
        return new Checkout(null, billing, products);
    }

    @Nonnull
    public static Checkout forApplication(@Nonnull Billing billing) {
        return new Checkout(null, billing);
    }


    @Nonnull
    Context getContext() {
        return mBilling.getContext();
    }

    @Nonnull
    List<String> getProducts() {
        return mProducts;
    }

    public void start() {
        start(null);
    }

    public void start(@Nullable final Listener listener) {
        Check.isMainThread();

        synchronized (mLock) {
            Check.isFalse(mState == State.STARTED, "Already started");
            Check.isNull(mRequests, "Already started");
            mState = State.STARTED;
            mBilling.onCheckoutStarted();
            mRequests = mBilling.getRequests(mContext);
            if (listener != null) {
                mListeners.add(listener);
            }
            for (final String product : mProducts) {
                mRequests.isBillingSupported(product, new RequestListener<Object>() {
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

        synchronized (mLock) {
            for (Map.Entry<String, Boolean> entry : mSupportedProducts.entrySet()) {
                listener.onReady(mRequests, entry.getKey(), entry.getValue());
            }

            if (isReady()) {
                checkIsNotStopped();
                Check.isNotNull(mRequests);
                listener.onReady(mRequests);
            } else {
                // still waiting
                mListeners.add(listener);
            }
        }
    }

    private void checkIsNotStopped() {
        Check.isFalse(mState == State.STOPPED, "Checkout is stopped");
    }

    private boolean isReady() {
        Check.isTrue(Thread.holdsLock(mLock), "Should be called from synchronized block");
        return mSupportedProducts.size() == mProducts.size();
    }

    private void onBillingSupported(@Nonnull String product, boolean supported) {
        synchronized (mLock) {
            mSupportedProducts.put(product, supported);
            mListeners.onReady(mRequests, product, supported);
            if (isReady()) {
                mListeners.onReady(mRequests);
                mListeners.clear();
            }
        }
    }

    /**
     * Creates an {@link Inventory} object according to the {@link Billing.Configuration}. This
     * method also starts loading a list of {@link Sku}s in the created {@link Inventory}
     *
     * @param skus list of SKU ids to be loaded in the inventory
     * @param callback inventory listener
     * @return inventory
     */
    @Nonnull
    public Inventory loadInventory(@Nonnull Inventory.SkuIds skus, @Nonnull Inventory.Callback callback) {
        final Inventory inventory = makeInventory();
        inventory.load(skus, callback);
        return inventory;
    }

    /**
     * Creates an {@link Inventory} object according to the {@link Billing.Configuration}.
     * @return inventory
     */
    @Nonnull
    public Inventory makeInventory() {
        Check.isMainThread();

        synchronized (mLock) {
            checkIsNotStopped();
        }

        final Inventory inventory;
        final Inventory fallbackInventory = mBilling.getConfiguration().getFallbackInventory(this, mOnLoadExecutor);
        if (fallbackInventory == null) {
            inventory = new CheckoutInventory(this);
        } else {
            inventory = new FallingBackInventory(this, fallbackInventory);
        }
        return inventory;
    }

    /**
     * Method clears all listeners and cancels all pending requests. After this method is called no
     * more work can be done with this class unless {@link Checkout#start()} method is called
     * again.
     */
    public void stop() {
        Check.isMainThread();

        synchronized (mLock) {
            mSupportedProducts.clear();
            mListeners.clear();
            if (mState != State.INITIAL) {
                mState = State.STOPPED;
            }
            if (mRequests != null) {
                mRequests.cancelAll();
                mRequests = null;
            }
            if (mState == State.STOPPED) {
                mBilling.onCheckoutStopped();
            }
        }
    }

    public boolean isBillingSupported(@Nonnull String product) {
        Check.isTrue(mProducts.contains(product), "Product should be added to the products list");
        Check.isTrue(mSupportedProducts.containsKey(product), "Billing information is not ready yet");
        return mSupportedProducts.get(product);
    }

    private enum State {
        INITIAL,
        STARTED,
        STOPPED
    }

    /**
     * Initial request listener, all methods are called on the main application thread
     */
    public interface Listener {
        /**
         * Called when {@link BillingRequests#isBillingSupported(String, RequestListener)} finished
         * for all products
         *
         * @param requests requests ready to use
         */
        void onReady(@Nonnull BillingRequests requests);

        /**
         * Called when {@link BillingRequests#isBillingSupported(String, RequestListener)} finished
         * for <var>product</var> with <var>billingSupported</var> result
         *
         * @param requests         requests ready to use
         * @param product          product for which check was done
         * @param billingSupported true if billing is supported for <var>product</var>
         */
        void onReady(@Nonnull BillingRequests requests, @Nonnull String product, boolean billingSupported);
    }

    /**
     * This adapter class provides empty implementations of the methods from {@link
     * Checkout.Listener}. Any custom listener that cares only about a subset of the methods of this
     * listener can simply subclass this adapter class instead of implementing the interface
     * directly.
     */
    public static abstract class ListenerAdapter implements Listener {
        @Override
        public void onReady(@Nonnull BillingRequests requests) {
        }

        @Override
        public void onReady(@Nonnull BillingRequests requests, @Nonnull String product, boolean billingSupported) {
        }
    }

    private static final class Listeners implements Listener {
        @Nonnull
        private final List<Listener> list = new ArrayList<>();

        public void add(@Nonnull Listener l) {
            if (!list.contains(l)) {
                list.add(l);
            }
        }

        @Override
        public void onReady(@Nonnull BillingRequests requests) {
            final List<Listener> localList = new ArrayList<>(list);
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
            synchronized (mLock) {
                executor = mRequests != null ? mRequests.getDeliveryExecutor() : null;
            }

            if (executor != null) {
                executor.execute(command);
            } else {
                Billing.error("Trying to deliver result on a stopped checkout.");
            }
        }
    }
}
