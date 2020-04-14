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

package de.nextbill.client.comparators;

import java.util.Comparator;

import de.nextbill.client.model.CostDistributionItem;


public class CostDistributionPositionComparator implements Comparator{
    public CostDistributionPositionComparator() {
    }

    @Override
    public int compare(Object lhs, Object rhs) {
        CostDistributionItem costDistributionItem1 = (CostDistributionItem) lhs;
        CostDistributionItem costDistributionItem2 = (CostDistributionItem) rhs;

        if (costDistributionItem1.getPosition() == null && costDistributionItem2.getPosition() == null){
            return 0;
        }else if (costDistributionItem1.getPosition() == null){
            return 1;
        }else if (costDistributionItem2.getPosition() == null){
            return -1;
        }else if (costDistributionItem1.getPosition() == costDistributionItem2.getPosition()) {
            return 0;
        }else if (costDistributionItem1.getPosition() > costDistributionItem2.getPosition()) {
            return 1;
        }else if (costDistributionItem1.getPosition() < costDistributionItem2.getPosition()) {
            return -1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object object) {
        return false;
    }
}
