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

package de.nextbill.client.model;

import android.content.Context;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.enums.CorrectionStatus;
import de.nextbill.client.enums.InvoiceSource;
import de.nextbill.client.enums.InvoiceStatusEnum;
import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.enums.PaymentTypeEnum;
import de.nextbill.client.enums.RepetitionTypeEnum;
import de.nextbill.client.interfaces.IPaymentItem;

public class Invoice implements java.io.Serializable, IPaymentItem {

    private static final long serialVersionUID = 1L;

    public Invoice() {
        this.invoiceId = UUID.randomUUID();
    }

    private Long id;
    private UUID createdById;
    private UUID standingOrderInvoiceTemplateId;
    private Date standingOrderStartDate;
    private UUID invoiceId;
    private InvoiceStatusEnum invoiceStatusEnum;
    private Date dateOfInvoice;
    private UUID payerId;
    private UUID paymentRecipientId;
    private UUID invoiceImageId;
    private Boolean specialType;
    private String remarks;
    private PaymentPersonTypeEnum payerTypeEnum;
    private RepetitionTypeEnum repetitionTypeEnum;
    private PaymentTypeEnum paymentTypeEnum;
    private PaymentPersonTypeEnum paymentRecipientTypeEnum;
    private BigDecimal sumOfInvoice;
    private InvoiceSource invoiceSource;
    private BigDecimal costPaid;
    private CorrectionStatus correctionStatus;

    private InvoiceCategory invoiceCategoryDTO;
    private List<CostDistributionItem> costDistributionItemDTOs;
    private List<InvoiceFailure> invoiceFailureDTOs;
    private PaymentPersonDTO paymentRecipientDTO;

    private List<ArticleDTO> articleDTOs = new ArrayList<>();

    private String invoiceFailureMessage;

    public String getInvoiceFailureMessage() {
        return invoiceFailureMessage;
    }

