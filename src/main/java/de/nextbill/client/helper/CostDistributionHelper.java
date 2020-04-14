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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.enums.CorrectionStatus;
import de.nextbill.client.enums.CostDistributionItemTypeEnum;
import de.nextbill.client.enums.InvoiceStatusEnum;
import de.nextbill.client.enums.MainFunctionEnum;
import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.interfaces.IPaymentItem;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.CostDistribution;
import de.nextbill.client.model.CostDistributionItem;
import de.nextbill.client.model.IPaymentPerson;
import de.nextbill.client.model.Invoice;
import de.nextbill.client.model.PaymentPerson;
import de.nextbill.client.model.UserContact;
import de.nextbill.client.utils.SqlBuilder;


public class CostDistributionHelper {

    public static BigDecimal getCalculatedRestSum(BigDecimal sum, List<CostDistributionItem> costDistributionItems){
        BigDecimal restSumTmp;

//        boolean hasRestType = false;
//        for (CostDistributionItem tmpCostDistributionItem : costDistributionItems) {
//            if (tmpCostDistributionItem.getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.REST)){
//                hasRestType = true;
//                break;
//            }
//        }
//
//        if (!hasRestType){
        BigDecimal sumOfAllCostDistributions = new BigDecimal(0);
        for (CostDistributionItem tmpCostDistributionItem : costDistributionItems) {
            sumOfAllCostDistributions = sumOfAllCostDistributions.add(calculateAmountForCostDistributionItem(tmpCostDistributionItem, costDistributionItems, sum));
        }

        restSumTmp = sum.subtract(sumOfAllCostDistributions);
//        }else{
//            restSumTmp = new BigDecimal(0);
//        }

        return restSumTmp;
    }

    public static BigDecimal getCalculatedRestSumPrecise(BigDecimal sum, List<CostDistributionItem> costDistributionItems){
        BigDecimal restSumTmp;

//        boolean hasRestType = false;
//        for (CostDistributionItem tmpCostDistributionItem : costDistributionItems) {
//            if (tmpCostDistributionItem.getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.REST)){
//                hasRestType = true;
//                break;
//            }
//        }
//
//        if (!hasRestType){
        BigDecimal sumOfAllCostDistributions = new BigDecimal(0);
        for (CostDistributionItem tmpCostDistributionItem : costDistributionItems) {
            sumOfAllCostDistributions = sumOfAllCostDistributions.add(calculateAmountForCostDistributionItemPrecise(tmpCostDistributionItem, costDistributionItems, sum));
        }

        restSumTmp = sum.subtract(sumOfAllCostDistributions);
//        }else{
//            restSumTmp = new BigDecimal(0);
//        }

        return restSumTmp;
    }

    public static BigDecimal getMaxQuotaForOneCostDistributionItem(List<CostDistributionItem> costDistributionItemsWithoutCurrentItem){
        BigDecimal maxQuota = CostDistributionHelper.getCountWithoutFixed(costDistributionItemsWithoutCurrentItem).add(new BigDecimal(1));
        BigDecimal maxQuotaWeighted = CostDistributionHelper.getCountWeightedWithoutFixed(costDistributionItemsWithoutCurrentItem);
        BigDecimal maxQuotaForOneCostDistributionItem = maxQuota.subtract(maxQuotaWeighted);

        return maxQuotaForOneCostDistributionItem;
    }

    public static BigDecimal getMaxAmountForOneQuota(List<CostDistributionItem> costDistributionItemsWithoutCurrentItem, BigDecimal sum){
        BigDecimal maxAmountForOneCostDistributionItem = CostDistributionHelper.getMaxAmountForOneCostDistributionItem(costDistributionItemsWithoutCurrentItem, sum);
        BigDecimal countWithoutFixed = CostDistributionHelper.getCountWithoutFixed(costDistributionItemsWithoutCurrentItem).add(new BigDecimal(1));

        BigDecimal result = maxAmountForOneCostDistributionItem.divide(countWithoutFixed, 50, RoundingMode.HALF_UP);

        return result;
    }

