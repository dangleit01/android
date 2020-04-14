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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.nextbill.client.R;
import de.nextbill.client.adapters.AdapterActivityBillingListView;
import de.nextbill.client.breceiver.RequestListener;
import de.nextbill.client.breceiver.RequestServiceBroadcastReceiver;
import de.nextbill.client.database.IOHelper;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.enums.BroadcastMessageActionEnum;
import de.nextbill.client.enums.LoginStatusEnum;
import de.nextbill.client.enums.OnlineStatusEnum;
import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.model.IPaymentPerson;
import de.nextbill.client.network.RequestManager;
import de.nextbill.client.network.RequestUpdateService;
import de.nextbill.client.utils.BillingListItem;

public class ActivityBilling extends BaseActivity implements DialogSumInput.EditSumDialogListener{

    private ListView billingListLv;
    private ProgressBar progressBar4;
    private RelativeLayout progressBarLinearLayout;

    private List<BillingListItem> billingListItems = new ArrayList<>();
    private AdapterActivityBillingListView bindingData;
    static int ACTIVITY_BILLING = 548;

    private RequestServiceBroadcastReceiver updateRequestReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLayoutInflater().inflate(R.layout.activity_billing, frameLayout);
        this.setTitle("Abrechnungen");

        View.OnClickListener onMenuItemClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                final Integer position = (Integer) view.getTag();
                showReportGenerator(position);
            }
        };

        progressBar4 = (ProgressBar) findViewById(R.id.progressBar4);

//        billingListItems = createBillingListItems();
        bindingData = new AdapterActivityBillingListView(this, billingListItems, onMenuItemClickListener);
        billingListLv = (ListView) findViewById(R.id.costDistributionLv);
        billingListLv.setAdapter(bindingData);

        progressBarLinearLayout = (RelativeLayout) findViewById(R.id.progressBarLinearLayout);

        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        handler.addObject("", StatusDatabaseHandler.OBJECT_TYPE_BILLING_LIST_ITEMS, StatusDatabaseHandler.UPDATE_STATUS_GET, new Date().getTime(), 1);

        Intent rIntent = new Intent(this, RequestUpdateService.class);
        rIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
        startService(rIntent);

        updateRequestReceiver = new RequestServiceBroadcastReceiver(new RequestListener() {

            @Override
            public void requestDone(String activityClassName) { }

            @Override
            public void generalMessage(String activityClassName, BroadcastMessageActionEnum broadcastMessageActionEnum, String message) {
                if (activityClassName.equals(ActivityBilling.class.getSimpleName()) && BroadcastMessageActionEnum.READY.equals(broadcastMessageActionEnum)){
                    Type listType = new TypeToken<List<BillingListItem>>() {
                    }.getType();
                    GsonBuilder builder = new GsonBuilder();
                    builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
                        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                            return new Date(json.getAsJsonPrimitive().getAsLong());
                        }
                    });
                    Gson gson = builder.create();
                    List<BillingListItem> billingListItemsTmp = gson.fromJson(message, listType);

                    billingListItems.clear();
                    billingListItems.addAll(billingListItemsTmp);
                    bindingData.notifyDataSetChanged();

                    progressBarLinearLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void loginMessage(LoginStatusEnum loginStatusEnum) {

            }

            @Override
            public void onlineStatusMessage(OnlineStatusEnum onlineStatusEnum) {

            }
        });

        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.baseline_menu_white_24);
        getSupportActionBar().setHomeAsUpIndicator(drawable);
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

    public void showReportGenerator(Integer position){
        Intent intent = new Intent(ActivityBilling.this, ActivityBillingCreate.class);

        BillingListItem billingListItem = billingListItems.get(position);
        IPaymentPerson invoicePayer = billingListItem.getInvoicePayer();
        IPaymentPerson costPayer = billingListItem.getCostPayer();
        if (invoicePayer.getPaymentPersonEnum().equals(PaymentPersonTypeEnum.CONTACT)){
            intent.putExtra("userContactId", invoicePayer.getPaymentPersonId().toString());
        }else{
            intent.putExtra("userContactId", costPayer.getPaymentPersonId().toString());
        }

        BigDecimal sumOfInvoiceDecimal = new BigDecimal(0);
        if (PaymentPersonTypeEnum.USER.equals(billingListItem.getInvoicePayer())){
            sumOfInvoiceDecimal = billingListItem.getSumToBePaid().setScale(2, BigDecimal.ROUND_HALF_EVEN).multiply(new BigDecimal(-1));
        }else{
            sumOfInvoiceDecimal = billingListItem.getSumToBePaid().setScale(2, BigDecimal.ROUND_HALF_EVEN);
        }
        String sumOfInvoice = sumOfInvoiceDecimal.toString();
        intent.putExtra("sum", sumOfInvoice);

        startActivityForResult(intent, ACTIVITY_BILLING);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == ACTIVITY_BILLING && resultCode == RESULT_OK && intent.getStringExtra("refresh") != null) {

            billingListItems.clear();
            bindingData.notifyDataSetChanged();

            progressBarLinearLayout.setVisibility(View.VISIBLE);

            StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
            handler.addObject("", StatusDatabaseHandler.OBJECT_TYPE_BILLING_LIST_ITEMS, StatusDatabaseHandler.UPDATE_STATUS_GET, new Date().getTime(), 1);

            Intent rIntent = new Intent(this, RequestUpdateService.class);
            rIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_TYPE_PATCH_UPDATE);
            startService(rIntent);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

//        getMenuInflater().inflate(R.menu.invoice_details_cost_distribution, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onFinishSumEditDialog(BigDecimal currentSum, Integer itemIdentifier) {
        BillingListItem billingListItem = billingListItems.get(itemIdentifier);
        if (currentSum != null){

        }
    }
}
