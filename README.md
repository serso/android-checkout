# Android Checkout Library

## Description

**Checkout** is a library for [Android In-App Billing (v3)](http://developer.android.com/google/play/billing/api.html).
The main goal is to reduce work which should be done by developers who want to integrate in-app purchases in
their products. The project is inspired by [Volley](https://android.googlesource.com/platform/frameworks/volley/) library and
is designed to be easy to use and flexible.

### Why do I need it?

Though In-App Billing Version 3 is much easier to use than Version 2 there are still some things you have to face while
integrating it, like:
* Concurrency, see, for example, warning in
[Querying for Items Available for Purchase](http://developer.android.com/google/play/billing/billing_integrate.html#QueryDetails)
* Security, see [Security And Design](http://developer.android.com/google/play/billing/billing_best_practices.html)

## Getting started

### Installation

There are several ways how to get this library:
1. Download sources from github and either copy them to your project or import them as a project dependency
2. For Maven users:```xml
<dependency>
    <groupId>org.solovyev.android</groupId>
    <artifactId>checkout</artifactId>
    <version>x.x.x</version>
</dependency>
```
3. For Gradle users:```groovy
compile 'org.solovyev.android:checkout:x.x.x'
```
4. Download artifacts from the [repository](https://oss.sonatype.org/content/repositories/releases/org/solovyev/android/checkout/)

Checkout requires `com.android.vending.BILLING` permission in runtime. 
If you use Checkout as a library project then nothing should be done - permission will be merged automatically during
 manifest merging. In any other cases, you need to include it into your application's manifest:
 ```xml
 <uses-permission android:name="com.android.vending.BILLING" />
 ```

### Usage

Checkout contains 3 classes which likely to be used in any app: [Billing](https://github.com/serso/android-checkout/blob/master/lib/src/main/java/org/solovyev/android/checkout/Billing.java),
[Checkout](https://github.com/serso/android-checkout/blob/master/lib/src/main/java/org/solovyev/android/checkout/Checkout.java)
and [Inventory](https://github.com/serso/android-checkout/blob/master/lib/src/main/java/org/solovyev/android/checkout/Inventory.java).

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

    @Nonnull
    public static MyApplication get() {
        return instance;
    }

    @Nonnull
    public Checkout getCheckout() {
        return checkout;
    }
}
```

## Advanced usage

### Samples

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