    public static BigDecimal getMaxAmountForOneCostDistributionItem(List<CostDistributionItem> costDistributionItemsWithoutCurrentItem, BigDecimal sum){
        BigDecimal amountWithoutFixed = new BigDecimal(0);
        for (CostDistributionItem tmpCostDistributionItem1 : costDistributionItemsWithoutCurrentItem) {
            if (tmpCostDistributionItem1.getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.FIXED_AMOUNT) || tmpCostDistributionItem1.getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.PERCENT)){
                amountWithoutFixed = amountWithoutFixed.add(calculateAmountForCostDistributionItem(tmpCostDistributionItem1, costDistributionItemsWithoutCurrentItem, sum));
            }
        }

        BigDecimal sumOfCostDistributionItemsWithoutFixedAmount = sum.subtract(amountWithoutFixed);

        return sumOfCostDistributionItemsWithoutFixedAmount;
    }

    public static BigDecimal getCountWeightedWithoutFixed(List<CostDistributionItem> costDistributionItemsWithoutCurrentItem){
        BigDecimal countWeightedWithoutFixed = new BigDecimal(0);
        for (CostDistributionItem tmpCostDistributionItem1 : costDistributionItemsWithoutCurrentItem) {
            if (tmpCostDistributionItem1.getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.QUOTA)){
                countWeightedWithoutFixed = countWeightedWithoutFixed.add(tmpCostDistributionItem1.getValue());
            }
        }

        return countWeightedWithoutFixed;
    }

    public static BigDecimal calculateQuotaForValue(CostDistributionItemTypeEnum costDistributionItemTypeOfValue, BigDecimal value, List<CostDistributionItem> costDistributionItemsWithoutCurrentItem, BigDecimal sum){
        BigDecimal quotaForEt = new BigDecimal(0);
        if (costDistributionItemTypeOfValue.equals(CostDistributionItemTypeEnum.FIXED_AMOUNT)) {

            BigDecimal quotaWeighted = new BigDecimal(0);
            if (value.compareTo(new BigDecimal(0)) == 1){


                BigDecimal maxQuotaForOneCostDistributionItemTmp = getMaxQuotaForOneCostDistributionItem(costDistributionItemsWithoutCurrentItem);
                BigDecimal maxAmountForOneQuota = calculateAmountForValue(CostDistributionItemTypeEnum.QUOTA, maxQuotaForOneCostDistributionItemTmp, costDistributionItemsWithoutCurrentItem, sum);

                BigDecimal valueForCalculating = value;

                if (value.compareTo(maxAmountForOneQuota) == 1){
                    valueForCalculating = maxAmountForOneQuota;
                }else if (value.compareTo(new BigDecimal(0)) == -1 || value.compareTo(new BigDecimal(0)) == 0){
                    valueForCalculating = new BigDecimal(0);
                }

                if (valueForCalculating.compareTo(new BigDecimal(0)) != 0){
                    BigDecimal percentOfMaxAmountForOneQuota = valueForCalculating.divide(maxAmountForOneQuota, 50, RoundingMode.HALF_UP);
                    BigDecimal maxQuotaForOneCostDistributionItem = getMaxQuotaForOneCostDistributionItem(costDistributionItemsWithoutCurrentItem);
                    quotaWeighted = percentOfMaxAmountForOneQuota.multiply(maxQuotaForOneCostDistributionItem).setScale(1, RoundingMode.HALF_UP);
                }
            }

            quotaForEt = quotaWeighted;
        } else if (costDistributionItemTypeOfValue.equals(CostDistributionItemTypeEnum.QUOTA)) {
            quotaForEt = value.setScale(50, RoundingMode.HALF_UP);
        } else if (costDistributionItemTypeOfValue.equals(CostDistributionItemTypeEnum.PERCENT)) {
            BigDecimal quotaWeighted = new BigDecimal(0);
            if (value.compareTo(new BigDecimal(0)) == 1){

                BigDecimal maxQuotaForOneCostDistributionItemTmp = getMaxQuotaForOneCostDistributionItem(costDistributionItemsWithoutCurrentItem);
                BigDecimal maxAmountForOneQuota = calculateAmountForValue(CostDistributionItemTypeEnum.QUOTA, maxQuotaForOneCostDistributionItemTmp, costDistributionItemsWithoutCurrentItem, sum);

                BigDecimal valueForCalculating = calculateAmountForValue(CostDistributionItemTypeEnum.PERCENT, value, costDistributionItemsWithoutCurrentItem, sum);

                if (value.compareTo(maxAmountForOneQuota) == 1){
                    valueForCalculating = maxAmountForOneQuota;
                }else if (value.compareTo(new BigDecimal(0)) == -1 || value.compareTo(new BigDecimal(0)) == 0){
                    valueForCalculating = new BigDecimal(0);
                }else if (valueForCalculating.compareTo(maxAmountForOneQuota) == 1 || valueForCalculating.compareTo(maxAmountForOneQuota) == 0){
                    valueForCalculating = maxAmountForOneQuota;
                }

                if (valueForCalculating.compareTo(new BigDecimal(0)) != 0){
                    BigDecimal percentOfMaxAmountForOneQuota = valueForCalculating.divide(maxAmountForOneQuota, 50, RoundingMode.HALF_UP);
                    BigDecimal maxQuotaForOneCostDistributionItem = getMaxQuotaForOneCostDistributionItem(costDistributionItemsWithoutCurrentItem);
                    quotaWeighted = percentOfMaxAmountForOneQuota.multiply(maxQuotaForOneCostDistributionItem).setScale(1, RoundingMode.HALF_UP);
                }
            }

            quotaForEt = quotaWeighted;
        } else if (costDistributionItemTypeOfValue.equals(CostDistributionItemTypeEnum.REST)) {
            BigDecimal maxQuota = CostDistributionHelper.getCountWithoutFixed(costDistributionItemsWithoutCurrentItem);
            BigDecimal maxQuotaWeighted = CostDistributionHelper.getCountWeightedWithoutFixed(costDistributionItemsWithoutCurrentItem);
            quotaForEt = maxQuota.subtract(maxQuotaWeighted);
        }

        return  quotaForEt.setScale(1, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateAmountForValue(CostDistributionItemTypeEnum costDistributionItemTypeOfValue, BigDecimal value, List<CostDistributionItem> costDistributionItemsWithoutCurrentItem, BigDecimal sum){
        BigDecimal calculateAmountForValue = new BigDecimal(0);

        if (costDistributionItemTypeOfValue.equals(CostDistributionItemTypeEnum.QUOTA)){
            BigDecimal maxAmountForOneQuota = getMaxAmountForOneQuota(costDistributionItemsWithoutCurrentItem, sum);

            if (maxAmountForOneQuota.compareTo(new BigDecimal(0)) == -1){
                maxAmountForOneQuota = new BigDecimal(0);
            }

            BigDecimal quotaOfCostDistributionItem = value.multiply(maxAmountForOneQuota).setScale(2, RoundingMode.HALF_UP);
            calculateAmountForValue = quotaOfCostDistributionItem;
        }else if (costDistributionItemTypeOfValue.equals(CostDistributionItemTypeEnum.FIXED_AMOUNT)){

            calculateAmountForValue = value;
        }else if (costDistributionItemTypeOfValue.equals(CostDistributionItemTypeEnum.PERCENT)){
            BigDecimal amountForQuota = sum.multiply(value).setScale(2, RoundingMode.HALF_UP);
            calculateAmountForValue = amountForQuota;
        }else if (costDistributionItemTypeOfValue.equals(CostDistributionItemTypeEnum.REST)) {

            BigDecimal sumOfAllCostDistributionItemsWithoutRest = new BigDecimal(0);
            for (CostDistributionItem tmpCostDistributionItem : costDistributionItemsWithoutCurrentItem) {
                if (!tmpCostDistributionItem.getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.REST)){
                    sumOfAllCostDistributionItemsWithoutRest.add(calculateAmountForCostDistributionItem(tmpCostDistributionItem, costDistributionItemsWithoutCurrentItem, sum));
                }
            }

            BigDecimal restSum = sum.subtract(sumOfAllCostDistributionItemsWithoutRest);
            calculateAmountForValue = restSum;
        }

        return calculateAmountForValue.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculatePercentForValue(CostDistributionItemTypeEnum costDistributionItemTypeOfValue, BigDecimal value, List<CostDistributionItem> costDistributionItemsWithoutCurrentItem, BigDecimal sum){
        BigDecimal resultPercent = new BigDecimal(0);

        if (costDistributionItemTypeOfValue.equals(CostDistributionItemTypeEnum.QUOTA)){
            BigDecimal amountForQuota = calculateAmountForValue(CostDistributionItemTypeEnum.QUOTA, value, costDistributionItemsWithoutCurrentItem, sum);
            BigDecimal percent = amountForQuota.divide(sum, 30, BigDecimal.ROUND_HALF_UP);
            resultPercent = percent;
        }else if (costDistributionItemTypeOfValue.equals(CostDistributionItemTypeEnum.FIXED_AMOUNT)){
            BigDecimal percent = value.divide(sum, 2, BigDecimal.ROUND_HALF_UP);
            resultPercent = percent;
        }else if (costDistributionItemTypeOfValue.equals(CostDistributionItemTypeEnum.PERCENT)){
            resultPercent = value;
        }

        return resultPercent.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal getCountWithoutFixed(List<CostDistributionItem> costDistributionItems){
        BigDecimal countWithoutFixed = new BigDecimal(0);
        for (CostDistributionItem tmpCostDistributionItem1 : costDistributionItems) {
            if (tmpCostDistributionItem1.getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.QUOTA)){
                countWithoutFixed = countWithoutFixed.add(new BigDecimal(1));
            }
        }

        return countWithoutFixed;
    }

    public static BigDecimal getCountOfFixedAmount(List<CostDistributionItem> costDistributionItems){
        BigDecimal getCountOfFixed = new BigDecimal(0);
        for (CostDistributionItem tmpCostDistributionItem1 : costDistributionItems) {
            if (tmpCostDistributionItem1.getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.FIXED_AMOUNT)){
                getCountOfFixed = getCountOfFixed.add(new BigDecimal(1));
            }
        }

        return getCountOfFixed;
    }

    public static BigDecimal getCountOfPercent(List<CostDistributionItem> costDistributionItems){
        BigDecimal getCountOfPercent = new BigDecimal(0);
        for (CostDistributionItem tmpCostDistributionItem1 : costDistributionItems) {
            if (tmpCostDistributionItem1.getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.PERCENT)){
                getCountOfPercent = getCountOfPercent.add(new BigDecimal(1));
            }
        }

        return getCountOfPercent;
    }

    public static BigDecimal calculateAmountForCostDistributionItem(CostDistributionItem costDistributionItem, List<CostDistributionItem> costDistributionItems, BigDecimal sum){

        List<CostDistributionItem> costDistributionItemsWithoutCurrentItem = new ArrayList<CostDistributionItem>();
        costDistributionItemsWithoutCurrentItem.addAll(costDistributionItems);
        costDistributionItemsWithoutCurrentItem.remove(costDistributionItem);

        return calculateAmountForValue(costDistributionItem.getCostDistributionItemTypeEnum(), costDistributionItem.getValue(), costDistributionItemsWithoutCurrentItem, sum).setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean isCostDistributionComplete(List<CostDistributionItem> costDistributionItems, BigDecimal sum){

        if (costDistributionItems.size() == 0){
            return false;
        }

        BigDecimal countWeightedWithoutFixed = getCountWeightedWithoutFixed(costDistributionItems);
        BigDecimal countWithoutFixed = getCountWithoutFixed(costDistributionItems);
        BigDecimal countOfFixed = getCountOfFixedAmount(costDistributionItems);
        BigDecimal countOfPercent = getCountOfPercent(costDistributionItems);

        if (countWithoutFixed.compareTo(new BigDecimal(0)) == 1){
            return countWithoutFixed.compareTo(countWeightedWithoutFixed) == 0;
        }else{
            if (countOfFixed.compareTo(new BigDecimal(0)) == 0 && countOfPercent.compareTo(new BigDecimal(0)) == 1){
                BigDecimal sumOfPercent = new BigDecimal(0);
                for (CostDistributionItem  tmpCostDistributionItem: costDistributionItems) {
                    sumOfPercent = sumOfPercent.add(tmpCostDistributionItem.getValue());
                }

                return sumOfPercent.compareTo(new BigDecimal(1)) == 0;
            }else if (countOfFixed.compareTo(new BigDecimal(0)) == 1 && countOfPercent.compareTo(new BigDecimal(0)) == 0){
                BigDecimal sumOfFixedAmount = new BigDecimal(0);
                for (CostDistributionItem  tmpCostDistributionItem: costDistributionItems) {
                    sumOfFixedAmount = sumOfFixedAmount.add(tmpCostDistributionItem.getValue());
                }
                return sumOfFixedAmount.compareTo(sum) == 0;
            }else if (countOfFixed.compareTo(new BigDecimal(0)) == 1 && countOfPercent.compareTo(new BigDecimal(0)) == 1){
                BigDecimal sumOfMixed = new BigDecimal(0);
                for (CostDistributionItem  tmpCostDistributionItem: costDistributionItems) {
                    sumOfMixed = calculateAmountForCostDistributionItem(tmpCostDistributionItem, costDistributionItems, sum);
                }
                return sumOfMixed.compareTo(sum) == 0;
            }
        }
        return true;
    }

    public static BigDecimal calculateAmountForCostDistributionItemPrecise(CostDistributionItem costDistributionItem, List<CostDistributionItem> costDistributionItems, BigDecimal sum){

        BigDecimal resultAmount;

        List<CostDistributionItem> costDistributionItemsWithoutCurrentItem = new ArrayList<CostDistributionItem>();
        costDistributionItemsWithoutCurrentItem.addAll(costDistributionItems);
        costDistributionItemsWithoutCurrentItem.remove(costDistributionItem);

        if (isCostDistributionComplete(costDistributionItems, sum)){
            Integer countCostDistributionItemsIncludeQuota = 0;
            Integer counter = 0;
            Integer costDistributionPosition = -1;
            List<CostDistributionItem> costDistributionItemQuotaList = new ArrayList<CostDistributionItem>();

            for (CostDistributionItem  tmpCostDistributionItem1: costDistributionItems) {
                if (!tmpCostDistributionItem1.getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.FIXED_AMOUNT)){

                    countCostDistributionItemsIncludeQuota = countCostDistributionItemsIncludeQuota + 1;

                    if (tmpCostDistributionItem1.equals(costDistributionItem)){
                        costDistributionPosition=counter;
                    }

                    costDistributionItemQuotaList.add(tmpCostDistributionItem1);

                    counter++;
                }
            }

            if (countCostDistributionItemsIncludeQuota > 1){
                BigDecimal calculatedRestSum = getCalculatedRestSum(sum, costDistributionItems);
                resultAmount = calculateAmountForValue(costDistributionItem.getCostDistributionItemTypeEnum(), costDistributionItem.getValue(), costDistributionItemsWithoutCurrentItem, sum).setScale(2, RoundingMode.HALF_UP);

                if (calculatedRestSum.compareTo(new BigDecimal(0)) == -1 || calculatedRestSum.compareTo(new BigDecimal(0)) == 1){

                    BigDecimal rest = calculatedRestSum;

                    BigDecimal corrector = new BigDecimal(1);

                    if (rest.compareTo(new BigDecimal(0)) == -1){
                        corrector = new BigDecimal(-1);
                    }

                    BigDecimal restAmountForOneCostDistributionItem = calculatedRestSum.multiply(corrector).multiply(new BigDecimal(100)).divide(new BigDecimal(countCostDistributionItemsIncludeQuota),50, RoundingMode.HALF_UP).setScale(0, BigDecimal.ROUND_DOWN);
                    BigDecimal remainder = calculatedRestSum.multiply(corrector).multiply(new BigDecimal(100)).remainder(new BigDecimal(countCostDistributionItemsIncludeQuota));

                    restAmountForOneCostDistributionItem = restAmountForOneCostDistributionItem.multiply(corrector);
                    remainder = remainder.multiply(corrector);

                    resultAmount = resultAmount.add(restAmountForOneCostDistributionItem.divide(new BigDecimal(100), 2, RoundingMode.HALF_UP));

                    for(int i = 0; i < remainder.multiply(corrector).intValue(); i++){
                        if (i == costDistributionPosition){
                            resultAmount = resultAmount.add(new BigDecimal(0.01).multiply(corrector));
                        }
                    }

                    resultAmount = resultAmount.setScale(2, RoundingMode.HALF_UP);

                }

            }else{
                resultAmount = calculateAmountForValue(costDistributionItem.getCostDistributionItemTypeEnum(), costDistributionItem.getValue(), costDistributionItemsWithoutCurrentItem, sum).setScale(2, RoundingMode.HALF_UP);
            }
        }else{
            resultAmount = calculateAmountForValue(costDistributionItem.getCostDistributionItemTypeEnum(), costDistributionItem.getValue(), costDistributionItemsWithoutCurrentItem, sum).setScale(2, RoundingMode.HALF_UP);
        }

        return resultAmount;
    }

    public static CostDistributionItem prepareItemForCostDistribution(CostDistributionItem costDistributionItem, CostDistribution costDistribution){
        CostDistributionItem costDistributionItemNew = new CostDistributionItem();
        costDistributionItemNew.setPaymentPersonTypeEnum(costDistributionItem.getPaymentPersonTypeEnum());
        costDistributionItemNew.setValue(costDistributionItem.getValue());
        costDistributionItemNew.setPayerId(costDistributionItem.getPayerId());
        if (costDistribution != null){
            costDistributionItemNew.setCostDistributionId(costDistribution.getCostDistributionId());
        }
        costDistributionItemNew.setCostDistributionItemTypeEnum(costDistributionItem.getCostDistributionItemTypeEnum());
        costDistributionItemNew.setPosition(costDistributionItem.getPosition());

        return costDistributionItemNew;
    }

    public static BigDecimal invoiceCostForPaymentPerson(Context context, Invoice invoice, IPaymentPerson paymentPerson){
        return invoiceCostForPaymentPerson(context, invoice, paymentPerson, null, new HashMap<String, List<String>>());
    }

    public static BigDecimal invoiceCostForPaymentPerson(Context context, Invoice invoice, IPaymentPerson paymentPerson, List<CostDistributionItem> costDistributionItems, Map<String, List<String>> userContactIdsMap){

        if (paymentPerson == null){
            return new BigDecimal(0);
        }

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);
        if (costDistributionItems == null){
            costDistributionItems = mainDatabaseHandler.findCostDistributionItems(MainDatabaseHandler.VAR_INVOICE_ID, invoice.getInvoiceId().toString());
        }

        BigDecimal resultInvoiceCost = null;

        UUID creator = invoice.getCreatedById();
        MainFunctionEnum mainFunctionEnum = null;
        if (invoice.getPayerId() != null && invoice.getPayerId().equals(creator)){
            mainFunctionEnum = MainFunctionEnum.EXPENSE;
        }else if (invoice.getPaymentRecipientId() != null && invoice.getPaymentRecipientId().equals(creator)){
            mainFunctionEnum = MainFunctionEnum.INCOME;
        }

        String paymentPersonUser = null;
        if (PaymentPersonTypeEnum.USER.equals(paymentPerson.getPaymentPersonEnum())){
            paymentPersonUser = paymentPerson.getPaymentPersonId().toString();
        }else if (PaymentPersonTypeEnum.CONTACT.equals(paymentPerson.getPaymentPersonEnum())){

            UserContact paymentPersonUserTmp = null;
            if (paymentPerson instanceof PaymentPerson){
                List<UserContact> foundAppUsers = mainDatabaseHandler.findUserContacts(MainDatabaseHandler.VAR_USER_CONTACT_ID, paymentPerson.getPaymentPersonId().toString());
                if (!foundAppUsers.isEmpty()){
                    paymentPersonUserTmp = foundAppUsers.get(0);
                }
            }else{
                paymentPersonUserTmp = (UserContact) paymentPerson;
            }

            if (paymentPersonUserTmp.getAppUserContactId() != null){
                paymentPersonUser = paymentPersonUserTmp.getAppUserContactId().toString();
            }
        }

        if (paymentPersonUser != null){

            List<String> userContactIds = null;
            if (userContactIdsMap != null){
                userContactIds = userContactIdsMap.get(paymentPersonUser);
            }

            if (userContactIds == null){

                userContactIds = new ArrayList<>();

                SqlBuilder sqlBuilder3 = new SqlBuilder(MainDatabaseHandler.TABLE_USER_CONTACT);
                sqlBuilder3.isEqual(MainDatabaseHandler.VAR_APP_USER_CONTACT_ID, paymentPersonUser);
                List<UserContact> foundUserContacts = mainDatabaseHandler.findUserContactsSql(sqlBuilder3);
                for (UserContact foundUserContact : foundUserContacts) {
                    userContactIds.add(foundUserContact.getUserContactId().toString());
                }
                userContactIdsMap.put(paymentPersonUser, userContactIds);
            }

            for (CostDistributionItem costDistributionItem : costDistributionItems) {
                if (costDistributionItem.getPaymentPersonTypeEnum() != null){
                    if (PaymentPersonTypeEnum.CONTACT.equals(costDistributionItem.getPaymentPersonTypeEnum()) &&
                            userContactIds.contains(costDistributionItem.getPayerId().toString())){
                        resultInvoiceCost = costDistributionItem.getMoneyValue();
                        break;
                    }else if (PaymentPersonTypeEnum.USER.equals(costDistributionItem.getPaymentPersonTypeEnum()) &&
                            paymentPersonUser.equals(costDistributionItem.getPayerId().toString())){
                        resultInvoiceCost = costDistributionItem.getMoneyValue();
                        break;
                    }
                }
            }

            if (resultInvoiceCost != null){
                if (MainFunctionEnum.INCOME.equals(mainFunctionEnum)){
                    return resultInvoiceCost;
                }else{
                    return resultInvoiceCost.multiply(new BigDecimal(-1));
                }
            }

            BigDecimal sumOfInvoice = invoice.getSumOfInvoice() != null ? invoice.getSumOfInvoice() : new BigDecimal(0);

            if (MainFunctionEnum.INCOME.equals(mainFunctionEnum)){
                if (invoice.getPayerId() != null && (invoice.getPayerId().toString().equals(paymentPersonUser) || userContactIds.contains(invoice.getPayerId().toString()))){
                    return sumOfInvoice.multiply(new BigDecimal(-1));
                }
            }else{
                if (invoice.getPaymentRecipientId() != null && (invoice.getPaymentRecipientId().toString().equals(paymentPersonUser) || userContactIds.contains(invoice.getPaymentRecipientId().toString()))){
                    return sumOfInvoice;
                }
            }

        }else if (PaymentPersonTypeEnum.CONTACT.equals(paymentPerson.getPaymentPersonEnum())){

            for (CostDistributionItem costDistributionItem : costDistributionItems) {
                if (costDistributionItem.getPaymentPersonTypeEnum() != null){
                    if (PaymentPersonTypeEnum.CONTACT.equals(costDistributionItem.getPaymentPersonTypeEnum()) &&
                            paymentPerson.getPaymentPersonId().equals(costDistributionItem.getPayerId())){
                        resultInvoiceCost = costDistributionItem.getMoneyValue();
                        break;
                    }
                }
            }

            if (resultInvoiceCost != null){
                if (MainFunctionEnum.INCOME.equals(mainFunctionEnum)){
                    return resultInvoiceCost;
                }else{
                    return resultInvoiceCost.multiply(new BigDecimal(-1));
                }
            }

            BigDecimal sumOfInvoice = invoice.getSumOfInvoice() != null ? invoice.getSumOfInvoice() : new BigDecimal(0);

            if (MainFunctionEnum.INCOME.equals(mainFunctionEnum)){
                if (invoice.getPayerId() != null && invoice.getPayerId().equals(paymentPerson.getPaymentPersonId())){
                    return sumOfInvoice.multiply(new BigDecimal(-1));
                }
            }else{
                if (invoice.getPaymentRecipientId() != null && invoice.getPaymentRecipientId().equals(paymentPerson.getPaymentPersonId())){
                    return sumOfInvoice;
                }
            }
        }

        return new BigDecimal(0);
    }

    public static BigDecimal getSumOfAllFixedCostDistributionItems(List<CostDistributionItem> allCostDistributionItems){
        BigDecimal amountOfFixed = new BigDecimal(0);
        for (CostDistributionItem tmpCostDistributionItem1 : allCostDistributionItems) {
            if (tmpCostDistributionItem1.getCostDistributionItemTypeEnum().equals(CostDistributionItemTypeEnum.FIXED_AMOUNT)){
                amountOfFixed = tmpCostDistributionItem1.getMoneyValue();
            }
        }

        return amountOfFixed;
    }

    public static boolean areCostDistributionItemListsEqual(List<CostDistributionItem> costDistributionItems1, List<CostDistributionItem> costDistributionItems2){

        for (CostDistributionItem costDistributionItem1 : costDistributionItems1) {

            boolean equalCostDistributionItemFound = false;
            for (CostDistributionItem costDistributionItem2 : costDistributionItems2) {
                if (areCostDistributionItemsEqual(costDistributionItem1, costDistributionItem2)){
                    equalCostDistributionItemFound = true;
                    break;
                }
            }

            if (!equalCostDistributionItemFound){
                return false;
            }

        }

        return true;
    }

    public static List<IPaymentItem> paymentItemsForCorrectionMode(Context context, Invoice currentInvoice, List<CostDistributionItem> costDistributionItems, boolean isCheck){

        List<IPaymentItem> paymentItems = new ArrayList<>();
        if (currentInvoice != null){

            List<String> userContactIds = LoginUserHelper.idsOfUserContactsWithCurrentUser(context);

            if (isCheck){
                if ((currentInvoice.getCorrectionStatus() == null || currentInvoice.getCorrectionStatus().equals(CorrectionStatus.CHECK)  || currentInvoice.getCorrectionStatus().equals(CorrectionStatus.PROBLEM))
                        && ( (currentInvoice.getPayerId() != null && userContactIds.contains(currentInvoice.getPayerId().toString())) ||  (currentInvoice.getPaymentRecipientId() != null && userContactIds.contains(currentInvoice.getPaymentRecipientId().toString())) )){
                    paymentItems.add(currentInvoice);
                }

                for (CostDistributionItem costDistributionItem : costDistributionItems) {
                    if ((costDistributionItem.getCorrectionStatus() == null || costDistributionItem.getCorrectionStatus().equals(CorrectionStatus.CHECK)  || costDistributionItem.getCorrectionStatus().equals(CorrectionStatus.PROBLEM))
                            && (costDistributionItem.getPayerId() != null && userContactIds.contains(costDistributionItem.getPayerId().toString()))){
                        paymentItems.add(costDistributionItem);
                    }
                }
            }else{
                if ((currentInvoice.getCorrectionStatus() != null && currentInvoice.getCorrectionStatus().equals(CorrectionStatus.READY))
                        && ( (currentInvoice.getPayerId() != null && userContactIds.contains(currentInvoice.getPayerId().toString())) ||  (currentInvoice.getPaymentRecipientId() != null && userContactIds.contains(currentInvoice.getPaymentRecipientId().toString())) )){
                    paymentItems.add(currentInvoice);
                }

                for (CostDistributionItem costDistributionItem : costDistributionItems) {
                    if ((costDistributionItem.getCorrectionStatus() != null && costDistributionItem.getCorrectionStatus().equals(CorrectionStatus.READY))){
                        paymentItems.add(costDistributionItem);
                    }
                }
            }


        }

        return paymentItems;
    }

    public static boolean isCreatedUserCheckMode(Context context, Invoice currentInvoice){
        AppUser currentUser = LoginUserHelper.currentLoggedInUser(context);

        if (currentUser.getAppUserId().equals(currentInvoice.getCreatedById())){
            if (InvoiceStatusEnum.CHECK.equals(currentInvoice.getInvoiceStatusEnum())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCreatedUserReadyMode(Context context, Invoice currentInvoice){
        AppUser currentUser = LoginUserHelper.currentLoggedInUser(context);

        if (currentUser.getAppUserId().equals(currentInvoice.getCreatedById())){
            if (InvoiceStatusEnum.READY.equals(currentInvoice.getInvoiceStatusEnum())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isExternalUserCheckMode(Context context, Invoice currentInvoice, List<CostDistributionItem> costDistributionItems){

        List<String> userContactIds = LoginUserHelper.idsOfUserContactsWithCurrentUser(context);

        if ((currentInvoice.getCorrectionStatus() == null || currentInvoice.getCorrectionStatus().equals(CorrectionStatus.CHECK)  || currentInvoice.getCorrectionStatus().equals(CorrectionStatus.PROBLEM))
                && ( (currentInvoice.getPayerId() != null && userContactIds.contains(currentInvoice.getPayerId().toString())) ||  (currentInvoice.getPaymentRecipientId() != null && userContactIds.contains(currentInvoice.getPaymentRecipientId().toString())) )){
            return true;
        }

        for (CostDistributionItem costDistributionItem : costDistributionItems) {
            if ((costDistributionItem.getCorrectionStatus() == null || costDistributionItem.getCorrectionStatus().equals(CorrectionStatus.CHECK)  || costDistributionItem.getCorrectionStatus().equals(CorrectionStatus.PROBLEM))
                    && (costDistributionItem.getPayerId() != null && userContactIds.contains(costDistributionItem.getPayerId().toString()))){
                return true;
            }
        }
        return false;
    }

    public static boolean isExternalUserReadyMode(Context context, Invoice currentInvoice, List<CostDistributionItem> costDistributionItems){

        List<String> userContactIds = LoginUserHelper.idsOfUserContactsWithCurrentUser(context);

        if ((currentInvoice.getCorrectionStatus() != null && currentInvoice.getCorrectionStatus().equals(CorrectionStatus.READY))
                && ( (currentInvoice.getPayerId() != null && userContactIds.contains(currentInvoice.getPayerId().toString())) ||  (currentInvoice.getPaymentRecipientId() != null && userContactIds.contains(currentInvoice.getPaymentRecipientId().toString())) )){
            return true;
        }

        for (CostDistributionItem costDistributionItem : costDistributionItems) {
            if ((costDistributionItem.getCorrectionStatus() != null && costDistributionItem.getCorrectionStatus().equals(CorrectionStatus.READY))
                    && ( (costDistributionItem.getPayerId() != null && userContactIds.contains(costDistributionItem.getPayerId().toString())))){
                return true;
            }
        }
        return false;
    }

    public static boolean areCostDistributionItemsEqual(CostDistributionItem costDistributionItem1, CostDistributionItem costDistributionItem2){
        if (costDistributionItem1.getCostDistributionItemTypeEnum() == null && costDistributionItem2.getCostDistributionItemTypeEnum() != null){
            return false;
        }else if (costDistributionItem1.getCostDistributionItemTypeEnum() != null && costDistributionItem2.getCostDistributionItemTypeEnum() == null){
            return false;
        }else if (costDistributionItem1.getCostDistributionItemTypeEnum() != null && costDistributionItem2.getCostDistributionItemTypeEnum() != null && !costDistributionItem1.getCostDistributionItemTypeEnum().equals(costDistributionItem2.getCostDistributionItemTypeEnum())){
            return false;
        }

        if (costDistributionItem1.getPaymentPersonTypeEnum() == null && costDistributionItem2.getPaymentPersonTypeEnum() != null){
            return false;
        }else if (costDistributionItem1.getPaymentPersonTypeEnum() != null && costDistributionItem2.getPaymentPersonTypeEnum() == null){
            return false;
        }else if (costDistributionItem1.getPaymentPersonTypeEnum() != null && costDistributionItem2.getPaymentPersonTypeEnum() != null && !costDistributionItem1.getPaymentPersonTypeEnum().equals(costDistributionItem2.getPaymentPersonTypeEnum())){
            return false;
        }

        if (costDistributionItem1.getPayerId() == null && costDistributionItem2.getPayerId() != null){
            return false;
        }else if (costDistributionItem1.getPayerId() != null && costDistributionItem2.getPayerId() == null){
            return false;
        }else if (costDistributionItem1.getPayerId() != null && costDistributionItem2.getPayerId() != null && !costDistributionItem1.getPayerId().equals(costDistributionItem2.getPayerId())){
            return false;
        }

        if (costDistributionItem1.getValue() == null && costDistributionItem2.getValue() != null){
            return false;
        }else if (costDistributionItem1.getValue() != null && costDistributionItem2.getValue() == null){
            return false;
        }else if (costDistributionItem1.getValue() != null && costDistributionItem2.getValue() != null && !costDistributionItem1.getValue().equals(costDistributionItem2.getValue())){
            return false;
        }

        return true;
    }
}
