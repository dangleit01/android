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
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import de.nextbill.client.R;
import de.nextbill.client.enums.MainFunctionEnum;
import de.nextbill.client.enums.Right;
import de.nextbill.client.helper.RightHelper;

public class DialogMainFunctions extends DialogFragment implements TextView.OnEditorActionListener {

    private boolean isStandingOrder = false;

    public DialogMainFunctions() {
    }

    public void setArguments(boolean isStandingOrder) {
        this.isStandingOrder = isStandingOrder;
    }

    public interface SelectMainFunctionDialogListener {
        void onFinishMainFunctionsDialog(MainFunctionEnum mainFunctionEnum, boolean isStandingOrder);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
//        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_main_functions, container);
        getDialog().setTitle("Neue Transaktion");

        LinearLayout autoRecognitionLinearLayout = (LinearLayout) view.findViewById(R.id.autoRecognitionLinearLayout);
        LinearLayout micAutoRecognitionLinearLayout = (LinearLayout) view.findViewById(R.id.micAutoRecognitionLinearLayout);
        TextView micTv = (TextView) view.findViewById(R.id.micTv);
        TextView takePictureTv = (TextView) view.findViewById(R.id.takePictureTv);

        if (isStandingOrder){
            autoRecognitionLinearLayout.setVisibility(View.GONE);
            micAutoRecognitionLinearLayout.setVisibility(View.GONE);
        }else{
            autoRecognitionLinearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SelectMainFunctionDialogListener activity = (SelectMainFunctionDialogListener) getActivity();

                    activity.onFinishMainFunctionsDialog(MainFunctionEnum.AUTO_RECOGNITION, isStandingOrder);
                    DialogMainFunctions.this.dismiss();
                }
            });

            micAutoRecognitionLinearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SelectMainFunctionDialogListener activity = (SelectMainFunctionDialogListener) getActivity();

                    activity.onFinishMainFunctionsDialog(MainFunctionEnum.MIC_AUTO_RECOGNITION, isStandingOrder);
                    DialogMainFunctions.this.dismiss();
                }
            });
        }

        if (RightHelper.hasUserRight(getContext(), Right.OCR)) {
            micTv.setText("Automatische Spracherkennung");
            takePictureTv.setText("Automatische Bilderkennung");
        }else{
            micTv.setText("Spracheingabe");
            takePictureTv.setText("Quittung aufnehmen");
        }

        LinearLayout expenseLinearLayout = (LinearLayout) view.findViewById(R.id.expenseLinearLayout);

        expenseLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SelectMainFunctionDialogListener activity = (SelectMainFunctionDialogListener) getActivity();

                activity.onFinishMainFunctionsDialog(MainFunctionEnum.NEW_EXPENSE, isStandingOrder);
                DialogMainFunctions.this.dismiss();
            }
        });

        LinearLayout incomeLinearLayout = (LinearLayout) view.findViewById(R.id.incomeLinearLayout);

        incomeLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SelectMainFunctionDialogListener activity = (SelectMainFunctionDialogListener) getActivity();

                activity.onFinishMainFunctionsDialog(MainFunctionEnum.NEW_INCOME, isStandingOrder);
                DialogMainFunctions.this.dismiss();
            }
        });



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
