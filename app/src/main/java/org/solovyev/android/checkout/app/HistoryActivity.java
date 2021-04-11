package org.solovyev.android.checkout.app;

import org.solovyev.android.checkout.Billing;
import org.solovyev.android.checkout.BillingRequests;
import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.EmptyRequestListener;
import org.solovyev.android.checkout.ProductTypes;
import org.solovyev.android.checkout.Purchase;
import org.solovyev.android.checkout.Purchases;
import org.solovyev.android.checkout.RequestListener;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

import javax.annotation.Nonnull;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Shows a list of recently purchased/consumed items.
 */
public class HistoryActivity extends AppCompatActivity {

    @BindView(R.id.recycler)
    RecyclerView mRecycler;
    private Checkout mCheckout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_history);
        ButterKnife.bind(this);

        final Adapter adapter = new Adapter();
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapter(adapter);

        final Billing billing = CheckoutApplication.get(this).getBilling();
        mCheckout = Checkout.forActivity(this, billing);
        mCheckout.start();
        mCheckout.whenReady(new HistoryLoader(adapter));
    }

    @Override
    protected void onDestroy() {
        mCheckout.stop();
        super.onDestroy();
    }

    private static class HistoryLoader extends Checkout.EmptyListener implements RequestListener<Purchases> {
        private final Adapter mAdapter;

        public HistoryLoader(Adapter adapter) {
            mAdapter = adapter;
        }

        @Override
        public void onReady(@Nonnull final BillingRequests requests) {
            requests.isGetPurchaseHistorySupported(ProductTypes.IN_APP, new EmptyRequestListener<Object>() {
                @Override
                public void onSuccess(@Nonnull Object result) {
                    requests.getWholePurchaseHistory(ProductTypes.IN_APP, null, HistoryLoader.this);
                }
            });
        }

        @Override
        public void onSuccess(@Nonnull Purchases purchases) {
            mAdapter.update(purchases);
        }

        @Override
        public void onError(int response, @Nonnull Exception e) {
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.purchase_sku)
        TextView mSku;
        @BindView(R.id.purchase_time)
        TextView mTime;
        @BindView(R.id.purchase_icon)
        ImageView mIcon;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        void onBind(Purchase purchase) {
            mSku.setText(purchase.sku);
            mTime.setText(DateFormat.getDateTimeInstance().format(new Date(purchase.time)));
            mIcon.setImageDrawable(new ColorDrawable(purchase.sku.hashCode()));
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private final LayoutInflater mInflater = LayoutInflater.from(HistoryActivity.this);
        @Nullable
        private Purchases mPurchases;

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = mInflater.inflate(R.layout.purchase, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (mPurchases == null) return;
            holder.onBind(mPurchases.list.get(position));
        }

        @Override
        public int getItemCount() {
            if (mPurchases == null) return 0;
            return mPurchases.list.size();
        }

        public void update(Purchases purchases) {
            mPurchases = purchases;
            notifyDataSetChanged();
        }
    }
}
