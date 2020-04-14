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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.nextbill.client.R;
import de.nextbill.client.adapters.AdapterFragmentReadyInvoicesListView;
import de.nextbill.client.comparators.InvoiceTimeComparator;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.enums.BasicDataSubType;
import de.nextbill.client.enums.CorrectionStatus;
import de.nextbill.client.enums.InvoiceStatusEnum;
import de.nextbill.client.helper.CostDistributionHelper;
import de.nextbill.client.helper.LoginUserHelper;
import de.nextbill.client.helper.SearchHelper;
import de.nextbill.client.interfaces.EndlessScrollListener;
import de.nextbill.client.interfaces.IFragmentToActivity;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.BasicData;
import de.nextbill.client.model.Invoice;
import de.nextbill.client.network.NetworkUtils;
import de.nextbill.client.network.RequestUpdateService;
import de.nextbill.client.pojos.SearchFilter;
import de.nextbill.client.utils.SqlBuilder;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import static de.nextbill.client.helper.SearchHelper.findBasicDataValue;


public class ActivityMainTabReadyInvoices extends Fragment {

    private SwipeRefreshLayout swipeContainer;

    StickyListHeadersListView list;

    static int INVOICE_DETAILS = 2;

    AdapterFragmentReadyInvoicesListView bindingData;

    Boolean autoTabShow = true;

//    private Date currentScrollDate = null;

    boolean showMoreMonths = false;
    private int currentAmountMonths = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_main_fragment_ready_invoices, container, false);

        bindingData = new AdapterFragmentReadyInvoicesListView(this.getActivity());

        list = (StickyListHeadersListView) view.findViewById(R.id.list);

        list.setAdapter(bindingData);

        list.setOnScrollListener(new EndlessScrollListener() {
            @Override
            public boolean onLoadMore(int page, int totalItemsCount) {

                currentAmountMonths = currentAmountMonths+1;

                showMoreMonths = true;
                refreshInvoiceList(false, false);

                return true;
            }
        });

        list.setSelector(R.drawable.activity_main_list_selector);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (!bindingData.getInvoiceList().get(position).getInvoiceStatusEnum().equals(InvoiceStatusEnum.ANALYZING) &&
                        !bindingData.getInvoiceList().get(position).getInvoiceStatusEnum().equals(InvoiceStatusEnum.WAIT_FOR_UPLOAD)) {
                    Intent intent = new Intent(getActivity().getApplicationContext(), ActivityInvoiceDetails.class);
                    intent.putExtra("idInvoice", bindingData.getInvoiceList().get(position).getInvoiceId().toString());
                    getActivity().startActivityForResult(intent, INVOICE_DETAILS);
                    getActivity().overridePendingTransition(R.anim.activity_in1, R.anim.activity_in2);
                }
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
//            currentScrollDate = null;
            currentAmountMonths = 1;

            list.destroyDrawingCache();
            bindingData.swapInvoiceList(new ArrayList<Invoice>());
            bindingData.notifyDataSetChanged();
            IFragmentToActivity iFragmentToActivity = (IFragmentToActivity) getActivity();
            iFragmentToActivity.refreshTabCounter(null, 0);
        }
    }

    public void refreshInvoiceList(boolean autoTabShowTmp){
        refreshInvoiceList(autoTabShowTmp, true);
    }

    public void refreshInvoiceList(boolean autoTabShowTmp, final Boolean useDelay){

        AppUser appUser = LoginUserHelper.currentLoggedInUser(getContext());
        if (appUser == null){
            return;
        }

        this.autoTabShow = autoTabShowTmp;

        if (useDelay){
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    new RefreshInvoiceListTask().execute();
                }
            }, 100);
        }else{
            new RefreshInvoiceListTask().execute();
        }
    }

