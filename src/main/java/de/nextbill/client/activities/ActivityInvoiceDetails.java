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
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import de.nextbill.client.R;
import de.nextbill.client.breceiver.RequestListener;
import de.nextbill.client.breceiver.RequestServiceBroadcastReceiver;
import de.nextbill.client.comparators.BusinessPartnerComparator;
import de.nextbill.client.comparators.EntityComparatorService;
import de.nextbill.client.comparators.InvoiceCategoryComparator;
import de.nextbill.client.comparators.PaymentPersonComparator;
import de.nextbill.client.database.IOHelper;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.enums.BasicStatusEnum;
import de.nextbill.client.enums.BroadcastMessageActionEnum;
import de.nextbill.client.enums.CorrectionStatus;
import de.nextbill.client.enums.CostDistributionItemTypeEnum;
import de.nextbill.client.enums.InvoiceCategoryType;
import de.nextbill.client.enums.InvoiceDetailsPaymentPersonDialogEnum;
import de.nextbill.client.enums.InvoiceSource;
import de.nextbill.client.enums.InvoiceStatusEnum;
import de.nextbill.client.enums.LoginStatusEnum;
import de.nextbill.client.enums.MainFunctionEnum;
import de.nextbill.client.enums.OnlineStatusEnum;
import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.enums.PaymentTypeEnum;
import de.nextbill.client.enums.RepetitionTypeEnum;
import de.nextbill.client.enums.Right;
import de.nextbill.client.helper.CostDistributionHelper;
import de.nextbill.client.helper.ImageHelper;
import de.nextbill.client.helper.LoginUserHelper;
import de.nextbill.client.helper.RightHelper;
import de.nextbill.client.helper.SearchHelper;
import de.nextbill.client.helper.StatisticHelper;
import de.nextbill.client.interfaces.CurrencyTextWatcher;
import de.nextbill.client.interfaces.CustomCurrencyTextWatcher;
import de.nextbill.client.interfaces.CustomDateTextWatcher;
import de.nextbill.client.interfaces.DateTextWatcher;
import de.nextbill.client.interfaces.IPaymentItem;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.ArticleDTO;
import de.nextbill.client.model.BusinessPartner;
import de.nextbill.client.model.CostDistribution;
import de.nextbill.client.model.CostDistributionItem;
import de.nextbill.client.model.IPaymentPerson;
import de.nextbill.client.model.Invoice;
import de.nextbill.client.model.InvoiceCategory;
import de.nextbill.client.model.InvoiceFailure;
import de.nextbill.client.model.StandingOrder;
import de.nextbill.client.model.UserContact;
import de.nextbill.client.network.RequestManager;
import de.nextbill.client.network.RequestUpdateService;
import de.nextbill.client.pojos.SearchFilter;
import de.nextbill.client.utils.SqlBuilder;

