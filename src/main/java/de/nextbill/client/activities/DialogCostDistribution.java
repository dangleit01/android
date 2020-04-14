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

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import de.nextbill.client.R;
import de.nextbill.client.adapters.AdapterDialogCostDistributionListView;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.model.CostDistribution;
import de.nextbill.client.network.RequestUpdateService;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class DialogCostDistribution extends DialogFragment {

    private StickyListHeadersListView mCostDistributionsLv;
    private Button editCostDistributionDetailsButton;

    private AdapterDialogCostDistributionListView costDistributionsAdaoter;

    private List<CostDistribution> costDistributions = new ArrayList<>();

    public DialogCostDistribution() {
    }

    public void setArguments(List<CostDistribution> costDistributions) {
        this.costDistributions = costDistributions;
    }

    public interface SelectCostDistributionListener {
        void onFinishCostDistributionSelectDialog(CostDistribution costDistribution, Boolean wantToEdit);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_cost_distribution, container);

        mCostDistributionsLv = (StickyListHeadersListView) view.findViewById(R.id.costDistributionsLv);
        editCostDistributionDetailsButton = (Button) view.findViewById(R.id.editCostDistributionDetailsButton);

        View.OnClickListener onClickListener = new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Integer position = (Integer) v.getTag();
                CostDistribution costDistribution = costDistributions.get(position);
                UUID costDistributionId = costDistribution.getCostDistributionId();

                MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(v.getContext());
                StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(v.getContext());

                List<CostDistribution> tmpCostDistributions = mainDatabaseHandler.findCostDistributions(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ID, costDistributionId.toString());
                mainDatabaseHandler.deleteCostDistributions(tmpCostDistributions);
                handler.addObject(costDistributionId.toString(), StatusDatabaseHandler.OBJECT_TYPE_COST_DISTRIBUTION, StatusDatabaseHandler.UPDATE_STATUS_DELETE, new Date().getTime(), 1);

                costDistributions.remove(costDistribution);
                costDistributionsAdaoter.notifyDataSetChanged();

                Intent sIntent = new Intent(v.getContext(), RequestUpdateService.class);
                sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
                v.getContext().startService(sIntent);
            }
        };

        View.OnClickListener onItemClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                Integer position = (Integer) view.getTag();

                SelectCostDistributionListener activity = (SelectCostDistributionListener) getActivity();

                activity.onFinishCostDistributionSelectDialog(costDistributions.get(position), null);
                DialogCostDistribution.this.dismiss();
            }
        };

        costDistributionsAdaoter = new AdapterDialogCostDistributionListView(this.getContext(), costDistributions, onClickListener, onItemClickListener);
        mCostDistributionsLv.setAdapter(costDistributionsAdaoter);

        editCostDistributionDetailsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectCostDistributionListener activity = (SelectCostDistributionListener) getActivity();

                activity.onFinishCostDistributionSelectDialog(null, true);
                DialogCostDistribution.this.dismiss();
            }
        });

        refreshCostDistributionList();

        return view;

    }

    private void refreshCostDistributionList() {
        costDistributionsAdaoter.notifyDataSetChanged();
    }

}
