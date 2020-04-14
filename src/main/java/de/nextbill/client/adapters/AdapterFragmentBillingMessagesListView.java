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
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.nextbill.client.R;
import de.nextbill.client.enums.BasicDataSubType;
import de.nextbill.client.enums.BasicDataType;
import de.nextbill.client.enums.MessageType;
import de.nextbill.client.model.BasicData;
import de.nextbill.client.pojos.MessageDTO;

public class AdapterFragmentBillingMessagesListView extends BaseAdapter{

    LayoutInflater inflater;
    ImageView thumb_image;
    List<BasicData> messagesCollection;
    ViewHolder holder;
    View.OnClickListener onFirstActionClickListener;
    View.OnClickListener onAbortActionClickListener;
    View.OnClickListener onDeleteActionClickListener;

    public AdapterFragmentBillingMessagesListView() {
        // TODO Auto-generated constructor stub
    }

    public AdapterFragmentBillingMessagesListView(Context context, View.OnClickListener onFirstActionListener, View.OnClickListener onAbortActionClickListener, View.OnClickListener onDeleteActionClickListener) {
        this.onFirstActionClickListener = onFirstActionListener;
        this.onAbortActionClickListener = onAbortActionClickListener;
        this.onDeleteActionClickListener = onDeleteActionClickListener;
        this.messagesCollection = new ArrayList<>();
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void swapMessagesList(List<BasicData> basicDatas){
        this.messagesCollection.clear();
        this.messagesCollection.addAll(basicDatas);
    }

    public int getCount() {
        return messagesCollection.size();
    }

    public BasicData getItem(int arg0) {
        return messagesCollection.get(arg0);
    }

    public long getItemId(int position) {
        return messagesCollection.get(position).getBasicDataId().getLeastSignificantBits();
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        View vi = convertView;
        if (convertView == null) {

            vi = inflater.inflate(R.layout.activity_messaging_listview_row, null);
            holder = new ViewHolder();

            holder.messageSubjectTv = (TextView) vi.findViewById(R.id.messageSubjectTv);
            holder.messageDateTv = (TextView) vi.findViewById(R.id.messageDateTv);
            holder.messageMessageTv = (TextView) vi.findViewById(R.id.messageMessageTv);
            holder.messageListIvStatus = (ImageView) vi.findViewById(R.id.messageListIvStatus);
            holder.messagesInteractionButton1 = (Button) vi.findViewById(R.id.messagesInteractionButton1);
            holder.messagesInteractionButton2 = (Button) vi.findViewById(R.id.messagesInteractionButton2);
            holder.messagesAbortButton = (Button) vi.findViewById(R.id.messagesAbortButton);

            vi.setTag(holder);
        } else {

            holder = (ViewHolder) vi.getTag();
        }

        String subject = "-";
        String message = "-";
        String date = "-";

        if (messagesCollection.get(position).getNumberValue() != null) {
            Date dateOfMessage = new Date();
            dateOfMessage.setTime(messagesCollection.get(position).getNumberValue().longValue());

            SimpleDateFormat sdf = new SimpleDateFormat("dd. MMMM yyyy");
            date = sdf.format(dateOfMessage);
        }
        holder.messageDateTv.setText(date);

        if (messagesCollection.get(position).getValue() != null) {

            BasicData basicData = messagesCollection.get(position);

            Type listType = new TypeToken<MessageDTO>() {
            }.getType();
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            MessageDTO messageDTO = gson.fromJson(messagesCollection.get(position).getValue(), listType);

            message = messageDTO.getMessage();
            subject = messageDTO.getSubject();
            MessageType messageType = messageDTO.getMessageType();

            if (messageType.equals(MessageType.TO_PAY) && (basicData.getBasicDataSubType() != null && BasicDataSubType.TO_PAY.equals(basicData.getBasicDataSubType()))){
                holder.messagesInteractionButton1.setVisibility(View.VISIBLE);
                holder.messagesInteractionButton1.setText("Zahlung ausgeführt");
                holder.messagesInteractionButton1.setOnClickListener(onFirstActionClickListener);
                holder.messagesInteractionButton1.setTag(position);

                holder.messagesAbortButton.setVisibility(View.VISIBLE);
                holder.messagesAbortButton.setText("Stornieren");
                holder.messagesAbortButton.setOnClickListener(onAbortActionClickListener);
                holder.messagesAbortButton.setTag(position);
            } else if (messageType.equals(MessageType.WAIT_FOR_PAYMENT) && (basicData.getBasicDataSubType() != null && BasicDataSubType.WAIT_FOR_PAYMENT.equals(basicData.getBasicDataSubType()))){
                holder.messagesAbortButton.setVisibility(View.VISIBLE);
                holder.messagesAbortButton.setText("Stornieren");
                holder.messagesAbortButton.setOnClickListener(onAbortActionClickListener);
                holder.messagesAbortButton.setTag(position);

                holder.messagesInteractionButton1.setVisibility(View.VISIBLE);
                holder.messagesInteractionButton1.setText("Zahlung erhalten");
                holder.messagesInteractionButton1.setOnClickListener(onFirstActionClickListener);
                holder.messagesInteractionButton1.setTag(position);
            } else if (messageType.equals(MessageType.PAID)){
                holder.messagesInteractionButton1.setVisibility(View.VISIBLE);
                holder.messagesInteractionButton1.setText("Zahlung erhalten");
                holder.messagesInteractionButton1.setOnClickListener(onFirstActionClickListener);
                holder.messagesInteractionButton1.setTag(position);
            }else if (messageType.equals(MessageType.PAYMENT_CONFIRMED)){
                holder.messagesInteractionButton1.setVisibility(View.VISIBLE);
                holder.messagesInteractionButton1.setText("Abrechnung abschließen");
                holder.messagesInteractionButton1.setOnClickListener(onFirstActionClickListener);
                holder.messagesInteractionButton1.setTag(position);
            }else if (messageType.equals(MessageType.BILLING)){
                holder.messagesInteractionButton1.setVisibility(View.VISIBLE);
                holder.messagesInteractionButton1.setText("Alles überprüft");
                holder.messagesInteractionButton1.setOnClickListener(onDeleteActionClickListener);
                holder.messagesInteractionButton1.setTag(position);
            }else{
                holder.messagesInteractionButton1.setVisibility(View.GONE);
                holder.messagesAbortButton.setVisibility(View.GONE);
            }

            String uri = null;
            if (basicData.getBasicDataType().equals(BasicDataType.BUDGET_MESSAGE)){
                uri = "drawable/bell";
            }else{
                uri = "drawable/distribution_message";
            }
            int imageResource = vi.getContext().getApplicationContext().getResources().getIdentifier(uri, null, vi.getContext().getApplicationContext().getPackageName());
            Drawable image = vi.getContext().getResources().getDrawable(imageResource);
            holder.messageListIvStatus.setImageDrawable(image);

        }
        holder.messageMessageTv.setText(message);
        holder.messageSubjectTv.setText(subject);



        return vi;
    }

    static class ViewHolder {

        TextView messageSubjectTv;
        TextView messageMessageTv;
        TextView messageDateTv;
        ImageView messageListIvStatus;
        Button messagesInteractionButton1;
        Button messagesInteractionButton2;
        Button messagesAbortButton;
    }

}
