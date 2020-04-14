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
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import de.nextbill.client.R;
import de.nextbill.client.breceiver.RequestListener;
import de.nextbill.client.breceiver.RequestServiceBroadcastReceiver;
import de.nextbill.client.comparators.InvoiceCategoryComparator;
import de.nextbill.client.comparators.PaymentPersonComparator;
import de.nextbill.client.database.IOHelper;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.enums.BasicStatusEnum;
import de.nextbill.client.enums.BroadcastMessageActionEnum;
import de.nextbill.client.enums.BudgetRepetitionType;
import de.nextbill.client.enums.InvoiceDetailsPaymentPersonDialogEnum;
import de.nextbill.client.enums.LoginStatusEnum;
import de.nextbill.client.enums.OnlineStatusEnum;
import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.enums.PaymentTypeEnum;
import de.nextbill.client.helper.LoginUserHelper;
import de.nextbill.client.interfaces.CurrencyTextWatcher;
import de.nextbill.client.interfaces.CustomCurrencyTextWatcher;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.BusinessPartner;
import de.nextbill.client.model.IPaymentPerson;
import de.nextbill.client.model.InvoiceCategory;
import de.nextbill.client.model.PaymentPersonDTO;
import de.nextbill.client.model.UserContact;
import de.nextbill.client.network.RequestManager;
import de.nextbill.client.network.RequestUpdateService;
import de.nextbill.client.pojos.BudgetDTO;
import de.nextbill.client.utils.SqlBuilder;

public class ActivityBudgetDetails extends AppCompatActivity implements DialogPaymentPerson.SelectPaymentPersonDialogListener, DialogInvoiceCategory.SelectInvoiceCategoryDialogListener {

    ListView list;

    private BudgetDTO budget;

    private ProgressBar progressBar2;
    private Switch specialTypeSw;
    private EditText invoiceSumEt;
    private EditText budgetNameEt;
    private EditText invoiceRemarksEt;
    private Spinner invoicePaymentTypeSpinner;
    private Spinner budgetRepetitionTypeSpinner;

    private Button userSelectionTv;
    private Button categoriesTv;
    private ImageButton addUserBtn;
    private ImageButton addCategoryBtn;
    private ImageButton clearUsersBtn;
    private ImageButton clearCategoriesBtn;

    CurrencyTextWatcher currencyTextWatcher;

    private List<PaymentTypeEnum> paymentTypeEnums = new ArrayList();
    private List<BudgetRepetitionType> budgetRepetitionTypeEnums = new ArrayList();

