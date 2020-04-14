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

import de.nextbill.client.model.BusinessPartner;


public class BusinessPartnerComparator implements Comparator{
    public BusinessPartnerComparator() {
    }

    @Override
    public int compare(Object lhs, Object rhs) {
        BusinessPartner businessPartner1 = (BusinessPartner) lhs;
        BusinessPartner businessPartner2 = (BusinessPartner) rhs;

        String bpname1 = businessPartner1.getBusinessPartnerName();
        if (bpname1 != null){
            bpname1 = bpname1.toLowerCase();
        }
        String bpname2 = businessPartner2.getBusinessPartnerName();
        if (bpname2 != null){
            bpname2 = bpname2.toLowerCase();
        }

        if (bpname1 == null && bpname2 == null){
            return 0;
        }else if (bpname1 == null){
            return 1;
        }else if (bpname2 == null){
            return -1;
        }else if(bpname1.equals(bpname2)) {
            return 0;
        }else{
            return bpname1.compareTo(bpname2);
        }
    }

    @Override
    public boolean equals(Object object) {
        return false;
    }
}
