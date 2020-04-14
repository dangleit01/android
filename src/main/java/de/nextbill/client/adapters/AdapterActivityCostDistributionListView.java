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
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import de.nextbill.client.R;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.enums.CostDistributionItemTypeEnum;
import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.enums.Right;
import de.nextbill.client.helper.CostDistributionHelper;
import de.nextbill.client.helper.RightHelper;
import de.nextbill.client.model.CostDistributionItem;
import de.nextbill.client.model.Invoice;
import de.nextbill.client.utils.SqlBuilder;

public class AdapterActivityCostDistributionListView extends BaseAdapter {

    private LayoutInflater inflater;
    private ViewHolder holder;
    private BigDecimal maxFixedAmount;
    private View.OnClickListener deleteButtonListener;
    private View.OnClickListener articleSelectButtonListener;
    private View.OnClickListener onMenuItemClickListener;

    private List<CostDistributionItem> costDistributionItems;

    private boolean showArticleSelectButton = false;

    public AdapterActivityCostDistributionListView() {
    }

    public AdapterActivityCostDistributionListView(Activity act, List<CostDistributionItem> map, BigDecimal maxFixedAmount, View.OnClickListener deleteButtonListener, View.OnClickListener articleSelectButtonListener, View.OnClickListener onMenuItemClickListener, boolean showArticleSelectButton) {

        this.deleteButtonListener = deleteButtonListener;
        this.articleSelectButtonListener = articleSelectButtonListener;
        this.costDistributionItems = map;
        this.maxFixedAmount = maxFixedAmount;
        this.onMenuItemClickListener = onMenuItemClickListener;
        this.showArticleSelectButton = showArticleSelectButton;

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

            vi = inflater.inflate(R.layout.activity_cost_distribution_listview_row, null);
            holder = new ViewHolder();

            holder.costDistributionRowAmountTv = (TextView) vi.findViewById(R.id.sumToPayTv);
            holder.costDistributionRowQuotaTv = (TextView) vi.findViewById(R.id.costDistributionRowQuotaTv);
            holder.costDistributionRowPaymentPersonTv = (TextView) vi.findViewById(R.id.costDistributionRowPaymentPersonTv);
            holder.deleteCostDistributionItemButton = (ImageButton) vi.findViewById(R.id.deleteCostDistributionItemButton);
            holder.costDistributionActivityMenuButton = (ImageButton) vi.findViewById(R.id.billingActivityIb);
            holder.articleSelectCostDistributionItemButton = (ImageButton) vi.findViewById(R.id.articleSelectCostDistributionItemButton);

            vi.setTag(holder);
        } else {

            holder = (ViewHolder) vi.getTag();
        }

        boolean hasScanRight = RightHelper.hasUserRight(vi.getContext(), Right.OCR);
        if (!hasScanRight || !showArticleSelectButton){
            holder.articleSelectCostDistributionItemButton.setVisibility(View.GONE);
        }else{
            holder.articleSelectCostDistributionItemButton.setVisibility(View.VISIBLE);
        }

        String paymentPersonName = "-";
        String quota = "-";
        String amount = "-";

        paymentPersonName = "-";
        if (costDistributionItems.get(position).getPaymentPersonName(vi.getContext().getApplicationContext()) != null) {
            paymentPersonName = costDistributionItems.get(position).getPaymentPersonName(vi.getContext().getApplicationContext());
            paymentPersonName = cutString(paymentPersonName);
        }
        holder.costDistributionRowPaymentPersonTv.setText(paymentPersonName);

        quota = "-";
        if (costDistributionItems.get(position).getCostDistributionItemTypeEnum() != null) {
            if (costDistributionItems.get(position).getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.FIXED_AMOUNT)){
                quota = "Fest";
            }else if (costDistributionItems.get(position).getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.QUOTA)){
                quota = costDistributionItems.get(position).getValue() + " / " + CostDistributionHelper.getCountWithoutFixed(costDistributionItems);
            }else if (costDistributionItems.get(position).getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.PERCENT)){
                BigDecimal percentFromAll = costDistributionItems.get(position).getValue().multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_EVEN);
                String percentOfAllString = percentFromAll.toString();
                quota = percentOfAllString + "%";
            }else if (costDistributionItems.get(position).getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.REST)){
                quota = "Rest";
            }
        }
        holder.costDistributionRowQuotaTv.setText(quota);

        BigDecimal amountDecimal = CostDistributionHelper.calculateAmountForCostDistributionItemPrecise(costDistributionItems.get(position), costDistributionItems, maxFixedAmount);
        holder.costDistributionRowAmountTv.setText(amountDecimal.toString() + "€");

        costDistributionItems.get(position).setMoneyValue(amountDecimal);

        holder.deleteCostDistributionItemButton.setTag(position);
        holder.deleteCostDistributionItemButton.setOnClickListener(deleteButtonListener);

        holder.articleSelectCostDistributionItemButton.setTag(position);
        holder.articleSelectCostDistributionItemButton.setOnClickListener(articleSelectButtonListener);

        UUID appUserId = null;
        if (!costDistributionItems.isEmpty()){
            MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(vi.getContext().getApplicationContext());
            SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE);
            sqlBuilder.isEqual(MainDatabaseHandler.VAR_INVOICE_ID, costDistributionItems.get(0).getInvoiceId().toString());
            List<Invoice> invoices = mainDatabaseHandler.findInvoicesSql(sqlBuilder);

            if (!invoices.isEmpty()){
                appUserId = invoices.get(0).getCreatedById();
            }
        }


        if (costDistributionItems.get(position).getPaymentPersonTypeEnum() != null &&
                (costDistributionItems.get(position).getPaymentPersonTypeEnum().equals(PaymentPersonTypeEnum.CONTACT) || costDistributionItems.get(position).getPaymentPersonTypeEnum().equals(PaymentPersonTypeEnum.USER)) &&
                !costDistributionItems.get(position).getPayerId().equals(appUserId)){
            String uri;
            if ((costDistributionItems.get(position).getCostPaid() == null && costDistributionItems.get(position).getMoneyValue().compareTo(new BigDecimal(0)) != 0) ||
                    (costDistributionItems.get(position).getCostPaid() != null && costDistributionItems.get(position).getCostPaid().compareTo(costDistributionItems.get(position).getMoneyValue()) != 0)){
                uri = "drawable/ic_clear_black_48dp";
            }else{
                uri = "drawable/money";
            }
            int imageResource = vi.getContext().getApplicationContext().getResources().getIdentifier(uri, null, vi.getContext().getApplicationContext().getPackageName());
            Drawable image = vi.getContext().getResources().getDrawable(imageResource);

            holder.costDistributionActivityMenuButton.setImageDrawable(image);
            holder.costDistributionActivityMenuButton.setTag(position);
            holder.costDistributionActivityMenuButton.setOnClickListener(onMenuItemClickListener);

//            holder.costDistributionActivityMenuButton.setVisibility(View.VISIBLE);
        }else{
//            holder.costDistributionActivityMenuButton.setVisibility(View.GONE);
        }

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
        TextView costDistributionRowQuotaTv;
        TextView costDistributionRowPaymentPersonTv;
        ImageButton deleteCostDistributionItemButton;
        ImageButton costDistributionActivityMenuButton;
        ImageButton articleSelectCostDistributionItemButton;
    }

}
