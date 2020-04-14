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

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import de.nextbill.client.R;
import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.helper.LoginUserHelper;
import de.nextbill.client.interfaces.CurrencyTextWatcher;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.IPaymentPerson;
import de.nextbill.client.utils.BillingListItem;

public class AdapterActivityBillingListView extends BaseAdapter {

    LayoutInflater inflater;
    List<BillingListItem> billingListItems;
    ViewHolder holder;
    private View.OnClickListener onMenuItemClickListener;

    public AdapterActivityBillingListView() {
    }

    public AdapterActivityBillingListView(Activity act, List<BillingListItem> map, View.OnClickListener onMenuItemClickListener) {

        this.billingListItems = map;
        this.onMenuItemClickListener = onMenuItemClickListener;

        inflater = (LayoutInflater) act
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return billingListItems.size();
    }

    public void swapCostDistributionCollection(List<BillingListItem> billingListItem){
        this.billingListItems = billingListItem;
        this.notifyDataSetChanged();
    }

    public Object getItem(int arg0) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        View vi = convertView;
        if (convertView == null) {

            vi = inflater.inflate(R.layout.activity_billing_listview_row, null);
            holder = new ViewHolder();

            holder.billingRowInvoicePayerTv = (TextView) vi.findViewById(R.id.billingRowInvoicePayerTv);
            holder.billingRowCostPayerTv = (TextView) vi.findViewById(R.id.billingRowCostPayerTv);
            holder.sumToPayTv = (TextView) vi.findViewById(R.id.sumToPayTv);
            holder.billingActivityMenuButton = (ImageView) vi.findViewById(R.id.billingActivityIb);

            holder.costPayerSumTv = (TextView) vi.findViewById(R.id.costPayerSumTv);
            holder.costPayerCategoriesTv = (TextView) vi.findViewById(R.id.costPayerCategoriesTv);
            holder.invoicePayerSumTv = (TextView) vi.findViewById(R.id.invoicePayerSumTv);
            holder.invoicePayerCategoriesTv = (TextView) vi.findViewById(R.id.invoicePayerCategoriesTv);

            vi.setTag(holder);
        } else {

            holder = (ViewHolder) vi.getTag();
        }



        boolean hasCurrentUser = false;
        AppUser currentUser = LoginUserHelper.currentLoggedInUser(vi.getContext());
        IPaymentPerson invoicePayer = billingListItems.get(position).getInvoicePayer();
        IPaymentPerson costPayer = billingListItems.get(position).getCostPayer();
        if (currentUser != null && invoicePayer != null && invoicePayer.getPaymentPersonEnum().equals(PaymentPersonTypeEnum.USER) && invoicePayer.getPaymentPersonId().equals(currentUser.getAppUserId())){
            hasCurrentUser = true;
        }else if (currentUser != null && costPayer != null && costPayer.getPaymentPersonEnum().equals(PaymentPersonTypeEnum.USER) && costPayer.getPaymentPersonId().equals(currentUser.getAppUserId())){
            hasCurrentUser = true;
        }
        if (!hasCurrentUser){
            holder.billingActivityMenuButton.setVisibility(View.GONE);
        }else{
            holder.billingActivityMenuButton.setVisibility(View.VISIBLE);
        }

        if (currentUser != null && costPayer != null){
            holder.billingRowCostPayerTv.setText(billingListItems.get(position).getCostPayer().getPaymentPersonName());

            holder.costPayerSumTv.setText(billingListItems.get(position).getCostPayer().getPaymentPersonName() + ": " + CurrencyTextWatcher.bigDecimalToString(billingListItems.get(position).getCostsFromCostPayer()) + " €");

            String categoriesText = "";

            for (int i = 0; i < billingListItems.get(position).getInvoiceCategoriesOfCostPayer().size(); i++) {

                String categoryItem = billingListItems.get(position).getInvoiceCategoriesOfCostPayer().get(i).getInvoiceCategoryName();

                if (i == 0){
                    categoriesText = categoryItem;
                }else if (i > 0){
                    categoriesText = categoriesText +", "+ categoryItem;
                }
            }
            if (!billingListItems.get(position).getInvoiceCategoriesOfCostPayer().isEmpty()){
                holder.costPayerCategoriesTv.setText("Kategorie(n): " +categoriesText);
            }else{
                holder.costPayerCategoriesTv.setText("Keine Kategorien gefunden.");
            }

        }else{
            holder.billingRowCostPayerTv.setText("");
            holder.costPayerCategoriesTv.setText("Keine Kategorien gefunden.");
            holder.costPayerSumTv.setText(billingListItems.get(position).getInvoicePayer().getPaymentPersonName() + ": - ");
        }

        if (currentUser != null && invoicePayer != null){
            holder.billingRowInvoicePayerTv.setText(billingListItems.get(position).getInvoicePayer().getPaymentPersonName());

            holder.invoicePayerSumTv.setText(billingListItems.get(position).getInvoicePayer().getPaymentPersonName() + ": " + CurrencyTextWatcher.bigDecimalToString(billingListItems.get(position).getCostsFromInvoicePayer()) + " €");

            String categoriesText = "";

            for (int i = 0; i < billingListItems.get(position).getInvoiceCategoriesOfInvoicePayer().size(); i++) {

                String categoryItem = billingListItems.get(position).getInvoiceCategoriesOfInvoicePayer().get(i).getInvoiceCategoryName();

                if (i == 0){
                    categoriesText = categoryItem;
                }else if (i > 0){
                    categoriesText = categoriesText +", "+ categoryItem;
                }
            }

            if (!billingListItems.get(position).getInvoiceCategoriesOfInvoicePayer().isEmpty()){
                holder.invoicePayerCategoriesTv.setText("Kategorie(n): " +categoriesText);
            }else{
                holder.invoicePayerCategoriesTv.setText("Keine Kategorien gefunden.");
            }

        }else{
            holder.billingRowInvoicePayerTv.setText("");
            holder.invoicePayerCategoriesTv.setText("Keine Kategorien gefunden.");
            holder.invoicePayerSumTv.setText(billingListItems.get(position).getInvoicePayer().getPaymentPersonName() + ": - ");
        }

        holder.sumToPayTv.setText(CurrencyTextWatcher.bigDecimalToString(billingListItems.get(position).getSumToBePaid())  + " €");

        holder.billingActivityMenuButton.setTag(position);
        holder.billingActivityMenuButton.setOnClickListener(onMenuItemClickListener);

        return vi;
    }

    private String cutString(String input) {
        String resultString = input;

        if (input.length() >= 30) {
            resultString = input.substring(0, 12);
            resultString += "...";
        }
        return resultString;
    }

    static class ViewHolder {

        TextView billingRowInvoicePayerTv;
        TextView billingRowCostPayerTv;
        TextView sumToPayTv;
        TextView costPayerSumTv;
        TextView costPayerCategoriesTv;
        TextView invoicePayerSumTv;
        TextView invoicePayerCategoriesTv;
        ImageView billingActivityMenuButton;
    }

}
