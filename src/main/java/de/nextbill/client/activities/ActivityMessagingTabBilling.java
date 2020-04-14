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

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import de.nextbill.client.R;
import de.nextbill.client.adapters.AdapterFragmentBillingMessagesListView;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.enums.BasicDataType;
import de.nextbill.client.enums.BillingStatusEnum;
import de.nextbill.client.enums.MessageType;
import de.nextbill.client.interfaces.IFragmentToActivity;
import de.nextbill.client.model.BasicData;
import de.nextbill.client.network.RequestUpdateService;
import de.nextbill.client.pojos.BillingPayment;
import de.nextbill.client.pojos.MessageDTO;
import de.nextbill.client.utils.SqlBuilder;

public class ActivityMessagingTabBilling extends Fragment {

    private IFragmentToActivity mCallback;

    private SwipeRefreshLayout swipeContainer;

    ListView list;

    AdapterFragmentBillingMessagesListView bindingData;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_main_fragment_open_invoices, container, false);

        View.OnClickListener onFirstActionClick = new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Integer position = (Integer) v.getTag();
                BasicData basicDataMessage = bindingData.getItem(position);

                UUID billingId = UUID.fromString(basicDataMessage.getObject1Id());

                Type listType = new TypeToken<MessageDTO>() {}.getType();
                GsonBuilder builder = new GsonBuilder();
                Gson gson = builder.create();
                MessageDTO messageDTO = gson.fromJson(basicDataMessage.getValue(), listType);
                MessageType messageType = messageDTO.getMessageType();

                BillingPayment billingPayment = new BillingPayment();
                billingPayment.setBillingId(billingId);
                if (MessageType.TO_PAY.equals(messageType)){
                    billingPayment.setBillingStatusEnum(BillingStatusEnum.PAID);
                }else if (MessageType.PAID.equals(messageType) || MessageType.WAIT_FOR_PAYMENT.equals(messageType)){
                    billingPayment.setBillingStatusEnum(BillingStatusEnum.PAYMENT_CONFIRMED);
                }else if (MessageType.PAYMENT_CONFIRMED.equals(messageType)){
                    billingPayment.setBillingStatusEnum(BillingStatusEnum.FINISHED);
                }

                String jsonString = gson.toJson(billingPayment);

                MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getContext());

                SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
                sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.MESSAGE.name())
                        .and()
                        .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_OBJECT_1_ID, billingId.toString())
                        .and()
                        .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_OBJECT_1_CLASS, "Billing");

                List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);
                if (!basicDatas.isEmpty()){
                    List<BasicData> basicDatas1 = new ArrayList<>();
                    basicDatas1.add(basicDatas.get(0));
                    mainDatabaseHandler.deleteBasicDatas(basicDatas1);
                }

                StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getContext());
                handler.addObject(jsonString, StatusDatabaseHandler.OBJECT_TYPE_BILLING, StatusDatabaseHandler.UPDATE_STATUS_UPDATE, new Date().getTime(), 1);

                Intent sIntent = new Intent(getContext(), RequestUpdateService.class);
                sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
                getActivity().startService(sIntent);

                refreshMessagesList();
            }
        };

        View.OnClickListener onAbortActionClick = new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Integer position = (Integer) v.getTag();
                BasicData basicDataMessage = bindingData.getItem(position);

                MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getContext());

                SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
                sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_ID, basicDataMessage.getBasicDataId().toString());

                List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);
                if (!basicDatas.isEmpty()){
                    List<BasicData> basicDatas1 = new ArrayList<>();
                    basicDatas1.add(basicDatas.get(0));
                    mainDatabaseHandler.deleteBasicDatas(basicDatas1);
                }

                StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getContext());
                handler.addObject(basicDataMessage.getObject1Id(), StatusDatabaseHandler.OBJECT_TYPE_BILLING, StatusDatabaseHandler.UPDATE_STATUS_DELETE, new Date().getTime(), 1);

                Intent sIntent = new Intent(getContext(), RequestUpdateService.class);
                sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
                getActivity().startService(sIntent);

                refreshMessagesList();
            }
        };

        View.OnClickListener onDeleteMessageClick = new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Integer position = (Integer) v.getTag();
                BasicData basicDataMessage = bindingData.getItem(position);

                MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getContext());

                SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
                sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_ID, basicDataMessage.getBasicDataId().toString());

                List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);
                if (!basicDatas.isEmpty()){
                    List<BasicData> basicDatas1 = new ArrayList<>();
                    basicDatas1.add(basicDatas.get(0));
                    mainDatabaseHandler.deleteBasicDatas(basicDatas1);
                }

                StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getContext());
                handler.addObject(basicDataMessage.getBasicDataId().toString(), StatusDatabaseHandler.OBJECT_TYPE_BASIC_DATA, StatusDatabaseHandler.UPDATE_STATUS_DELETE, new Date().getTime(), 1);

                Intent sIntent = new Intent(getContext(), RequestUpdateService.class);
                sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
                getActivity().startService(sIntent);

                refreshMessagesList();
            }
        };

        bindingData = new AdapterFragmentBillingMessagesListView(this.getActivity(), onFirstActionClick, onAbortActionClick, onDeleteMessageClick);

        list = (ListView) view.findViewById(R.id.list);

        list.setAdapter(bindingData);

        list.setSelector(R.drawable.activity_main_list_selector);

        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        swipeContainer.setBackgroundColor(Color.rgb(230, 230, 230));
        swipeContainer.setProgressBackgroundColorSchemeColor(Color.rgb(230, 230, 230));

        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        refreshMessagesList();

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mCallback = (IFragmentToActivity) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement IFragmentToActivity");
        }
    }

    @Override
    public void onDetach() {
        mCallback = null;
        super.onDetach();
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

    public void refreshMessagesList(){

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
            list.destroyDrawingCache();

            MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getContext());

            SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
            sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.MESSAGE.name())
                        .or()
                        .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.BUDGET_MESSAGE.name());

            List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

            bindingData.swapMessagesList(basicDatas);
            bindingData.notifyDataSetChanged();

            IFragmentToActivity iFragmentToActivity = (IFragmentToActivity) getActivity();
            if (iFragmentToActivity != null){
                iFragmentToActivity.refreshTabCounter(basicDatas.size(), null);

                if (!basicDatas.isEmpty()){
                    iFragmentToActivity.whichTabToShow(true, null, true);
                }else{
                    iFragmentToActivity.whichTabToShow(false, null, true);
                }
            }
            }
        }, 50);



    }
}
