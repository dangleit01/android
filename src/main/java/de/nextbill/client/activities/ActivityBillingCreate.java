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
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.nextbill.client.R;
import de.nextbill.client.breceiver.RequestListener;
import de.nextbill.client.breceiver.RequestServiceBroadcastReceiver;
import de.nextbill.client.database.IOHelper;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.enums.BroadcastMessageActionEnum;
import de.nextbill.client.enums.InvoiceDetailsPaymentPersonDialogEnum;
import de.nextbill.client.enums.LoginStatusEnum;
import de.nextbill.client.enums.OnlineStatusEnum;
import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.enums.UpdateModusType;
import de.nextbill.client.helper.LoginUserHelper;
import de.nextbill.client.interfaces.CurrencyTextWatcher;
import de.nextbill.client.interfaces.CustomDateTextWatcher;
import de.nextbill.client.interfaces.DateTextWatcher;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.IPaymentPerson;
import de.nextbill.client.model.UserContact;
import de.nextbill.client.network.RequestManager;
import de.nextbill.client.network.RequestUpdateService;
import de.nextbill.client.utils.BillingConfig;

public class ActivityBillingCreate extends AppCompatActivity implements DialogPaymentPerson.SelectPaymentPersonDialogListener{

    ListView list;

    private EditText startDateEt;
    private EditText endDateEt;
    private TextView userSelectionSw;
    private Button createInternalInvoiceBt;
    private ProgressBar progressBar2;
    private RelativeLayout periodRl;
    private CheckBox showTimeRangeCb;
    private Switch sendMailInvoicePayerSw;
    private Switch sendMailCostPayerSw;
    private DateTextWatcher startDateTextWatcher;
    private DateTextWatcher endDateTextWatcher;
    private RelativeLayout inputMailRl;
    private EditText inputMailEt;
    private ImageView imageView12;
    private TextView sumToPayTv;
    private RelativeLayout relativeLayout4;

    private RequestServiceBroadcastReceiver updateRequestReceiver;

    private IPaymentPerson payer;
    private BigDecimal sum = new BigDecimal(0);

