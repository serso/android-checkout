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

import com.android.vending.billing.InAppBillingServiceImpl;
import com.android.vending.billing.InAppBillingService;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
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

/**
 * A core class of the Checkout's implementation of Android's Billing API.
 * This class is responsible for:
 * <ol>
 * <li>Connecting and disconnecting to the billing service</li>
 * <li>Performing billing requests</li>
 * <li>Caching the requests results</li>
 * <li>Creating {@link Checkout} objects</li>
 * <li>Logging</li>
 * </ol>
 * Though, this class can be used on its own to obtain the billing information from Android it's
 * recommended to use higher abstractions, such as {@link Checkout} and {@link Inventory}, for such
 * purposes.
 */
@SuppressWarnings("WeakerAccess")
public final class Billing {

    static final int V3 = 3;
    static final int V5 = 5;
    static final int V6 = 6;
    static final int V7 = 7;

    static final long SECOND = 1000L;
    static final long MINUTE = SECOND * 60L;
    static final long HOUR = MINUTE * 60L;
    static final long DAY = HOUR * 24L;

    @Nonnull
    private static final String TAG = "Checkout";
    @Nonnull
    private static final EmptyRequestListener sEmptyListener = new EmptyRequestListener();
    // a list of states from which transition to this state is allowed
    @Nonnull
    private static final EnumMap<State, List<State>> sPreviousStates = new EnumMap<>(State.class);
    @Nonnull
    private static Logger sLogger = newLogger();

    static {
        sPreviousStates.put(State.INITIAL, Collections.<State>emptyList());
        sPreviousStates.put(State.CONNECTING, Arrays.asList(State.INITIAL, State.FAILED, State.DISCONNECTED, State.DISCONNECTING));
        sPreviousStates.put(State.CONNECTED, Collections.singletonList(State.CONNECTING));
        sPreviousStates.put(State.DISCONNECTING, Collections.singletonList(State.CONNECTED));
        sPreviousStates.put(State.DISCONNECTED, Arrays.asList(State.DISCONNECTING, State.CONNECTING));
        sPreviousStates.put(State.FAILED, Collections.singletonList(State.CONNECTING));
    }

