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

import de.nextbill.client.model.UserContact;


public class UserContactComparator implements Comparator{
    public UserContactComparator() {
    }

    @Override
    public int compare(Object lhs, Object rhs) {
        UserContact userContact1 = (UserContact) lhs;
        UserContact userContact2 = (UserContact) rhs;

        String contactName1 = userContact1.getContactName();
        if (contactName1 != null){
            contactName1 = contactName1.toLowerCase();
        }
        String contactName2 = userContact2.getContactName();
        if (contactName2 != null){
            contactName2 = contactName2.toLowerCase();
        }

        if (contactName1 == null && contactName2 == null){
            return 0;
        }else if (contactName1 == null){
            return 1;
        }else if (contactName2 == null){
            return -1;
        }else if(contactName1.equals(contactName2)) {
            return 0;
        }else{
            return contactName1.compareTo(contactName2);
        }
    }

    @Override
    public boolean equals(Object object) {
        return false;
    }
}
