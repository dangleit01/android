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

import java.math.BigDecimal;
import java.util.UUID;

import de.nextbill.client.enums.BasicDataSubType;
import de.nextbill.client.enums.BasicDataType;

public class BasicData {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BasicData() {
        this.basicDataId = UUID.randomUUID();
    }

    private UUID basicDataId;

    private BasicDataType basicDataType;

    private BasicDataSubType basicDataSubType;

    private BigDecimal numberValue;

    private UUID appUserId;

    private String object1Class;

    private String object1Id;

    private String object2Class;

    private String object2Id;

    private String value;

    public UUID getBasicDataId() {
        return basicDataId;
    }

    public void setBasicDataId(UUID basicDataId) {
        this.basicDataId = basicDataId;
    }

    public BasicDataType getBasicDataType() {
        return basicDataType;
    }

    public void setBasicDataType(BasicDataType basicDataType) {
        this.basicDataType = basicDataType;
    }

    public BasicDataSubType getBasicDataSubType() {
        return basicDataSubType;
    }

    public void setBasicDataSubType(BasicDataSubType basicDataSubType) {
        this.basicDataSubType = basicDataSubType;
    }

    public BigDecimal getNumberValue() {
        return numberValue;
    }

    public void setNumberValue(BigDecimal numberValue) {
        this.numberValue = numberValue;
    }

    public UUID getAppUserId() {
        return appUserId;
    }

    public void setAppUserId(UUID appUserId) {
        this.appUserId = appUserId;
    }

    public String getObject1Class() {
        return object1Class;
    }

    public void setObject1Class(String object1Class) {
        this.object1Class = object1Class;
    }

    public String getObject1Id() {
        return object1Id;
    }

    public void setObject1Id(String object1Id) {
        this.object1Id = object1Id;
    }

    public String getObject2Class() {
        return object2Class;
    }

    public void setObject2Class(String object2Class) {
        this.object2Class = object2Class;
    }

    public String getObject2Id() {
        return object2Id;
    }

    public void setObject2Id(String object2Id) {
        this.object2Id = object2Id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
