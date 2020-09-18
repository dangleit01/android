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


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import de.nextbill.client.R;
import de.nextbill.client.adapters.AdapterActivityMainTabs;
import de.nextbill.client.breceiver.RequestListener;
import de.nextbill.client.breceiver.RequestServiceBroadcastReceiver;
import de.nextbill.client.database.IOHelper;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.enums.BroadcastMessageActionEnum;
import de.nextbill.client.enums.CostDistributionItemTypeEnum;
import de.nextbill.client.enums.InvoiceSource;
import de.nextbill.client.enums.InvoiceStatusEnum;
import de.nextbill.client.enums.LoginStatusEnum;
import de.nextbill.client.enums.MainFunctionEnum;
import de.nextbill.client.enums.OnlineStatusEnum;
import de.nextbill.client.enums.RepetitionTypeEnum;
import de.nextbill.client.enums.UpdateModusType;
import de.nextbill.client.helper.LoginUserHelper;
import de.nextbill.client.interfaces.IFragmentToActivity;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.CostDistributionItem;
import de.nextbill.client.model.Invoice;
import de.nextbill.client.network.NetworkUtils;
import de.nextbill.client.network.RequestManager;
import de.nextbill.client.network.RequestUpdateService;
import de.nextbill.client.utils.SqlBuilder;

public class ActivityMain extends BaseActivity implements  DialogMainFunctions.SelectMainFunctionDialogListener, IFragmentToActivity, DialogUpdateRunning.OnBackPressedInDialog {

    private static final String TAG = "NextBill";

    private RequestServiceBroadcastReceiver updateRequestReceiver;

    public static final String PREF_NAME = "nextbill_prefs";
    private static final String PREF_FIRST_INSTALL = "first_install";
    private static final int MY_PERMISSIONS_REQUEST_TAKE_PICTURE = 64324;

    static int TAKE_PICTURE = 1;
    static int INVOICE_DETAILS = 2;
    static int LOGIN = 3;
    static int REQ_CODE_SPEECH_INPUT = 5;

    private ViewPager mViewPager;
    private AdapterActivityMainTabs adapter;
    private TabLayout tabLayout;
    private ProgressBar internetActivityBar;
    TextView filterTv;

    private Boolean openTabHasEntries;
    private Boolean closeTabHasEntries;

    private Boolean abortRefreshLists;

    private DialogUpdateRunning dialogUpdateRunning;

    private List<Invoice> openInvoices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getLayoutInflater().inflate(R.layout.activity_main, frameLayout);
        this.setTitle("Rechnungen");

        IOHelper.getInstance().setCtx(this);
        SharedPreferences settings = getSharedPreferences(PREF_NAME, 0);
        int fi = settings.getInt(PREF_FIRST_INSTALL, -1);
        if (fi < 0) {
            IOHelper.deleteAppDir();
            SharedPreferences.Editor sEditor = settings.edit();
            sEditor.putInt(PREF_FIRST_INSTALL, 1);
            sEditor.commit();
        }

        initializeTabs();

        internetActivityBar = (ProgressBar) findViewById(R.id.internetActivityBar);
        filterTv = (TextView) findViewById(R.id.filterTv);

