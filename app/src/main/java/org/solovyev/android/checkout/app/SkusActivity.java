package org.solovyev.android.checkout.app;

import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.solovyev.android.checkout.ActivityCheckout;
import org.solovyev.android.checkout.Billing;
import org.solovyev.android.checkout.BillingRequests;
import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.Inventory;
import org.solovyev.android.checkout.ProductTypes;
import org.solovyev.android.checkout.Purchase;
import org.solovyev.android.checkout.RequestListener;
import org.solovyev.android.checkout.ResponseCodes;
import org.solovyev.android.checkout.Sku;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Shows a list of SKUs available for purchase. User can purchase/consume an item by selecting it
 * from the list. Purchased items are strikethroughed.
 */
public class SkusActivity extends AppCompatActivity {

    @BindView(R.id.recycler)
    RecyclerView mRecycler;
    private ActivityCheckout mCheckout;
    private InventoryCallback mInventoryCallback;

    private static List<String> getInAppSkus() {
        final List<String> skus = new ArrayList<>(Arrays.asList("coffee", "beer", "cake", "hamburger"));
        for (int i = 0; i < 20; i++) {
            final int id = i + 1;
            final String sku = id < 10 ? "item_0" + id : "item_" + id;
            skus.add(sku);
        }
        return skus;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_skus);
        ButterKnife.bind(this);

        final Adapter adapter = new Adapter();
        mInventoryCallback = new InventoryCallback(adapter);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapter(adapter);

        final Billing billing = CheckoutApplication.get(this).getBilling();
        mCheckout = Checkout.forActivity(this, billing);
        mCheckout.start();
        reloadInventory();
    }

    private void reloadInventory() {
        final Inventory.Request request = Inventory.Request.create();
        // load purchase info
        request.loadAllPurchases();
        // load SKU details
        request.loadSkus(ProductTypes.IN_APP, getInAppSkus());
        mCheckout.loadInventory(request, mInventoryCallback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mCheckout.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Purchase cancelled", Toast.LENGTH_LONG).show();
        } else if (resultCode == RESULT_OK) {
            Toast.makeText(this, "Purchase successful", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        mCheckout.stop();
        super.onDestroy();
    }

    private void purchase(Sku sku) {
        final RequestListener<Purchase> listener = makeRequestListener();
        mCheckout.startPurchaseFlow(sku, null, listener);
    }

    /**
     * @return {@link RequestListener} that reloads inventory when the action is finished
     */
    private <T> RequestListener<T> makeRequestListener() {
        return new RequestListener<T>() {
            @Override
            public void onSuccess(@Nonnull T result) {
                reloadInventory();
            }

            @Override
            public void onError(int response, @Nonnull Exception e) {
                if (BuildConfig.DEBUG)
                    Log.e("SKUActivity ", "Couldn't complete the purchase", e);
                switch (response) {
                    case ResponseCodes.ITEM_ALREADY_OWNED:
                    case ResponseCodes.USER_CANCELED:
                    case ResponseCodes.WRONG_SIGNATURE:
                    case ResponseCodes.SERVICE_NOT_CONNECTED:
                    case ResponseCodes.OK:
                    case ResponseCodes.NULL_INTENT:
                    case ResponseCodes.ITEM_UNAVAILABLE:
                    case ResponseCodes.EXCEPTION:
                    case ResponseCodes.ITEM_NOT_OWNED:
                    case ResponseCodes.ERROR:
                    case ResponseCodes.DEVELOPER_ERROR:
                    case ResponseCodes.BILLING_UNAVAILABLE:
                    case ResponseCodes.ACCOUNT_ERROR:
                        // Handle the errors the way you like
                        break;
                    default:
                        throw new RuntimeException("unhandled response code received");
                }
                reloadInventory();
            }
        };
    }

    private void consume(final Purchase purchase) {
        mCheckout.whenReady(new Checkout.EmptyListener() {
            @Override
            public void onReady(@Nonnull BillingRequests requests) {
                requests.consume(purchase.token, makeRequestListener());
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final Adapter mAdapter;
        @BindView(R.id.sku_title)
        TextView mTitle;
        @BindView(R.id.sku_description)
        TextView mDescription;
        @BindView(R.id.sku_price)
        TextView mPrice;
        @BindView(R.id.sku_icon)
        ImageView mIcon;

        @Nullable
        private Sku mSku;

        ViewHolder(View view, Adapter adapter) {
            super(view);
            mAdapter = adapter;
            ButterKnife.bind(this, view);

            view.setOnClickListener(this);
        }

        private static void strikeThrough(TextView view, boolean strikeThrough) {
            int flags = view.getPaintFlags();
            if (strikeThrough) {
                flags |= Paint.STRIKE_THRU_TEXT_FLAG;
            } else {
                flags &= ~Paint.STRIKE_THRU_TEXT_FLAG;
            }
            view.setPaintFlags(flags);
        }

        void onBind(Sku sku, boolean purchased) {
            mSku = sku;
            mTitle.setText(getTitle(sku));
            mDescription.setText(sku.description);
            strikeThrough(mTitle, purchased);
            strikeThrough(mDescription, purchased);
            mPrice.setText(sku.price);
            mIcon.setImageDrawable(new ColorDrawable(sku.title.hashCode() + sku.description.hashCode()));
        }

        /**
         * @return SKU title without application name that is automatically added by Play Services
         */
        private String getTitle(Sku sku) {
            final int i = sku.title.indexOf("(");
            if (i > 0) {
                return sku.title.substring(0, i);
            }
            return sku.title;
        }

        @Override
        public void onClick(View v) {
            if (mSku == null) {
                return;
            }
            mAdapter.onClick(mSku);
        }
    }

    /**
     * Updates {@link Adapter} when {@link Inventory.Products} are loaded.
     */
    private static class InventoryCallback implements Inventory.Callback {
        private final Adapter mAdapter;

        public InventoryCallback(Adapter adapter) {
            mAdapter = adapter;
        }

        @Override
        public void onLoaded(@Nonnull Inventory.Products products) {
            final Inventory.Product product = products.get(ProductTypes.IN_APP);
            if (!product.supported) {
                // billing is not supported, user can't purchase anything
                return;
            }
            mAdapter.update(product);
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private final LayoutInflater mInflater = LayoutInflater.from(SkusActivity.this);
        private Inventory.Product mProduct = Inventory.Products.empty().get(ProductTypes.IN_APP);

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = mInflater.inflate(R.layout.sku, parent, false);
            return new ViewHolder(view, this);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final Sku sku = mProduct.getSkus().get(position);
            holder.onBind(sku, mProduct.isPurchased(sku));
        }

        @Override
        public int getItemCount() {
            return mProduct.getSkus().size();
        }

        public void update(Inventory.Product product) {
            mProduct = product;
            notifyDataSetChanged();
        }

        public void onClick(Sku sku) {
            final Purchase purchase = mProduct.getPurchaseInState(sku, Purchase.State.PURCHASED);
            if (purchase != null) {
                consume(purchase);
            } else {
                purchase(sku);
            }
        }
    }
}
