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

package de.nextbill.client.comparators;

import java.util.Calendar;
import java.util.Date;

import de.nextbill.client.model.CostDistributionItem;
import de.nextbill.client.model.Invoice;


public class EntityComparatorService {

    public static boolean invoicesEqual(Invoice o1, Invoice o2) {
        if (o1 == o2) return true;
        if (o2 == null || o1.getClass() != o2.getClass()) return false;

        Invoice invoice = o1;
        Invoice invoice2 = o2;

        Date invoice1Date = correctDate(invoice.getDateOfInvoice());
        Date invoice2Date = correctDate(invoice2.getDateOfInvoice());

        String remarks1 = correctRemarks(invoice.getRemarks());
        String remarks2 = correctRemarks(invoice2.getRemarks());

        if (invoice2.getCreatedById() != null ? !invoice2.getCreatedById().equals(invoice.getCreatedById()) : invoice.getCreatedById() != null)
            return false;
        if (invoice2.getStandingOrderInvoiceTemplateId() != null ? !invoice2.getStandingOrderInvoiceTemplateId().equals(invoice.getStandingOrderInvoiceTemplateId()) : invoice.getStandingOrderInvoiceTemplateId() != null)
            return false;
        if (invoice2.getInvoiceId() != null ? !invoice2.getInvoiceId().equals(invoice.getInvoiceId()) : invoice.getInvoiceId() != null)
            return false;
        if (invoice2.getInvoiceStatusEnum() != null ? !invoice2.getInvoiceStatusEnum().equals(invoice.getInvoiceStatusEnum()) : invoice.getInvoiceStatusEnum() != null) return false;
        if (invoice2Date != null ? !invoice2Date.equals(invoice1Date) : invoice1Date != null)
            return false;
        if (invoice2.getStandingOrderStartDate() != null ? !invoice2.getStandingOrderStartDate().equals(invoice.getStandingOrderStartDate()) : invoice.getStandingOrderStartDate() != null)
            return false;
        if (invoice2.getSumOfInvoice() != null ? invoice2.getSumOfInvoice().compareTo(invoice.getSumOfInvoice()) != 0 : invoice.getSumOfInvoice() != null)
            return false;
        if (invoice2.getPayerId() != null ? !invoice2.getPayerId().equals(invoice.getPayerId()) : invoice.getPayerId() != null)
            return false;
        if (invoice2.getPayerTypeEnum() != null ? !invoice2.getPayerTypeEnum().equals(invoice.getPayerTypeEnum()) : invoice.getPayerTypeEnum() != null) return false;
        if (invoice2.getPaymentRecipientId() != null ? !invoice2.getPaymentRecipientId().equals(invoice.getPaymentRecipientId()) : invoice.getPaymentRecipientId() != null)
            return false;
        if (invoice2.getPaymentRecipientTypeEnum() != null ? !invoice2.getPaymentRecipientTypeEnum().equals(invoice.getPaymentRecipientTypeEnum()) : invoice.getPaymentRecipientTypeEnum() != null) return false;
        if (invoice2.getInvoiceCategoryDTO() != null ? !invoice2.getInvoiceCategoryDTO().getInvoiceCategoryId().equals(invoice.getInvoiceCategoryDTO().getInvoiceCategoryId()) : invoice.getInvoiceCategoryDTO() != null)
            return false;
        if (invoice2.getPaymentTypeEnum() != null ? !invoice2.getPaymentTypeEnum().equals(invoice.getPaymentTypeEnum()) : invoice.getPaymentTypeEnum() != null) return false;

        if (invoice2.getInvoiceImageId() != null ? !invoice2.getInvoiceImageId().equals(invoice.getInvoiceImageId()) : invoice.getInvoiceImageId() != null)
            return false;
        if (invoice2.getSpecialType() != null ? !invoice2.getSpecialType().equals(invoice.getSpecialType()) : invoice.getSpecialType() != null)
            return false;
        if (remarks2 != null ? !remarks2.equals(remarks1) : remarks1 != null)
            return false;
        if (invoice2.getRepetitionTypeEnum() != null ? !invoice2.getRepetitionTypeEnum().equals(invoice.getRepetitionTypeEnum()) : invoice.getRepetitionTypeEnum() != null)
            return false;
        if (invoice2.getCorrectionStatus() != null ? !invoice2.getCorrectionStatus().equals(invoice.getCorrectionStatus()) : invoice.getCorrectionStatus() != null)
            return false;
        if (invoice2.getCostPaid() != null ? invoice2.getCostPaid().compareTo(invoice.getCostPaid()) != 0 : invoice.getCostPaid() != null)
            return false;

        return true;
    }

