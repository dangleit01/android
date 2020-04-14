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

import java.util.List;
import java.util.UUID;


public class CostDistribution {

    public CostDistribution() {
        this.costDistributionId = UUID.randomUUID();
    }

    private Long id;
    private UUID costDistributionId;
    private String name;
    private UUID createdById;
    private List<CostDistributionItem> costDistributionItemDTOs;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getCostDistributionId() {
        return costDistributionId;
    }

    public void setCostDistributionId(UUID costDistributionId) {
        this.costDistributionId = costDistributionId;
    }

    public List<CostDistributionItem> getCostDistributionItemDTOs() {
        return costDistributionItemDTOs;
    }

    public void setCostDistributionItemDTOs(List<CostDistributionItem> costDistributionItemDTOs) {
        this.costDistributionItemDTOs = costDistributionItemDTOs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getCreatedById() {
        return createdById;
    }

    public void setCreatedById(UUID createdById) {
        this.createdById = createdById;
    }
}