public class ActivityInvoiceDetails extends AppCompatActivity implements DialogDatePicker.EditDateDialogListener, DialogCreateStandingOrder.CreateStandingOrderDialogListener,
        DialogPaymentPerson.SelectPaymentPersonDialogListener, DialogCostDistribution.SelectCostDistributionListener, DialogInvoiceCategory.SelectInvoiceCategoryDialogListener, DialogConflict.InputConflictDialogListener{

    static int COST_DISTRIBUTION = 1;
    static int INVOICE_DETAILS = 2;
    static int TAKE_PICTURE = 3;
    static int LOGIN = 22;
    static int ARTICLE_SELECT = 6;

    private static final int MY_PERMISSIONS_REQUEST_TAKE_PICTURE = 64424;
    private static final int MY_PERMISSIONS_REQUEST_VIEW_PICTURE = 1345;
    private static final int MY_PERMISSIONS_REQUEST_DOWNLOAD_PICTURE = 1425;

    NumberFormat numberFormatter;

    EditText mInvoiceDateEt;
    Button mInvoicePayerButton;
    Button mInvoicePaymentRecipientButton;
    EditText mInvoiceSumEt;
    TextView isExternInvoiceTv;
    TextView invoiceFailureMessageTv;
    ListView costDistributionsGv;
    ImageButton mustBeBilledIb;
    ImageButton mustBeBilled2Ib;

    CoordinatorLayout coordinatorLayout;
    AppBarLayout appBarLayout;

    FloatingActionButton fab;

    private RepetitionTypeEnum repetitionTypeEnum;
    private PaymentTypeEnum paymentTypeEnum;
    private Spinner invoicePaymentTypeSpinner;
    private List<RepetitionTypeEnum> repetitionTypeEnums = new ArrayList();
    private List<PaymentTypeEnum> paymentTypeEnums = new ArrayList();

    Button invoiceInvoiceCategoryButton;
    Button mInvoiceCostDistributionButton;

    MenuItem addImage;
    MenuItem deleteImage;

    EditText invoiceRemarksEt;
    Switch specialTypeSw;
    Button costDistributionDialogButton;
    MenuItem problemBtn;

    RelativeLayout standingOrderConfigRl;
    EditText standingOrderDatePickerEt;
    Spinner standingOrderRepetitionTypeSpinner;

    ImageView mInvoiceReceiptIv;

    MenuItem checkMenuBtton;
    MenuItem createStandingOrder;
    MenuItem deleteStandingOrder;
    MenuItem resetMenuButton;
    MenuItem deleteMenuButton;
    MenuItem deleteSubMenuButton;
    MenuItem articleSelectionMenuButton;

    TextView placeholderTv;

    CurrencyTextWatcher currencyTextWatcher;
    DateTextWatcher dateTextWatcher;
    RelativeLayout dateAndSumRl;
    RelativeLayout payerRl;
    RelativeLayout paymentRecipientRl;
    RelativeLayout costDistributionItemsRl;
    View specialDividerRl;
    View paymentRecipientLineView;

    List<InvoiceFailure> allInvoiceFailures = new ArrayList<InvoiceFailure>();

    private Invoice currentInvoice;
    private Invoice originalInvoice;

    private boolean isStandingOrder = false;
    private DateTextWatcher standingOrderDateTextWatcher;

    private IPaymentPerson payer;
    private IPaymentPerson paymentRecipient;
    private MainFunctionEnum mainFunctionEnum;
    private List<CostDistributionItem> costDistributionItems = new ArrayList<>();
    private List<CostDistributionItem> costDistributionItemsOriginal = new ArrayList<>();
    private List<CostDistributionItem> costDistributionItemsToDelete = new ArrayList<>();

    private RequestServiceBroadcastReceiver updateRequestReceiver;

    private HashMap<PaymentPersonTypeEnum, String> localizationForPaymentPersonTypeEnums = new HashMap<PaymentPersonTypeEnum, String>();

    private boolean isReadyMode;
    private boolean doUploadImage = false;
    private boolean doDeleteImage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_details);
        IOHelper.getInstance().setCtx(this);

        numberFormatter = NumberFormat.getNumberInstance(Locale.GERMAN);
        numberFormatter.setMaximumFractionDigits(2);
        numberFormatter.setMinimumFractionDigits(2);

        final MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());

        allInvoiceFailures.clear();

        boolean unlockViews = true;

        Intent intentTmp = getIntent();
        if (intentTmp != null){

            boolean isStandingOrder = intentTmp.getBooleanExtra("isStandingOrder", false);
            this.isStandingOrder = isStandingOrder;

            String transactionType = intentTmp.getStringExtra("transactionType");
            if (transactionType != null){
                mainFunctionEnum = MainFunctionEnum.valueOf(transactionType);
                currentInvoice = new Invoice();
                AppUser appUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());
                currentInvoice.setCreatedById(appUser.getAppUserId());

                String stringDate = DateTextWatcher.dateToString(new Date());
                Date correctDate = DateTextWatcher.stringToDate(stringDate);

                currentInvoice.setDateOfInvoice(correctDate);
                currentInvoice.setSumOfInvoice(new BigDecimal(0));

                currentInvoice.setRemarks("");
                currentInvoice.setInvoiceSource(InvoiceSource.MANUAL);

                if (isStandingOrder){
                    currentInvoice.setStandingOrderStartDate(correctDate);
                    currentInvoice.setRepetitionTypeEnum(RepetitionTypeEnum.MONTHLY);
                    currentInvoice.setInvoiceStatusEnum(InvoiceStatusEnum.CHECK);

                }else{
                    currentInvoice.setRepetitionTypeEnum(RepetitionTypeEnum.ONCE);
                    currentInvoice.setInvoiceStatusEnum(InvoiceStatusEnum.READY);
                }

                currentInvoice.setPaymentTypeEnum(PaymentTypeEnum.NOT_DEFINED);

                AppUser currentUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());
                if(mainFunctionEnum.equals(MainFunctionEnum.EXPENSE) || mainFunctionEnum.equals(MainFunctionEnum.NEW_EXPENSE)){
                    currentInvoice.setPayer(currentUser);
                }else if (mainFunctionEnum.equals(MainFunctionEnum.INCOME) || mainFunctionEnum.equals(MainFunctionEnum.NEW_INCOME)){
                    currentInvoice.setPaymentRecipient(currentUser);
                }

                originalInvoice = EntityComparatorService.copyInvoice(currentInvoice);

                CostDistributionItem costDistributionItem = new CostDistributionItem();
                costDistributionItem.setCostPaid(new BigDecimal(0));
                costDistributionItem.setMoneyValue(new BigDecimal(0));
                costDistributionItem.setPosition(0);
                costDistributionItem.setCostDistributionItemTypeEnum(CostDistributionItemTypeEnum.QUOTA);
                costDistributionItem.setValue(new BigDecimal(1));
                costDistributionItem.setInvoiceId(currentInvoice.getInvoiceId());
                costDistributionItem.setPayer(currentUser);
                costDistributionItems.add(costDistributionItem);
                costDistributionItemsOriginal.add(EntityComparatorService.copyCostDistributionItem(costDistributionItem));

                unlockViews = true;
            }else{
                String id = intentTmp.getStringExtra("idInvoice");
                List<Invoice> tempInvoices = mainDatabaseHandler.findInvoices(MainDatabaseHandler.VAR_INVOICE_ID, id);

                if (!tempInvoices.isEmpty()){
                    currentInvoice = tempInvoices.get(0);

                    originalInvoice = EntityComparatorService.copyInvoice(tempInvoices.get(0));

                    payer = currentInvoice.getPayer(getApplicationContext());
                    paymentRecipient = currentInvoice.getPaymentRecipient(getApplicationContext());

                    allInvoiceFailures = mainDatabaseHandler.findInvoiceFailures(MainDatabaseHandler.VAR_INVOICE_ID, currentInvoice.getInvoiceId().toString());

                    SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM);
                    sqlBuilder.isEqual(MainDatabaseHandler.VAR_INVOICE_ID, currentInvoice.getInvoiceId().toString())
                            .orderBy(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ITEM_POSITION);
                    costDistributionItems = mainDatabaseHandler.findCostDistributionItemsSql(sqlBuilder);
                    costDistributionItems.removeAll(costDistributionItemsToDelete);

                    for (CostDistributionItem costDistributionItem : costDistributionItems) {
                        costDistributionItemsOriginal.add(EntityComparatorService.copyCostDistributionItem(costDistributionItem));
                    }

                    mainFunctionEnum = recognizeInvoiceType(currentInvoice);
                    repetitionTypeEnum = currentInvoice.getRepetitionTypeEnum();
                    paymentTypeEnum = currentInvoice.getPaymentTypeEnum();

                    AppUser currentUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());

                    if (currentUser.getAppUserId().equals(currentInvoice.getCreatedById())) {
                        unlockViews = true;
                    }else{
                        unlockViews = false;
                    }
                }
            }
        }

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            Invoice nextCheckInvoice = null;
            if (CostDistributionHelper.isCreatedUserReadyMode(getApplicationContext(), currentInvoice)){
                currentInvoice.setInvoiceStatusEnum(InvoiceStatusEnum.CHECK);
            }else if (CostDistributionHelper.isExternalUserReadyMode(getApplicationContext(), currentInvoice, costDistributionItems)){
                List<IPaymentItem> paymentItems = CostDistributionHelper.paymentItemsForCorrectionMode(getApplicationContext(), currentInvoice, costDistributionItems, false);
                for (IPaymentItem paymentItem : paymentItems) {
                    paymentItem.setCorrectionStatus(CorrectionStatus.CHECK);
                }
            }else if (CostDistributionHelper.isCreatedUserCheckMode(getApplicationContext(), currentInvoice)){
                currentInvoice.setInvoiceStatusEnum(InvoiceStatusEnum.READY);
                currentInvoice.setInvoiceFailureMessage(null);
                nextCheckInvoice = nextOpenInvoice();
            }else if (CostDistributionHelper.isExternalUserCheckMode(getApplicationContext(), currentInvoice, costDistributionItems)){
                List<IPaymentItem> paymentItems = CostDistributionHelper.paymentItemsForCorrectionMode(getApplicationContext(), currentInvoice, costDistributionItems, true);
                for (IPaymentItem paymentItem : paymentItems) {
                    paymentItem.setCorrectionStatus(CorrectionStatus.READY);
                }
                nextCheckInvoice = nextOpenInvoice();
            }

            saveAndUpload();

            if (nextCheckInvoice == null) {
                Intent newIntent = new Intent(getApplicationContext(), ActivityMain.class);
                startActivity(newIntent);
                overridePendingTransition(R.anim.activity_out1, R.anim.activity_out2);
            }else{
                showNextCheckInvoice(nextCheckInvoice);
            }
            }
        });

        isReadyMode = CostDistributionHelper.isCreatedUserReadyMode(getApplicationContext(), currentInvoice) ||
                CostDistributionHelper.isExternalUserReadyMode(getApplicationContext(), currentInvoice, costDistributionItems);
        if (isStandingOrder || isReadyMode){
//            CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
//            p.setAnchorId(View.NO_ID);
//            fab.setLayoutParams(p);
            fab.setX(-5000f);
        }

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);

        invoiceRemarksEt = (EditText) findViewById(R.id.invoiceRemarksEt);
        specialTypeSw = (Switch) findViewById(R.id.specialTypeSw);
        specialTypeSw.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                currentInvoice.setSpecialType(isChecked);
                refreshResetButtonView();
            }
        });

        mustBeBilledIb = (ImageButton) findViewById(R.id.mustBeBilledIb);
        mustBeBilledIb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((currentInvoice.getCostPaid() == null && currentInvoice.getMoneyValue().compareTo(new BigDecimal(0)) != 0) ||
                        (currentInvoice.getCostPaid() != null && currentInvoice.getCostPaid().compareTo(currentInvoice.getMoneyValue()) != 0)){
                    currentInvoice.setCostPaid(currentInvoice.getMoneyValue());
                }else{
                    currentInvoice.setCostPaid(new BigDecimal(0));
                }

                refreshViews();
            }
        });

        mustBeBilled2Ib = (ImageButton) findViewById(R.id.mustBeBilled2Ib);
        mustBeBilled2Ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((currentInvoice.getCostPaid() == null && currentInvoice.getMoneyValue().compareTo(new BigDecimal(0)) != 0) ||
                        (currentInvoice.getCostPaid() != null && currentInvoice.getCostPaid().compareTo(currentInvoice.getMoneyValue()) != 0)){
                    currentInvoice.setCostPaid(currentInvoice.getMoneyValue());
                }else{
                    currentInvoice.setCostPaid(new BigDecimal(0));
                }

                refreshViews();
            }
        });

        placeholderTv = (TextView) findViewById(R.id.placeholderTv);

        isExternInvoiceTv = (TextView) findViewById(R.id.isExternInvoiceTv);
        invoiceFailureMessageTv = (TextView) findViewById(R.id.invoiceFailureMessageTv);

        mInvoiceDateEt = (EditText) findViewById(R.id.invoiceDatePicker);
        dateTextWatcher = new DateTextWatcher(mInvoiceDateEt, currentInvoice.getDateOfInvoice());

        mInvoiceReceiptIv = (ImageView) findViewById(R.id.imageView5);

        dateAndSumRl = (RelativeLayout) findViewById(R.id.dateAndSumRl);

        costDistributionItemsRl = (RelativeLayout) findViewById(R.id.costDistributionItemsRl);

        mInvoicePayerButton = (Button) findViewById(R.id.invoicePayerButton);
        mInvoicePayerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInvoicePayerEditDialog();
            }
        });

        mInvoiceSumEt = (EditText) findViewById(R.id.invoiceSumEt);
        currencyTextWatcher = new CurrencyTextWatcher(mInvoiceSumEt, currentInvoice.getSumOfInvoice());

        costDistributionsGv = (ListView) findViewById(R.id.costDistributionsGv);

        costDistributionDialogButton = (Button) findViewById(R.id.costDistributionDialogButton);
        costDistributionDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCostDistributionSelectDialog();
            }
        });

        mInvoiceCostDistributionButton = (Button) findViewById(R.id.invoiceCostDistributionButton);
        mInvoiceCostDistributionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCostDistributionSelectDialog();
            }
        });

        repetitionTypeEnums.clear();
        repetitionTypeEnums.addAll(Arrays.asList(RepetitionTypeEnum.values()));
        repetitionTypeEnums.remove(RepetitionTypeEnum.NONE);

        paymentTypeEnums.clear();
        paymentTypeEnums.addAll(Arrays.asList(PaymentTypeEnum.values()));

        standingOrderConfigRl = (RelativeLayout) findViewById(R.id.standingOrderConfigRl);
        standingOrderDatePickerEt = (EditText) findViewById(R.id.standingOrderDatePickerEt);
        standingOrderRepetitionTypeSpinner = (Spinner) findViewById(R.id.standingOrderRepetitionTypeSpinner);
        if (this.isStandingOrder){
            standingOrderDateTextWatcher = new DateTextWatcher(standingOrderDatePickerEt, currentInvoice.getStandingOrderStartDate());
            CustomDateTextWatcher standingOrderDateTextWatcherCustom = new CustomDateTextWatcher() {

                @Override
                public void currentValue(Date currentValue){
                    currentInvoice.setStandingOrderStartDate(currentValue);

                    boolean hasChangesInDate = (originalInvoice.getStandingOrderStartDate().equals(currentInvoice.getStandingOrderStartDate()));

                    if (!hasChangesInDate){
                        currentInvoice.setDateOfInvoice(currentInvoice.getStandingOrderStartDate());
                        dateTextWatcher.setResult(currentInvoice.getStandingOrderStartDate());
                    }else{
                        currentInvoice.setDateOfInvoice(originalInvoice.getDateOfInvoice());
                        dateTextWatcher.setResult(originalInvoice.getDateOfInvoice());
                    }

                    refreshResetButtonView();
                }

                public void afterTextChanged(Editable s) {

                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }
            };
            standingOrderDateTextWatcher.addCustomTextWatcher(standingOrderDateTextWatcherCustom);


            repetitionTypeEnums.remove(RepetitionTypeEnum.ONCE);

            ArrayAdapter<RepetitionTypeEnum> dataAdapter = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_spinner_item, repetitionTypeEnums);
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            standingOrderRepetitionTypeSpinner.setAdapter(dataAdapter);

            standingOrderRepetitionTypeSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    repetitionTypeEnum = repetitionTypeEnums.get(position);
                    currentInvoice.setRepetitionTypeEnum(repetitionTypeEnum);
                    refreshResetButtonView();
                    refreshViews();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

        }

//        adapterActivityInvoiceDetailsGridView = new AdapterActivityInvoiceDetailsGridView(this, costDistributionItems, currentInvoice.getSumOfInvoice(), null);
//        costDistributionsGv.setAdapter(adapterActivityInvoiceDetailsGridView);

        invoiceInvoiceCategoryButton = (Button) findViewById(R.id.invoiceInvoiceCategoryButton);
        invoiceInvoiceCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInvoiceCategorySelectDialog();
            }
        });

        mInvoicePaymentRecipientButton = (Button) findViewById(R.id.invoicePaymentRecipientButton);
        mInvoicePaymentRecipientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInvoicePaymentRecipientEditDialog();
            }
        });

