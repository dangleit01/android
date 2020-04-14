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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import java.util.Collections;
import java.util.List;

import de.nextbill.client.R;
import de.nextbill.client.adapters.AdapterFragmentOpenInvoicesListView;
import de.nextbill.client.breceiver.RequestListener;
import de.nextbill.client.breceiver.RequestServiceBroadcastReceiver;
import de.nextbill.client.comparators.InvoiceStatusComparator;
import de.nextbill.client.database.IOHelper;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.enums.BroadcastMessageActionEnum;
import de.nextbill.client.enums.InvoiceStatusEnum;
import de.nextbill.client.enums.LoginStatusEnum;
import de.nextbill.client.enums.MainFunctionEnum;
import de.nextbill.client.enums.OnlineStatusEnum;
import de.nextbill.client.helper.LoginUserHelper;
import de.nextbill.client.helper.SearchHelper;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.Invoice;
import de.nextbill.client.network.RequestManager;
import de.nextbill.client.pojos.SearchFilter;
import de.nextbill.client.utils.SqlBuilder;

public class ActivityStandingOrder extends BaseActivity implements DialogMainFunctions.SelectMainFunctionDialogListener  {

    private RequestServiceBroadcastReceiver updateRequestReceiver;

    ListView list;
    TextView filterTv;
    private ProgressBar internetActivityBar;

    static int INVOICE_DETAILS = 2;

    AdapterFragmentOpenInvoicesListView bindingData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = getLayoutInflater().inflate(R.layout.activity_standing_order, frameLayout);
        this.setTitle("Daueraufträge");

        filterTv = (TextView) findViewById(R.id.filterTv);

        internetActivityBar = (ProgressBar) findViewById(R.id.internetActivityBar);

        list = (ListView) findViewById(R.id.list);

        bindingData = new AdapterFragmentOpenInvoicesListView(this);

        list = (ListView) view.findViewById(R.id.list);
        list.setAdapter(bindingData);
        list.setSelector(R.drawable.activity_main_list_selector);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                StatusDatabaseHandler statusDatabaseHandler = StatusDatabaseHandler.getInstance(getApplicationContext());
                System.out.println("statusDatabaseHandler.getStatus()" + statusDatabaseHandler.getStatus());

