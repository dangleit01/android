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
import java.util.Date;
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


public class AllDataPatch {

    private Date lastModifiedDateFromServer;
    private List<DatabaseChangeDTO<AppUser>> appUserDTOs = new ArrayList<>();
    private List<DatabaseChangeDTO<BasicData>> basicDataDTOs = new ArrayList<>();
    private List<DatabaseChangeDTO<BusinessPartner>> businessPartnerDTOs = new ArrayList<>();
    private List<DatabaseChangeDTO<CostDistribution>> costDistributionDTOs = new ArrayList<>();
    private List<DatabaseChangeDTO<CostDistributionItem>> costDistributionItemDTOs = new ArrayList<>();
    private List<DatabaseChangeDTO<Invoice>> invoiceDTOs = new ArrayList<>();
    private List<DatabaseChangeDTO<InvoiceCategory>> invoiceCategorieDTOs = new ArrayList<>();
    private List<DatabaseChangeDTO<InvoiceFailure>> invoiceFailureDTOs = new ArrayList<>();
    private List<DatabaseChangeDTO<UserContact>> userContactDTOs = new ArrayList<>();

    public Date getLastModifiedDateFromServer() {
        return lastModifiedDateFromServer;
    }

    public void setLastModifiedDateFromServer(Date lastModifiedDateFromServer) {
        this.lastModifiedDateFromServer = lastModifiedDateFromServer;
    }

    public List<DatabaseChangeDTO<AppUser>> getAppUserDTOs() {
        return appUserDTOs;
    }

    public void setAppUserDTOs(List<DatabaseChangeDTO<AppUser>> appUserDTOs) {
        this.appUserDTOs = appUserDTOs;
    }

    public List<DatabaseChangeDTO<BasicData>> getBasicDataDTOs() {
        return basicDataDTOs;
    }

    public void setBasicDataDTOs(List<DatabaseChangeDTO<BasicData>> basicDataDTOs) {
        this.basicDataDTOs = basicDataDTOs;
    }

    public List<DatabaseChangeDTO<BusinessPartner>> getBusinessPartnerDTOs() {
        return businessPartnerDTOs;
    }

    public void setBusinessPartnerDTOs(List<DatabaseChangeDTO<BusinessPartner>> businessPartnerDTOs) {
        this.businessPartnerDTOs = businessPartnerDTOs;
    }

    public List<DatabaseChangeDTO<CostDistribution>> getCostDistributionDTOs() {
        return costDistributionDTOs;
    }

    public void setCostDistributionDTOs(List<DatabaseChangeDTO<CostDistribution>> costDistributionDTOs) {
        this.costDistributionDTOs = costDistributionDTOs;
    }

    public List<DatabaseChangeDTO<CostDistributionItem>> getCostDistributionItemDTOs() {
        return costDistributionItemDTOs;
    }

    public void setCostDistributionItemDTOs(List<DatabaseChangeDTO<CostDistributionItem>> costDistributionItemDTOs) {
        this.costDistributionItemDTOs = costDistributionItemDTOs;
    }

    public List<DatabaseChangeDTO<Invoice>> getInvoiceDTOs() {
        return invoiceDTOs;
    }

    public void setInvoiceDTOs(List<DatabaseChangeDTO<Invoice>> invoiceDTOs) {
        this.invoiceDTOs = invoiceDTOs;
    }

    public List<DatabaseChangeDTO<InvoiceCategory>> getInvoiceCategorieDTOs() {
        return invoiceCategorieDTOs;
    }

    public void setInvoiceCategorieDTOs(List<DatabaseChangeDTO<InvoiceCategory>> invoiceCategorieDTOs) {
        this.invoiceCategorieDTOs = invoiceCategorieDTOs;
    }

    public List<DatabaseChangeDTO<InvoiceFailure>> getInvoiceFailureDTOs() {
        return invoiceFailureDTOs;
    }

    public void setInvoiceFailureDTOs(List<DatabaseChangeDTO<InvoiceFailure>> invoiceFailureDTOs) {
        this.invoiceFailureDTOs = invoiceFailureDTOs;
    }

    public List<DatabaseChangeDTO<UserContact>> getUserContactDTOs() {
        return userContactDTOs;
    }

    public void setUserContactDTOs(List<DatabaseChangeDTO<UserContact>> userContactDTOs) {
        this.userContactDTOs = userContactDTOs;
    }
}
