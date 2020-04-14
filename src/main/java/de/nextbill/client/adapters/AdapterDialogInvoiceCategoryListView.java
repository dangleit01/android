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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.nextbill.client.R;
import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.model.InvoiceCategory;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

public class AdapterDialogInvoiceCategoryListView extends BaseAdapter implements StickyListHeadersAdapter {

    private List<InvoiceCategory> invoiceCategories;
    private LayoutInflater inflater;
    private ViewHolder holder;
    private HeaderViewHolder headerHolder;
    private View.OnClickListener deleteButtonListener;
    private View.OnClickListener itemClickListener;
    public Map<UUID, String> categoryTitles = new HashMap<>();
    private View.OnClickListener addSubCategoryListener;
    private View.OnClickListener openNewSubCategoryListener;
    private View.OnClickListener closeNewSubCategoryListener;

    private HashMap<PaymentPersonTypeEnum, String> localizationForPaymentPersonTypeEnums = new HashMap<PaymentPersonTypeEnum, String>();

    public AdapterDialogInvoiceCategoryListView(Context context, List<InvoiceCategory> invoiceCategories, View.OnClickListener deleteButtonListener, View.OnClickListener itemClickListener, View.OnClickListener addSubCategoryListener, Map<UUID, String> categoryTitles,
                                                View.OnClickListener openNewSubCategoryListener,View.OnClickListener closeNewSubCategoryListener) {
        inflater = LayoutInflater.from(context);
        this.invoiceCategories = invoiceCategories;
        this.deleteButtonListener = deleteButtonListener;
        this.itemClickListener = itemClickListener;
        this.categoryTitles = categoryTitles;
        this.addSubCategoryListener = addSubCategoryListener;
        this.openNewSubCategoryListener = openNewSubCategoryListener;
        this.closeNewSubCategoryListener = closeNewSubCategoryListener;

        localizationForPaymentPersonTypeEnums.clear();
        localizationForPaymentPersonTypeEnums.put(PaymentPersonTypeEnum.NEW, "Neu");
        localizationForPaymentPersonTypeEnums.put(PaymentPersonTypeEnum.USER, "Benutzer");
        localizationForPaymentPersonTypeEnums.put(PaymentPersonTypeEnum.BUSINESS_PARTNER, "Unternehmen");
        localizationForPaymentPersonTypeEnums.put(PaymentPersonTypeEnum.CONTACT, "Privatperson");
        localizationForPaymentPersonTypeEnums.put(PaymentPersonTypeEnum.PROJECT, "Projekt");
    }

    @Override
    public int getCount() {
        return invoiceCategories.size();
    }

    @Override
    public Object getItem(int position) {
        return invoiceCategories.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.dialog_invoice_category_listview_item, parent, false);
            holder.text = (TextView) convertView.findViewById(R.id.text);
            holder.imageView = (ImageView) convertView.findViewById(R.id.imageView10);


            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SwipeLayout swipeLayout =  (SwipeLayout) convertView.findViewById(R.id.sample1);
        swipeLayout.setShowMode(SwipeLayout.ShowMode.LayDown);
        swipeLayout.addDrag(SwipeLayout.DragEdge.Left, convertView.findViewById(R.id.bottom_wrapper));
        swipeLayout.setRightSwipeEnabled(false);

        holder.imageView.setTag(position);
        holder.imageView.setOnClickListener(deleteButtonListener);

        if (invoiceCategories.get(position).getAppUserId() == null){
            swipeLayout.setSwipeEnabled(false);
        }else {
            swipeLayout.setSwipeEnabled(true);
        }

        holder.text.setText(invoiceCategories.get(position).getInvoiceCategoryName());

        holder.text.setTag(position);
        holder.text.setOnClickListener(itemClickListener);

        return convertView;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            headerHolder = new HeaderViewHolder();
            convertView = inflater.inflate(R.layout.dialog_invoice_category_listview_section, parent, false);
            headerHolder.text = (TextView) convertView.findViewById(R.id.textSeparator);
            headerHolder.addSubCategoryIv = (ImageView) convertView.findViewById(R.id.addSubCategoryIv);
            headerHolder.abortSubCategoryAddIv = (ImageView) convertView.findViewById(R.id.abortSubCategoryAddIv);
            headerHolder.addSubCategoryReadyIv = (ImageView) convertView.findViewById(R.id.addSubCategoryReadyIv);
            headerHolder.subCategoryNameEt = (EditText) convertView.findViewById(R.id.subCategoryNameEt);
            headerHolder.addSubCategoryRl = (RelativeLayout) convertView.findViewById(R.id.addSubCategoryRl);

            convertView.setTag(headerHolder);
        } else {
            headerHolder = (HeaderViewHolder) convertView.getTag();
        }

        headerHolder.addSubCategoryIv.setOnClickListener(openNewSubCategoryListener);

        headerHolder.abortSubCategoryAddIv.setOnClickListener(closeNewSubCategoryListener);

        headerHolder.addSubCategoryReadyIv.setTag(invoiceCategories.get(position).getParentInvoiceCategoryDTO().getInvoiceCategoryId());
        headerHolder.addSubCategoryReadyIv.setOnClickListener(addSubCategoryListener);

        String headerText = categoryTitles.get(invoiceCategories.get(position).getParentInvoiceCategoryDTO().getInvoiceCategoryId());
        headerHolder.text.setText(headerText);
        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        UUID hashCodeParentInvoiceCategoryId = invoiceCategories.get(position).getParentInvoiceCategoryDTO().getInvoiceCategoryId();
        if (hashCodeParentInvoiceCategoryId == null){
            return 238423277;
        }
        return hashCodeParentInvoiceCategoryId.hashCode();
    }

    public class HeaderViewHolder {
        TextView text;
        ImageView addSubCategoryIv;
        ImageView abortSubCategoryAddIv;
        ImageView addSubCategoryReadyIv;
        EditText subCategoryNameEt;
        RelativeLayout addSubCategoryRl;
    }

    class ViewHolder {
        TextView text;
        ImageView imageView;
    }

}