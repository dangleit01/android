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

import java.util.UUID;

import de.nextbill.client.enums.InvoiceFailureTypeEnum;

public class InvoiceFailure {

    public InvoiceFailure() {
        this.invoiceFailureId = UUID.randomUUID();
    }

    private Long id;
    private UUID invoiceFailureId;
    private String message;
    private UUID invoiceId;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getInvoiceFailureId() {
        return this.invoiceFailureId;
    }

    public void setInvoiceFailureId(UUID invoiceFailureId) {
        this.invoiceFailureId = invoiceFailureId;
    }
    private InvoiceFailureTypeEnum invoiceFailureTypeEnum;

    public InvoiceFailureTypeEnum getInvoiceFailureTypeEnum() {
        return this.invoiceFailureTypeEnum;
    }

    public void setInvoiceFailureTypeEnum(InvoiceFailureTypeEnum invoiceFailureTypeEnum) {
        this.invoiceFailureTypeEnum = invoiceFailureTypeEnum;
    }

    public UUID getInvoiceId() {
        return this.invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }


}