//        progressBarImageDownload = (ProgressBar) findViewById(R.id.progressBarImageDownload);

        payerRl = (RelativeLayout) findViewById(R.id.payerRl);
        paymentRecipientLineView = (View) findViewById(R.id.paymentRecipientLineView);
        paymentRecipientRl = (RelativeLayout) findViewById(R.id.paymentRecipientRl);
        specialDividerRl = (View) findViewById(R.id.specialDividerRl);

        invoicePaymentTypeSpinner = (Spinner) findViewById(R.id.invoicePaymentTypeSpinner);


        TextWatcher remarkChangedListener = new TextWatcher() {

            public void afterTextChanged(Editable s) {
                currentInvoice.setRemarks(s.toString());
                refreshResetButtonView();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        };
        invoiceRemarksEt.addTextChangedListener(remarkChangedListener);

        ArrayAdapter<PaymentTypeEnum> dataAdapter = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_spinner_item, paymentTypeEnums);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        invoicePaymentTypeSpinner.setAdapter(dataAdapter);

        invoicePaymentTypeSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                paymentTypeEnum = paymentTypeEnums.get(position);
                currentInvoice.setPaymentTypeEnum(paymentTypeEnum);
                refreshResetButtonView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        localizationForPaymentPersonTypeEnums.clear();
        localizationForPaymentPersonTypeEnums.put(PaymentPersonTypeEnum.NEW, "Neu");
        localizationForPaymentPersonTypeEnums.put(PaymentPersonTypeEnum.USER, "Benutzer");
        localizationForPaymentPersonTypeEnums.put(PaymentPersonTypeEnum.BUSINESS_PARTNER, "Unternehmen");
        localizationForPaymentPersonTypeEnums.put(PaymentPersonTypeEnum.NEW_BUSINESS_PARTNER, "Unternehmen");
        localizationForPaymentPersonTypeEnums.put(PaymentPersonTypeEnum.CONTACT, "Privatperson");
        localizationForPaymentPersonTypeEnums.put(PaymentPersonTypeEnum.NEW_CONTACT, "Privatperson");
        localizationForPaymentPersonTypeEnums.put(PaymentPersonTypeEnum.PROJECT, "Projekt");

        updateRequestReceiver = new RequestServiceBroadcastReceiver(new RequestListener() {

            @Override
            public void requestDone(String activityClassName) {
            }

            @Override
            public void generalMessage(String activityClassName, BroadcastMessageActionEnum broadcastMessageActionEnum, String message) {
                if (activityClassName.equals(ActivityInvoiceDetails.class.getSimpleName())){

                    if (BroadcastMessageActionEnum.READY.equals(broadcastMessageActionEnum) && message.equals("StandingOrderCreated")) {
                        createStandingOrder.setVisible(false);
                        deleteStandingOrder.setVisible(true);
                        Toast.makeText(getApplicationContext(), "Dauerauftrag erfolgreich erstellt!", Toast.LENGTH_LONG).show();
                    }else if (BroadcastMessageActionEnum.ERROR.equals(broadcastMessageActionEnum) && message.equals("StandingOrderCreated")) {
                        Toast.makeText(getApplicationContext(), "Interner Fehler!", Toast.LENGTH_LONG).show();
                        currentInvoice.setStandingOrderInvoiceTemplateId(null);
                    }else if (BroadcastMessageActionEnum.READY.equals(broadcastMessageActionEnum) && message.equals("StandingOrderDeleted")) {
                        if (!isStandingOrder){

                            MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());

                            if (currentInvoice.getStandingOrderInvoiceTemplateId() != null){
                                SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE);
                                sqlBuilder.isEqual(MainDatabaseHandler.VAR_INVOICE_ID, currentInvoice.getStandingOrderInvoiceTemplateId().toString());

                                List<Invoice> invoices1 = mainDatabaseHandler.findInvoicesSql(sqlBuilder);
                                mainDatabaseHandler.deleteInvoices(invoices1);
                            }

                            createStandingOrder.setVisible(true);
                            deleteStandingOrder.setVisible(false);
                            currentInvoice.setStandingOrderInvoiceTemplateId(null);
                            originalInvoice.setStandingOrderInvoiceTemplateId(null);
                            Toast.makeText(getApplicationContext(), "Dauerauftrag erfolgreich gelöscht!", Toast.LENGTH_LONG).show();
                        }
                    }else if (BroadcastMessageActionEnum.ERROR.equals(broadcastMessageActionEnum) && message.equals("StandingOrderDeleted")){
                        Toast.makeText(getApplicationContext(), "Interner Fehler! Bitte wenden Sie sich an den Support.", Toast.LENGTH_LONG).show();
                    }else if (BroadcastMessageActionEnum.REFRESH.equals(broadcastMessageActionEnum) && message.equals("image")){
//                        progressBarImageDownload.setVisibility(View.INVISIBLE);
                        refreshImageView();
                        refreshImageMenuItems();
                    }else if (BroadcastMessageActionEnum.ERROR.equals(broadcastMessageActionEnum) && message.equals("image")){
                        refreshImageView();
//                        Toast.makeText(getApplicationContext(), "Interner Fehler!", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void loginMessage(LoginStatusEnum loginStatusEnum) {

            }

            @Override
            public void onlineStatusMessage(OnlineStatusEnum onlineStatusEnum) {
                if (!OnlineStatusEnum.MANUAL_OFFLINE.equals(onlineStatusEnum)){
                    Toast.makeText(getApplicationContext(), "Offline-Modus aktiv! Funktion nicht ausführbar.", Toast.LENGTH_SHORT).show();
                }
                refreshImageView();
            }

        });

        sendImageDownloadRequest();

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
                mInvoiceSumEt.removeTextChangedListener(this);

                currentInvoice.setSumOfInvoice(currencyTextWatcher.getResult());
                for (CostDistributionItem costDistributionItem : costDistributionItems) {
                    costDistributionItem.setMoneyValue(CostDistributionHelper.calculateAmountForCostDistributionItemPrecise(costDistributionItem, costDistributionItems, currentInvoice.getSumOfInvoice()));
                }
                refreshCostDistributionItemsView();

                refreshMustBeBilledButtons();

                refreshResetButtonView();

                mInvoiceSumEt.addTextChangedListener(this);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        };
        currencyTextWatcher.addCustomTextWatcher(sumTextWatcher);

        CustomDateTextWatcher dateTextWatcherCustom = new CustomDateTextWatcher() {

            @Override
            public void currentValue(Date currentValue){
                currentInvoice.setDateOfInvoice(currentValue);
                refreshResetButtonView();
            }

            public void afterTextChanged(Editable s) {

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        };
        dateTextWatcher.addCustomTextWatcher(dateTextWatcherCustom);

        refreshViews();

        dateAndSumRl.requestFocus();

        unlockViews(unlockViews);
    }



    public void configureToolbar(final Integer internalHeight){
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final int height = size.y;


        appBarLayout.post(new Runnable() {
            @Override
            public void run() {
                int heightPx = (height / 3) * 2;
                final int heightDif = (height - internalHeight);
                heightPx = heightPx - heightDif;
                setAppBarOffset(heightPx);
            }
        });
    }

    private void setAppBarOffset(int offsetPx) {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
        AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
        if (behavior != null) {
            behavior.onNestedPreScroll(coordinatorLayout, appBarLayout, null, 0, offsetPx, new int[]{0, 0}, ViewCompat.TYPE_NON_TOUCH);
        }
    }

    public boolean refreshResetButtonView(){
        return refreshResetButtonView(true);
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

    public boolean refreshResetButtonView(boolean refreshView){

        boolean anythingChanged = false;

        boolean invoicesEqual = EntityComparatorService.invoicesEqual(currentInvoice, originalInvoice);

        if (!invoicesEqual){
            anythingChanged = true;
        }

        if (costDistributionItems.size() != costDistributionItemsOriginal.size()){
            anythingChanged = true;
        }

        if (this.doDeleteImage || this.doUploadImage) {
            anythingChanged = true;
        }

        if (!anythingChanged){
            for (int i = 0; i < costDistributionItems.size(); i++) {

                CostDistributionItem costDistributionItem = costDistributionItems.get(i);
                CostDistributionItem costDistributionItemOriginal = costDistributionItemsOriginal.get(i);

                boolean costDistributionItemsEqual = EntityComparatorService.costDistributionItemsEqual(costDistributionItem, costDistributionItemOriginal);

                if (!costDistributionItemsEqual){
                    anythingChanged = true;
                }
            }
        }

        if (resetMenuButton != null && refreshView){
            if (anythingChanged){
                resetMenuButton.setVisible(true);
                Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.baseline_save_white_36);
                getSupportActionBar().setHomeAsUpIndicator(drawable);
            }else{
                resetMenuButton.setVisible(false);
                Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.baseline_keyboard_backspace_white_36);
                getSupportActionBar().setHomeAsUpIndicator(drawable);
            }
        }

