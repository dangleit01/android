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

package de.nextbill.client.pojos;

import java.util.ArrayList;
import java.util.List;

import de.nextbill.client.utils.InvoiceCostDistributionItem;


public class SearchFilter {
    List<InvoiceCostDistributionItem> invoiceCostDistributionItemList = new ArrayList<>();
    String searchString;
    boolean showSearchString = true;

    public List<InvoiceCostDistributionItem> getInvoiceCostDistributionItemList() {
        return invoiceCostDistributionItemList;
    }

    public void setInvoiceCostDistributionItemList(List<InvoiceCostDistributionItem> invoiceCostDistributionItemList) {
        this.invoiceCostDistributionItemList = invoiceCostDistributionItemList;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public boolean isShowSearchString() {
        return showSearchString;
    }

    public void setShowSearchString(boolean showSearchString) {
        this.showSearchString = showSearchString;
    }
}
