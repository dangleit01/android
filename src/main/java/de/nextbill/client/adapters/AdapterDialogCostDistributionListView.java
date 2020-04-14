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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;

import de.nextbill.client.R;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.enums.CostDistributionDialogTypesEnum;
import de.nextbill.client.enums.CostDistributionItemTypeEnum;
import de.nextbill.client.helper.CostDistributionHelper;
import de.nextbill.client.model.CostDistribution;
import de.nextbill.client.model.CostDistributionItem;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

public class AdapterDialogCostDistributionListView extends BaseAdapter implements StickyListHeadersAdapter {

    private List<CostDistribution> costDistributions;
    private LayoutInflater inflater;
    private View.OnClickListener deleteButtonListener;
    private View.OnClickListener onItemClickListener;

    private HashMap<CostDistributionDialogTypesEnum, String> localizationForCostDistributionDialogTypesEnum = new HashMap<CostDistributionDialogTypesEnum, String>();

    public AdapterDialogCostDistributionListView(Context context, List<CostDistribution> costDistributions, View.OnClickListener deleteButtonListener, View.OnClickListener onItemClickListener) {
        inflater = LayoutInflater.from(context);
        this.costDistributions = costDistributions;
        this.deleteButtonListener = deleteButtonListener;
        this.onItemClickListener = onItemClickListener;

        localizationForCostDistributionDialogTypesEnum.clear();
        localizationForCostDistributionDialogTypesEnum.put(CostDistributionDialogTypesEnum.SAVED_COST_DISTRIBUTION, "Gespeicherte Vorlagen");
    }

    @Override
    public int getCount() {
        return costDistributions.size();
    }

    @Override
    public Object getItem(int position) {
        return costDistributions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.dialog_cost_distribution_listview_item, parent, false);
            holder.text = (TextView) convertView.findViewById(R.id.text);
            holder.costDistributionDetailsTv = (TextView) convertView.findViewById(R.id.costDistributionDetailsTv);
            holder.imageView10 = (ImageView) convertView.findViewById(R.id.imageView10);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.text.setText(costDistributions.get(position).getName());

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(convertView.getContext());
        List<CostDistributionItem> costDistributionItems = mainDatabaseHandler.findCostDistributionItems(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ID, costDistributions.get(position).getCostDistributionId().toString());
        String costDistributionText = "";
        for (int i = 0; i < costDistributionItems.size(); i++) {
            String costDistributionItemString = formattedTextForCostDistributionItem(convertView.getContext(), costDistributionItems.get(i), costDistributionItems);

            if (i == 0){
                costDistributionText = costDistributionItemString;
            }else if (i % 3 != 0){
                costDistributionText = costDistributionText +"  -  "+ costDistributionItemString;
            }else if (i % 3 == 0){
                costDistributionText = costDistributionText + "\n" + costDistributionItemString;
            }

        }

        SwipeLayout swipeLayout =  (SwipeLayout) convertView.findViewById(R.id.sample1);
        swipeLayout.setShowMode(SwipeLayout.ShowMode.LayDown);
        swipeLayout.addDrag(SwipeLayout.DragEdge.Left, convertView.findViewById(R.id.bottom_wrapper));
        swipeLayout.setRightSwipeEnabled(false);

        holder.imageView10.setTag(position);
        holder.imageView10.setOnClickListener(deleteButtonListener);

        holder.text.setTag(position);
        holder.text.setOnClickListener(onItemClickListener);
        holder.costDistributionDetailsTv.setTag(position);
        holder.costDistributionDetailsTv.setOnClickListener(onItemClickListener);

        holder.costDistributionDetailsTv.setText(costDistributionText);

        return convertView;
    }

    public String formattedTextForCostDistributionItem(Context context, CostDistributionItem costDistributionItem, List<CostDistributionItem> allCostDistributionItems){

        String resultString = "-";
        if (costDistributionItem.getCostDistributionItemTypeEnum() != null) {
            if (costDistributionItem.getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.FIXED_AMOUNT)){
                resultString = "Fest";
            }else if (costDistributionItem.getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.QUOTA)){
                resultString = costDistributionItem.getValue() + " / " + CostDistributionHelper.getCountWithoutFixed(allCostDistributionItems);
            }else if (costDistributionItem.getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.PERCENT)){
                BigDecimal percentFromAll = costDistributionItem.getValue().multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_EVEN);
                String percentOfAllString = percentFromAll.toString();
                resultString = percentOfAllString + "%";
            }else if (costDistributionItem.getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.REST)){
                resultString = "Rest";
            }
        }

        resultString = costDistributionItem.getPaymentPersonName(context) +":"+ resultString;

        return resultString;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;
        if (convertView == null) {
            holder = new HeaderViewHolder();
            convertView = inflater.inflate(R.layout.dialog_cost_distribution_listview_section, parent, false);
            holder.text = (TextView) convertView.findViewById(R.id.textSeparator);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }
        //set header text as first char in name
        String headerText = localizationForCostDistributionDialogTypesEnum.get(CostDistributionDialogTypesEnum.SAVED_COST_DISTRIBUTION);
        holder.text.setText(headerText);

        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        return localizationForCostDistributionDialogTypesEnum.get(CostDistributionDialogTypesEnum.SAVED_COST_DISTRIBUTION).hashCode();
    }

    class HeaderViewHolder {
        TextView text;
    }

    class ViewHolder {
        TextView text;
        TextView costDistributionDetailsTv;
        ImageView imageView10;
    }

}