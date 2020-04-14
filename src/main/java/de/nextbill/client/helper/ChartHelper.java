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

package de.nextbill.client.helper;

import android.content.Context;

import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.nextbill.client.comparators.FloatComparator;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.enums.BasicStatusEnum;
import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.IPaymentPerson;
import de.nextbill.client.model.InvoiceCategory;
import de.nextbill.client.model.PaymentPerson;
import de.nextbill.client.model.UserContact;
import de.nextbill.client.pojos.DiagrammDataChart;
import de.nextbill.client.pojos.DiagrammDataChartCoordinate;
import de.nextbill.client.utils.InvoiceCostDistributionItem;
import de.nextbill.client.utils.SqlBuilder;


public class ChartHelper {

    public static DiagrammDataChart chartForTimeRange(Context context, List<InvoiceCostDistributionItem> invoiceCostDistributionItems, boolean useAbsolutValues, Integer maxPastMonth){
        DiagrammDataChart diagrammDataChart = new DiagrammDataChart();
        diagrammDataChart.setDisplayName("Bilanz im Monatsvergleich");
        diagrammDataChart.setxAxeDisplayName("Monat");
        diagrammDataChart.setyAxeDisplayName("Bilanz");

        List<BigDecimal> valuesforMonth = balanceForEachMonth(context, invoiceCostDistributionItems, maxPastMonth);

        Map<BigDecimal, String> xValues = new HashMap<>();

        Map<Integer, String> numberMonthMap = new HashMap<>();
        numberMonthMap.put(1, "Jan");
        numberMonthMap.put(2, "Feb");
        numberMonthMap.put(3, "Mär");
        numberMonthMap.put(4, "Apr");
        numberMonthMap.put(5, "Mai");
        numberMonthMap.put(6, "Jun");
        numberMonthMap.put(7, "Jul");
        numberMonthMap.put(8, "Aug");
        numberMonthMap.put(9, "Sep");
        numberMonthMap.put(10, "Okt");
        numberMonthMap.put(11, "Nov");
        numberMonthMap.put(12, "Dez");

        Integer counter = valuesforMonth.size()-1;
        List<DiagrammDataChartCoordinate> diagrammDataChartCoordinateList = new ArrayList<>();
        for (BigDecimal monthValue : valuesforMonth) {

            DiagrammDataChartCoordinate diagrammDataChartCoordinate = new DiagrammDataChartCoordinate();
            diagrammDataChartCoordinate.setxValueObject(counter);
            if (useAbsolutValues){
                diagrammDataChartCoordinate.setyValue(monthValue.abs());
            }else{
                diagrammDataChartCoordinate.setyValue(monthValue);
            }

            diagrammDataChartCoordinateList.add(diagrammDataChartCoordinate);
            counter--;
        }

//        Collections.reverse(diagrammDataChartCoordinateBeanList);

        for (int i = 0; i < diagrammDataChartCoordinateList.size(); i++) {

            Integer month = (Integer) diagrammDataChartCoordinateList.get(i).getxValueObject();

            Calendar monthBefore = Calendar.getInstance();
            monthBefore.add(Calendar.MONTH, (month * -1));

            Integer monthNumberBefore = monthBefore.get(Calendar.MONTH)+1;

            xValues.put(new BigDecimal(i), numberMonthMap.get(monthNumberBefore));
            diagrammDataChartCoordinateList.get(i).setxValue(new BigDecimal(i));
        }

        diagrammDataChart.setCoordinates(diagrammDataChartCoordinateList);
        diagrammDataChart.setxAxesValues(xValues);
        diagrammDataChart.setSortByYValues(false);

        return diagrammDataChart;
    }

