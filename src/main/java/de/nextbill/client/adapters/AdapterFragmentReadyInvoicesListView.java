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
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import de.nextbill.client.R;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.enums.InvoiceSource;
import de.nextbill.client.enums.InvoiceStatusEnum;
import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.enums.SQLOperatorEnum;
import de.nextbill.client.helper.CostDistributionHelper;
import de.nextbill.client.helper.LoginUserHelper;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.IPaymentPerson;
import de.nextbill.client.model.Invoice;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

public class AdapterFragmentReadyInvoicesListView extends BaseAdapter implements StickyListHeadersAdapter  {

    LayoutInflater inflater;
    ImageView thumb_image;
    private List<Invoice> invoiceDataCollection;
    private Map<UUID, BigDecimal> invoiceSumMap = new HashMap<>();
    ViewHolder holder;

    Map<Integer, String> monthTitleMap = new HashMap<>();

    SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy");

    NumberFormat numberFormatter;

    public AdapterFragmentReadyInvoicesListView() {
        // TODO Auto-generated constructor stub
    }

    public AdapterFragmentReadyInvoicesListView(Context context) {

        this.invoiceDataCollection = new ArrayList<Invoice>();

        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        numberFormatter = NumberFormat.getNumberInstance(Locale.GERMAN);
        numberFormatter.setMaximumFractionDigits(2);
        numberFormatter.setMinimumFractionDigits(2);
        monthTitleMap.clear();
    }

    public List<Invoice> getInvoiceList(){
        return this.invoiceDataCollection;
    }

    public Map<UUID, BigDecimal> getInvoiceSumMap(){
        return this.invoiceSumMap;
    }

    public void swapInvoiceList(List<Invoice> invoices){
        monthTitleMap.clear();
        invoiceSumMap.clear();
        this.invoiceDataCollection.clear();
        this.invoiceDataCollection.addAll(invoices);
        this.notifyDataSetChanged();
    }

    public void swapInvoiceList(List<Invoice> invoices, Map<UUID, BigDecimal> invoiceSumMap){
        monthTitleMap.clear();
        this.invoiceSumMap.clear();
        this.invoiceSumMap.putAll(invoiceSumMap);
        this.invoiceDataCollection.clear();
        this.invoiceDataCollection.addAll(invoices);
        this.notifyDataSetChanged();
    }

    public void addInvoice(Invoice invoice){
        this.invoiceDataCollection.add(invoice);
        this.notifyDataSetChanged();
    }

    public void addInvoices(List<Invoice> invoices){
        this.invoiceDataCollection.addAll(invoices);
        this.notifyDataSetChanged();
    }

    public void removeInvoice(Invoice invoice){
        this.invoiceDataCollection.remove(invoice);
        this.notifyDataSetChanged();
    }

    public int getCount() {
        return invoiceDataCollection.size();
    }

    public Object getItem(int arg0) {
        return invoiceDataCollection.get(arg0);
    }

