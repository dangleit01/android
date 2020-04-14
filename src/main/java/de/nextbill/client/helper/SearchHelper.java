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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.enums.BasicDataSubType;
import de.nextbill.client.enums.BasicDataType;
import de.nextbill.client.enums.BasicSelectionEnum;
import de.nextbill.client.enums.MainFunctionEnum;
import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.enums.RepetitionTypeEnum;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.BasicData;
import de.nextbill.client.model.CostDistribution;
import de.nextbill.client.model.CostDistributionItem;
import de.nextbill.client.model.IPaymentPerson;
import de.nextbill.client.model.Invoice;
import de.nextbill.client.model.InvoiceCategory;
import de.nextbill.client.pojos.SearchFilter;
import de.nextbill.client.utils.InvoiceCostDistributionItem;
import de.nextbill.client.utils.SqlBuilder;


public class SearchHelper {

    public static SearchFilter searchAndUseSearchConfiguration(Context context, boolean distinctInvoices, SqlBuilder sqlBuilder){
        return searchAndUseSearchConfiguration(context, distinctInvoices, sqlBuilder, null);
    }

    public static SearchFilter searchAndUseSearchConfiguration(Context context, boolean distinctInvoices, SqlBuilder sqlBuilder, Date listStartDate){
        return searchAndUseSearchConfiguration(context, distinctInvoices, sqlBuilder, listStartDate, false);
    }

    public static SearchFilter searchAndUseSearchConfiguration(Context context, boolean distinctInvoices, SqlBuilder sqlBuilder, Date listStartDate, Boolean getOnlySearchBean){

        List<String> filters = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("dd. MMMM yyyy");

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(context);
        if (currentUser == null){
            SearchFilter searchFilter = new SearchFilter();
            searchFilter.setInvoiceCostDistributionItemList(new ArrayList<InvoiceCostDistributionItem>());
            searchFilter.setShowSearchString(false);
            return searchFilter;
        }

        List<MainFunctionEnum> mainFunctionEnums = new ArrayList<>();
        BasicData basicData = findBasicDataValue(context, BasicDataSubType.EXPENSE);
        if (basicData != null){
            mainFunctionEnums.add(MainFunctionEnum.EXPENSE);
            filters.add("Ausgaben");
        }

        basicData = findBasicDataValue(context, BasicDataSubType.INCOME);
        if (basicData != null){
            mainFunctionEnums.add(MainFunctionEnum.INCOME);
            filters.add("Einnahmen");
        }

        IPaymentPerson payer = null;
        basicData = findBasicDataValue(context, BasicDataSubType.PAYER);
        if (basicData != null){
            IPaymentPerson paymentPerson = mainDatabaseHandler.paymentPersonByTypeAndId(PaymentPersonTypeEnum.valueOf(basicData.getObject1Class()), UUID.fromString(basicData.getObject1Id()), context);
            if (paymentPerson != null){
                payer = paymentPerson;
                filters.add("Geldgeber: " +payer.getPaymentPersonName());
            }
        }

        IPaymentPerson paymentRecipient = null;
        basicData = findBasicDataValue(context, BasicDataSubType.PAYMENT_RECIPIENT);
        if (basicData != null){
            IPaymentPerson paymentPerson = mainDatabaseHandler.paymentPersonByTypeAndId(PaymentPersonTypeEnum.valueOf(basicData.getObject1Class()), UUID.fromString(basicData.getObject1Id()), context);
            if (paymentPerson != null){
                paymentRecipient = paymentPerson;
                filters.add("Zahlungsempfänger: " +paymentRecipient.getPaymentPersonName());
            }
        }

        IPaymentPerson costDistributor = null;
        basicData = findBasicDataValue(context, BasicDataSubType.COST_DISTRIBUTOR);
        if (basicData != null){
            IPaymentPerson paymentPerson = mainDatabaseHandler.paymentPersonByTypeAndId(PaymentPersonTypeEnum.valueOf(basicData.getObject1Class()), UUID.fromString(basicData.getObject1Id()), context);
            if (paymentPerson != null){
                costDistributor = paymentPerson;
                filters.add("Träger: " +costDistributor.getPaymentPersonName());
            }
        }

        InvoiceCategory invoiceCategory = null;
        basicData = findBasicDataValue(context, BasicDataSubType.INVOICE_CATEGORY);
        if (basicData != null){

            SqlBuilder sqlBuilder2 = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE_CATEGORY);
            sqlBuilder2.isEqual(MainDatabaseHandler.VAR_INVOICE_CATEGORY_ID, basicData.getValue());

            List<InvoiceCategory> invoiceCategories = mainDatabaseHandler.findInvoiceCategoriesSql(sqlBuilder2);

            if (!invoiceCategories.isEmpty()){
                invoiceCategory = invoiceCategories.get(0);
                filters.add("Kategorie: " +invoiceCategory.getInvoiceCategoryName());
            }
        }

