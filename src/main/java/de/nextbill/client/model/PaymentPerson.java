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


public class PaymentPerson implements IPaymentPerson {

    private String payerName;
    private UUID payerId;
    private PaymentPersonTypeEnum payerEnum;
    private String payerEmail;
    private PaymentPersonTypeEnum virtualPayerEnum;

    public void setPaymentPersonName(String payerName) {
        this.payerName = payerName;
    }

    public void setPaymentPersonId(UUID payerId) {
        this.payerId = payerId;
    }

    public void setPaymentPersonEnum(PaymentPersonTypeEnum payerEnum) {
        this.payerEnum = payerEnum;
    }

    public PaymentPersonTypeEnum getVirtualPayerEnum() {
        return virtualPayerEnum;
    }

    public void setVirtualPayerEnum(PaymentPersonTypeEnum virtualPayerEnum) {
        this.virtualPayerEnum = virtualPayerEnum;
    }

    public void setPayerEmail(String payerEmail) {
        this.payerEmail = payerEmail;
    }

    @Override
    public String getPaymentPersonName() {
        return payerName;
    }

    @Override
    public UUID getPaymentPersonId() {
        return payerId;
    }

    @Override
    public PaymentPersonTypeEnum getPaymentPersonEnum() {
        return payerEnum;
    }

    @Override
    public String getEmail() {
        return payerEmail;
    }

    @Override
    public PaymentPersonTypeEnum getVirtualPaymentPersonEnum() {
        return virtualPayerEnum;
    }

    public static PaymentPerson fromIPaymentPerson(IPaymentPerson paymentPerson){
        PaymentPerson paymentPersonBean = new PaymentPerson();
        String paymentPersonName = paymentPerson.getPaymentPersonName();
        if (paymentPersonName != null){
            paymentPersonBean.setPaymentPersonName(paymentPersonName.trim());
        }
        paymentPersonBean.setPaymentPersonEnum(paymentPerson.getPaymentPersonEnum());
        paymentPersonBean.setPaymentPersonId(paymentPerson.getPaymentPersonId());
        paymentPersonBean.setPayerEmail(paymentPerson.getEmail());
        paymentPersonBean.setVirtualPayerEnum(paymentPerson.getVirtualPaymentPersonEnum());
        return paymentPersonBean;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PaymentPerson that = (PaymentPerson) o;

        if (payerName != null ? !payerName.equals(that.payerName) : that.payerName != null)
            return false;
        if (payerId != null ? !payerId.equals(that.payerId) : that.payerId != null) return false;
        if (payerEnum != that.payerEnum) return false;
        if (payerEmail != null ? !payerEmail.equals(that.payerEmail) : that.payerEmail != null)
            return false;
        return virtualPayerEnum == that.virtualPayerEnum;

    }

    @Override
    public int hashCode() {
        int result = payerName != null ? payerName.hashCode() : 0;
        result = 31 * result + (payerId != null ? payerId.hashCode() : 0);
        result = 31 * result + (payerEnum != null ? payerEnum.hashCode() : 0);
        result = 31 * result + (payerEmail != null ? payerEmail.hashCode() : 0);
        result = 31 * result + (virtualPayerEnum != null ? virtualPayerEnum.hashCode() : 0);
        return result;
    }
}
