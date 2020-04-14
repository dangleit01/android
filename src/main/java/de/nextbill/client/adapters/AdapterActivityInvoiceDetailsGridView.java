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
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.List;

import de.nextbill.client.R;
import de.nextbill.client.helper.CostDistributionHelper;
import de.nextbill.client.model.CostDistributionItem;

public class AdapterActivityInvoiceDetailsGridView extends BaseAdapter {

    LayoutInflater inflater;
    List<CostDistributionItem> costDistributionItems;
    ViewHolder holder;
    BigDecimal maxFixedAmount;
    View.OnClickListener deleteButtonListener;
    private View.OnClickListener onMenuItemClickListener;

    public AdapterActivityInvoiceDetailsGridView() {
    }

    public AdapterActivityInvoiceDetailsGridView(Activity act, List<CostDistributionItem> map, BigDecimal maxFixedAmount, View.OnClickListener onMenuItemClickListener) {

        this.costDistributionItems = map;
        this.maxFixedAmount = maxFixedAmount;
        this.onMenuItemClickListener = onMenuItemClickListener;

        inflater = (LayoutInflater) act
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return costDistributionItems.size();
    }

    public void swapCostDistributionCollection(List<CostDistributionItem> costDistributionItems){
        this.costDistributionItems = costDistributionItems;
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

            vi = inflater.inflate(R.layout.activity_invoice_details_costs_listview_row, null);
            holder = new ViewHolder();

            holder.costDistributionRowAmountTv = (TextView) vi.findViewById(R.id.sumToPayTv);
            holder.costDistributionRowPaymentPersonTv = (TextView) vi.findViewById(R.id.costDistributionRowPaymentPersonTv);
//            holder.costDistributionActivityMenuButton = (ImageView) vi.findViewById(R.id.billingActivityMenuButton);

            vi.setTag(holder);
        } else {

            holder = (ViewHolder) vi.getTag();
        }

        String paymentPersonName = "-";

        paymentPersonName = "-";
        if (costDistributionItems.get(position).getPaymentPersonName(vi.getContext().getApplicationContext()) != null) {
            paymentPersonName = costDistributionItems.get(position).getPaymentPersonName(vi.getContext().getApplicationContext());
            paymentPersonName = cutString(paymentPersonName);
        }
        holder.costDistributionRowPaymentPersonTv.setText(paymentPersonName);

        BigDecimal amountDecimal = CostDistributionHelper.calculateAmountForCostDistributionItemPrecise(costDistributionItems.get(position), costDistributionItems, maxFixedAmount);
        holder.costDistributionRowAmountTv.setText(amountDecimal.toString() + "€");

//        holder.costDistributionActivityMenuButton.setTag(position);
//        holder.costDistributionActivityMenuButton.setOnClickListener(onMenuItemClickListener);

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

        TextView costDistributionRowAmountTv;
        TextView costDistributionRowPaymentPersonTv;
//        ImageView costDistributionActivityMenuButton;
    }

}