    public static DiagrammDataChart chartForCategories(Context context, List<InvoiceCostDistributionItem> invoiceCostDistributionItems, boolean useAbsolutValues){
        DiagrammDataChart diagrammDataChart = new DiagrammDataChart();
        diagrammDataChart.setDisplayName("Bilanzvergleich von Kategorien");
        diagrammDataChart.setxAxeDisplayName("Kategorie");
        diagrammDataChart.setyAxeDisplayName("Bilanz");

        Map<InvoiceCategory, BigDecimal> valuesforMonth = valuesForEachCategory(context, invoiceCostDistributionItems);

        Map<BigDecimal, String> xValues = new HashMap<>();

        List<DiagrammDataChartCoordinate> diagrammDataChartCoordinateList = new ArrayList<>();
        for (InvoiceCategory invoiceCategory : valuesforMonth.keySet()) {

            DiagrammDataChartCoordinate diagrammDataChartCoordinate = new DiagrammDataChartCoordinate();
            diagrammDataChartCoordinate.setxValueObject(invoiceCategory);
            if (useAbsolutValues){
                diagrammDataChartCoordinate.setyValue(valuesforMonth.get(invoiceCategory).abs());
            }else{
                diagrammDataChartCoordinate.setyValue(valuesforMonth.get(invoiceCategory));
            }

            diagrammDataChartCoordinateList.add(diagrammDataChartCoordinate);
        }

        Collections.sort(diagrammDataChartCoordinateList, new FloatComparator());

        for (int i = 0; i < diagrammDataChartCoordinateList.size(); i++) {
            InvoiceCategory paymentPersonBean = (InvoiceCategory) diagrammDataChartCoordinateList.get(i).getxValueObject();
            xValues.put(new BigDecimal(i), paymentPersonBean.getInvoiceCategoryName());
            diagrammDataChartCoordinateList.get(i).setxValue(new BigDecimal(i));
        }

        diagrammDataChart.setCoordinates(diagrammDataChartCoordinateList);
        diagrammDataChart.setxAxesValues(xValues);

        return diagrammDataChart;
    }

    public static DiagrammDataChart chartForDaysInMonth(Context context, List<InvoiceCostDistributionItem> invoiceCostDistributionItems, boolean useAbsolutValues){
        DiagrammDataChart diagrammDataChart = new DiagrammDataChart();
        diagrammDataChart.setDisplayName("Bilanz im Monatsverlauf");
        diagrammDataChart.setxAxeDisplayName("Tag");
        diagrammDataChart.setyAxeDisplayName("Bilanz");

        List<BigDecimal> valuesforEachDay = balanceForEachDay(context, invoiceCostDistributionItems);

        Map<BigDecimal, String> xValues = new HashMap<>();

        List<DiagrammDataChartCoordinate> diagrammDataChartCoordinateList = new ArrayList<>();
        for (int i = 0; i < 31; i++) {

            BigDecimal valueForDay = valuesforEachDay.get(i);

            DiagrammDataChartCoordinate diagrammDataChartCoordinate = new DiagrammDataChartCoordinate();
            diagrammDataChartCoordinate.setxValueObject("Tag " + i);
            if (useAbsolutValues){
                diagrammDataChartCoordinate.setyValue(valueForDay.abs());
            }else{
                diagrammDataChartCoordinate.setyValue(valueForDay);
            }

            diagrammDataChartCoordinateList.add(diagrammDataChartCoordinate);
        }

        for (int i = 0; i < diagrammDataChartCoordinateList.size(); i++) {
            String dayName = (String) diagrammDataChartCoordinateList.get(i).getxValueObject();
            xValues.put(new BigDecimal(i), dayName);
            diagrammDataChartCoordinateList.get(i).setxValue(new BigDecimal(i));
        }

        diagrammDataChart.setCoordinates(diagrammDataChartCoordinateList);
        diagrammDataChart.setxAxesValues(xValues);

        return diagrammDataChart;
    }