    private RequestServiceBroadcastReceiver updateRequestReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_details);
        this.setTitle("Budget-Details");

        progressBar2 = (ProgressBar) findViewById(R.id.progressBar2);
        specialTypeSw = (Switch) findViewById(R.id.specialTypeSw);

        invoiceSumEt = (EditText) findViewById(R.id.invoiceSumEt);

        invoiceRemarksEt = (EditText) findViewById(R.id.invoiceRemarksEt);

        budgetNameEt = (EditText) findViewById(R.id.budgetNameEt);

        addUserBtn = (ImageButton) findViewById(R.id.addUserBtn);
        addCategoryBtn = (ImageButton) findViewById(R.id.addCategoryBtn);
        clearUsersBtn = (ImageButton) findViewById(R.id.clearUsersBtn);
        clearCategoriesBtn = (ImageButton) findViewById(R.id.clearCategoriesBtn);

        userSelectionTv = (Button) findViewById(R.id.invoicePersonButton);
        categoriesTv = (Button) findViewById(R.id.invoiceInvoiceCategoryButton);

        invoicePaymentTypeSpinner = (Spinner) findViewById(R.id.invoicePaymentTypeSpinner);
        budgetRepetitionTypeSpinner = (Spinner) findViewById(R.id.budgetRepetitionTypeSpinner);

        addUserBtn.setBackground(null);
        addCategoryBtn.setBackground(null);
        clearUsersBtn.setBackground(null);
        clearCategoriesBtn.setBackground(null);

        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();

        if (getIntent().hasExtra("budget")){
            budget = gson.fromJson(getIntent().getStringExtra("budget"), BudgetDTO.class);
        }else{
            budget = new BudgetDTO();
        }

        specialTypeSw.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                budget.setSpecialType(isChecked);
            }
        });

        clearUsersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                budget.setPayerDTOS(new ArrayList<PaymentPersonDTO>());

                refreshViews();
            }
        });

        addCategoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInvoiceCategorySelectDialog();
            }
        });
        clearCategoriesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                budget.setInvoiceCategoryDTOS(new ArrayList<InvoiceCategory>());

                refreshViews();
            }
        });

        paymentTypeEnums.clear();
        paymentTypeEnums.addAll(Arrays.asList(PaymentTypeEnum.NOT_DEFINED.values()));

        budgetRepetitionTypeEnums.clear();
        budgetRepetitionTypeEnums.addAll(Arrays.asList(BudgetRepetitionType.values()));

        ArrayAdapter<PaymentTypeEnum> dataAdapter = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_spinner_item, paymentTypeEnums);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        invoicePaymentTypeSpinner.setAdapter(dataAdapter);
        invoicePaymentTypeSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PaymentTypeEnum paymentTypeEnum = paymentTypeEnums.get(position);
                budget.setPaymentTypeEnum(paymentTypeEnum);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ArrayAdapter<BudgetRepetitionType> dataAdapterForBudgetTypes = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_spinner_item, budgetRepetitionTypeEnums);
        dataAdapterForBudgetTypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        budgetRepetitionTypeSpinner.setAdapter(dataAdapterForBudgetTypes);
        budgetRepetitionTypeSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                BudgetRepetitionType budgetRepetitionType = budgetRepetitionTypeEnums.get(position);
                budget.setBudgetRepetitionType(budgetRepetitionType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        addCategoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInvoiceCategorySelectDialog();
            }
        });

        addUserBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPersonsAddDialog();
            }
        });

        clearCategoriesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                budget.setInvoiceCategoryDTOS(new ArrayList<InvoiceCategory>());

                refreshViews();
            }
        });

        updateRequestReceiver = new RequestServiceBroadcastReceiver(new RequestListener() {

            @Override
            public void requestDone(String activityClassName) { }

            @Override
            public void generalMessage(String activityClassName, BroadcastMessageActionEnum broadcastMessageActionEnum, String message) {
                if (activityClassName.equals(ActivityBudgetDetails.class.getSimpleName())){
                    if (broadcastMessageActionEnum != null && BroadcastMessageActionEnum.READY.equals(broadcastMessageActionEnum)) {
                        progressBar2.setVisibility(View.INVISIBLE);
                        Toast.makeText(getApplicationContext(), "Budget erfolgreich gespeichert!", Toast.LENGTH_LONG).show();

                        Intent j = getIntent();
                        j.putExtra("refresh", "");
                        setResult(RESULT_OK, j);
                        finish();
                    }else if (broadcastMessageActionEnum != null && BroadcastMessageActionEnum.ERROR.equals(broadcastMessageActionEnum)) {
                        progressBar2.setVisibility(View.INVISIBLE);
                        Toast.makeText(getApplicationContext(), "Interner Fehler!\n Bitte später nochmal versuchen!", Toast.LENGTH_LONG).show();
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

        CustomCurrencyTextWatcher sumTextWatcher = new CustomCurrencyTextWatcher() {

            @Override
            public Boolean validate(BigDecimal newSum) {

//                BigDecimal sumOfAllFixedOnes = CostDistributionHelper.getSumOfAllFixedCostDistributionItems(costDistributionItems);
//                if (newSum.compareTo(sumOfAllFixedOnes) == -1){
//                    return false;
//                }
                return true;
            }

            public void afterTextChanged(Editable s) {
                invoiceSumEt.removeTextChangedListener(this);

                budget.setSum(currencyTextWatcher.getResult());

                invoiceSumEt.addTextChangedListener(this);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        };
        currencyTextWatcher = new CurrencyTextWatcher(invoiceSumEt, budget.getSum() != null ? budget.getSum() : new BigDecimal(0));
        currencyTextWatcher.addCustomTextWatcher(sumTextWatcher);

        TextWatcher nameChangedListener = new TextWatcher() {

            public void afterTextChanged(Editable s) {
                budget.setName(s.toString());
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        };
        budgetNameEt.addTextChangedListener(nameChangedListener);

        TextWatcher remarkChangedListener = new TextWatcher() {

            public void afterTextChanged(Editable s) {
                budget.setRemarks(s.toString());
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        };
        invoiceRemarksEt.addTextChangedListener(remarkChangedListener);

        refreshViews();

        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.baseline_save_white_36);
        getSupportActionBar().setHomeAsUpIndicator(drawable);
    }

    public void refreshViews(){
        if (budget.getSum() != null){
            invoiceSumEt.setText(bigDecimalToString(budget.getSum()));
            invoiceSumEt.setTextColor(Color.parseColor("#000000"));
        }else{
            invoiceSumEt.setTextColor(Color.parseColor("#7a7a7a"));
        }

        if (budget.getBudgetRepetitionType() != null){
            budgetRepetitionTypeSpinner.setSelection(budgetRepetitionTypeEnums.indexOf(budget.getBudgetRepetitionType()));
        }else{
            budgetRepetitionTypeSpinner.setSelection(0);
        }

        if (budget.getPaymentTypeEnum() != null){
            invoicePaymentTypeSpinner.setSelection(paymentTypeEnums.indexOf(budget.getPaymentTypeEnum()));
        }else{
            invoicePaymentTypeSpinner.setSelection(0);
        }

        if (budget.getRemarks() != null){
            invoiceRemarksEt.setText(budget.getRemarks());
        }else{
            invoiceRemarksEt.setText("");
        }

        if (budget.getName() != null){
            budgetNameEt.setText(budget.getName());
        }else{
            budgetNameEt.setText("");
        }

        if (budget.getSpecialType() != null && budget.getSpecialType()) {
            specialTypeSw.setChecked(true);
        }else {
            specialTypeSw.setChecked(false);
        }
        
        refreshCostPayersView();
        refreshInvoiceCategoriesView();
    }

    public void refreshCostPayersView(){
        if (budget.getPayerDTOS() != null && !budget.getPayerDTOS().isEmpty()){
            String payersResultString = "";
            userSelectionTv.setTextColor(Color.parseColor("#000000"));

            for (int i = 0; i < budget.getPayerDTOS().size(); i++) {

                PaymentPersonDTO paymentPersonDTO = budget.getPayerDTOS().get(i);
                
                String payerString = paymentPersonDTO.getDisplayName();

                if (i == 0){
                    payersResultString = payerString;
                }else if (i % 2 != 0){
                    payersResultString = payersResultString +" - "+ payerString;
                }else if (i % 2 == 0){
                    payersResultString = payersResultString + "\n" + payerString;
                }
            }

            userSelectionTv.setText(payersResultString);
        }else{
            userSelectionTv.setText("Keine Transaktionspartner ausgewählt");
            userSelectionTv.setTextColor(Color.parseColor("#7a7a7a"));
        }
    }

    public void refreshInvoiceCategoriesView(){
        if (budget.getInvoiceCategoryDTOS() != null && !budget.getInvoiceCategoryDTOS().isEmpty()){
            String categoriesResultString = "";
            categoriesTv.setTextColor(Color.parseColor("#000000"));

            for (int i = 0; i < budget.getInvoiceCategoryDTOS().size(); i++) {

                InvoiceCategory invoiceCategory = budget.getInvoiceCategoryDTOS().get(i);

                String categoryName = invoiceCategory.getInvoiceCategoryName();

                if (i == 0){
                    categoriesResultString = categoryName;
                }else if (i % 2 != 0){
                    categoriesResultString = categoriesResultString +" - "+ categoryName;
                }else if (i % 2 == 0){
                    categoriesResultString = categoriesResultString + "\n" + categoryName;
                }
            }

            categoriesTv.setText(categoriesResultString);
        }else{
            categoriesTv.setText("Keine Kategorien ausgewählt");
            categoriesTv.setTextColor(Color.parseColor("#7a7a7a"));
        }
    }

    public static String bigDecimalToString(BigDecimal currentDecimal){
        NumberFormat numberFormatter = NumberFormat.getNumberInstance(Locale.GERMAN);
        numberFormatter.setGroupingUsed(false);
        numberFormatter.setMaximumFractionDigits(2);
        numberFormatter.setMinimumFractionDigits(2);

        String number = numberFormatter.format(currentDecimal);
        return number;
    }

    private void showPersonsAddDialog() {

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

    public boolean saveAndUpload(){

        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());

        if (budget.getSum() == null){
            budget.setSum(new BigDecimal(0));
        }

        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        String budgetAsString = gson.toJson(budget);

        UUID randomId = UUID.randomUUID();
        if (budget.getBudgetId() == null){
            budget.setBudgetId(randomId);
            handler.addObject(budgetAsString, StatusDatabaseHandler.OBJECT_TYPE_BUDGET, StatusDatabaseHandler.UPDATE_STATUS_ADD, new Date().getTime(), 1);
        }else{
            handler.addObject(budgetAsString, StatusDatabaseHandler.OBJECT_TYPE_BUDGET, StatusDatabaseHandler.UPDATE_STATUS_PUT, new Date().getTime(), 1);
        }

        Intent sIntent = new Intent(getApplicationContext(), RequestUpdateService.class);
        sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
        getApplicationContext().startService(sIntent);

        return true;
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(updateRequestReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.budget_details, menu);
        return true;
    }

    @Override
    public void onBackPressed(){
        Intent in = getIntent();
        setResult(RESULT_OK, in);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.deleteBudgetSubMenuButton:
                break;
            case R.id.deleteBudgetMenuButton:
                deleteBudget();
                Intent i = getIntent();
                setResult(RESULT_OK, i);
                finish();
                break;
            default:
                saveAndUpload();
                onBackPressed();
        }

        return true;
    }

    public void deleteBudget(){

        if (budget.getBudgetId() != null){
            StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
            handler.addObject(budget.getBudgetId().toString(), StatusDatabaseHandler.OBJECT_TYPE_BUDGET, StatusDatabaseHandler.UPDATE_STATUS_DELETE, new Date().getTime(), 1);
            Intent sIntent = new Intent(getApplicationContext(), RequestUpdateService.class);
            sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
            getApplicationContext().startService(sIntent);
        }
    }

    @Override
    public void onFinishPaymentPersonSelectDialog(InvoiceDetailsPaymentPersonDialogEnum invoiceDetailsPaymentPersonDialogEnum, IPaymentPerson iPaymentPerson) {
        if (iPaymentPerson != null) {
            PaymentPersonDTO paymentPersonDTO = new PaymentPersonDTO();
            paymentPersonDTO.setDisplayName(iPaymentPerson.getPaymentPersonName());
            paymentPersonDTO.setId(iPaymentPerson.getPaymentPersonId());
            paymentPersonDTO.setPaymentPersonTypeEnum(iPaymentPerson.getPaymentPersonEnum());

            budget.getPayerDTOS().add(paymentPersonDTO);

            refreshViews();
        }
    }

    @Override
    public void onFinishInvoiceCategorySelectDialog(InvoiceCategory invoiceCategory) {
        if (invoiceCategory != null){
            budget.getInvoiceCategoryDTOS().add(invoiceCategory);

            refreshViews();
        }
    }
}
