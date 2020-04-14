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
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.nextbill.client.R;
import de.nextbill.client.adapters.AdapterFragmentConflictMessagesListView;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.enums.BasicDataType;
import de.nextbill.client.interfaces.IFragmentToActivity;
import de.nextbill.client.model.BasicData;
import de.nextbill.client.model.Invoice;
import de.nextbill.client.network.RequestUpdateService;
import de.nextbill.client.utils.SqlBuilder;

public class ActivityMessagingTabConflict extends Fragment {

    private IFragmentToActivity mCallback;

    private SwipeRefreshLayout swipeContainer;

    ListView list;

    AdapterFragmentConflictMessagesListView bindingData;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_main_fragment_open_invoices, container, false);

        View.OnClickListener onFirstActionClick = new View.OnClickListener(){

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
                handler.addObject(basicDataMessage.getBasicDataId().toString(), StatusDatabaseHandler.OBJECT_TYPE_PAYMENT_MISTAKE, StatusDatabaseHandler.UPDATE_STATUS_DELETE, new Date().getTime(), 1);

                Intent sIntent = new Intent(getContext(), RequestUpdateService.class);
                sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
                getActivity().startService(sIntent);

                refreshMessagesList();
            }
        };

        View.OnClickListener onSecondActionClick = new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Integer position = (Integer) v.getTag();
                BasicData basicDataMessage = bindingData.getItem(position);

                MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getContext());

                List<Invoice> tempInvoices = mainDatabaseHandler.findInvoices(MainDatabaseHandler.VAR_INVOICE_ID, basicDataMessage.getObject1Id());

                if (!tempInvoices.isEmpty()) {
                    Intent intent = new Intent(getActivity().getApplicationContext(), ActivityInvoiceDetails.class);
                    intent.putExtra("idInvoice", basicDataMessage.getObject1Id());
                    getActivity().startActivity(intent);
                    getActivity().overridePendingTransition(R.anim.activity_in1, R.anim.activity_in2);
                }else{
                    Toast.makeText(getContext(), "Rechnung wurde bereits gelöscht.", Toast.LENGTH_LONG).show();
                }
            }
        };

        bindingData = new AdapterFragmentConflictMessagesListView(this.getActivity(), onFirstActionClick, onSecondActionClick);

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
            sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.MISTAKE_MESSAGE.name())
                    .or()
                    .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.CHANGED_MESSAGE.name());

            List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

            bindingData.swapMessagesList(basicDatas);
            bindingData.notifyDataSetChanged();

            IFragmentToActivity iFragmentToActivity = (IFragmentToActivity) getActivity();
            if (iFragmentToActivity != null){
                iFragmentToActivity.refreshTabCounter(null, basicDatas.size());

                if (!basicDatas.isEmpty()){
                    iFragmentToActivity.whichTabToShow(null, true, true);
                }else{
                    iFragmentToActivity.whichTabToShow(null, false, true);
                }
            }
            }
        }, 50);



    }
}
