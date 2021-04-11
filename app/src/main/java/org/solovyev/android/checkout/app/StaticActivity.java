package org.solovyev.android.checkout.app;

import org.solovyev.android.checkout.ActivityCheckout;
import org.solovyev.android.checkout.Billing;
import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.Logger;
import org.solovyev.android.checkout.ProductTypes;
import org.solovyev.android.checkout.Purchase;
import org.solovyev.android.checkout.RequestListener;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import javax.annotation.Nonnull;

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
    @BindView(R.id.console)
    TextView mConsole;
    private ActivityCheckout mCheckout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_static);
        ButterKnife.bind(this);

        mBuy.setOnClickListener(this);
        mSkus.setAdapter(new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, new String[]{"android.test.purchased", "android.test.canceled", "android.test.refunded", "android.test.item_unavailable"}));
        mSkus.setSelection(0);
        Billing.setLogger(Billing.newMainThreadLogger(new BillingLogger(mConsole)));

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
        Billing.setLogger(Billing.newLogger());
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buy:
                // prevent starting several purchase flows simultaneously
                mBuy.setEnabled(false);
                final String sku = (String) mSkus.getSelectedItem();
                mConsole.setText("");
                mCheckout.startPurchaseFlow(ProductTypes.IN_APP, sku, null, new RequestListener<Purchase>() {
                    @Override
                    public void onSuccess(@Nonnull Purchase result) {
                        mBuy.setEnabled(true);
                    }

                    @Override
                    public void onError(int response, @Nonnull Exception e) {
                        mBuy.setEnabled(true);
                    }
                });
                break;
        }
    }

    private static class BillingLogger implements Logger {
        private final TextView mView;

        public BillingLogger(TextView view) {
            mView = view;
        }

        @Override
        public void v(@Nonnull String tag, @Nonnull String msg) {
            log("v", tag, msg, null);
        }

        private void log(String level, @Nonnull String tag, @Nonnull String msg, @Nullable Throwable e) {
            if (!tag.equals("Checkout")) {
                return;
            }
            final boolean empty = mView.getText().length() == 0;
            if(!empty) {
                mView.append("\n");
            }
            mView.append(level + ": " + msg);
            if (e == null) {
                return;
            }
            final String message = e.getMessage();
            mView.append("\n");
            mView.append(!TextUtils.isEmpty(message) ? message : e.getClass().getSimpleName());
        }

        @Override
        public void v(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
            log("v", tag, msg, e);
        }

        @Override
        public void d(@Nonnull String tag, @Nonnull String msg) {
            log("d", tag, msg, null);
        }

        @Override
        public void d(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
            log("d", tag, msg, e);
        }

        @Override
        public void i(@Nonnull String tag, @Nonnull String msg) {
            log("i", tag, msg, null);
        }

        @Override
        public void i(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
            log("i", tag, msg, e);
        }

        @Override
        public void w(@Nonnull String tag, @Nonnull String msg) {
            log("w", tag, msg, null);
        }

        @Override
        public void w(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
            log("w", tag, msg, e);
        }

        @Override
        public void e(@Nonnull String tag, @Nonnull String msg) {
            log("e", tag, msg, null);
        }

        @Override
        public void e(@Nonnull String tag, @Nonnull String msg, @Nonnull Throwable e) {
            log("e", tag, msg, e);
        }
    }
}
