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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.enums.BasicDataSubType;
import de.nextbill.client.enums.BasicDataType;
import de.nextbill.client.enums.BasicStatusEnum;
import de.nextbill.client.enums.CorrectionStatus;
import de.nextbill.client.enums.InvoiceStatusEnum;
import de.nextbill.client.enums.MainFunctionEnum;
import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.enums.RepetitionTypeEnum;
import de.nextbill.client.enums.SQLOperatorEnum;
import de.nextbill.client.interfaces.IPaymentItem;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.BasicData;
import de.nextbill.client.model.BusinessPartner;
import de.nextbill.client.model.CostDistribution;
import de.nextbill.client.model.CostDistributionItem;
import de.nextbill.client.model.IPaymentPerson;
import de.nextbill.client.model.Invoice;
import de.nextbill.client.model.InvoiceCategory;
import de.nextbill.client.model.PaymentPerson;
import de.nextbill.client.model.UserContact;
import de.nextbill.client.utils.BillingListItem;
import de.nextbill.client.utils.InvoiceCostDistributionItem;
import de.nextbill.client.utils.SqlBuilder;


public class StatisticHelper {

    public static BigDecimal restSumForMonth(Context context){
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

        AppUser appUser = LoginUserHelper.currentLoggedInUser(context);

        BigDecimal sumForCurrentMonth = sumForMonth(context);

        BigDecimal averageExpenseAfterNow = new BigDecimal(0);
        BigDecimal monthAverageExpense = new BigDecimal(0);
        BigDecimal estimatedIncomeForCurrentMonth = new BigDecimal(0);
        BigDecimal averageExpenseTillNow = new BigDecimal(0);
        BigDecimal monthAverageIncome = new BigDecimal(0);

        Boolean areThereEnoughEntriesForAnalysis = true;

        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_APP_USER_ID, appUser.getAppUserId().toString())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.STATISTIC.name())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_SUB_TYPE, BasicDataSubType.AVERAGE_EXPENSE_TILL_NOW.name());
        List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

        if (!basicDatas.isEmpty()){
            averageExpenseTillNow = basicDatas.get(0).getNumberValue();
        }

        sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_APP_USER_ID, appUser.getAppUserId().toString())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.STATISTIC.name())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_SUB_TYPE, BasicDataSubType.AVERAGE_EXPENSE_AFTER_NOW.name());
        basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

        if (!basicDatas.isEmpty()){
            averageExpenseAfterNow = basicDatas.get(0).getNumberValue();
        }

        sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_APP_USER_ID, appUser.getAppUserId().toString())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.STATISTIC.name())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_SUB_TYPE, BasicDataSubType.MONTH_AVERAGE_EXPENSE.name());
        basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

        if (!basicDatas.isEmpty()){
            monthAverageExpense = basicDatas.get(0).getNumberValue();
        }

        sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_APP_USER_ID, appUser.getAppUserId().toString())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.STATISTIC.name())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_SUB_TYPE, BasicDataSubType.MONTH_AVERAGE_INCOME.name());
        basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

        if (!basicDatas.isEmpty()){
            monthAverageIncome = basicDatas.get(0).getNumberValue();
        }

        sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_APP_USER_ID, appUser.getAppUserId().toString())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.STATISTIC.name())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_SUB_TYPE, BasicDataSubType.ESTIMATED_INCOME_FOR_CURRENT_MONTH.name());
        basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

        if (!basicDatas.isEmpty()){
            estimatedIncomeForCurrentMonth = basicDatas.get(0).getNumberValue();
        }

        sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_APP_USER_ID, appUser.getAppUserId().toString())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.STATISTIC.name())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_SUB_TYPE, BasicDataSubType.ARE_THERE_ENOUGH_ENTRIES_FOR_ANALYSIS.name());
        basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

        if (!basicDatas.isEmpty()){
            areThereEnoughEntriesForAnalysis = Boolean.parseBoolean(basicDatas.get(0).getValue());
        }

        BigDecimal estimatedTotalSumForMonth = sumForCurrentMonth.add(averageExpenseAfterNow);
