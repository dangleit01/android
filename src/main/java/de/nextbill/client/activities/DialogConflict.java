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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.fragment.app.DialogFragment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;

import de.nextbill.client.R;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.model.Invoice;
import de.nextbill.client.network.RequestUpdateService;
import de.nextbill.client.utils.MistakeMessage;

public class DialogConflict extends DialogFragment {

    private EditText nameEt;
    private Button okBtn;
    private Invoice invoice;

    public DialogConflict() {
    }

    public void setArguments(Invoice invoice){
        this.invoice = invoice;
    }

    public interface InputConflictDialogListener {
        void onFinishInputConflictDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_conflict, container);

        nameEt = (EditText) view.findViewById(R.id.nameEt);
        okBtn = (Button) view.findViewById(R.id.okBtn);

        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MistakeMessage mistakeMessage = new MistakeMessage();

                mistakeMessage.setInvoiceId(invoice.getInvoiceId());
                mistakeMessage.setMessage(nameEt.getText().toString());

                GsonBuilder builder = new GsonBuilder();
                Gson gson = builder.create();

                String jsonString = gson.toJson(mistakeMessage);

                StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getContext());
                handler.addObject(jsonString, StatusDatabaseHandler.OBJECT_TYPE_PAYMENT_MISTAKE, StatusDatabaseHandler.UPDATE_STATUS_ADD, new Date().getTime(), 1);

                Intent sIntent = new Intent(getContext(), RequestUpdateService.class);
                sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
                getActivity().startService(sIntent);

                InputConflictDialogListener activity = (InputConflictDialogListener) getActivity();

                activity.onFinishInputConflictDialog();
                DialogConflict.this.dismiss();
            }
        });

        nameEt.requestFocus();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return view;

    }
}