    public static Date correctDate(Date date){
        if (date == null){
            return null;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static String correctRemarks(String remark){
        if (remark == null || remark.equals("")){
            return null;
        }

        return remark;
    }

    public static boolean costDistributionItemsEqual(CostDistributionItem o1, CostDistributionItem o2) {
        if (o1 == o2) return true;
        if (o2 == null || o1.getClass() != o2.getClass()) return false;

        CostDistributionItem costDistributionItem1 = o1;
        CostDistributionItem costDistributionItem2 = o2;

        if (costDistributionItem1.getCostDistributionItemTypeEnum() != null ? !costDistributionItem1.getCostDistributionItemTypeEnum().equals(costDistributionItem2.getCostDistributionItemTypeEnum()) : costDistributionItem2.getCostDistributionItemTypeEnum() != null) return false;
        if (costDistributionItem1.getValue() != null ? costDistributionItem1.getValue().compareTo(costDistributionItem2.getValue()) != 0 : costDistributionItem2.getValue() != null) return false;
        if (costDistributionItem1.getMoneyValue() != null ? costDistributionItem1.getMoneyValue().compareTo(costDistributionItem2.getMoneyValue()) != 0 : costDistributionItem2.getMoneyValue() != null)
            return false;
        if (costDistributionItem1.getCostPaid() != null ? costDistributionItem1.getCostPaid().compareTo(costDistributionItem2.getCostPaid()) != 0 : costDistributionItem2.getCostPaid() != null)
            return false;
        if (costDistributionItem1.getPayerId() != null ? !costDistributionItem1.getPayerId().equals(costDistributionItem2.getPayerId()) : costDistributionItem2.getPayerId() != null) return false;
        if (costDistributionItem1.getPaymentPersonTypeEnum() != null ? !costDistributionItem1.getPaymentPersonTypeEnum().equals(costDistributionItem2.getPaymentPersonTypeEnum()) : costDistributionItem2.getPaymentPersonTypeEnum() != null) return false;
        if (costDistributionItem1.getPaymentPersonName() != null ? !costDistributionItem1.getPaymentPersonName().equals(costDistributionItem2.getPaymentPersonName()) : costDistributionItem2.getPaymentPersonName() != null)
            return false;
        if (costDistributionItem1.getPosition() != null ? !costDistributionItem1.getPosition().equals(costDistributionItem2.getPosition()) : costDistributionItem2.getPosition() != null)
            return false;
        if (costDistributionItem1.getCostDistributionItemId() != null ? !costDistributionItem1.getCostDistributionItemId().equals(costDistributionItem2.getCostDistributionItemId()) : costDistributionItem2.getCostDistributionItemId() != null)
            return false;
        if (costDistributionItem1.getInvoiceId() != null ? !costDistributionItem1.getInvoiceId().equals(costDistributionItem2.getInvoiceId()) : costDistributionItem2.getInvoiceId() != null)
            return false;
        if (costDistributionItem1.getIsTemporary() != null ? !costDistributionItem1.getIsTemporary().equals(costDistributionItem2.getIsTemporary()) : costDistributionItem2.getIsTemporary() != null)
            return false;
        if (costDistributionItem1.getCostDistributionId() != null ? !costDistributionItem1.getCostDistributionId().equals(costDistributionItem2.getCostDistributionId()) : costDistributionItem2.getCostDistributionId() != null)
            return false;
        if (costDistributionItem1.getCorrectionStatus() != null ? !costDistributionItem1.getCorrectionStatus().equals(costDistributionItem2.getCorrectionStatus()) : costDistributionItem2.getCorrectionStatus() != null)
            return false;
        if (!((costDistributionItem2.getRemarks() == null && costDistributionItem1.getRemarks() == null) || (costDistributionItem2.getRemarks() != null && costDistributionItem2.getRemarks().equals("") && costDistributionItem1.getRemarks() != null && costDistributionItem1.getRemarks().equals("")) || (costDistributionItem2.getRemarks() != null && costDistributionItem2.getRemarks().equals(costDistributionItem1.getRemarks()))))
            return false;

        return true;
    }

    public static Invoice copyInvoice(Invoice invoice){
        Invoice newInvoice = new Invoice();

        newInvoice.setId(invoice.getId());
        newInvoice.setCreatedById(invoice.getCreatedById());
        newInvoice.setStandingOrderInvoiceTemplateId(invoice.getStandingOrderInvoiceTemplateId());
        newInvoice.setStandingOrderStartDate(invoice.getStandingOrderStartDate());
        newInvoice.setInvoiceId(invoice.getInvoiceId());
        newInvoice.setInvoiceStatusEnum(invoice.getInvoiceStatusEnum());
        newInvoice.setDateOfInvoice(invoice.getDateOfInvoice());
        newInvoice.setPayerId(invoice.getPayerId());
        newInvoice.setPaymentRecipientId(invoice.getPaymentRecipientId());
        newInvoice.setInvoiceImageId(invoice.getInvoiceImageId());
        newInvoice.setSpecialType(invoice.getSpecialType());
        newInvoice.setRemarks(invoice.getRemarks());
        newInvoice.setInvoiceSource(invoice.getInvoiceSource());
        newInvoice.setPayerTypeEnum(invoice.getPayerTypeEnum());
        newInvoice.setPaymentTypeEnum(invoice.getPaymentTypeEnum());
        newInvoice.setRepetitionTypeEnum(invoice.getRepetitionTypeEnum());
        newInvoice.setInvoiceCategoryDTO(invoice.getInvoiceCategoryDTO());
        newInvoice.setPaymentRecipientTypeEnum(invoice.getPaymentRecipientTypeEnum());
        newInvoice.setSumOfInvoice(invoice.getSumOfInvoice());
        newInvoice.setCostPaid(invoice.getCostPaid());
        newInvoice.setCorrectionStatus(invoice.getCorrectionStatus());

        return newInvoice;
    }

    public static CostDistributionItem copyCostDistributionItem(CostDistributionItem costDistributionItem){
        CostDistributionItem newCostDistributionItem = new CostDistributionItem();

        newCostDistributionItem.setId(costDistributionItem.getId());
        newCostDistributionItem.setCostDistributionItemTypeEnum(costDistributionItem.getCostDistributionItemTypeEnum());
        newCostDistributionItem.setValue(costDistributionItem.getValue());
        newCostDistributionItem.setMoneyValue(costDistributionItem.getMoneyValue());
        newCostDistributionItem.setCostPaid(costDistributionItem.getCostPaid());
        newCostDistributionItem.setPayerId(costDistributionItem.getPayerId());
        newCostDistributionItem.setPaymentPersonTypeEnum(costDistributionItem.getPaymentPersonTypeEnum());
        newCostDistributionItem.setPaymentPersonName(costDistributionItem.getPaymentPersonName());
        newCostDistributionItem.setPosition(costDistributionItem.getPosition());
        newCostDistributionItem.setCostDistributionItemId(costDistributionItem.getCostDistributionItemId());
        newCostDistributionItem.setInvoiceId(costDistributionItem.getInvoiceId());
        newCostDistributionItem.setIsTemporary(costDistributionItem.getIsTemporary());
        newCostDistributionItem.setCostDistributionId(costDistributionItem.getCostDistributionId());
        newCostDistributionItem.setRemarks(costDistributionItem.getRemarks());
        newCostDistributionItem.setCorrectionStatus(costDistributionItem.getCorrectionStatus());

        return newCostDistributionItem;
    }
}
