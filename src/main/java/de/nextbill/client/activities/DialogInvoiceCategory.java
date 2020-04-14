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
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.daimajia.swipe.SwipeLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.nextbill.client.R;
import de.nextbill.client.adapters.AdapterDialogInvoiceCategoryListView;
import de.nextbill.client.breceiver.RequestListener;
import de.nextbill.client.breceiver.RequestServiceBroadcastReceiver;
import de.nextbill.client.comparators.InvoiceCategoryComparator;
import de.nextbill.client.database.IOHelper;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.enums.BasicStatusEnum;
import de.nextbill.client.enums.BroadcastMessageActionEnum;
import de.nextbill.client.enums.LoginStatusEnum;
import de.nextbill.client.enums.OnlineStatusEnum;
import de.nextbill.client.helper.LoginUserHelper;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.InvoiceCategory;
import de.nextbill.client.network.RequestManager;
import de.nextbill.client.network.RequestUpdateService;
import de.nextbill.client.utils.SqlBuilder;
import se.emilsjolander.stickylistheaders.ExpandableStickyListHeadersListView;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class DialogInvoiceCategory extends DialogFragment implements TextView.OnEditorActionListener {

    private ExpandableStickyListHeadersListView mPayerLv;
    private EditText searchEt;
    private TextView infoBoxTv;
    private RelativeLayout infoBoxRL;
    private ProgressBar infoBoxPg;

    private List<InvoiceCategory> invoiceCategories = new ArrayList<>();
    private List<InvoiceCategory> titleInvoiceCategories = new ArrayList<>();
    private InvoiceCategory tempInvoiceCategory;

    private AdapterDialogInvoiceCategoryListView invoiceCategoriesAdapter;
    private List<InvoiceCategory> internalInvoiceCategories = new ArrayList<>();

    private List<InvoiceCategory> tempInvoiceCategoryDBList = new ArrayList<>();

    private RequestServiceBroadcastReceiver updateRequestReceiver;

    private boolean synchronize = false;

    public DialogInvoiceCategory() {

    }

    public void setArguments(List<InvoiceCategory> tempInvoiceCategoryDBList){
        this.tempInvoiceCategoryDBList = tempInvoiceCategoryDBList;
    }

    public interface SelectInvoiceCategoryDialogListener {
        void onFinishInvoiceCategorySelectDialog(InvoiceCategory invoiceCategory);
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
        View view = inflater.inflate(R.layout.dialog_invoice_category, container);

        View.OnClickListener onItemClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Integer position = (Integer) view.getTag();
                InvoiceCategory invoiceCategory = invoiceCategories.get(position);

                InputMethodManager inputMethodManager =
                        (InputMethodManager) getActivity().getSystemService(
                                Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(
                        searchEt.getWindowToken(), 0);

                SelectInvoiceCategoryDialogListener activity = (SelectInvoiceCategoryDialogListener) getActivity();

                activity.onFinishInvoiceCategorySelectDialog(invoiceCategory);
                DialogInvoiceCategory.this.dismiss();
            }
        };

        View.OnClickListener onDeleteClickListener = new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Integer position = (Integer) v.getTag();
                InvoiceCategory invoiceCategory = invoiceCategories.get(position);

                View parent = (View)v.getParent();
                if (parent != null) {
                    View parentOfParent = (View)parent.getParent();
                    if (parentOfParent != null) {
                        SwipeLayout swipeLayout =  (SwipeLayout) parentOfParent;
                        swipeLayout.close();
                    }
                }

                MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(v.getContext());
                StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(v.getContext());

                invoiceCategory.setBasicStatusEnum(BasicStatusEnum.DELETED);
                mainDatabaseHandler.updateInvoiceCategory(invoiceCategory);
                handler.addObject(invoiceCategory.getInvoiceCategoryId().toString(), StatusDatabaseHandler.OBJECT_TYPE_INVOICE_CATEGORY, StatusDatabaseHandler.UPDATE_STATUS_DELETE, new Date().getTime(), 1);

                invoiceCategories.remove(invoiceCategory);
                invoiceCategoriesAdapter.notifyDataSetChanged();

                Intent sIntent = new Intent(v.getContext(), RequestUpdateService.class);
                sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
                v.getContext().startService(sIntent);
            }
        };

        View.OnClickListener openSubCategoryAddingClickListener = new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                View parent = (View)v.getParent();
                if (parent != null) {
                    View parentOfParent = (View)parent.getParent();
                    if (parentOfParent != null) {
                        RelativeLayout relativeLayout = (RelativeLayout) parentOfParent.findViewById(R.id.addSubCategoryRl);
                        relativeLayout.setVisibility(View.VISIBLE);

                        EditText editText = (EditText) relativeLayout.findViewById(R.id.subCategoryNameEt);
                    }
                }
            }
        };

        View.OnClickListener closeSubCategoryAddingClickListener = new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                View parent = (View)v.getParent();
                if (parent != null) {
                    View parentOfParent = (View)parent.getParent();
                    if (parentOfParent != null) {
                        RelativeLayout relativeLayout = (RelativeLayout) parentOfParent.findViewById(R.id.addSubCategoryRl);
                        relativeLayout.setVisibility(View.GONE);
                    }
                }
            }
        };

        View.OnClickListener onAddSubCategoryClickListener = new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                UUID position = (UUID) v.getTag();

                String subCategoryName = "Fehler!";

                View parent = (View)v.getParent();
                if (parent != null) {
                    EditText inputText = (EditText) parent.findViewById(R.id.subCategoryNameEt);
                    subCategoryName = inputText.getText().toString();

                    View parentOfParent = (View)parent.getParent();
                    if (parentOfParent != null) {
                        RelativeLayout relativeLayout = (RelativeLayout) parentOfParent.findViewById(R.id.addSubCategoryRl);
                        relativeLayout.setVisibility(View.GONE);
                    }
                }

                UUID titleInvoiceCategoryId = position;

                MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(v.getContext());
                StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(v.getContext());

                InvoiceCategory newInvoiceCategory = new InvoiceCategory();
                newInvoiceCategory.setInvoiceCategoryName(subCategoryName);

                InvoiceCategory parentInvoiceCategory = new InvoiceCategory();
                parentInvoiceCategory.setInvoiceCategoryId(titleInvoiceCategoryId);
                newInvoiceCategory.setParentInvoiceCategoryDTO(parentInvoiceCategory);

                AppUser currentUser = LoginUserHelper.currentLoggedInUser(getContext());
                newInvoiceCategory.setAppUserId(currentUser.getAppUserId());
                newInvoiceCategory.setBasicStatusEnum(BasicStatusEnum.OK);
                mainDatabaseHandler.insertInvoiceCategory(newInvoiceCategory);

                handler.addObject(newInvoiceCategory.getInvoiceCategoryId().toString(), StatusDatabaseHandler.OBJECT_TYPE_INVOICE_CATEGORY, StatusDatabaseHandler.UPDATE_STATUS_ADD, new Date().getTime(), 1);

                List<InvoiceCategory> tmpInvoiceCategories = new ArrayList<>();
                SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE_CATEGORY);
                sqlBuilder.
                        isNull(MainDatabaseHandler.VAR_PARENT_INVOICE_CATEGORY_ID).
                        and().
                        isEqual(MainDatabaseHandler.VAR_BASIC_STATUS_ENUM, BasicStatusEnum.OK.name());
                List<InvoiceCategory> titleInvoiceCategories = mainDatabaseHandler.findInvoiceCategoriesSql(sqlBuilder);
                Collections.sort(titleInvoiceCategories, new InvoiceCategoryComparator());
                for (InvoiceCategory tmpTitleInvoiceCategory : titleInvoiceCategories) {
                    SqlBuilder sqlBuilder2 = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE_CATEGORY);
                    sqlBuilder2
                            .isEqual(MainDatabaseHandler.VAR_PARENT_INVOICE_CATEGORY_ID, tmpTitleInvoiceCategory.getInvoiceCategoryId().toString())
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
                    tmpInvoiceCategories.addAll(subInvoiceCategories);
                }

                internalInvoiceCategories.clear();
                internalInvoiceCategories.addAll(tmpInvoiceCategories);
                tempInvoiceCategoryDBList.clear();
                tempInvoiceCategoryDBList.addAll(tmpInvoiceCategories);

                refreshPaymentPersonList();

                mPayerLv.expand(titleInvoiceCategoryId.hashCode());
            }
        };

        mPayerLv = (ExpandableStickyListHeadersListView ) view.findViewById(R.id.invoiceDetailsPayerDialogPayerLv);

        Map<UUID, String> categoryTitles = new HashMap<>();
        AppUser currentUser = LoginUserHelper.currentLoggedInUser(getContext());

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getContext());
        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE_CATEGORY);
        sqlBuilder
                .isNull(MainDatabaseHandler.VAR_PARENT_INVOICE_CATEGORY_ID)
                .and()
                .startBracket()
                    .isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString())
                    .or()
                    .isNull(MainDatabaseHandler.VAR_APP_USER_ID)
                .endBracket();

        titleInvoiceCategories = mainDatabaseHandler.findInvoiceCategoriesSql(sqlBuilder);
        Collections.sort(titleInvoiceCategories, new InvoiceCategoryComparator());

        for (InvoiceCategory titleInvoiceCategory : titleInvoiceCategories) {
            categoryTitles.put(titleInvoiceCategory.getInvoiceCategoryId(), titleInvoiceCategory.getInvoiceCategoryName());
        }

        invoiceCategoriesAdapter = new AdapterDialogInvoiceCategoryListView(this.getContext(), invoiceCategories, onDeleteClickListener, onItemClickListener, onAddSubCategoryClickListener, categoryTitles, openSubCategoryAddingClickListener, closeSubCategoryAddingClickListener);
        mPayerLv.setAdapter(invoiceCategoriesAdapter);
        mPayerLv.setOnHeaderClickListener(new StickyListHeadersListView.OnHeaderClickListener() {
            @Override
            public void onHeaderClick(StickyListHeadersListView l, View header, int itemPosition, long headerId, boolean currentlySticky) {
                if(mPayerLv.isHeaderCollapsed(headerId)){
                    mPayerLv.expand(headerId);
                }else {
                    mPayerLv.collapse(headerId);
                }
            }

            @Override
            public boolean onHeaderLongClick(StickyListHeadersListView l, View header, int itemPosition, long headerId, boolean currentlySticky) {
                return false;
            }
        });

        internalInvoiceCategories.clear();
        internalInvoiceCategories.addAll(tempInvoiceCategoryDBList);

        updateRequestReceiver = new RequestServiceBroadcastReceiver(new RequestListener() {

            @Override
            public void requestDone(String activityClassName) {
                if (DialogInvoiceCategory.class.getSimpleName().equals(activityClassName)){
                    closeDialog();
                }
            }

            @Override
            public void generalMessage(String activityClassName, BroadcastMessageActionEnum broadcastMessageActionEnum, String message) {

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

                internalInvoiceCategories.clear();

                infoBoxRL.setVisibility(View.GONE);

                if (!searchEt.getText().toString().equals("")) {

                    for (InvoiceCategory titleInvoiceCategory : titleInvoiceCategories) {
                        Integer hashCodeOfHeader = titleInvoiceCategory.getInvoiceCategoryId().hashCode();
                        mPayerLv.expand(hashCodeOfHeader);
                    }

                    for (InvoiceCategory tmpInvoiceCategory : tempInvoiceCategoryDBList) {
                        if (tmpInvoiceCategory.getInvoiceCategoryName().toLowerCase().contains(searchEt.getText().toString().toLowerCase())) {
                            internalInvoiceCategories.add(tmpInvoiceCategory);
                        }
                    }

                    refreshPaymentPersonList();
                } else {

                    for (InvoiceCategory titleInvoiceCategory : titleInvoiceCategories) {
                        Integer hashCodeOfHeader = titleInvoiceCategory.getInvoiceCategoryId().hashCode();
                        mPayerLv.collapse(hashCodeOfHeader);
                    }

                    internalInvoiceCategories.addAll(tempInvoiceCategoryDBList);

                    refreshPaymentPersonList();
                }
            }
        };

        for (InvoiceCategory titleInvoiceCategory : titleInvoiceCategories) {
            Integer hashCodeOfHeader = titleInvoiceCategory.getInvoiceCategoryId().hashCode();
            mPayerLv.collapse(hashCodeOfHeader);
        }

        searchEt = (EditText) view.findViewById(R.id.invoiceDetailsPayerDialogSearchEt);
        infoBoxTv = (TextView) view.findViewById(R.id.infoBoxTv);
        infoBoxRL = (RelativeLayout) view.findViewById(R.id.infoBoxRL);
        infoBoxPg = (ProgressBar) view.findViewById(R.id.infoBoxPg);

        searchEt.addTextChangedListener(inputTextWatcher);

        InputMethodManager inputMethodManager =
                (InputMethodManager) getActivity().getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                searchEt.getWindowToken(), 0);

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
        filter.addAction(RequestManager.BC_ONLINE);
        this.getContext().registerReceiver(updateRequestReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.getContext().unregisterReceiver(updateRequestReceiver);
    }

    public void synchronizeAndCloseDialog(){
        if (synchronize){
            Intent sIntent = new Intent(getActivity(), RequestUpdateService.class);
            sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
            sIntent.putExtra(RequestUpdateService.ACTIVITY_CLASS, this.getClass().getSimpleName());
            getActivity().startService(sIntent);
            showInfoBox("Synchronisiere...", true);
        }else{
            closeDialog();
        }
    }

    public void closeDialog(){
        InputMethodManager inputMethodManager =
                (InputMethodManager) getActivity().getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                searchEt.getWindowToken(), 0);
        SelectInvoiceCategoryDialogListener activity = (SelectInvoiceCategoryDialogListener) getActivity();

        activity.onFinishInvoiceCategorySelectDialog(tempInvoiceCategory);
        DialogInvoiceCategory.this.dismiss();
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

        invoiceCategories.clear();
        Integer currentIndex = 0;

        if(searchEt.getText() != null && !searchEt.getText().toString().equals("")){

            boolean searchTextIsPaymentPersonInList = false;

            if (!searchTextIsPaymentPersonInList){
                for (InvoiceCategory tmpInvoiceCategory : tempInvoiceCategoryDBList) {
                    if (tmpInvoiceCategory.getInvoiceCategoryName().toLowerCase().equals(searchEt.getText().toString().toLowerCase())) {
                        searchTextIsPaymentPersonInList = true;
                        break;
                    }
                }
            }

        }

        for (InvoiceCategory tmpInvoiceCategory: internalInvoiceCategories) {
            invoiceCategories.add(tmpInvoiceCategory);
            currentIndex++;
        }

        invoiceCategoriesAdapter.notifyDataSetChanged();
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
