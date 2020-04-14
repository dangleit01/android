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

public class UserContact implements IPaymentPerson {

    public UserContact() {
        this.userContactId = UUID.randomUUID();
    }

    private Long id;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    private UUID userContactId;

    private String contactName;

    private UUID appUserId;

    private UUID appUserContactId;

    private String email;

    private Boolean project;

    public UUID getUserContactId() {
        return userContactId;
    }

    public void setUserContactId(UUID userContactId) {
        this.userContactId = userContactId;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public UUID getAppUserId() {
        return appUserId;
    }

    public void setAppUserId(UUID appUserId) {
        this.appUserId = appUserId;
    }

    public UUID getAppUserContactId() {
        return appUserContactId;
    }

    public void setAppUserContactId(UUID appUserContactId) {
        this.appUserContactId = appUserContactId;
    }

    private BasicStatusEnum basicStatusEnum;

    public BasicStatusEnum getBasicStatusEnum() {
        return basicStatusEnum;
    }

    public void setBasicStatusEnum(BasicStatusEnum basicStatusEnum) {
        this.basicStatusEnum = basicStatusEnum;
    }

    public Boolean getProject() {
        return project;
    }

    public void setProject(Boolean project) {
        this.project = project;
    }

    @Override
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getPaymentPersonName() {
        return getContactName();
    }

    @Override
    public UUID getPaymentPersonId() {
        if (getUserContactId() != null) {
            return getUserContactId();
        }
        return null;
    }

    @Override
    public PaymentPersonTypeEnum getPaymentPersonEnum() {
        return PaymentPersonTypeEnum.CONTACT;
    }

    @Override
    public PaymentPersonTypeEnum getVirtualPaymentPersonEnum() {

        if (appUserContactId != null){
            return PaymentPersonTypeEnum.USER;
        }else if (project != null && project == true){
            return PaymentPersonTypeEnum.PROJECT;
        }
        return PaymentPersonTypeEnum.CONTACT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserContact that = (UserContact) o;

        return userContactId.equals(that.userContactId);

    }

    @Override
    public int hashCode() {
        return userContactId.hashCode();
    }
}