        CostDistribution costDistribution = null;
        basicData = findBasicDataValue(context, BasicDataSubType.COST_DISTRIBUTION);
        if (basicData != null){

            List<CostDistribution> costDistributions = mainDatabaseHandler.findCostDistributions(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ID, basicData.getValue());

            if (!costDistributions.isEmpty()){
                costDistribution = costDistributions.get(0);
                filters.add("Kostenverteilung: " +costDistribution.getName());
            }
        }

        RepetitionTypeEnum repetitionTypeEnum = null;
        basicData = findBasicDataValue(context, BasicDataSubType.REPETITION_TYPE);
        if (basicData != null){

            RepetitionTypeEnum repetitionTypeEnumTmp = null;
            try{
                repetitionTypeEnumTmp  = RepetitionTypeEnum.valueOf(basicData.getValue());
            }catch (IllegalArgumentException e){
            }

            if (repetitionTypeEnumTmp != null){
                repetitionTypeEnum = repetitionTypeEnumTmp;
                filters.add("Bezugsraum: " +repetitionTypeEnum.getDisplayName());
            }
        }

        Boolean specialType = null;
        basicData = findBasicDataValue(context, BasicDataSubType.SPECIAL_TYPE);
        if (basicData != null){

            BasicSelectionEnum basicSelectionEnum = null;
            try{
                basicSelectionEnum  = BasicSelectionEnum.valueOf(basicData.getValue());
            }catch (IllegalArgumentException e){
            }

            if (basicSelectionEnum != null){
                if (basicSelectionEnum.equals(BasicSelectionEnum.YES)){
                    specialType = true;
                    filters.add("Sonderfälle");
                }else if (basicSelectionEnum.equals(BasicSelectionEnum.NO)){
                    specialType = false;
                    filters.add("Keine Sonderfälle");
                }
            }
        }

        Date dateStart = null;
        basicData = findBasicDataValue(context, BasicDataSubType.DATE_START);
        if (basicData != null){

            Date startDate = new Date();
            startDate.setTime(basicData.getNumberValue().longValue());

            if (startDate != null){
                dateStart = startDate;
                filters.add("Start: " + sdf.format(dateStart));
            }
        }else if (listStartDate != null){
            dateStart = listStartDate;
        }

        Date dateEnd = null;
        basicData = findBasicDataValue(context, BasicDataSubType.DATE_END);
        if (basicData != null){

            Date startDate = new Date();
            startDate.setTime(basicData.getNumberValue().longValue());

            if (startDate != null){
                dateEnd = startDate;
                filters.add("Ende: " + sdf.format(dateEnd));
            }
        }

        SearchFilter searchFilter = new SearchFilter();

        if (!getOnlySearchBean){
            List<InvoiceCostDistributionItem> invoiceCostDistributionItems = search(context, mainFunctionEnums, distinctInvoices, payer, paymentRecipient, costDistributor, specialType, repetitionTypeEnum, costDistribution, invoiceCategory, dateStart, dateEnd, sqlBuilder);
            searchFilter.setInvoiceCostDistributionItemList(invoiceCostDistributionItems);
        }

        if (!filters.isEmpty()){
            searchFilter.setSearchString(joinStrings(filters));
            searchFilter.setShowSearchString(true);
        }else{
            searchFilter.setShowSearchString(false);
        }

