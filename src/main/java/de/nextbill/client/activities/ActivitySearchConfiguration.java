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
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import de.nextbill.client.R;
import de.nextbill.client.comparators.BusinessPartnerComparator;
import de.nextbill.client.comparators.InvoiceCategoryComparator;
import de.nextbill.client.comparators.PaymentPersonComparator;
import de.nextbill.client.database.IOHelper;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.enums.BasicDataSubType;
import de.nextbill.client.enums.BasicSelectionEnum;
import de.nextbill.client.enums.BasicStatusEnum;
import de.nextbill.client.enums.InvoiceDetailsPaymentPersonDialogEnum;
import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.enums.RepetitionTypeEnum;
import de.nextbill.client.helper.LoginUserHelper;
import de.nextbill.client.interfaces.CustomDateTextWatcher;
import de.nextbill.client.interfaces.DateTextWatcher;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.BasicData;
import de.nextbill.client.model.BusinessPartner;
import de.nextbill.client.model.CostDistribution;
import de.nextbill.client.model.IPaymentPerson;
import de.nextbill.client.model.InvoiceCategory;
import de.nextbill.client.model.UserContact;
import de.nextbill.client.utils.SqlBuilder;

public class ActivitySearchConfiguration extends AppCompatActivity implements DialogPaymentPerson.SelectPaymentPersonDialogListener, DialogInvoiceCategory.SelectInvoiceCategoryDialogListener, DialogCostDistribution.SelectCostDistributionListener{

    private List<RepetitionTypeEnum> repetitionTypeEnums = new ArrayList();
    private List<BasicSelectionEnum> basicSelectionEnums = new ArrayList();

    static int LOGIN = 22;

    private Button searchPayerBtn;
    private Button searchPaymentRecipientBtn;
    private Button searchCostDistributorCostsBtn;
    private Button searchCostDistributionBtn;
    private Button searchCategoryBtn;
    private EditText searchTempStartBtn;
    private EditText searchTempEndBtn;
    private Spinner searchSpecialTypeBtn;
    private Spinner searchRepetitionTypeBtn;
    private ToggleButton searchIncomeBtn;
    private ToggleButton searchExpenseBtn;

    private DateTextWatcher dateStartTextWatcher;
    private DateTextWatcher dateEndTextWatcher;