    public static DiagrammDataChart chartForCostPayer(Context context, List<InvoiceCostDistributionItem> invoiceCostDistributionItems, boolean useAbsolutValues){
        DiagrammDataChart diagrammDataChart = new DiagrammDataChart();
        diagrammDataChart.setDisplayName("Bilanz je Träger");
        diagrammDataChart.setxAxeDisplayName("Träger");
        diagrammDataChart.setyAxeDisplayName("Bilanz");

        Map<PaymentPerson, BigDecimal> valuesForPersons = costsByCostsPayer(context, invoiceCostDistributionItems);

        Map<BigDecimal, String> xValues = new HashMap<>();

        List<DiagrammDataChartCoordinate> diagrammDataChartCoordinateList = new ArrayList<>();
        for (PaymentPerson paymentPerson : valuesForPersons.keySet()) {

            DiagrammDataChartCoordinate diagrammDataChartCoordinate = new DiagrammDataChartCoordinate();
            diagrammDataChartCoordinate.setxValueObject(paymentPerson);
            if (useAbsolutValues){
                diagrammDataChartCoordinate.setyValue(valuesForPersons.get(paymentPerson).abs());
            }else{
                diagrammDataChartCoordinate.setyValue(valuesForPersons.get(paymentPerson));
            }

            diagrammDataChartCoordinateList.add(diagrammDataChartCoordinate);
        }

        Collections.sort(diagrammDataChartCoordinateList, new FloatComparator());

        for (int i = 0; i < diagrammDataChartCoordinateList.size(); i++) {
            PaymentPerson paymentPerson = (PaymentPerson) diagrammDataChartCoordinateList.get(i).getxValueObject();
            xValues.put(new BigDecimal(i), paymentPerson.getPaymentPersonName());
            diagrammDataChartCoordinateList.get(i).setxValue(new BigDecimal(i));
        }

        diagrammDataChart.setCoordinates(diagrammDataChartCoordinateList);
        diagrammDataChart.setxAxesValues(xValues);

        return diagrammDataChart;
    }

    public static DiagrammDataChart chartForPaymentRecipients(Context context, List<InvoiceCostDistributionItem> invoiceCostDistributionItems, boolean useAbsolutValues){
        DiagrammDataChart diagrammDataChart = new DiagrammDataChart();
        diagrammDataChart.setDisplayName("Bilanzvergleich je Zahlungsempfänger");
        diagrammDataChart.setxAxeDisplayName("Zahlungsempfänger");
        diagrammDataChart.setyAxeDisplayName("Bilanz");

        Map<PaymentPerson, BigDecimal> valuesForPersons = costsByPaymentRecipients(context, invoiceCostDistributionItems);

        Map<BigDecimal, String> xValues = new HashMap<>();

        List<DiagrammDataChartCoordinate> diagrammDataChartCoordinateList = new ArrayList<>();
        for (PaymentPerson paymentPerson : valuesForPersons.keySet()) {

            DiagrammDataChartCoordinate diagrammDataChartCoordinate = new DiagrammDataChartCoordinate();
            diagrammDataChartCoordinate.setxValueObject(paymentPerson);
            if (useAbsolutValues){
                diagrammDataChartCoordinate.setyValue(valuesForPersons.get(paymentPerson).abs());
            }else{
                diagrammDataChartCoordinate.setyValue(valuesForPersons.get(paymentPerson));
            }

            diagrammDataChartCoordinateList.add(diagrammDataChartCoordinate);
        }

        Collections.sort(diagrammDataChartCoordinateList, new FloatComparator());

        for (int i = 0; i < diagrammDataChartCoordinateList.size(); i++) {
            PaymentPerson paymentPerson = (PaymentPerson) diagrammDataChartCoordinateList.get(i).getxValueObject();
            xValues.put(new BigDecimal(i), paymentPerson.getPaymentPersonName());
            diagrammDataChartCoordinateList.get(i).setxValue(new BigDecimal(i));
        }

        diagrammDataChart.setCoordinates(diagrammDataChartCoordinateList);
        diagrammDataChart.setxAxesValues(xValues);

        return diagrammDataChart;
    }

