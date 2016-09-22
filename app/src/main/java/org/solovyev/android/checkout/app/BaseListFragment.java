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

package org.solovyev.android.checkout.app;

import org.solovyev.android.checkout.ActivityCheckout;
import org.solovyev.android.checkout.Inventory;
import org.solovyev.android.checkout.SkuIds;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.annotation.Nonnull;

import static android.view.animation.AnimationUtils.loadAnimation;

public class BaseListFragment extends Fragment {

    @Nonnull
    protected ActivityCheckout checkout;
    @Nonnull
    protected ListView listView;
    @Nonnull
    protected ProgressBar progressBar;
    @Nonnull
    protected TextView emptyView;
    @Nonnull
    protected TextView titleView;
    @Nonnull
    protected Inventory inventory;
    private boolean listShown;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        checkout = ((BaseActivity) activity).getCheckout();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inventory = checkout.loadInventory(SkuIds.create());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_list, container, false);
        titleView = (TextView) view.findViewById(R.id.list_title);
        listView = (ListView) view.findViewById(android.R.id.list);
        progressBar = (ProgressBar) view.findViewById(android.R.id.progress);
        emptyView = (TextView) view.findViewById(android.R.id.empty);
        return view;
    }

    public void setListShown(boolean shown, boolean animate) {
        if (listShown == shown) {
            return;
        }
        listShown = shown;

        if (shown) {
            final View view = listView.getCount() > 0 ? listView : emptyView;
            if (animate) {
                progressBar.startAnimation(loadAnimation(getActivity(), android.R.anim.fade_out));
                view.startAnimation(loadAnimation(getActivity(), android.R.anim.fade_in));
            }
            progressBar.setVisibility(View.GONE);
            view.setVisibility(View.VISIBLE);
        } else {
            final View view = listView.getVisibility() == View.VISIBLE ? listView : emptyView;
            if (animate) {
                progressBar.startAnimation(loadAnimation(getActivity(), android.R.anim.fade_in));
                view.startAnimation(loadAnimation(getActivity(), android.R.anim.fade_out));
            }
            progressBar.setVisibility(View.VISIBLE);
            view.setVisibility(View.INVISIBLE);
        }
    }

    public void setListShown(boolean shown) {
        setListShown(shown, true);
    }

    public void setListShownNoAnimation(boolean shown) {
        setListShown(shown, false);
    }
}
