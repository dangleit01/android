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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.legacy.app.ActionBarDrawerToggle;

import com.google.android.material.navigation.NavigationView;

import java.math.BigDecimal;
import java.util.List;

import de.nextbill.client.R;
import de.nextbill.client.breceiver.AlarmManagerBroadcastReceiver;
import de.nextbill.client.breceiver.RequestListener;
import de.nextbill.client.breceiver.RequestServiceBroadcastReceiver;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.enums.BasicDataSubType;
import de.nextbill.client.enums.BasicDataType;
import de.nextbill.client.enums.BroadcastMessageActionEnum;
import de.nextbill.client.enums.LoginStatusEnum;
import de.nextbill.client.enums.OnlineStatusEnum;
import de.nextbill.client.enums.UpdateModusType;
import de.nextbill.client.helper.LoginUserHelper;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.BasicData;
import de.nextbill.client.network.RequestManager;
import de.nextbill.client.network.RequestUpdateService;
import de.nextbill.client.utils.SqlBuilder;

public class BaseActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    protected FrameLayout frameLayout;
//    protected ListView mDrawerList;
//    protected RelativeLayout main_drawer_layout;
//    protected Button logOutBt;
    protected TextView loginUsernameTv;
    protected TextView moneyTillMonthEndTv;

    private static final String BACKGROUND_SYNC = "background_sync";
    public static final String SERVER_URL = "server_url";
    public static final String LAST_LOGGED_IN_USER = "user_name";
    public static final String MAIL_SENT_ACTIVE = "mail_sent_activated";

    protected String[] listArray = {"Rechnungen", "Daueraufträge", "Benachrichtigungen", "Abrechnungen", "Budgets", "Auswertungen"};

    public static int position;

    private static boolean isLaunch = true;

    private CompoundButton onlineSwitchView;
    private CompoundButton syncSwitchView;

    private MenuItem billingsMenuItem;
    private MenuItem budgetsMenuItem;

    private NavigationView nav_view;

    private DrawerLayout mDrawerLayout;

    private ActionBarDrawerToggle actionBarDrawerToggle;
    private RequestServiceBroadcastReceiver updateRequestReceiver;

    static int LOGIN = 3;
    static int ACTIVITY_SEARCH = 4;

    private TextView textCartItemCount;
    int mCartItemCount = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_drawer_base_layout);

        frameLayout = (FrameLayout)findViewById(R.id.content_frame);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