        updateRequestReceiver = new RequestServiceBroadcastReceiver(new RequestListener() {

            @Override
            public void requestDone(String activityClassName) {
                swipeRefreshStatus(false);
                showInternetActivityBarStatus(false);
                showDialogUpdateRunning(false);
                refreshInvoiceLists(false);
            }

            @Override
            public void generalMessage(String activityClassName, BroadcastMessageActionEnum broadcastMessageActionEnum, String message) {
                if (activityClassName.equals(ActivityMain.class.getSimpleName())){
                    if (BroadcastMessageActionEnum.REFRESH.equals(broadcastMessageActionEnum)) {
                        refreshInvoiceLists(true);
                    }else if (BroadcastMessageActionEnum.INTERNAL.equals(broadcastMessageActionEnum) &&
                        message != null && message.equals("clear")) {
                        clearLists();
                    }else if (BroadcastMessageActionEnum.ERROR.equals(broadcastMessageActionEnum)) {
                        Toast.makeText(getApplicationContext(), "OCR-Service derzeit nicht erreichbar! Die Analyse kann sich verzögern!", Toast.LENGTH_LONG).show();
                    }
                }else if (activityClassName.equals(DialogUpdateRunning.class.getSimpleName())){
                    if (BroadcastMessageActionEnum.PROGRESS.equals(broadcastMessageActionEnum)) {
                        if (dialogUpdateRunning != null){
                            dialogUpdateRunning.updateText(message);
                        }
                    }else if (BroadcastMessageActionEnum.ERROR.equals(broadcastMessageActionEnum)) {
                        LoginUserHelper.logoutAllUsers(getApplicationContext());
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(ActivityMain.this, ActivityLogin.class);
                        startActivityForResult(intent, LOGIN);
                    }
                }
            }

            @Override
            public void onlineStatusMessage(OnlineStatusEnum onlineStatusEnum) {
                swipeRefreshStatus(false);
                showInternetActivityBarStatus(false);
                showDialogUpdateRunning(false);
                refreshInvoiceLists(false);
            }

            @Override
            public void loginMessage(LoginStatusEnum loginStatusEnum) {

            }

        });

        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.baseline_menu_white_24);
        getSupportActionBar().setHomeAsUpIndicator(drawable);
    }

    public List<Invoice> invoicesWithStatusAnalyzing(){
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
        List<Invoice> invoices = mainDatabaseHandler.findInvoices(MainDatabaseHandler.VAR_INVOICE_STATUS_ENUM, null, InvoiceStatusEnum.ANALYZING.toString());
        List<Invoice> invoices2 = mainDatabaseHandler.findInvoices(MainDatabaseHandler.VAR_INVOICE_STATUS_ENUM, null, InvoiceStatusEnum.WAIT_FOR_UPLOAD.toString());
        invoices.addAll(invoices2);
        if (!invoices.isEmpty()){
            return invoices;
        }

        return new ArrayList<>();
    }

    @Override
    public void onBackPressedInUpdateRunningDialog() {
        LoginUserHelper.logoutAllUsers(getApplicationContext());
        Toast.makeText(getApplicationContext(), "Download abgebrochen! Bitte erneut einloggen.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(ActivityMain.this, ActivityLogin.class);
        startActivityForResult(intent, LOGIN);
    }

    interface BroadcastUpdateListener {
        void update(String text);
    }

    public void showDialogUpdateRunning(boolean show){
        if (show){
            FragmentManager fm = getSupportFragmentManager();
            dialogUpdateRunning = new DialogUpdateRunning();
            dialogUpdateRunning.show(fm, "dialogUpdateRunning");
        }else{
            if (dialogUpdateRunning != null){
                dialogUpdateRunning.dismiss();
            }
        }
    }

    public void clearLists(){
        Fragment fragment = adapter.getFragment(0, mViewPager);
        if (fragment != null){
            ActivityMainTabOpenInvoices activityMainTabOpenInvoices = (ActivityMainTabOpenInvoices) fragment;
            activityMainTabOpenInvoices.clearList();
        }

        fragment = adapter.getFragment(1, mViewPager);
        if (fragment != null){
            ActivityMainTabReadyInvoices activityMainTabReadyInvoices = (ActivityMainTabReadyInvoices) fragment;
            activityMainTabReadyInvoices.clearList();
        }
    }

    public void refreshInvoiceLists(boolean autoTabShow){
        AppUser appUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());
        if (appUser == null){
            return;
        }
        Fragment fragment = adapter.getFragment(0, mViewPager);
        if (fragment != null){
            ActivityMainTabOpenInvoices activityMainTabOpenInvoices = (ActivityMainTabOpenInvoices) fragment;
            activityMainTabOpenInvoices.refreshInvoiceList(autoTabShow);
        }

        fragment = adapter.getFragment(1, mViewPager);
        if (fragment != null){
            ActivityMainTabReadyInvoices activityMainTabReadyInvoices = (ActivityMainTabReadyInvoices) fragment;
            activityMainTabReadyInvoices.refreshInvoiceList(autoTabShow);
        }
    }



    public void swipeRefreshStatus(Boolean active){
        Fragment fragment = adapter.getFragment(0, mViewPager);
        if (fragment != null){
            ((ActivityMainTabOpenInvoices) fragment).swipeRefreshStatus(active);
        }

        fragment = adapter.getFragment(1, mViewPager);
        if (fragment != null){
            ((ActivityMainTabReadyInvoices) fragment).swipeRefreshStatus(active);
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
    protected void onDestroy() {
        super.onDestroy();

        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        handler.updateStatus(StatusDatabaseHandler.STATUS_UPDATE_DONE);
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

//        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
//        List<Invoice> openInvoices = mainDatabaseHandler.findInvoices(MainDatabaseHandler.VAR_INVOICE_STATUS_ENUM, SQLOperatorEnum.NOT,
//                InvoiceStatusEnum.READY.toString(), InvoiceStatusEnum.DELETED.toString());
//
        initializeTabs();
//
//        if (openInvoices.isEmpty()){
//            mViewPager.setCurrentItem(1);
//        }

        StatusDatabaseHandler statusDatabaseHandler = StatusDatabaseHandler.getInstance(getApplicationContext());
        int status = statusDatabaseHandler.getStatus();
        if (status != StatusDatabaseHandler.STATUS_RUNNING) {
            showInternetActivityBarStatus(false);
        }else{
            showInternetActivityBarStatus(true);
        }

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_APP_USER);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_APP_USER_IS_LOGGED_IN, "1");

        List<AppUser> appUserList = mainDatabaseHandler.findAppUsers(sqlBuilder);

        AppUser appUser = null;
        if (!appUserList.isEmpty()){

            Calendar calendarTmp = new GregorianCalendar();
            calendarTmp.setTime(appUserList.get(0).getAppUserAccessExpiration());
            calendarTmp.add(Calendar.HOUR, 96);
            Date criticalDate = calendarTmp.getTime();

            if (new Date().before(criticalDate)){
                appUser = appUserList.get(0);
            }
        }

        if (appUser != null){

            if (status == StatusDatabaseHandler.STATUS_MANUAL_OFFLINE) {
                refreshInvoiceLists(false);
                return;
            }

            boolean doUpdateTmp = false;

            Date now = new Date();
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(now);
            calendar.add(Calendar.MINUTE, Integer.valueOf(-15));
            Date updateUntilTime = calendar.getTime();

            if (UpdateModusType.FULL_UPDATE.equals(appUser.getUpdateModusType())){
                doUpdateTmp = true;
            }else if (UpdateModusType.PATCH_UPDATE.equals(appUser.getUpdateModusType()) ||
                    appUser.getLastUpdate().before(updateUntilTime)){
                doUpdateTmp = true;
            }

            if (doUpdateTmp){
                showInternetActivityBarStatus(true);
                if (UpdateModusType.FULL_UPDATE.equals(appUser.getUpdateModusType())){
                    showDialogUpdateRunning(true);
                }

                synchronizeAll();
            }else{
                if (this.abortRefreshLists == null || this.abortRefreshLists == false){
                    refreshInvoiceLists(false);
                }else{
                    this.abortRefreshLists = false;
                }
            }
        } else {
            LoginUserHelper.logoutAllUsers(getApplicationContext());

            statusDatabaseHandler.updateStatus(StatusDatabaseHandler.STATUS_UPDATE_DONE);

            Intent intent = new Intent(this, ActivityLogin.class);
            startActivityForResult(intent, LOGIN);
        }
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Nennen Sie Unternehmen, Datum und Summe.");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(), "Sorry, Spracherkennungs-Funktion nicht in Gerät installiert!", Toast.LENGTH_SHORT).show();
        }
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
        tabs.add("Offen");
        tabs.add("Überprüft");

        if (adapter == null) {
            adapter = new AdapterActivityMainTabs(this, getSupportFragmentManager(), tabs);
        }

        if (mViewPager.getAdapter() == null){
            mViewPager.setAdapter(adapter);
            tabLayout.setupWithViewPager(mViewPager);
        }
    }

    private void showMainFunctionsDialog() {
        FragmentManager fm = getSupportFragmentManager();
        DialogMainFunctions dialogMainFunctions = new DialogMainFunctions();

        dialogMainFunctions.show(fm, "dialog_main_functions");
    }

    public boolean hasPermission(int requestCode, String... permissionCodes){
        List<String> grantsToAsk = new ArrayList<>();

        for (String permissionCode : permissionCodes) {
            if (ContextCompat.checkSelfPermission(this, permissionCode) != PackageManager.PERMISSION_GRANTED) {
                grantsToAsk.add(permissionCode);
            }
        }

        String[] grantsArray = new String[grantsToAsk.size()];
        grantsArray = grantsToAsk.toArray(grantsArray);

        if (!grantsToAsk.isEmpty()) {
            ActivityCompat.requestPermissions(this, grantsArray, requestCode);
            return false;
        }

        return true;
    }

    public void takePicture(){

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
        Invoice invoice = new Invoice();
        invoice.setInvoiceSource(InvoiceSource.CAMERA);
        invoice.setInvoiceId(UUID.randomUUID());
        invoice.setInvoiceStatusEnum(InvoiceStatusEnum.TEMP);
        mainDatabaseHandler.insertInvoice(invoice);

        File file = IOHelper.getTempUploadMediaFile(invoice.getInvoiceId().toString());

        Uri photoURI = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".fileprovider", file);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, TAKE_PICTURE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_TAKE_PICTURE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePicture();
