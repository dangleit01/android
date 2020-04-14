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
import java.util.List;
import java.util.UUID;

import de.nextbill.client.enums.BillingStatusEnum;
import de.nextbill.client.model.CostDistributionItem;


public class BillingPayment {

    private UUID billingId;
    private BillingStatusEnum billingStatusEnum;
    private BigDecimal sumPaid;
    private BigDecimal restSumAfterPayment;
    private List<CostDistributionItem> costDistributionItemsPaid;
    private List<CostDistributionItem> costDistributionItemsNotPaid;

    public BigDecimal getSumPaid() {
        return sumPaid;
    }

    public void setSumPaid(BigDecimal sumPaid) {
        this.sumPaid = sumPaid;
    }

    public BigDecimal getRestSumAfterPayment() {
        return restSumAfterPayment;
    }

    public void setRestSumAfterPayment(BigDecimal restSumAfterPayment) {
        this.restSumAfterPayment = restSumAfterPayment;
    }

    public List<CostDistributionItem> getCostDistributionItemsPaid() {
        return costDistributionItemsPaid;
    }

    public void setCostDistributionItemsPaid(List<CostDistributionItem> costDistributionItemsPaid) {
        this.costDistributionItemsPaid = costDistributionItemsPaid;
    }

    public List<CostDistributionItem> getCostDistributionItemsNotPaid() {
        return costDistributionItemsNotPaid;
    }

    public void setCostDistributionItemsNotPaid(List<CostDistributionItem> costDistributionItemsNotPaid) {
        this.costDistributionItemsNotPaid = costDistributionItemsNotPaid;
    }

    public BillingStatusEnum getBillingStatusEnum() {
        return billingStatusEnum;
    }

    public void setBillingStatusEnum(BillingStatusEnum billingStatusEnum) {
        this.billingStatusEnum = billingStatusEnum;
    }

    public UUID getBillingId() {
        return billingId;
    }

    public void setBillingId(UUID billingId) {
        this.billingId = billingId;
    }
}
