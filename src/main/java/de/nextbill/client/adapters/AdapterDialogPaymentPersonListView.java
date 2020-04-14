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

package de.nextbill.client.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;

import java.util.HashMap;
import java.util.List;

import de.nextbill.client.R;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.helper.LoginUserHelper;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.BusinessPartner;
import de.nextbill.client.model.IPaymentPerson;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

public class AdapterDialogPaymentPersonListView extends BaseAdapter implements StickyListHeadersAdapter {

    private List<IPaymentPerson> paymentPersons;
    private LayoutInflater inflater;
    private ViewHolder holder;
    private Boolean showDeleteButtonForBusinessPartner;
    private Boolean showDeleteButtonForUserContacts;
    View.OnClickListener deleteButtonListener;
    View.OnClickListener itemClickListener;

    private HashMap<PaymentPersonTypeEnum, String> localizationForPaymentPersonTypeEnums = new HashMap<PaymentPersonTypeEnum, String>();

    public AdapterDialogPaymentPersonListView(Context context, List<IPaymentPerson> paymentPersons, View.OnClickListener deleteButtonListener, View.OnClickListener itemClickListener, boolean showDeleteButtonForBusinessPartner, boolean showDeleteButtonForUserContacts) {
        inflater = LayoutInflater.from(context);
        this.paymentPersons = paymentPersons;
        this.showDeleteButtonForBusinessPartner = showDeleteButtonForBusinessPartner;
        this.showDeleteButtonForUserContacts = showDeleteButtonForUserContacts;
        this.deleteButtonListener = deleteButtonListener;
        this.itemClickListener = itemClickListener;

        localizationForPaymentPersonTypeEnums.clear();
        localizationForPaymentPersonTypeEnums.put(PaymentPersonTypeEnum.NEW, "Neu");
        localizationForPaymentPersonTypeEnums.put(PaymentPersonTypeEnum.USER, "Benutzer");
        localizationForPaymentPersonTypeEnums.put(PaymentPersonTypeEnum.BUSINESS_PARTNER, "Unternehmen");
        localizationForPaymentPersonTypeEnums.put(PaymentPersonTypeEnum.CONTACT, "Privatperson");
        localizationForPaymentPersonTypeEnums.put(PaymentPersonTypeEnum.PROJECT, "Projekt");
    }

    @Override
    public int getCount() {
        return paymentPersons.size();
    }

    @Override
    public Object getItem(int position) {
        return paymentPersons.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.dialog_payment_person_listview_item, parent, false);
            holder.text = (TextView) convertView.findViewById(R.id.text);
            holder.imageView = (ImageView) convertView.findViewById(R.id.imageView10);

//            if ((paymentPersons.get(position).getPaymentPersonEnum().equals(PaymentPersonTypeEnum.BUSINESS_PARTNER) && !showDeleteButtonForBusinessPartner) ||
//                    (paymentPersons.get(position).getPaymentPersonEnum().equals(PaymentPersonTypeEnum.CONTACT) && !showDeleteButtonForUserContacts) || paymentPersons.get(position).getPaymentPersonEnum().equals(PaymentPersonTypeEnum.USER)){
//                holder.imageView.setVisibility(View.GONE);
//            }

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SwipeLayout swipeLayout =  (SwipeLayout) convertView.findViewById(R.id.sample1);
        swipeLayout.setShowMode(SwipeLayout.ShowMode.LayDown);
        swipeLayout.addDrag(SwipeLayout.DragEdge.Left, convertView.findViewById(R.id.bottom_wrapper));
        swipeLayout.setRightSwipeEnabled(false);

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(convertView.getContext());

        if ((paymentPersons.get(position).getPaymentPersonEnum().equals(PaymentPersonTypeEnum.CONTACT) && showDeleteButtonForUserContacts)
                || (paymentPersons.get(position).getPaymentPersonEnum().equals(PaymentPersonTypeEnum.USER) && (currentUser != null && !paymentPersons.get(position).getPaymentPersonId().equals(currentUser.getAppUserId())))
                || paymentPersons.get(position).getPaymentPersonEnum().equals(PaymentPersonTypeEnum.PROJECT)){
            swipeLayout.setSwipeEnabled(true);
        }else if (paymentPersons.get(position).getPaymentPersonEnum().equals(PaymentPersonTypeEnum.BUSINESS_PARTNER) && showDeleteButtonForBusinessPartner){
            MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(convertView.getContext());
            List<BusinessPartner> businessPartners = mainDatabaseHandler.findBusinessPartners(MainDatabaseHandler.VAR_BUSINESS_PARTNER_ID, paymentPersons.get(position).getPaymentPersonId().toString());

            if (!businessPartners.isEmpty() && businessPartners.get(0).getAppUserId() != null){
                swipeLayout.setSwipeEnabled(true);
            }else{
                swipeLayout.setSwipeEnabled(false);
            }
        }else{
            swipeLayout.setSwipeEnabled(false);
        }

        holder.text.setText(paymentPersons.get(position).getPaymentPersonName());

        holder.imageView.setTag(position);
        holder.imageView.setOnClickListener(deleteButtonListener);

        holder.text.setTag(position);
        holder.text.setOnClickListener(itemClickListener);

        return convertView;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;
        if (convertView == null) {
            holder = new HeaderViewHolder();
            convertView = inflater.inflate(R.layout.dialog_payment_person_listview_section, parent, false);
            holder.text = (TextView) convertView.findViewById(R.id.textSeparator);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }
        //set header text as first char in name


        String headerText = localizationForPaymentPersonTypeEnums.get(paymentPersons.get(position).getVirtualPaymentPersonEnum());
        holder.text.setText(headerText);
        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        return paymentPersons.get(position).getVirtualPaymentPersonEnum().toString().charAt(0);
    }

    class HeaderViewHolder {
        TextView text;
    }

    class ViewHolder {
        TextView text;
        ImageView imageView;
    }

}