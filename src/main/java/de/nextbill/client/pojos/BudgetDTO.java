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

package de.nextbill.client.pojos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import de.nextbill.client.enums.BudgetRepetitionType;
import de.nextbill.client.enums.PaymentTypeEnum;
import de.nextbill.client.model.InvoiceCategory;
import de.nextbill.client.model.PaymentPersonDTO;

public class BudgetDTO {

    private UUID budgetId;

    private String name;

    private BigDecimal sum;

    private BigDecimal currentSum;

    private String filterText;

    private BudgetRepetitionType budgetRepetitionType;

    private Boolean specialType;

    private String remarks;

    private PaymentTypeEnum paymentTypeEnum;

    private List<PaymentPersonDTO> payerDTOS = new ArrayList<>();

    private List<InvoiceCategory> invoiceCategoryDTOS = new ArrayList<>();

    private BigDecimal lastSum;

    private Date lastExceeding;

    private BigDecimal lastExceedingSum;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(UUID budgetId) {
        this.budgetId = budgetId;
    }

    public BigDecimal getSum() {
        return sum;
    }

    public void setSum(BigDecimal sum) {
        this.sum = sum;
    }

    public BigDecimal getCurrentSum() {
        return currentSum;
    }

    public void setCurrentSum(BigDecimal currentSum) {
        this.currentSum = currentSum;
    }

    public String getFilterText() {
        return filterText;
    }

    public void setFilterText(String filterText) {
        this.filterText = filterText;
    }

    public BudgetRepetitionType getBudgetRepetitionType() {
        return budgetRepetitionType;
    }

    public void setBudgetRepetitionType(BudgetRepetitionType budgetRepetitionType) {
        this.budgetRepetitionType = budgetRepetitionType;
    }

    public Boolean getSpecialType() {
        return specialType;
    }

    public void setSpecialType(Boolean specialType) {
        this.specialType = specialType;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public PaymentTypeEnum getPaymentTypeEnum() {
        return paymentTypeEnum;
    }

    public void setPaymentTypeEnum(PaymentTypeEnum paymentTypeEnum) {
        this.paymentTypeEnum = paymentTypeEnum;
    }

    public List<PaymentPersonDTO> getPayerDTOS() {
        return payerDTOS;
    }

    public void setPayerDTOS(List<PaymentPersonDTO> payerDTOS) {
        this.payerDTOS = payerDTOS;
    }

    public List<InvoiceCategory> getInvoiceCategoryDTOS() {
        return invoiceCategoryDTOS;
    }

    public void setInvoiceCategoryDTOS(List<InvoiceCategory> invoiceCategoryDTOS) {
        this.invoiceCategoryDTOS = invoiceCategoryDTOS;
    }

    public BigDecimal getLastSum() {
        return lastSum;
    }

    public void setLastSum(BigDecimal lastSum) {
        this.lastSum = lastSum;
    }

    public Date getLastExceeding() {
        return lastExceeding;
    }

    public void setLastExceeding(Date lastExceeding) {
        this.lastExceeding = lastExceeding;
    }

    public BigDecimal getLastExceedingSum() {
        return lastExceedingSum;
    }

    public void setLastExceedingSum(BigDecimal lastExceedingSum) {
        this.lastExceedingSum = lastExceedingSum;
    }
}
