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

import java.util.List;

import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.BasicData;
import de.nextbill.client.model.BusinessPartner;
import de.nextbill.client.model.CostDistribution;
import de.nextbill.client.model.CostDistributionItem;
import de.nextbill.client.model.Invoice;
import de.nextbill.client.model.InvoiceCategory;
import de.nextbill.client.model.InvoiceFailure;
import de.nextbill.client.model.UserContact;


public class AllData {

    private List<AppUser> appUserDTOs;
    private List<BasicData> basicDataDTOs;
    private List<BusinessPartner> businessPartnerDTOs;
    private List<CostDistribution> costDistributionDTOs;
    private List<CostDistributionItem> costDistributionItemDTOs;
    private List<Invoice> invoiceDTOs;
    private List<InvoiceCategory> invoiceCategorieDTOs;
    private List<InvoiceFailure> invoiceFailureDTOs;
    private List<UserContact> userContactDTOs;

    public List<AppUser> getAppUserDTOs() {
        return appUserDTOs;
    }

    public void setAppUserDTOs(List<AppUser> appUserDTOs) {
        this.appUserDTOs = appUserDTOs;
    }

    public List<BasicData> getBasicDataDTOs() {
        return basicDataDTOs;
    }

    public void setBasicDataDTOs(List<BasicData> basicDataDTOs) {
        this.basicDataDTOs = basicDataDTOs;
    }

    public List<BusinessPartner> getBusinessPartnerDTOs() {
        return businessPartnerDTOs;
    }

    public void setBusinessPartnerDTOs(List<BusinessPartner> businessPartnerDTOs) {
        this.businessPartnerDTOs = businessPartnerDTOs;
    }

    public List<CostDistribution> getCostDistributionDTOs() {
        return costDistributionDTOs;
    }

    public void setCostDistributionDTOs(List<CostDistribution> costDistributionDTOs) {
        this.costDistributionDTOs = costDistributionDTOs;
    }

    public List<CostDistributionItem> getCostDistributionItemDTOs() {
        return costDistributionItemDTOs;
    }

    public void setCostDistributionItemDTOs(List<CostDistributionItem> costDistributionItemDTOs) {
        this.costDistributionItemDTOs = costDistributionItemDTOs;
    }

    public List<Invoice> getInvoiceDTOs() {
        return invoiceDTOs;
    }

    public void setInvoiceDTOs(List<Invoice> invoiceDTOs) {
        this.invoiceDTOs = invoiceDTOs;
    }

    public List<InvoiceCategory> getInvoiceCategorieDTOs() {
        return invoiceCategorieDTOs;
    }

    public void setInvoiceCategorieDTOs(List<InvoiceCategory> invoiceCategorieDTOs) {
        this.invoiceCategorieDTOs = invoiceCategorieDTOs;
    }

    public List<InvoiceFailure> getInvoiceFailureDTOs() {
        return invoiceFailureDTOs;
    }

    public void setInvoiceFailureDTOs(List<InvoiceFailure> invoiceFailureDTOs) {
        this.invoiceFailureDTOs = invoiceFailureDTOs;
    }

    public List<UserContact> getUserContactDTOs() {
        return userContactDTOs;
    }

    public void setUserContactDTOs(List<UserContact> userContactDTOs) {
        this.userContactDTOs = userContactDTOs;
    }
}
