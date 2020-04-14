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
import de.nextbill.client.enums.InvoiceCategoryType;

public class InvoiceCategory {

	private Long id;

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public InvoiceCategory() {
		this.invoiceCategoryId = UUID.randomUUID();
	}

	private UUID invoiceCategoryId;
	public UUID getInvoiceCategoryId() {
		return this.invoiceCategoryId;
	}
	public void setInvoiceCategoryId(UUID invoiceCategoryId) {
		this.invoiceCategoryId = invoiceCategoryId;
	}

	private InvoiceCategoryType invoiceCategoryType;

	public InvoiceCategoryType getInvoiceCategoryType() {
		return invoiceCategoryType;
	}

	public void setInvoiceCategoryType(InvoiceCategoryType invoiceCategoryType) {
		this.invoiceCategoryType = invoiceCategoryType;
	}

	private BasicStatusEnum basicStatusEnum;

	public BasicStatusEnum getBasicStatusEnum() {
		return basicStatusEnum;
	}

	public void setBasicStatusEnum(BasicStatusEnum basicStatusEnum) {
		this.basicStatusEnum = basicStatusEnum;
	}

	private InvoiceCategory parentInvoiceCategoryDTO;

	public InvoiceCategory getParentInvoiceCategoryDTO() {
		return parentInvoiceCategoryDTO;
	}

	public void setParentInvoiceCategoryDTO(InvoiceCategory parentInvoiceCategoryDTO) {
		this.parentInvoiceCategoryDTO = parentInvoiceCategoryDTO;
	}

	private UUID appUserId;

	public UUID getAppUserId() {
		return appUserId;
	}

	public void setAppUserId(UUID appUserId) {
		this.appUserId = appUserId;
	}

	private String invoiceCategoryName;
	public String getInvoiceCategoryName() {
		return this.invoiceCategoryName;
	}

	public void setInvoiceCategoryName(String invoiceCategoryName) {
		this.invoiceCategoryName = invoiceCategoryName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		InvoiceCategory that = (InvoiceCategory) o;

		return !(invoiceCategoryId != null ? !invoiceCategoryId.equals(that.invoiceCategoryId) : that.invoiceCategoryId != null);

	}

	@Override
	public int hashCode() {
		return invoiceCategoryId != null ? invoiceCategoryId.hashCode() : 0;
	}
}
