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
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.fragment.app.DialogFragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import de.nextbill.client.R;
import de.nextbill.client.enums.RepetitionTypeEnum;
import de.nextbill.client.interfaces.CustomDateTextWatcher;
import de.nextbill.client.interfaces.DateTextWatcher;

public class DialogCreateStandingOrder extends DialogFragment {

    private Date startDate;

    private EditText startDateEt;
    private Spinner repetitionTypeSpinner;
    private Button okButton;
    private Switch isAlwaysPaidSwitch;
    private RelativeLayout customdialog;

    private List<RepetitionTypeEnum> repetitionTypeEnums = new ArrayList();
    private RepetitionTypeEnum repetitionTypeEnum;

    private DateTextWatcher dateTextWatcher;

    public void setArguments(Date startDate){
        this.startDate = startDate;
    }

    public DialogCreateStandingOrder() {
    }

    public interface CreateStandingOrderDialogListener {
        void onFinishCreateStandingOrderDialog(Date startDate, RepetitionTypeEnum repetitionTypeEnum, boolean isAlwasPaid);
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
        View view = inflater.inflate(R.layout.dialog_create_standing_order, container);
        okButton = (Button) view.findViewById(R.id.createStandingOrderButton);
        getDialog().setTitle("Erstelle Dauerauftrag");

        repetitionTypeSpinner = (Spinner) view.findViewById(R.id.invoiceRepetitionTypeSpinner);
        isAlwaysPaidSwitch = (Switch) view.findViewById(R.id.isAlwaysPaidSwitch);
        startDateEt = (EditText) view.findViewById(R.id.startDateEt);

        if (startDate == null){
            startDate = new Date();
        }
        dateTextWatcher = new DateTextWatcher(startDateEt, startDate);

        CustomDateTextWatcher dateTextWatcherCustom = new CustomDateTextWatcher() {

            @Override
            public void currentValue(Date currentValue){
                startDate = currentValue;
            }

            public void afterTextChanged(Editable s) {

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        };
        dateTextWatcher.addCustomTextWatcher(dateTextWatcherCustom);

        customdialog = (RelativeLayout) view.findViewById(R.id.customdialog);

        repetitionTypeEnums.addAll(Arrays.asList(RepetitionTypeEnum.values()));
        repetitionTypeEnums.remove(RepetitionTypeEnum.ONCE);
        repetitionTypeEnums.remove(RepetitionTypeEnum.NONE);
        ArrayAdapter<RepetitionTypeEnum> dataAdapter = new ArrayAdapter<>(this.getContext(), android.R.layout.simple_spinner_item, repetitionTypeEnums);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repetitionTypeSpinner.setAdapter(dataAdapter);

        repetitionTypeSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                repetitionTypeEnum = repetitionTypeEnums.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

                CreateStandingOrderDialogListener activity = (CreateStandingOrderDialogListener) getActivity();
                activity.onFinishCreateStandingOrderDialog(dateTextWatcher.getResult(), repetitionTypeEnum, isAlwaysPaidSwitch.isChecked());
                DialogCreateStandingOrder.this.dismiss();
            }
        });

        customdialog.requestFocus();

        return view;

    }

    private Date convertToDate(String text){
        SimpleDateFormat tmpSdf = new SimpleDateFormat("dd.MM.yyyy");

        try {
            Date dateFormat = tmpSdf.parse(text);
            return dateFormat;
        } catch (ParseException e) {
        }

        return null;
    }

}
