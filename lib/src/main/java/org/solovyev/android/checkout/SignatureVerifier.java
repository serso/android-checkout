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

import javax.annotation.Nonnull;

interface SignatureVerifier {
	/**
	 * Checks that signature of <var>data</var> with <var>publicKey</var> is the same as <var>signature</var>
	 * @param publicKey public key to be used in signature generation
	 * @param data data
	 * @param signature signature to be verified
	 * @return true in case of successful verification (signature(data, publicKey) == signature)
	 */
	boolean verify(@Nonnull String publicKey, @Nonnull String data, @Nonnull String signature);
}