        return searchFilter;
    }

    public static String joinStrings(List<String> strings){
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < strings.size(); i++) {
            if (i == 0){
                stringBuilder.append(strings.get(i));
            }else{
                stringBuilder.append(", " + strings.get(i));
            }
        }
        return stringBuilder.toString();
    }

    public static BasicData findBasicDataValue(Context context, BasicDataSubType basicDataSubType){
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(context);

        if (currentUser == null){
            return null;
        }

        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.SEARCH.name())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_SUB_TYPE, basicDataSubType.name())
                .and()
                .isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString());

        List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

        if (!basicDatas.isEmpty()){
            return basicDatas.get(0);
        }

        return null;
    }

    public static List<InvoiceCostDistributionItem> search(Context context, List<MainFunctionEnum> mainFunctionEnums, boolean distinctInvoice, IPaymentPerson payerPerson, IPaymentPerson paymentRecipientPerson, IPaymentPerson costPayer, Boolean isSpecialType, RepetitionTypeEnum repetitionTypeEnum,
                                                    CostDistribution costDistribution, InvoiceCategory invoiceCategory, Date startDate, Date endDate, SqlBuilder sqlBuilder){
        List<InvoiceCostDistributionItem> invoiceCostDistributionItems = new ArrayList<>();

//        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.TABLE_INVOICE);
//        sqlBuilder.addTable(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM);

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(context);
        List<String> currentUserContacts = LoginUserHelper.idsOfUserContactsWithCurrentUser(context);

        if (currentUser == null){
            return new ArrayList<>();
        }

        buildQuery(mainFunctionEnums, payerPerson, paymentRecipientPerson, costPayer, isSpecialType, repetitionTypeEnum, invoiceCategory, startDate, endDate, sqlBuilder, currentUser, currentUserContacts);

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);
        List<CostDistributionItem> costDistributionItems = mainDatabaseHandler.findCostDistributionItemsSql(sqlBuilder);

        Set<String> distinctInvoices = new HashSet<>();
        for (CostDistributionItem costDistributionItem : costDistributionItems) {

            if (distinctInvoice && distinctInvoices.contains(costDistributionItem.getInvoiceId().toString())){
                continue;
            }

            InvoiceCostDistributionItem invoiceCostDistributionItem = new InvoiceCostDistributionItem();
            invoiceCostDistributionItem.setCostDistributionItem(costDistributionItem);

            SqlBuilder sqlBuilder1 = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE);
            sqlBuilder1.isEqual(MainDatabaseHandler.VAR_INVOICE_ID, costDistributionItem.getInvoiceId().toString());

            List<Invoice> invoicesTmp = mainDatabaseHandler.findInvoicesSql(sqlBuilder1);

            if (!invoicesTmp.isEmpty()){
                Invoice invoice = invoicesTmp.get(0);
                invoiceCostDistributionItem.setInvoice(invoice);
                invoiceCostDistributionItems.add(invoiceCostDistributionItem);

                distinctInvoices.add(invoice.getInvoiceId().toString());
            }
        }

        List<Invoice> invoices = mainDatabaseHandler.findInvoicesSql(sqlBuilder);
        for (Invoice invoice : invoices) {
            if (!distinctInvoices.contains(invoice.getInvoiceId().toString())){
                InvoiceCostDistributionItem invoiceCostDistributionItem = new InvoiceCostDistributionItem();
                invoiceCostDistributionItem.setInvoice(invoice);

                invoiceCostDistributionItems.add(invoiceCostDistributionItem);

                distinctInvoices.add(invoice.getInvoiceId().toString());
            }
        }

        if (costDistribution != null){
            List<InvoiceCostDistributionItem> resultInvoiceCostDistributionItems = new ArrayList<>();

            SqlBuilder sqlBuilder2 = new SqlBuilder(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM);
            sqlBuilder2.isEqual(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ID, costDistribution.getCostDistributionId().toString());
            List<CostDistributionItem> costDistributionItemsForCostDistribution = mainDatabaseHandler.findCostDistributionItemsSql(sqlBuilder2);

            for (InvoiceCostDistributionItem invoiceCostDistributionItem : invoiceCostDistributionItems) {
                SqlBuilder sqlBuilder1 = new SqlBuilder(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM);
                sqlBuilder1.isEqual(MainDatabaseHandler.VAR_INVOICE_ID, invoiceCostDistributionItem.getInvoice().getInvoiceId().toString());
                List<CostDistributionItem> costDistributionItemsForInvoice = mainDatabaseHandler.findCostDistributionItemsSql(sqlBuilder1);

                boolean isCostDistribution = CostDistributionHelper.areCostDistributionItemListsEqual(costDistributionItemsForInvoice, costDistributionItemsForCostDistribution);
                if (isCostDistribution){
                    resultInvoiceCostDistributionItems.add(invoiceCostDistributionItem);
                }
            }

            return resultInvoiceCostDistributionItems;

        }

        return invoiceCostDistributionItems;
    }

    private static SqlBuilder buildQuery(List<MainFunctionEnum> mainFunctionEnums, IPaymentPerson payerPerson, IPaymentPerson paymentRecipientPerson, IPaymentPerson costPayer, Boolean isSpecialType, RepetitionTypeEnum repetitionTypeEnum, InvoiceCategory invoiceCategory, Date startDate, Date endDate, SqlBuilder sqlBuilder, AppUser currentUser, List<String> currentUserContacts) {

        if (mainFunctionEnums != null && !mainFunctionEnums.isEmpty()){

            boolean incomePartUsed = false;

            if (mainFunctionEnums.contains(MainFunctionEnum.INCOME)){

                sqlBuilder.startBracket()
                        .startBracket()
                            .isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_PAYMENT_RECIPIENT_ID, currentUser.getAppUserId().toString())
                            .or()
                            .isIn(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_PAYMENT_RECIPIENT_ID, currentUserContacts)
                            .or()
                            .startBracket()
                                .isEqual(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.VAR_PAYER_ID, currentUser.getAppUserId().toString())
                                .and()
                                .isEqualFields(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_PAYMENT_RECIPIENT_ID, MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_CREATED_BY_ID)
                            .endBracket()
                            .or()
                            .startBracket()
                                .isIn(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.VAR_PAYER_ID, currentUserContacts)
                                .and()
                                .isEqualFields(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_PAYMENT_RECIPIENT_ID, MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_CREATED_BY_ID)
                            .endBracket()
                        .endBracket();

                incomePartUsed = true;
            }

            if (mainFunctionEnums.contains(MainFunctionEnum.EXPENSE)){

                if (incomePartUsed){
                    sqlBuilder.or();
                }

                sqlBuilder.startBracket()
                            .isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_PAYER_ID, currentUser.getAppUserId().toString())
                            .or()
                            .isIn(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_PAYER_ID, currentUserContacts)
                            .or()
                            .startBracket()
                                .isEqual(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.VAR_PAYER_ID, currentUser.getAppUserId().toString())
                                .and()
                                .isEqualFields(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_PAYER_ID, MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_CREATED_BY_ID)
                            .endBracket()
                            .or()
                            .startBracket()
                                .isIn(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.VAR_PAYER_ID, currentUserContacts)
                                .and()
                                .isEqualFields(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_PAYER_ID, MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_CREATED_BY_ID)
                            .endBracket()
                        .endBracket();
            }

            if (incomePartUsed){
                sqlBuilder.endBracket();
            }

            sqlBuilder.and();

        }

        sqlBuilder.isEqualFields(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.VAR_INVOICE_ID, MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_ID);

        if (payerPerson != null){
            sqlBuilder.and().isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_PAYER_ID, payerPerson.getPaymentPersonId().toString());
        }

        if (paymentRecipientPerson != null){
            sqlBuilder.and().isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_PAYMENT_RECIPIENT_ID, paymentRecipientPerson.getPaymentPersonId().toString());
        }

        if (costPayer != null){
            sqlBuilder.and().isEqual(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.VAR_PAYER_ID, costPayer.getPaymentPersonId().toString());
        }

        if (isSpecialType != null){
            if (isSpecialType){
                sqlBuilder.and().startBracket()
                        .isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_SPECIAL_TYPE, "1")
                        .endBracket();
            }else{
                sqlBuilder.and().startBracket()
                        .isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_SPECIAL_TYPE, "0")
                        .or()
                        .isNull(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_SPECIAL_TYPE)
                        .endBracket();
            }
        }

        if (repetitionTypeEnum != null){
            sqlBuilder.and().isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_REPETITION_TYPE, repetitionTypeEnum.name());
        }

        if (invoiceCategory != null){
            sqlBuilder.and().isEqual(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_CATEGORY_ID, invoiceCategory.getInvoiceCategoryId().toString());
        }

//        if (startDate != null & endDate != null){
//            sqlBuilder.and().isBetween(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_DATE_OF_INVOICE, startDate, endDate);
//        }

        if (startDate != null){
            sqlBuilder.and().isAfterThisDate(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_DATE_OF_INVOICE, startDate);
        }

        if (endDate != null){
            sqlBuilder.and().isBeforeThisDate(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_DATE_OF_INVOICE, endDate);
        }

        sqlBuilder.and().isNotNull(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.VAR_INVOICE_ID);

        return sqlBuilder;
    }

    public static List<Invoice> invoicesInInvoiceCostDistributionItems(List<InvoiceCostDistributionItem> invoiceCostDistributionItems){
        List<Invoice> invoices = new ArrayList<>();
        for (InvoiceCostDistributionItem invoiceCostDistributionItem : invoiceCostDistributionItems) {
            invoices.add(invoiceCostDistributionItem.getInvoice());
        }

        return invoices;
    }
}
