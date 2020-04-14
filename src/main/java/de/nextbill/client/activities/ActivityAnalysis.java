/*
 * NextBill Android client application
 *
 * @author Michael Roedel
 * Copyright (c) 2020 Michael Roedel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.nextbill.client.activities;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

import de.nextbill.client.R;
import de.nextbill.client.adapters.AdapterActivityAnalysisTabs;
import de.nextbill.client.breceiver.RequestListener;
import de.nextbill.client.breceiver.RequestServiceBroadcastReceiver;
import de.nextbill.client.database.IOHelper;
import de.nextbill.client.enums.BroadcastMessageActionEnum;
import de.nextbill.client.enums.LoginStatusEnum;
import de.nextbill.client.enums.OnlineStatusEnum;
import de.nextbill.client.network.RequestManager;

public class ActivityAnalysis extends BaseActivity {

    private RequestServiceBroadcastReceiver updateRequestReceiver;

    ListView list;

    private ViewPager mViewPager;
    private AdapterActivityAnalysisTabs adapter;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLayoutInflater().inflate(R.layout.activity_analysis, frameLayout);
        this.setTitle("Auswertungen");

        initializeTabs();

        updateRequestReceiver = new RequestServiceBroadcastReceiver(new RequestListener() {

            @Override
            public void requestDone(String activityClassName) {
                refreshDiagrams();
            }

            @Override
            public void generalMessage(String activityClassName, BroadcastMessageActionEnum broadcastMessageActionEnum, String message) {
                if (activityClassName.equals(ActivityAnalysis.class.getSimpleName())){
                    if (BroadcastMessageActionEnum.REFRESH.equals(broadcastMessageActionEnum)) {
                        refreshDiagrams();
                    }
                }
            }

            @Override
            public void loginMessage(LoginStatusEnum loginStatusEnum) {

            }

            @Override
            public void onlineStatusMessage(OnlineStatusEnum onlineStatusEnum) {

            }

        });

        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.baseline_menu_white_24);
        getSupportActionBar().setHomeAsUpIndicator(drawable);
    }

    public void refreshDiagrams(){
        Fragment fragment = adapter.getFragment(0, mViewPager);
        if (fragment != null){
//            ActivityAnalysisTabTimeRange activityAnalysisTabTimeRange = (ActivityAnalysisTabTimeRange) fragment;
//            activityAnalysisTabTimeRange.refreshViews();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IOHelper.getInstance().setCtx(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(RequestManager.BC_SEND_MESSAGE);
        registerReceiver(updateRequestReceiver, filter);

        initializeTabs();

        refreshDiagrams();
//        calculateDiagram();
    }

    public void initializeTabs() {
        if (mViewPager == null) {
            mViewPager = (ViewPager) findViewById(R.id.pager);
        }
        if (tabLayout == null) {
            tabLayout = (TabLayout) findViewById(R.id.tab_layout);
            tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        }

        ArrayList<String> tabs = new ArrayList<>();
        tabs.add("Zeitraum");

        if (adapter == null) {
            adapter = new AdapterActivityAnalysisTabs(this, getSupportFragmentManager(), tabs);
        }

        if (mViewPager.getAdapter() == null){
            mViewPager.setAdapter(adapter);
            tabLayout.setupWithViewPager(mViewPager);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(updateRequestReceiver);
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

//        getMenuInflater().inflate(R.menu.invoice_details_cost_distribution, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            default:
                onBackPressed();
        }

        return true;
    }
}
