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

import de.nextbill.client.enums.BasicStatusEnum;
import de.nextbill.client.enums.PaymentPersonTypeEnum;

public class BusinessPartner implements IPaymentPerson {

	public BusinessPartner() {
		this.businessPartnerId = UUID.randomUUID();
	}

	private Long id;

	private UUID businessPartnerId;

	private BasicStatusEnum basicStatusEnum;

	private UUID appUserId;

	private UUID invoiceCategoryId;

	private String businessPartnerName;

	private String businessPartnerReceiptName;

	public UUID getAppUserId() {
		return appUserId;
	}

	public void setAppUserId(UUID appUserId) {
		this.appUserId = appUserId;
	}

	public BasicStatusEnum getBasicStatusEnum() {
		return basicStatusEnum;
	}

	public void setBasicStatusEnum(BasicStatusEnum basicStatusEnum) {
		this.basicStatusEnum = basicStatusEnum;
	}

	public UUID getBusinessPartnerId() {
		return this.businessPartnerId;
	}

	public void setBusinessPartnerId(UUID businessPartnerId) {
		this.businessPartnerId = businessPartnerId;
	}

	public String getBusinessPartnerName() {
		return this.businessPartnerName;
	}

	public void setBusinessPartnerName(String businessPartnerName) {
		this.businessPartnerName = businessPartnerName;
	}

	public String getBusinessPartnerReceiptName() {
		return this.businessPartnerReceiptName;
	}

	public void setBusinessPartnerReceiptName(String businessPartnerReceiptName) {
		this.businessPartnerReceiptName = businessPartnerReceiptName;
	}

//	private String businessPartnerCode;
//	public String getBusinessPartnerCode() {
//		return this.businessPartnerCode;
//	}
//	public void setBusinessPartnerCode(String businessPartnerCode) {
//		this.businessPartnerCode = businessPartnerCode;
//	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public UUID getInvoiceCategoryId() {
		return invoiceCategoryId;
	}

	public void setInvoiceCategoryId(UUID invoiceCategoryId) {
		this.invoiceCategoryId = invoiceCategoryId;
	}

	public boolean equals(Object otherObject) {
        if (this == otherObject) return true;
        if ( !(otherObject instanceof BusinessPartner) ) return false;

        final BusinessPartner other = (BusinessPartner) otherObject;
		
		if (other.getBusinessPartnerReceiptName().equals(this.getBusinessPartnerReceiptName())){
			return true;
		}
		return false;
		
	}
	
    public int hashCode() {
        int result;
        result = getBusinessPartnerReceiptName().hashCode();
        result = 29 * result + getBusinessPartnerId().hashCode();
        return result;
    }

	@Override
	public String getPaymentPersonName() {
		return getBusinessPartnerName();
	}

	@Override
	public UUID getPaymentPersonId() {
		if (getBusinessPartnerId() != null){
			return getBusinessPartnerId();
		}
		return null;
	}

	@Override
	public PaymentPersonTypeEnum getPaymentPersonEnum() {
		return PaymentPersonTypeEnum.BUSINESS_PARTNER;
	}

	@Override
	public PaymentPersonTypeEnum getVirtualPaymentPersonEnum() {
		return PaymentPersonTypeEnum.BUSINESS_PARTNER;
	}

	@Override
	public String getEmail() {
		return "";
	}
}
