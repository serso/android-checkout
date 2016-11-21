package org.solovyev.android.checkout.app;

import org.solovyev.android.checkout.ActivityCheckout;
import org.solovyev.android.checkout.Billing;
import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.EmptyRequestListener;
import org.solovyev.android.checkout.ProductTypes;
import org.solovyev.android.checkout.Purchase;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Shows a list of the static responses. Each response can be triggered by pressing Buy button.
 */
public class StaticActivity extends AppCompatActivity implements View.OnClickListener {

    @BindView(R.id.buy)
    View mBuy;
    @BindView(R.id.skus)
    Spinner mSkus;
    private ActivityCheckout mCheckout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_static);
        ButterKnife.bind(this);

        mBuy.setOnClickListener(this);
        mSkus.setAdapter(new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, new String[]{"android.test.purchased", "android.test.canceled", "android.test.refunded", "android.test.item_unavailable"}));
        mSkus.setSelection(0);

        final Billing billing = CheckoutApplication.get(this).getBilling();
        mCheckout = Checkout.forActivity(this, billing);
        mCheckout.start();
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
        switch (v.getId()) {
            case R.id.buy:
                final String sku = (String) mSkus.getSelectedItem();
                mCheckout.startPurchaseFlow(ProductTypes.IN_APP, sku, null, new EmptyRequestListener<Purchase>());
                break;
        }
    }
}
