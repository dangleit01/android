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

import de.nextbill.client.model.Invoice;


public class InvoiceTimeComparator implements Comparator{
    public InvoiceTimeComparator() {
    }

    @Override
    public int compare(Object lhs, Object rhs) {
        Invoice invoice1 = (Invoice) lhs;
        Invoice invoice2 = (Invoice) rhs;

        if (invoice1.getDateOfInvoice() == null && invoice2.getDateOfInvoice() == null){
            return 0;
        }else if (invoice1.getDateOfInvoice() == null){
            return 1;
        }else if (invoice2.getDateOfInvoice() == null){
            return -1;
        }else if(invoice1.getDateOfInvoice().before(invoice2.getDateOfInvoice())){
            return 1;
        }else if(invoice2.getDateOfInvoice().before(invoice1.getDateOfInvoice())){
            return -1;
        }else if(invoice1.getDateOfInvoice().equals(invoice2.getDateOfInvoice())){
            return 0;
        }
        return 0;
    }

    @Override
    public boolean equals(Object object) {
        return false;
    }
}
