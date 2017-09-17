package org.solovyev.android.checkout.app;

import org.solovyev.android.checkout.ActivityCheckout;
import org.solovyev.android.checkout.Billing;
import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.EmptyRequestListener;
import org.solovyev.android.checkout.Inventory;
import org.solovyev.android.checkout.ProductTypes;
import org.solovyev.android.checkout.Purchase;
import org.solovyev.android.checkout.Sku;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VirtualRealityActivity extends AppCompatActivity implements View.OnClickListener {

    @BindView(R.id.buy)
    View mBuy;
    @BindView(R.id.skus)
    Spinner mSkus;
    private ActivityCheckout mCheckout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_vr);
        ButterKnife.bind(this);
        mBuy.setOnClickListener(this);

        final Billing billing = CheckoutApplication.get(this).getBilling();
        mCheckout = Checkout.forActivity(this, billing);
        mCheckout.start();
        mCheckout.loadInventory(Inventory.Request.create().loadAllPurchases().loadSkus(ProductTypes.IN_APP, SkusActivity.getInAppSkus()), new MyInventoryCallback());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mCheckout.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        mCheckout.stop();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() != R.id.buy) return;

        final Sku sku = (Sku) mSkus.getAdapter().getItem(mSkus.getSelectedItemPosition());
        if (sku == null) return;
        purchase(sku);
    }

    private void purchase(Sku sku) {
        final Bundle extraParams = new Bundle();
        extraParams.putBoolean("vr", true);
        mCheckout.startPurchaseFlow(sku.id.product, sku.id.code, null, extraParams, new EmptyRequestListener<Purchase>());
    }

    private class MyInventoryCallback implements Inventory.Callback {
        @Override
        public void onLoaded(@Nonnull Inventory.Products products) {
            final Inventory.Product product = products.get(ProductTypes.IN_APP);
            if (!product.supported) return;
            final List<Sku> skus = new ArrayList<>();
            for (Sku sku : product.getSkus()) {
                if (product.isPurchased(sku)) continue;
                skus.add(sku);
            }
            mSkus.setAdapter(new ArrayAdapter<>(VirtualRealityActivity.this, R.layout.support_simple_spinner_dropdown_item, skus.toArray(new Sku[skus.size()])));
            mBuy.setEnabled(true);
        }
    }
}
