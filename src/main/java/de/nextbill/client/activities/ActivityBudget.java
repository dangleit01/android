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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.nextbill.client.R;
import de.nextbill.client.adapters.AdapterActivityBudgetListView;
import de.nextbill.client.breceiver.RequestListener;
import de.nextbill.client.breceiver.RequestServiceBroadcastReceiver;
import de.nextbill.client.database.IOHelper;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.enums.BroadcastMessageActionEnum;
import de.nextbill.client.enums.LoginStatusEnum;
import de.nextbill.client.enums.OnlineStatusEnum;
import de.nextbill.client.enums.PaymentTypeEnum;
import de.nextbill.client.network.RequestManager;
import de.nextbill.client.network.RequestUpdateService;
import de.nextbill.client.pojos.BudgetDTO;

public class ActivityBudget extends BaseActivity {

    private ListView budgetLv;
    private RelativeLayout progressBarLinearLayout;

    private List<BudgetDTO> budgets = new ArrayList<>();
    private AdapterActivityBudgetListView budgetData;
    static int ACTIVITY_BUDGET = 547;

    private RequestServiceBroadcastReceiver updateRequestReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLayoutInflater().inflate(R.layout.activity_budget, frameLayout);
        this.setTitle("Budgets");

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());

        View.OnClickListener onMenuItemClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                final Integer position = (Integer) view.getTag();
                showDetails(position);
            }
        };

//        billingListItems = createBillingListItems();
        budgetData = new AdapterActivityBudgetListView(this, budgets, onMenuItemClickListener);
        budgetLv = (ListView) findViewById(R.id.budgetLv);
        budgetLv.setAdapter(budgetData);

        progressBarLinearLayout = (RelativeLayout) findViewById(R.id.progressBarLinearLayout);

        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        handler.addObject("", StatusDatabaseHandler.OBJECT_TYPE_BUDGET, StatusDatabaseHandler.UPDATE_STATUS_GET_LIST, new Date().getTime(), 1);

        Intent rIntent = new Intent(this, RequestUpdateService.class);
        rIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
        startService(rIntent);

        updateRequestReceiver = new RequestServiceBroadcastReceiver(new RequestListener() {

            @Override
            public void requestDone(String activityClassName) { }

            @Override
            public void generalMessage(String activityClassName, BroadcastMessageActionEnum broadcastMessageActionEnum, String message) {
                if (activityClassName.equals(ActivityBudget.class.getSimpleName()) && BroadcastMessageActionEnum.READY.equals(broadcastMessageActionEnum)){
                    Type listType = new TypeToken<List<BudgetDTO>>() {
                    }.getType();
                    GsonBuilder builder = new GsonBuilder();
                    builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
                        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
                            try {
                                return simpleDateFormat.parse(json.getAsJsonPrimitive().getAsString());
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    });
                    Gson gson = builder.create();
                    List<BudgetDTO> budgetDTOS = gson.fromJson(message, listType);

                    budgets.clear();
                    budgets.addAll(budgetDTOS);
                    budgetData.notifyDataSetChanged();

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

    public void showDetails(Integer position){
        Intent intent = new Intent(ActivityBudget.this, ActivityBudgetDetails.class);

        BudgetDTO budgetDTO = budgets.get(position);

        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        String budgetAsString = gson.toJson(budgetDTO);

        intent.putExtra("budget", budgetAsString);

        startActivityForResult(intent, ACTIVITY_BUDGET);
    }

    public void addBudget(){
        Intent intent = new Intent(ActivityBudget.this, ActivityBudgetDetails.class);

        BudgetDTO budgetDTO = new BudgetDTO();
        budgetDTO.setSum(new BigDecimal(0));
        budgetDTO.setPaymentTypeEnum(PaymentTypeEnum.NOT_DEFINED);

        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        String budgetAsString = gson.toJson(budgetDTO);

        intent.putExtra("budget", budgetAsString);

        startActivityForResult(intent, ACTIVITY_BUDGET);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == ACTIVITY_BUDGET && resultCode == RESULT_OK) {

            budgets.clear();
            budgetData.notifyDataSetChanged();

            progressBarLinearLayout.setVisibility(View.VISIBLE);

            StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
            handler.addObject("", StatusDatabaseHandler.OBJECT_TYPE_BUDGET, StatusDatabaseHandler.UPDATE_STATUS_GET_LIST, new Date().getTime(), 1);

            Intent rIntent = new Intent(this, RequestUpdateService.class);
            rIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
            startService(rIntent);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.budget, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.manualAddBudgetMenuButton:
                addBudget();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


}
