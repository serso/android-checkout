package org.solovyev.android.checkout.app;

import org.solovyev.android.checkout.Billing;
import org.solovyev.android.checkout.BillingRequests;
import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.EmptyRequestListener;
import org.solovyev.android.checkout.IntentStarter;
import org.solovyev.android.checkout.Inventory;
import org.solovyev.android.checkout.ProductTypes;
import org.solovyev.android.checkout.Purchase;
import org.solovyev.android.checkout.UiCheckout;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import javax.annotation.Nonnull;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BuyConsumeFragmentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_fragment);

        if (savedState == null) {
            final FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction()
                    .add(R.id.fragment, new MyFragment())
                    .commit();
        }
    }

    public static final class MyFragment extends Fragment implements View.OnClickListener {

        private static final String AD_FREE = "ad_free";
        @BindView(R.id.item)
        TextView mItem;
        @BindView(R.id.buy_consume)
        Button mBuyConsume;
        private UiCheckout mCheckout;
        @Nullable
        private Purchase mPurchase;

        public MyFragment() {
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            final Activity activity = (Activity) context;
            final Billing billing = CheckoutApplication.get(activity).getBilling();
            mCheckout = Checkout.forUi(new MyIntentStarter(this), this, billing);
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mCheckout.start();
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedState) {
            return inflater.inflate(R.layout.fragment_buy_consume, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            ButterKnife.bind(this, view);
            mItem.setText(AD_FREE);
            mBuyConsume.setOnClickListener(this);
            mCheckout.loadInventory(Inventory.Request.create().loadAllPurchases(), new InventoryCallback());
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            mCheckout.onActivityResult(requestCode, resultCode, data);
            super.onActivityResult(requestCode, resultCode, data);
        }

        @Override
        public void onDestroy() {
            mCheckout.stop();
            super.onDestroy();
        }

        @Override
        public void onClick(View v) {
            final Purchase purchase = mPurchase;
            if (purchase == null) {
                mCheckout.startPurchaseFlow(ProductTypes.IN_APP, AD_FREE, null, new PurchaseListener());
            } else {
                mCheckout.whenReady(new Checkout.EmptyListener() {
                    @Override
                    public void onReady(@Nonnull BillingRequests requests) {
                        requests.consume(purchase.token, new ConsumeListener());
                    }
                });
            }
        }

        private void onPurchaseChanged() {
            mBuyConsume.setText(mPurchase != null ? R.string.consume : R.string.buy);
        }

        private class ConsumeListener extends EmptyRequestListener<Object> {
            @Override
            public void onSuccess(@Nonnull Object result) {
                mPurchase = null;
                onPurchaseChanged();
            }
        }

        private class PurchaseListener extends EmptyRequestListener<Purchase> {
            @Override
            public void onSuccess(@Nonnull Purchase purchase) {
                mPurchase = purchase;
                onPurchaseChanged();
            }
        }

        private class InventoryCallback implements Inventory.Callback {
            @Override
            public void onLoaded(@Nonnull Inventory.Products products) {
                final Inventory.Product product = products.get(ProductTypes.IN_APP);
                if (!product.supported) {
                    return;
                }
                mPurchase = product.getPurchaseInState(AD_FREE, Purchase.State.PURCHASED);
                onPurchaseChanged();
                mBuyConsume.setEnabled(true);
            }
        }
    }

    /**
     * Trivial implementation of {@link IntentStarter} for the support lib's {@link Fragment}.
     */
    private static class MyIntentStarter implements IntentStarter {
        @Nonnull
        private final Fragment mFragment;

        public MyIntentStarter(@Nonnull Fragment fragment) {
            mFragment = fragment;
        }

        @Override
        public void startForResult(@Nonnull IntentSender intentSender, int requestCode, @Nonnull Intent intent) throws IntentSender.SendIntentException {
            mFragment.startIntentSenderForResult(intentSender, requestCode, intent, 0, 0, 0, null);
        }
    }
}
