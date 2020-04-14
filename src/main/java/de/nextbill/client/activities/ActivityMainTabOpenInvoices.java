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

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import de.nextbill.client.R;
import de.nextbill.client.adapters.AdapterFragmentOpenInvoicesListView;
import de.nextbill.client.comparators.InvoiceStatusComparator;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.enums.CorrectionStatus;
import de.nextbill.client.enums.InvoiceStatusEnum;
import de.nextbill.client.helper.CostDistributionHelper;
import de.nextbill.client.helper.LoginUserHelper;
import de.nextbill.client.helper.SearchHelper;
import de.nextbill.client.interfaces.IFragmentToActivity;
import de.nextbill.client.interfaces.IPaymentItem;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.CostDistributionItem;
import de.nextbill.client.model.Invoice;
import de.nextbill.client.network.NetworkUtils;
import de.nextbill.client.network.RequestUpdateService;
import de.nextbill.client.pojos.SearchFilter;
import de.nextbill.client.utils.SqlBuilder;


public class ActivityMainTabOpenInvoices extends Fragment {

    private SwipeRefreshLayout swipeContainer;

    ListView list;

    static int INVOICE_DETAILS = 2;

    AdapterFragmentOpenInvoicesListView bindingData;

    Boolean autoTabShow = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_main_fragment_open_invoices, container, false);

        bindingData = new AdapterFragmentOpenInvoicesListView(this.getActivity());

        list = (ListView) view.findViewById(R.id.list);

        list.setAdapter(bindingData);

        list.setSelector(R.drawable.activity_main_list_selector);
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                StatusDatabaseHandler statusDatabaseHandler = StatusDatabaseHandler.getInstance(getActivity().getApplicationContext());
                System.out.println("statusDatabaseHandler.getStatus()" + statusDatabaseHandler.getStatus());

                if (!bindingData.getInvoiceList().get(position).getInvoiceStatusEnum().equals(InvoiceStatusEnum.ANALYZING) &&
                        !bindingData.getInvoiceList().get(position).getInvoiceStatusEnum().equals(InvoiceStatusEnum.WAIT_FOR_UPLOAD)) {
                    Intent intent = new Intent(getActivity().getApplicationContext(), ActivityInvoiceDetails.class);
                    intent.putExtra("idInvoice", bindingData.getInvoiceList().get(position).getInvoiceId().toString());
                    getActivity().startActivityForResult(intent, INVOICE_DETAILS);
                    getActivity().overridePendingTransition(R.anim.activity_in1, R.anim.activity_in2);
                }
            }
        });

        list.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            private int nr = 0;

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                // TODO Auto-generated method stub
                bindingData.clearSelection();
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // TODO Auto-generated method stub

                nr = 0;
                MenuInflater inflater = getActivity().getMenuInflater();
                inflater.inflate(R.menu.open_invoices_cab, menu);
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                // TODO Auto-generated method stub
                switch (item.getItemId()) {

                    case R.id.menuItemAllChecked:
                        nr = 0;
                        setItemsAsChecked();
                        mode.finish();
                }
                return false;
            }

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, final int position,
                                                  long id, boolean checked) {
                // TODO Auto-generated method stub
                if (checked) {
                    nr++;
                    bindingData.setNewSelection(position, checked);
                } else {
                    nr--;
                    bindingData.removeSelection(position);
                }

                bindingData.notifyDataSetChanged();
            }
        });

        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int position, long arg3) {
                // TODO Auto-generated method stub

                list.setItemChecked(position, !bindingData.isPositionChecked(position));
                return false;
            }
        });

        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        swipeContainer.setBackgroundColor(Color.rgb(230, 230, 230));
        swipeContainer.setProgressBackgroundColorSchemeColor(Color.rgb(230, 230, 230));
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshStatus(false);
                synchronizeAll();
            }
        });

        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    public void setItemsAsChecked(){
        List<Invoice> invoices = getCheckedInvoices();
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getContext());
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getContext());

        List<IPaymentItem> paymentItemsToUpdate = new ArrayList<>();
        for (Invoice currentInvoice : invoices) {

            SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM);
            sqlBuilder.isEqual(MainDatabaseHandler.VAR_INVOICE_ID, currentInvoice.getInvoiceId().toString());
            List<CostDistributionItem> costDistributionItems = mainDatabaseHandler.findCostDistributionItemsSql(sqlBuilder);

            if (CostDistributionHelper.isCreatedUserCheckMode(getContext(), currentInvoice)){
                currentInvoice.setInvoiceStatusEnum(InvoiceStatusEnum.READY);
                paymentItemsToUpdate.add(currentInvoice);
            }else if (CostDistributionHelper.isExternalUserCheckMode(getContext(), currentInvoice, costDistributionItems)){
                List<IPaymentItem> paymentItems = CostDistributionHelper.paymentItemsForCorrectionMode(getContext(), currentInvoice, costDistributionItems, true);
                for (IPaymentItem paymentItem : paymentItems) {
                    paymentItem.setCorrectionStatus(CorrectionStatus.READY);
                }
                paymentItemsToUpdate.addAll(paymentItems);
            }
        }

        mainDatabaseHandler.updatePaymentItems(paymentItemsToUpdate);

        IFragmentToActivity iFragmentToActivity = (IFragmentToActivity) getActivity();
        iFragmentToActivity.refreshLists(false);

        for (IPaymentItem iPaymentItem : paymentItemsToUpdate) {
            if (iPaymentItem instanceof Invoice){
                handler.addObject(((Invoice) iPaymentItem).getInvoiceId().toString(), StatusDatabaseHandler.OBJECT_TYPE_INVOICE, StatusDatabaseHandler.UPDATE_STATUS_PUT, new Date().getTime(), 1);
            }else{
                handler.addObject(((CostDistributionItem) iPaymentItem).getCostDistributionItemId().toString(), StatusDatabaseHandler.OBJECT_TYPE_COST_DISTRIBUTION_ITEM, StatusDatabaseHandler.UPDATE_STATUS_PUT, new Date().getTime(), 1);
            }
        }

        Intent sIntent = new Intent(getContext(), RequestUpdateService.class);
        sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
        getContext().startService(sIntent);
    }

    public void synchronizeAll() {

        if (NetworkUtils.getConnectivityStatus(getActivity().getApplicationContext()) != NetworkUtils.TYPE_NOT_CONNECTED){
            Intent rIntent = new Intent(getActivity().getApplicationContext(), RequestUpdateService.class);
            rIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_TYPE_PATCH_UPDATE);
            this.getActivity().getApplicationContext().startService(rIntent);

            IFragmentToActivity iFragmentToActivity = (IFragmentToActivity) getActivity();
            iFragmentToActivity.showInternetActivityBar(true);
        }else{
            Toast.makeText(this.getActivity().getApplicationContext(), "Keine Internet-Verbindung!", Toast.LENGTH_LONG).show();

            IFragmentToActivity iFragmentToActivity = (IFragmentToActivity) getActivity();
            iFragmentToActivity.showInternetActivityBar(false);
            swipeRefreshStatus(false);
            refreshInvoiceList(false);
        }

    }

    public List<Invoice> getCheckedInvoices(){
        return bindingData.getCheckedInvoices();
    }

    public void swipeRefreshStatus(Boolean active){
        if (active){
            swipeContainer.post(new Runnable() {
                @Override
                public void run() {
                    swipeContainer.setRefreshing(true);
                }
            });
        }else{
            swipeContainer.setRefreshing(false);
        }
    }

    public void clearList(){
        if (list != null){
            list.destroyDrawingCache();
            bindingData.swapInvoiceList(new ArrayList<Invoice>());
            bindingData.notifyDataSetChanged();
            IFragmentToActivity iFragmentToActivity = (IFragmentToActivity) getActivity();
            iFragmentToActivity.refreshTabCounter(0, null);
        }
    }

    public void refreshInvoiceList(boolean autoTabShowTmp){

        AppUser appUser = LoginUserHelper.currentLoggedInUser(getContext());
        if (appUser == null){
            return;
        }

        this.autoTabShow = autoTabShowTmp;

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (list == null){
                    return;
                }
                list.destroyDrawingCache();

                List<String> userContactIds = LoginUserHelper.idsOfUserContactsWithCurrentUser(getContext());
                AppUser currentUser = LoginUserHelper.currentLoggedInUser(getContext());

                SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.TABLE_INVOICE);
                sqlBuilder.addTable(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM);
                sqlBuilder
                    .startBracket()
                        .startBracket()
                            .isNotEqualFields(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_ID, MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_STANDING_ORDER_INVOICE_TEMPLATE_ID)
                            .or()
                            .isNull(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_STANDING_ORDER_INVOICE_TEMPLATE_ID)
                        .endBracket()
                        .and()
                        .startBracket()
                            .isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_CREATED_BY_ID, currentUser.getAppUserId().toString())
                            .and()
                            .startBracket()
                                .isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_STATUS_ENUM, InvoiceStatusEnum.CHECK.name())
                                .or()
                                .isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_STATUS_ENUM, InvoiceStatusEnum.ANALYZING.name())
                                .or()
                                .isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_STATUS_ENUM, InvoiceStatusEnum.WAIT_FOR_UPLOAD.name())
                            .endBracket()
                        .endBracket()
                        .or()
                        .startBracket()
                            .isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_STATUS_ENUM, InvoiceStatusEnum.READY.name())
                            .and()
                            .isIn(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.VAR_PAYER_ID, userContactIds)
                            .and()
                            .startBracket()
                                .isNull(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.VAR_COST_DISTRIBUTION_ITEM_CORRECTION_STATUS)
                                .or()
                                .isEqual(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.VAR_COST_DISTRIBUTION_ITEM_CORRECTION_STATUS, CorrectionStatus.CHECK.name())
                                .or()
                                .isEqual(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.VAR_COST_DISTRIBUTION_ITEM_CORRECTION_STATUS, CorrectionStatus.PROBLEM.name())
                            .endBracket()
                        .endBracket()
                        .or()
                        .startBracket()
                            .isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_STATUS_ENUM, InvoiceStatusEnum.READY.name())
                            .and()
                            .startBracket()
                                .isIn(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_PAYER_ID, userContactIds)
                                .or()
                                .isIn(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_PAYMENT_RECIPIENT_ID, userContactIds)
                            .endBracket()
                            .and()
                            .startBracket()
                                .isNull(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_CORRECTION_STATUS)
                                .or()
                                .isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_CORRECTION_STATUS, CorrectionStatus.CHECK.name())
                                .or()
                                .isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_CORRECTION_STATUS, CorrectionStatus.PROBLEM.name())
                            .endBracket()
                        .endBracket()
                    .endBracket()
                    .and();

                SearchFilter searchFilter = SearchHelper.searchAndUseSearchConfiguration(getContext(), true, sqlBuilder);
                List<Invoice> invoices = SearchHelper.invoicesInInvoiceCostDistributionItems(searchFilter.getInvoiceCostDistributionItemList());

                Collections.sort(invoices, new InvoiceStatusComparator());


                bindingData.swapInvoiceList(invoices);
                bindingData.notifyDataSetChanged();

                IFragmentToActivity iFragmentToActivity = (IFragmentToActivity) getActivity();
                if (iFragmentToActivity != null){
                    iFragmentToActivity.refreshTabCounter(invoices.size(), null);
                    iFragmentToActivity.filterText(searchFilter.isShowSearchString(), searchFilter.getSearchString());

                    if (!invoices.isEmpty()){
                        iFragmentToActivity.whichTabToShow(true, null, autoTabShow);
                    }else{
                        iFragmentToActivity.whichTabToShow(false, null, autoTabShow);
                    }
                }

            }
        }, 100);
    }
}
