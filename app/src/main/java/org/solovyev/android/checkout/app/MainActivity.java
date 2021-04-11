package org.solovyev.android.checkout.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Shows a list of use cases covered in the app.
 */
public class MainActivity extends AppCompatActivity {

    @BindView(R.id.recycler)
    RecyclerView mRecycler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapter(new Adapter(this));
    }

    private enum UseCase {
        STATIC(StaticActivity.class, R.string.use_case_title_static, R.string.use_case_desc_static),
        BANNER(BannerActivity.class, R.string.use_case_title_banner, R.string.use_case_desc_banner),
        SKUS(SkusActivity.class, R.string.use_case_title_skus, R.string.use_case_desc_skus),
        SUBSCRIPTIONS(SubscriptionsActivity.class, R.string.use_case_title_subscriptions, R.string.use_case_desc_subscriptions),
        FRAGMENT(BuyConsumeFragmentActivity.class, R.string.use_case_title_fragment, R.string.use_case_desc_fragment),
        HISTORY(HistoryActivity.class, R.string.use_case_title_history, R.string.use_case_desc_history);

        // activity to be started on click
        final Class<? extends Activity> activity;
        @StringRes
        final int title;
        @StringRes
        final int description;

        UseCase(Class<? extends Activity> activity, @StringRes int title, @StringRes int description) {
            this.activity = activity;
            this.title = title;
            this.description = description;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final Activity mActivity;
        @BindView(R.id.use_case_title)
        TextView mTitle;
        @BindView(R.id.use_case_description)
        TextView mDescription;
        @Nullable
        private UseCase mUseCase;


        ViewHolder(Activity activity, View view) {
            super(view);
            mActivity = activity;
            ButterKnife.bind(this, view);

            view.setOnClickListener(this);
        }

        void onBind(UseCase useCase) {
            mUseCase = useCase;
            mTitle.setText(useCase.title);
            mDescription.setText(useCase.description);
        }

        @Override
        public void onClick(View v) {
            if (mUseCase == null) {
                return;
            }
            mActivity.startActivity(new Intent(mActivity, mUseCase.activity));
        }
    }

    private static class Adapter extends RecyclerView.Adapter<ViewHolder> {

        private final Activity mActivity;
        private final UseCase[] mUseCases = UseCase.values();
        private final LayoutInflater mInflater;

        private Adapter(Activity activity) {
            mActivity = activity;
            mInflater = LayoutInflater.from(activity);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = mInflater.inflate(R.layout.use_case, parent, false);
            return new ViewHolder(mActivity, view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.onBind(mUseCases[position]);
        }

        @Override
        public int getItemCount() {
            return mUseCases.length;
        }
    }
}