    private boolean eMailChanged = false;

    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_generator);
        this.setTitle("Abrechnung erstellen");

        startDateEt = (EditText) findViewById(R.id.startDateEt);
        endDateEt = (EditText) findViewById(R.id.endDateEt);
        userSelectionSw = (TextView) findViewById(R.id.userSelectionBt);

        createInternalInvoiceBt = (Button) findViewById(R.id.createInternalInvoiceBt);
        progressBar2 = (ProgressBar) findViewById(R.id.progressBar2);
        periodRl = (RelativeLayout) findViewById(R.id.periodRl);
        showTimeRangeCb = (CheckBox) findViewById(R.id.showTimeRangeCb);
        inputMailEt = (EditText) findViewById(R.id.inputMailEt);
        imageView12 = (ImageView) findViewById(R.id.imageView12);
        sumToPayTv = (TextView) findViewById(R.id.sumToPayTv);
        relativeLayout4 = (RelativeLayout) findViewById(R.id.relativeLayout4);

        if (getIntent().getStringExtra("sum") != null){
            BigDecimal currentAmount = new BigDecimal(getIntent().getStringExtra("sum"));
            sum = currentAmount;
        }

        sumToPayTv.setText(CurrencyTextWatcher.bigDecimalToString(sum) + " €");

        TextWatcher mailChangedListener = new TextWatcher() {

            public void afterTextChanged(Editable s) {

                inputMailEt.removeTextChangedListener(this);
                if (payer != null){
                    isReportCreationPossible();
                }

                inputMailEt.addTextChangedListener(this);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        };
        inputMailEt.addTextChangedListener(mailChangedListener);


        inputMailRl = (RelativeLayout) findViewById(R.id.inputMailRl);
        showTimeRangeCb.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showOrHideTimeRange(isChecked);
            }
        });
        sendMailInvoicePayerSw = (Switch) findViewById(R.id.sendMailInvoicePayerSw);
        sendMailCostPayerSw = (Switch) findViewById(R.id.sendMailCostPayerSw);
        sendMailCostPayerSw.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showOrHideEmailInput(isChecked);
            }
        });

        Calendar calendar = Calendar.getInstance();
        Integer maxDayMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        Integer minDayMonth = calendar.getActualMinimum(Calendar.DAY_OF_MONTH);
        Integer year = calendar.get(Calendar.YEAR);
        Integer month = calendar.get(Calendar.MONTH);

        Calendar startDate = Calendar.getInstance();
        startDate.set( year, month, minDayMonth);
        Date startDateDate = startDate.getTime();
        Calendar endDate = Calendar.getInstance();
        endDate.set(year, month, maxDayMonth);
        Date endDateDate = endDate.getTime();

        startDateTextWatcher = new DateTextWatcher(startDateEt, startDateDate);
        endDateTextWatcher = new DateTextWatcher(endDateEt, endDateDate);

        if (getIntent().getStringExtra("userContactId") != null){
            UUID userContactId = UUID.fromString(getIntent().getStringExtra("userContactId"));
            MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());

            List<UserContact> userContacts = mainDatabaseHandler.findUserContacts(MainDatabaseHandler.VAR_USER_CONTACT_ID, userContactId.toString());
            if (!userContacts.isEmpty()){
                payer = userContacts.get(0);
                userSelectionSw.setText(payer.getPaymentPersonName());
                inputMailEt.setText(payer.getEmail());
            }
        }

        updateRequestReceiver = new RequestServiceBroadcastReceiver(new RequestListener() {

            @Override
            public void requestDone(String activityClassName) { }

            @Override
            public void generalMessage(String activityClassName, BroadcastMessageActionEnum broadcastMessageActionEnum, String message) {
                if (activityClassName.equals(ActivityBillingCreate.class.getSimpleName())){
                    if (BroadcastMessageActionEnum.READY.equals(broadcastMessageActionEnum)) {
                        progressBar2.setVisibility(View.INVISIBLE);
                        Toast.makeText(getApplicationContext(), "Abrechnung erfolgreich erstellt!", Toast.LENGTH_LONG).show();

                        Intent j = getIntent();
                        j.putExtra("refresh", "");
                        setResult(RESULT_OK, j);
                        finish();
                    }else if (BroadcastMessageActionEnum.ERROR.equals(broadcastMessageActionEnum)) {
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

//        userSelectionSw.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showInvoicePayerEditDialog();
//            }
//        });

        CustomDateTextWatcher startDateTextWatcherCustom = new CustomDateTextWatcher() {

            @Override
            public void currentValue(Date currentValue){
                isReportCreationPossible();
            }

            public void afterTextChanged(Editable s) {

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        };
        startDateTextWatcher.addCustomTextWatcher(startDateTextWatcherCustom);

        CustomDateTextWatcher endDateTextWatcherCustom = new CustomDateTextWatcher() {

            @Override
            public void currentValue(Date currentValue){
                isReportCreationPossible();
            }

            public void afterTextChanged(Editable s) {

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        };
        endDateTextWatcher.addCustomTextWatcher(endDateTextWatcherCustom);


        createInternalInvoiceBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createReport();
            }
        });

        SharedPreferences settings = getApplicationContext().getSharedPreferences(ActivityMain.PREF_NAME, 0);
        boolean mailSentActive = settings.getBoolean(BaseActivity.MAIL_SENT_ACTIVE, false);
        if(mailSentActive) {
            relativeLayout4.setVisibility(View.VISIBLE);
        }else{
            sendMailInvoicePayerSw.setChecked(false);
            sendMailCostPayerSw.setChecked(false);
            relativeLayout4.setVisibility(View.GONE);
        }

        periodRl.requestFocus();
    }

    private boolean isReportCreationPossible(){
        boolean isValid = true;

        if (showTimeRangeCb.isChecked()){
            if (DateTextWatcher.stringToDate(startDateEt.getText().toString()) == null){
                isValid = false;
            }
            if (DateTextWatcher.stringToDate(endDateEt.getText().toString()) == null){
                isValid = false;
            }
        }

        if (sendMailCostPayerSw.isChecked()){

            if (payer != null){
                Pattern pattern = Pattern.compile(EMAIL_PATTERN);
                Matcher matcher = pattern.matcher(inputMailEt.getText().toString());
                if (matcher.matches()) {

                    String uri = "drawable/ic_done_black_48dp";
                    int imageResource = getApplicationContext().getResources().getIdentifier(uri, null, getApplicationContext().getPackageName());
                    Drawable image = getApplicationContext().getResources().getDrawable(imageResource);
                    imageView12.setImageDrawable(image);

                    MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
                    if (payer.getPaymentPersonEnum().equals(PaymentPersonTypeEnum.CONTACT)){
                        List<UserContact> contacts = mainDatabaseHandler.findUserContacts(MainDatabaseHandler.VAR_USER_CONTACT_ID, payer.getPaymentPersonId().toString());

                        if (contacts != null && !contacts.isEmpty()){
                            UserContact userContact = contacts.get(0);
                            userContact.setEmail(inputMailEt.getText().toString());
                            eMailChanged = true;
                        }
                    }

                } else {
                    String uri = "drawable/ic_clear_black_48dp";
                    int imageResource = getApplicationContext().getResources().getIdentifier(uri, null, getApplicationContext().getPackageName());
                    Drawable image = getApplicationContext().getResources().getDrawable(imageResource);
                    imageView12.setImageDrawable(image);
                    isValid = false;
                }
            }
        }

        enableCreateReportButton(isValid);

        return isValid;
    }

    private void showOrHideTimeRange(boolean show){
        if (show){
            periodRl.setVisibility(View.VISIBLE);
        }else{
            periodRl.setVisibility(View.GONE);
        }
        isReportCreationPossible();
    }

    private void showOrHideEmailInput(boolean show){
        if (show){
            inputMailRl.setVisibility(View.VISIBLE);
        }else{
            inputMailRl.setVisibility(View.GONE);
        }
        isReportCreationPossible();
    }

    public BillingConfig createReportConfig(){
        BillingConfig billingConfig = new BillingConfig();

        if (showTimeRangeCb.isChecked()){
            Date startDate = startDateTextWatcher.getResult();
            Date endDate = endDateTextWatcher.getResult();

            if (startDate != null){
                billingConfig.setStartDate(startDate);
            }
            if (endDate != null){
                billingConfig.setEndDate(endDate);
            }
        }else{
            billingConfig.setStartDate(null);
            billingConfig.setEndDate(null);
        }

        if (payer != null){
            MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
            String payerUUID = "";
            if (payer.getPaymentPersonEnum().equals(PaymentPersonTypeEnum.CONTACT)){
                List<UserContact> contacts = mainDatabaseHandler.findUserContacts(MainDatabaseHandler.VAR_USER_CONTACT_ID, payer.getPaymentPersonId().toString());

                if (contacts != null && !contacts.isEmpty()){
                    payerUUID = contacts.get(0).getUserContactId().toString();
                    billingConfig.setUserSelectionPaymentPersonTypeEnum(PaymentPersonTypeEnum.CONTACT);
                }

            }
            billingConfig.setUserSelection(payerUUID);
        }
        billingConfig.setUserPayer(LoginUserHelper.currentLoggedInUser(getApplicationContext()).getAppUserId().toString());
        billingConfig.setUserPayerPaymentPersonTypeEnum(PaymentPersonTypeEnum.USER);

        billingConfig.setUsePaidInvoices(false);
        billingConfig.setMarkAsPaid(false);

        billingConfig.setSendMailInvoicePayer(sendMailInvoicePayerSw.isChecked());
        billingConfig.setSendMailCostPayer(sendMailCostPayerSw.isChecked());

        return billingConfig;
    }

    private void enableCreateReportButton(boolean enable){
        createInternalInvoiceBt.setClickable(enable);
        createInternalInvoiceBt.setEnabled(enable);
    }

    private Date convertToDate(String text){
        SimpleDateFormat tmpSdf = new SimpleDateFormat("dd.MM.yyyy");

        try {
            Date dateFormat = tmpSdf.parse(text);
            return dateFormat;
        } catch (ParseException e) {
        }

        return null;
    }

    private void createReport() {

        createInternalInvoiceBt.setEnabled(false);
        progressBar2.setVisibility(View.VISIBLE);

        BillingConfig billingConfig = createReportConfig();

        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        Gson gson = new GsonBuilder()
                .setDateFormat("dd MMM yyyy HH:mm:ss").create();
        String jsonString = gson.toJson(billingConfig);

        if (eMailChanged){
            handler.addObject(payer.getPaymentPersonId().toString(), StatusDatabaseHandler.OBJECT_TYPE_USER_CONTACT, StatusDatabaseHandler.UPDATE_STATUS_PUT, new Date().getTime(), 1);
        }

        handler.addObject(jsonString, StatusDatabaseHandler.OBJECT_TYPE_BILLING, StatusDatabaseHandler.UPDATE_STATUS_ADD, new Date().getTime(), 1);

        Intent sIntent = new Intent(getApplicationContext(), RequestUpdateService.class);
        sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
        getApplicationContext().startService(sIntent);
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

//        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onFinishPaymentPersonSelectDialog(InvoiceDetailsPaymentPersonDialogEnum invoiceDetailsPaymentPersonDialogEnum, IPaymentPerson iPaymentPerson) {
        if (iPaymentPerson != null) {

            if (invoiceDetailsPaymentPersonDialogEnum.equals(InvoiceDetailsPaymentPersonDialogEnum.PAYER)) {
                payer = iPaymentPerson;
                userSelectionSw.setText(payer.getPaymentPersonName());
            }
        }
    }
}