//    public void resetSearchAmount(){
//        currentScrollDate = null;
//    }

    public void resetSearchAmount(){
        currentAmountMonths = 1;
    }

    public class RefreshInvoiceListTask extends AsyncTask<Object, Integer, List<Object>> {

        AppUser currentUser;
        List<String> userContactIds;
        boolean showMoreMonthsInternal;

        @Override
        protected List<Object> doInBackground(Object... objects) {

            currentUser = LoginUserHelper.currentLoggedInUser(getContext());
            userContactIds = LoginUserHelper.idsOfUserContactsWithCurrentUser(getContext());

            showMoreMonthsInternal = showMoreMonths;
            showMoreMonths = false;

            List<Invoice> resultInvoices = new ArrayList<>();
            Map<UUID, BigDecimal> sumOfInvoicesMap = new HashMap<>();
            if (showMoreMonthsInternal){
                resultInvoices.addAll(bindingData.getInvoiceList());
                sumOfInvoicesMap.putAll(bindingData.getInvoiceSumMap());
            }

            SearchFilter searchFilter = null;
            if (currentAmountMonths == 1){
                for (int i = -4; i < 48; i++){
                    currentAmountMonths = i;

                    Calendar month = Calendar.getInstance();
                    month.add(Calendar.MONTH, -1 * (i -1));
                    month.set(Calendar.DAY_OF_MONTH, 1);
                    month.set(Calendar.HOUR_OF_DAY, 0);
                    month.set(Calendar.MINUTE, 0);
                    month.set(Calendar.SECOND, 0);
                    month.set(Calendar.MILLISECOND, 0);
                    Date firstDayDate = month.getTime();

                    Calendar monthEnd = Calendar.getInstance();
                    monthEnd.add(Calendar.MONTH, -1 * (i -1));
                    monthEnd.set(Calendar.DAY_OF_MONTH, monthEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
                    monthEnd.set(Calendar.HOUR_OF_DAY, 23);
                    monthEnd.set(Calendar.MINUTE, 59);
                    monthEnd.set(Calendar.SECOND, 59);
                    monthEnd.set(Calendar.MILLISECOND, 999);

                    Date lastDayDate = monthEnd.getTime();

                    if (isEntryInDateRange(firstDayDate, lastDayDate) == 0) {
                        continue;
                    }

                    List<Object> resultObjects = searchForDateRange(firstDayDate, lastDayDate);

                    List<Invoice> resultInvoicesTmp = (List<Invoice>) resultObjects.get(0);
                    Map<UUID, BigDecimal> sumOfInvoicesMapTmp = sumOfInvoicesMap(resultInvoicesTmp, currentUser);
                    searchFilter = (SearchFilter) resultObjects.get(1);

                    resultInvoices.addAll(resultInvoicesTmp);
                    sumOfInvoicesMap.putAll(sumOfInvoicesMapTmp);

                    if (resultInvoices.size() > 15){
                        break;
                    }else{
                        showMoreMonthsInternal = true;
                    }
                }
            }else{
                Calendar month = Calendar.getInstance();
                month.add(Calendar.MONTH, -1 * (currentAmountMonths -1));
                month.set(Calendar.DAY_OF_MONTH, 1);
                month.set(Calendar.HOUR_OF_DAY, 0);
                month.set(Calendar.MINUTE, 0);
                month.set(Calendar.SECOND, 0);
                month.set(Calendar.MILLISECOND, 0);
                Date firstDayDate = month.getTime();

                Calendar monthEnd = Calendar.getInstance();
                if (showMoreMonthsInternal){
                    monthEnd.add(Calendar.MONTH, -1 * (currentAmountMonths -1));
                    monthEnd.set(Calendar.DAY_OF_MONTH, monthEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
                    monthEnd.set(Calendar.HOUR_OF_DAY, 23);
                    monthEnd.set(Calendar.MINUTE, 59);
                    monthEnd.set(Calendar.SECOND, 59);
                    monthEnd.set(Calendar.MILLISECOND, 999);
                }
                Date lastDayDate = monthEnd.getTime();

                List<Object> resultObjects = searchForDateRange(firstDayDate, lastDayDate);

                List<Invoice> resultInvoicesTmp = (List<Invoice>) resultObjects.get(0);
                Map<UUID, BigDecimal> sumOfInvoicesMapTmp = sumOfInvoicesMap(resultInvoicesTmp, currentUser);
                searchFilter = (SearchFilter) resultObjects.get(1);

                resultInvoices.addAll(resultInvoicesTmp);
                sumOfInvoicesMap.putAll(sumOfInvoicesMapTmp);
            }

            List<Object> results = new ArrayList<>();
            results.add(resultInvoices);
            results.add(searchFilter);
            results.add(sumOfInvoicesMap);

            return results;
        }

        public Date firstDayOfMonth(Date date) {
            Calendar month = Calendar.getInstance();
            month.setTime(date);
            month.set(Calendar.DAY_OF_MONTH, 1);
            month.set(Calendar.HOUR_OF_DAY, 0);
            month.set(Calendar.MINUTE, 0);
            month.set(Calendar.SECOND, 0);
            month.set(Calendar.MILLISECOND, 0);
            Date firstDayDate = month.getTime();

            return firstDayDate;
        }

        public Date lastDayOfMonth(Date date) {
            Calendar month = Calendar.getInstance();
            month.setTime(date);
            month.set(Calendar.DAY_OF_MONTH, month.getActualMaximum(Calendar.DAY_OF_MONTH));
            month.set(Calendar.HOUR_OF_DAY, 23);
            month.set(Calendar.MINUTE, 59);
            month.set(Calendar.SECOND, 59);
            month.set(Calendar.MILLISECOND, 999);
            Date lastDayOfMonth = month.getTime();

            return lastDayOfMonth;
        }

        public Map<UUID, BigDecimal> sumOfInvoicesMap(List<Invoice> invoices, AppUser currentUser){
            Map<UUID, BigDecimal> sumMap = new HashMap<>();
            for (Invoice invoice : invoices) {
                sumMap.put(invoice.getInvoiceId(), CostDistributionHelper.invoiceCostForPaymentPerson(getContext(), invoice, currentUser));
            }

            return sumMap;
        }

        protected Integer isEntryInDateRange(Date start, Date endDate){

            Map<String, String> tableAliasMap = new HashMap<>();
            tableAliasMap.put(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.TABLE_INVOICE);
            tableAliasMap.put(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM);

            SqlBuilder sqlBuilder = new SqlBuilder("select count(" + MainDatabaseHandler.TABLE_INVOICE + "." + MainDatabaseHandler.VAR_INVOICE_ID + ") ", tableAliasMap);
            sqlBuilder = buildQuery(start, endDate, sqlBuilder);

            SqlBuilder sqlBuilderCopy = sqlBuilder.copy();

            MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getContext());
            return mainDatabaseHandler.countQuery(sqlBuilderCopy);
        }

        protected List<Object> searchForDateRange(Date startDate,Date lastDate){

            SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.TABLE_INVOICE);
            sqlBuilder.addTable(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM);
            sqlBuilder = buildQuery(startDate, lastDate, sqlBuilder);
            sqlBuilder = sqlBuilder.and();

            SqlBuilder sqlBuilderCopy = sqlBuilder.copy();

            SearchFilter searchFilter = SearchHelper.searchAndUseSearchConfiguration(getContext(), true, sqlBuilderCopy, null);
            List<Invoice> invoicesTmp = SearchHelper.invoicesInInvoiceCostDistributionItems(searchFilter.getInvoiceCostDistributionItemList());

            Collections.sort(invoicesTmp, new InvoiceTimeComparator());

            List<Object> results = new ArrayList<>();
            results.add(invoicesTmp);
            results.add(searchFilter);

            return results;
        }

        private SqlBuilder buildQuery(Date startDate, Date lastDate, SqlBuilder sqlBuilder) {
            sqlBuilder
                .startBracket()
                    .isAfterThisDate(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_DATE_OF_INVOICE, startDate)
                    .and()
                    .isBeforeThisDate(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_DATE_OF_INVOICE, lastDate)
                    .and()
                    .startBracket()
                        .isNotEqualFields(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_ID, MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_STANDING_ORDER_INVOICE_TEMPLATE_ID)
                        .or()
                        .isNull(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_STANDING_ORDER_INVOICE_TEMPLATE_ID)
                    .endBracket()
                    .and()
                    .startBracket()
                        .startBracket()
                            .isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_CREATED_BY_ID, currentUser.getAppUserId().toString())
                            .and()
                            .isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_STATUS_ENUM, InvoiceStatusEnum.READY.name())
                        .endBracket()
                        .or()
                        .startBracket()
                            .isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_STATUS_ENUM, InvoiceStatusEnum.READY.name())
                            .and()
                            .isIn(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.VAR_PAYER_ID, userContactIds)
                            .and()
                            .startBracket()
                                .isEqual(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.VAR_COST_DISTRIBUTION_ITEM_CORRECTION_STATUS, CorrectionStatus.READY.name())
                                .or()
                                .isEqual(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.VAR_COST_DISTRIBUTION_ITEM_CORRECTION_STATUS, CorrectionStatus.IGNORE.name())
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
                                .isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_CORRECTION_STATUS, CorrectionStatus.READY.name())
                                .or()
                                .isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_CORRECTION_STATUS, CorrectionStatus.IGNORE.name())
                            .endBracket()
                        .endBracket()
                    .endBracket()
                .endBracket();
            return sqlBuilder;
        }

        @Override
        protected void onPostExecute(List<Object> objects) {

            List<Invoice> resultInvoices = (List<Invoice>) objects.get(0);
            Map<UUID, BigDecimal> sumInvoicesMap = (Map<UUID, BigDecimal>) objects.get(2);

            IFragmentToActivity iFragmentToActivity = (IFragmentToActivity) getActivity();

            SearchFilter searchFilter = (SearchFilter) objects.get(1);
            if (iFragmentToActivity != null){
                if (searchFilter != null){
                    iFragmentToActivity.filterText(searchFilter.isShowSearchString(), searchFilter.getSearchString());
                }else{
                    iFragmentToActivity.filterText(false, "");
                }
            }

            bindingData.swapInvoiceList(resultInvoices, sumInvoicesMap);

            if (iFragmentToActivity != null){
                iFragmentToActivity.refreshTabCounter(null, resultInvoices.size());

                if (!resultInvoices.isEmpty()){
                    iFragmentToActivity.whichTabToShow(null, true, autoTabShow);
                }else{
                    iFragmentToActivity.whichTabToShow(null, false, autoTabShow);
                }
            }
        }
    }
}
