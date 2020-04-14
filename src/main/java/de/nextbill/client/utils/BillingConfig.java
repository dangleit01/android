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

package de.nextbill.client.utils;

import java.util.Date;

import de.nextbill.client.enums.PaymentPersonTypeEnum;


public class BillingConfig {
    private Date startDate;
    private Date endDate;

    private boolean usePaidInvoices;
    private boolean markAsPaid;

    private String userPayer;
    private String userSelection;

    private PaymentPersonTypeEnum userPayerPaymentPersonTypeEnum;
    private PaymentPersonTypeEnum userSelectionPaymentPersonTypeEnum;

    private boolean sendMailInvoicePayer;
    private boolean sendMailCostPayer;

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public boolean isUsePaidInvoices() {
        return usePaidInvoices;
    }

    public void setUsePaidInvoices(boolean usePaidInvoices) {
        this.usePaidInvoices = usePaidInvoices;
    }

    public boolean isMarkAsPaid() {
        return markAsPaid;
    }

    public void setMarkAsPaid(boolean markAsPaid) {
        this.markAsPaid = markAsPaid;
    }

    public String getUserPayer() {
        return userPayer;
    }

    public void setUserPayer(String userPayer) {
        this.userPayer = userPayer;
    }

    public String getUserSelection() {
        return userSelection;
    }

    public void setUserSelection(String userSelection) {
        this.userSelection = userSelection;
    }

    public PaymentPersonTypeEnum getUserPayerPaymentPersonTypeEnum() {
        return userPayerPaymentPersonTypeEnum;
    }

    public void setUserPayerPaymentPersonTypeEnum(PaymentPersonTypeEnum userPayerPaymentPersonTypeEnum) {
        this.userPayerPaymentPersonTypeEnum = userPayerPaymentPersonTypeEnum;
    }

    public PaymentPersonTypeEnum getUserSelectionPaymentPersonTypeEnum() {
        return userSelectionPaymentPersonTypeEnum;
    }

    public void setUserSelectionPaymentPersonTypeEnum(PaymentPersonTypeEnum userSelectionPaymentPersonTypeEnum) {
        this.userSelectionPaymentPersonTypeEnum = userSelectionPaymentPersonTypeEnum;
    }

    public boolean isSendMailCostPayer() {
        return sendMailCostPayer;
    }

    public void setSendMailCostPayer(boolean sendMailCostPayer) {
        this.sendMailCostPayer = sendMailCostPayer;
    }

    public boolean isSendMailInvoicePayer() {
        return sendMailInvoicePayer;
    }

    public void setSendMailInvoicePayer(boolean sendMailInvoicePayer) {
        this.sendMailInvoicePayer = sendMailInvoicePayer;
    }
}
