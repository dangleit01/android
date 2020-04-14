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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.nextbill.client.R;

public class DialogDatePicker extends DialogFragment implements TextView.OnEditorActionListener {

    private Date currentDate;
    private DatePicker mDatePicker;
    private Button okButton;

    public void setArguments(Date currentDate){
        this.currentDate = currentDate;
    }

    public DialogDatePicker() {
    }

    public interface EditDateDialogListener {
        void onFinishDateEditDialog(Date dateOfInvoice);
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
        View view = inflater.inflate(R.layout.dialog_date_picker, container);
        mDatePicker = (DatePicker) view.findViewById(R.id.datePicker);
        okButton = (Button) view.findViewById(R.id.datePickerOk);
        getDialog().setTitle("Rechnungsdatum");

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
                try {
                    Date dateOfInvoice = sdf.parse(mDatePicker.getDayOfMonth()+"."+(mDatePicker.getMonth()+1)+"."+mDatePicker.getYear());
                    EditDateDialogListener activity = (EditDateDialogListener) getActivity();
                    activity.onFinishDateEditDialog(dateOfInvoice);
                    DialogDatePicker.this.dismiss();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });

        if (currentDate != null){
            SimpleDateFormat year = new SimpleDateFormat("yyyy");
            Integer yearText = Integer.valueOf(year.format(currentDate));

            SimpleDateFormat day = new SimpleDateFormat("dd");
            Integer dayText = Integer.valueOf(day.format(currentDate));

            SimpleDateFormat month = new SimpleDateFormat("MM");
            Integer monthText = Integer.valueOf(month.format(currentDate)) - 1;

            mDatePicker.updateDate(yearText, monthText, dayText);
        }

        // Show soft keyboard automatically
//        mEditText.requestFocus();
//        getDialog().getWindow().setSoftInputMode(
//                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
//        mEditText.setOnEditorActionListener(this);
        return view;

    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            this.dismiss();
            return true;
        }
        return false;
    }

}