//        if (anythingChanged){
//            List<IPaymentItem> paymentItems = paymentItemsForCorrectionMode(true);
//            for (IPaymentItem paymentItem : paymentItems) {
//                paymentItem.setCorrectionStatus(CorrectionStatus.CHECK);
//            }
//
//            MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
//            mainDatabaseHandler.updatePaymentItems(paymentItems);
//        }

        return anythingChanged;
    }

    private void showMistakeMessageDialog() {

        dateAndSumRl.requestFocus();

        FragmentManager fm = getSupportFragmentManager();
        DialogConflict dialogConflict = new DialogConflict();
        dialogConflict.setArguments(currentInvoice);

        dialogConflict.show(fm, "dialog_conflict");

    }

    public void deleteStandingOrder(){
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
        mainDatabaseHandler.deleteInvoiceFailures(allInvoiceFailures);


        List<Invoice> invoices = new ArrayList<Invoice>();
        invoices.add(currentInvoice);
        mainDatabaseHandler.deleteInvoices(invoices);

//        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE);
//        sqlBuilder.isEqual(MainDatabaseHandler.VAR_INVOICE_STANDING_ORDER_INVOICE_TEMPLATE_ID, currentInvoice.getInvoiceId().toString());
//
//        List<Invoice> invoices1 = mainDatabaseHandler.findInvoicesSql(sqlBuilder);
//        mainDatabaseHandler.deleteInvoices(invoices1);
    }

    public void takePicture(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File file = IOHelper.getTempUploadMediaFile(currentInvoice.getInvoiceId().toString());
        if (file.exists()){
            file.delete();
        }

        Uri photoURI = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".fileprovider", file);

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        startActivityForResult(intent, TAKE_PICTURE);
    }

    public void deletePicture(){
        this.doUploadImage = false;
        this.doDeleteImage = true;

        deletePictureFileWithourRequest();
    }

    public void deletePictureFileWithourRequest(){

        File file = imageExists();
        if (file != null){
            file.delete();
        }
        refreshImageView();

        refreshResetButtonView();
    }

    public MainFunctionEnum recognizeInvoiceType(Invoice invoice){
        AppUser currentUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_USER_CONTACT);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_APP_USER_CONTACT_ID, currentUser.getAppUserId().toString());

        List<UUID> currentUserUserContactsUUIDs = new ArrayList<>();
        List<UserContact> userContacts = mainDatabaseHandler.findUserContactsSql(sqlBuilder);
        for (UserContact userContact : userContacts) {
            currentUserUserContactsUUIDs.add(userContact.getUserContactId());
        }

        if (invoice.getPayerId() != null && invoice.getPayerId().equals(currentUser.getAppUserId())){
            return MainFunctionEnum.EXPENSE;
        }else if (invoice.getPaymentRecipientId() != null && invoice.getPaymentRecipientId().equals(currentUser.getAppUserId())){
            return MainFunctionEnum.INCOME;
        }else if (currentUserUserContactsUUIDs.contains(invoice.getPaymentRecipientId())){
            return MainFunctionEnum.INCOME;
        }else if (currentUserUserContactsUUIDs.contains(invoice.getPayerId())){
            return MainFunctionEnum.EXPENSE;
        }else if(invoice.getCreatedById() != null && invoice.getCreatedById().equals(invoice.getPayerId())){
            return MainFunctionEnum.EXPENSE;
        }else if(invoice.getCreatedById() != null && invoice.getCreatedById().equals(invoice.getPaymentRecipientId())) {
            return MainFunctionEnum.INCOME;
        }

        return MainFunctionEnum.EXPENSE;
    }

    public void hideSoftInput(){
        InputMethodManager inputMethodManager =
                (InputMethodManager) this.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                mInvoiceSumEt.getWindowToken(), 0);
        inputMethodManager.hideSoftInputFromWindow(
                mInvoiceDateEt.getWindowToken(), 0);
    }

    public void sendImageDownloadRequest(){
        if (hasPermission(MY_PERMISSIONS_REQUEST_DOWNLOAD_PICTURE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            sendImageDownloadRequestExecution();
        }


    }

    public void sendImageDownloadRequestExecution(){
        if (currentInvoice.getInvoiceId() == null || imageExists() != null || MainFunctionEnum.NEW_EXPENSE.equals(mainFunctionEnum) || MainFunctionEnum.NEW_INCOME.equals(mainFunctionEnum)){
//            progressBarImageDownload.setVisibility(View.INVISIBLE);
            return;
        }

//        progressBarImageDownload.setVisibility(View.VISIBLE);

        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        long rowId = handler.addObject(currentInvoice.getInvoiceId().toString(), StatusDatabaseHandler.OBJECT_TYPE_INVOICE_IMAGE, StatusDatabaseHandler.UPDATE_STATUS_GET, new Date().getTime(), 1);

        Intent sIntent = new Intent(getApplicationContext(), RequestUpdateService.class);
        sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
        getApplicationContext().startService(sIntent);
    }

    public void refreshViews(){
        if (this.currentInvoice != null){

            MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
            AppUser currentUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());

            if (isStandingOrder){
                standingOrderConfigRl.setVisibility(View.VISIBLE);

                if (currentInvoice.getRepetitionTypeEnum() != null){
                    standingOrderRepetitionTypeSpinner.setSelection(repetitionTypeEnums.indexOf(currentInvoice.getRepetitionTypeEnum()));
                }else{
                    standingOrderRepetitionTypeSpinner.setSelection(0);
                }

                Date tempDate = currentInvoice.getStandingOrderStartDate();
                if (tempDate != null) {
                    standingOrderDateTextWatcher.setResult(tempDate);
                    standingOrderDatePickerEt.setTextColor(Color.parseColor("#000000"));
                }else{
                    standingOrderDatePickerEt.setTextColor(Color.parseColor("#7a7a7a"));
                }
            }else{
                standingOrderConfigRl.setVisibility(View.GONE);
            }

            if (isStandingOrder){
//                fab.setVisibility(View.GONE);
            }else{
                boolean changeIconSymbol = false;
                if (CostDistributionHelper.isCreatedUserReadyMode(getApplicationContext(), currentInvoice)){
                    changeIconSymbol = true;
                }else if (CostDistributionHelper.isExternalUserReadyMode(getApplicationContext(), currentInvoice, costDistributionItems)){
                    changeIconSymbol = true;
                }

                if (changeIconSymbol){
                    String uri = "drawable/ic_lock_outline_black_48dp";
                    int imageResource = getApplicationContext().getResources().getIdentifier(uri, null, getApplicationContext().getPackageName());
                    Drawable image = getApplicationContext().getResources().getDrawable(imageResource);
                    fab.setImageDrawable(image);
                }
            }

            Date tempDate = currentInvoice.getDateOfInvoice();
            if (tempDate != null){
                dateTextWatcher.setResult(tempDate);
                mInvoiceDateEt.setTextColor(Color.parseColor("#000000"));
            } else {
                mInvoiceDateEt.setTextColor(Color.parseColor("#7a7a7a"));
            }

            if (paymentRecipient != null){
                String descriptionText = localizationForPaymentPersonTypeEnums.get(paymentRecipient.getVirtualPaymentPersonEnum());
                mInvoicePaymentRecipientButton.setText(paymentRecipient.getPaymentPersonName() + " (" + descriptionText + ")");
                mInvoicePaymentRecipientButton.setTextColor(Color.parseColor("#000000"));
            }else{
                mInvoicePaymentRecipientButton.setText("Zahlungsempfänger");
                mInvoicePaymentRecipientButton.setTextColor(Color.parseColor("#7a7a7a"));
            }

            if (payer != null){

                String descriptionText = localizationForPaymentPersonTypeEnums.get(payer.getVirtualPaymentPersonEnum());
                mInvoicePayerButton.setText(payer.getPaymentPersonName() + " (" + descriptionText + ")");
                mInvoicePayerButton.setTextColor(Color.parseColor("#000000"));
            }else{
                mInvoicePayerButton.setText("Geldgeber");
                mInvoicePayerButton.setTextColor(Color.parseColor("#7a7a7a"));
            }

            if (currentInvoice.getInvoiceFailureMessage() != null){
                invoiceFailureMessageTv.setText(currentInvoice.getInvoiceFailureMessage());
                invoiceFailureMessageTv.setVisibility(View.VISIBLE);
            }else{
                invoiceFailureMessageTv.setVisibility(View.GONE);
            }

            if (currentUser != null && !currentInvoice.getCreatedById().equals(currentUser.getAppUserId())){
                List<AppUser> appUsers = mainDatabaseHandler.findAppUsers(MainDatabaseHandler.VAR_APP_USER_ID, currentInvoice.getCreatedById().toString());
                isExternInvoiceTv.setText("Rechnung von " + appUsers.get(0).getPaymentPersonName());
                isExternInvoiceTv.setVisibility(View.VISIBLE);

//                SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_USER_CONTACT);
//                sqlBuilder.isEqual(MainDatabaseHandler.VAR_APP_USER_CONTACT_ID, currentUser.getAppUserId().toString());

//                List<UUID> currentUserUserContactsUUIDs = new ArrayList<>();
//                List<UserContact> userContacts = mainDatabaseHandler.findUserContactsSql(sqlBuilder.getSqlString(),sqlBuilder.getValues());
//                for (UserContact userContact : userContacts) {
//                    currentUserUserContactsUUIDs.add(userContact.getUserContactId());
//                }
//
//                if (currentUserUserContactsUUIDs.contains(currentInvoice.getPaymentRecipientId()) || currentUserUserContactsUUIDs.contains(currentInvoice.getPayerId())){
//                    costDistributionItemsRl.setVisibility(View.GONE);
//                }else{
//                    costDistributionItemsRl.setVisibility(View.VISIBLE);
//                }
            }else{
                isExternInvoiceTv.setVisibility(View.GONE);
            }

            if (currentInvoice.getSumOfInvoice() != null){
                currencyTextWatcher.setSum(currentInvoice.getSumOfInvoice());
                mInvoiceSumEt.setTextColor(Color.parseColor("#000000"));
            }else{
                mInvoiceSumEt.setTextColor(Color.parseColor("#7a7a7a"));
            }

            if (currentInvoice.getInvoiceCategoryDTO() != null){
                SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE_CATEGORY);
                sqlBuilder.isEqual(MainDatabaseHandler.VAR_INVOICE_CATEGORY_ID, currentInvoice.getInvoiceCategoryDTO().getInvoiceCategoryId().toString());
                List<InvoiceCategory> invoiceCategories = mainDatabaseHandler.findInvoiceCategoriesSql(sqlBuilder);
                if (!invoiceCategories.isEmpty()){
                    String parentInvoiceCategoryText = "";
                    if (invoiceCategories.get(0).getParentInvoiceCategoryDTO() != null){
                        SqlBuilder sqlBuilder2 = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE_CATEGORY);
                        sqlBuilder2.isEqual(MainDatabaseHandler.VAR_INVOICE_CATEGORY_ID, invoiceCategories.get(0).getParentInvoiceCategoryDTO().getInvoiceCategoryId().toString());
                        List<InvoiceCategory> parentInvoiceCategories = mainDatabaseHandler.findInvoiceCategoriesSql(sqlBuilder2);


                        if (!parentInvoiceCategories .isEmpty()){
                            parentInvoiceCategoryText = parentInvoiceCategories.get(0).getInvoiceCategoryName() + " ➡ ";
                        }
                    }
                    invoiceInvoiceCategoryButton.setText(parentInvoiceCategoryText + invoiceCategories.get(0).getInvoiceCategoryName());
                    invoiceInvoiceCategoryButton.setTextColor(Color.parseColor("#000000"));
                }
            }else{
                invoiceInvoiceCategoryButton.setText("Kategorie");
                invoiceInvoiceCategoryButton.setTextColor(Color.parseColor("#7a7a7a"));
            }

            if (currentInvoice.getPaymentTypeEnum() != null){
                invoicePaymentTypeSpinner.setSelection(paymentTypeEnums.indexOf(currentInvoice.getPaymentTypeEnum()));
            }else{
                invoicePaymentTypeSpinner.setSelection(0);
            }

            if (currentInvoice.getSpecialType() != null && currentInvoice.getSpecialType() == true){
                specialTypeSw.setChecked(true);
            }else{
                specialTypeSw.setChecked(false);
            }

            if (currentInvoice.getRemarks() != null){
                invoiceRemarksEt.setText(currentInvoice.getRemarks());
            }else{
                invoiceRemarksEt.setText("");
            }

            refreshImageView();

            refreshCostDistributionItemsView();

            if (currentInvoice.getInvoiceId() != null){
                mInvoiceDateEt.setBackgroundColor(0);
                mInvoiceSumEt.setBackgroundColor(0);
                mInvoicePayerButton.setBackgroundColor(0);
                mInvoicePaymentRecipientButton.setBackgroundColor(0);
            }

            if (mainFunctionEnum.equals(MainFunctionEnum.EXPENSE) || mainFunctionEnum.equals(MainFunctionEnum.NEW_EXPENSE)){
                payerRl.setVisibility(View.GONE);
                specialDividerRl.setVisibility(View.GONE);
                paymentRecipientRl.setVisibility(View.VISIBLE);
                paymentRecipientLineView.setVisibility(View.VISIBLE);
                this.setTitle("Ausgabe");
            }else if (mainFunctionEnum.equals(MainFunctionEnum.INCOME) || mainFunctionEnum.equals(MainFunctionEnum.NEW_INCOME)){
                payerRl.setVisibility(View.VISIBLE);
                specialDividerRl.setVisibility(View.VISIBLE);
                paymentRecipientRl.setVisibility(View.GONE);
                paymentRecipientLineView.setVisibility(View.GONE);
                this.setTitle("Einnahme");
            }

            refreshMustBeBilledButtons();

            refreshResetButtonView();
        }
    }

    public void refreshMustBeBilledButtons(){
        if (mainFunctionEnum.equals(MainFunctionEnum.EXPENSE) || mainFunctionEnum.equals(MainFunctionEnum.NEW_EXPENSE)){
            if (currentInvoice.getPaymentRecipientTypeEnum() != null &&
                    (currentInvoice.getPaymentRecipientTypeEnum().equals(PaymentPersonTypeEnum.CONTACT) || currentInvoice.getPaymentRecipientTypeEnum().equals(PaymentPersonTypeEnum.USER)) &&
                    !currentInvoice.getPaymentRecipientId().equals(currentInvoice.getCreatedById()) &&
                    currentInvoice.getMoneyValue().compareTo(new BigDecimal(0)) != 0){
                String uri;
                if ((currentInvoice.getCostPaid() == null && currentInvoice.getMoneyValue().compareTo(new BigDecimal(0)) != 0) ||
                        (currentInvoice.getCostPaid() != null && currentInvoice.getCostPaid().compareTo(currentInvoice.getMoneyValue()) != 0)){
                    uri = "drawable/ic_clear_black_48dp";
                }else{
                    uri = "drawable/money";
                }
                int imageResource = getApplicationContext().getResources().getIdentifier(uri, null, getApplicationContext().getPackageName());
                Drawable image = getApplicationContext().getResources().getDrawable(imageResource);
                mustBeBilled2Ib.setImageDrawable(image);
//                mustBeBilled2Ib.setVisibility(View.VISIBLE);

            }else{
//                mustBeBilled2Ib.setVisibility(View.GONE);
            }
        }else if (mainFunctionEnum.equals(MainFunctionEnum.INCOME) || mainFunctionEnum.equals(MainFunctionEnum.NEW_INCOME)){

            if (currentInvoice.getPayerTypeEnum() != null &&
                    (currentInvoice.getPayerTypeEnum().equals(PaymentPersonTypeEnum.CONTACT) || currentInvoice.getPayerTypeEnum().equals(PaymentPersonTypeEnum.USER)) &&
                    !currentInvoice.getPayerId().equals(currentInvoice.getCreatedById()) &&
                    currentInvoice.getMoneyValue().compareTo(new BigDecimal(0)) != 0){
                String uri;
                if ((currentInvoice.getCostPaid() == null && currentInvoice.getMoneyValue().compareTo(new BigDecimal(0)) != 0) ||
                        (currentInvoice.getCostPaid() != null && currentInvoice.getCostPaid().compareTo(currentInvoice.getMoneyValue()) != 0)){
                    uri = "drawable/ic_clear_black_48dp";
                }else{
                    uri = "drawable/money";
                }
                int imageResource = getApplicationContext().getResources().getIdentifier(uri, null, getApplicationContext().getPackageName());
                Drawable image = getApplicationContext().getResources().getDrawable(imageResource);
                mustBeBilledIb.setImageDrawable(image);
//                mustBeBilledIb.setVisibility(View.VISIBLE);
            }else {
//                mustBeBilledIb.setVisibility(View.GONE);
            }
        }

        if (currentInvoice.getCostPaid() != null && currentInvoice.getCostPaid().compareTo(new BigDecimal(0)) != 0){
            currentInvoice.setCostPaid(currentInvoice.getSumOfInvoice());
        }

        for (CostDistributionItem costDistributionItem : costDistributionItems) {
            if (costDistributionItem.getCostPaid() != null && costDistributionItem.getCostPaid().compareTo(new BigDecimal(0)) != 0){
                costDistributionItem.setCostPaid(costDistributionItem.getMoneyValue());
            }
        }
    }

    public void tryAutoFillBusinessPartnerDependencies(){
        if (currentInvoice.getInvoiceCategoryDTO() == null && paymentRecipient != null){
            InvoiceCategory invoiceCategory = StatisticHelper.findCategoryForBusinessPartner(getApplicationContext(), paymentRecipient);
            if (invoiceCategory != null){
                currentInvoice.setInvoiceCategoryDTO(invoiceCategory);
            }
        }
    }

    public void tryAutoFillInvoiceCategoryDependencies(){
        if (currentInvoice.getInvoiceCategoryDTO() != null){
            AppUser currentUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());

            if (costDistributionItems.size() == 1 && costDistributionItems.get(0).getValue().compareTo(new BigDecimal(1)) == 0 && costDistributionItems.get(0).getPayerId().equals(currentUser.getAppUserId())){

                CostDistribution costDistribution = StatisticHelper.findCostDistributionForCategory(getApplicationContext(), currentInvoice.getInvoiceCategoryDTO().getInvoiceCategoryId());
                if (costDistribution != null){
                    MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
                    List<CostDistributionItem> costDistributionItems = mainDatabaseHandler.findCostDistributionItems(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ID, costDistribution.getCostDistributionId().toString());

                    List<CostDistributionItem> newCostDistributionItems = new ArrayList<>();
                    for (CostDistributionItem costDistributionItem : costDistributionItems) {
                        CostDistributionItem costDistributionItem1 = CostDistributionHelper.prepareItemForCostDistribution(costDistributionItem, null);
                        costDistributionItem1.setMoneyValue(CostDistributionHelper.calculateAmountForCostDistributionItemPrecise(costDistributionItem, costDistributionItems, currentInvoice.getSumOfInvoice()));
                        costDistributionItem1.setInvoiceId(currentInvoice.getInvoiceId());
                        costDistributionItem1.setCostDistributionId(null);
                        newCostDistributionItems.add(costDistributionItem1);
                    }

                    costDistributionItemsToDelete.addAll(this.costDistributionItems);
                    this.costDistributionItems = newCostDistributionItems;
                }
            }

            if (currentInvoice.getSpecialType() == null || currentInvoice.getSpecialType() == false){
                Boolean specialTypeForCategoryTmp = StatisticHelper.findSpecialTypeForCategory(getApplicationContext(), currentInvoice.getInvoiceCategoryDTO().getInvoiceCategoryId());

                if (specialTypeForCategoryTmp != null){
                    currentInvoice.setSpecialType(specialTypeForCategoryTmp);
                }
            }
        }
    }

    public void refreshCostDistributionItemsView(){
        if (costDistributionItems != null && !costDistributionItems.isEmpty()){
            String costDistributionText = "";
            mInvoiceCostDistributionButton.setTextColor(Color.parseColor("#000000"));

            for (int i = 0; i < costDistributionItems.size(); i++) {

                BigDecimal price = costDistributionItems.get(i).getMoneyValue();
                String paid = "";

                if ((costDistributionItems.get(i).getCostPaid() != null && price.compareTo(costDistributionItems.get(i).getCostPaid()) == 0)
                        || ((costDistributionItems.get(i).getPayerId().equals(currentInvoice.getPayerId()) || costDistributionItems.get(i).getPayerId().equals(currentInvoice.getPaymentRecipientId())) &&
                        (!PaymentPersonTypeEnum.CONTACT.equals(currentInvoice.getPayerTypeEnum()) && !PaymentPersonTypeEnum.CONTACT.equals(currentInvoice.getPaymentRecipientTypeEnum())))){
                    paid = " \uD83D\uDCB0";
                }
                String costDistributionItemString = costDistributionItems.get(i).getPaymentPersonName(getApplicationContext()) + ": " + numberFormatter.format(price) + " €"+paid;

                if (i == 0){
                    costDistributionText = costDistributionItemString;
                }else if (i % 2 != 0 || i == 1){
                    costDistributionText = costDistributionText +" - "+ costDistributionItemString;
                }else if (i % 2 == 0){
                    costDistributionText = costDistributionText + "\n" + costDistributionItemString;
                }

            }

            mInvoiceCostDistributionButton.setText(costDistributionText);
        }else{
            if (mainFunctionEnum.equals(MainFunctionEnum.EXPENSE) || mainFunctionEnum.equals(MainFunctionEnum.NEW_EXPENSE)){
                mInvoiceCostDistributionButton.setText("Kostenverteilung");
            }else if (mainFunctionEnum.equals(MainFunctionEnum.INCOME) || mainFunctionEnum.equals(MainFunctionEnum.NEW_INCOME)){
                mInvoiceCostDistributionButton.setText("Einnahmenverteilung");
            }
            mInvoiceCostDistributionButton.setTextColor(Color.parseColor("#7a7a7a"));
        }
    }

    public void refreshImageView(){
        if (hasPermission(MY_PERMISSIONS_REQUEST_VIEW_PICTURE, Manifest.permission.READ_EXTERNAL_STORAGE)){
            File file = imageExists();
            if (file != null){
                Bitmap myImg = BitmapFactory.decodeFile(file.getAbsolutePath());

                if (myImg != null){
                    Matrix matrix = new Matrix();

                    Bitmap rotated = Bitmap.createBitmap(myImg, 0, 0, myImg.getWidth(), myImg.getHeight(),
                            matrix, true);

                    placeholderTv.setVisibility(View.GONE);
                    mInvoiceReceiptIv.setVisibility(View.VISIBLE);

                    mInvoiceReceiptIv.setImageBitmap(rotated);

                    Integer toolbarHeight = mInvoiceReceiptIv.getDrawable().getIntrinsicHeight();
                    configureToolbar(toolbarHeight);
                }

            }else{
                placeholderTv.setVisibility(View.VISIBLE);
                mInvoiceReceiptIv.setVisibility(View.GONE);

//                configureToolbar(300);
            }
        }
    }

    public File imageExists(){
        if (currentInvoice == null){
            return null;
        }

        IOHelper ioHelper = IOHelper.getInstance();
        ioHelper.setCtx(getApplicationContext());

        String pathTmp = ioHelper.getImageDirectory();
        File file = new File(pathTmp + "/" +currentInvoice.getInvoiceId().toString() + "_thumbnail");

        if (file.exists()){
            return file;
        }

        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        IOHelper.getInstance().setCtx(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(RequestManager.BC_ONLINE);
        filter.addAction(RequestManager.BC_SEND_MESSAGE);
        registerReceiver(updateRequestReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(updateRequestReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();
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
            case MY_PERMISSIONS_REQUEST_VIEW_PICTURE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    refreshImageView();
                } else {
//                    Toast.makeText(getApplicationContext(), "Bitte erteilen Sie den Lesezugriff auf die Speicherkarte! Andernfalls keine anzeige von Quittungsfotos möglich!", Toast.LENGTH_LONG).show();
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_DOWNLOAD_PICTURE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendImageDownloadRequestExecution();
                } else {
//                    Toast.makeText(getApplicationContext(), "Bitte erteilen Sie den Lesezugriff auf die Speicherkarte! Andernfalls kein Download von Quittungsfotos möglich!", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == TAKE_PICTURE && resultCode == RESULT_OK) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            startUploadImageService();
            refreshResetButtonView();
        } else if (requestCode == COST_DISTRIBUTION && resultCode == RESULT_OK) {

            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            Type listType = new TypeToken<List<CostDistributionItem>>() {
            }.getType();

            costDistributionItemsToDelete.addAll(this.costDistributionItems);
            costDistributionItems = gson.fromJson(intent.getStringExtra("costDistributionItemsResult"), listType);

            refreshViews();
        } else if (requestCode == LOGIN && resultCode == RESULT_OK) {
            Intent rIntent = new Intent(this, RequestUpdateService.class);
            rIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
            startService(rIntent);
        } else if (requestCode == ARTICLE_SELECT && resultCode == RESULT_OK) {

            String clazz = intent.getStringExtra("paymentItemClass");

            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();

            Type listType = new TypeToken<List<ArticleDTO>>() {
            }.getType();

            List<ArticleDTO> articleDTOS = gson.fromJson(intent.getStringExtra("articleDTOS"), listType);

            if (clazz.equals(Invoice.class.getSimpleName())) {
                currentInvoice.setSumOfInvoice(BigDecimal.valueOf(intent.getDoubleExtra("sum", 0)));
                currentInvoice.setArticleDTOs(articleDTOS);
            }

            for (CostDistributionItem costDistributionItem : costDistributionItems) {
                costDistributionItem.setMoneyValue(CostDistributionHelper.calculateAmountForCostDistributionItemPrecise(costDistributionItem, costDistributionItems, currentInvoice.getSumOfInvoice()));
            }

            refreshCostDistributionItemsView();

            refreshMustBeBilledButtons();

            refreshResetButtonView();

            refreshViews();
        }
    }

    public void showActivityArticlesSelect(){

        StatusDatabaseHandler statusDatabaseHandler = StatusDatabaseHandler.getInstance(getApplicationContext());
        int status = statusDatabaseHandler.getStatus();
        if (status == StatusDatabaseHandler.STATUS_MANUAL_OFFLINE) {
            Toast.makeText(getApplicationContext(), "Offline-Modus aktiv! Funktion nicht ausführbar.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(ActivityInvoiceDetails.this, ActivityArticlesSelect.class);

        IPaymentPerson paymentPerson = MainFunctionEnum.EXPENSE.equals(mainFunctionEnum) ? paymentRecipient : payer;

        intent.putExtra("payer", paymentPerson.getPaymentPersonName());
        intent.putExtra("paymentItemId", currentInvoice.getInvoiceId().toString());
        intent.putExtra("paymentItemClazz", Invoice.class.getSimpleName());
        intent.putExtra("invoiceId", currentInvoice.getInvoiceId().toString());

        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        Type listType = new TypeToken<List<ArticleDTO>>() {}.getType();

        String articleDTOsAsJson = gson.toJson(currentInvoice.getArticleDTOs(), listType);
        intent.putExtra("articleDTOS", articleDTOsAsJson);

        startActivityForResult(intent, ARTICLE_SELECT);
        overridePendingTransition(R.anim.activity_in1, R.anim.activity_in2);
    }

    protected void startUploadImageService(){
        Toast.makeText(getApplicationContext(), "Bitte warten, Bild wird berechnet...", Toast.LENGTH_LONG).show();

        this.doDeleteImage = false;
        this.doUploadImage = true;

        new GenerateInvoiceThumbnail().execute(this.currentInvoice.getInvoiceId().toString());
    }

    private void showInvoiceCategorySelectDialog() {

        dateAndSumRl.requestFocus();

        List<InvoiceCategory> invoiceCategories = new ArrayList<>();

        InvoiceCategoryType invoiceCategoryType = null;
        if (MainFunctionEnum.EXPENSE.equals(mainFunctionEnum) || MainFunctionEnum.NEW_EXPENSE.equals(mainFunctionEnum)){
            invoiceCategoryType = InvoiceCategoryType.EXPENSE;
        }else if(MainFunctionEnum.INCOME.equals(mainFunctionEnum) || MainFunctionEnum.NEW_INCOME.equals(mainFunctionEnum)){
            invoiceCategoryType = InvoiceCategoryType.INCOME;
        }

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE_CATEGORY);
        sqlBuilder.
                isNull(MainDatabaseHandler.VAR_PARENT_INVOICE_CATEGORY_ID).
                and().
                isEqual(MainDatabaseHandler.VAR_BASIC_STATUS_ENUM, BasicStatusEnum.OK.name())
        .and()
            .startBracket()
                .isNull(MainDatabaseHandler.VAR_INVOICE_CATEGORY_TYPE)
                .or()
                .isEqual(MainDatabaseHandler.VAR_INVOICE_CATEGORY_TYPE, invoiceCategoryType.name())
            .endBracket();
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

    private void showCostDistributionSelectDialog() {

        dateAndSumRl.requestFocus();

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());

        List<CostDistribution> costDistributions = mainDatabaseHandler.findCostDistributions();
        if (costDistributions.isEmpty()){

            Intent intent = new Intent(this, ActivityCostDistribution.class);

            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            String costDistributionItemsAsString = gson.toJson(costDistributionItems);

            intent.putExtra("costDistributionItems", costDistributionItemsAsString);

            String sumOfInvoice = "0.0";
            if (currentInvoice.getSumOfInvoice() != null){
                BigDecimal sumOfInvoiceDecimal = currentInvoice.getSumOfInvoice().setScale(2, BigDecimal.ROUND_HALF_EVEN);
                sumOfInvoice = sumOfInvoiceDecimal.toString();
            }
            intent.putExtra("sum", sumOfInvoice);
            intent.putExtra("invoiceId", currentInvoice.getInvoiceId().toString());
            intent.putExtra("showArticleSelect", imageExists() != null && (!this.doUploadImage && !this.doDeleteImage) && RightHelper.hasUserRight(getApplicationContext(), Right.OCR));
            startActivityForResult(intent, COST_DISTRIBUTION);
        }else{
            FragmentManager fm = getSupportFragmentManager();
            DialogCostDistribution selectCostDistributionDialog = new DialogCostDistribution();

            selectCostDistributionDialog.setArguments(costDistributions);

            selectCostDistributionDialog.show(fm, "dialog_cost_distribution_select");
        }

    }

    private void showInvoicePayerEditDialog() {
        dateAndSumRl.requestFocus();

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
        SqlBuilder sqlBuilder2 = new SqlBuilder(MainDatabaseHandler.TABLE_BUSINESS_PARTNER);
        sqlBuilder2
                .startBracket()
                    .isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString())
                    .or()
                    .isNull(MainDatabaseHandler.VAR_APP_USER_ID)
                .endBracket()
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_STATUS_ENUM, BasicStatusEnum.OK.name());
        List<BusinessPartner> businessPartners = mainDatabaseHandler.findBusinessPartnersSql(sqlBuilder2);
        Collections.sort(businessPartners, new BusinessPartnerComparator());

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

        List<IPaymentPerson> userContactsAndProjects = new ArrayList<>();
        userContactsAndProjects.addAll(mainDatabaseHandler.findUserContactsSql(sqlBuilder2));
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
        projects.addAll(mainDatabaseHandler.findUserContactsSql(sqlBuilder2));
        Collections.sort(projects, new PaymentPersonComparator());
        userContactsAndProjects.addAll(projects);

        editPayerDialog.setArguments(InvoiceDetailsPaymentPersonDialogEnum.PAYER, paymentPersonTypeEnumList, businessPartners, appUsers, userContactsAndProjects, true, true, true);

        editPayerDialog.show(fm, "dialog_payment_person");
    }

    private void showInvoicePaymentRecipientEditDialog() {
        dateAndSumRl.requestFocus();

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
        SqlBuilder sqlBuilder2 = new SqlBuilder(MainDatabaseHandler.TABLE_BUSINESS_PARTNER);
        sqlBuilder2
                .startBracket()
                    .isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString())
                    .or()
                    .isNull(MainDatabaseHandler.VAR_APP_USER_ID)
                .endBracket()
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_STATUS_ENUM, BasicStatusEnum.OK.name());
        List<BusinessPartner> businessPartners = mainDatabaseHandler.findBusinessPartnersSql(sqlBuilder2);
        Collections.sort(businessPartners, new BusinessPartnerComparator());

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

        editPayerDialog.setArguments(InvoiceDetailsPaymentPersonDialogEnum.PAYMENT_RECIPIENT, paymentPersonTypeEnumList, businessPartners, appUsers, userContactsAndProjects, true, true, true);

        editPayerDialog.show(fm, "dialog_payment_person");
    }


    @Override
    public void onFinishDateEditDialog(Date dateOfInvoice) {
        currentInvoice.setDateOfInvoice(dateOfInvoice);
        refreshViews();
    }

    @Override
    public void onFinishPaymentPersonSelectDialog(InvoiceDetailsPaymentPersonDialogEnum invoiceDetailsPaymentPersonDialogEnum, IPaymentPerson iPaymentPerson) {
        if (iPaymentPerson != null) {

            if (invoiceDetailsPaymentPersonDialogEnum.equals(InvoiceDetailsPaymentPersonDialogEnum.PAYER)) {
                payer = iPaymentPerson;
                currentInvoice.setPayer(payer);

                if (iPaymentPerson.getVirtualPaymentPersonEnum().equals(PaymentPersonTypeEnum.USER)) {
                    currentInvoice.setCorrectionStatus(CorrectionStatus.CHECK);
                }else if (iPaymentPerson.getVirtualPaymentPersonEnum().equals(PaymentPersonTypeEnum.CONTACT)){
                    currentInvoice.setCorrectionStatus(CorrectionStatus.READY);
                }else{
                    currentInvoice.setCorrectionStatus(CorrectionStatus.IGNORE);
                }
            } else if (invoiceDetailsPaymentPersonDialogEnum.equals(InvoiceDetailsPaymentPersonDialogEnum.PAYMENT_RECIPIENT)) {
                paymentRecipient = iPaymentPerson;
                currentInvoice.setPaymentRecipient(paymentRecipient);

                if (iPaymentPerson.getVirtualPaymentPersonEnum().equals(PaymentPersonTypeEnum.USER)){
                    currentInvoice.setCorrectionStatus(CorrectionStatus.CHECK);
                }else if (iPaymentPerson.getVirtualPaymentPersonEnum().equals(PaymentPersonTypeEnum.CONTACT)) {
                    currentInvoice.setCorrectionStatus(CorrectionStatus.READY);
                }else{
                    currentInvoice.setCorrectionStatus(CorrectionStatus.IGNORE);
                }

                tryAutoFillBusinessPartnerDependencies();
                tryAutoFillInvoiceCategoryDependencies();
            }
        }

        refreshViews();
    }

    public void unlockViews(boolean unlock){
        if (isStandingOrder){
            mInvoiceDateEt.setEnabled(false);
        }else{
            mInvoiceDateEt.setEnabled(unlock);
        }
        mInvoicePaymentRecipientButton.setEnabled(unlock);
        mInvoicePayerButton.setEnabled(unlock);
        isExternInvoiceTv.setEnabled(unlock);
        costDistributionItemsRl.setEnabled(unlock);
        mInvoiceSumEt.setEnabled(unlock);
        invoiceInvoiceCategoryButton.setEnabled(unlock);

        specialTypeSw.setEnabled(unlock);
        invoiceRemarksEt.setEnabled(unlock);
        mInvoiceCostDistributionButton.setEnabled(unlock);
        payerRl.setEnabled(unlock);
        paymentRecipientRl.setEnabled(unlock);
    }

    public void unlockMenu(boolean unlock){
        resetMenuButton.setVisible(unlock);
//        deleteMenuButton.setVisible(unlock);

        resetMenuButton.setEnabled(unlock);
//        deleteMenuButton.setEnabled(unlock);
    }

    public void deleteInvoice(){

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());

        if (currentInvoice.getInvoiceId() != null){
            StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
            handler.addObject(currentInvoice.getInvoiceId().toString(), StatusDatabaseHandler.OBJECT_TYPE_INVOICE, StatusDatabaseHandler.UPDATE_STATUS_DELETE, new Date().getTime(), 1);
            Intent sIntent = new Intent(getApplicationContext(), RequestUpdateService.class);
            sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
            getApplicationContext().startService(sIntent);
        }


        mainDatabaseHandler.deleteInvoiceFailures(allInvoiceFailures);

        List<Invoice> invoices = new ArrayList<Invoice>();
        invoices.add(currentInvoice);
        mainDatabaseHandler.deleteInvoices(invoices);
    }

    public boolean saveAndUpload(){

        if (!refreshResetButtonView(false)){
            return false;
        }

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());

        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());

        if (InvoiceStatusEnum.READY.equals(currentInvoice.getInvoiceStatusEnum())){
            for (InvoiceFailure invoiceFailure: allInvoiceFailures) {
                handler.addObject(invoiceFailure.getInvoiceFailureId().toString(), StatusDatabaseHandler.OBJECT_TYPE_INVOICE_FAILURE, StatusDatabaseHandler.UPDATE_STATUS_DELETE, new Date().getTime(), 1);
            }
            mainDatabaseHandler.deleteInvoiceFailures(allInvoiceFailures);
        }

        if (repetitionTypeEnum == null){
            currentInvoice.setRepetitionTypeEnum(RepetitionTypeEnum.ONCE);
        }

        if (paymentTypeEnum != null){
            currentInvoice.setPaymentTypeEnum(paymentTypeEnums.get(invoicePaymentTypeSpinner.getSelectedItemPosition()));
        }else{
            currentInvoice.setPaymentTypeEnum(PaymentTypeEnum.NOT_DEFINED);
        }

        if (payer != null){
            IPaymentPerson newPaymentPerson = mainDatabaseHandler.insertNewPaymentPersonAndAddStatusHandlerObject(getApplicationContext(), payer);
            currentInvoice.setPayer(newPaymentPerson);
        }

        if ( paymentRecipient != null){
            IPaymentPerson newPaymentPerson = mainDatabaseHandler.insertNewPaymentPersonAndAddStatusHandlerObject(getApplicationContext(), paymentRecipient);
            currentInvoice.setPaymentRecipient(newPaymentPerson);
        }

        currentInvoice.setRemarks(invoiceRemarksEt.getText().toString());
        currentInvoice.setSpecialType(specialTypeSw.isChecked());

        UUID randomId = UUID.randomUUID();
        if (mainFunctionEnum.equals(MainFunctionEnum.NEW_EXPENSE) || mainFunctionEnum.equals(MainFunctionEnum.NEW_INCOME)){
            if (this.isStandingOrder){
                currentInvoice.setStandingOrderInvoiceTemplateId(randomId);
            }

            mainDatabaseHandler.insertInvoice(currentInvoice);
            handler.addObject(currentInvoice.getInvoiceId().toString(), StatusDatabaseHandler.OBJECT_TYPE_INVOICE, StatusDatabaseHandler.UPDATE_STATUS_ADD, new Date().getTime(), 1);
        }else{
            mainDatabaseHandler.updateInvoice(currentInvoice);
            handler.addObject(currentInvoice.getInvoiceId().toString(), StatusDatabaseHandler.OBJECT_TYPE_INVOICE, StatusDatabaseHandler.UPDATE_STATUS_PUT, new Date().getTime(), 1);
        }

        for (CostDistributionItem costDistributionItem : costDistributionItemsToDelete) {

            List<CostDistributionItem> costDistributionItems = new ArrayList<>();
            costDistributionItems.add(costDistributionItem);
            mainDatabaseHandler.deleteCostDistributionItems(costDistributionItems);
            handler.addObject(costDistributionItem.getCostDistributionItemId().toString(), StatusDatabaseHandler.OBJECT_TYPE_COST_DISTRIBUTION_ITEM, StatusDatabaseHandler.UPDATE_STATUS_DELETE, new Date().getTime(), 1);
        }

        for (CostDistributionItem costDistributionItem : costDistributionItems) {

            mainDatabaseHandler.insertNewPaymentPersonAndAddStatusHandlerObject(getApplicationContext(), costDistributionItem.getPayer(getApplicationContext()));
            costDistributionItem.setInvoiceId(currentInvoice.getInvoiceId());

            List<CostDistributionItem> costDistributionItems = mainDatabaseHandler.findCostDistributionItems(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ITEM_ID, costDistributionItem.getCostDistributionItemId().toString());
            if (costDistributionItems.isEmpty()){
                mainDatabaseHandler.insertCostDistributionItem(costDistributionItem);
                handler.addObject(costDistributionItem.getCostDistributionItemId().toString(), StatusDatabaseHandler.OBJECT_TYPE_COST_DISTRIBUTION_ITEM, StatusDatabaseHandler.UPDATE_STATUS_ADD, new Date().getTime(), 1);
            }else{
                mainDatabaseHandler.updateCostDistributionItem(costDistributionItem);
                handler.addObject(costDistributionItem.getCostDistributionItemId().toString(), StatusDatabaseHandler.OBJECT_TYPE_COST_DISTRIBUTION_ITEM, StatusDatabaseHandler.UPDATE_STATUS_PUT, new Date().getTime(), 1);
            }
        }

        if (this.isStandingOrder){
            StandingOrder standingOrder = new StandingOrder();
            standingOrder.setInvoiceTemplateId(currentInvoice.getInvoiceId());
            standingOrder.setStartDate(currentInvoice.getStandingOrderStartDate());
            standingOrder.setRepetitionTypeEnum(currentInvoice.getRepetitionTypeEnum());
            standingOrder.setFutureInvoiceTemplateId(randomId);

            Gson gson = new GsonBuilder().setDateFormat("dd MM yyyy HH:mm:ss").create();
            String jsonStandingOrderString = gson.toJson(standingOrder);

            if (mainFunctionEnum.equals(MainFunctionEnum.NEW_EXPENSE) || mainFunctionEnum.equals(MainFunctionEnum.NEW_INCOME)){
                handler.addObject(jsonStandingOrderString, StatusDatabaseHandler.OBJECT_TYPE_STANDING_ORDER, StatusDatabaseHandler.UPDATE_STATUS_ADD, new Date().getTime(), 1);
            }else{
                handler.addObject(jsonStandingOrderString, StatusDatabaseHandler.OBJECT_TYPE_STANDING_ORDER, StatusDatabaseHandler.UPDATE_STATUS_PUT, new Date().getTime(), 1);
            }
        }

        if (this.doDeleteImage) {
            handler.addObject(currentInvoice.getInvoiceId().toString(), StatusDatabaseHandler.OBJECT_TYPE_INVOICE_IMAGE, StatusDatabaseHandler.UPDATE_STATUS_DELETE, new Date().getTime(), 1);
        }

        if (this.doUploadImage) {
            handler.addObject(currentInvoice.getInvoiceId().toString(), StatusDatabaseHandler.OBJECT_TYPE_INVOICE_IMAGE, StatusDatabaseHandler.UPDATE_STATUS_PUT, new Date().getTime(), 1);
        }

        Intent sIntent = new Intent(getApplicationContext(), RequestUpdateService.class);
        sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
        getApplicationContext().startService(sIntent);

        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.invoice_details, menu);
        checkMenuBtton = menu.findItem(R.id.checkMenuBtton);