    public long getItemId(int position) {
        return invoiceDataCollection.get(position).getInvoiceId().getLeastSignificantBits();
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        View vi = convertView;
        if (convertView == null) {

            vi = inflater.inflate(R.layout.activity_main_listview_row, null);
            holder = new ViewHolder();

            holder.invoiceListTvVendor = (TextView) vi.findViewById(R.id.invoiceListTvBusinessPartner);
            holder.invoiceListTvDate = (TextView) vi.findViewById(R.id.invoiceListTvDate);
            holder.invoiceListTvSum = (TextView) vi.findViewById(R.id.invoiceListTvSum);
            holder.invoiceListTvStatus = (ImageView) vi.findViewById(R.id.invoiceListIvStatus);
            holder.externalNameTv = (TextView) vi.findViewById(R.id.externalNameTv);

            vi.setTag(holder);
        } else {

            holder = (ViewHolder) vi.getTag();
        }

        String vendorName = "-";
        String date = "-";
        String sum = "-";
        if (invoiceDataCollection.get(position).getInvoiceStatusEnum().equals(InvoiceStatusEnum.WAIT_FOR_UPLOAD)) {
            vendorName = "Upload l??uft...";
            holder.invoiceListTvVendor.setText(vendorName);
            date = "";
            holder.invoiceListTvDate.setText(date);
            sum = "";
            holder.invoiceListTvSum.setText(sum);
            holder.externalNameTv.setVisibility(View.GONE);
            String uri = "drawable/upload";
            int imageResource = vi.getContext().getApplicationContext().getResources().getIdentifier(uri, null, vi.getContext().getApplicationContext().getPackageName());
            Drawable image = vi.getContext().getResources().getDrawable(imageResource);
            holder.invoiceListTvStatus.setImageDrawable(image);
        } else if (invoiceDataCollection.get(position).getInvoiceStatusEnum().equals(InvoiceStatusEnum.ANALYZING)) {
            vendorName = "Wird analysiert...";
            holder.invoiceListTvVendor.setText(vendorName);
            date = "";
            holder.invoiceListTvDate.setText(date);
            sum = "";
            holder.invoiceListTvSum.setText(sum);
            holder.externalNameTv.setVisibility(View.GONE);
            String uri = "drawable/analyze";
            int imageResource = vi.getContext().getApplicationContext().getResources().getIdentifier(uri, null, vi.getContext().getApplicationContext().getPackageName());
            Drawable image = vi.getContext().getResources().getDrawable(imageResource);
            holder.invoiceListTvStatus.setImageDrawable(image);
        }else {

            vendorName = "-";
            AppUser currentUser = LoginUserHelper.currentLoggedInUser(inflater.getContext());
            IPaymentPerson paymentPersonDisplayName = null;

            if (currentUser != null){
                if (invoiceDataCollection.get(position).getPayerId() != null && PaymentPersonTypeEnum.USER.equals(invoiceDataCollection.get(position).getPayerTypeEnum()) && !PaymentPersonTypeEnum.CONTACT.equals(invoiceDataCollection.get(position).getPaymentRecipientTypeEnum())){
                    paymentPersonDisplayName = invoiceDataCollection.get(position).getPaymentRecipient(inflater.getContext());
                }else if (invoiceDataCollection.get(position).getPaymentRecipientId() != null & PaymentPersonTypeEnum.USER.equals(invoiceDataCollection.get(position).getPaymentRecipientTypeEnum()) && !PaymentPersonTypeEnum.CONTACT.equals(invoiceDataCollection.get(position).getPayerTypeEnum())){
                    paymentPersonDisplayName = invoiceDataCollection.get(position).getPayer(inflater.getContext());
                }else {
                    if (currentUser.getAppUserId().equals(invoiceDataCollection.get(position).getCreatedById())){
                        if (invoiceDataCollection.get(position).getPayerId() != null & PaymentPersonTypeEnum.USER.equals(invoiceDataCollection.get(position).getPayerTypeEnum()) && PaymentPersonTypeEnum.CONTACT.equals(invoiceDataCollection.get(position).getPaymentRecipientTypeEnum())){
                            paymentPersonDisplayName = invoiceDataCollection.get(position).getPaymentRecipient(inflater.getContext());
                        }else if (invoiceDataCollection.get(position).getPaymentRecipientId() != null & PaymentPersonTypeEnum.USER.equals(invoiceDataCollection.get(position).getPaymentRecipientTypeEnum()) && PaymentPersonTypeEnum.CONTACT.equals(invoiceDataCollection.get(position).getPayerTypeEnum())){
                            paymentPersonDisplayName = invoiceDataCollection.get(position).getPayer(inflater.getContext());
                        }
                    }else{
                        if (invoiceDataCollection.get(position).getPayerId() != null & PaymentPersonTypeEnum.USER.equals(invoiceDataCollection.get(position).getPayerTypeEnum()) && PaymentPersonTypeEnum.CONTACT.equals(invoiceDataCollection.get(position).getPaymentRecipientTypeEnum())){
                            paymentPersonDisplayName = invoiceDataCollection.get(position).getPayer(inflater.getContext());
                        }else if (invoiceDataCollection.get(position).getPaymentRecipientId() != null & PaymentPersonTypeEnum.USER.equals(invoiceDataCollection.get(position).getPaymentRecipientTypeEnum()) && PaymentPersonTypeEnum.CONTACT.equals(invoiceDataCollection.get(position).getPayerTypeEnum())){
                            paymentPersonDisplayName = invoiceDataCollection.get(position).getPaymentRecipient(inflater.getContext());
                        }
                    }

                }

                if (!invoiceDataCollection.get(position).getCreatedById().equals(currentUser.getAppUserId())){

                    MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(vi.getContext());
                    List<AppUser> appUsers = mainDatabaseHandler.findAppUsers(MainDatabaseHandler.VAR_APP_USER_ID, invoiceDataCollection.get(position).getCreatedById().toString(), SQLOperatorEnum.EQUAL);

                    if (!appUsers.isEmpty()){
                        holder.externalNameTv.setVisibility(View.VISIBLE);
                        holder.externalNameTv.setText(appUsers.get(0).getAppUserName());
                    }else{
                        holder.externalNameTv.setVisibility(View.GONE);
                    }

                }else{
                    holder.externalNameTv.setVisibility(View.GONE);
                }


            }else{
                holder.externalNameTv.setVisibility(View.GONE);
            }

            if (paymentPersonDisplayName != null){
                vendorName = cutString(paymentPersonDisplayName.getPaymentPersonName());
            }
            holder.invoiceListTvVendor.setText(vendorName);

            date = "-";
            if (invoiceDataCollection.get(position).getDateOfInvoice() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd. MMMM yyyy");
                date = sdf.format(invoiceDataCollection.get(position).getDateOfInvoice());
            }
            holder.invoiceListTvDate.setText(date);


            sum = "-";
            if (invoiceDataCollection.get(position).getSumOfInvoice() != null) {
                BigDecimal sumDecimal = new BigDecimal(0);

                AppUser loggedInAppUser = LoginUserHelper.currentLoggedInUser(vi.getContext());
                if (loggedInAppUser != null){
                    sumDecimal = sumDecimal.add(calculateOrReturnSumOfInvoice(invoiceDataCollection.get(position), vi.getContext(), loggedInAppUser));
                }
                if (sumDecimal.compareTo(new BigDecimal(0)) == 1){
                    holder.invoiceListTvSum.setTextColor(Color.rgb(51,204,51));
                }else{
                    holder.invoiceListTvSum.setTextColor(Color.BLACK);
                }

                sum = numberFormatter.format(sumDecimal);
            }
            holder.invoiceListTvSum.setText(sum + " ???");

            String uri = "drawable/keyboard";
            if (invoiceDataCollection.get(position).getInvoiceFailureMessage() != null){
                uri = "drawable/lightning";
            }else{
                if (currentUser != null && !invoiceDataCollection.get(position).getCreatedById().equals(currentUser.getAppUserId())){
                    uri = "drawable/user";
                }else if (invoiceDataCollection.get(position).getInvoiceStatusEnum().equals(InvoiceStatusEnum.READY)) {
                    if (invoiceDataCollection.get(position).getInvoiceSource() != null) {
                        if (invoiceDataCollection.get(position).getInvoiceSource().equals(InvoiceSource.CAMERA)){
                            uri = "drawable/camera";
                        }else if (invoiceDataCollection.get(position).getInvoiceSource().equals(InvoiceSource.MAIL)){
                            uri = "drawable/email";
                        }
                    }
                }
            }
            int imageResource = vi.getContext().getApplicationContext().getResources().getIdentifier(uri, null, vi.getContext().getApplicationContext().getPackageName());
            Drawable image = vi.getContext().getResources().getDrawable(imageResource);
            holder.invoiceListTvStatus.setImageDrawable(image);

        }


        return vi;
    }

