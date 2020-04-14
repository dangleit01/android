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

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.nextbill.client.R;
import de.nextbill.client.adapters.AdapterDialogPaymentPersonListView;
import de.nextbill.client.breceiver.RequestListener;
import de.nextbill.client.breceiver.RequestServiceBroadcastReceiver;
import de.nextbill.client.database.IOHelper;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.enums.BasicStatusEnum;
import de.nextbill.client.enums.BroadcastMessageActionEnum;
import de.nextbill.client.enums.InvoiceDetailsPaymentPersonDialogEnum;
import de.nextbill.client.enums.LoginStatusEnum;
import de.nextbill.client.enums.OnlineStatusEnum;
import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.helper.LoginUserHelper;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.BusinessPartner;
import de.nextbill.client.model.IPaymentPerson;
import de.nextbill.client.model.PaymentPerson;
import de.nextbill.client.model.UserContact;
import de.nextbill.client.network.RequestManager;
import de.nextbill.client.network.RequestUpdateService;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class DialogPaymentPerson extends DialogFragment implements TextView.OnEditorActionListener {

    private StickyListHeadersListView mPayerLv;
    private EditText searchEt;
    private TextView mPaymentPersonTv;
    private TextView infoBoxTv;
    private RelativeLayout infoBoxRL;
    private ProgressBar infoBoxPg;

    private AdapterDialogPaymentPersonListView paymentPersonsAdapter;
    private List<IPaymentPerson> appUserPaymentPersons = new ArrayList<>();
    private List<IPaymentPerson> businessPartnerPaymentPersons = new ArrayList<>();
    private List<IPaymentPerson> userContactsPaymentPersons = new ArrayList<>();

    private List<BusinessPartner> tempBussinessPartnerDBList = new ArrayList<>();
    private List<IPaymentPerson> tempAppUserDBList = new ArrayList<>();
    private List<IPaymentPerson> tempUserContactDBList = new ArrayList<>();

    private List<IPaymentPerson> paymentPersonLvList = new ArrayList<>();
    private PaymentPersonTypeEnum currentPayerEnum;
    private InvoiceDetailsPaymentPersonDialogEnum invoiceDetailsPaymentPersonDialogEnum;

    private List<PaymentPersonTypeEnum> paymentPersonTypeEnums = new ArrayList<>();
    private List<PaymentPersonTypeEnum> internalPaymentPersonTypeEnums = new ArrayList<>();
    private Boolean showDeleteButtonForBusinessPartner;
    private Boolean showDeleteButtonForUserContacts;
    private Boolean canAddBusinessPartners;

    private RequestServiceBroadcastReceiver updateRequestReceiver;

    private PaymentPerson tempPaymentPerson;

    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    public DialogPaymentPerson() {

    }

    public void setArguments(InvoiceDetailsPaymentPersonDialogEnum invoiceDetailsPaymentPersonDialogEnum, List<PaymentPersonTypeEnum> paymentPersonTypeEnums,
                             List<BusinessPartner> tempBussinessPartnerDBList, List<IPaymentPerson> tempAppUserDBList, List<IPaymentPerson> tempUserContactDBList,
                             boolean showDeleteButtonForBusinessPartner, boolean showDeleteButtonForUserContacts, boolean canAddBusinessPartners){
        this.invoiceDetailsPaymentPersonDialogEnum = invoiceDetailsPaymentPersonDialogEnum;
        this.paymentPersonTypeEnums = paymentPersonTypeEnums;
        this.tempBussinessPartnerDBList = tempBussinessPartnerDBList;
        this.tempAppUserDBList = tempAppUserDBList;
        this.tempUserContactDBList = tempUserContactDBList;
        this.showDeleteButtonForBusinessPartner = showDeleteButtonForBusinessPartner;
        this.showDeleteButtonForUserContacts = showDeleteButtonForUserContacts;
        this.canAddBusinessPartners = canAddBusinessPartners;
    }

    public interface SelectPaymentPersonDialogListener {
        void onFinishPaymentPersonSelectDialog(InvoiceDetailsPaymentPersonDialogEnum invoiceDetailsPaymentPersonDialogEnum, IPaymentPerson iPaymentPerson);
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_payment_person, container);

        View.OnClickListener onItemClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                Integer position = (Integer) view.getTag();

                tempPaymentPerson = PaymentPerson.fromIPaymentPerson(paymentPersonLvList.get(Integer.valueOf(position)));

                if (paymentPersonLvList.get(Integer.valueOf(position)).getPaymentPersonEnum().equals(PaymentPersonTypeEnum.NEW)) {
                    PopupMenu popup = new PopupMenu(getContext(), view);
                    popup.getMenuInflater().inflate(R.menu.payment_person_dialog_type_selection, popup.getMenu());

                    MenuItem menuItemBusinessPartner = popup.getMenu().findItem(R.id.payment_person_business_partner);
                    menuItemBusinessPartner.setVisible(canAddBusinessPartners);

                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getActivity());
                            StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getActivity());

                            switch (item.getItemId()) {
                                case R.id.payment_person_person:
                                    UserContact userContact = new UserContact();
                                    userContact.setAppUserId(LoginUserHelper.currentLoggedInUser(getActivity()).getAppUserId());
                                    userContact.setContactName(tempPaymentPerson.getPaymentPersonName());
                                    userContact.setBasicStatusEnum(BasicStatusEnum.OK);
                                    mainDatabaseHandler.insertUserContact(userContact);

                                    handler.addObject(userContact.getUserContactId().toString(), StatusDatabaseHandler.OBJECT_TYPE_USER_CONTACT, StatusDatabaseHandler.UPDATE_STATUS_ADD, new Date().getTime(), 1);

                                    tempPaymentPerson = PaymentPerson.fromIPaymentPerson(userContact);
                                    tempPaymentPerson.setPaymentPersonEnum(PaymentPersonTypeEnum.CONTACT);
                                    synchronizeAndCloseDialog();
                                    break;
                                case R.id.payment_person_project:
                                    UserContact userContact2 = new UserContact();
                                    userContact2.setAppUserId(LoginUserHelper.currentLoggedInUser(getActivity()).getAppUserId());
                                    userContact2.setContactName(tempPaymentPerson.getPaymentPersonName());
                                    userContact2.setBasicStatusEnum(BasicStatusEnum.OK);
                                    userContact2.setProject(true);
                                    mainDatabaseHandler.insertUserContact(userContact2);

                                    handler.addObject(userContact2.getUserContactId().toString(), StatusDatabaseHandler.OBJECT_TYPE_USER_CONTACT, StatusDatabaseHandler.UPDATE_STATUS_ADD, new Date().getTime(), 1);

                                    tempPaymentPerson = PaymentPerson.fromIPaymentPerson(userContact2);
                                    tempPaymentPerson.setPaymentPersonEnum(PaymentPersonTypeEnum.CONTACT);
                                    synchronizeAndCloseDialog();
                                    break;
                                case R.id.payment_person_business_partner:
                                    BusinessPartner businessPartner = new BusinessPartner();
                                    businessPartner.setBusinessPartnerName(tempPaymentPerson.getPaymentPersonName());
                                    businessPartner.setBusinessPartnerReceiptName(tempPaymentPerson.getPaymentPersonName());
                                    businessPartner.setBasicStatusEnum(BasicStatusEnum.OK);
                                    businessPartner.setAppUserId(LoginUserHelper.currentLoggedInUser(getContext()).getAppUserId());
                                    mainDatabaseHandler.insertBusinessPartner(businessPartner);

                                    handler.addObject(businessPartner.getBusinessPartnerId().toString(), StatusDatabaseHandler.OBJECT_TYPE_BUSINESS_PARTNER, StatusDatabaseHandler.UPDATE_STATUS_ADD, new Date().getTime(), 1);

                                    tempPaymentPerson = PaymentPerson.fromIPaymentPerson(businessPartner);
                                    tempPaymentPerson.setPaymentPersonEnum(PaymentPersonTypeEnum.BUSINESS_PARTNER);
                                    synchronizeAndCloseDialog();
                                    break;
                                case R.id.payment_person_app_user:

                                    StatusDatabaseHandler statusDatabaseHandler = StatusDatabaseHandler.getInstance(getActivity());
                                    int status = statusDatabaseHandler.getStatus();
                                    if (status == StatusDatabaseHandler.STATUS_MANUAL_OFFLINE) {
                                        tempPaymentPerson = null;
                                        showInfoBox("Offline-Modus aktiv! Benutzer-Suche nicht möglich.", false);
                                        return true;
                                    }

                                    tempPaymentPerson.setPaymentPersonEnum(PaymentPersonTypeEnum.USER);

                                    Pattern pattern = Pattern.compile(EMAIL_PATTERN);
                                    Matcher matcher = pattern.matcher(tempPaymentPerson.getPaymentPersonName());
                                    if (matcher.matches()) {

                                        List<AppUser> appUsers = mainDatabaseHandler.findAppUsers(MainDatabaseHandler.VAR_EMAIL, tempPaymentPerson.getPaymentPersonName());

                                        List<UserContact> userContacts = new ArrayList<>();
                                        if (!appUsers.isEmpty()){
                                            userContacts = mainDatabaseHandler.findUserContacts(MainDatabaseHandler.VAR_APP_USER_CONTACT_ID, appUsers.get(0).getAppUserId().toString());
                                        }
                                        if (!userContacts.isEmpty()){

                                            UserContact userContact1 = userContacts.get(0);
                                            userContact1.setBasicStatusEnum(BasicStatusEnum.OK);
                                            mainDatabaseHandler.updateUserContact(userContact1);

                                            tempPaymentPerson = PaymentPerson.fromIPaymentPerson(userContact1);

                                            handler.addObject(tempPaymentPerson.getPaymentPersonId().toString(), StatusDatabaseHandler.OBJECT_TYPE_USER_CONTACT, StatusDatabaseHandler.UPDATE_STATUS_PUT, new Date().getTime(), 1);

                                            closeDialog();
                                        }else{
                                            handler.addObject(tempPaymentPerson.getPaymentPersonName(), StatusDatabaseHandler.OBJECT_TYPE_APP_USER, StatusDatabaseHandler.UPDATE_STATUS_GET, new Date().getTime(), 1);

                                            Intent sIntent = new Intent(getActivity(), RequestUpdateService.class);
                                            sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
                                            getActivity().startService(sIntent);

                                            showInfoBox("Suche Benutzer...", true);
                                        }

                                    } else {
                                        showInfoBox("E-Mail-Adresse nicht korrekt formatiert!", false);
                                    }

                                    return true;
                            }

                            return true;
                        }
                    });

