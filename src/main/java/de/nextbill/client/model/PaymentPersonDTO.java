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

import de.nextbill.client.enums.PaymentPersonTypeEnum;


public class PaymentPersonDTO implements IPaymentPerson{

    private UUID id;
    private PaymentPersonTypeEnum paymentPersonTypeEnum;
    private String displayName;


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PaymentPersonTypeEnum getPaymentPersonTypeEnum() {
        return paymentPersonTypeEnum;
    }

    public void setPaymentPersonTypeEnum(PaymentPersonTypeEnum paymentPersonTypeEnum) {
        this.paymentPersonTypeEnum = paymentPersonTypeEnum;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getPaymentPersonName() {
        return displayName;
    }

    @Override
    public UUID getPaymentPersonId() {
        return id;
    }

    @Override
    public PaymentPersonTypeEnum getPaymentPersonEnum() {
        return paymentPersonTypeEnum;
    }

    @Override
    public String getEmail() {
        return null;
    }

    @Override
    public PaymentPersonTypeEnum getVirtualPaymentPersonEnum() {
        return null;
    }
}