                if (!bindingData.getInvoiceList().get(position).getInvoiceStatusEnum().equals(InvoiceStatusEnum.ANALYZING) &&
                        !bindingData.getInvoiceList().get(position).getInvoiceStatusEnum().equals(InvoiceStatusEnum.WAIT_FOR_UPLOAD)) {
                    Intent intent = new Intent(getApplicationContext(), ActivityInvoiceDetails.class);
                    intent.putExtra("idInvoice", bindingData.getInvoiceList().get(position).getInvoiceId().toString());
                    intent.putExtra("isStandingOrder", true);
                    startActivityForResult(intent, INVOICE_DETAILS);
                    overridePendingTransition(R.anim.activity_in1, R.anim.activity_in2);
                }
            }
        });

        updateRequestReceiver = new RequestServiceBroadcastReceiver(new RequestListener() {

            @Override
            public void requestDone(String activityClassName) {
//                showInternetActivityBarStatus(false);
                refreshInvoiceList();
                showInternetActivityBarStatus(false);
            }

            @Override
            public void generalMessage(String activityClassName, BroadcastMessageActionEnum broadcastMessageActionEnum, String message) {
                if (activityClassName.equals(ActivityStandingOrder.class.getSimpleName())){
                    if (broadcastMessageActionEnum != null && BroadcastMessageActionEnum.READY.equals(broadcastMessageActionEnum) && message.equals("StandingOrderCreated")) {
                        Toast.makeText(getApplicationContext(), "Dauerauftrag erfolgreich erstellt!", Toast.LENGTH_LONG).show();
                    }else if (broadcastMessageActionEnum != null && BroadcastMessageActionEnum.ERROR.equals(broadcastMessageActionEnum) && message.equals("StandingOrderCreated")) {
                        Toast.makeText(getApplicationContext(), "Interner Fehler! Dauerauftrag konnte nicht erstellt werden!", Toast.LENGTH_LONG).show();
                    }else if (broadcastMessageActionEnum != null && BroadcastMessageActionEnum.REFRESH.equals(broadcastMessageActionEnum)) {
                        refreshInvoiceList();
                    }
                }
            }

            @Override
            public void loginMessage(LoginStatusEnum loginStatusEnum) {

            }

            @Override
            public void onlineStatusMessage(OnlineStatusEnum onlineStatusEnum) {
                refreshInvoiceList();
                showInternetActivityBarStatus(false);
            }

        });

        refreshInvoiceList();

        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.baseline_menu_white_24);
        getSupportActionBar().setHomeAsUpIndicator(drawable);
    }

    public void refreshInvoiceList(){
        AppUser appUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());
        if (appUser == null){
            return;
        }

        list.destroyDrawingCache();

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());

        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.TABLE_INVOICE);
        sqlBuilder.addTable(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM);
        sqlBuilder
            .startBracket()
                .startBracket()
                    .isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_CREATED_BY_ID, currentUser.getAppUserId().toString())
                    .and()
                    .isEqualFields(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_ID, MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_STANDING_ORDER_INVOICE_TEMPLATE_ID)
                .endBracket()
            .endBracket()
            .and();

        SearchFilter searchFilter = SearchHelper.searchAndUseSearchConfiguration(getApplicationContext(), true, sqlBuilder);
        List<Invoice> invoices = SearchHelper.invoicesInInvoiceCostDistributionItems(searchFilter.getInvoiceCostDistributionItemList());

        Collections.sort(invoices, new InvoiceStatusComparator());

        bindingData.swapInvoiceList(invoices);
        bindingData.notifyDataSetChanged();

        filterText(searchFilter.isShowSearchString(), searchFilter.getSearchString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        IOHelper.getInstance().setCtx(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(RequestManager.BC_REQUEST_DONE);
        filter.addAction(RequestManager.BC_SEND_MESSAGE);
        filter.addAction(RequestManager.BC_ONLINE);
        registerReceiver(updateRequestReceiver, filter);

        StatusDatabaseHandler statusDatabaseHandler = StatusDatabaseHandler.getInstance(getApplicationContext());
        int status = statusDatabaseHandler.getStatus();
        if (status != StatusDatabaseHandler.STATUS_RUNNING) {
            showInternetActivityBarStatus(false);
        }else{
            showInternetActivityBarStatus(true);
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

        getMenuInflater().inflate(R.menu.standing_order, menu);
        return true;
    }

    private void showMainFunctionsDialog() {
        FragmentManager fm = getSupportFragmentManager();
        DialogMainFunctions dialogMainFunctions = new DialogMainFunctions();
        dialogMainFunctions.setArguments(true);

        dialogMainFunctions.show(fm, "dialog_main_functions_standing_order");
    }

    public void onFinishMainFunctionsDialog(MainFunctionEnum mainFunctionEnum, boolean isStandingOrder) {
        if (mainFunctionEnum.equals(MainFunctionEnum.NEW_EXPENSE)){
            Intent intent = new Intent(ActivityStandingOrder.this, ActivityInvoiceDetails.class);
            intent.putExtra("transactionType", "NEW_EXPENSE");
            if (isStandingOrder){
                intent.putExtra("isStandingOrder", true);
            }
            startActivityForResult(intent, INVOICE_DETAILS);
            overridePendingTransition(R.anim.activity_in1, R.anim.activity_in2);
        }else if (mainFunctionEnum.equals(MainFunctionEnum.NEW_INCOME)){
            Intent intent = new Intent(ActivityStandingOrder.this, ActivityInvoiceDetails.class);
            intent.putExtra("transactionType", "NEW_INCOME");
            if (isStandingOrder){
                intent.putExtra("isStandingOrder", true);
            }
            startActivityForResult(intent, INVOICE_DETAILS);
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
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == INVOICE_DETAILS && resultCode == RESULT_OK) {
            refreshInvoiceList();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.manualAddMenuButton:
                showMainFunctionsDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void filterText(boolean show, String text) {
        if (show && filterTv != null){
            filterTv.setText("Filter: " + text);
            filterTv.setVisibility(View.VISIBLE);
        }else{
            filterTv.setVisibility(View.GONE);
        }
    }
}