    public void setInvoiceFailureMessage(String invoiceFailureMessage) {
        this.invoiceFailureMessage = invoiceFailureMessage;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getStandingOrderStartDate() {
        return standingOrderStartDate;
    }

    public void setStandingOrderStartDate(Date standingOrderStartDate) {
        this.standingOrderStartDate = standingOrderStartDate;
    }

    public UUID getStandingOrderInvoiceTemplateId() {

        return standingOrderInvoiceTemplateId;
    }

    public void setStandingOrderInvoiceTemplateId(UUID standingOrderInvoiceTemplateId) {
        this.standingOrderInvoiceTemplateId = standingOrderInvoiceTemplateId;
    }

    public PaymentPersonDTO getPaymentRecipientDTO() {
        return paymentRecipientDTO;
    }

    public void setPaymentRecipientDTO(PaymentPersonDTO paymentRecipientDTO) {
        this.paymentRecipientDTO = paymentRecipientDTO;
    }

    public PaymentTypeEnum getPaymentTypeEnum() {
        return paymentTypeEnum;
    }

    public void setPaymentTypeEnum(PaymentTypeEnum paymentTypeEnum) {
        this.paymentTypeEnum = paymentTypeEnum;
    }

    public UUID getCreatedById() {
        return createdById;
    }

    public void setCreatedById(UUID createdById) {
        this.createdById = createdById;
    }

    public UUID getInvoiceId() {
        return this.invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public InvoiceStatusEnum getInvoiceStatusEnum() {
        return this.invoiceStatusEnum;
    }

    public void setInvoiceStatusEnum(InvoiceStatusEnum invoiceStatusEnum) {
        this.invoiceStatusEnum = invoiceStatusEnum;
    }

    public Date getDateOfInvoice() {
        return this.dateOfInvoice;
    }

    public void setDateOfInvoice(Date dateOfInvoice) {
        this.dateOfInvoice = dateOfInvoice;
    }

    public BigDecimal getSumOfInvoice() {
        if (sumOfInvoice == null) {
            sumOfInvoice = new BigDecimal(0);
        }
        return this.sumOfInvoice;
    }

    public void setSumOfInvoice(BigDecimal sumOfInvoice) {
        this.sumOfInvoice = sumOfInvoice;
    }


    public UUID getPayerId() {
        return this.payerId;
    }

    public void setPayerId(UUID payerId) {
        this.payerId = payerId;
    }

    public PaymentPersonTypeEnum getPayerTypeEnum() {
        return this.payerTypeEnum;
    }

    public void setPayerTypeEnum(PaymentPersonTypeEnum payerTypeEnum) {
        this.payerTypeEnum = payerTypeEnum;
    }


    public UUID getPaymentRecipientId() {
        return this.paymentRecipientId;
    }

    public void setPaymentRecipientId(UUID paymentRecipientId) {
        this.paymentRecipientId = paymentRecipientId;
    }

    public PaymentPersonTypeEnum getPaymentRecipientTypeEnum() {
        return this.paymentRecipientTypeEnum;
    }

    public void setPaymentRecipientTypeEnum(PaymentPersonTypeEnum paymentRecipientTypeEnum) {
        this.paymentRecipientTypeEnum = paymentRecipientTypeEnum;
    }

    public UUID getInvoiceImageId() {
        return this.invoiceImageId;
    }

    public void setInvoiceImageId(UUID invoiceImageId) {
        this.invoiceImageId = invoiceImageId;
    }

    public IPaymentPerson getPaymentRecipient(Context context) {
        return MainDatabaseHandler.paymentPersonByTypeAndId(getPaymentRecipientTypeEnum(), getPaymentRecipientId(), context);
    }

    public IPaymentPerson getPayer(Context context) {
        return MainDatabaseHandler.paymentPersonByTypeAndId(getPayerTypeEnum(), getPayerId(), context);
    }

    public void setPaymentRecipient(IPaymentPerson iPaymentPerson) {
        if (iPaymentPerson != null) {
            paymentRecipientId = iPaymentPerson.getPaymentPersonId();
            paymentRecipientTypeEnum = iPaymentPerson.getPaymentPersonEnum();
        }
    }

    public void setPayer(IPaymentPerson iPaymentPerson) {
        if (iPaymentPerson != null) {
            payerId = iPaymentPerson.getPaymentPersonId();
            payerTypeEnum = iPaymentPerson.getPaymentPersonEnum();
        }
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

    public RepetitionTypeEnum getRepetitionTypeEnum() {
        return repetitionTypeEnum;
    }

    public void setRepetitionTypeEnum(RepetitionTypeEnum repetitionTypeEnum) {
        this.repetitionTypeEnum = repetitionTypeEnum;
    }

    public InvoiceSource getInvoiceSource() {
        return invoiceSource;
    }

    public void setInvoiceSource(InvoiceSource invoiceSource) {
        this.invoiceSource = invoiceSource;
    }

    public InvoiceCategory getInvoiceCategoryDTO() {
        return invoiceCategoryDTO;
    }

    public void setInvoiceCategoryDTO(InvoiceCategory invoiceCategoryDTO) {
        this.invoiceCategoryDTO = invoiceCategoryDTO;
    }

    public List<CostDistributionItem> getCostDistributionItemDTOs() {
        return costDistributionItemDTOs;
    }

    public void setCostDistributionItemDTOs(List<CostDistributionItem> costDistributionItemDTOs) {
        this.costDistributionItemDTOs = costDistributionItemDTOs;
    }

    public List<InvoiceFailure> getInvoiceFailureDTOs() {
        return invoiceFailureDTOs;
    }

    public void setInvoiceFailureDTOs(List<InvoiceFailure> invoiceFailureDTOs) {
        this.invoiceFailureDTOs = invoiceFailureDTOs;
    }

    public BigDecimal getCostPaid() {
        return costPaid;
    }

    @Override
    public BigDecimal getMoneyValue() {
        if (sumOfInvoice != null){
            return sumOfInvoice;
        }
        return new BigDecimal(0);
    }

    public void setCostPaid(BigDecimal costPaid) {
        this.costPaid = costPaid;
    }

    public CorrectionStatus getCorrectionStatus() {
        return correctionStatus;
    }

    @Override
    public List<ArticleDTO> getArticleDTOs() {
        return articleDTOs;
    }

    public void setArticleDTOs(List<ArticleDTO> articleDTOs) {
        this.articleDTOs = articleDTOs;
    }

    public void setCorrectionStatus(CorrectionStatus correctionStatus) {
        this.correctionStatus = correctionStatus;
    }
}