    public static BarData convertDiagrammDataChartBeanToBarChart(DiagrammDataChart diagrammDataChart){

        ArrayList<BarEntry> entries = new ArrayList<>();

        List<DiagrammDataChartCoordinate> diagrammDataChartCoordinateList = new ArrayList<>();
        diagrammDataChartCoordinateList.addAll(diagrammDataChart.getCoordinates());

        if (diagrammDataChart.isSortByYValues()){
            Collections.sort(diagrammDataChartCoordinateList, new FloatComparator());
        }

        for (int i = 0; i < diagrammDataChartCoordinateList.size(); i++) {
            DiagrammDataChartCoordinate diagrammDataChartCoordinate = diagrammDataChartCoordinateList.get(i);

            BigDecimal xValueTmp = diagrammDataChartCoordinate.getxValue();
            xValueTmp = xValueTmp.remainder(new BigDecimal(999999));
            BarEntry barEntry = new BarEntry(xValueTmp.floatValue(), diagrammDataChartCoordinate.getyValue().floatValue());
            entries.add(barEntry);
        }

        BarDataSet dataSet = new BarDataSet(entries, diagrammDataChart.getDisplayName());
        BarData barData = new BarData(dataSet);

        return barData;
    }

    public static PieData convertDiagrammDataChartBeanToPieChart(DiagrammDataChart diagrammDataChart){

        ArrayList<PieEntry> entries = new ArrayList<>();

        for (int i = 0; i < diagrammDataChart.getCoordinates().size(); i++) {
            DiagrammDataChartCoordinate diagrammDataChartCoordinate = diagrammDataChart.getCoordinates().get(i);

            PieEntry barEntry = new PieEntry(diagrammDataChartCoordinate.getyValue().floatValue(), diagrammDataChart.getxAxesValues().get(new BigDecimal(i)));
            entries.add(barEntry);
        }

        PieDataSet dataSet = new PieDataSet(entries, diagrammDataChart.getDisplayName());
        PieData barData = new PieData(dataSet);

        ArrayList<Integer> colors = new ArrayList<>();

        for (int c : ColorTemplate.VORDIPLOM_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.JOYFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.COLORFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.LIBERTY_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.PASTEL_COLORS)
            colors.add(c);

        colors.add(ColorTemplate.getHoloBlue());

        dataSet.setColors(colors);

        return barData;
    }

    private static List<BigDecimal> balanceForEachMonth(Context context, List<InvoiceCostDistributionItem> invoiceCostDistributionItems, Integer maxPastMonth){

        List<BigDecimal> monthValue = new ArrayList<>();

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(context);

        for (int i = maxPastMonth; i >= 0; i--){
            BigDecimal valueSum = new BigDecimal(0);

            Calendar month = Calendar.getInstance();
            month.add(Calendar.MONTH, -1 * i);
            Integer firstDayOfMonth = month.getActualMinimum(Calendar.DAY_OF_MONTH);
            Integer lastDayOfMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);
            Calendar firstDay = (Calendar) month.clone();
            Calendar lastDay = (Calendar) month.clone();
            firstDay.set(Calendar.DAY_OF_MONTH, firstDayOfMonth);
            firstDay.set(Calendar.HOUR_OF_DAY, 0);
            firstDay.set(Calendar.MINUTE, 0);
            lastDay.set(Calendar.DAY_OF_MONTH, lastDayOfMonth);
            lastDay.set(Calendar.HOUR_OF_DAY, 23);
            lastDay.set(Calendar.MINUTE, 59);
            Date firstDayDate = firstDay.getTime();
            Date lastDayDate = lastDay.getTime();

            for (InvoiceCostDistributionItem invoiceCostDistributionItem : invoiceCostDistributionItems) {
                Date dateOfInvoice = invoiceCostDistributionItem.getInvoice().getDateOfInvoice();

                if (dateOfInvoice == null){
                    continue;
                }

                Calendar invoiceDate =  Calendar.getInstance();
                invoiceDate.setTime(dateOfInvoice);
                invoiceDate.set(Calendar.HOUR_OF_DAY, 6);
                invoiceDate.set(Calendar.MINUTE, 0);
                Date invoiceDateDate = invoiceDate.getTime();

                if (invoiceDateDate.getTime() > firstDayDate.getTime() && invoiceDateDate.getTime() < lastDayDate.getTime()){
                    BigDecimal sum = CostDistributionHelper.invoiceCostForPaymentPerson(context, invoiceCostDistributionItem.getInvoice(), currentUser);
                    valueSum = valueSum.add(sum);
                }
            }

            monthValue.add(valueSum);
        }