//                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
//                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                    popup.show();

                } else {
                    closeDialog();
                }
            }
        };

        View.OnClickListener onClickListener = new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Integer position = (Integer) v.getTag();
                IPaymentPerson paymentPerson = paymentPersonLvList.get(position);
                UUID paymentPersonId = paymentPerson.getPaymentPersonId();

                MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(v.getContext());
                StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(v.getContext());
                if (paymentPerson.getPaymentPersonEnum().equals(PaymentPersonTypeEnum.BUSINESS_PARTNER)){

                    List<BusinessPartner> businessPartners = mainDatabaseHandler.findBusinessPartners(MainDatabaseHandler.VAR_BUSINESS_PARTNER_ID, paymentPersonId.toString());
                    BusinessPartner businessPartner = businessPartners.get(0);
                    businessPartner.setBasicStatusEnum(BasicStatusEnum.DELETED);
                    mainDatabaseHandler.updateBusinessPartner(businessPartner);

                    handler.addObject(paymentPersonId.toString(), StatusDatabaseHandler.OBJECT_TYPE_BUSINESS_PARTNER, StatusDatabaseHandler.UPDATE_STATUS_DELETE, new Date().getTime(), 1);

                    if (paymentPerson instanceof BusinessPartner) tempBussinessPartnerDBList.remove((BusinessPartner) paymentPerson);
                }else if (paymentPerson.getPaymentPersonEnum().equals(PaymentPersonTypeEnum.CONTACT)){
                    List<UserContact> userContacts = mainDatabaseHandler.findUserContacts(MainDatabaseHandler.VAR_USER_CONTACT_ID, paymentPersonId.toString());
                    UserContact userContact = userContacts.get(0);
                    userContact.setBasicStatusEnum(BasicStatusEnum.DELETED);
                    mainDatabaseHandler.updateUserContact(userContact);
                    handler.addObject(paymentPersonId.toString(), StatusDatabaseHandler.OBJECT_TYPE_USER_CONTACT, StatusDatabaseHandler.UPDATE_STATUS_DELETE, new Date().getTime(), 1);

                    if (paymentPerson instanceof UserContact) userContactsPaymentPersons.remove((UserContact) paymentPerson);

                }

                paymentPersonLvList.remove(paymentPerson);
                paymentPersonsAdapter.notifyDataSetChanged();

                Intent sIntent = new Intent(v.getContext(), RequestUpdateService.class);
                sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
                v.getContext().startService(sIntent);
            }
        };

        mPayerLv = (StickyListHeadersListView) view.findViewById(R.id.invoiceDetailsPayerDialogPayerLv);
        paymentPersonsAdapter = new AdapterDialogPaymentPersonListView(this.getContext(), paymentPersonLvList, onClickListener, onItemClickListener, showDeleteButtonForBusinessPartner, showDeleteButtonForUserContacts);
        mPayerLv.setAdapter(paymentPersonsAdapter);

        businessPartnerPaymentPersons.clear();
        businessPartnerPaymentPersons.addAll(tempBussinessPartnerDBList);
        appUserPaymentPersons.clear();
        appUserPaymentPersons.addAll(tempAppUserDBList);
        userContactsPaymentPersons.clear();
        userContactsPaymentPersons.addAll(tempUserContactDBList);

        currentPayerEnum = PaymentPersonTypeEnum.USER;

        updateRequestReceiver = new RequestServiceBroadcastReceiver(new RequestListener() {

            @Override
            public void requestDone(String activityClassName) {
                if (DialogPaymentPerson.class.getSimpleName().equals(activityClassName)){
                    closeDialog();
                }
            }

            @Override
            public void generalMessage(String activityClassName, BroadcastMessageActionEnum broadcastMessageActionEnum, String message) {
                if (activityClassName.equals(DialogPaymentPerson.class.getSimpleName())){

                    if (BroadcastMessageActionEnum.FOUND.equals(broadcastMessageActionEnum)) {
                        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getActivity());
                        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getActivity());
                        List<AppUser> appUsers = mainDatabaseHandler.findAppUsers(MainDatabaseHandler.VAR_APP_USER_ID, message);

                        if (!appUsers.isEmpty() && (appUsers.get(0).getEmail() != null &&
                                !appUsers.get(0).getEmail().equals(LoginUserHelper.currentLoggedInUser(getActivity()).getEmail()))) {
                            AppUser appUser = appUsers.get(0);

                            UserContact userContact = new UserContact();
                            userContact.setAppUserId(LoginUserHelper.currentLoggedInUser(getActivity()).getAppUserId());
                            userContact.setEmail(appUser.getEmail());
                            userContact.setContactName(appUser.getAppUserName());
                            userContact.setAppUserContactId(UUID.fromString(message));
                            mainDatabaseHandler.insertUserContact(userContact);

                            tempPaymentPerson = PaymentPerson.fromIPaymentPerson(userContact);

                            handler.addObject(userContact.getUserContactId().toString(), StatusDatabaseHandler.OBJECT_TYPE_USER_CONTACT, StatusDatabaseHandler.UPDATE_STATUS_ADD, new Date().getTime(), 1);
                            synchronizeAndCloseDialog();
                        }
                    }else if (BroadcastMessageActionEnum.NOT_FOUND.equals(broadcastMessageActionEnum)){
                        showInfoBox("Keinen Benutzer gefunden!", false);
                    }
                }
            }

            @Override
            public void loginMessage(LoginStatusEnum loginStatusEnum) {

            }

            @Override
            public void onlineStatusMessage(OnlineStatusEnum onlineStatusEnum) {
                closeDialog();
            }
        });

        TextWatcher inputTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

                businessPartnerPaymentPersons.clear();
                appUserPaymentPersons.clear();
                userContactsPaymentPersons.clear();

                infoBoxRL.setVisibility(View.GONE);

                if (!searchEt.getText().toString().equals("")) {
                    for (BusinessPartner tmpBusinessPartner : tempBussinessPartnerDBList) {
                        if (tmpBusinessPartner.getBusinessPartnerName().toLowerCase().contains(searchEt.getText().toString().toLowerCase())) {
                            businessPartnerPaymentPersons.add(tmpBusinessPartner);
                        }
                    }

                    for (IPaymentPerson tmpAppUser : tempAppUserDBList) {
                        if (tmpAppUser.getPaymentPersonName().toLowerCase().contains(searchEt.getText().toString().toLowerCase())) {
                            appUserPaymentPersons.add(tmpAppUser);
                        }
                    }

                    for (IPaymentPerson tmpUserContact : tempUserContactDBList) {
                        if (tmpUserContact.getPaymentPersonName().toLowerCase().contains(searchEt.getText().toString().toLowerCase()) ||
                                (tmpUserContact.getEmail() != null && tmpUserContact.getEmail().toLowerCase().contains(searchEt.getText().toString().toLowerCase()))) {
                            userContactsPaymentPersons.add(tmpUserContact);
                        }
                    }

                    refreshPaymentPersonList();
                } else {

                    businessPartnerPaymentPersons.addAll(tempBussinessPartnerDBList);
                    appUserPaymentPersons.addAll(tempAppUserDBList);
                    userContactsPaymentPersons.addAll(tempUserContactDBList);

                    refreshPaymentPersonList();
                }
            }
        };

        searchEt = (EditText) view.findViewById(R.id.invoiceDetailsPayerDialogSearchEt);
        infoBoxTv = (TextView) view.findViewById(R.id.infoBoxTv);
        infoBoxRL = (RelativeLayout) view.findViewById(R.id.infoBoxRL);
        infoBoxPg = (ProgressBar) view.findViewById(R.id.infoBoxPg);

        searchEt.addTextChangedListener(inputTextWatcher);
        searchEt.requestFocus();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        searchEt.setOnEditorActionListener(this);
        searchEt.setSelection(0, searchEt.getText().length());

        refreshPaymentPersonList();

        return view;

    }

    @Override
    public void onResume() {
        super.onResume();
        IOHelper.getInstance().setCtx(this.getContext());

        IntentFilter filter = new IntentFilter();
        filter.addAction(RequestManager.BC_REQUEST_DONE);
        filter.addAction(RequestManager.BC_SEND_MESSAGE);
        filter.addAction(RequestManager.BC_ONLINE);
        this.getContext().registerReceiver(updateRequestReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.getContext().unregisterReceiver(updateRequestReceiver);
    }

    public void synchronizeAndCloseDialog(){
        Intent sIntent = new Intent(getActivity(), RequestUpdateService.class);
        sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
        sIntent.putExtra(RequestUpdateService.ACTIVITY_CLASS, this.getClass().getSimpleName());
        getActivity().startService(sIntent);
        showInfoBox("Synchronisiere...", true);
    }

    public void closeDialog(){
        InputMethodManager inputMethodManager =
                (InputMethodManager) getActivity().getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                searchEt.getWindowToken(), 0);
        SelectPaymentPersonDialogListener activity = (SelectPaymentPersonDialogListener) getActivity();

        activity.onFinishPaymentPersonSelectDialog(invoiceDetailsPaymentPersonDialogEnum, tempPaymentPerson);
        DialogPaymentPerson.this.dismiss();
    }

    public void showInfoBox(String message, boolean showProgressBar){
        infoBoxTv.setText(message);
        infoBoxRL.setVisibility(View.VISIBLE);
        if (showProgressBar){
            infoBoxPg.setVisibility(View.VISIBLE);
        }else{
            infoBoxPg.setVisibility(View.GONE);
        }
    }

    private void refreshPaymentPersonList() {

        paymentPersonLvList.clear();
        internalPaymentPersonTypeEnums.clear();
        Integer currentIndex = 0;

        if(searchEt.getText() != null && !searchEt.getText().toString().equals("")){

            boolean searchTextIsPaymentPersonInList = false;

            if (paymentPersonTypeEnums.contains(PaymentPersonTypeEnum.BUSINESS_PARTNER)){
                for (BusinessPartner tmpBusinessPartner : tempBussinessPartnerDBList) {
                    if (tmpBusinessPartner.getBusinessPartnerName().toLowerCase().equals(searchEt.getText().toString().toLowerCase())) {
                        searchTextIsPaymentPersonInList = true;
                        break;
                    }
                }
            }

            if (paymentPersonTypeEnums.contains(PaymentPersonTypeEnum.USER)){
                if (!searchTextIsPaymentPersonInList){
                    for (IPaymentPerson tmpAppUser : tempAppUserDBList) {
                        if (tmpAppUser.getPaymentPersonName().toLowerCase().equals(searchEt.getText().toString().toLowerCase())) {
                            searchTextIsPaymentPersonInList = true;
                            break;
                        }
                    }
                }
            }

            if (paymentPersonTypeEnums.contains(PaymentPersonTypeEnum.CONTACT)){
                if (!searchTextIsPaymentPersonInList){
                    for (IPaymentPerson tmpUserContact : tempUserContactDBList) {
                        if (tmpUserContact.getPaymentPersonName().toLowerCase().equals(searchEt.getText().toString().toLowerCase()) || (
                                tmpUserContact.getEmail() != null && tmpUserContact.getEmail().toLowerCase().equals(searchEt.getText().toString().toLowerCase()))) {
                            searchTextIsPaymentPersonInList = true;
                            break;
                        }
                    }
                }
            }

            if (paymentPersonTypeEnums.contains(PaymentPersonTypeEnum.NEW) && !searchTextIsPaymentPersonInList){
                internalPaymentPersonTypeEnums.add(PaymentPersonTypeEnum.NEW);
            }

        }

        if (paymentPersonTypeEnums.contains(PaymentPersonTypeEnum.USER) && !appUserPaymentPersons.isEmpty()){
            internalPaymentPersonTypeEnums.add(PaymentPersonTypeEnum.USER);
        }
        if (paymentPersonTypeEnums.contains(PaymentPersonTypeEnum.CONTACT) && !userContactsPaymentPersons.isEmpty()){
            internalPaymentPersonTypeEnums.add(PaymentPersonTypeEnum.CONTACT);
        }
        if (paymentPersonTypeEnums.contains(PaymentPersonTypeEnum.BUSINESS_PARTNER) && !businessPartnerPaymentPersons.isEmpty()){
            internalPaymentPersonTypeEnums.add(PaymentPersonTypeEnum.BUSINESS_PARTNER);
        }

        for (PaymentPersonTypeEnum tmpPaymentPersonTypeEnum : internalPaymentPersonTypeEnums) {

            if (tmpPaymentPersonTypeEnum.equals(PaymentPersonTypeEnum.NEW)){
                PaymentPerson paymentPerson = new PaymentPerson();
                paymentPerson.setPaymentPersonName(searchEt.getText().toString().trim());
                paymentPerson.setPaymentPersonEnum(PaymentPersonTypeEnum.NEW);
                paymentPerson.setVirtualPayerEnum(PaymentPersonTypeEnum.NEW);
                paymentPersonLvList.add(paymentPerson);
                currentIndex++;
            }else if (tmpPaymentPersonTypeEnum.equals(PaymentPersonTypeEnum.USER)){
                for (IPaymentPerson tmpAppUser: appUserPaymentPersons) {
                    paymentPersonLvList.add(tmpAppUser);
                    currentIndex++;
                }
            }else if (tmpPaymentPersonTypeEnum.equals(PaymentPersonTypeEnum.CONTACT)){
                for (IPaymentPerson tmpUserContacts: userContactsPaymentPersons) {
                    paymentPersonLvList.add(tmpUserContacts);
                    currentIndex++;
                }
            }else if (tmpPaymentPersonTypeEnum.equals(PaymentPersonTypeEnum.BUSINESS_PARTNER)){
                for (IPaymentPerson tmpBusinessPartner: businessPartnerPaymentPersons) {
                    paymentPersonLvList.add(tmpBusinessPartner);
                    currentIndex++;
                }
            }
        }

        paymentPersonsAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

        if (EditorInfo.IME_ACTION_DONE == actionId) {
            this.dismiss();
            return true;
        }
        return false;
    }

}
