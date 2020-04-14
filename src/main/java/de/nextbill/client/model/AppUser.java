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
import java.util.List;
import java.util.UUID;

import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.enums.UpdateModusType;

public class AppUser implements IPaymentPerson {

	public AppUser() {
		this.appUserId = UUID.randomUUID();
	}

	private Long id;

	private String email;

	private UUID appUserId;

	private String appUserName;

	private String appUserPassword;

	private String appUserAccessKey;

	private Date appUserAccessExpiration;

	private String appUserRefreshToken;

	private Date lastUpdate;

	private UpdateModusType updateModusType;

	private List<AppRightDTO> appRightDTOs;

	private Boolean isLoggedIn;

	public Boolean getLoggedIn() {
		return isLoggedIn;
	}

	public void setLoggedIn(Boolean loggedIn) {
		isLoggedIn = loggedIn;
	}

	public List<AppRightDTO> getAppRightDTOs() {
		return appRightDTOs;
	}

	public void setAppRightDTOs(List<AppRightDTO> appRightDTOs) {
		this.appRightDTOs = appRightDTOs;
	}

	public UpdateModusType getUpdateModusType() {
		return updateModusType;
	}

	public void setUpdateModusType(UpdateModusType updateModusType) {
		this.updateModusType = updateModusType;
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public UUID getAppUserId() {
		return this.appUserId;
	}

	public void setAppUserId(UUID appUserId) {
		this.appUserId = appUserId;
	}

	public String getAppUserName() {
		return this.appUserName;
	}

	public void setAppUserName(String appUserName) {
		this.appUserName = appUserName;
	}

	public String getAppUserPassword() {
		return this.appUserPassword;
	}

	public void setAppUserPassword(String appUserPassword) {
		this.appUserPassword = appUserPassword;
	}

	public String getAppUserAccessKey() {
		return appUserAccessKey;
	}

	public void setAppUserAccessKey(String appUserAccessKey) {
		this.appUserAccessKey = appUserAccessKey;
	}

	public Date getAppUserAccessExpiration() {
		return appUserAccessExpiration;
	}

	public void setAppUserAccessExpiration(Date appUserAccessExpiration) {
		this.appUserAccessExpiration = appUserAccessExpiration;
	}

	public String getAppUserRefreshToken() {
		return appUserRefreshToken;
	}

	public void setAppUserRefreshToken(String appUserRefreshToken) {
		this.appUserRefreshToken = appUserRefreshToken;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	@Override
	public String getPaymentPersonName() {
		return getAppUserName();
	}

	@Override
	public UUID getPaymentPersonId() {
		if (getAppUserId() != null){
			return getAppUserId();
		}
		return null;
	}

	@Override
	public PaymentPersonTypeEnum getPaymentPersonEnum() {
		return PaymentPersonTypeEnum.USER;
	}

	@Override
	public PaymentPersonTypeEnum getVirtualPaymentPersonEnum() {
		return PaymentPersonTypeEnum.USER;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		AppUser appUser = (AppUser) o;

		return appUserId.equals(appUser.appUserId);

	}

	@Override
	public int hashCode() {
		return appUserId.hashCode();
	}
}
