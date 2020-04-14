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

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import java.math.BigDecimal;

import de.nextbill.client.R;
import de.nextbill.client.interfaces.CurrencyTextWatcher;
import de.nextbill.client.interfaces.CustomCurrencyTextWatcher;

public class DialogSumInput extends DialogFragment implements TextView.OnEditorActionListener {

    private BigDecimal currentSum;
    private BigDecimal sumToPay;
    private EditText invoiceSumEt;
    private Button okButton;
    private Integer itemIdentifier;
    private TextView sumInputTitleTv;
    private CurrencyTextWatcher currencyTextWatcher;

    public void setArguments(BigDecimal currentSum, BigDecimal sumToPay, Integer itemIdentifier){
        this.currentSum = currentSum;
        this.sumToPay = sumToPay;
        this.itemIdentifier = itemIdentifier;
    }

    public DialogSumInput() {
    }

    public interface EditSumDialogListener {
        void onFinishSumEditDialog(BigDecimal currentSum, Integer itemIdentifier);
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
        View view = inflater.inflate(R.layout.dialog_sum_input, container);
        sumInputTitleTv = (TextView) view.findViewById(R.id.sumInputTitleTv);
        invoiceSumEt = (EditText) view.findViewById(R.id.invoiceSumEt);
        okButton = (Button) view.findViewById(R.id.sumInputOk);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                InputMethodManager inputMethodManager =
                        (InputMethodManager) getActivity().getSystemService(
                                Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(
                        invoiceSumEt.getWindowToken(), 0);

                EditSumDialogListener activity = (EditSumDialogListener) getActivity();
                activity.onFinishSumEditDialog(currencyTextWatcher.getResult(), itemIdentifier);
                DialogSumInput.this.dismiss();
            }
        });

        invoiceSumEt.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        invoiceSumEt.setOnEditorActionListener(this);

        BigDecimal tmpCurrentSum = currentSum;
        if (tmpCurrentSum == null || tmpCurrentSum.compareTo(new BigDecimal(0)) == 0){
            tmpCurrentSum = sumToPay;
        }

        currencyTextWatcher = new CurrencyTextWatcher(invoiceSumEt, tmpCurrentSum);

        CustomCurrencyTextWatcher sumTextWatcher = new CustomCurrencyTextWatcher() {

            @Override
            public Boolean validate(BigDecimal newSum) {
                if (newSum.compareTo(sumToPay) == 1){
                    return false;
                }

                return true;
            }

            public void afterTextChanged(Editable s) {
                invoiceSumEt.removeTextChangedListener(this);

                currentSum = currencyTextWatcher.getResult();

                invoiceSumEt.addTextChangedListener(this);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        };
        currencyTextWatcher.addCustomTextWatcher(sumTextWatcher);

        sumInputTitleTv.setText("Bisher bezahlt (Kosten: " + CurrencyTextWatcher.bigDecimalToString(sumToPay)+" €)");

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
