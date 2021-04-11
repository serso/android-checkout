package org.solovyev.android.checkout.app;

import org.solovyev.android.checkout.ActivityCheckout;
import org.solovyev.android.checkout.Billing;
import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.EmptyRequestListener;
import org.solovyev.android.checkout.Inventory;
import org.solovyev.android.checkout.ProductTypes;
import org.solovyev.android.checkout.Purchase;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import javax.annotation.Nonnull;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Shows some text and an advertisement below it. The ad banner is removed when user purchases
 * "ad_free" product.
 */
public class BannerActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String AD_FREE = "ad_free";
    @BindView(R.id.ad)
    View mAd;
    private ActivityCheckout mCheckout;
    private boolean mAdFree = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_banner);
        ButterKnife.bind(this);
        mAd.setOnClickListener(this);

        final Billing billing = CheckoutApplication.get(this).getBilling();
        mCheckout = Checkout.forActivity(this, billing);
        mCheckout.start();
        mCheckout.loadInventory(Inventory.Request.create().loadAllPurchases(), new InventoryCallback());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mCheckout.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.banner, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final boolean show = super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_buy_ad_free).setEnabled(!mAdFree);
        return show;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_buy_ad_free:
                buyAdFree();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void buyAdFree() {
        mCheckout.startPurchaseFlow(ProductTypes.IN_APP, AD_FREE, null, new PurchaseListener());
    }

    @Override
    protected void onDestroy() {
        mCheckout.stop();
        super.onDestroy();
    }

    private void showAd() {
        mAdFree = false;
        mAd.setVisibility(View.VISIBLE);
    }

    private void hideAd() {
        mAdFree = true;
        mAd.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ad:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/serso/android-checkout")));
                break;
        }
    }

    private class PurchaseListener extends EmptyRequestListener<Purchase> {
        @Override
        public void onSuccess(@Nonnull Purchase purchase) {
            hideAd();
        }
    }

    private class InventoryCallback implements Inventory.Callback {
        @Override
        public void onLoaded(@Nonnull Inventory.Products products) {
            final Inventory.Product product = products.get(ProductTypes.IN_APP);
            if (!product.supported) {
                // billing is not supported, user can't purchase anything. Don't show ads in this
                // case
                return;
            }
            if (product.isPurchased(AD_FREE)) {
                return;
            }
            showAd();
        }
    }
}