    private ImageView searchPayerDeleteBtn;
    private ImageView searchPaymentRecipientDeleteBtn;
    private ImageView searchCostDistributorCostsDeleteBtn;
    private ImageView searchCostDistributionDeleteBtn;
    private ImageView searchCategoryDeleteBtn;
    private ImageView searchTempDeleteBtn;
    private ImageView searchSpecialTypeDeleteBtn;
    private ImageView searchRepetitionTypeDeleteBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_configuration);
        this.setTitle("Suchfilter");

        searchPayerBtn = (Button) findViewById(R.id.searchPayerBtn);
        searchPaymentRecipientBtn =(Button) findViewById(R.id.searchPaymentRecipientBtn);
        searchCostDistributorCostsBtn =(Button) findViewById(R.id.searchCostDistributorCostsBtn);
        searchCostDistributionBtn =(Button) findViewById(R.id.searchCostDistributionBtn);
        searchCategoryBtn =(Button) findViewById(R.id.searchCategoryBtn);
        searchTempStartBtn =(EditText) findViewById(R.id.searchTempStartBtn);
        searchTempEndBtn =(EditText) findViewById(R.id.searchTempEndBtn);
        searchSpecialTypeBtn =(Spinner) findViewById(R.id.searchSpecialTypeBtn);
        searchRepetitionTypeBtn =(Spinner) findViewById(R.id.searchRepetitionTypeBtn);
        searchIncomeBtn =(ToggleButton) findViewById(R.id.searchIncomeBtn);
        searchExpenseBtn =(ToggleButton) findViewById(R.id.searchExpenseBtn);

        searchPayerDeleteBtn = (ImageView) findViewById(R.id.searchPayerDeleteBtn);
        searchPaymentRecipientDeleteBtn =(ImageView) findViewById(R.id.searchPaymentRecipientDeleteBtn);
        searchCostDistributorCostsDeleteBtn =(ImageView) findViewById(R.id.searchCostDistributorCostsDeleteBtn);
        searchCostDistributionDeleteBtn =(ImageView) findViewById(R.id.searchCostDistributionDeleteBtn);
        searchCategoryDeleteBtn =(ImageView) findViewById(R.id.searchCategoryDeleteBtn);
        searchTempDeleteBtn =(ImageView) findViewById(R.id.searchTempDeleteBtn);
        searchSpecialTypeDeleteBtn =(ImageView) findViewById(R.id.searchSpecialTypeDeleteBtn);
        searchRepetitionTypeDeleteBtn =(ImageView) findViewById(R.id.searchRepetitionTypeDeleteBtn);

        searchIncomeBtn.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked){
                    BasicData basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.INCOME);
                    if (basicData != null){
                        MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                    }
                }else{
                    BasicData basicData = MainDatabaseHandler.findBasicSearchDataOrCreate(getApplicationContext(), BasicDataSubType.INCOME);
                    MainDatabaseHandler.saveBasicSearchData(getApplicationContext(), basicData);
                }

                refreshViews();
            }
        });

        searchExpenseBtn.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked){
                    BasicData basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.EXPENSE);
                    if (basicData != null){
                        MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                    }
                }else{
                    BasicData basicData = MainDatabaseHandler.findBasicSearchDataOrCreate(getApplicationContext(), BasicDataSubType.EXPENSE);
                    MainDatabaseHandler.saveBasicSearchData(getApplicationContext(), basicData);
                }

                refreshViews();
            }
        });

        searchPayerDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BasicData basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.PAYER);
                if (basicData != null){
                    MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                }

                refreshViews();
            }
        });
        searchPaymentRecipientDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BasicData basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.PAYMENT_RECIPIENT);
                if (basicData != null){
                    MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                }

                refreshViews();
            }
        });
        searchCostDistributorCostsDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BasicData basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.COST_DISTRIBUTOR);
                if (basicData != null){
                    MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                }

                refreshViews();
            }
        });
        searchCostDistributionDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BasicData basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.COST_DISTRIBUTION);
                if (basicData != null){
                    MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                }

                refreshViews();
            }
        });
        searchCategoryDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BasicData basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.INVOICE_CATEGORY);
                if (basicData != null){
                    MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                }

                refreshViews();
            }
        });
        searchTempDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BasicData basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.DATE_START);
                if (basicData != null){
                    MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                }
                basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.DATE_END);
                if (basicData != null){
                    MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                }

                refreshViews();
            }
        });
        searchSpecialTypeDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BasicData basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.SPECIAL_TYPE);
                if (basicData != null){
                    MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                }

                refreshViews();
            }
        });
        searchRepetitionTypeDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BasicData basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.REPETITION_TYPE);
                if (basicData != null){
                    MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                }

                refreshViews();
            }
        });



        dateStartTextWatcher = new DateTextWatcher(searchTempStartBtn);
        dateEndTextWatcher = new DateTextWatcher(searchTempEndBtn);

        searchPayerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPaymentPersonDialog(InvoiceDetailsPaymentPersonDialogEnum.PAYER);
            }
        });

        searchPaymentRecipientBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPaymentPersonDialog(InvoiceDetailsPaymentPersonDialogEnum.PAYMENT_RECIPIENT);
            }
        });

        searchCostDistributorCostsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPaymentPersonDialog(InvoiceDetailsPaymentPersonDialogEnum.COST_DISTRIBUTOR);
            }
        });

        searchCostDistributionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCostDistributionSelectDialog();
            }
        });

        searchCategoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInvoiceCategorySelectDialog();
            }
        });

        repetitionTypeEnums.clear();
        repetitionTypeEnums.addAll(Arrays.asList(RepetitionTypeEnum.values()));
        ArrayAdapter<RepetitionTypeEnum> dataAdapter = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_spinner_item, repetitionTypeEnums);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        searchRepetitionTypeBtn.setAdapter(dataAdapter);

        searchRepetitionTypeBtn.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                RepetitionTypeEnum repetitionTypeEnum = repetitionTypeEnums.get(position);

                if (!RepetitionTypeEnum.NONE.equals(repetitionTypeEnum)){
                    BasicData basicData = MainDatabaseHandler.findBasicSearchDataOrCreate(getApplicationContext(), BasicDataSubType.REPETITION_TYPE);
                    basicData.setValue(repetitionTypeEnum.name());
                    MainDatabaseHandler.saveBasicSearchData(getApplicationContext(), basicData);
                }

                refreshViews();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        basicSelectionEnums.clear();
        basicSelectionEnums.addAll(Arrays.asList(BasicSelectionEnum.values()));
        ArrayAdapter<BasicSelectionEnum> dataAdapter2 = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_spinner_item, basicSelectionEnums);
        dataAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        searchSpecialTypeBtn.setAdapter(dataAdapter2);

        searchSpecialTypeBtn.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                BasicSelectionEnum basicSelectionEnum = basicSelectionEnums.get(position);

                if (!BasicSelectionEnum.NONE.equals(basicSelectionEnum)) {
                    BasicData basicData = MainDatabaseHandler.findBasicSearchDataOrCreate(getApplicationContext(), BasicDataSubType.SPECIAL_TYPE);
                    basicData.setValue(basicSelectionEnum.name());
                    MainDatabaseHandler.saveBasicSearchData(getApplicationContext(), basicData);
                }

                refreshViews();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        CustomDateTextWatcher dateStartTextWatcherCustom = new CustomDateTextWatcher() {

            @Override
            public void currentValue(Date currentValue){
                if (currentValue != null){
                    BasicData basicData = MainDatabaseHandler.findBasicSearchDataOrCreate(getApplicationContext(), BasicDataSubType.DATE_START);
                    basicData.setNumberValue(new BigDecimal(currentValue.getTime()));
                    MainDatabaseHandler.saveBasicSearchData(getApplicationContext(), basicData);
                }
            }

            public void afterTextChanged(Editable s) {

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        };
        dateStartTextWatcher.addCustomTextWatcher(dateStartTextWatcherCustom);

        CustomDateTextWatcher dateEndTextWatcherCustom = new CustomDateTextWatcher() {

            @Override
            public void currentValue(Date currentValue){
                if (currentValue != null){
                    BasicData basicData = MainDatabaseHandler.findBasicSearchDataOrCreate(getApplicationContext(), BasicDataSubType.DATE_END);
                    basicData.setNumberValue(new BigDecimal(currentValue.getTime()));
                    MainDatabaseHandler.saveBasicSearchData(getApplicationContext(), basicData);
                }
            }

            public void afterTextChanged(Editable s) {

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        };
        dateEndTextWatcher.addCustomTextWatcher(dateEndTextWatcherCustom);

        refreshViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IOHelper.getInstance().setCtx(this);
    }

    private void showCostDistributionSelectDialog() {

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
        List<CostDistribution> costDistributions = mainDatabaseHandler.findCostDistributions();

        FragmentManager fm = getSupportFragmentManager();
        DialogCostDistribution selectCostDistributionDialog = new DialogCostDistribution();

        selectCostDistributionDialog.setArguments(costDistributions);

        selectCostDistributionDialog.show(fm, "dialog_cost_distribution_select");

    }

    private void showPaymentPersonDialog(InvoiceDetailsPaymentPersonDialogEnum invoiceDetailsPaymentPersonDialogEnum) {

        FragmentManager fm = getSupportFragmentManager();
        DialogPaymentPerson editPayerDialog = new DialogPaymentPerson();
        ArrayList<PaymentPersonTypeEnum> paymentPersonTypeEnumList = new ArrayList<PaymentPersonTypeEnum>();
        paymentPersonTypeEnumList.add(PaymentPersonTypeEnum.USER);
        paymentPersonTypeEnumList.add(PaymentPersonTypeEnum.BUSINESS_PARTNER);
        paymentPersonTypeEnumList.add(PaymentPersonTypeEnum.CONTACT);
        paymentPersonTypeEnumList.add(PaymentPersonTypeEnum.PROJECT);

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());

        List<BusinessPartner> businessPartners = new ArrayList<>();

        boolean canAddBusinessPartner = false;

        SqlBuilder sqlBuilder2;
        if (InvoiceDetailsPaymentPersonDialogEnum.PAYER.equals(invoiceDetailsPaymentPersonDialogEnum) ||
                InvoiceDetailsPaymentPersonDialogEnum.PAYMENT_RECIPIENT.equals(invoiceDetailsPaymentPersonDialogEnum)){
            sqlBuilder2 = new SqlBuilder(MainDatabaseHandler.TABLE_BUSINESS_PARTNER);
            sqlBuilder2
                    .startBracket()
                    .isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString())
                    .or()
                    .isNull(MainDatabaseHandler.VAR_APP_USER_ID)
                    .endBracket()
                    .and()
                    .isEqual(MainDatabaseHandler.VAR_BASIC_STATUS_ENUM, BasicStatusEnum.OK.name());
            businessPartners = mainDatabaseHandler.findBusinessPartnersSql(sqlBuilder2);
            Collections.sort(businessPartners, new BusinessPartnerComparator());

            canAddBusinessPartner = true;
        }

        List<IPaymentPerson> appUsers = new ArrayList<>();

        sqlBuilder2 = new SqlBuilder(MainDatabaseHandler.TABLE_USER_CONTACT);
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

        List<IPaymentPerson> userContacts2 = new ArrayList<>();
        userContacts2.addAll(mainDatabaseHandler.findUserContactsSql(sqlBuilder));
        Collections.sort(userContacts2, new PaymentPersonComparator());

        sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_USER_CONTACT);
        sqlBuilder
                .isEqual(MainDatabaseHandler.VAR_IS_PROJECT, "1")
                .and()
                .isNull(MainDatabaseHandler.VAR_APP_USER_CONTACT_ID)
                .and()
                .isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_STATUS_ENUM, BasicStatusEnum.OK.name());

        List<IPaymentPerson> projects = new ArrayList<>();
        projects.addAll(mainDatabaseHandler.findUserContactsSql(sqlBuilder));
        Collections.sort(projects, new PaymentPersonComparator());
        userContacts2.addAll(projects);

        editPayerDialog.setArguments(invoiceDetailsPaymentPersonDialogEnum, paymentPersonTypeEnumList, businessPartners, appUsers, userContacts2, true, true, canAddBusinessPartner );

        editPayerDialog.show(fm, "dialog_payment_person");
    }

    private void showInvoiceCategorySelectDialog() {

        List<InvoiceCategory> invoiceCategories = new ArrayList<>();

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE_CATEGORY);
        sqlBuilder.
                isNull(MainDatabaseHandler.VAR_PARENT_INVOICE_CATEGORY_ID).
                and().
                isEqual(MainDatabaseHandler.VAR_BASIC_STATUS_ENUM, BasicStatusEnum.OK.name());
        List<InvoiceCategory> titleInvoiceCategories = mainDatabaseHandler.findInvoiceCategoriesSql(sqlBuilder);
        Collections.sort(titleInvoiceCategories, new InvoiceCategoryComparator());

        for (InvoiceCategory titleInvoiceCategory : titleInvoiceCategories) {
            SqlBuilder sqlBuilder2 = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE_CATEGORY);
            sqlBuilder2
                    .isEqual(MainDatabaseHandler.VAR_PARENT_INVOICE_CATEGORY_ID, titleInvoiceCategory.getInvoiceCategoryId().toString())
                    .and()
                    .startBracket()
                        .isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString())
                        .or()
                        .isNull(MainDatabaseHandler.VAR_APP_USER_ID)
                    .endBracket()
                    .and()
                    .isEqual(MainDatabaseHandler.VAR_BASIC_STATUS_ENUM, BasicStatusEnum.OK.name());
            List<InvoiceCategory> subInvoiceCategories = mainDatabaseHandler.findInvoiceCategoriesSql(sqlBuilder2);
            Collections.sort(subInvoiceCategories, new InvoiceCategoryComparator());
            invoiceCategories.addAll(subInvoiceCategories);
        }

        FragmentManager fm = getSupportFragmentManager();
        DialogInvoiceCategory selectInvoiceCategoryDialog = new DialogInvoiceCategory();

        selectInvoiceCategoryDialog.setArguments(invoiceCategories);

        selectInvoiceCategoryDialog.show(fm, "dialog_invoice_category_select");

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void refreshViews(){
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());

        BasicData basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.INCOME);
        if (basicData != null){
            searchIncomeBtn.setChecked(true);
        }else{
            searchIncomeBtn.setChecked(false);
        }

        basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.EXPENSE);
        if (basicData != null){
            searchExpenseBtn.setChecked(true);
        }else{
            searchExpenseBtn.setChecked(false);
        }

        basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.PAYER);
        if (basicData != null){
            IPaymentPerson paymentPerson = mainDatabaseHandler.paymentPersonByTypeAndId(PaymentPersonTypeEnum.valueOf(basicData.getObject1Class()), UUID.fromString(basicData.getObject1Id()), getApplicationContext());
            if (paymentPerson != null){
                searchPayerBtn.setText(paymentPerson.getPaymentPersonName());
            }else{
                MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                searchPayerBtn.setText("Keine Auswahl");
            }
        }else{
            searchPayerBtn.setText("Keine Auswahl");
        }

        basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.PAYMENT_RECIPIENT);
        if (basicData != null){
            IPaymentPerson paymentPerson = mainDatabaseHandler.paymentPersonByTypeAndId(PaymentPersonTypeEnum.valueOf(basicData.getObject1Class()), UUID.fromString(basicData.getObject1Id()), getApplicationContext());
            if (paymentPerson != null){
                searchPaymentRecipientBtn.setText(paymentPerson.getPaymentPersonName());
            }else{
                MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                searchPaymentRecipientBtn.setText("Keine Auswahl");
            }
        }else{
            searchPaymentRecipientBtn.setText("Keine Auswahl");
        }

        basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.COST_DISTRIBUTOR);
        if (basicData != null){
            IPaymentPerson paymentPerson = mainDatabaseHandler.paymentPersonByTypeAndId(PaymentPersonTypeEnum.valueOf(basicData.getObject1Class()), UUID.fromString(basicData.getObject1Id()), getApplicationContext());
            if (paymentPerson != null){
                searchCostDistributorCostsBtn.setText(paymentPerson.getPaymentPersonName());
            }else{
                MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                searchCostDistributorCostsBtn.setText("Keine Auswahl");
            }
        }else{
            searchCostDistributorCostsBtn.setText("Keine Auswahl");
        }

        basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.INVOICE_CATEGORY);
        if (basicData != null){

            SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE_CATEGORY);
            sqlBuilder.isEqual(MainDatabaseHandler.VAR_INVOICE_CATEGORY_ID, basicData.getValue());

            List<InvoiceCategory> invoiceCategories = mainDatabaseHandler.findInvoiceCategoriesSql(sqlBuilder);

            if (!invoiceCategories.isEmpty()){
                searchCategoryBtn.setText(invoiceCategories.get(0).getInvoiceCategoryName());
            }else{
                MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                searchCategoryBtn.setText("Keine Auswahl");
            }

        }else{
            searchCategoryBtn.setText("Keine Auswahl");
        }

        basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.COST_DISTRIBUTION);
        if (basicData != null){

            List<CostDistribution> costDistributions = mainDatabaseHandler.findCostDistributions(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ID, basicData.getValue());

            if (!costDistributions.isEmpty()){
                searchCostDistributionBtn.setText(costDistributions.get(0).getName());
            }else{
                MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                searchCostDistributionBtn.setText("Keine Auswahl");
            }

        }else{
            searchCostDistributionBtn.setText("Keine Auswahl");
        }

        basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.REPETITION_TYPE);
        if (basicData != null){

            RepetitionTypeEnum repetitionTypeEnum = null;
            try{
                repetitionTypeEnum  = RepetitionTypeEnum.valueOf(basicData.getValue());
            }catch (IllegalArgumentException e){
            }

            if (repetitionTypeEnum != null){
                searchRepetitionTypeBtn.setSelection(repetitionTypeEnums.indexOf(repetitionTypeEnum));
            }else{
                MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                searchRepetitionTypeBtn.setSelection(0);
            }

        }else{
            searchRepetitionTypeBtn.setSelection(0);
        }

        basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.SPECIAL_TYPE);
        if (basicData != null){

            BasicSelectionEnum basicSelectionEnum = null;
            try{
                basicSelectionEnum  = BasicSelectionEnum.valueOf(basicData.getValue());
            }catch (IllegalArgumentException e){
            }

            if (basicSelectionEnum != null){
                searchSpecialTypeBtn.setSelection(basicSelectionEnums.indexOf(basicSelectionEnum));
            }else{
                MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                searchSpecialTypeBtn.setSelection(0);
            }

        }else{
            searchSpecialTypeBtn.setSelection(0);
        }

        basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.DATE_START);
        if (basicData != null){

            Date startDate = new Date();
            startDate.setTime(basicData.getNumberValue().longValue());

            if (startDate != null){
                dateStartTextWatcher.setResult(startDate);
            }else{
                MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                dateStartTextWatcher.reset();
            }

        }else{
            dateStartTextWatcher.reset();
        }

        basicData = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.DATE_END);
        if (basicData != null){

            Date startDate = new Date();
            startDate.setTime(basicData.getNumberValue().longValue());

            if (startDate != null){
                dateEndTextWatcher.setResult(startDate);
            }else{
                MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicData);
                dateEndTextWatcher.reset();
            }

        } else{
            dateEndTextWatcher.reset();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.search_configuration, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.filterReadyMenuButton:
                onBackPressed();
                break;
            default:
                onBackPressed();
        }

        return true;
    }

    @Override
    public void onBackPressed(){
        Intent in = getIntent();
        setResult(RESULT_OK, in);
        finish();
    }

    @Override
    public void onFinishPaymentPersonSelectDialog(InvoiceDetailsPaymentPersonDialogEnum invoiceDetailsPaymentPersonDialogEnum, IPaymentPerson iPaymentPerson) {

        if (iPaymentPerson != null) {

            if (invoiceDetailsPaymentPersonDialogEnum.equals(InvoiceDetailsPaymentPersonDialogEnum.PAYER)) {
                BasicData basicData = MainDatabaseHandler.findBasicSearchDataOrCreate(getApplicationContext(), BasicDataSubType.PAYER);
                basicData.setObject1Class(iPaymentPerson.getPaymentPersonEnum().name());
                basicData.setObject1Id(iPaymentPerson.getPaymentPersonId().toString());
                MainDatabaseHandler.saveBasicSearchData(getApplicationContext(), basicData);
            } else if (invoiceDetailsPaymentPersonDialogEnum.equals(InvoiceDetailsPaymentPersonDialogEnum.PAYMENT_RECIPIENT)) {
                BasicData basicData = MainDatabaseHandler.findBasicSearchDataOrCreate(getApplicationContext(), BasicDataSubType.PAYMENT_RECIPIENT);
                basicData.setObject1Class(iPaymentPerson.getPaymentPersonEnum().name());
                basicData.setObject1Id(iPaymentPerson.getPaymentPersonId().toString());
                MainDatabaseHandler.saveBasicSearchData(getApplicationContext(), basicData);
            }else if (invoiceDetailsPaymentPersonDialogEnum.equals(InvoiceDetailsPaymentPersonDialogEnum.COST_DISTRIBUTOR)) {
                BasicData basicData = MainDatabaseHandler.findBasicSearchDataOrCreate(getApplicationContext(), BasicDataSubType.COST_DISTRIBUTOR);
                basicData.setObject1Class(iPaymentPerson.getPaymentPersonEnum().name());
                basicData.setObject1Id(iPaymentPerson.getPaymentPersonId().toString());
                MainDatabaseHandler.saveBasicSearchData(getApplicationContext(), basicData);
            }
        }

        refreshViews();
    }

    @Override
    public void onFinishInvoiceCategorySelectDialog(InvoiceCategory invoiceCategory) {
        if (invoiceCategory != null){
            BasicData basicData = MainDatabaseHandler.findBasicSearchDataOrCreate(getApplicationContext(), BasicDataSubType.INVOICE_CATEGORY);
            basicData.setValue(invoiceCategory.getInvoiceCategoryId().toString());
            MainDatabaseHandler.saveBasicSearchData(getApplicationContext(), basicData);
        }

        refreshViews();
    }

    @Override
    public void onFinishCostDistributionSelectDialog(CostDistribution costDistribution, Boolean wantToEdit) {

        if (costDistribution != null){
            BasicData basicData = MainDatabaseHandler.findBasicSearchDataOrCreate(getApplicationContext(), BasicDataSubType.COST_DISTRIBUTION);
            basicData.setValue(costDistribution.getCostDistributionId().toString());
            MainDatabaseHandler.saveBasicSearchData(getApplicationContext(), basicData);
        }

        refreshViews();
    }
}
