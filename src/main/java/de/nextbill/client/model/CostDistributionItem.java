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
import java.util.List;
import java.util.UUID;

import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.enums.CorrectionStatus;
import de.nextbill.client.enums.CostDistributionItemTypeEnum;
import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.interfaces.IPaymentItem;


public class CostDistributionItem implements IPaymentItem {

    public CostDistributionItem() {
        this.costDistributionItemId = UUID.randomUUID();
    }

    private Long id;
    private CostDistributionItemTypeEnum costDistributionItemTypeEnum;
    private BigDecimal value;
    private BigDecimal moneyValue;
    private BigDecimal costPaid;
    private UUID payerId;
    private PaymentPersonTypeEnum paymentPersonTypeEnum;
    private String paymentPersonName;
    private Integer position;
    private UUID costDistributionItemId;
    private UUID invoiceId;
    private String isTemporary;
    private UUID costDistributionId;
    private String remarks;
    private CorrectionStatus correctionStatus;
    private List<ArticleDTO> articleDTOs = new ArrayList<>();

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public IPaymentPerson getIPaymentPerson() {
        PaymentPerson paymentPerson = new PaymentPerson();
        paymentPerson.setPaymentPersonId(payerId);
        paymentPerson.setPaymentPersonEnum(paymentPersonTypeEnum);
        paymentPerson.setPaymentPersonName(paymentPersonName);
        paymentPerson.setVirtualPayerEnum(paymentPersonTypeEnum);
        return paymentPerson;
    }

    public String getPaymentPersonName() {
        return paymentPersonName;
    }

    public String getPaymentPersonName(Context context) {
        if (paymentPersonName != null){
            return paymentPersonName;
        }
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);
        IPaymentPerson paymentPerson = MainDatabaseHandler.paymentPersonByTypeAndId(paymentPersonTypeEnum, payerId, context);
        return paymentPerson.getPaymentPersonName();
    }

    public void setPaymentPersonName(String paymentPersonName) {
        this.paymentPersonName = paymentPersonName;
    }

    public CostDistributionItemTypeEnum getCostDistributionItemTypeEnum() {
        return costDistributionItemTypeEnum;
    }

    public void setCostDistributionItemTypeEnum(CostDistributionItemTypeEnum costDistributionItemTypeEnum) {
        this.costDistributionItemTypeEnum = costDistributionItemTypeEnum;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getCostPaid() {
        if (costPaid == null){
            costPaid = new BigDecimal(0);
        }

        return costPaid;
    }

    public void setCostPaid(BigDecimal costPaid) {
        this.costPaid = costPaid;
    }

    public UUID getPayerId() {
        return payerId;
    }

    public void setPayerId(UUID payerId) {
        this.payerId = payerId;
    }

    public PaymentPersonTypeEnum getPaymentPersonTypeEnum() {
        return paymentPersonTypeEnum;
    }

    public void setPaymentPersonTypeEnum(PaymentPersonTypeEnum paymentPersonTypeEnum) {
        this.paymentPersonTypeEnum = paymentPersonTypeEnum;
    }

    public UUID getCostDistributionItemId() {
        return costDistributionItemId;
    }

    public void setCostDistributionItemId(UUID costDistributionItemId) {
        this.costDistributionItemId = costDistributionItemId;
    }

    public void setPayer(IPaymentPerson iPaymentPerson){
        if (iPaymentPerson != null){
            payerId = iPaymentPerson.getPaymentPersonId();
            paymentPersonTypeEnum = iPaymentPerson.getPaymentPersonEnum();
        }
    }

    public String getIsTemporary() {
        return isTemporary;
    }

    public void setIsTemporary(String isTemporary) {
        this.isTemporary = isTemporary;
    }

    public IPaymentPerson getPayer(Context context) {
        IPaymentPerson paymentPerson = MainDatabaseHandler.paymentPersonByTypeAndId(getPaymentPersonTypeEnum(), getPayerId(), context, paymentPersonName);
        return paymentPerson;
    }

    public UUID getCostDistributionId() {
        return costDistributionId;
    }

    public void setCostDistributionId(UUID costDistributionId) {
        this.costDistributionId = costDistributionId;
    }

    public BigDecimal getMoneyValue() {
        if (moneyValue == null){
            moneyValue = new BigDecimal(0);
        }
        return moneyValue;
    }

    public void setMoneyValue(BigDecimal moneyValue) {
        this.moneyValue = moneyValue;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
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