        return monthValue;
    }


    private static List<BigDecimal> balanceForEachDay(Context context, List<InvoiceCostDistributionItem> invoiceCostDistributionItems){

        List<BigDecimal> dayValue = new ArrayList<>();

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(context);

        for (int i = 1; i <= 31; i++){
            BigDecimal valueSum = new BigDecimal(0);
            for (InvoiceCostDistributionItem invoiceCostDistributionItem : invoiceCostDistributionItems) {
                Date dateOfInvoice = invoiceCostDistributionItem.getInvoice().getDateOfInvoice();

                if (dateOfInvoice == null){
                    continue;
                }

                Calendar dayDate = Calendar.getInstance();
                dayDate.setTime(dateOfInvoice);

                Integer dayOfMonth = dayDate.get(Calendar.DAY_OF_MONTH);

                if (dayOfMonth == i){
                    BigDecimal sum = CostDistributionHelper.invoiceCostForPaymentPerson(context, invoiceCostDistributionItem.getInvoice(), currentUser);
                    valueSum = valueSum.add(sum);
                }
            }

            dayValue.add(valueSum);
        }

        return dayValue;
    }

    private static Map<InvoiceCategory, BigDecimal> valuesForEachCategory(Context context, List<InvoiceCostDistributionItem> invoiceCostDistributionItems){

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

        Map<InvoiceCategory, BigDecimal> categoryValue = new HashMap<>();

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(context);

        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE_CATEGORY);
        sqlBuilder.startBracket()
                .isEqual(MainDatabaseHandler.VAR_APP_USER_ID,  currentUser.getAppUserId().toString())
                .or()
                .isNull(MainDatabaseHandler.VAR_APP_USER_ID)
                .endBracket()
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_STATUS_ENUM, BasicStatusEnum.OK.name());

        List<InvoiceCategory> invoiceCategories = mainDatabaseHandler.findInvoiceCategoriesSql(sqlBuilder);

        for (InvoiceCategory invoiceCategory : invoiceCategories) {
            BigDecimal sumForCategory = new BigDecimal(0);

            for (InvoiceCostDistributionItem invoiceCostDistributionItem : invoiceCostDistributionItems) {
                if (invoiceCostDistributionItem.getInvoice().getInvoiceCategoryDTO() != null &&
                        invoiceCostDistributionItem.getInvoice().getInvoiceCategoryDTO().getInvoiceCategoryId().toString().equals(invoiceCategory.getInvoiceCategoryId().toString())){
                    BigDecimal sum = CostDistributionHelper.invoiceCostForPaymentPerson(context, invoiceCostDistributionItem.getInvoice(), currentUser);
                    sumForCategory = sumForCategory.add(sum);
                }
            }

            if (sumForCategory.compareTo(new BigDecimal(0)) != 0){
                categoryValue.put(invoiceCategory, sumForCategory);
            }
        }

        return categoryValue;
    }

    private static Map<PaymentPerson, BigDecimal> costsByPaymentRecipients(Context context, List<InvoiceCostDistributionItem> invoiceCostDistributionItems){

        Map<PaymentPerson, BigDecimal> personValue = new HashMap<>();

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(context);

        List<String> userContactIdsOfCurrentUser = LoginUserHelper.idsOfUserContactsWithCurrentUser(context);
        userContactIdsOfCurrentUser.add(currentUser.getAppUserId().toString());

        for (InvoiceCostDistributionItem invoiceCostDistributionItem : invoiceCostDistributionItems) {
            if (invoiceCostDistributionItem.getInvoice().getPaymentRecipientId() != null && !userContactIdsOfCurrentUser.contains(invoiceCostDistributionItem.getInvoice().getPaymentRecipientId().toString())){

                IPaymentPerson paymentRecipient = invoiceCostDistributionItem.getInvoice().getPaymentRecipient(context);
                PaymentPerson paymentPerson = PaymentPerson.fromIPaymentPerson(paymentRecipient);

                BigDecimal sum = CostDistributionHelper.invoiceCostForPaymentPerson(context, invoiceCostDistributionItem.getInvoice(), currentUser);
                if (personValue.containsKey(paymentPerson)){
                    sum = sum.add(personValue.get(paymentPerson));
                }

                personValue.remove(paymentPerson);
                personValue.put(paymentPerson, sum);
            }
        }

        return personValue;
    }

    private static Map<PaymentPerson, BigDecimal> costsByCostsPayer(Context context, List<InvoiceCostDistributionItem> invoiceCostDistributionItems){

        Map<PaymentPerson, BigDecimal> personValue = new HashMap<>();

        Map<PaymentPerson, List<PaymentPerson>> personIds = new HashMap<>();

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(context);
        List<PaymentPerson> userIds = new ArrayList<>();

        PaymentPerson currentUserBean = new PaymentPerson();
        currentUserBean.setPaymentPersonEnum(PaymentPersonTypeEnum.USER);
        currentUserBean.setPaymentPersonId(currentUser.getAppUserId());
        currentUserBean.setPaymentPersonName(currentUser.getAppUserName());

        userIds.add(currentUserBean);
        personIds.put(currentUserBean, userIds);

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);
        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_USER_CONTACT);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString())
        .and().isEqual(MainDatabaseHandler.VAR_BASIC_STATUS_ENUM, BasicStatusEnum.OK.name());
        List<UserContact> userContacts = mainDatabaseHandler.findUserContactsSql(sqlBuilder);

        for (UserContact userContact : userContacts) {
            List<PaymentPerson> userContactIds = new ArrayList<>();

            PaymentPerson userContactBean = new PaymentPerson();
            userContactBean.setPaymentPersonEnum(PaymentPersonTypeEnum.CONTACT);
            userContactBean.setPaymentPersonId(userContact.getUserContactId());
            userContactBean.setPaymentPersonName(userContact.getContactName());

            if (userContact.getAppUserContactId() != null){
                PaymentPerson appUserContactBean = new PaymentPerson();
                appUserContactBean.setPaymentPersonEnum(PaymentPersonTypeEnum.USER);
                appUserContactBean.setPaymentPersonId(userContact.getAppUserContactId());
                appUserContactBean.setPaymentPersonName(userContact.getContactName());

                userContactIds.add(appUserContactBean);
            }else{
                userContactIds.add(userContactBean);
            }

            personIds.put(userContactBean, userContactIds);
        }

        for (Map.Entry<PaymentPerson, List<PaymentPerson>> paymentPersonBeanListEntry : personIds.entrySet()) {

            PaymentPerson paymentPerson = paymentPersonBeanListEntry.getKey();

            BigDecimal sumTotalForBean = new BigDecimal(0);
            for (InvoiceCostDistributionItem invoiceCostDistributionItem : invoiceCostDistributionItems) {

                for (PaymentPerson personBean : paymentPersonBeanListEntry.getValue()) {
                    BigDecimal sum = CostDistributionHelper.invoiceCostForPaymentPerson(context, invoiceCostDistributionItem.getInvoice(), personBean);

                    sumTotalForBean = sumTotalForBean.add(sum);
                }
            }

            if (!sumTotalForBean.equals(new BigDecimal(0))){
                personValue.put(paymentPerson, sumTotalForBean);
            }
        }

        return personValue;
    }

}