//        unlockMenuBtton = menu.findItem(R.id.unlockMenuBtton);
        createStandingOrder = menu.findItem(R.id.createStandingOrder);
        deleteStandingOrder = menu.findItem(R.id.deleteStandingOrder);
        resetMenuButton = menu.findItem(R.id.resetMenuButton);
        deleteMenuButton = menu.findItem(R.id.deleteMenuButton);
        deleteSubMenuButton = menu.findItem(R.id.deleteSubMenuButton);
        addImage = menu.findItem(R.id.addImage);
        deleteImage = menu.findItem(R.id.deleteImage);
        problemBtn = menu.findItem(R.id.problemBtn);
        articleSelectionMenuButton = menu.findItem(R.id.articleSelection);
        refreshResetButtonView();

        if (!RightHelper.hasUserRight(getApplicationContext(), Right.OCR)){
            articleSelectionMenuButton.setEnabled(false);
        }else{
            if (!this.doDeleteImage && !this.doUploadImage){
                articleSelectionMenuButton.setVisible(true);
            }else{
                articleSelectionMenuButton.setVisible(false);
            }
        }

        if (CostDistributionHelper.isExternalUserCheckMode(getApplicationContext(), currentInvoice, costDistributionItems)) {
            problemBtn.setVisible(true);
        }else if (CostDistributionHelper.isCreatedUserCheckMode(getApplicationContext(), currentInvoice)){
            problemBtn.setVisible(false);
        }

        if (isReadyMode){
            checkMenuBtton.setVisible(true);
        }

        if (currentInvoice != null){
            if (isStandingOrder){
                createStandingOrder.setVisible(false);
                deleteStandingOrder.setVisible(false);
            }else{
                if (currentInvoice.getStandingOrderInvoiceTemplateId() != null){
                    createStandingOrder.setVisible(false);
                    deleteStandingOrder.setVisible(true);
                }else{
                    createStandingOrder.setVisible(true);
                    deleteStandingOrder.setVisible(false);
                }
            }

            refreshImageMenuItems();
        }

