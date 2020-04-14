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
import de.nextbill.client.enums.MessageType;
import de.nextbill.client.model.BasicData;
import de.nextbill.client.pojos.MessageDTO;

public class AdapterFragmentConflictMessagesListView extends BaseAdapter{

    LayoutInflater inflater;
    ImageView thumb_image;
    List<BasicData> messagesCollection;
    ViewHolder holder;
    View.OnClickListener onFirstActionClickListener;
    View.OnClickListener onSecondActionListener;

    public AdapterFragmentConflictMessagesListView() {
        // TODO Auto-generated constructor stub
    }

    public AdapterFragmentConflictMessagesListView(Context context, View.OnClickListener onFirstActionListener, View.OnClickListener onSecondActionListener) {
        this.onFirstActionClickListener = onFirstActionListener;
        this.onSecondActionListener = onSecondActionListener;
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
            Type listType = new TypeToken<MessageDTO>() {
            }.getType();
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            MessageDTO messageDTO = gson.fromJson(messagesCollection.get(position).getValue(), listType);

            message = messageDTO.getMessage();
            subject = messageDTO.getSubject();
            MessageType messageType = messageDTO.getMessageType();

            if (messageType.equals(MessageType.MISTAKE)){
                holder.messagesInteractionButton1.setVisibility(View.VISIBLE);
                holder.messagesInteractionButton1.setText("Problem gelöst");
                holder.messagesInteractionButton1.setOnClickListener(onFirstActionClickListener);
                holder.messagesInteractionButton1.setTag(position);

                holder.messagesInteractionButton2.setVisibility(View.VISIBLE);
                holder.messagesInteractionButton2.setText("Rechnung anzeigen");
                holder.messagesInteractionButton2.setOnClickListener(onSecondActionListener);
                holder.messagesInteractionButton2.setTag(position);
            }else{
                holder.messagesInteractionButton1.setVisibility(View.GONE);
                holder.messagesInteractionButton2.setVisibility(View.GONE);
            }
        }
        holder.messageMessageTv.setText(message);
        holder.messageSubjectTv.setText(subject);

        String uri = "drawable/baseline_question_answer_black_36";
        int imageResource = vi.getContext().getApplicationContext().getResources().getIdentifier(uri, null, vi.getContext().getApplicationContext().getPackageName());
        Drawable image = vi.getContext().getResources().getDrawable(imageResource);
        holder.messageListIvStatus.setImageDrawable(image);

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

    /*
     *
     * */
    static class ViewHolder {

        TextView messageSubjectTv;
        TextView messageMessageTv;
        TextView messageDateTv;
        ImageView messageListIvStatus;
        Button messagesInteractionButton1;
        Button messagesInteractionButton2;
    }

}
