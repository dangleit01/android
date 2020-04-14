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

import de.nextbill.client.enums.InvoiceStatusEnum;
import de.nextbill.client.model.Invoice;


public class InvoiceStatusComparator implements Comparator{
    public InvoiceStatusComparator() {
    }

    @Override
    public int compare(Object lhs, Object rhs) {
        Invoice invoice1 = (Invoice) lhs;
        Invoice invoice2 = (Invoice) rhs;

        if (invoice1.getInvoiceStatusEnum() == null && invoice2.getInvoiceStatusEnum() == null){
            return 0;
        }else if (invoice1.getInvoiceStatusEnum() == null){
            return 1;
        }else if (invoice2.getInvoiceStatusEnum() == null){
            return -1;
        }else if(invoice1.getInvoiceStatusEnum().equals(InvoiceStatusEnum.CHECK) &&
                !invoice2.getInvoiceStatusEnum().equals(InvoiceStatusEnum.CHECK)){
            return -1;
        }else if(invoice2.getInvoiceStatusEnum().equals(InvoiceStatusEnum.CHECK) &&
                !invoice1.getInvoiceStatusEnum().equals(InvoiceStatusEnum.CHECK)){
            return 1;
        }else if(invoice1.getInvoiceStatusEnum().equals(InvoiceStatusEnum.ANALYZING) &&
                invoice2.getInvoiceStatusEnum().equals(InvoiceStatusEnum.WAIT_FOR_UPLOAD)){
            return -1;
        }else if(invoice2.getInvoiceStatusEnum().equals(InvoiceStatusEnum.ANALYZING) &&
                invoice1.getInvoiceStatusEnum().equals(InvoiceStatusEnum.WAIT_FOR_UPLOAD)){
            return 1;
        }else if(invoice1.getInvoiceStatusEnum().equals(invoice2.getInvoiceStatusEnum())){
            return 0;
        }
        return 0;
    }

    @Override
    public boolean equals(Object object) {
        return false;
    }
}
