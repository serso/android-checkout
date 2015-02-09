# Checkout (Android In-App Billing Library)

## Description

<img src="https://github.com/serso/android-checkout/blob/master/app/misc/res/logo256x256.png" align="right" />
**Checkout** is a library for [Android In-App Billing (v3)](http://developer.android.com/google/play/billing/api.html).
The main goal is to reduce work which should be done by developers who want to integrate in-app purchases in
their products. The project is inspired by [Volley](https://android.googlesource.com/platform/frameworks/volley/) library and
is designed to be easy to use, fast and flexible.

**Current version:** 0.7.0

### Why do I need it?

Though In-App Billing Version 3 is much easier to use than Version 2 there are still some things you have to face while
integrating it, like:
* Concurrency, see, for example, warning in
[Querying for Items Available for Purchase](http://developer.android.com/google/play/billing/billing_integrate.html#QueryDetails)
* Security, see [Security And Design](http://developer.android.com/google/play/billing/billing_best_practices.html)

**Checkout** has a good test coverage and is build continuously on Travis CI: 
[![Build Status](https://travis-ci.org/serso/android-checkout.svg)](https://travis-ci.org/serso/android-checkout)

### Who else uses it?

* Say it right! ([Google Play](https://play.google.com/store/apps/details?id=org.solovyev.android.dictionary.forvo))
* Calculator++ ([Google Play](https://play.google.com/store/apps/details?id=org.solovyev.android.calculator), [sources](https://github.com/serso/android-calculatorpp))
* AcDisplay ([Google Play](http://get.acdisplay.org), [sources](http://repo.acdisplay.org))

## Getting started

### Installation

There are several ways how you can get the library:
- Download sources from github and either copy them to your project or import them as a project dependency
- For Maven users:
```xml
<dependency>
    <groupId>org.solovyev.android</groupId>
    <artifactId>checkout</artifactId>
    <version>x.x.x</version>
    <type>apklib</type>
</dependency>
```
- For Gradle users:
```groovy
compile 'org.solovyev.android:checkout:x.x.x@aar'
```
- Download artifacts from the [repository](https://oss.sonatype.org/content/repositories/releases/org/solovyev/android/checkout/)

**Checkout** requires `com.android.vending.BILLING` permission in runtime. 
If you use **Checkout** as a library project then nothing should be done - permission will be merged automatically during
the manifest merging step. In any other cases you need to include it into your application's manifest:
```xml
<uses-permission android:name="com.android.vending.BILLING" />
```

### Usage

**Checkout** contains 3 classes which likely to be used in any app: [Billing](https://github.com/serso/android-checkout/blob/master/lib/src/main/java/org/solovyev/android/checkout/Billing.java),
[Checkout](https://github.com/serso/android-checkout/blob/master/lib/src/main/java/org/solovyev/android/checkout/Checkout.java)
and [Inventory](https://github.com/serso/android-checkout/blob/master/lib/src/main/java/org/solovyev/android/checkout/Inventory.java).
The code for the project with one in-app product might look like this:
```java
public class MyApplication extends Application {
    /**
     * For better performance billing class should be used as singleton
     */
    @Nonnull
    private final Billing billing = new Billing(this, new Billing.Configuration() {
        @Nonnull
        @Override
        public String getPublicKey() {
            return "Your public key, don't forget to encrypt it somehow";
        }

        @Nullable
        @Override
        public Cache getCache() {
            return Billing.newCache();
        }
    });

    /**
     * Application wide {@link org.solovyev.android.checkout.Checkout} instance (can be used anywhere in the app).
     * This instance contains all available products in the app.
     */
    @Nonnull
    private final Checkout checkout = Checkout.forApplication(billing, Products.create().add(IN_APP, asList("product")));

    @Nonnull
    private static MyApplication instance;

    public MyApplication() {
        instance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        billing.connect();
    }

    public static MyApplication get() {
        return instance;
    }
    
    @Nonnull
    public Checkout getCheckout() {
        return checkout;
    }

    //...
}
```

```java
public class MyActivity extends Activity {
    @Nonnull
    private final ActivityCheckout checkout = Checkout.forActivity(this, MyApplication.get().getCheckout());

    @Nonnull
    private Inventory inventory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkout.start();
        // you only need this if this activity starts purchase process
        checkout.createPurchaseFlow(new PurchaseListener());
        // you only need this if this activity needs information about purchases/SKUs
        inventory = checkout.loadInventory();
        inventory.whenLoaded(new InventoryLoadedListener())
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        checkout.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        checkout.stop();
        super.onDestroy();
    }

    //...
}
``` 

## Advanced usage

### Samples

Checkout sample app on [Google Play](https://play.google.com/store/apps/details?id=org.solovyev.android.checkout.app).
Sources are available [here](https://github.com/serso/android-checkout/tree/master/app).

### Building from sources

**Checkout** is built by Gradle. The project structure and build procedure are standard for Android libraries.
An environmental variable with name ANDROID_HOME must be set before building and should point to Android SDK installation folder (f.e. /opt/android/sdk).
Please
refer to [Gradle User Guide](http://tools.android.com/tech-docs/new-build-system/user-guide) for more information about building.

Android Studio/IDEA project configuration files are also checked into the repository. Use "File->Open..." to open the project.

### Classes overview

**Checkout** contains 3 main classes: [Billing](https://github.com/serso/android-checkout/blob/master/lib/src/main/java/org/solovyev/android/checkout/Billing.java),
[Checkout](https://github.com/serso/android-checkout/blob/master/lib/src/main/java/org/solovyev/android/checkout/Checkout.java)
and [Inventory](https://github.com/serso/android-checkout/blob/master/lib/src/main/java/org/solovyev/android/checkout/Inventory.java).

**Billing** class is the main controller which:
* connects to the [Billing service](https://github.com/serso/android-checkout/blob/master/lib/src/main/aidl/com/android/vending/billing/IInAppBillingService.aidl)
* executes all incoming requests (and caches responses)
* provides access to [BillingRequests](https://github.com/serso/android-checkout/blob/master/lib/src/main/java/org/solovyev/android/checkout/BillingRequests.java)
It should be used as a singleton in order to avoid several connections to the Billing service.
Though this class might be used directly usually it's better to interact with [Checkout](https://github.com/serso/android-checkout/blob/master/lib/src/main/java/org/solovyev/android/checkout/Checkout.java).

**Checkout** provides access to [Billing](https://github.com/serso/android-checkout/blob/master/lib/src/main/java/org/solovyev/android/checkout/Billing.java) for set of products.
It checks if billing is supported and executes requests if it is ready. [ActivityCheckout](https://github.com/serso/android-checkout/blob/master/lib/src/main/java/org/solovyev/android/checkout/ActivityCheckout.java)
is a subclass which also provides access to [PurchaseFlow](https://github.com/serso/android-checkout/blob/master/lib/src/main/java/org/solovyev/android/checkout/PurchaseFlow.java).

**Inventory** contains static information abouts products, purchases and SKUs. It should be reloaded every time the purchase state is changed in order to be actual.

### Purchase verification

By default **Checkout** uses simple purchase verification algorithm (see [DefaultPurchaseVerifier](https://github.com/serso/android-checkout/blob/master/lib/src/main/java/org/solovyev/android/checkout/DefaultPurchaseVerifier.java)). As exmplained in Android [documentation](http://developer.android.com/google/play/billing/billing_best_practices.html#sign) it's better to verify purchases on remote server. **Checkout** allows you to provide your own [PurchaseVerifier](https://github.com/serso/android-checkout/blob/master/lib/src/main/java/org/solovyev/android/checkout/PurchaseVerifier.java) by implementing ```Billing.Configuration#getPurchaseVerifier```. Note that ```PurchaseVerifier#verify``` is called every time purchases information is requested from Android Billing API and might be called on both main and background threads. [BasePurchaseVerifier](https://github.com/serso/android-checkout/blob/master/lib/src/main/java/org/solovyev/android/checkout/BasePurchaseVerifier.java) deals with thread management and it is recommended to extend this class if you want to provide custom verification procedure. It is safe to do network requests from ```BasePurchaseVerifier#doVerify``` as this method is guaranteed to be called on the background thread.

### Billing Version 2

Some users might not have Google Play Services supporting Billing Version 3. As these users might have purchases it should be possible to retrieve purchase information for them. You can provide fallback functionality for such users by returning not null object from ```Billing.Configuration#getFallbackInventory``` method.
If you used [Robotmedia Android Billing Library](https://github.com/robotmedia/AndroidBillingLibrary) before fallback is already implemented, see [RobotmediaInvenotry](https://github.com/serso/android-checkout/blob/master/lib/src/main/java/org/solovyev/android/checkout/RobotmediaInventory.java) class ([sample code](https://github.com/serso/android-checkout/blob/master/app/src/main/java/org/solovyev/android/checkout/app/CheckoutApplication.java))

### Proguard

You need to include the contents of [proguard rules](https://github.com/serso/android-checkout/blob/master/lib/proguard-rules.txt)
into your proguard configuration.

## License

Copyright 2014 serso aka se.solovyev

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
