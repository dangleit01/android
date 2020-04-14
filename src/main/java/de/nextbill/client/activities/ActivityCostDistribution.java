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
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import de.nextbill.client.R;
import de.nextbill.client.adapters.AdapterActivityCostDistributionListView;
import de.nextbill.client.comparators.CostDistributionPositionComparator;
import de.nextbill.client.comparators.PaymentPersonComparator;
import de.nextbill.client.database.IOHelper;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.enums.BasicStatusEnum;
import de.nextbill.client.enums.CorrectionStatus;
import de.nextbill.client.enums.CostDistributionItemTypeEnum;
import de.nextbill.client.enums.InvoiceDetailsPaymentPersonDialogEnum;
import de.nextbill.client.enums.MainFunctionEnum;
import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.helper.CostDistributionHelper;
import de.nextbill.client.helper.LoginUserHelper;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.ArticleDTO;
import de.nextbill.client.model.BusinessPartner;
import de.nextbill.client.model.CostDistribution;
import de.nextbill.client.model.CostDistributionItem;
import de.nextbill.client.model.IPaymentPerson;
import de.nextbill.client.model.UserContact;
import de.nextbill.client.network.RequestUpdateService;
import de.nextbill.client.utils.SqlBuilder;

public class ActivityCostDistribution extends AppCompatActivity implements DialogPaymentPerson.SelectPaymentPersonDialogListener, DialogCostDistributionItem.EditCostDistributionDetailsDialogListener, DialogName.SelectNameDialogListener,
        DialogSumInput.EditSumDialogListener{

    static int ARTICLE_SELECT = 6;

    private ProgressBar progressBar;
    private TextView currentCostTv;
    private TextView maxCostTv;
    private ListView costDistributionLv;

    private List<CostDistributionItem> costDistributionItems;
    private List<CostDistributionItem> costDistributionItemsToDelete;
    private AdapterActivityCostDistributionListView bindingData;

    private BigDecimal sum;
    private MainFunctionEnum mainFunctionEnum;
    private boolean showArticleSelect = false;
    private UUID invoiceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cost_distribution);

        if (getIntent().getStringExtra("sum") == null){
            return;
        }

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());

        BigDecimal currentAmount = new BigDecimal(0);
        if (getIntent().getStringExtra("sum") != null){
            currentAmount = new BigDecimal(getIntent().getStringExtra("sum"));
        }
        sum = currentAmount;

        if (getIntent().getBooleanExtra("showArticleSelect", false)){
            showArticleSelect = getIntent().getBooleanExtra("showArticleSelect", false);
        }

        if (getIntent().getStringExtra("mainFunctionEnum") != null){
            mainFunctionEnum = MainFunctionEnum.valueOf(getIntent().getStringExtra("mainFunctionEnum"));
        }else{
            mainFunctionEnum = MainFunctionEnum.EXPENSE;
        }

        if (getIntent().getStringExtra("invoiceId") != null){
            invoiceId = UUID.fromString(getIntent().getStringExtra("invoiceId"));
        }

        costDistributionItemsToDelete = new ArrayList<>();

        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        Type listType = new TypeToken<List<CostDistributionItem>>() {
        }.getType();
        costDistributionItems = gson.fromJson(getIntent().getStringExtra("costDistributionItems"), listType);

        Collections.sort(costDistributionItems, new CostDistributionPositionComparator());

        View.OnClickListener deleteButtonListener = new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                int position = (Integer) v.getTag();
                costDistributionItems.remove(position);
                bindingData.swapCostDistributionCollection(costDistributionItems);

                refreshCurrentState();
            }
        };

        View.OnClickListener articleSelectButtonListener = new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Integer position = (Integer) v.getTag();
                CostDistributionItem costDistributionItem = costDistributionItems.get(position);

                showActivityArticlesSelect(costDistributionItem);

                bindingData.notifyDataSetChanged();
            }
        };

        View.OnClickListener onMenuItemClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                final Integer position = (Integer) view.getTag();
                CostDistributionItem costDistributionItem= costDistributionItems.get(position);
                showActivityArticlesSelect(costDistributionItem);

                bindingData.notifyDataSetChanged();
            }
        };

        bindingData = new AdapterActivityCostDistributionListView(this, costDistributionItems, currentAmount, deleteButtonListener, articleSelectButtonListener, onMenuItemClickListener, showArticleSelect);

        costDistributionLv = (ListView) findViewById(R.id.costDistributionLv);

        costDistributionLv.setAdapter(bindingData);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        currentCostTv = (TextView) findViewById(R.id.costDistributionCurrentCost);
        maxCostTv = (TextView) findViewById(R.id.costDistributionMaxCost);

        costDistributionLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            if (costDistributionItems.get(i) != null) {
                showCostDistributionDetailsDialog(costDistributionItems.get(i), String.valueOf(i));
            }

            }
        });

        refreshCurrentState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IOHelper.getInstance().setCtx(this);
    }

    @Override
    public void onStart(){
        super.onStart();
    }

    public void refreshCurrentState(){

//        sum = newInvoice.getSumOfInvoice();
        maxCostTv.setText(sum.setScale(2, RoundingMode.HALF_EVEN) + "€");

        boolean hasRestType = false;
        for (CostDistributionItem tmpCostDistributionItem : costDistributionItems) {
            if (tmpCostDistributionItem.getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.REST)){
                hasRestType = true;
                break;
            }
        }

        if (!hasRestType){

            BigDecimal restOfAllCostDistributions = CostDistributionHelper.getCalculatedRestSumPrecise(sum, costDistributionItems);
            if (!costDistributionItems.isEmpty()){
                BigDecimal percentOfAllCostDistributions = null;
                if (sum.compareTo(new BigDecimal(0)) != 0){
                    percentOfAllCostDistributions = restOfAllCostDistributions.divide(sum, 2, RoundingMode.HALF_EVEN).multiply(new BigDecimal(100));
                }else{
                    percentOfAllCostDistributions = new BigDecimal(100);
                }
                BigDecimal percentOfAllCostDistributionsNegated = new BigDecimal(100).subtract(percentOfAllCostDistributions);
                progressBar.setProgress(percentOfAllCostDistributionsNegated.intValue());
                currentCostTv.setText("Offen: " + restOfAllCostDistributions.setScale(2, RoundingMode.HALF_EVEN) + "€");
            }else{
                progressBar.setProgress(0);
                currentCostTv.setText("Offen: " + sum.setScale(2, RoundingMode.HALF_EVEN) + "€");
            }

        }else{
            progressBar.setProgress(100);
            currentCostTv.setText("Offen: 0€");
        }

        notifyProgressBar();

    }

    public void notifyProgressBar() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Integer progress = progressBar.getProgress();
                progressBar.setProgress(progress);
            }
        });
    }

    @Override
    public void onFinishPaymentPersonSelectDialog(InvoiceDetailsPaymentPersonDialogEnum invoiceDetailsPaymentPersonDialogEnum, IPaymentPerson paymentPerson) {

        if (paymentPerson != null) {
            CostDistributionItem costDistributionItem = new CostDistributionItem();
            costDistributionItem.setCostDistributionItemTypeEnum(CostDistributionItemTypeEnum.QUOTA);
            costDistributionItem.setValue(new BigDecimal(1).setScale(1, RoundingMode.HALF_EVEN));
            costDistributionItem.setCostPaid(new BigDecimal(0));
            costDistributionItem.setPayerId(paymentPerson.getPaymentPersonId());
            costDistributionItem.setPaymentPersonName(paymentPerson.getPaymentPersonName());
            costDistributionItem.setPaymentPersonTypeEnum(paymentPerson.getPaymentPersonEnum());
            costDistributionItem.setPosition(costDistributionItems.size());
            costDistributionItem.setInvoiceId(invoiceId);

            if (mainFunctionEnum.equals(MainFunctionEnum.EXPENSE) || mainFunctionEnum.equals(MainFunctionEnum.NEW_EXPENSE)){
                if (paymentPerson.getVirtualPaymentPersonEnum().equals(PaymentPersonTypeEnum.USER)) {
                    costDistributionItem.setCorrectionStatus(CorrectionStatus.CHECK);
                }else if (paymentPerson.getVirtualPaymentPersonEnum().equals(PaymentPersonTypeEnum.CONTACT)){
                    costDistributionItem.setCorrectionStatus(CorrectionStatus.READY);
                }else{
                    costDistributionItem.setCorrectionStatus(CorrectionStatus.IGNORE);
                }
            }else{
                costDistributionItem.setCorrectionStatus(CorrectionStatus.READY);
            }

            costDistributionItems.add(costDistributionItem);
            refreshCurrentState();
        }

        bindingData.notifyDataSetChanged();
    }

    public void showCostDistributionDetailsDialog(CostDistributionItem costDistributionItem, String id){
        if (sum.compareTo(new BigDecimal(0)) == 0){
            Toast.makeText(getApplicationContext(), "Summe nicht aufteilbar!", Toast.LENGTH_LONG).show();
            return;
        }

        FragmentManager fm = getSupportFragmentManager();
        DialogCostDistributionItem editCostDistributionDialog = new DialogCostDistributionItem();

        refreshCurrentState();

        editCostDistributionDialog.setArguments(costDistributionItem.getPayer(this.getApplicationContext()), costDistributionItem, costDistributionItems, sum, id);

        editCostDistributionDialog.show(fm, "dialog_cost_distribution");
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.invoice_details_cost_distribution, menu);
        return true;
    }

    public void showAddPaymentPersonDialog(){
        FragmentManager fm = getSupportFragmentManager();
        DialogPaymentPerson editPayerDialog = new DialogPaymentPerson();
        ArrayList<PaymentPersonTypeEnum> paymentPersonTypeEnumList = new ArrayList<PaymentPersonTypeEnum>();
        paymentPersonTypeEnumList.add(PaymentPersonTypeEnum.NEW);
        paymentPersonTypeEnumList.add(PaymentPersonTypeEnum.USER);
        paymentPersonTypeEnumList.add(PaymentPersonTypeEnum.BUSINESS_PARTNER);
        paymentPersonTypeEnumList.add(PaymentPersonTypeEnum.CONTACT);
        paymentPersonTypeEnumList.add(PaymentPersonTypeEnum.PROJECT);

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());

        List<IPaymentPerson> appUsers = new ArrayList<>();

        appUsers.add(currentUser);
        SqlBuilder sqlBuilder2 = new SqlBuilder(MainDatabaseHandler.TABLE_USER_CONTACT);
        sqlBuilder2
                .isNotNull(MainDatabaseHandler.VAR_APP_USER_CONTACT_ID)
                .and()
                .isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_STATUS_ENUM, BasicStatusEnum.OK.name());

        List<UserContact> userContacts = mainDatabaseHandler.findUserContactsSql(sqlBuilder2);
        appUsers.addAll(userContacts);
        Collections.sort(appUsers, new PaymentPersonComparator());

        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_USER_CONTACT);
        sqlBuilder
                .startBracket()
                .isNull(MainDatabaseHandler.VAR_IS_PROJECT)
                .or()
                .isEqual(MainDatabaseHandler.VAR_IS_PROJECT, "0")
                .endBracket()
                .and()
                .isNull(MainDatabaseHandler.VAR_APP_USER_CONTACT_ID)
                .and()
                .isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_STATUS_ENUM, BasicStatusEnum.OK.name());

        List<IPaymentPerson> userContactsAndProjects = new ArrayList<>();
        userContactsAndProjects.addAll(mainDatabaseHandler.findUserContactsSql(sqlBuilder));
        Collections.sort(userContactsAndProjects, new PaymentPersonComparator());

        sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_USER_CONTACT);
        sqlBuilder
                .isEqual(MainDatabaseHandler.VAR_IS_PROJECT, "1")
                .and()
                .isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString())
                .and()
                .isNull(MainDatabaseHandler.VAR_APP_USER_CONTACT_ID)
                .and()
                .isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_STATUS_ENUM, BasicStatusEnum.OK.name());

        List<IPaymentPerson> projects = new ArrayList<>();
        projects.addAll(mainDatabaseHandler.findUserContactsSql(sqlBuilder));
        Collections.sort(projects, new PaymentPersonComparator());
        userContactsAndProjects.addAll(projects);

        editPayerDialog.setArguments(InvoiceDetailsPaymentPersonDialogEnum.PAYER, paymentPersonTypeEnumList, new ArrayList<BusinessPartner>(), appUsers, userContactsAndProjects, true, true, false);

        editPayerDialog.show(fm, "dialog_payment_person");
    }

    public void showActivityArticlesSelect(CostDistributionItem costDistributionItem){

        StatusDatabaseHandler statusDatabaseHandler = StatusDatabaseHandler.getInstance(getApplicationContext());
        int status = statusDatabaseHandler.getStatus();
        if (status == StatusDatabaseHandler.STATUS_MANUAL_OFFLINE) {
            Toast.makeText(getApplicationContext(), "Offline-Modus aktiv! Funktion nicht ausführbar.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(ActivityCostDistribution.this, ActivityArticlesSelect.class);

        intent.putExtra("payer", costDistributionItem.getPaymentPersonName(getApplicationContext()));
        intent.putExtra("paymentItemId", costDistributionItem.getCostDistributionItemId().toString());
        intent.putExtra("paymentItemClazz", CostDistributionItem.class.getSimpleName());
        intent.putExtra("invoiceId", invoiceId.toString());

        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        Type listType = new TypeToken<List<ArticleDTO>>() {}.getType();

        String articleDTOsAsJson = gson.toJson(costDistributionItem.getArticleDTOs(), listType);
        intent.putExtra("articleDTOS", articleDTOsAsJson);

        startActivityForResult(intent, ARTICLE_SELECT);
        overridePendingTransition(R.anim.activity_in1, R.anim.activity_in2);
    }

    public void showAddCostDistributionDialog(){
        FragmentManager fm = getSupportFragmentManager();
        DialogName editCostDistributionNameDialog = new DialogName();
        editCostDistributionNameDialog.show(fm, "dialog_cost_distribution_name");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == ARTICLE_SELECT && resultCode == RESULT_OK) {

            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();

            Type listType = new TypeToken<List<ArticleDTO>>() {
            }.getType();

            List<ArticleDTO> articleDTOS = gson.fromJson(intent.getStringExtra("articleDTOS"), listType);

            String costDistributionIdFound = null;
            if (intent.getStringExtra("paymentItemClass").equals(CostDistributionItem.class.getSimpleName())) {

                costDistributionIdFound = intent.getStringExtra("paymentItemId");
                BigDecimal sumReturned = BigDecimal.valueOf(intent.getDoubleExtra("sum", 0));

                for (CostDistributionItem costDistributionItem : costDistributionItems) {
                    if (costDistributionItem.getCostDistributionItemId().toString().equals(costDistributionIdFound)) {

                        costDistributionItem.setMoneyValue(sumReturned);
                        costDistributionItem.setValue(sumReturned);
                        costDistributionItem.setCostDistributionItemTypeEnum(CostDistributionItemTypeEnum.FIXED_AMOUNT);
                        costDistributionItem.setArticleDTOs(articleDTOS);
                        break;
                    }
                }

            }

            for (CostDistributionItem costDistributionItem : costDistributionItems) {
                if (!costDistributionItem.getCostDistributionItemId().toString().equals(costDistributionIdFound)) {
                    costDistributionItem.setMoneyValue(CostDistributionHelper.calculateAmountForCostDistributionItemPrecise(costDistributionItem, costDistributionItems, sum));
                }
            }

            refreshCurrentState();
            bindingData.notifyDataSetChanged();
        }
    }

    @Override
    public void onBackPressed(){

        Intent in = getIntent();

        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        String costDistributionItemsAsString = gson.toJson(costDistributionItems);

        in.putExtra("costDistributionItemsResult", costDistributionItemsAsString);

        setResult(RESULT_OK, in);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.addPersonMenuButton:
                showAddPaymentPersonDialog();
                break;
            case R.id.addToCostDistribution:
                showAddCostDistributionDialog();
                break;
            default:
                onBackPressed();
        }

        return true;
    }

    @Override
    public void onFinishCostDistributionDetailsDialog(CostDistributionItem costDistributionItem, String id) {
        if (costDistributionItem != null){
            Integer idToInt = Integer.valueOf(id);
            costDistributionItems.set(idToInt, costDistributionItem);

            refreshCurrentState();
        }

        bindingData.notifyDataSetChanged();
    }

    @Override
    public void onFinishNameDialog(String name) {

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());

        CostDistribution costDistribution = new CostDistribution();
        AppUser appUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());
        costDistribution.setCreatedById(appUser.getAppUserId());
        costDistribution.setName(name);
        costDistribution.setCreatedById(LoginUserHelper.currentLoggedInUser(getApplicationContext()).getAppUserId());
        mainDatabaseHandler.insertCostDistribution(costDistribution);

        handler.addObject(costDistribution.getCostDistributionId().toString(), StatusDatabaseHandler.OBJECT_TYPE_COST_DISTRIBUTION, StatusDatabaseHandler.UPDATE_STATUS_ADD, new Date().getTime(), 1);

        for (CostDistributionItem costDistributionItem : costDistributionItems) {
            CostDistributionItem costDistributionItem1 = CostDistributionHelper.prepareItemForCostDistribution(costDistributionItem, costDistribution);
            costDistributionItem1.setInvoiceId(null);
            costDistributionItem1.setPosition(costDistributionItems.indexOf(costDistributionItem));
            mainDatabaseHandler.insertCostDistributionItem(costDistributionItem1);

            handler.addObject(costDistributionItem1.getCostDistributionItemId().toString(), StatusDatabaseHandler.OBJECT_TYPE_COST_DISTRIBUTION_ITEM, StatusDatabaseHandler.UPDATE_STATUS_ADD, new Date().getTime(), 1);
        }

        Intent sIntent = new Intent(getApplicationContext(), RequestUpdateService.class);
        sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
        getApplicationContext().startService(sIntent);

        Toast.makeText(getApplicationContext(), "Kostenverteilung als Vorlage gespeichert!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFinishSumEditDialog(BigDecimal currentSum, Integer itemIdentifier) {
        CostDistributionItem costDistributionItem = costDistributionItems.get(itemIdentifier);
        if (currentSum != null){
            costDistributionItem.setCostPaid(currentSum);
        }
    }
}
