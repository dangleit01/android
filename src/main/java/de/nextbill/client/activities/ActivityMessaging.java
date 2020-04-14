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
import android.view.View;
import android.widget.ProgressBar;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

import de.nextbill.client.R;
import de.nextbill.client.adapters.AdapterActivityMessagingTabs;
import de.nextbill.client.breceiver.RequestListener;
import de.nextbill.client.breceiver.RequestServiceBroadcastReceiver;
import de.nextbill.client.database.IOHelper;
import de.nextbill.client.enums.BroadcastMessageActionEnum;
import de.nextbill.client.enums.LoginStatusEnum;
import de.nextbill.client.enums.OnlineStatusEnum;
import de.nextbill.client.interfaces.IFragmentToActivity;
import de.nextbill.client.network.RequestManager;

public class ActivityMessaging extends BaseActivity implements IFragmentToActivity {

    private RequestServiceBroadcastReceiver updateRequestReceiver;

    private ViewPager mViewPager;
    private AdapterActivityMessagingTabs adapter;
    private TabLayout tabLayout;
    private ProgressBar internetActivityBar;

    private Boolean openTabHasEntries;
    private Boolean closeTabHasEntries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLayoutInflater().inflate(R.layout.activity_main, frameLayout);
        this.setTitle("Benachrichtigungen");

        initializeTabs();

        internetActivityBar = (ProgressBar) findViewById(R.id.internetActivityBar);

        updateRequestReceiver = new RequestServiceBroadcastReceiver(new RequestListener() {

            @Override
            public void requestDone(String activityClassName) {
                swipeRefreshStatus(false);
                showInternetActivityBarStatus(false);
                refreshMessagesLists();
            }

            @Override
            public void generalMessage(String activityClassName, BroadcastMessageActionEnum broadcastMessageActionEnum, String message) {
                if (activityClassName.equals(ActivityMessaging.class.getSimpleName())){
                    if (broadcastMessageActionEnum != null && BroadcastMessageActionEnum.REFRESH.equals(broadcastMessageActionEnum)) {
                        refreshMessagesLists();
                    }
                }
            }

            @Override
            public void loginMessage(LoginStatusEnum loginStatusEnum) {
            }

            @Override
            public void onlineStatusMessage(OnlineStatusEnum onlineStatusEnum) {
                swipeRefreshStatus(false);
                showInternetActivityBarStatus(false);
                refreshMessagesLists();
            }
        });

        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.baseline_menu_white_24);
        getSupportActionBar().setHomeAsUpIndicator(drawable);
    }

    public void refreshMessagesLists(){
        Fragment fragment = adapter.getFragment(0, mViewPager);
        if (fragment != null){
            ActivityMessagingTabBilling activityMessagingTabBilling = (ActivityMessagingTabBilling) fragment;
            activityMessagingTabBilling.refreshMessagesList();
        }
    }



    public void swipeRefreshStatus(Boolean active){
        Fragment fragment = adapter.getFragment(0, mViewPager);
        if (fragment != null){
            ((ActivityMessagingTabBilling) fragment).swipeRefreshStatus(active);
        }
    }

    public void showInternetActivityBarStatus(Boolean active){
        if (active){
            internetActivityBar.setVisibility(View.VISIBLE);
        }else{
            internetActivityBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IOHelper.getInstance().setCtx(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(RequestManager.BC_REQUEST_DONE);
        filter.addAction(RequestManager.BC_ONLINE);
        filter.addAction(RequestManager.BC_SEND_MESSAGE);
        registerReceiver(updateRequestReceiver, filter);
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
        tabs.add("Allgemein");
        tabs.add("Konflikte");

        if (adapter == null) {
            adapter = new AdapterActivityMessagingTabs(this, getSupportFragmentManager(), tabs);
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

//        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void startFilterActivity(){
        Intent intent = new Intent(this, ActivitySearchConfiguration.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void refreshTabCounter(Integer number1, Integer number2) {
        Fragment fragment = adapter.getFragment(0, mViewPager);
        if (fragment != null && number1 != null){
            tabLayout.getTabAt(0).setText("Allgemein (" + number1 + ")");
        }

        fragment = adapter.getFragment(1, mViewPager);
        if (fragment != null && number2 != null){
            tabLayout.getTabAt(1).setText("Konflikte (" + number2 + ")");
        }
    }

    @Override
    public void whichTabToShow(Boolean tab1, Boolean tab2, boolean autoTabShow){
        if (tab1 != null){
            this.openTabHasEntries = tab1;
        }
        if (tab2 != null){
            this.closeTabHasEntries = tab2;
        }

        if (openTabHasEntries != null && closeTabHasEntries != null){
            if (openTabHasEntries == false && closeTabHasEntries == false){
                Intent intent = new Intent(ActivityMessaging.this, ActivityMain.class);
                startActivity(intent);
                return;
            }

            if (openTabHasEntries){
                mViewPager.setCurrentItem(0);
            }else{
                mViewPager.setCurrentItem(1);
            }
        }
    }

    @Override
    public void refreshLists(boolean autoTabShow) {

    }

    @Override
    public void showInternetActivityBar(Boolean active) {
        showInternetActivityBarStatus(active);
    }

    @Override
    public void filterText(boolean show, String text) {

    }
}
