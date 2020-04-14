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

import java.util.Date;
import java.util.UUID;

import de.nextbill.client.enums.RepetitionTypeEnum;


public class StandingOrder {

    private UUID standingOrderId;

    private UUID invoiceTemplateId;

    private Date startDate;

    private UUID futureInvoiceTemplateId;

    private RepetitionTypeEnum repetitionTypeEnum;

    private Boolean isAlwaysPaid;

    public UUID getStandingOrderId() {
        return standingOrderId;
    }

    public void setStandingOrderId(UUID standingOrderId) {
        this.standingOrderId = standingOrderId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public RepetitionTypeEnum getRepetitionTypeEnum() {
        return repetitionTypeEnum;
    }

    public void setRepetitionTypeEnum(RepetitionTypeEnum repetitionTypeEnum) {
        this.repetitionTypeEnum = repetitionTypeEnum;
    }

    public UUID getInvoiceTemplateId() {
        return invoiceTemplateId;
    }

    public void setInvoiceTemplateId(UUID invoiceTemplateId) {
        this.invoiceTemplateId = invoiceTemplateId;
    }

    public UUID getFutureInvoiceTemplateId() {
        return futureInvoiceTemplateId;
    }

    public void setFutureInvoiceTemplateId(UUID futureInvoiceTemplateId) {
        this.futureInvoiceTemplateId = futureInvoiceTemplateId;
    }

    public Boolean getIsAlwaysPaid() {
        return isAlwaysPaid;
    }

    public void setIsAlwaysPaid(Boolean isAlwaysPaid) {
        this.isAlwaysPaid = isAlwaysPaid;
    }
}