//            BigDecimal differenceBetweenNowAndAverage = averageExpenseTillNow.subtract(sumForCurrentMonth);

        BigDecimal resultPlusAtMonthEnd = new BigDecimal(0);
        if (areThereEnoughEntriesForAnalysis){
            resultPlusAtMonthEnd = estimatedIncomeForCurrentMonth.subtract(estimatedTotalSumForMonth);
        }else{
            resultPlusAtMonthEnd = estimatedIncomeForCurrentMonth.subtract(monthAverageExpense);
        }

        return resultPlusAtMonthEnd;
    }

    public static BigDecimal sumForMonth(Context context){

        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.TABLE_INVOICE);
        sqlBuilder.addTable(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM);
        sqlBuilder
            .startBracket()
                .isNotEqualFields(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_ID, MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_STANDING_ORDER_INVOICE_TEMPLATE_ID)
                .or()
                .isNull(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_STANDING_ORDER_INVOICE_TEMPLATE_ID)
            .endBracket()
            .and();

        SqlBuilder sqlBuilderCopy = sqlBuilder.copy();

        List<MainFunctionEnum> mainFunctionEna = new ArrayList<>();
        mainFunctionEna.add(MainFunctionEnum.EXPENSE);

        List<InvoiceCostDistributionItem> invoiceCostDistributionItems = SearchHelper.search(context, mainFunctionEna,true,null,null,null,null,null,null,null,null,null,sqlBuilderCopy);
        List<Invoice> invoices = SearchHelper.invoicesInInvoiceCostDistributionItems(invoiceCostDistributionItems);

        Date now = new Date();

        Calendar calendar = new GregorianCalendar();
        calendar.setTime(now);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.add(Calendar.MINUTE, -1);
        Date startDate = calendar.getTime();

        Calendar calendar2 = new GregorianCalendar();
        calendar2.setTime(now);
        Date endDate = calendar2.getTime();

        AppUser appUser = LoginUserHelper.currentLoggedInUser(context);

        BigDecimal sumResult = new BigDecimal(0);

        for (Invoice invoice : invoices) {

            if (invoice.getDateOfInvoice() == null){
                continue;
            }

            Calendar invoiceDate =  Calendar.getInstance();
            invoiceDate.setTime(invoice.getDateOfInvoice());
            invoiceDate.set(Calendar.HOUR_OF_DAY, 6);
            invoiceDate.set(Calendar.MINUTE, 5);
            Date invoiceDateDate = invoiceDate.getTime();

            if (invoiceDateDate.after(startDate) && invoiceDateDate.before(endDate)){
                BigDecimal invoiceCost = CostDistributionHelper.invoiceCostForPaymentPerson(context, invoice, appUser);
                sumResult = sumResult.add(invoiceCost);
            }
        }

        sumResult = sumResult.multiply(new BigDecimal(-1));

        return sumResult;
    }

    public static Boolean findSpecialTypeForCategory(Context context, UUID invoiceCategoryId){
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(context);
        if (currentUser == null){
            return null;
        }

        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_OBJECT_1_ID, invoiceCategoryId.toString())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_OBJECT_1_CLASS, InvoiceCategory.class.getSimpleName())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.STATISTIC.name())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_SUB_TYPE, BasicDataSubType.SPECIAL_TYPE_FOR_CATEGORY.name())
                .and()
                .isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString());

        List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

        if (!basicDatas.isEmpty()){
            String value = basicDatas.get(0).getObject2Id();
            Boolean booleanValue = Boolean.valueOf(value);
            return booleanValue;
        }

        return null;
    }

    public static RepetitionTypeEnum findRepetitionTypeForCategory(Context context, UUID invoiceCategoryId){
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(context);
        if (currentUser == null){
            return null;
        }

        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_OBJECT_1_ID, invoiceCategoryId.toString())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_OBJECT_1_CLASS, InvoiceCategory.class.getSimpleName())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.STATISTIC.name())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_SUB_TYPE, BasicDataSubType.REPETITION_TYPE_FOR_CATEGORY.name())
                .and()
                .isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString());

        List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

        if (!basicDatas.isEmpty()){
            RepetitionTypeEnum value = RepetitionTypeEnum.valueOf(basicDatas.get(0).getObject2Id());
            return value;
        }

        return null;
    }

    public static InvoiceCategory findCategoryForBusinessPartner(Context context, IPaymentPerson businessPartner){
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(context);
        if (currentUser == null){
            return null;
        }

        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_OBJECT_1_ID, businessPartner.getPaymentPersonId().toString())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_OBJECT_1_CLASS, BusinessPartner.class.getSimpleName())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.STATISTIC.name())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_SUB_TYPE, BasicDataSubType.CATEGORY_FOR_BUSINESS_PARTNER.name())
                .and()
                .isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString());

        List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

        if (!basicDatas.isEmpty()){
            SqlBuilder sqlBuilder1 = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE_CATEGORY);
            sqlBuilder1.isEqual(MainDatabaseHandler.VAR_INVOICE_CATEGORY_ID, basicDatas.get(0).getObject2Id());

            List<InvoiceCategory> invoiceCategories = mainDatabaseHandler.findInvoiceCategoriesSql(sqlBuilder1);

            if (!invoiceCategories.isEmpty()){
                return invoiceCategories.get(0);
            }
        }

        return null;
    }

    public static CostDistribution findCostDistributionForCategory(Context context, UUID invoiceCategoryId
    ){
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(context);
        if (currentUser == null){
            return null;
        }

        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_OBJECT_1_ID, invoiceCategoryId.toString())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_OBJECT_1_CLASS, InvoiceCategory.class.getSimpleName())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.STATISTIC.name())
                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_SUB_TYPE, BasicDataSubType.COST_DISTRIBUTION_FOR_CATEGORY.name())
                .and()
                .isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString());

        List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

        if (!basicDatas.isEmpty()){
            List<CostDistribution> costDistributions = mainDatabaseHandler.findCostDistributions(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ID, basicDatas.get(0).getObject2Id());

            if (!costDistributions.isEmpty()){
                return costDistributions.get(0);
            }
        }

        return null;
    }

    public static List<BillingListItem> createBillingListItems(Context context){

        List<BillingListItem> billingListItems = new ArrayList<>();

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

        AppUser currentUser = LoginUserHelper.currentLoggedInUser(context);

        SqlBuilder sqlBuilder2 = new SqlBuilder(MainDatabaseHandler.TABLE_USER_CONTACT);
        sqlBuilder2
//                .isNotNull(MainDatabaseHandler.VAR_APP_USER_CONTACT_ID)
//                .and()
                .isEqual(MainDatabaseHandler.VAR_BASIC_STATUS_ENUM, BasicStatusEnum.OK.name());

        List<UserContact> userContactAppUsers = mainDatabaseHandler.findUserContactsSql(sqlBuilder2);

        List<AppUser> appUsersForFoundUserContacts = new ArrayList<>();

        for (UserContact userContact : userContactAppUsers) {
            BillingListItem sumToDebtBillingItem = sumToBeDebt(context, currentUser, userContact, false);
            BigDecimal sumToDebt = sumToDebtBillingItem.getSumToBePaid();

            if (sumToDebt.compareTo(new BigDecimal(0)) != 0){
                if (sumToDebt.compareTo(new BigDecimal(0)) < 0){
                    BillingListItem billingListItem = new BillingListItem();
                    billingListItem.setInvoicePayer(PaymentPerson.fromIPaymentPerson(currentUser));
                    billingListItem.setSumToBePaid(sumToDebt.multiply(new BigDecimal(-1)));
                    billingListItem.setCostPayer(PaymentPerson.fromIPaymentPerson(userContact));
                    billingListItem.setInvoiceCategoriesOfCostPayer(sumToDebtBillingItem.getInvoiceCategoriesOfInvoicePayer());
                    billingListItem.setInvoiceCategoriesOfInvoicePayer(sumToDebtBillingItem.getInvoiceCategoriesOfCostPayer());
                    BigDecimal costsFromCostPayerResult = sumToDebtBillingItem.getCostsFromCostPayer().compareTo(new BigDecimal(0)) < 0 ? sumToDebtBillingItem.getCostsFromCostPayer().multiply(new BigDecimal(-1)) : sumToDebtBillingItem.getCostsFromCostPayer();
                    billingListItem.setCostsFromInvoicePayer(costsFromCostPayerResult);
                    BigDecimal costsFromInvoicePayerResult = sumToDebtBillingItem.getCostsFromInvoicePayer().compareTo(new BigDecimal(0)) < 0 ? sumToDebtBillingItem.getCostsFromInvoicePayer().multiply(new BigDecimal(-1)) : sumToDebtBillingItem.getCostsFromInvoicePayer();
                    billingListItem.setCostsFromCostPayer(costsFromInvoicePayerResult);

                    billingListItems.add(billingListItem);
                }else{
                    BillingListItem billingListItem = new BillingListItem();
                    billingListItem.setInvoicePayer(PaymentPerson.fromIPaymentPerson(userContact));
                    billingListItem.setSumToBePaid(sumToDebt);
                    billingListItem.setCostPayer(PaymentPerson.fromIPaymentPerson(currentUser));
                    billingListItem.setInvoiceCategoriesOfCostPayer(sumToDebtBillingItem.getInvoiceCategoriesOfCostPayer());
                    billingListItem.setInvoiceCategoriesOfInvoicePayer(sumToDebtBillingItem.getInvoiceCategoriesOfInvoicePayer());
                    BigDecimal costsFromInvoicePayerResult = sumToDebtBillingItem.getCostsFromInvoicePayer().compareTo(new BigDecimal(0)) < 0 ? sumToDebtBillingItem.getCostsFromInvoicePayer().multiply(new BigDecimal(-1)) : sumToDebtBillingItem.getCostsFromInvoicePayer();
                    billingListItem.setCostsFromInvoicePayer(costsFromInvoicePayerResult);
                    BigDecimal costsFromCostPayerResult = sumToDebtBillingItem.getCostsFromCostPayer().compareTo(new BigDecimal(0)) < 0 ? sumToDebtBillingItem.getCostsFromCostPayer().multiply(new BigDecimal(-1)) : sumToDebtBillingItem.getCostsFromCostPayer();
                    billingListItem.setCostsFromCostPayer(costsFromCostPayerResult);

                    billingListItems.add(billingListItem);
                }

                if (userContact.getAppUserContactId() != null){
                    SqlBuilder sqlBuilder3 = new SqlBuilder(MainDatabaseHandler.TABLE_APP_USER);
                    sqlBuilder3.isEqual(MainDatabaseHandler.VAR_APP_USER_ID, userContact.getAppUserContactId().toString());
                    List<AppUser> foundAppUsers = mainDatabaseHandler.findAppUsers(sqlBuilder3);

                    if (!foundAppUsers.isEmpty()){
                        appUsersForFoundUserContacts.add(foundAppUsers.get(0));
                    }
                }
            }
        }

        List<AppUser> appUsers = mainDatabaseHandler.findAppUsers(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString(), SQLOperatorEnum.NOT);
        appUsers.removeAll(appUsersForFoundUserContacts);

        for (AppUser appUser : appUsers) {

            sqlBuilder2 = new SqlBuilder(MainDatabaseHandler.TABLE_USER_CONTACT);
            sqlBuilder2.isEqual(MainDatabaseHandler.VAR_APP_USER_CONTACT_ID, currentUser.getAppUserId().toString())
                    .and()
                    .isEqual(MainDatabaseHandler.VAR_BASIC_STATUS_ENUM, BasicStatusEnum.OK.name())
                    .and()
                    .isEqual(MainDatabaseHandler.VAR_APP_USER_ID, appUser.getAppUserId().toString());

            userContactAppUsers = mainDatabaseHandler.findUserContactsSql(sqlBuilder2);

            UserContact tmpUserContact = null;
            if(userContactAppUsers.isEmpty()){
                continue;
            }
            tmpUserContact = userContactAppUsers.get(0);

            BillingListItem sumToDebtBillingItem = sumToBeDebt(context, appUser, tmpUserContact, false);
            BigDecimal sumToDebt = sumToDebtBillingItem.getSumToBePaid();

            if (sumToDebt.compareTo(new BigDecimal(0)) != 0){
                if (sumToDebt.compareTo(new BigDecimal(0)) < 0){
                    BillingListItem billingListItem = new BillingListItem();
                    billingListItem.setInvoicePayer(PaymentPerson.fromIPaymentPerson(appUser));
                    billingListItem.setSumToBePaid(sumToDebt.multiply(new BigDecimal(-1)));
                    billingListItem.setCostPayer(PaymentPerson.fromIPaymentPerson(tmpUserContact));
                    billingListItem.setInvoiceCategoriesOfCostPayer(sumToDebtBillingItem.getInvoiceCategoriesOfInvoicePayer());
                    billingListItem.setInvoiceCategoriesOfInvoicePayer(sumToDebtBillingItem.getInvoiceCategoriesOfCostPayer());
                    BigDecimal costsFromCostPayerResult = sumToDebtBillingItem.getCostsFromCostPayer().compareTo(new BigDecimal(0)) < 0 ? sumToDebtBillingItem.getCostsFromCostPayer().multiply(new BigDecimal(-1)) : sumToDebtBillingItem.getCostsFromCostPayer();
                    billingListItem.setCostsFromInvoicePayer(costsFromCostPayerResult);
                    BigDecimal costsFromInvoicePayerResult = sumToDebtBillingItem.getCostsFromInvoicePayer().compareTo(new BigDecimal(0)) < 0 ? sumToDebtBillingItem.getCostsFromInvoicePayer().multiply(new BigDecimal(-1)) : sumToDebtBillingItem.getCostsFromInvoicePayer();
                    billingListItem.setCostsFromCostPayer(costsFromInvoicePayerResult);

                    billingListItems.add(billingListItem);
                }else{
                    BillingListItem billingListItem = new BillingListItem();
                    billingListItem.setInvoicePayer(PaymentPerson.fromIPaymentPerson(tmpUserContact));
                    billingListItem.setSumToBePaid(sumToDebt);
                    billingListItem.setCostPayer(PaymentPerson.fromIPaymentPerson(appUser));
                    billingListItem.setInvoiceCategoriesOfCostPayer(sumToDebtBillingItem.getInvoiceCategoriesOfCostPayer());
                    billingListItem.setInvoiceCategoriesOfInvoicePayer(sumToDebtBillingItem.getInvoiceCategoriesOfInvoicePayer());
                    BigDecimal costsFromInvoicePayerResult = sumToDebtBillingItem.getCostsFromInvoicePayer().compareTo(new BigDecimal(0)) < 0 ? sumToDebtBillingItem.getCostsFromInvoicePayer().multiply(new BigDecimal(-1)) : sumToDebtBillingItem.getCostsFromInvoicePayer();
                    billingListItem.setCostsFromInvoicePayer(costsFromInvoicePayerResult);
                    BigDecimal costsFromCostPayerResult = sumToDebtBillingItem.getCostsFromCostPayer().compareTo(new BigDecimal(0)) < 0 ? sumToDebtBillingItem.getCostsFromCostPayer().multiply(new BigDecimal(-1)) : sumToDebtBillingItem.getCostsFromCostPayer();
                    billingListItem.setCostsFromCostPayer(costsFromCostPayerResult);

                    billingListItems.add(billingListItem);
                }
            }
        }

        return billingListItems;
    }

    public static BillingListItem sumToBeDebt(Context context, AppUser currentUser, UserContact costPayer, boolean isUsePaidInvoices){

        BillingListItem billingListItem = new BillingListItem();

        List<IPaymentItem> sumTotalItemsExpense = sumToBeDebtExpenseNormal(context, currentUser, costPayer, isUsePaidInvoices);
        BigDecimal sumTotalNormal = new BigDecimal(0);
        BigDecimal sumTotalNormalExpense = new BigDecimal(0);
        for (IPaymentItem costDistributionItem : sumTotalItemsExpense) {
            sumTotalNormal = sumTotalNormal.add(costDistributionItem.getMoneyValue());
            sumTotalNormalExpense = sumTotalNormalExpense.add(costDistributionItem.getMoneyValue());
        }

        List<IPaymentItem> sumTotalItemsIncome = sumToBeDebtIncomeNormal(context, currentUser, costPayer, isUsePaidInvoices);
        BigDecimal sumTotalNormalIncome = new BigDecimal(0);
        for (IPaymentItem costDistributionItem : sumTotalItemsIncome) {
            sumTotalNormal = sumTotalNormal.subtract(costDistributionItem.getMoneyValue());
            sumTotalNormalIncome = sumTotalNormalIncome.add(costDistributionItem.getMoneyValue());
        }

        List<IPaymentItem> sumTotalReverseItemsExpense = new ArrayList<>();
        BigDecimal sumTotalReverse = new BigDecimal(0);
        BigDecimal sumTotalReverseExpense = new BigDecimal(0);
        if (costPayer.getAppUserContactId() != null){
            sumTotalReverseItemsExpense = sumToBeDebtExpenseReverse(context, costPayer, currentUser, isUsePaidInvoices);
            for (IPaymentItem costDistributionItem : sumTotalReverseItemsExpense) {
                sumTotalReverse = sumTotalReverse.add(costDistributionItem.getMoneyValue());
                sumTotalReverseExpense = sumTotalReverseExpense.add(costDistributionItem.getMoneyValue());
            }
        }

        List<IPaymentItem> sumTotalReverseItemsIncome = new ArrayList<>();
        BigDecimal sumTotalReverseIncome = new BigDecimal(0);
        if (costPayer.getAppUserContactId() != null){
            sumTotalReverseItemsIncome = sumToBeDebtIncomeReverse(context, costPayer, currentUser, isUsePaidInvoices);
            for (IPaymentItem costDistributionItem : sumTotalReverseItemsIncome) {
                sumTotalReverse = sumTotalReverse.subtract(costDistributionItem.getMoneyValue());
                sumTotalReverseIncome = sumTotalReverseIncome.add(costDistributionItem.getMoneyValue());
            }
        }

        BigDecimal sumTotal = sumTotalNormal.subtract(sumTotalReverse);

        billingListItem.setSumToBePaid(sumTotal);
        billingListItem.setCostsFromInvoicePayer(sumTotalNormalIncome.add(sumTotalReverseExpense));
        billingListItem.setCostsFromCostPayer(sumTotalNormalExpense.add(sumTotalReverseIncome));

        List<IPaymentItem> costItemsForInvoicePayer = new ArrayList<>();
        costItemsForInvoicePayer.addAll(sumTotalItemsIncome);
        costItemsForInvoicePayer.addAll(sumTotalReverseItemsExpense);
        List<InvoiceCategory> invoiceCategoriesInvoicePayer = categoriesForPaymentItems(context, costItemsForInvoicePayer);

        List<IPaymentItem> costItemsForCostPayer = new ArrayList<>();
        costItemsForCostPayer.addAll(sumTotalItemsExpense);
        costItemsForCostPayer.addAll(sumTotalReverseItemsIncome);
        List<InvoiceCategory> invoiceCategoriesCostPayer = categoriesForPaymentItems(context, costItemsForCostPayer);

        billingListItem.setInvoiceCategoriesOfInvoicePayer(invoiceCategoriesInvoicePayer);
        billingListItem.setInvoiceCategoriesOfCostPayer(invoiceCategoriesCostPayer);

        return billingListItem;
    }

    public static List<IPaymentItem> sumToBeDebtExpenseNormal(Context context, AppUser currentUser, UserContact costPayer, boolean isUsePaidInvoices){

        List<IPaymentItem> costDistributionItems = new ArrayList<>();
        costDistributionItems = findCostDistributionItems(context, MainFunctionEnum.EXPENSE, currentUser.getPaymentPersonId(), currentUser.getPaymentPersonEnum(), costPayer.getPaymentPersonId(), costPayer.getPaymentPersonEnum(), InvoiceStatusEnum.READY, isUsePaidInvoices);
        costDistributionItems.addAll(findDirectTransactionsCostDistributionItems(context, MainFunctionEnum.INCOME, currentUser.getPaymentPersonId(), currentUser.getPaymentPersonEnum(), costPayer.getPaymentPersonId(), costPayer.getPaymentPersonEnum(), InvoiceStatusEnum.READY, isUsePaidInvoices));

        return costDistributionItems;
    }

    public static List<IPaymentItem> sumToBeDebtIncomeNormal(Context context, AppUser currentUser, UserContact costPayer, boolean isUsePaidInvoices){

        List<IPaymentItem> costDistributionItems = new ArrayList<>();
        costDistributionItems = findCostDistributionItems(context, MainFunctionEnum.INCOME, currentUser.getPaymentPersonId(), currentUser.getPaymentPersonEnum(), costPayer.getPaymentPersonId(), costPayer.getPaymentPersonEnum(), InvoiceStatusEnum.READY, isUsePaidInvoices);
        costDistributionItems.addAll(findDirectTransactionsCostDistributionItems(context, MainFunctionEnum.EXPENSE, currentUser.getPaymentPersonId(), currentUser.getPaymentPersonEnum(), costPayer.getPaymentPersonId(), costPayer.getPaymentPersonEnum(), InvoiceStatusEnum.READY, isUsePaidInvoices));

        return costDistributionItems;
    }

    public static List<IPaymentItem> sumToBeDebtExpenseReverse(Context context, UserContact costPayer, AppUser currentUser, boolean isUsePaidInvoices){
        List<IPaymentItem> costDistributionItems = new ArrayList<>();

        if (costPayer.getAppUserContactId() != null){

            MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

            List<AppUser> appUsers = mainDatabaseHandler.findAppUsers(MainDatabaseHandler.VAR_APP_USER_ID, costPayer.getAppUserContactId().toString());
            AppUser contactAsAppUser = appUsers.get(0);

            SqlBuilder sqlBuilder2 = new SqlBuilder(MainDatabaseHandler.TABLE_USER_CONTACT);
            sqlBuilder2.isEqual(MainDatabaseHandler.VAR_APP_USER_ID, costPayer.getAppUserContactId().toString())
                    .and()
                    .isEqual(MainDatabaseHandler.VAR_APP_USER_CONTACT_ID, currentUser.getAppUserId().toString());

            List<UserContact> newCostPayerContacts = mainDatabaseHandler.findUserContactsSql(sqlBuilder2);
            UserContact newCostPayerContact = null;
            if (!newCostPayerContacts.isEmpty()){
                newCostPayerContact = newCostPayerContacts.get(0);
            }
            if (newCostPayerContact != null){
                costDistributionItems = findCostDistributionItems(context, MainFunctionEnum.EXPENSE, contactAsAppUser.getPaymentPersonId(), contactAsAppUser.getPaymentPersonEnum(), newCostPayerContact.getPaymentPersonId(), newCostPayerContact.getPaymentPersonEnum(), InvoiceStatusEnum.READY, isUsePaidInvoices);
                costDistributionItems.addAll(findDirectTransactionsCostDistributionItems(context, MainFunctionEnum.INCOME, contactAsAppUser.getPaymentPersonId(), contactAsAppUser.getPaymentPersonEnum(), newCostPayerContact.getPaymentPersonId(), newCostPayerContact.getPaymentPersonEnum(), InvoiceStatusEnum.READY, isUsePaidInvoices));
            }
        }

        return costDistributionItems;
    }

    public static List<IPaymentItem> sumToBeDebtIncomeReverse(Context context, UserContact costPayer, AppUser currentUser, boolean isUsePaidInvoices){
        List<IPaymentItem> costDistributionItems = new ArrayList<>();

        if (costPayer.getAppUserContactId() != null){

            MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

            List<AppUser> appUsers = mainDatabaseHandler.findAppUsers(MainDatabaseHandler.VAR_APP_USER_ID, costPayer.getAppUserContactId().toString());
            AppUser contactAsAppUser = appUsers.get(0);

            SqlBuilder sqlBuilder2 = new SqlBuilder(MainDatabaseHandler.TABLE_USER_CONTACT);
            sqlBuilder2.isEqual(MainDatabaseHandler.VAR_APP_USER_ID, costPayer.getAppUserContactId().toString())
                    .and()
                    .isEqual(MainDatabaseHandler.VAR_APP_USER_CONTACT_ID, currentUser.getAppUserId().toString());

            List<UserContact> newCostPayerContacts = mainDatabaseHandler.findUserContactsSql(sqlBuilder2);
            UserContact newCostPayerContact = null;
            if (!newCostPayerContacts.isEmpty()){
                newCostPayerContact = newCostPayerContacts.get(0);
            }
            if (newCostPayerContact != null){
                costDistributionItems = findCostDistributionItems(context, MainFunctionEnum.INCOME, contactAsAppUser.getPaymentPersonId(), contactAsAppUser.getPaymentPersonEnum(), newCostPayerContact.getPaymentPersonId(), newCostPayerContact.getPaymentPersonEnum(), InvoiceStatusEnum.READY, isUsePaidInvoices);
                costDistributionItems.addAll(findDirectTransactionsCostDistributionItems(context, MainFunctionEnum.EXPENSE, contactAsAppUser.getPaymentPersonId(), contactAsAppUser.getPaymentPersonEnum(), newCostPayerContact.getPaymentPersonId(), newCostPayerContact.getPaymentPersonEnum(), InvoiceStatusEnum.READY, isUsePaidInvoices));
            }
        }

        return costDistributionItems;
    }

    public static List<InvoiceCategory> categoriesForPaymentItems(Context context, List<IPaymentItem> costDistributionItems){
        List<InvoiceCategory> invoiceCategories = new ArrayList<>();

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);
        for (IPaymentItem costDistributionItem : costDistributionItems) {

            if (costDistributionItem instanceof CostDistributionItem){

                List<Invoice> invoices = mainDatabaseHandler.findInvoices(MainDatabaseHandler.VAR_INVOICE_ID, SQLOperatorEnum.EQUAL, ((CostDistributionItem) costDistributionItem).getInvoiceId().toString());
                if (!invoices.isEmpty() && invoices.get(0).getInvoiceCategoryDTO() != null){

                    SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE_CATEGORY);
                    sqlBuilder.isEqual(MainDatabaseHandler.VAR_INVOICE_CATEGORY_ID, invoices.get(0).getInvoiceCategoryDTO().getInvoiceCategoryId().toString());

                    List<InvoiceCategory> invoiceCategoriesTmp = mainDatabaseHandler.findInvoiceCategoriesSql(sqlBuilder);
                    if (!invoiceCategoriesTmp.isEmpty() && !invoiceCategories.contains(invoiceCategoriesTmp.get(0))){
                        invoiceCategories.add(invoiceCategoriesTmp.get(0));
                    }
                }
            }else if (costDistributionItem instanceof Invoice){
                if (((Invoice) costDistributionItem).getInvoiceCategoryDTO() != null){
                    SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE_CATEGORY);
                    sqlBuilder.isEqual(MainDatabaseHandler.VAR_INVOICE_CATEGORY_ID, ((Invoice) costDistributionItem).getInvoiceCategoryDTO().getInvoiceCategoryId().toString());

                    List<InvoiceCategory> invoiceCategoriesTmp = mainDatabaseHandler.findInvoiceCategoriesSql(sqlBuilder);
                    if (!invoiceCategoriesTmp.isEmpty() && !invoiceCategories.contains(invoiceCategoriesTmp.get(0))){
                        invoiceCategories.add(invoiceCategoriesTmp.get(0));
                    }
                }
            }
        }

        return invoiceCategories;
    }

    public static List<IPaymentItem> findCostDistributionItems(Context context, MainFunctionEnum mainFunctionEnum, UUID invoicePayerId, PaymentPersonTypeEnum invoicePaymentPersonTypeEnum, UUID costPayerId, PaymentPersonTypeEnum costPaymentPersonTypeEnum, InvoiceStatusEnum invoiceStatusEnum, boolean paidAndNotPaid) {
        paidAndNotPaid = !paidAndNotPaid;

        List<IPaymentItem> resultCostDistributionItems = new ArrayList<>();

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_PAYER_ID, costPayerId.toString())
                .and()
                .isEqual(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ITEM_PAYMENT_PERSON_TYPE_ENUM, costPaymentPersonTypeEnum.name())
                .and()
                .isNotNull(MainDatabaseHandler.VAR_INVOICE_ID);

        if (paidAndNotPaid){
            sqlBuilder.and()
                .startBracket()
                    .startBracket()
                        .isNull(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ITEM_COST_PAID)
                        .or()
                        .isNull(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ITEM_MONEY_VALUE)
                        .or()
                        .isEqual(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ITEM_COST_PAID, String.valueOf(0))
                    .endBracket()
                    .and()
                    .isEqual(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ITEM_CORRECTION_STATUS, CorrectionStatus.READY.name())
                .endBracket();
        }

        List<CostDistributionItem> costDistributionItems = mainDatabaseHandler.findCostDistributionItemsSql(sqlBuilder);

        for (CostDistributionItem costDistributionItem : costDistributionItems) {

            SqlBuilder sqlBuilder2 = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE);

            if (mainFunctionEnum.equals(MainFunctionEnum.EXPENSE)){
                sqlBuilder2.isEqual(MainDatabaseHandler.VAR_INVOICE_ID, costDistributionItem.getInvoiceId().toString())
                        .and()
                        .isEqual(MainDatabaseHandler.VAR_PAYER_ID, invoicePayerId.toString())
                        .and()
                        .isEqual(MainDatabaseHandler.VAR_INVOICE_STATUS_ENUM, invoiceStatusEnum.name())
                        .and()
                        .isEqual(MainDatabaseHandler.VAR_PAYER_TYPE_ENUM, invoicePaymentPersonTypeEnum.name());
            }else{
                sqlBuilder2.isEqual(MainDatabaseHandler.VAR_INVOICE_ID, costDistributionItem.getInvoiceId().toString())
                        .and()
                        .isEqual(MainDatabaseHandler.VAR_PAYMENT_RECIPIENT_ID, invoicePayerId.toString())
                        .and()
                        .isEqual(MainDatabaseHandler.VAR_INVOICE_STATUS_ENUM, invoiceStatusEnum.name())
                        .and()
                        .isEqual(MainDatabaseHandler.VAR_PAYMENT_RECIPIENT_TYPE_ENUM, invoicePaymentPersonTypeEnum.name());
            }

            List<Invoice> invoices = mainDatabaseHandler.findInvoicesSql(sqlBuilder2);
            if (!invoices.isEmpty()){
                resultCostDistributionItems.add(costDistributionItem);
            }
        }

        return resultCostDistributionItems;
    }

    public static List<IPaymentItem> findDirectTransactionsCostDistributionItems(Context context, MainFunctionEnum mainFunctionEnum, UUID invoicePayerId, PaymentPersonTypeEnum invoicePaymentPersonTypeEnum, UUID costPayerId, PaymentPersonTypeEnum costPaymentPersonTypeEnum, InvoiceStatusEnum invoiceStatusEnum, boolean paidAndNotPaid) {
        paidAndNotPaid = !paidAndNotPaid;

        List<IPaymentItem> resultCostDistributionItems = new ArrayList<>();

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

        SqlBuilder sqlBuilder2 = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE);

        if (mainFunctionEnum.equals(MainFunctionEnum.EXPENSE)){
            sqlBuilder2.isEqual(MainDatabaseHandler.VAR_PAYMENT_RECIPIENT_ID, costPayerId.toString())
                    .and()
                    .isEqual(MainDatabaseHandler.VAR_PAYMENT_RECIPIENT_TYPE_ENUM, costPaymentPersonTypeEnum.name())
                    .and()
                    .isEqual(MainDatabaseHandler.VAR_PAYER_ID, invoicePayerId.toString())
                    .and()
                    .isEqual(MainDatabaseHandler.VAR_INVOICE_STATUS_ENUM, invoiceStatusEnum.name())
                    .and()
                    .isEqual(MainDatabaseHandler.VAR_PAYER_TYPE_ENUM, invoicePaymentPersonTypeEnum.name());
        }else{
            sqlBuilder2.isEqual(MainDatabaseHandler.VAR_PAYER_ID, costPayerId.toString())
                    .and()
                    .isEqual(MainDatabaseHandler.VAR_INVOICE_STATUS_ENUM, invoiceStatusEnum.name())
                    .and()
                    .isEqual(MainDatabaseHandler.VAR_PAYER_TYPE_ENUM, costPaymentPersonTypeEnum.name())
                    .and()
                    .isEqual(MainDatabaseHandler.VAR_PAYMENT_RECIPIENT_ID, invoicePayerId.toString())
                    .and()
                    .isEqual(MainDatabaseHandler.VAR_PAYMENT_RECIPIENT_TYPE_ENUM, invoicePaymentPersonTypeEnum.name());
        }

        if (paidAndNotPaid){
            sqlBuilder2.and()
                    .startBracket()
                        .startBracket()
                            .isNull(MainDatabaseHandler.VAR_SUM_OF_INVOICE)
                            .or()
                            .isNull(MainDatabaseHandler.VAR_INVOICE_COST_PAID)
                            .or()
                            .isEqual(MainDatabaseHandler.VAR_SUM_OF_INVOICE, String.valueOf(0))
                        .endBracket()
                        .and()
                        .isEqual(MainDatabaseHandler.VAR_INVOICE_CORRECTION_STATUS, CorrectionStatus.READY.name())
                    .endBracket();
        }

        List<Invoice> invoices = mainDatabaseHandler.findInvoicesSql(sqlBuilder2);
        if (!invoices.isEmpty()){
            resultCostDistributionItems.addAll(invoices);
        }

        return resultCostDistributionItems;
    }
}