//                } else {
//                    Toast.makeText(getApplicationContext(), "Bitte erteilen Sie die Kamerafreigabe! Andernfalls ist die Nutzung der Funktion nicht möglich!", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == TAKE_PICTURE && resultCode == RESULT_OK) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            startUploadImageService();

            alertDialogForTakePhoto();
        }else if (requestCode == TAKE_PICTURE && resultCode == RESULT_CANCELED) {
            MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
            List<Invoice> invoices = mainDatabaseHandler.findInvoices(MainDatabaseHandler.VAR_INVOICE_STATUS_ENUM, InvoiceStatusEnum.TEMP.name());
            if (!invoices.isEmpty()){
                mainDatabaseHandler.deleteInvoices(invoices);
            }
        }else if (requestCode == LOGIN && resultCode == RESULT_OK) {
//            clearLists();
        }else if (requestCode == REQ_CODE_SPEECH_INPUT  && resultCode == RESULT_OK ) {
            ArrayList<String> result = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            Log.i(TAG, "Speech recognition result:"+result.get(0));

            StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
            long rowId = handler.addObject(result.get(0), StatusDatabaseHandler.OBJECT_TYPE_SPEECH_RECOGNITION, StatusDatabaseHandler.UPDATE_STATUS_ADD, new Date().getTime(), 1);

            Intent sIntent = new Intent(getApplicationContext(), RequestUpdateService.class);
            sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
            getApplicationContext().startService(sIntent);

        }else if (requestCode == ACTIVITY_SEARCH  && resultCode == RESULT_OK ) {
            resetSearchAmount();
        }
    }

    private void alertDialogForTakePhoto(){
        AlertDialog.Builder builder = new AlertDialog.Builder(ActivityMain.this);
        builder.setTitle("Weiteren Beleg aufnehmen?");

        builder.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                takePicture();
            }
        });
        builder.setNegativeButton("Nein", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    protected void startUploadImageService(){

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
        List<Invoice> invoices = mainDatabaseHandler.findInvoices(MainDatabaseHandler.VAR_INVOICE_STATUS_ENUM, InvoiceStatusEnum.TEMP.name());
        if (invoices.isEmpty()){
            Log.w(TAG, "No tempUploadImageInvoice!");
            Toast.makeText(getApplicationContext(),
                    "Fehler!", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(getApplicationContext(),
                "Bitte warten!", Toast.LENGTH_LONG).show();

        Invoice newInvoice = new Invoice();
        AppUser appUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());
        newInvoice.setCreatedById(appUser.getAppUserId());
        UUID newInvoiceId = invoices.get(0).getInvoiceId();
        newInvoice.setInvoiceId(newInvoiceId);
        newInvoice.setRepetitionTypeEnum(RepetitionTypeEnum.ONCE);
        newInvoice.setSpecialType(false);
        newInvoice.setInvoiceSource(InvoiceSource.CAMERA);
        newInvoice.setPayer(appUser);
        newInvoice.setInvoiceStatusEnum(InvoiceStatusEnum.WAIT_FOR_UPLOAD);
        mainDatabaseHandler.insertInvoice(newInvoice);

        CostDistributionItem costDistributionItem = new CostDistributionItem();
        costDistributionItem.setPayer(appUser);
        costDistributionItem.setCostDistributionItemTypeEnum(CostDistributionItemTypeEnum.QUOTA);
        costDistributionItem.setValue(new BigDecimal(1));
        costDistributionItem.setInvoiceId(newInvoiceId);
        costDistributionItem.setPosition(0);
        mainDatabaseHandler.insertCostDistributionItem(costDistributionItem);

        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        handler.addObject(newInvoiceId.toString(), StatusDatabaseHandler.OBJECT_TYPE_INVOICE_IMAGE, StatusDatabaseHandler.UPDATE_STATUS_ADD, new Date().getTime(), 1);

        Intent sIntent = new Intent(getApplicationContext(), RequestUpdateService.class);
        sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
        getApplicationContext().startService(sIntent);

//        mViewPager.setCurrentItem(0, true);

        List<Invoice> invoicesToDelete = mainDatabaseHandler.findInvoices(MainDatabaseHandler.VAR_INVOICE_STATUS_ENUM, InvoiceStatusEnum.TEMP.name());
        if (!invoicesToDelete.isEmpty()){
            mainDatabaseHandler.deleteInvoicesByInternalId(invoicesToDelete);
        }

        showInternetActivityBarStatus(true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        if (handler.getStatus() != StatusDatabaseHandler.STATUS_MANUAL_OFFLINE) {
            handler.updateStatus(StatusDatabaseHandler.STATUS_UPDATE_DONE);
        }


        unregisterReceiver(updateRequestReceiver);
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void startFilterActivity(){
        Intent intent = new Intent(ActivityMain.this, ActivitySearchConfiguration.class);
        startActivityForResult(intent, ACTIVITY_SEARCH);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.manualAddMenuButton:
                showMainFunctionsDialogButton();
                return true;
            case R.id.filterMenuButton:
                startFilterActivity();
                return true;
            case R.id.takePhoto:
                onFinishMainFunctionsDialog(MainFunctionEnum.AUTO_RECOGNITION, false);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void synchronizeAll() {
        if (NetworkUtils.getConnectivityStatus(getApplicationContext()) != NetworkUtils.TYPE_NOT_CONNECTED){
            Intent rIntent = new Intent(this, RequestUpdateService.class);
            rIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_TYPE_PATCH_UPDATE);
            startService(rIntent);
            Toast.makeText(this, "Synchronisiere...", Toast.LENGTH_LONG).show();
        }else{
//            Toast.makeText(this, "Keine Internet-Verbindung!", Toast.LENGTH_LONG).show();
            showInternetActivityBarStatus(false);
            showDialogUpdateRunning(false);
        }

    }

    public void showMainFunctionsDialogButton() {
        showMainFunctionsDialog();
    }

    public void onFinishMainFunctionsDialog(MainFunctionEnum mainFunctionEnum, boolean isStandingOrder) {
        if (mainFunctionEnum.equals(MainFunctionEnum.AUTO_RECOGNITION)) {

            if (hasPermission(MY_PERMISSIONS_REQUEST_TAKE_PICTURE, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                takePicture();
            }

        }else if (mainFunctionEnum.equals(MainFunctionEnum.MIC_AUTO_RECOGNITION)){
            promptSpeechInput();
        }else if (mainFunctionEnum.equals(MainFunctionEnum.NEW_EXPENSE)){
            Intent intent = new Intent(ActivityMain.this, ActivityInvoiceDetails.class);
            intent.putExtra("transactionType", "NEW_EXPENSE");
            startActivityForResult(intent, INVOICE_DETAILS);
            overridePendingTransition(R.anim.activity_in1, R.anim.activity_in2);
        }else if (mainFunctionEnum.equals(MainFunctionEnum.NEW_INCOME)){
            Intent intent = new Intent(ActivityMain.this, ActivityInvoiceDetails.class);
            intent.putExtra("transactionType", "NEW_INCOME");
            startActivityForResult(intent, INVOICE_DETAILS);
            overridePendingTransition(R.anim.activity_in1, R.anim.activity_in2);
        }
    }

    @Override
    public void showInternetActivityBar(Boolean active) {
        showInternetActivityBarStatus(active);

        AppUser appUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());
        if (appUser != null && appUser.getLastUpdate() == null){
            showDialogUpdateRunning(active);
        }
    }

    @Override
    public void refreshTabCounter(Integer number1, Integer number2) {
        Fragment fragment = adapter.getFragment(0, mViewPager);
        if (fragment != null && number1 != null){
            tabLayout.getTabAt(0).setText("Offen (" + number1 + ")");
        }

        fragment = adapter.getFragment(1, mViewPager);
        if (fragment != null && number2 != null){
            tabLayout.getTabAt(1).setText("Überprüft (" + number2 + ")");
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
            if (autoTabShow){
                if (openTabHasEntries){
                    mViewPager.setCurrentItem(0);
                }else{
                    mViewPager.setCurrentItem(1);
                }
            }else{
                if (!closeTabHasEntries){
                    mViewPager.setCurrentItem(0);
                }else if (!openTabHasEntries){
                    mViewPager.setCurrentItem(1);
                }
            }
        }
    }

    @Override
    public void refreshLists(boolean autoTabShow) {
        refreshInvoiceLists(autoTabShow);
    }

    public void resetSearchAmount() {
        Fragment fragment = adapter.getFragment(1, mViewPager);
        if (fragment != null){
            ((ActivityMainTabReadyInvoices) fragment).resetSearchAmount();
        }
    }

    @Override
    public void filterText(boolean show, String text) {
        if (show && filterTv != null){
            filterTv.setText("Filter: " + text);
            filterTv.setVisibility(View.VISIBLE);
        }else{
            filterTv.setVisibility(View.GONE);
        }
    }
}
