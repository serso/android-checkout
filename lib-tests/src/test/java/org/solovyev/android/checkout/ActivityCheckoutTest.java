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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import javax.annotation.Nonnull;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.solovyev.android.checkout.PurchaseFlowTest.newOkIntent;
import static org.solovyev.android.checkout.ResponseCodes.NULL_INTENT;

@RunWith(RobolectricTestRunner.class)
public class ActivityCheckoutTest {

	@Nonnull
	private Billing billing;

	@Nonnull
	private ActivityCheckout checkout;

	@Before
	public void setUp() throws Exception {
		final Products products = Products.create().
				add("product", asList("sku1", "sku2", "sku3")).
				add("subscription", asList("sku1", "sku2", "sku3"));
		billing = Tests.newBilling();
		checkout = Checkout.forActivity(new Activity(), billing, products);
	}

	@Test
	public void testShouldCreatePurchaseFlow() throws Exception {
		checkout.createPurchaseFlow(100, mock(RequestListener.class));
		assertNotNull(checkout.getPurchaseFlow(100));
	}

	@Test
	public void testShouldCreateDefaultPurchaseFlow() throws Exception {
		checkout.createPurchaseFlow(mock(RequestListener.class));
		assertNotNull(checkout.getPurchaseFlow());
	}

	@Test
	public void testShouldCreateOneShotPurchaseFlow() throws Exception {
		checkout.createOneShotPurchaseFlow(101, mock(RequestListener.class));
		assertNotNull(checkout.getPurchaseFlow(101));
	}

	@Test
	public void testShouldCreateDefaultOneShotPurchaseFlow() throws Exception {
		checkout.createPurchaseFlow(mock(RequestListener.class));
		assertNotNull(checkout.getPurchaseFlow());
	}

	@Test
	public void testDestroyShouldRemovePurchaseFlow() throws Exception {
		checkout.createPurchaseFlow(102, mock(RequestListener.class));
		checkout.destroyPurchaseFlow(102);
		verifyPurchaseFlowDoesntExist(102);
	}

	private void verifyPurchaseFlowDoesntExist(int requestCode) {
		try {
			checkout.getPurchaseFlow(requestCode);
			fail();
		} catch (IllegalArgumentException e) {
			// ok
		}
	}

	private void verifyPurchaseFlowDoesntExist() {
		try {
			checkout.getPurchaseFlow();
			fail();
		} catch (IllegalArgumentException e) {
			// ok
		}
	}

	@Test
	public void testDestroyShouldRemoveDefaultPurchaseFlow() throws Exception {
		checkout.createPurchaseFlow(mock(RequestListener.class));
		checkout.destroyPurchaseFlow();
		verifyPurchaseFlowDoesntExist();
	}

	@Test
	public void testDestroyShouldCancelPurchaseFlow() throws Exception {
		final CancellableRequestListener l = mock(CancellableRequestListener.class);
		checkout.createPurchaseFlow(l);
		checkout.destroyPurchaseFlow();

		verify(l).cancel();
	}

	@Test
	public void testOneShowPurchaseFlowShouldBeRemovedOnError() throws Exception {
		RequestListener l = mock(RequestListener.class);
		checkout.createOneShotPurchaseFlow(l);

		checkout.onActivityResult(ActivityCheckout.DEFAULT_REQUEST_CODE, Activity.RESULT_CANCELED, null);

		verify(l).onError(eq(NULL_INTENT), any(Exception.class));
		verifyPurchaseFlowDoesntExist();
	}

	@Test
	public void testOneShowPurchaseFlowShouldBeRemovedOnSuccess() throws Exception {
		final SignatureVerifier verifier = mock(SignatureVerifier.class);
		when(verifier.verify(anyString(), anyString(), anyString())).thenReturn(true);
		billing.setSignatureVerifier(verifier);

		final RequestListener l = mock(RequestListener.class);
		checkout.createOneShotPurchaseFlow(l);

		checkout.onActivityResult(ActivityCheckout.DEFAULT_REQUEST_CODE, Activity.RESULT_OK, newOkIntent());

		verify(l).onSuccess(anyObject());
		verifyPurchaseFlowDoesntExist();

	}
}