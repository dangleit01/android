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
import android.widget.ProgressBar;
import android.widget.TextView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import de.nextbill.client.R;
import de.nextbill.client.interfaces.CurrencyTextWatcher;
import de.nextbill.client.pojos.BudgetDTO;

public class AdapterActivityBudgetListView extends BaseAdapter {

    LayoutInflater inflater;
    List<BudgetDTO> budgets;
    ViewHolder holder;
    private View.OnClickListener onMenuItemClickListener;

    public AdapterActivityBudgetListView() {
    }

    public AdapterActivityBudgetListView(Activity act, List<BudgetDTO> budgets, View.OnClickListener onMenuItemClickListener) {

        this.budgets = budgets;
        this.onMenuItemClickListener = onMenuItemClickListener;

        inflater = (LayoutInflater) act
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return budgets.size();
    }

    public List<BudgetDTO> getBudgetList(){
        return this.budgets;
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

            vi = inflater.inflate(R.layout.activity_budget_listview_row, null);
            holder = new ViewHolder();

            holder.sumTv = (TextView) vi.findViewById(R.id.sumTv);
            holder.filterTv = (TextView) vi.findViewById(R.id.filterTv);
            holder.budgetNameTv = (TextView) vi.findViewById(R.id.budgetNameTv);
            holder.budgetActivityMenuButton = (ImageView) vi.findViewById(R.id.budgetActivityMenuButton);
            holder.progressBar = (ProgressBar) vi.findViewById(R.id.progressBar);

            vi.setTag(holder);
        } else {
            holder = (ViewHolder) vi.getTag();
        }

        BudgetDTO budgetDTO = budgets.get(position);

        if (budgetDTO.getName() != null && !budgetDTO.getName().equals("")){
            holder.budgetNameTv.setText(budgetDTO.getName());
            holder.budgetNameTv.setVisibility(View.VISIBLE);
        }else{
            holder.budgetNameTv.setVisibility(View.GONE);
        }

        String sum = "-";
        if (budgetDTO.getSum() != null){
            sum = CurrencyTextWatcher.bigDecimalToString(budgetDTO.getSum().setScale(2, RoundingMode.HALF_EVEN));
        }

        String currentSum = "-";
        if (budgetDTO.getCurrentSum() != null){
            currentSum = CurrencyTextWatcher.bigDecimalToString(budgetDTO.getCurrentSum().setScale(2, RoundingMode.HALF_EVEN));
        }
        holder.sumTv.setText(currentSum + " € / " + sum + " €");

        String filterText = "Keine Kriterien definiert";
        if (budgetDTO.getCurrentSum() != null){
            filterText = budgetDTO.getFilterText();
        }
        holder.filterTv.setText(filterText);

        refreshCurrentState(budgetDTO);

        holder.budgetActivityMenuButton.setTag(position);
        holder.budgetActivityMenuButton.setOnClickListener(onMenuItemClickListener);

        return vi;
    }

    public void refreshCurrentState(BudgetDTO budget){

        BigDecimal sum = budget.getSum();
        BigDecimal currentSum = budget.getCurrentSum() != null ? budget.getCurrentSum() : new BigDecimal(0);

        BigDecimal percentOfCurrentSum = null;
        if (sum.compareTo(new BigDecimal(0)) != 0){
            percentOfCurrentSum = currentSum.divide(sum, 2, RoundingMode.HALF_EVEN).multiply(new BigDecimal(100));
        }else{
            percentOfCurrentSum = new BigDecimal(100);
        }

        percentOfCurrentSum = percentOfCurrentSum.compareTo(new BigDecimal(100)) > 0 ? new BigDecimal(100) : percentOfCurrentSum;

        holder.progressBar.setProgress(percentOfCurrentSum.intValue());

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

        TextView sumTv;
        TextView filterTv;
        TextView budgetNameTv;
        ImageView budgetActivityMenuButton;
        ProgressBar progressBar;

    }

}