//        if (currentInvoice.getInvoiceStatusEnum().equals(InvoiceStatusEnum.CHECK)) {
//            unlockMenu(true);
//        }else if (currentInvoice.getInvoiceStatusEnum().equals(InvoiceStatusEnum.READY)){
//            unlockMenu(false);
//        }

        return true;
    }

    public void refreshImageMenuItems(){
        if (imageExists() != null){
            deleteImage.setVisible(true);
            addImage.setVisible(false);
            if (!this.doDeleteImage && !this.doUploadImage){
                articleSelectionMenuButton.setVisible(true);
            }else{
                articleSelectionMenuButton.setVisible(false);
            }
        }else{
            deleteImage.setVisible(false);
            addImage.setVisible(true);
            articleSelectionMenuButton.setVisible(false);
        }
    }

    @Override
    public void onBackPressed(){

        boolean saved = saveAndUpload();

        if (isStandingOrder){
            Intent newIntent = new Intent(getApplicationContext(), ActivityStandingOrder.class);
            startActivity(newIntent);
            overridePendingTransition(R.anim.activity_out1, R.anim.activity_out2);
        }else{
            if (CostDistributionHelper.isCreatedUserCheckMode(getApplicationContext(), currentInvoice) ||
                    (CostDistributionHelper.isExternalUserCheckMode(getApplicationContext(), currentInvoice, costDistributionItems))){
                Invoice nextCheckInvoice = nextOpenInvoice();

                if (nextCheckInvoice != null && saved){
                    showNextCheckInvoice(nextCheckInvoice);
                }else{
                    Intent newIntent = new Intent(getApplicationContext(), ActivityMain.class);
                    startActivity(newIntent);
                    overridePendingTransition(R.anim.activity_out1, R.anim.activity_out2);
                }
            }else{
                Intent k = getIntent();
                setResult(RESULT_OK, k);
                finish();
                overridePendingTransition(R.anim.activity_out1, R.anim.activity_out2);
            }
        }

    }

    public void resetAll(){

        currentInvoice = EntityComparatorService.copyInvoice(originalInvoice);

        payer = currentInvoice.getPayer(getApplicationContext());
        paymentRecipient = currentInvoice.getPaymentRecipient(getApplicationContext());

        costDistributionItems.clear();
        for (CostDistributionItem costDistributionItem : costDistributionItemsOriginal) {
            costDistributionItems.add(EntityComparatorService.copyCostDistributionItem(costDistributionItem));
        }

        this.doDeleteImage = false;
        this.doUploadImage = false;

        if (originalInvoice.getInvoiceImageId() != null) {
            sendImageDownloadRequest();
        } else {
            deletePictureFileWithourRequest();
        }

        refreshViews();
    }

    private boolean showNextCheckInvoice(Invoice invoice) {

        if (invoice != null) {
            Intent newIntent = new Intent(getApplicationContext(), ActivityInvoiceDetails.class);
            newIntent.putExtra("idInvoice", invoice.getInvoiceId().toString());
            startActivity(newIntent);
            overridePendingTransition(R.anim.activity_in1, R.anim.activity_in2);
            return true;
        }else{
            return false;
        }
    }

    public Invoice nextOpenInvoice() {


        List<String> userContactIds = LoginUserHelper.idsOfUserContactsWithCurrentUser(getApplicationContext());
        AppUser currentUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());

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

        SearchFilter searchFilter = SearchHelper.searchAndUseSearchConfiguration(getApplicationContext(), true, sqlBuilder);
        List<Invoice> invoices = SearchHelper.invoicesInInvoiceCostDistributionItems(searchFilter.getInvoiceCostDistributionItemList());

        if (invoices != null && !invoices.isEmpty()){
            for (Invoice invoice : invoices) {
                if (invoice.getInvoiceId() != null && !invoice.getInvoiceId().toString().equals(currentInvoice.getInvoiceId().toString())){
                    return invoice;
                }
            }
        }

        return null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.addImage:
                if (hasPermission(MY_PERMISSIONS_REQUEST_TAKE_PICTURE, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    takePicture();
                }
                break;
            case R.id.deleteImage:
                deletePicture();
                refreshImageMenuItems();
                break;
            case R.id.problemBtn:
                showMistakeMessageDialog();
                break;
            case R.id.deleteSubMenuButton:
                break;
            case R.id.articleSelection:
                showActivityArticlesSelect();
                break;
            case R.id.deleteMenuButton:

                if (isStandingOrder){

                    StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
                    handler.addObject(currentInvoice.getInvoiceId().toString(), StatusDatabaseHandler.OBJECT_TYPE_STANDING_ORDER, StatusDatabaseHandler.UPDATE_STATUS_DELETE, new Date().getTime(), 1);
                    Intent sIntent = new Intent(getApplicationContext(), RequestUpdateService.class);
                    sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
                    getApplicationContext().startService(sIntent);

                    deleteStandingOrder();

                    Intent newIntent = new Intent(getApplicationContext(), ActivityStandingOrder.class);
                    startActivity(newIntent);
                    overridePendingTransition(R.anim.activity_out1, R.anim.activity_out2);

                    break;
                }else{
                    deleteInvoice();

                    Intent newIntent = new Intent(getApplicationContext(), ActivityMain.class);
                    startActivity(newIntent);
                    overridePendingTransition(R.anim.activity_out1, R.anim.activity_out2);
                    break;
                }
            case R.id.checkMenuBtton:
                Intent k = getIntent();

                if (CostDistributionHelper.isCreatedUserReadyMode(getApplicationContext(), currentInvoice)){
                    currentInvoice.setInvoiceStatusEnum(InvoiceStatusEnum.CHECK);
                }else if (CostDistributionHelper.isExternalUserReadyMode(getApplicationContext(), currentInvoice, costDistributionItems)){
                    List<IPaymentItem> paymentItems = CostDistributionHelper.paymentItemsForCorrectionMode(getApplicationContext(), currentInvoice, costDistributionItems, false);
                    for (IPaymentItem paymentItem : paymentItems) {
                        paymentItem.setCorrectionStatus(CorrectionStatus.CHECK);
                    }
                }

                saveAndUpload();

                Intent newIntent = new Intent(getApplicationContext(), ActivityMain.class);
                startActivity(newIntent);
                overridePendingTransition(R.anim.activity_out1, R.anim.activity_out2);
                break;
            case R.id.resetMenuButton:
                resetAll();
                break;
            case R.id.createStandingOrder:
                FragmentManager fm = getSupportFragmentManager();
                DialogCreateStandingOrder editPayerDialog = new DialogCreateStandingOrder();
                if (currentInvoice.getDateOfInvoice() != null){
                    editPayerDialog.setArguments(currentInvoice.getDateOfInvoice());
                }
                editPayerDialog.show(fm, "dialog_createStandingOrder");
                break;
            case R.id.deleteStandingOrder:
                StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
                handler.addObject(currentInvoice.getInvoiceId().toString(), StatusDatabaseHandler.OBJECT_TYPE_STANDING_ORDER, StatusDatabaseHandler.UPDATE_STATUS_DELETE_SPECIAL, new Date().getTime(), 1);
                Intent sIntent = new Intent(getApplicationContext(), RequestUpdateService.class);
                sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
                getApplicationContext().startService(sIntent);
                break;
            default:
                onBackPressed();
        }

        return true;
    }

    @Override
    public void onFinishCostDistributionSelectDialog(CostDistribution costDistribution, Boolean wantToEdit) {

        if (wantToEdit != null && wantToEdit){

            Intent intent = new Intent(this, ActivityCostDistribution.class);

            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            String costDistributionItemsAsString = gson.toJson(costDistributionItems);

            intent.putExtra("costDistributionItems", costDistributionItemsAsString);

            String sumOfInvoice = "0.0";
            if (currentInvoice.getSumOfInvoice() != null){
                BigDecimal sumOfInvoiceDecimal = currentInvoice.getSumOfInvoice().setScale(2, BigDecimal.ROUND_HALF_EVEN);
                sumOfInvoice = sumOfInvoiceDecimal.toString();
            }
            intent.putExtra("sum", sumOfInvoice);
            intent.putExtra("invoiceId", currentInvoice.getInvoiceId().toString());
            intent.putExtra("showArticleSelect", imageExists() != null && (!this.doUploadImage && !this.doDeleteImage) && RightHelper.hasUserRight(getApplicationContext(), Right.OCR));

            startActivityForResult(intent, COST_DISTRIBUTION);

        }else if (costDistribution != null){

            MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
            List<CostDistributionItem> costDistributionItems = mainDatabaseHandler.findCostDistributionItems(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ID, costDistribution.getCostDistributionId().toString());

            List<CostDistributionItem> newCostDistributionItems = new ArrayList<>();
            for (CostDistributionItem costDistributionItem : costDistributionItems) {
                CostDistributionItem costDistributionItem1 = CostDistributionHelper.prepareItemForCostDistribution(costDistributionItem, null);
                costDistributionItem1.setMoneyValue(CostDistributionHelper.calculateAmountForCostDistributionItemPrecise(costDistributionItem, costDistributionItems, currentInvoice.getSumOfInvoice()));
                costDistributionItem1.setInvoiceId(currentInvoice.getInvoiceId());
                costDistributionItem1.setCostDistributionId(null);
                newCostDistributionItems.add(costDistributionItem1);
            }

            costDistributionItemsToDelete.addAll(this.costDistributionItems);
            this.costDistributionItems = newCostDistributionItems;

            refreshViews();
        }
    }

    @Override
    public void onFinishCreateStandingOrderDialog(Date startDate, RepetitionTypeEnum repetitionTypeEnum, boolean isAlwaysPaid) {

        if (startDate != null && repetitionTypeEnum != null){

            currentInvoice.setRepetitionTypeEnum(repetitionTypeEnum);
            refreshViews();

            saveAndUpload();

            StandingOrder standingOrder = new StandingOrder();
            standingOrder.setInvoiceTemplateId(currentInvoice.getInvoiceId());
            standingOrder.setStartDate(startDate);
            standingOrder.setRepetitionTypeEnum(repetitionTypeEnum);
            standingOrder.setIsAlwaysPaid(isAlwaysPaid);

            UUID randomId = UUID.randomUUID();
            standingOrder.setFutureInvoiceTemplateId(randomId);

            StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
            Gson gson = new GsonBuilder()
                    .setDateFormat("dd MM yyyy HH:mm:ss").create();
            String jsonString = gson.toJson(standingOrder);

            handler.addObject(jsonString, StatusDatabaseHandler.OBJECT_TYPE_STANDING_ORDER, StatusDatabaseHandler.UPDATE_STATUS_ADD, new Date().getTime(), 1);

            Intent sIntent = new Intent(getApplicationContext(), RequestUpdateService.class);
            sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
            getApplicationContext().startService(sIntent);

            currentInvoice.setStandingOrderInvoiceTemplateId(randomId);
        }
    }

    @Override
    public void onFinishInvoiceCategorySelectDialog(InvoiceCategory invoiceCategory) {
        if (invoiceCategory != null){
            currentInvoice.setInvoiceCategoryDTO(invoiceCategory);

            tryAutoFillInvoiceCategoryDependencies();

            refreshViews();
        }
    }

    @Override
    public void onFinishInputConflictDialog() {

    }

    public class GenerateInvoiceThumbnail extends AsyncTask<String, Integer, List<String>> {

        @Override
        protected List<String> doInBackground(String... strings) {

            generateInvoiceThumbnail(strings[0]);

            return new ArrayList<>();
        }

        public void generateInvoiceThumbnail(String invoiceId) {

            File inputFile = IOHelper.getTempUploadMediaFile(invoiceId);
            try {

                if (!inputFile.exists()) {
                    throw new IOException();
                }

                IOHelper ioHelper = IOHelper.getInstance();
                ioHelper.setCtx(getApplicationContext());
                String pathTmp = ioHelper.getImageDirectory();
                File thumbnailFile = new File(pathTmp + "/" + invoiceId + "_thumbnail");

                Bitmap unscaledBitmap = ImageHelper.decodeFile(inputFile.getAbsolutePath(), 2500, 2500);
                Bitmap scaledBitmap = ImageHelper.createScaledBitmap(2500, 2500, unscaledBitmap);
                OutputStream os = new BufferedOutputStream(new FileOutputStream(thumbnailFile));
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 96, os);
                os.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPostExecute(List<String> objects) {

            refreshImageView();

        }
    }
}