//        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        nav_view = (NavigationView) findViewById(R.id.nav_view);
        nav_view.setNavigationItemSelectedListener(this);

        View headerView = nav_view.getHeaderView(0);

        MenuItem switchItem = nav_view.getMenu().findItem(R.id.onlineMenuItem);
        MenuItem syncMenuItem = nav_view.getMenu().findItem(R.id.syncMenuItem);
        billingsMenuItem = nav_view.getMenu().findItem(R.id.billingsMenuItem);
        budgetsMenuItem = nav_view.getMenu().findItem(R.id.budgetsMenuItem);

        onlineSwitchView = (CompoundButton) MenuItemCompat.getActionView(switchItem);
        syncSwitchView = (CompoundButton) MenuItemCompat.getActionView(syncMenuItem);

        MenuItem menuItem = nav_view.getMenu().findItem(R.id.messagesMenuItem);

        View actionView = MenuItemCompat.getActionView(menuItem);
        textCartItemCount = (TextView) actionView.findViewById(R.id.cart_badge);

        moneyTillMonthEndTv = (TextView) headerView.findViewById(R.id.moneyTillMonthEndTv);

        loginUsernameTv = (TextView) headerView.findViewById(R.id.loginUsernameTv);

        actionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMessageActivity();
            }
        });

        StatusDatabaseHandler statusDatabaseHandler = StatusDatabaseHandler.getInstance(getApplicationContext());
        int status = statusDatabaseHandler.getStatus();
        if(status == StatusDatabaseHandler.STATUS_MANUAL_OFFLINE) {
            billingsMenuItem.setVisible(false);
            budgetsMenuItem.setVisible(false);
        }else{
            billingsMenuItem.setVisible(true);
            budgetsMenuItem.setVisible(true);
        }

        updateRequestReceiver = new RequestServiceBroadcastReceiver(new RequestListener() {

            @Override
            public void requestDone(String activityClassName) {
            }

            @Override
            public void generalMessage(String activityClassName, BroadcastMessageActionEnum broadcastMessageActionEnum, String message) {
                if (activityClassName.equals(BaseActivity.class.getSimpleName())){

                     if (BroadcastMessageActionEnum.REFRESH.equals(broadcastMessageActionEnum)){
                         refreshRestSumForMonth(message);
                    }
                }
            }

            @Override
            public void loginMessage(LoginStatusEnum loginStatusEnum) {
                if (LoginStatusEnum.ACCESS_TOKEN_INVALID.equals(loginStatusEnum)){
                    Toast.makeText(getApplicationContext(), "Authentifizierungsfehler! Bitte nochmal einloggen.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(BaseActivity.this, ActivityLogin.class);
                    startActivityForResult(intent, LOGIN);
                }
            }

            @Override
            public void onlineStatusMessage(OnlineStatusEnum onlineStatusEnum) {
                if (OnlineStatusEnum.EXCEPTION.equals(onlineStatusEnum)){
                    Toast.makeText(getApplicationContext(), "Sorry, es ist ein unerwarteter Synchronisations-Fehler aufgetreten!\nDie fehlerhaften Daten wurden entfernt.\nEs wird ein vollständiger Download durchgeführt.", Toast.LENGTH_LONG).show();

                    AppUser appUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());
                    if (appUser != null){
                        appUser.setUpdateModusType(UpdateModusType.FULL_UPDATE);
                        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
                        mainDatabaseHandler.updateAppUser(appUser);
                    }

                    Intent intent = new Intent(BaseActivity.this, ActivityMain.class);
                    startActivity(intent);
                }else if (OnlineStatusEnum.UNEXPECTED_OFFLINE.equals(onlineStatusEnum)){
                    Toast.makeText(getApplicationContext(), "Keine Internet-Verbindung!\nSynchronistation erst sobald wieder online.", Toast.LENGTH_LONG).show();
                    StatusDatabaseHandler statusDatabaseHandler = StatusDatabaseHandler.getInstance(getApplicationContext());
                    statusDatabaseHandler.updateStatus(StatusDatabaseHandler.STATUS_UNEXPECTED_OFFLINE);
                } else if (OnlineStatusEnum.VERSION_CONFLICT.equals(onlineStatusEnum)) {
                    Toast.makeText(getApplicationContext(), "Inkompatible Android und Server-Version. Bitte Server und/oder App updaten.", Toast.LENGTH_LONG).show();

                    StatusDatabaseHandler statusDatabaseHandler = StatusDatabaseHandler.getInstance(getApplicationContext());

                    for (StatusDatabaseHandler.UpdateObject tmpUpdateObject : statusDatabaseHandler.getUpdateObjects()) {
                        statusDatabaseHandler.deleteObject(tmpUpdateObject.dbId);
                    }

                    AppUser appUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());
                    if (appUser != null) {
                        appUser.setUpdateModusType(UpdateModusType.FULL_UPDATE);
                        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
                        mainDatabaseHandler.updateAppUser(appUser);
                    }

                    Intent intent = new Intent(BaseActivity.this, ActivityMain.class);
                    startActivity(intent);
                }
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        actionBarDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.drawable.ic_drawer,
                R.string.open_drawer,
                R.string.close_drawer)
        {
            @Override
            public void onDrawerClosed(View drawerView) {
                getSupportActionBar().setTitle(listArray[position]);
                Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.baseline_menu_white_24);
                getSupportActionBar().setHomeAsUpIndicator(drawable);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle("NextBill");
                Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.baseline_menu_open_white_24);
                getSupportActionBar().setHomeAsUpIndicator(drawable);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                refreshMessageView();
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                super.onDrawerStateChanged(newState);
            }
        };
        mDrawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
        actionBarDrawerToggle.setHomeAsUpIndicator(R.drawable.ic_drawer);

        if(isLaunch){
            isLaunch = false;
            openActivity();
        }

        SharedPreferences settings = getSharedPreferences(ActivityMain.PREF_NAME, 0);
        String fi = settings.getString(SERVER_URL, "");
        if(!fi.equals("")) {
            RequestManager.setApiUrl(fi);
        }

        refreshRestSumForMonth(null);
    }

    protected void startInvoicesActivity(){
        startActivity(new Intent(BaseActivity.this, ActivityMain.class));
        overridePendingTransition(0, 0);
    }

    protected void startSettingsActivity(){
        startActivity(new Intent(BaseActivity.this, SettingsActivity.class));
    }

    protected void openMessageActivity(){
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());

        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.MESSAGE.name())
                .or()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.MISTAKE_MESSAGE.name())
                .or()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.CHANGED_MESSAGE.name())
                .or()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.BUDGET_MESSAGE.name());;

        List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

        if (basicDatas.isEmpty()){
            Toast.makeText(getApplicationContext(), "Keine Benachrichtigungen vorhanden!", Toast.LENGTH_LONG).show();
        }else{
            BaseActivity.position = 2;

            startActivity(new Intent(this, ActivityMessaging.class));
            overridePendingTransition(0, 0);
        }
    }

    public void enableBackgroundSync() {

        SharedPreferences settings = getSharedPreferences(ActivityMain.PREF_NAME, 0);
        int fi = settings.getInt(BACKGROUND_SYNC, -1);
        if (fi != 1) {
            Intent intent = new Intent(this, AlarmManagerBroadcastReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 31531278, intent, 0);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

            alarmManager.cancel(pendingIntent);
            alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + 100000, AlarmManager.INTERVAL_HALF_DAY, pendingIntent);
        }

        SharedPreferences.Editor sEditor = settings.edit();
        sEditor.putInt(BACKGROUND_SYNC, 1);
        sEditor.commit();

        syncSwitchView.setChecked(true);
    }

    public void disableBackgroundSync(int status) {

        Intent intent = new Intent(this, AlarmManagerBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 31531278, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        SharedPreferences settings = getSharedPreferences(ActivityMain.PREF_NAME, 0);
        SharedPreferences.Editor sEditor = settings.edit();
        sEditor.putInt(BACKGROUND_SYNC, status);
        sEditor.commit();

        syncSwitchView.setChecked(false);
    }

    public void refreshRestSumForMonth(String appUserId){
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
        AppUser appUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());

        String appUserIdTmp = null;
        if (appUser != null){
            appUserIdTmp  = appUser.getAppUserId().toString();
        }else if (appUserId != null){
            appUserIdTmp = appUserId;
        }

        BigDecimal restSumForMonth = new BigDecimal(0);
        if (appUserIdTmp != null){
            SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
            sqlBuilder.isEqual(MainDatabaseHandler.VAR_APP_USER_ID, appUserIdTmp)
                    .and()
                    .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.STATISTIC.name())
                    .and()
                    .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_SUB_TYPE, BasicDataSubType.CALCULATED_PROFIT_FOR_MONTH.name());
            List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);


            if (!basicDatas.isEmpty()){
                restSumForMonth = basicDatas.get(0).getNumberValue();
            }

            moneyTillMonthEndTv.setText(restSumForMonth.intValue() + " €");
        }
    }

    protected void refreshMessageView(){

        if (textCartItemCount == null){
            return;
        }

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());

        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.MESSAGE.name())
                .or()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.MISTAKE_MESSAGE.name())
                .or()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.CHANGED_MESSAGE.name())
                .or()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.BUDGET_MESSAGE.name());

        List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

        if (basicDatas.isEmpty()){
            if (textCartItemCount.getVisibility() != View.GONE) {
                textCartItemCount.setVisibility(View.GONE);
            }
        } else {
            textCartItemCount.setText(String.valueOf(Math.min(basicDatas.size(), 99)));
            if (textCartItemCount.getVisibility() != View.VISIBLE) {
                textCartItemCount.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(updateRequestReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(RequestManager.BC_SEND_MESSAGE);
        filter.addAction(RequestManager.BC_LOGIN);
        filter.addAction(RequestManager.BC_ONLINE);
        registerReceiver(updateRequestReceiver, filter);

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());
        if (currentUser != null){
            loginUsernameTv.setText(currentUser.getEmail());
        }else{
            loginUsernameTv.setText("Unbekannter Benutzer");
        }

        refreshMessageView();

        onlineSwitchView.setOnCheckedChangeListener(null);
        StatusDatabaseHandler statusDatabaseHandler = StatusDatabaseHandler.getInstance(getApplicationContext());
        int status = statusDatabaseHandler.getStatus();
        if(status == StatusDatabaseHandler.STATUS_UNEXPECTED_OFFLINE || status == StatusDatabaseHandler.STATUS_MANUAL_OFFLINE) {
            onlineSwitchView.setChecked(true);
        }else{
            onlineSwitchView.setChecked(false);
        }

        onlineSwitchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                StatusDatabaseHandler statusDatabaseHandler = StatusDatabaseHandler.getInstance(getApplicationContext());

                if (isChecked){
                    statusDatabaseHandler.updateStatus(StatusDatabaseHandler.STATUS_MANUAL_OFFLINE);
                    Toast.makeText(getApplicationContext(), "Offline-Modus aktiviert", Toast.LENGTH_SHORT).show();

                    disableBackgroundSync(-2);
                    syncSwitchView.setEnabled(false);
                    syncSwitchView.setChecked(false);
                    billingsMenuItem.setVisible(false);
                    budgetsMenuItem.setVisible(false);

//                    final Handler handler = new Handler();
//                    handler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            startActivity(new Intent(BaseActivity.this, ActivityMain.class));
//                            overridePendingTransition(0, 0);
//                        }
//                    }, 800);
                }else{
                    statusDatabaseHandler.updateStatus(StatusDatabaseHandler.STATUS_UPDATE_DONE);

                    AppUser appUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());
                    if (appUser != null) {
                        appUser.setUpdateModusType(UpdateModusType.PATCH_UPDATE);

                        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
                        mainDatabaseHandler.updateAppUser(appUser);
                    }

                    enableBackgroundSync();
                    syncSwitchView.setEnabled(true);
                    syncSwitchView.setChecked(true);
                    billingsMenuItem.setVisible(true);
                    budgetsMenuItem.setVisible(true);

//                    final Handler handler = new Handler();
//                    handler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            startActivity(new Intent(BaseActivity.this, ActivityMain.class));
//                            overridePendingTransition(0, 0);
//                        }
//                    }, 800);
                }
            }
        });

        syncSwitchView.setOnCheckedChangeListener(null);

        SharedPreferences settings = getSharedPreferences(ActivityMain.PREF_NAME, 0);
        int fi = settings.getInt(BACKGROUND_SYNC, -1);
        if(fi == -2 || fi == 0 || status == StatusDatabaseHandler.STATUS_UNEXPECTED_OFFLINE || status == StatusDatabaseHandler.STATUS_MANUAL_OFFLINE) {

            if (fi == -2){
                syncSwitchView.setEnabled(false);
                disableBackgroundSync(-2);
            }else{
                syncSwitchView.setEnabled(true);
                disableBackgroundSync(0);
            }
        }else{
            syncSwitchView.setEnabled(true);
            enableBackgroundSync();
        }

        syncSwitchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked){
                    enableBackgroundSync();
                    Toast.makeText(getApplicationContext(), "Hintergrund-Synchronisation ist aktiviert.", Toast.LENGTH_SHORT).show();
                }else{
                    disableBackgroundSync(0);
                    Toast.makeText(getApplicationContext(), "Hintergrund-Synchronisation ist deaktiviert.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void startLoginActivity(){

        LoginUserHelper.logoutAllUsers(getApplicationContext());

        mDrawerLayout.closeDrawer(nav_view);
        Intent intent = new Intent(this, ActivityLogin.class);
        startActivityForResult(intent, LOGIN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == LOGIN && resultCode == RESULT_OK) {
            openActivity();
        }
    }

    protected void openActivity() {
        mDrawerLayout.closeDrawer(nav_view);

        startInvoicesActivity();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(nav_view);
//        menu.findItem(R.id.action_settings).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        if (drawer.isDrawerOpen(GravityCompat.START)) {
//            drawer.closeDrawer(GravityCompat.START);
//        } else {
//            drawer.openDrawer(GravityCompat.START);
            super.onBackPressed();
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        StatusDatabaseHandler statusDatabaseHandler = StatusDatabaseHandler.getInstance(getApplicationContext());
        int status = statusDatabaseHandler.getStatus();
        if(status == StatusDatabaseHandler.STATUS_MANUAL_OFFLINE) {
            onlineSwitchView.setChecked(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        StatusDatabaseHandler statusDatabaseHandler = StatusDatabaseHandler.getInstance(getApplicationContext());
        int status = statusDatabaseHandler.getStatus();

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());
        if (currentUser != null){
            MainDatabaseHandler.deleteAllBasicSearchData(getApplicationContext());
        }

        final Handler handler = new Handler();
        switch (item.getItemId()) {
            case R.id.logoutMenuItem:
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startLoginActivity();
                    }
                }, 400);
                break;
            case R.id.settingsMenuItem:
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startSettingsActivity();
                    }
                }, 400);
                break;
            case R.id.messagesMenuItem:

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        openMessageActivity();
                    }
                }, 400);
                break;
            case R.id.invoicesMenuItem:

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        BaseActivity.position = 0;
                        startInvoicesActivity();
                    }
                }, 400);
                break;
            case R.id.standingOrdersMenuItem:

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        BaseActivity.position = 1;
                        startActivity(new Intent(BaseActivity.this, ActivityStandingOrder.class));
                        overridePendingTransition(0, 0);
                    }
                }, 400);

                break;
            case R.id.billingsMenuItem:

                if (status == StatusDatabaseHandler.STATUS_MANUAL_OFFLINE || status == StatusDatabaseHandler.STATUS_UNEXPECTED_OFFLINE) {
                    Toast.makeText(getApplicationContext(), "Offline-Modus aktiv.\nFunktion derzeit nicht möglich.", Toast.LENGTH_LONG).show();
                    break;
                }

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        BaseActivity.position = 3;
                        startActivity(new Intent(BaseActivity.this, ActivityBilling.class));
                        overridePendingTransition(0, 0);
                    }
                }, 400);

                break;
            case R.id.budgetsMenuItem:

                if (status == StatusDatabaseHandler.STATUS_MANUAL_OFFLINE || status == StatusDatabaseHandler.STATUS_UNEXPECTED_OFFLINE) {
                    Toast.makeText(getApplicationContext(), "Offline-Modus aktiv.\nFunktion derzeit nicht möglich.", Toast.LENGTH_LONG).show();
                    break;
                }

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        BaseActivity.position = 4;
                        startActivity(new Intent(BaseActivity.this, ActivityBudget.class));
                        overridePendingTransition(0, 0);
                    }
                }, 400);
                break;
            case R.id.analysisMenuItem:
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        BasicData basicDataExpense = MainDatabaseHandler.findBasicSearchDataOrCreate(getApplicationContext(), BasicDataSubType.EXPENSE);
                        MainDatabaseHandler.saveBasicSearchData(getApplicationContext(), basicDataExpense);
                        BaseActivity.position = 5;
                        startActivity(new Intent(BaseActivity.this, ActivityAnalysisTabTimeRange.class));
                        overridePendingTransition(0, 0);
                    }
                }, 400);

                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
