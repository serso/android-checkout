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

import com.android.vending.billing.IInAppBillingService;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import static java.lang.System.currentTimeMillis;
import static org.solovyev.android.checkout.ResponseCodes.ITEM_ALREADY_OWNED;
import static org.solovyev.android.checkout.ResponseCodes.ITEM_NOT_OWNED;

public final class Billing {

    static final int V3 = 3;
    static final int V5 = 5;
    static final long SECOND = 1000L;
    static final long MINUTE = SECOND * 60L;
    static final long HOUR = MINUTE * 60L;
    static final long DAY = HOUR * 24L;
    @Nonnull
    private static final String TAG = "Checkout";
    @Nonnull
    private static final EmptyListener EMPTY_LISTENER = new EmptyListener();
    @Nonnull
    private static Logger LOGGER = new DefaultLogger();

    @Nonnull
    private final Context context;
    @Nonnull
    private final Object lock = new Object();
    @Nonnull
    private final Configuration configuration;
    @Nonnull
    private final ConcurrentCache cache;
    @Nonnull
    private final PendingRequests pendingRequests = new PendingRequests();
    @Nonnull
    private final BillingRequests requests = newRequestsBuilder().withTag(null).onBackgroundThread().create();
    @GuardedBy("lock")
    @Nullable
    private IInAppBillingService service;
    @GuardedBy("lock")
    @Nonnull
    private volatile State state = State.INITIAL;
    @Nonnull
    private CancellableExecutor mainThread;
    @Nonnull
    private Executor background = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(@Nonnull Runnable r) {
            return new Thread(r, "RequestThread");
        }
    });
    @Nonnull
    private ServiceConnector connector = new DefaultServiceConnector();

    @Nonnull
    private PurchaseVerifier purchaseVerifier;

    @GuardedBy("lock")
    private volatile int checkouts;

    /**
     * Same as {@link #Billing(android.content.Context, android.os.Handler, Configuration)} with new
     * handler
     */
    public Billing(@Nonnull Context context, @Nonnull Configuration configuration) {
        this(context, new Handler(), configuration);
        Check.isMainThread();
    }

    /**
     * Creates an instance. After creation, it will be ready to use. This constructor does not
     * block and is safe to call from a UI thread.
     *
     * @param context       application or activity context. Needed to bind to the in-app billing
     *                      service.
     * @param configuration billing configuration
     */
    public Billing(@Nonnull Context context, @Nonnull Handler handler, @Nonnull Configuration configuration) {
        if (context instanceof Application) {
            // context.getApplicationContext() might return null for applications as we allow create Billing before
            // Application#onCreate is called
            this.context = context;
        } else {
            this.context = context.getApplicationContext();
        }
        this.mainThread = new MainThread(handler);
        this.configuration = new StaticConfiguration(configuration);
        Check.isNotEmpty(this.configuration.getPublicKey());
        final Cache cache = configuration.getCache();
        this.cache = new ConcurrentCache(cache == null ? null : new SafeCache(cache));
        this.purchaseVerifier = configuration.getPurchaseVerifier();
    }

    /**
     * Sometimes Google Play is not that fast in updating information on device. Let's wait it a
     * little bit as if we
     * don't wait we might cache expired information (though, it will be updated soon as
     * RequestType#GET_PURCHASES
     * cache entry expires quite often)
     */
    static void waitGooglePlay() {
        try {
            Thread.sleep(100L);
        } catch (InterruptedException e) {
            error(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    private static <R> RequestListener<R> emptyListener() {
        return EMPTY_LISTENER;
    }

    static void error(@Nonnull String message) {
        LOGGER.e(TAG, message);
    }

    static void error(@Nonnull Exception e) {
        error(e.getMessage(), e);
    }

    static void error(@Nonnull String message, @Nonnull Exception e) {
        if (e instanceof BillingException) {
            final BillingException be = (BillingException) e;
            switch (be.getResponse()) {
                case ResponseCodes.OK:
                case ResponseCodes.USER_CANCELED:
                case ResponseCodes.ACCOUNT_ERROR:
                    LOGGER.e(TAG, message, e);
                    break;
                default:
                    LOGGER.e(TAG, message, e);
            }
        } else {
            LOGGER.e(TAG, message, e);
        }
    }

    static void debug(@Nonnull String subTag, @Nonnull String message) {
        LOGGER.d(TAG + "/" + subTag, message);
    }

    static void debug(@Nonnull String message) {
        LOGGER.d(TAG, message);
    }

    static void warning(@Nonnull String message) {
        LOGGER.w(TAG, message);
    }

    public static void setLogger(@Nullable Logger logger) {
        Billing.LOGGER = logger == null ? new EmptyLogger() : logger;
    }

    /**
     * @return default cache implementation
     */
    @Nonnull
    public static Cache newCache() {
        return new MapCache();
    }

    /**
     * @return default purchase verifier
     */
    @Nonnull
    public static PurchaseVerifier newPurchaseVerifier(@Nonnull String publicKey) {
        return new DefaultPurchaseVerifier(publicKey);
    }

    /**
     * Cancels listener recursively
     *
     * @param listener listener to be cancelled
     */
    static void cancel(@Nonnull RequestListener<?> listener) {
        if (listener instanceof CancellableRequestListener) {
            ((CancellableRequestListener) listener).cancel();
        }
    }

    @Nonnull
    public Context getContext() {
        return context;
    }

    @Nonnull
    Configuration getConfiguration() {
        return configuration;
    }

    @Nonnull
    ServiceConnector getConnector() {
        return connector;
    }

    void setConnector(@Nonnull ServiceConnector connector) {
        this.connector = connector;
    }

    void setService(@Nullable IInAppBillingService service, boolean connecting) {
        synchronized (lock) {
            final State newState;
            if (connecting) {
                if (state != State.CONNECTING) {
                    return;
                }
                if (service == null) {
                    newState = State.FAILED;
                } else {
                    newState = State.CONNECTED;
                }
            } else {
                if (state == State.INITIAL) {
                    // preserve initial state
                    return;
                }
                // service might be disconnected abruptly
                newState = State.DISCONNECTED;
            }
            this.service = service;
            setState(newState);
        }
    }

    void setBackground(@Nonnull Executor background) {
        this.background = background;
    }

    void setMainThread(@Nonnull CancellableExecutor mainThread) {
        this.mainThread = mainThread;
    }

    void setPurchaseVerifier(@Nonnull PurchaseVerifier purchaseVerifier) {
        this.purchaseVerifier = purchaseVerifier;
    }

    private void executePendingRequests() {
        background.execute(pendingRequests);
    }

    @Nonnull
    State getState() {
        synchronized (lock) {
            return state;
        }
    }

    void setState(@Nonnull State newState) {
        synchronized (lock) {
            if (state != newState) {
                state = newState;
                switch (state) {
                    case CONNECTED:
                        executePendingRequests();
                        break;
                    case FAILED:
                        mainThread.execute(new Runnable() {
                            @Override
                            public void run() {
                                pendingRequests.onConnectionFailed();
                            }
                        });
                        break;
                }
            }
        }
    }

    /**
     * Connects to Billing service. Called automatically when first request is done,
     * Use {@link #disconnect()} to disconnect.
     * It's allowed to call this method several times, if service is already connected nothing will
     * happen.
     */
    public void connect() {
        synchronized (lock) {
            if (state == State.CONNECTED) {
                executePendingRequests();
                return;
            }
            if (state == State.CONNECTING) {
                return;
            }
            if (configuration.isAutoConnect() && checkouts <= 0) {
                warning("Auto connection feature is turned on. There is no need in calling Billing.connect() manually. See Billing.Configuration.isAutoConnect");
            }
            setState(State.CONNECTING);
            mainThread.execute(new Runnable() {
                @Override
                public void run() {
                    connectOnMainThread();
                }
            });
        }
    }

    private void connectOnMainThread() {
        Check.isMainThread();
        final boolean connecting = connector.connect();
        if (!connecting) {
            setState(State.FAILED);
        }
    }

    /**
     * Disconnects from Billing service cancelling all pending requests. Any subsequent
     * request will automatically reconnect Billing service, thus, don't run any requests after
     * disconnection (otherwise Billing service will be connected again).
     * It's allowed to call this method several times, if service is already disconnected nothing
     * will happen.
     */
    public void disconnect() {
        synchronized (lock) {
            if (state == State.DISCONNECTED || state == State.DISCONNECTING || state == State.INITIAL) {
                return;
            }
            setState(State.DISCONNECTING);
            mainThread.execute(new Runnable() {
                @Override
                public void run() {
                    disconnectOnMainThread();
                }
            });
            pendingRequests.cancelAll();
        }
    }

    private void disconnectOnMainThread() {
        Check.isMainThread();
        connector.disconnect();
    }

    private int runWhenConnected(@Nonnull Request request, @Nullable Object tag) {
        return runWhenConnected(request, null, tag);
    }

    <R> int runWhenConnected(@Nonnull Request<R> request, @Nullable RequestListener<R> listener, @Nullable Object tag) {
        if (listener != null) {
            if (cache.hasCache()) {
                listener = new CachingRequestListener<R>(request, listener);
            }
            request.setListener(listener);
        }
        if (tag != null) {
            request.setTag(tag);
        }

        pendingRequests.add(onConnectedService(request));
        connect();

        return request.getId();
    }

    /**
     * Cancels request with <var>requestId</var>
     *
     * @param requestId id of request
     */
    public void cancel(int requestId) {
        pendingRequests.cancel(requestId);
    }

    /**
     * Cancels all billing requests
     */
    public void cancelAll() {
        pendingRequests.cancelAll();
    }

    @Nonnull
    private RequestRunnable onConnectedService(@Nonnull final Request request) {
        return new OnConnectedServiceRunnable(request);
    }

    /**
     * @return new requests builder
     */
    @Nonnull
    public RequestsBuilder newRequestsBuilder() {
        return new RequestsBuilder();
    }

    /**
     * Requests executed on the returned object will be marked with <var>activity</var> tag and will
     * be delivered on the
     * main application thread
     *
     * @param activity activity
     * @return requests for given <var>activity</var>
     */
    @Nonnull
    public BillingRequests getRequests(@Nonnull Activity activity) {
        return new RequestsBuilder().withTag(activity).onMainThread().create();
    }

    /**
     * Requests executed on the returned object will be marked with <var>service</var> tag and will
     * be delivered on the
     * main application thread
     *
     * @param service service
     * @return requests for given <var>context</var>
     */
    @Nonnull
    public BillingRequests getRequests(@Nonnull Service service) {
        return new RequestsBuilder().withTag(service).onMainThread().create();
    }

    /**
     * Requests executed on the returned object will be marked with no tag and will be delivered on
     * the
     * main application thread
     *
     * @return default application requests
     */
    @Nonnull
    public BillingRequests getRequests() {
        return requests;
    }

    @Nonnull
    Requests getRequests(@Nullable Context context) {
        if (context instanceof Activity) {
            return (Requests) getRequests((Activity) context);
        } else if (context instanceof Service) {
            return (Requests) getRequests((Service) context);
        } else {
            Check.isNull(context);
            return (Requests) getRequests();
        }
    }

    @Nonnull
    PurchaseFlow createPurchaseFlow(@Nonnull Activity activity, int requestCode, @Nonnull RequestListener<Purchase> listener) {
        if (cache.hasCache()) {
            listener = new RequestListenerWrapper<Purchase>(listener) {
                @Override
                public void onSuccess(@Nonnull Purchase result) {
                    cache.removeAll(RequestType.GET_PURCHASES.getCacheKeyType());
                    super.onSuccess(result);
                }
            };
        }
        return new PurchaseFlow(activity, requestCode, listener, purchaseVerifier);
    }

    @Nonnull
    <R> RequestListener<R> onMainThread(@Nonnull final RequestListener<R> listener) {
        return new MainThreadRequestListener<R>(mainThread, listener);
    }

    public void onCheckoutStarted() {
        Check.isMainThread();
        synchronized (lock) {
            checkouts++;
            if (checkouts > 0 && configuration.isAutoConnect()) {
                connect();
            }
        }
    }

    public void onCheckoutStopped() {
        Check.isMainThread();
        synchronized (lock) {
            checkouts--;
            if (checkouts < 0) {
                checkouts = 0;
                warning("Billing#onCheckoutStopped is called more than Billing#onCheckoutStarted");
            }
            if (checkouts == 0 && configuration.isAutoConnect()) {
                disconnect();
            }
        }
    }

    /**
     * Service connection state
     */
    enum State {
        /**
         * Service is not connected, no requests can be done, initial state
         */
        INITIAL,
        /**
         * Service is connecting
         */
        CONNECTING,
        /**
         * Service is connected, requests can be executed
         */
        CONNECTED,
        /**
         * Service is disconnecting
         */
        DISCONNECTING,
        /**
         * Service is disconnected
         */
        DISCONNECTED,
        /**
         * Service failed to connect
         */
        FAILED,
    }

    static interface ServiceConnector {
        boolean connect();

        void disconnect();
    }

    public static interface Configuration {
        /**
         * @return application's public key, encoded in base64.
         * This is used for verification of purchase signatures. You can find app's base64-encoded
         * public key in application's page on Google Play Developer Console. Note that this
         * is NOT "developer public key".
         */
        @Nonnull
        String getPublicKey();

        /**
         * @return cache instance to be used for caching, null for no caching
         * @see Billing#newCache()
         */
        @Nullable
        Cache getCache();

        /**
         * @return {@link PurchaseVerifier} to be used to validate the purchases
         * @see org.solovyev.android.checkout.PurchaseVerifier
         */
        @Nonnull
        PurchaseVerifier getPurchaseVerifier();

        /**
         * @param checkout       checkout
         * @param onLoadExecutor executor to be used to call {@link org.solovyev.android.checkout.Inventory.Listener}
         *                       methods
         * @return inventory to be used if Billing v.3 is not supported
         */
        @Nullable
        Inventory getFallbackInventory(@Nonnull Checkout checkout, @Nonnull Executor onLoadExecutor);

        /**
         * Return true if you want Billing to connect to/disconnect from Billing API Service
         * automatically. If this method returns true then there is not need in calling {@link
         * Billing#connect()}
         * or {@link Billing#disconnect()} manually.
         *
         * @return true if Billing should connect to/disconnect from Billing API service
         * automatically
         * according to the number of started Checkouts
         */
        boolean isAutoConnect();
    }

    /**
     * Dummy listener, used if user didn't provide {@link RequestListener}
     *
     * @param <R> type of result
     */
    private static class EmptyListener<R> implements RequestListener<R> {
        @Override
        public void onSuccess(@Nonnull R result) {
        }

        @Override
        public void onError(int response, @Nonnull Exception e) {
        }
    }

    public abstract static class DefaultConfiguration implements Configuration {
        @Nullable
        @Override
        public Cache getCache() {
            return newCache();
        }

        @Nonnull
        @Override
        public PurchaseVerifier getPurchaseVerifier() {
            Billing.warning("Default purchase verification procedure is used, please read https://github.com/serso/android-checkout#purchase-verification");
            return newPurchaseVerifier(getPublicKey());
        }

        @Nullable
        @Override
        public Inventory getFallbackInventory(@Nonnull Checkout checkout, @Nonnull Executor onLoadExecutor) {
            return null;
        }

        @Override
        public boolean isAutoConnect() {
            return true;
        }
    }

    /**
     * Gets public key only once, all other methods are called from original configuration
     */
    private static final class StaticConfiguration implements Configuration {
        @Nonnull
        private final Configuration original;

        @Nonnull
        private final String publicKey;

        private StaticConfiguration(@Nonnull Configuration original) {
            this.original = original;
            this.publicKey = original.getPublicKey();
        }

        @Nonnull
        @Override
        public String getPublicKey() {
            return publicKey;
        }

        @Nullable
        @Override
        public Cache getCache() {
            return original.getCache();
        }

        @Nonnull
        @Override
        public PurchaseVerifier getPurchaseVerifier() {
            return original.getPurchaseVerifier();
        }

        @Nullable
        @Override
        public Inventory getFallbackInventory(@Nonnull Checkout checkout, @Nonnull Executor onLoadExecutor) {
            return original.getFallbackInventory(checkout, onLoadExecutor);
        }

        @Override
        public boolean isAutoConnect() {
            return original.isAutoConnect();
        }
    }

    private final class OnConnectedServiceRunnable implements RequestRunnable {

        @GuardedBy("this")
        @Nullable
        private Request request;

        public OnConnectedServiceRunnable(@Nonnull Request request) {
            this.request = request;
        }

        @Override
        public boolean run() {
            final Request localRequest = getRequest();
            if (localRequest == null) {
                // request was cancelled => finish here
                return true;
            }

            if (checkCache(localRequest)) return true;

            // request is alive, let's check the service state
            final State localState;
            final IInAppBillingService localService;
            synchronized (lock) {
                localState = state;
                localService = service;
            }
            if (localState == State.CONNECTED) {
                Check.isNotNull(localService);
                // service is connected, let's start request
                try {
                    localRequest.start(localService, context.getPackageName());
                } catch (RemoteException e) {
                    localRequest.onError(e);
                } catch (RequestException e) {
                    localRequest.onError(e);
                } catch (RuntimeException e) {
                    localRequest.onError(e);
                }
            } else {
                // service is not connected, let's check why
                if (localState != State.FAILED) {
                    // service was disconnected
                    connect();
                    return false;
                } else {
                    // service was not connected in the first place => can't do anything, aborting the request
                    localRequest.onError(ResponseCodes.SERVICE_NOT_CONNECTED);
                }
            }

            return true;
        }

        private boolean checkCache(@Nonnull Request request) {
            if (cache.hasCache()) {
                final String key = request.getCacheKey();
                if (key != null) {
                    final Cache.Entry entry = cache.get(request.getType().getCacheKey(key));
                    if (entry != null) {
                        //noinspection unchecked
                        request.onSuccess(entry.data);
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        @Nullable
        public Request getRequest() {
            synchronized (this) {
                return request;
            }
        }

        public void cancel() {
            synchronized (this) {
                if (request != null) {
                    Billing.debug("Cancelling request: " + request);
                    request.cancel();
                }
                request = null;
            }
        }

        @Override
        public int getId() {
            synchronized (this) {
                return request != null ? request.getId() : -1;
            }
        }

        @Nullable
        @Override
        public Object getTag() {
            synchronized (this) {
                return request != null ? request.getTag() : null;
            }
        }

        @Override
        public String toString() {
            return String.valueOf(request);
        }
    }

    /**
     * {@link org.solovyev.android.checkout.BillingRequests} builder. Allows to specify request tags
     * and
     * result delivery methods
     */
    public final class RequestsBuilder {
        @Nullable
        private Object tag;
        @Nullable
        private Boolean onMainThread;

        private RequestsBuilder() {
        }

        @Nonnull
        public RequestsBuilder withTag(@Nullable Object tag) {
            Check.isNull(this.tag);
            this.tag = tag;
            return this;
        }

        @Nonnull
        public RequestsBuilder onBackgroundThread() {
            Check.isNull(onMainThread);
            onMainThread = false;
            return this;
        }

        @Nonnull
        public RequestsBuilder onMainThread() {
            Check.isNull(onMainThread);
            onMainThread = true;
            return this;
        }

        @Nonnull
        public BillingRequests create() {
            return new Requests(tag, onMainThread == null ? true : onMainThread);
        }
    }

    final class Requests implements BillingRequests {

        @Nullable
        private final Object tag;

        private final boolean onMainThread;

        private Requests(@Nullable Object tag, boolean onMainThread) {
            this.tag = tag;
            this.onMainThread = onMainThread;
        }

        @Override
        public int isBillingSupported(@Nonnull String product) {
            return isBillingSupported(product, emptyListener());
        }

        @Override
        public int isBillingSupported(@Nonnull String product, int apiVersion) {
            return isBillingSupported(product, apiVersion, emptyListener());
        }

        @Override
        public int isBillingSupported(@Nonnull String product, int apiVersion,
                                      @Nonnull RequestListener<Object> listener) {
            Check.isNotEmpty(product);
            return runWhenConnected(new BillingSupportedRequest(product, apiVersion), wrapListener(listener), tag);
        }

        @Override
        public int isBillingSupported(@Nonnull final String product, @Nonnull RequestListener<Object> listener) {
            return isBillingSupported(product, V3, listener);
        }

        @Nonnull
        private <R> RequestListener<R> wrapListener(@Nonnull RequestListener<R> listener) {
            return onMainThread ? onMainThread(listener) : listener;
        }

        @Nonnull
        Executor getDeliveryExecutor() {
            return onMainThread ? mainThread : SameThreadExecutor.INSTANCE;
        }

        @Override
        public int getPurchases(@Nonnull final String product, @Nullable final String continuationToken, @Nonnull RequestListener<Purchases> listener) {
            Check.isNotEmpty(product);
            return runWhenConnected(new GetPurchasesRequest(product, continuationToken, purchaseVerifier), wrapListener(listener), tag);
        }

        @Override
        public int getAllPurchases(@Nonnull String product, @Nonnull RequestListener<Purchases> listener) {
            Check.isNotEmpty(product);
            final GetAllPurchasesListener getAllPurchasesListener = new GetAllPurchasesListener(listener);
            final GetPurchasesRequest request = new GetPurchasesRequest(product, null, purchaseVerifier);
            getAllPurchasesListener.request = request;
            return runWhenConnected(request, wrapListener(getAllPurchasesListener), tag);
        }

        @Override
        public int isPurchased(@Nonnull final String product, @Nonnull final String sku, @Nonnull final RequestListener<Boolean> listener) {
            Check.isNotEmpty(sku);
            final IsPurchasedListener isPurchasedListener = new IsPurchasedListener(sku, listener);
            final GetPurchasesRequest request = new GetPurchasesRequest(product, null, purchaseVerifier);
            isPurchasedListener.request = request;
            return runWhenConnected(request, wrapListener(isPurchasedListener), tag);
        }

        @Override
        public int getSkus(@Nonnull String product, @Nonnull List<String> skus, @Nonnull RequestListener<Skus> listener) {
            Check.isNotEmpty(product);
            Check.isNotEmpty(skus);
            return runWhenConnected(new GetSkuDetailsRequest(product, skus), wrapListener(listener), tag);
        }

        @Override
        public int purchase(@Nonnull String product, @Nonnull String sku, @Nullable String payload, @Nonnull PurchaseFlow purchaseFlow) {
            Check.isNotEmpty(product);
            Check.isNotEmpty(sku);
            return runWhenConnected(new PurchaseRequest(product, sku, payload), wrapListener(purchaseFlow), tag);
        }

        @Override
        public int changeSubscription(@Nonnull List<String> oldSkus,
                                      @Nonnull String newSku, @Nullable String payload,
                                      @Nonnull PurchaseFlow purchaseFlow) {
            Check.isNotEmpty(oldSkus);
            Check.isNotEmpty(newSku);
            return runWhenConnected(
                    new ChangePurchaseRequest(ProductTypes.SUBSCRIPTION, oldSkus, newSku, payload),
                    wrapListener(purchaseFlow), tag);
        }

        @Override
        public int changeSubscription(@Nonnull List<Sku> oldSkus, @Nonnull Sku newSku,
                                      @Nullable String payload, @Nonnull PurchaseFlow purchaseFlow) {
            Check.isTrue(ProductTypes.SUBSCRIPTION.equals(newSku.product), "Only subscriptions can be downgraded/upgraded");
            final List<String> oldSkuIds = new ArrayList<>(oldSkus.size());
            for (Sku oldSku : oldSkus) {
                Check.isTrue(oldSku.product.equals(newSku.product), "Product type can't be changed");
                oldSkuIds.add(oldSku.id);
            }
            return changeSubscription(oldSkuIds, newSku.id, payload, purchaseFlow);
        }

        @Override
        public int isChangeSubscriptionSupported(RequestListener<Object> listener) {
            return isBillingSupported(ProductTypes.SUBSCRIPTION, Billing.V5, listener);
        }

        @Override
        public int purchase(@Nonnull Sku sku, @Nullable String payload, @Nonnull PurchaseFlow purchaseFlow) {
            return purchase(sku.product, sku.id, payload, purchaseFlow);
        }

        @Override
        public int consume(@Nonnull String token, @Nonnull RequestListener<Object> listener) {
            Check.isNotEmpty(token);
            return runWhenConnected(new ConsumePurchaseRequest(token), wrapListener(listener), tag);
        }

        @Override
        public void cancelAll() {
            pendingRequests.cancelAll(tag);
        }

        /**
         * This class waits for the result from {@link GetPurchasesRequest} and checks if purchases
         * contains specified
         * <var>sku</var>. If there is a <var>continuationToken</var> and item can't be found in
         * this bulk of purchases
         * another (recursive) request is executed (to load other purchases) and the search is done
         * again. New (additional)
         * request has the same ID and listener as original request, thus, can be cancelled with
         * original request ID.
         */
        private final class IsPurchasedListener implements CancellableRequestListener<Purchases> {

            @Nonnull
            private final String sku;
            @Nonnull
            private final RequestListener<Boolean> listener;
            @Nonnull
            private GetPurchasesRequest request;

            public IsPurchasedListener(@Nonnull String sku, @Nonnull RequestListener<Boolean> listener) {
                this.sku = sku;
                this.listener = listener;
            }

            @Override
            public void onSuccess(@Nonnull Purchases purchases) {
                final Purchase purchase = purchases.getPurchase(sku);
                if (purchase != null) {
                    listener.onSuccess(purchase.state == Purchase.State.PURCHASED);
                } else {
                    // we need to check continuation token
                    if (purchases.continuationToken != null) {
                        request = new GetPurchasesRequest(request, purchases.continuationToken);
                        runWhenConnected(request, tag);
                    } else {
                        listener.onSuccess(false);
                    }
                }
            }

            @Override
            public void onError(int response, @Nonnull Exception e) {
                listener.onError(response, e);
            }

            @Override
            public void cancel() {
                Billing.cancel(listener);
            }
        }

        private final class GetAllPurchasesListener implements CancellableRequestListener<Purchases> {

            @Nonnull
            private final RequestListener<Purchases> listener;
            private final List<Purchase> result = new ArrayList<Purchase>();
            @Nonnull
            private GetPurchasesRequest request;

            public GetAllPurchasesListener(@Nonnull RequestListener<Purchases> listener) {
                this.listener = listener;
            }

            @Override
            public void onSuccess(@Nonnull Purchases purchases) {
                result.addAll(purchases.list);
                // we need to check continuation token
                if (purchases.continuationToken != null) {
                    request = new GetPurchasesRequest(request, purchases.continuationToken);
                    runWhenConnected(request, tag);
                } else {
                    listener.onSuccess(new Purchases(purchases.product, result, null));
                }
            }

            @Override
            public void onError(int response, @Nonnull Exception e) {
                listener.onError(response, e);
            }

            @Override
            public void cancel() {
                Billing.cancel(listener);
            }
        }

    }

    private class CachingRequestListener<R> extends RequestListenerWrapper<R> {
        @Nonnull
        private final Request<R> request;

        public CachingRequestListener(@Nonnull Request<R> request, @Nonnull RequestListener<R> listener) {
            super(listener);
            Check.isTrue(cache.hasCache(), "Cache must exist");
            this.request = request;
        }

        @Override
        public void onSuccess(@Nonnull R result) {
            final String key = request.getCacheKey();
            final RequestType type = request.getType();
            if (key != null) {
                final long now = currentTimeMillis();
                final Cache.Entry entry = new Cache.Entry(result, now + type.expiresIn);
                cache.putIfNotExist(type.getCacheKey(key), entry);
            }
            switch (type) {
                case PURCHASE:
                case CHANGE_PURCHASE:
                case CONSUME_PURCHASE:
                    // these requests might affect the state of purchases => we need to invalidate caches.
                    // see Billing#onPurchaseFinished() also
                    cache.removeAll(RequestType.GET_PURCHASES.getCacheKeyType());
                    break;
            }
            super.onSuccess(result);
        }

        @Override
        public void onError(int response, @Nonnull Exception e) {
            final RequestType type = request.getType();
            // sometimes it is possible that cached data is not synchronized with data on Google Play => we can
            // clear caches if such situation occurred
            switch (type) {
                case PURCHASE:
                case CHANGE_PURCHASE:
                    if (response == ITEM_ALREADY_OWNED) {
                        cache.removeAll(RequestType.GET_PURCHASES.getCacheKeyType());
                    }
                    break;
                case CONSUME_PURCHASE:
                    if (response == ITEM_NOT_OWNED) {
                        cache.removeAll(RequestType.GET_PURCHASES.getCacheKeyType());
                    }
                    break;
            }
            super.onError(response, e);
        }
    }

    private final class DefaultServiceConnector implements ServiceConnector {

        @Nonnull
        private final ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                setService(null, false);
            }

            @Override
            public void onServiceConnected(ComponentName name,
                                           IBinder service) {
                setService(IInAppBillingService.Stub.asInterface(service), true);
            }
        };

        @Override
        public boolean connect() {
            try {
                final Intent intent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
                intent.setPackage("com.android.vending");
                return context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
            } catch (IllegalArgumentException e) {
                // some devices throw IllegalArgumentException (Service Intent must be explicit)
                // even though we set package name explicitly. Let's not crash the app and catch
                // such exceptions here, the billing on such devices will not work.
                return false;
            }
        }

        @Override
        public void disconnect() {
            context.unbindService(connection);
        }
    }

}
