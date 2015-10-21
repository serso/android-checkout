package org.solovyev.android.checkout;

import org.junit.runners.model.InitializationError;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.res.Fs;

public class CheckoutTestRunner extends RobolectricTestRunner {
	private static final int MAX_SDK_SUPPORTED_BY_ROBOLECTRIC = 18;

	public CheckoutTestRunner(Class<?> testClass) throws InitializationError {
		super(testClass);
	}

	@Override
	protected AndroidManifest getAppManifest(Config config) {
		final String manifestFilePath = "../app/src/main/AndroidManifest.xml";
		final String resourcesFilePath = "../app/src/main/res";
		final String assetsFilePath = "../app/src/main/assets";
		return new AndroidManifest(Fs.fileFromPath(manifestFilePath), Fs.fileFromPath(resourcesFilePath), Fs.fileFromPath(assetsFilePath)) {
			@Override
			public int getTargetSdkVersion() {
				return MAX_SDK_SUPPORTED_BY_ROBOLECTRIC;
			}
		};
	}
}