    @Nonnull
    private final Context mContext;
    @Nonnull
    private final Object mLock = new Object();
    @Nonnull
    private final StaticConfiguration mConfiguration;
    @Nonnull
    private final ConcurrentCache mCache;
    @Nonnull
    private final PendingRequests mPendingRequests = new PendingRequests();
    @Nonnull
    private final BillingRequests mRequests = newRequestsBuilder().withTag(null).onBackgroundThread().create();
    @GuardedBy("mLock")
    @Nonnull
    private final PlayStoreBroadcastReceiver mPlayStoreBroadcastReceiver;
    @Nonnull
    private final PlayStoreListener mPlayStoreListener = new PlayStoreListener() {
        @Override
        public void onPurchasesChanged() {
            mCache.removeAll(RequestType.GET_PURCHASES.getCacheKeyType());
        }
    };
    @GuardedBy("mLock")
    @Nullable
    private InAppBillingService mService;
    @GuardedBy("mLock")
    @Nonnull
    private State mState = State.INITIAL;
    @Nonnull
    private CancellableExecutor mMainThread;
    @Nonnull
    private Executor mBackground = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(@Nonnull Runnable r) {
            return new Thread(r, "RequestThread");
        }
    });
    @Nonnull
    private ServiceConnector mConnector = new DefaultServiceConnector();
    @GuardedBy("mLock")
    private int mCheckoutCount;

    public Billing(@Nonnull Context context, @Nonnull Configuration configuration) {
        this(context, new Handler(), configuration);
        Check.isMainThread();
    }

    /**
     * @param context       application or activity context. Needed to bind to the in-app billing
     *                      service.
     * @param configuration billing configuration
     */
    public Billing(@Nonnull Context context, @Nonnull Handler handler, @Nonnull Configuration configuration) {
        if (context instanceof Application) {
            // mContext.getApplicationContext() might return null for applications as we allow create Billing before
            // Application#onCreate is called
            mContext = context;
        } else {
            mContext = context.getApplicationContext();
        }
        mMainThread = new MainThread(handler);
        mConfiguration = new StaticConfiguration(configuration);
        Check.isNotEmpty(mConfiguration.getPublicKey());
        final Cache cache = configuration.getCache();
        mCache = new ConcurrentCache(cache == null ? null : new SafeCache(cache));
        mPlayStoreBroadcastReceiver = new PlayStoreBroadcastReceiver(mContext, mLock);
    }

    /**
     * Sometimes Google Play is not that fast in updating information on device. Let's wait it a
     * little bit as if we don't wait we might cache expired information (though, it will be
     * updated soon as RequestType#GET_PURCHASES cache entry expires quite often)
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
        return sEmptyListener;
    }

    static void error(@Nonnull String message) {
        sLogger.e(TAG, message);
    }

    static void error(@Nonnull Exception e) {
        final String msg = e.getMessage();
        error(msg == null ? "" : msg, e);
    }

    static void error(@Nonnull String message, @Nonnull Exception e) {
        if (e instanceof BillingException) {
            final BillingException be = (BillingException) e;
            switch (be.getResponse()) {
                case ResponseCodes.OK:
                case ResponseCodes.USER_CANCELED:
                case ResponseCodes.ACCOUNT_ERROR:
                    sLogger.e(TAG, message, e);
                    break;
                default:
                    sLogger.e(TAG, message, e);
            }
        } else {
            sLogger.e(TAG, message, e);
        }
    }

    static void debug(@Nonnull String subTag, @Nonnull String message) {
        sLogger.d(TAG + "/" + subTag, message);
    }

    static void debug(@Nonnull String message) {
        sLogger.d(TAG, message);
    }

    static void warning(@Nonnull String message) {
        sLogger.w(TAG, message);
    }

    public static void setLogger(@Nullable Logger logger) {
        Billing.sLogger = logger == null ? new EmptyLogger() : logger;
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
     * @return default logger
     */
    @Nonnull
    public static Logger newLogger() {
        return new DefaultLogger();
    }

    /**
     * @return logger whose methods are called only on the main thread
     */
    @Nonnull
    public static Logger newMainThreadLogger(@Nonnull Logger logger) {
        return new MainThreadLogger(logger);
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
        return mContext;
    }

    @Nonnull
    Configuration getConfiguration() {
        return mConfiguration;
    }

    @Nonnull
    ServiceConnector getConnector() {
        return mConnector;
    }

    void setConnector(@Nonnull ServiceConnector connector) {
        mConnector = connector;
    }

    void setService(@Nullable InAppBillingService service, boolean connecting) {
        synchronized (mLock) {
            final State newState;
            if (connecting) {
                if (mState != State.CONNECTING) {
                    // don't leak the service and disconnect directly without going through Billing#setState
                    if (service != null) {
                        mConnector.disconnect();
                    }
                    return;
                }
                newState = service == null ? State.FAILED : State.CONNECTED;
            } else {
                if (mState == State.INITIAL || mState == State.DISCONNECTED || mState == State.FAILED) {
                    // preserve the state
                    Check.isNull(mService);
                    return;
                }
                // service might be disconnected abruptly but we must go through CONNECTED->DISCONNECTING->DISCONNECTED
                // routine to free the acquired resources. If, however, the current state was not
                // CONNECTED (only one option left is CONNECTING) then we should directly jump to
                // FAILED state as something strange has happened on the billing service side
                if (mState == State.CONNECTED) {
                    setState(State.DISCONNECTING);
                }
                if (mState == State.DISCONNECTING) {
                    newState = State.DISCONNECTED;
                } else {
                    Check.isTrue(mState == State.CONNECTING, "Unexpected state: " + mState);
                    // DISCONNECTED state can occur only after the established connection. If the
                    // connection was never established it's a
                    newState = State.FAILED;
                }
            }
            mService = service;
            setState(newState);
        }
    }

    void setBackground(@Nonnull Executor background) {
        mBackground = background;
    }

    void setMainThread(@Nonnull CancellableExecutor mainThread) {
        mMainThread = mainThread;
    }

    void setPurchaseVerifier(@Nonnull PurchaseVerifier purchaseVerifier) {
        mConfiguration.setPurchaseVerifier(purchaseVerifier);
    }

    private void executePendingRequests() {
        mBackground.execute(mPendingRequests);
    }

    @Nonnull
    State getState() {
        synchronized (mLock) {
            return mState;
        }
    }

    void setState(@Nonnull State newState) {
        synchronized (mLock) {
            if (mState == newState) {
                return;
            }
            Check.isTrue(sPreviousStates.get(newState).contains(mState), "State " + newState + " can't come right after " + mState + " state");
            mState = newState;
            switch (mState) {
                case DISCONNECTING:
                    // as we can jump directly from DISCONNECTING to CONNECTED state let's remove
                    // the listener here instead of in DISCONNECTED state. That also will protect
                    // us from getting in the following trap: CONNECTED->DISCONNECTING->CONNECTING->FAILED
                    mPlayStoreBroadcastReceiver.removeListener(mPlayStoreListener);
                    break;
                case CONNECTED:
                    // CONNECTED is the only state when we know for sure that Play Store is available.
                    // Registering the listener here also means that it should be never registered
                    // in the FAILED state
                    mPlayStoreBroadcastReceiver.addListener(mPlayStoreListener);
                    executePendingRequests();
                    break;
                case FAILED:
                    // the play store listener should not be registered in the receiver in case of
                    // failure as FAILED state can't occur after CONNECTED
                    Check.isTrue(!mPlayStoreBroadcastReceiver.contains(mPlayStoreListener), "Leaking the listener");
                    mMainThread.execute(new Runnable() {
                        @Override
                        public void run() {
                            mPendingRequests.onConnectionFailed();
                        }
                    });
                    break;
            }
        }
    }

    /**
     * Connects to the Billing service. Called automatically when first request is done,
     * Use {@link #disconnect()} to disconnect.
     * It's allowed to call this method several times, if service is already connected nothing will
     * happen.
     */
    public void connect() {
        synchronized (mLock) {
            if (mState == State.CONNECTED) {
                executePendingRequests();
                return;
            }
            if (mState == State.CONNECTING) {
                return;
            }
            if (mConfiguration.isAutoConnect() && mCheckoutCount <= 0) {
                warning("Auto connection feature is turned on. There is no need in calling Billing.connect() manually. See Billing.Configuration.isAutoConnect");
            }
            setState(State.CONNECTING);
            mMainThread.execute(new Runnable() {
                @Override
                public void run() {
                    connectOnMainThread();
                }
            });
        }
    }

    private void connectOnMainThread() {
        Check.isMainThread();
        final boolean connecting = mConnector.connect();
        if (!connecting) {
            setState(State.FAILED);
        }
    }

    /**
     * Adds {@link PlayStoreListener} possibly registering a {@link android.content.BroadcastReceiver}
     * responsible for getting "com.android.vending.billing.PURCHASES_UPDATED" intent from the Play
     * Store.
     *
     * @param listener listener to be added
     */
    public void addPlayStoreListener(@Nonnull PlayStoreListener listener) {
        synchronized (mLock) {
            mPlayStoreBroadcastReceiver.addListener(listener);
        }
    }

    /**
     * Removes previously added {@link PlayStoreListener}. This method might also unregister the
     * {@link android.content.BroadcastReceiver}.
     *
     * @param listener listener to be removed
     */
    public void removePlayStoreListener(@Nonnull PlayStoreListener listener) {
        synchronized (mLock) {
            mPlayStoreBroadcastReceiver.removeListener(listener);
        }
    }

    /**
     * Disconnects from the Billing service cancelling all pending requests if any. Any subsequent
     * request will automatically reconnect the Billing service. Thus, no more requests should be
     * scheduled after this method has been called (otherwise the service will be connected again).
     * It's allowed to call this method several times, if the service is already disconnected
     * nothing happens.
     */
    public void disconnect() {
        synchronized (mLock) {
            if (mState == State.DISCONNECTED || mState == State.DISCONNECTING || mState == State.INITIAL) {
                return;
            }
            if (mState == State.FAILED) {
                // it would be strange to change the state from FAILED to DISCONNECTING/DISCONNECTED,
                // thus, just cancelling all pending the requested here and returning without updating
                // the state
                mPendingRequests.cancelAll();
                return;
            }
            if (mState == State.CONNECTED) {
                setState(State.DISCONNECTING);
                mMainThread.execute(new Runnable() {
                    @Override
                    public void run() {
                        disconnectOnMainThread();
                    }
                });
            } else {
                // if we're still CONNECTING - skip DISCONNECTING state
                setState(State.DISCONNECTED);
            }
            // requests should be cancelled only when Billing#disconnect() is called explicitly as
            // it's only then we know for sure that no more work should be done
            mPendingRequests.cancelAll();
        }
    }

    private void disconnectOnMainThread() {
        Check.isMainThread();
        mConnector.disconnect();
    }

    private int runWhenConnected(@Nonnull Request request, @Nullable Object tag) {
        return runWhenConnected(request, null, tag);
    }

    <R> int runWhenConnected(@Nonnull Request<R> request, @Nullable RequestListener<R> listener, @Nullable Object tag) {
        if (listener != null) {
            if (mCache.hasCache()) {
                listener = new CachingRequestListener<>(request, listener);
            }
            request.setListener(listener);
        }
        if (tag != null) {
            request.setTag(tag);
        }

        mPendingRequests.add(onConnectedService(request));
        connect();

        return request.getId();
    }

    /**
     * Cancels a pending request with the given <var>requestId</var>.
     *
     * @param requestId id of request
     */
    public void cancel(int requestId) {
        mPendingRequests.cancel(requestId);
    }

    /**
     * Cancels all pending requests.
     */
    public void cancelAll() {
        mPendingRequests.cancelAll();
    }

    @Nonnull
    private RequestRunnable onConnectedService(@Nonnull final Request request) {
        return new OnConnectedServiceRunnable(request);
    }

    /**
     * A factory method of {@link RequestsBuilder}.
     *
     * @return new instance of {@link RequestsBuilder}
     */
    @Nonnull
    public RequestsBuilder newRequestsBuilder() {
        return new RequestsBuilder();
    }

    /**
     * @return default requests object associated with this {@link Billing} class. All methods of
     * {@link RequestListener} used in it are called on the main application thread.
     */
    @Nonnull
    public BillingRequests getRequests() {
        return mRequests;
    }

    /**
     * A factory method of {@link BillingRequests}. The constructed object is marked with the given
     * <var>tag</var>. All methods of {@link RequestListener} used in this {@link BillingRequests}
     * are called on the main application thread.
     *
     * @param tag requests marker
     * @return requests for the given <var>tag</var>
     */
    @Nonnull
    public Requests getRequests(@Nullable Object tag) {
        if (tag == null) {
            return (Requests) getRequests();
        }
        return (Requests) new RequestsBuilder().withTag(tag).onMainThread().create();
    }

    @Nonnull
    PurchaseFlow createPurchaseFlow(@Nonnull IntentStarter intentStarter, int requestCode, @Nonnull RequestListener<Purchase> listener) {
        if (mCache.hasCache()) {
            listener = new RequestListenerWrapper<Purchase>(listener) {
                @Override
                public void onSuccess(@Nonnull Purchase result) {
                    mCache.removeAll(RequestType.GET_PURCHASES.getCacheKeyType());
                    super.onSuccess(result);
                }
            };
        }
        return new PurchaseFlow(intentStarter, requestCode, listener, mConfiguration.getPurchaseVerifier());
    }

    @Nonnull
    private <R> RequestListener<R> onMainThread(@Nonnull final RequestListener<R> listener) {
        return new MainThreadRequestListener<>(mMainThread, listener);
    }

    public void onCheckoutStarted() {
        Check.isMainThread();
        synchronized (mLock) {
            mCheckoutCount++;
            if (mCheckoutCount > 0 && mConfiguration.isAutoConnect()) {
                connect();
            }
        }
    }

    void onCheckoutStopped() {
        Check.isMainThread();
        synchronized (mLock) {
            mCheckoutCount--;
            if (mCheckoutCount < 0) {
                mCheckoutCount = 0;
                warning("Billing#onCheckoutStopped is called more than Billing#onCheckoutStarted");
            }
            if (mCheckoutCount == 0 && mConfiguration.isAutoConnect()) {
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
        FAILED
    }

    interface ServiceConnector {
        boolean connect();

        void disconnect();
    }

    /**
     * An interface that represents {@link Billing}'s configuration. Each {@link Billing} object
     * gets an instance of this class when it is constructed. Once {@link Billing} is created the
     * configuration can't be changed.
     * A {@link DefaultConfiguration} can be used as a base class for common configurations.
     */
    public interface Configuration {
        /**
         * This is used for verification of purchase signatures. You can find app's base64-encoded
         * public key in application's page on Google Play Developer Console. Note that this
         * is *not* "developer public key".
         *
         * @return application's public key, encoded in base64.
         */
        @Nonnull
        String getPublicKey();

        /**
         * Though, Android's Billing API claims to support client caching Checkout library uses its
         * own cache. The main reason is to avoid too frequent inter-process communication (IPC)
         * between the app and the billing service. This feature can be disabled if a null
         * reference is returned by this method.
         *
         * @return cache instance to be used for caching, null for no caching
         * @see Billing#newCache()
         */
        @Nullable
        Cache getCache();

        /**
         * A hook to perform a custom signature verification via {@link PurchaseVerifier}
         * interface.
         * One and only one instance of {@link PurchaseVerifier} is used in {@link Billing}: this
         * method is called from the {@link Billing}'s constructor and the returned value is cached
         * and later reused.
         *
         * @return {@link PurchaseVerifier} to be used to validate purchases
         * @see PurchaseVerifier
         */
        @Nonnull
        PurchaseVerifier getPurchaseVerifier();

        /**
         * A fallback inventory is used to recover purchases that were done in the earlier Billing
         * API versions and that can't be restored automatically in Billing API v.3
         *
         * @param checkout       checkout
         * @param onLoadExecutor executor to be used to call {@link Inventory.Callback} methods
         * @return inventory to be used if Billing v.3 is not supported
         */
        @Nullable
        Inventory getFallbackInventory(@Nonnull Checkout checkout, @Nonnull Executor onLoadExecutor);

        /**
         * Internally, Checkout library connects to the Billing service and uses it to perform
         * the API requests. As often only some application activities require Billing information
         * there is no need in keeping the connection all the time. Starting and stopping
         * {@link Billing} manually in the activities that need it is one way to solve the problem.
         * Another way is to allow {@link Billing} to manage the connection itself. If
         * <code>true</code> is returned from this method {@link Billing} will count all the
         * {@link Checkout} objects created in it and will close the connection as soon as the last
         * {@link Checkout} is destroyed.
         *
         * @return true if {@link Billing} should connect to/disconnect from Billing API service
         * automatically
         */
        boolean isAutoConnect();
    }

    /**
     * Class that partially implements {@link Configuration} interface. {@link Billing} instance
     * configured with this class will get a cache from {@link #newCache()}, a purchase verifier
     * from {@link #newPurchaseVerifier(String)}, no fallback inventory and will auto-connect to
     * the billing service when needed.
     */
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
     * {@link Configuration} that caches and re-uses some fields of the original
     * {@link Configuration} passed to its constructor.
     */
    private static final class StaticConfiguration implements Configuration {
        @Nonnull
        private final Configuration mOriginal;
        @Nonnull
        private final String mPublicKey;
        @Nonnull
        private PurchaseVerifier mPurchaseVerifier;

        private StaticConfiguration(@Nonnull Configuration original) {
            mOriginal = original;
            mPublicKey = original.getPublicKey();
            mPurchaseVerifier = original.getPurchaseVerifier();
        }

        @Nonnull
        @Override
        public String getPublicKey() {
            return mPublicKey;
        }

        @Nullable
        @Override
        public Cache getCache() {
            return mOriginal.getCache();
        }

        @Nonnull
        @Override
        public PurchaseVerifier getPurchaseVerifier() {
            return mPurchaseVerifier;
        }

        void setPurchaseVerifier(@Nonnull PurchaseVerifier purchaseVerifier) {
            mPurchaseVerifier = purchaseVerifier;
        }

        @Nullable
        @Override
        public Inventory getFallbackInventory(@Nonnull Checkout checkout, @Nonnull Executor onLoadExecutor) {
            return mOriginal.getFallbackInventory(checkout, onLoadExecutor);
        }

        @Override
        public boolean isAutoConnect() {
            return mOriginal.isAutoConnect();
        }
    }

    private final class OnConnectedServiceRunnable implements RequestRunnable {
        @GuardedBy("this")
        @Nullable
        private Request mRequest;

        public OnConnectedServiceRunnable(@Nonnull Request request) {
            mRequest = request;
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
            final InAppBillingService localService;
            synchronized (mLock) {
                localState = mState;
                localService = mService;
            }
            if (localState == State.CONNECTED) {
                Check.isNotNull(localService);
                // service is connected, let's start request
                try {
                    localRequest.start(localService, mContext.getPackageName());
                } catch (RemoteException | RuntimeException | RequestException e) {
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
            if (!mCache.hasCache()) {
                return false;
            }
            final String key = request.getCacheKey();
            if (key == null) {
                return false;
            }
            final Cache.Entry entry = mCache.get(request.getType().getCacheKey(key));
            if (entry == null) {
                return false;
            }
            request.onSuccess(entry.data);
            return true;
        }

        @Override
        @Nullable
        public Request getRequest() {
            synchronized (this) {
                return mRequest;
            }
        }

        public void cancel() {
            synchronized (this) {
                if (mRequest != null) {
                    Billing.debug("Cancelling request: " + mRequest);
                    mRequest.cancel();
                }
                mRequest = null;
            }
        }

        @Override
        public int getId() {
            synchronized (this) {
                return mRequest != null ? mRequest.getId() : -1;
            }
        }

        @Nullable
        @Override
        public Object getTag() {
            synchronized (this) {
                return mRequest != null ? mRequest.getTag() : null;
            }
        }

        @Override
        public String toString() {
            return String.valueOf(mRequest);
        }
    }

    /**
     * A {@link BillingRequests} builder. Allows to specify request tags and result delivery
     * methods
     */
    public final class RequestsBuilder {
        @Nullable
        private Object mTag;
        @Nullable
        private Boolean mOnMainThread;

        private RequestsBuilder() {
        }

        /**
         * @param tag tab to be used for all requests initiated by the constructed {@link
         *            BillingRequests}
         * @return this builder
         */
        @Nonnull
        public RequestsBuilder withTag(@Nullable Object tag) {
            Check.isNull(mTag);
            mTag = tag;
            return this;
        }

        /**
         * Makes {@link RequestListener} methods to be called on a background thread.
         *
         * @return this builder
         */
        @Nonnull
        public RequestsBuilder onBackgroundThread() {
            Check.isNull(mOnMainThread);
            mOnMainThread = false;
            return this;
        }

        /**
         * Makes {@link RequestListener} methods to be called on the main application thread.
         * Default choice if neither this nor {@link #onBackgroundThread()} was called.
         *
         * @return this builder
         */
        @Nonnull
        public RequestsBuilder onMainThread() {
            Check.isNull(mOnMainThread);
            mOnMainThread = true;
            return this;
        }

        @Nonnull
        public BillingRequests create() {
            return new Requests(mTag, mOnMainThread == null ? true : mOnMainThread);
        }
    }

    final class Requests implements BillingRequests {

        @Nullable
        private final Object mTag;

        private final boolean mOnMainThread;

        private Requests(@Nullable Object tag, boolean onMainThread) {
            mTag = tag;
            mOnMainThread = onMainThread;
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
            return runWhenConnected(new BillingSupportedRequest(product, apiVersion, null), wrapListener(listener), mTag);
        }

        @Override
        public int isBillingSupported(@Nonnull String product, int apiVersion, @Nonnull Bundle extraParams, @Nonnull RequestListener<Object> listener) {
            Check.isNotEmpty(product);
            return runWhenConnected(new BillingSupportedRequest(product, apiVersion, extraParams), wrapListener(listener), mTag);
        }

        @Override
        public int isBillingSupported(@Nonnull final String product, @Nonnull RequestListener<Object> listener) {
            return isBillingSupported(product, V3, listener);
        }

        @Nonnull
        private <R> RequestListener<R> wrapListener(@Nonnull RequestListener<R> listener) {
            return mOnMainThread ? onMainThread(listener) : listener;
        }

        @Nonnull
        Executor getDeliveryExecutor() {
            return mOnMainThread ? mMainThread : SameThreadExecutor.INSTANCE;
        }

        @Override
        public int getPurchases(@Nonnull final String product, @Nullable final String continuationToken, @Nonnull RequestListener<Purchases> listener) {
            Check.isNotEmpty(product);
            return runWhenConnected(new GetPurchasesRequest(product, continuationToken, mConfiguration.getPurchaseVerifier()), wrapListener(listener), mTag);
        }

        @Override
        public int getAllPurchases(@Nonnull String product, @Nonnull RequestListener<Purchases> listener) {
            Check.isNotEmpty(product);
            final GetPurchasesRequest request = new GetPurchasesRequest(product, null, mConfiguration.getPurchaseVerifier());
            return runWhenConnected(request, wrapListener(new GetAllPurchasesListener(request, listener)), mTag);
        }

        @Override
        public int getPurchaseHistory(@Nonnull String product, @Nullable String continuationToken, @Nullable Bundle extraParams, @Nonnull RequestListener<Purchases> listener) {
            Check.isNotEmpty(product);
            return runWhenConnected(new GetPurchaseHistoryRequest(product, continuationToken, extraParams), wrapListener(listener), mTag);
        }

        @Override
        public int getWholePurchaseHistory(@Nonnull String product, @Nullable Bundle extraParams, @Nonnull RequestListener<Purchases> listener) {
            Check.isNotEmpty(product);
            final GetPurchaseHistoryRequest request = new GetPurchaseHistoryRequest(product, null, extraParams);
            return runWhenConnected(request, wrapListener(new GetWholePurchaseHistoryListener(request, listener)), mTag);
        }

        @Override
        public int isGetPurchaseHistorySupported(@Nonnull String product, @Nonnull RequestListener<Object> listener) {
            Check.isNotEmpty(product);
            return isBillingSupported(product, Billing.V6, listener);
        }

        @Override
        public int isPurchased(@Nonnull final String product, @Nonnull final String sku, @Nonnull final RequestListener<Boolean> listener) {
            Check.isNotEmpty(sku);
            final IsPurchasedListener isPurchasedListener = new IsPurchasedListener(sku, listener);
            final GetPurchasesRequest request = new GetPurchasesRequest(product, null, mConfiguration.getPurchaseVerifier());
            isPurchasedListener.mRequest = request;
            return runWhenConnected(request, wrapListener(isPurchasedListener), mTag);
        }

        @Override
        public int getSkus(@Nonnull String product, @Nonnull List<String> skus, @Nonnull RequestListener<Skus> listener) {
            Check.isNotEmpty(product);
            Check.isNotEmpty(skus);
            return runWhenConnected(new GetSkuDetailsRequest(product, skus), wrapListener(listener), mTag);
        }

        @Override
        public int purchase(@Nonnull String product, @Nonnull String sku, @Nullable String payload, @Nonnull PurchaseFlow purchaseFlow) {
            Check.isNotEmpty(product);
            Check.isNotEmpty(sku);
            return runWhenConnected(new PurchaseRequest(product, sku, payload), wrapListener(purchaseFlow), mTag);
        }

        @Override
        public int purchase(@Nonnull String product, @Nonnull String sku, @Nullable String payload, @Nullable Bundle extraParams, @Nonnull PurchaseFlow purchaseFlow) {
            Check.isNotEmpty(product);
            Check.isNotEmpty(sku);
            return runWhenConnected(new PurchaseRequest(product, sku, payload, extraParams), wrapListener(purchaseFlow), mTag);
        }

        @Override
        public int isPurchaseWithExtraParamsSupported(@Nonnull String product, @Nonnull RequestListener<Object> listener) {
            Check.isNotEmpty(product);
            return isBillingSupported(product, Billing.V6, listener);
        }

        @Override
        public int changeSubscription(@Nonnull List<String> oldSkus,
                                      @Nonnull String newSku, @Nullable String payload,
                                      @Nonnull PurchaseFlow purchaseFlow) {
            Check.isNotEmpty(oldSkus);
            Check.isNotEmpty(newSku);
            return runWhenConnected(
                    new ChangePurchaseRequest(ProductTypes.SUBSCRIPTION, oldSkus, newSku, payload),
                    wrapListener(purchaseFlow), mTag);
        }

        @Override
        public int changeSubscription(@Nonnull List<Sku> oldSkus, @Nonnull Sku newSku,
                                      @Nullable String payload, @Nonnull PurchaseFlow purchaseFlow) {
            Check.isTrue(ProductTypes.SUBSCRIPTION.equals(newSku.id.product), "Only subscriptions can be downgraded/upgraded");
            final List<String> oldSkuIds = new ArrayList<>(oldSkus.size());
            for (Sku oldSku : oldSkus) {
                Check.isTrue(oldSku.id.product.equals(newSku.id.product), "Product type can't be changed");
                oldSkuIds.add(oldSku.id.code);
            }
            return changeSubscription(oldSkuIds, newSku.id.code, payload, purchaseFlow);
        }

        @Override
        public int isChangeSubscriptionSupported(RequestListener<Object> listener) {
            return isBillingSupported(ProductTypes.SUBSCRIPTION, Billing.V5, listener);
        }

        @Override
        public int purchase(@Nonnull Sku sku, @Nullable String payload, @Nonnull PurchaseFlow purchaseFlow) {
            return purchase(sku.id.product, sku.id.code, payload, purchaseFlow);
        }

        @Override
        public int consume(@Nonnull String token, @Nonnull RequestListener<Object> listener) {
            Check.isNotEmpty(token);
            return runWhenConnected(new ConsumePurchaseRequest(token), wrapListener(listener), mTag);
        }

        @Override
        public void cancelAll() {
            mPendingRequests.cancelAll(mTag);
        }

        @Override
        public void cancel(int requestId) {
            mPendingRequests.cancel(requestId);
        }

        /**
         * This class waits for the result from {@link GetPurchasesRequest} and checks if purchases
         * contains specified <var>sku</var>. If there is a <var>continuationToken</var> and item
         * can't be found in this bulk of purchases another (recursive) request is executed (to
         * load other purchases) and the search is done again. New (additional) request has the
         * same ID and the same listener as the original request and, thus, can be cancelled with
         * the original request ID.
         */
        private final class IsPurchasedListener implements CancellableRequestListener<Purchases> {
            @Nonnull
            private final String mSku;
            @Nonnull
            private final RequestListener<Boolean> mListener;
            @Nonnull
            private GetPurchasesRequest mRequest;

            public IsPurchasedListener(@Nonnull String sku, @Nonnull RequestListener<Boolean> listener) {
                mSku = sku;
                mListener = listener;
            }

            @Override
            public void onSuccess(@Nonnull Purchases purchases) {
                final Purchase purchase = purchases.getPurchase(mSku);
                if (purchase != null) {
                    mListener.onSuccess(purchase.state == Purchase.State.PURCHASED);
                    return;
                }
                if (purchases.continuationToken == null) {
                    mListener.onSuccess(false);
                    return;
                }
                mRequest = new GetPurchasesRequest(mRequest, purchases.continuationToken);
                runWhenConnected(mRequest, mTag);
            }

            @Override
            public void onError(int response, @Nonnull Exception e) {
                mListener.onError(response, e);
            }

            @Override
            public void cancel() {
                Billing.cancel(mListener);
            }
        }

        private final class GetAllPurchasesListener extends BaseAllPurchasesListener {

            GetAllPurchasesListener(@Nonnull GetPurchasesRequest initialRequest, @Nonnull RequestListener<Purchases> listener) {
                super(initialRequest, listener);
            }

            @Nonnull
            @Override
            protected GetPurchasesRequest makeContinuationRequest(@Nonnull BasePurchasesRequest request, @Nonnull String continuationToken) {
                return new GetPurchasesRequest((GetPurchasesRequest) request, continuationToken);
            }
        }

        private final class GetWholePurchaseHistoryListener extends BaseAllPurchasesListener {
            GetWholePurchaseHistoryListener(@Nonnull GetPurchaseHistoryRequest initialRequest, @Nonnull RequestListener<Purchases> listener) {
                super(initialRequest, listener);
            }

            @Nonnull
            @Override
            protected BasePurchasesRequest makeContinuationRequest(@Nonnull BasePurchasesRequest request, @Nonnull String continuationToken) {
                return new GetPurchaseHistoryRequest((GetPurchaseHistoryRequest) request, continuationToken);
            }
        }

        private abstract class BaseAllPurchasesListener implements CancellableRequestListener<Purchases> {
            @Nonnull
            private final RequestListener<Purchases> mListener;
            @Nonnull
            private final List<Purchase> mPurchases = new ArrayList<>();
            @Nonnull
            private BasePurchasesRequest mRequest;

            BaseAllPurchasesListener(@Nonnull BasePurchasesRequest initialRequest, @Nonnull RequestListener<Purchases> listener) {
                mRequest = initialRequest;
                mListener = listener;
            }

            @Override
            public void onSuccess(@Nonnull Purchases purchases) {
                mPurchases.addAll(purchases.list);
                // we need to check continuation token
                final String continuationToken = purchases.continuationToken;
                if (continuationToken == null) {
                    mListener.onSuccess(new Purchases(purchases.product, mPurchases, null));
                    return;
                }
                mRequest = makeContinuationRequest(mRequest, continuationToken);
                runWhenConnected(mRequest, mTag);
            }

            @Nonnull
            protected abstract BasePurchasesRequest makeContinuationRequest(@Nonnull BasePurchasesRequest request, @Nonnull String continuationToken);

            @Override
            public void onError(int response, @Nonnull Exception e) {
                mListener.onError(response, e);
            }

            @Override
            public void cancel() {
                Billing.cancel(mListener);
            }
        }

    }

    private class CachingRequestListener<R> extends RequestListenerWrapper<R> {
        @Nonnull
        private final Request<R> mRequest;

        public CachingRequestListener(@Nonnull Request<R> request, @Nonnull RequestListener<R> listener) {
            super(listener);
            Check.isTrue(mCache.hasCache(), "Cache must exist");
            mRequest = request;
        }

        @Override
        public void onSuccess(@Nonnull R result) {
            final String key = mRequest.getCacheKey();
            final RequestType type = mRequest.getType();
            if (key != null) {
                final long now = currentTimeMillis();
                final Cache.Entry entry = new Cache.Entry(result, now + type.expiresIn);
                mCache.putIfNotExist(type.getCacheKey(key), entry);
            }
            switch (type) {
                case PURCHASE:
                case CHANGE_PURCHASE:
                case CONSUME_PURCHASE:
                    // these requests might affect the state of purchases => we need to invalidate caches.
                    // see Billing#onPurchaseFinished() also
                    mCache.removeAll(RequestType.GET_PURCHASES.getCacheKeyType());
                    break;
            }
            super.onSuccess(result);
        }

        @Override
        public void onError(int response, @Nonnull Exception e) {
            final RequestType type = mRequest.getType();
            // sometimes it is possible that cached data is not synchronized with data on Google Play => we can
            // clear caches if such situation occurs
            switch (type) {
                case PURCHASE:
                case CHANGE_PURCHASE:
                    if (response == ITEM_ALREADY_OWNED) {
                        mCache.removeAll(RequestType.GET_PURCHASES.getCacheKeyType());
                    }
                    break;
                case CONSUME_PURCHASE:
                    if (response == ITEM_NOT_OWNED) {
                        mCache.removeAll(RequestType.GET_PURCHASES.getCacheKeyType());
                    }
                    break;
            }
            super.onError(response, e);
        }
    }

    private final class DefaultServiceConnector implements ServiceConnector {
        @Nonnull
        private final ServiceConnection mConnection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                setService(null, false);
            }

            @Override
            public void onServiceConnected(ComponentName name,
                                           IBinder service) {
                setService(InAppBillingServiceImpl.make(service), true);
            }
        };

        @Override
        public boolean connect() {
            try {
                final Intent intent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
                intent.setPackage("com.android.vending");
                return mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            } catch (IllegalArgumentException e) {
                // some devices throw IllegalArgumentException (Service Intent must be explicit)
                // even though we set package name explicitly. Let's not crash the app and catch
                // such exceptions here, the billing on such devices will not work.
                return false;
            } catch (NullPointerException e) {
                // Meizu M3s phones might throw an NPE in Context#bindService (Attempt to read from field 'int com.android.server.am.ProcessRecord.uid' on a null object reference).
                // As in-app purchases don't work if connection to the billing service can't be
                // established let's not crash and allow users to continue using the app
                return false;
            }
        }

        @Override
        public void disconnect() {
            mContext.unbindService(mConnection);
        }
    }
}
