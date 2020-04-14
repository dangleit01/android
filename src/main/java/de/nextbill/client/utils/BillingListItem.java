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

import java.math.BigDecimal;
import java.util.List;

import de.nextbill.client.model.InvoiceCategory;
import de.nextbill.client.model.PaymentPerson;


public class BillingListItem {
    private PaymentPerson invoicePayer;
    private PaymentPerson costPayer;

    private BigDecimal sumToBePaid;

    private BigDecimal costsFromInvoicePayer;
    private BigDecimal costsFromCostPayer;

    private List<InvoiceCategory> invoiceCategoriesOfInvoicePayer;
    private List<InvoiceCategory> invoiceCategoriesOfCostPayer;

    public PaymentPerson getInvoicePayer() {
        return invoicePayer;
    }

    public void setInvoicePayer(PaymentPerson invoicePayer) {
        this.invoicePayer = invoicePayer;
    }

    public PaymentPerson getCostPayer() {
        return costPayer;
    }

    public void setCostPayer(PaymentPerson costPayer) {
        this.costPayer = costPayer;
    }

    public BigDecimal getSumToBePaid() {
        return sumToBePaid;
    }

    public void setSumToBePaid(BigDecimal sumToBePaid) {
        this.sumToBePaid = sumToBePaid;
    }

    public BigDecimal getCostsFromInvoicePayer() {
        return costsFromInvoicePayer;
    }

    public void setCostsFromInvoicePayer(BigDecimal costsFromInvoicePayer) {
        this.costsFromInvoicePayer = costsFromInvoicePayer;
    }

    public BigDecimal getCostsFromCostPayer() {
        return costsFromCostPayer;
    }

    public void setCostsFromCostPayer(BigDecimal costsFromCostPayer) {
        this.costsFromCostPayer = costsFromCostPayer;
    }

    public List<InvoiceCategory> getInvoiceCategoriesOfInvoicePayer() {
        return invoiceCategoriesOfInvoicePayer;
    }

    public void setInvoiceCategoriesOfInvoicePayer(List<InvoiceCategory> invoiceCategoriesOfInvoicePayer) {
        this.invoiceCategoriesOfInvoicePayer = invoiceCategoriesOfInvoicePayer;
    }

    public List<InvoiceCategory> getInvoiceCategoriesOfCostPayer() {
        return invoiceCategoriesOfCostPayer;
    }

    public void setInvoiceCategoriesOfCostPayer(List<InvoiceCategory> invoiceCategoriesOfCostPayer) {
        this.invoiceCategoriesOfCostPayer = invoiceCategoriesOfCostPayer;
    }
}