    private BigDecimal calculateOrReturnSumOfInvoice(Invoice invoice, Context context, AppUser loggedInAppUser){
        if (invoiceSumMap.get(invoice.getInvoiceId()) != null){
            return invoiceSumMap.get(invoice.getInvoiceId());
        }else{
            BigDecimal sum = CostDistributionHelper.invoiceCostForPaymentPerson(context, invoice, loggedInAppUser);
            invoiceSumMap.put(invoice.getInvoiceId(), sum);
            return sum;
        }
    }

    private String cutString(String input) {
        String resultString = input;

        if (input.length() >= 20) {
            resultString = input.substring(0, 20);
            resultString += "...";
        }
        return resultString;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;
        if (convertView == null) {
            holder = new HeaderViewHolder();
            convertView = inflater.inflate(R.layout.activity_main_listview_section, parent, false);
            holder.text = (TextView) convertView.findViewById(R.id.textSeparator);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }
        //set header text as first char in name

        String headerText = "Unbekanntes Datum";
        if (invoiceDataCollection.get(position).getDateOfInvoice() != null){

            if (monthTitleMap.get(position) == null){
                headerText = sdf.format(invoiceDataCollection.get(position).getDateOfInvoice());
                BigDecimal sum = sumOfInvoices(convertView.getContext(), invoiceDataCollection, headerText);
                headerText = headerText + "  -  ("+ numberFormatter.format(sum)+" Euro)";

                monthTitleMap.put(position, headerText);
            }else{
                headerText = monthTitleMap.get(position);
            }
        }else{
            BigDecimal sum = sumOfInvoices(convertView.getContext(), invoiceDataCollection, null);
            headerText = headerText + "  -  ("+numberFormatter.format(sum)+" Euro)";
        }

        holder.text.setText(headerText);
        return convertView;
    }

    public BigDecimal sumOfInvoices(Context context, List<Invoice> invoices, String monthAndYear){
        BigDecimal sum = new BigDecimal(0);
        AppUser loggedInAppUser = LoginUserHelper.currentLoggedInUser(context);
        if (loggedInAppUser == null){
            return sum;
        }

        for (Invoice invoice : invoices) {
            if ((monthAndYear == null && invoice.getDateOfInvoice() == null) || (invoice.getDateOfInvoice() != null && monthAndYear != null && sdf.format(invoice.getDateOfInvoice()).equals(monthAndYear))){
                sum = sum.add(calculateOrReturnSumOfInvoice(invoice, context, loggedInAppUser));
            }
        }
        return sum;
    }

    @Override
    public long getHeaderId(int position) {
        if (invoiceDataCollection.get(position).getDateOfInvoice() == null){
            return Long.valueOf(34546847);
        }
        return Long.valueOf(sdf.format(invoiceDataCollection.get(position).getDateOfInvoice()).hashCode());
    }

    /*
     *
     * */
    static class ViewHolder {

        TextView invoiceListTvVendor;
        TextView invoiceListTvSum;
        TextView invoiceListTvDate;
        TextView externalNameTv;
        ImageView invoiceListTvStatus;
    }

    class HeaderViewHolder {
        TextView text;
    }

}
