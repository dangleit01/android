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

package de.nextbill.client.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import de.nextbill.client.enums.BasicDataSubType;
import de.nextbill.client.enums.BasicDataType;
import de.nextbill.client.enums.BasicStatusEnum;
import de.nextbill.client.enums.CorrectionStatus;
import de.nextbill.client.enums.CostDistributionItemTypeEnum;
import de.nextbill.client.enums.InvoiceCategoryType;
import de.nextbill.client.enums.InvoiceFailureTypeEnum;
import de.nextbill.client.enums.InvoiceSource;
import de.nextbill.client.enums.InvoiceStatusEnum;
import de.nextbill.client.enums.PaymentPersonTypeEnum;
import de.nextbill.client.enums.PaymentTypeEnum;
import de.nextbill.client.enums.RepetitionTypeEnum;
import de.nextbill.client.enums.SQLOperatorEnum;
import de.nextbill.client.enums.UpdateModusType;
import de.nextbill.client.helper.LoginUserHelper;
import de.nextbill.client.interfaces.IPaymentItem;
import de.nextbill.client.model.AppRightDTO;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.ArticleDTO;
import de.nextbill.client.model.BasicData;
import de.nextbill.client.model.BusinessPartner;
import de.nextbill.client.model.CostDistribution;
import de.nextbill.client.model.CostDistributionItem;
import de.nextbill.client.model.IPaymentPerson;
import de.nextbill.client.model.Invoice;
import de.nextbill.client.model.InvoiceCategory;
import de.nextbill.client.model.InvoiceFailure;
import de.nextbill.client.model.PaymentPerson;
import de.nextbill.client.model.PaymentPersonDTO;
import de.nextbill.client.model.UserContact;
import de.nextbill.client.pojos.DatabaseChangeDTO;
import de.nextbill.client.utils.SqlBuilder;

public class MainDatabaseHandler extends SQLiteOpenHelper {
	public static final String VAR_ID="id";

	public static final String TABLE_INVOICE="Invoice";
	public static final String VAR_INVOICE_ID="invoiceId";
	public static final String VAR_SUM_OF_INVOICE="sumOfInvoice";
	public static final String VAR_DATE_OF_INVOICE="dateOfInvoice";
	public static final String VAR_INVOICE_STATUS_ENUM="invoiceStatusEnum";
	public static final String VAR_INVOICE_STANDING_ORDER_INVOICE_TEMPLATE_ID="invoiceStandingOrderInvoiceTemplateId";
	public static final String VAR_INVOICE_STANDING_ORDER_START_DATE ="invoiceStandingOrderStartDate";
	public static final String VAR_INVOICE_SPECIAL_TYPE="invoiceSpecialType";
	public static final String VAR_INVOICE_REMARKS="invoiceRemarks";
	public static final String VAR_INVOICE_INVOICE_FAILURE_MESSAGE="invoiceFailureMessage";
	public static final String VAR_INVOICE_REPETITION_TYPE="invoiceRepetitionType";
	public static final String VAR_INVOICE_SOURCE="invoiceSource";
	public static final String VAR_INVOICE_COST_PAID ="invoiceCostPaid";
	public static final String VAR_INVOICE_CORRECTION_STATUS ="invoiceCorrectionStatus";
	public static final String VAR_ARTICLES_DTOS ="articleDTOs";
	public static final String VAR_RIGHTS_DTOS ="appRightDTOs";
	public static final String VAR_PAYMENT_TYPE_ENUM ="paymentTypeEnum";

	public static final String TABLE_BUSINESS_PARTNER="BusinessPartner";
	public static final String VAR_BUSINESS_PARTNER_ID="businessPartnerId";
	public static final String VAR_BUSINESS_PARTNER_NAME="businessPartnerName";
	public static final String VAR_BUSINESS_PARTNER_RECEIPT_NAME="businessPartnerReceiptName";
//	public static final String VAR_BUSINESS_PARTNER_CODE="businessPartnerCode";
	public static final String VAR_PAYER_ID="payerId";
	public static final String VAR_PAYER_TYPE_ENUM="payerTypeEnum";
	public static final String VAR_PAYMENT_RECIPIENT_ID="paymentRecipientId";
	public static final String VAR_PAYMENT_RECIPIENT_TYPE_ENUM="paymentRecipientTypeEnum";

	public static final String TABLE_APP_USER="AppUser";
	public static final String VAR_APP_USER_ID="appUserId";
	public static final String VAR_APP_USER_NAME="appUserName";
	public static final String VAR_APP_USER_PASSWORD="appUserPassword";
	public static final String VAR_APP_USER_IS_LOGGED_IN ="isLoggedIn";
	public static final String VAR_APP_USER_ACCESS_KEY="accessKey";
	public static final String VAR_APP_USER_REFRESH_TOKEN="refreshToken";
	public static final String VAR_APP_USER_ACCESS_EXPIRATION="accessKeyExpiration";
	public static final String VAR_APP_USER_LAST_UPDATE="lastUpdate";
	public static final String VAR_APP_USER_UPDATE_MODUS_TYPE ="updateModusType";

	public static final String VAR_INVOICE_IMAGE_ID="invoiceImageId";

	public static final String TABLE_INVOICE_FAILURE = "InvoiceFailure";
	public static final String VAR_INVOICE_FAILURE_ID="invoiceFailureId";
	public static final String VAR_INVOICE_FAILURE_TYPE_ENUM="invoiceFailureTypeEnum";

	public static final String TABLE_COST_DISTRIBUTION="CostDistribution";
	public static final String VAR_COST_DISTRIBUTION_ID="costDistributionId";
	public static final String VAR_COST_DISTRIBUTION_NAME="costDistribution";

	public static final String TABLE_COST_DISTRIBUTION_ITEM="CostDistributionItem";
	public static final String VAR_COST_DISTRIBUTION_ITEM_ID="costDistributionItemId";
	public static final String VAR_COST_DISTRIBUTION_ITEM_COST_DISTRIBUTION_ITEM_TYPE_ENUM="costDistributionItemTypeEnum";
	public static final String VAR_COST_DISTRIBUTION_ITEM_VALUE="costDistributionItemValue";
	public static final String VAR_COST_DISTRIBUTION_ITEM_MONEY_VALUE="costDistributionItemMoneyValue";
	public static final String VAR_COST_DISTRIBUTION_ITEM_COST_PAID ="costDistributionItemCostPaid";
	public static final String VAR_COST_DISTRIBUTION_ITEM_PAYMENT_PERSON_TYPE_ENUM="costDistributionItemPaymentPersonTypeEnum";
	public static final String VAR_COST_DISTRIBUTION_ITEM_POSITION="costDistributionItemPosition";
	public static final String VAR_COST_DISTRIBUTION_ITEM_REMARKS ="costDistributionItemRemarks";
	public static final String VAR_COST_DISTRIBUTION_ITEM_CORRECTION_STATUS ="costDistributionItemCorrectionStatus";
	public static final String VAR_IS_TEMPORARY="isTemporary";

	public static final String TABLE_INVOICE_CATEGORY="InvoiceCategory";
	public static final String VAR_INVOICE_CATEGORY_ID="invoiceCategoryId";
	public static final String VAR_PARENT_INVOICE_CATEGORY_ID="parentInvoiceCategoryId";
	public static final String VAR_INVOICE_CATEGORY_NAME="invoiceCategoryName";
	public static final String VAR_INVOICE_CATEGORY_TYPE="invoiceCategoryType";

	public static final String TABLE_USER_CONTACT="UserContact";
	public static final String VAR_USER_CONTACT_ID="userContactId";
	public static final String VAR_CONTACT_NAME="contactName";
	public static final String VAR_APP_USER_CONTACT_ID="appUserContactId";
	public static final String VAR_EMAIL="email";
	public static final String VAR_IS_PROJECT="isProject";

	public static final String TABLE_BASIC_DATA="BasicData";
	public static final String VAR_BASIC_DATA_ID="basicDataId";
	public static final String VAR_BASIC_DATA_TYPE="basicDataType";
	public static final String VAR_BASIC_DATA_SUB_TYPE="basicDataSubType";
	public static final String VAR_BASIC_DATA_VALUE="value";
	public static final String VAR_BASIC_DATA_NUMBER_VALUE="numberValue";
	public static final String VAR_BASIC_DATA_OBJECT_1_CLASS="object1Class";
	public static final String VAR_BASIC_DATA_OBJECT_1_ID="object1Id";
	public static final String VAR_BASIC_DATA_OBJECT_2_CLASS="object2Class";
	public static final String VAR_BASIC_DATA_OBJECT_2_ID="object2Id";

	public static final String VAR_CREATED_BY_ID ="createdById";
	public static final String VAR_BASIC_STATUS_ENUM="basicStatusEnum";
		
	public static final String DB_NAME="nextbill";
	public static final int DB_VERSION=10;

	private static MainDatabaseHandler mInstance = null;

	private MainDatabaseHandler(Context context) {
		super(context, DB_NAME + ".db", null, DB_VERSION);
	}

	public static MainDatabaseHandler getInstance(Context ctx) {

		if (mInstance == null) {
			mInstance = new MainDatabaseHandler(ctx.getApplicationContext());
		}
		return mInstance;
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		System.out.println("Creating SQLite Database");
		String sqlString;
		sqlString="CREATE TABLE "+TABLE_INVOICE+
				" ( "+VAR_ID+" INTEGER PRIMARY KEY,"+
				VAR_INVOICE_ID+" TEXT," +
				VAR_SUM_OF_INVOICE+" REAL," +
				VAR_DATE_OF_INVOICE+" LONG,"+
				VAR_PAYER_ID+" TEXT,"+
				VAR_PAYER_TYPE_ENUM+" TEXT,"+
				VAR_PAYMENT_RECIPIENT_ID+" TEXT,"+
				VAR_PAYMENT_RECIPIENT_TYPE_ENUM+" TEXT,"+
				VAR_INVOICE_IMAGE_ID+" TEXT,"+
				VAR_INVOICE_INVOICE_FAILURE_MESSAGE+" TEXT,"+
				VAR_CREATED_BY_ID +" TEXT,"+
				VAR_INVOICE_CATEGORY_ID+" TEXT," +
				VAR_INVOICE_COST_PAID +" REAL,"+
				VAR_INVOICE_CORRECTION_STATUS +" TEXT," +
				VAR_INVOICE_SPECIAL_TYPE+" INTEGER," +
				VAR_INVOICE_REPETITION_TYPE+" TEXT," +
				VAR_INVOICE_SOURCE+" TEXT," +
				VAR_INVOICE_REMARKS+" TEXT," +
				VAR_ARTICLES_DTOS +" TEXT," +
				VAR_PAYMENT_TYPE_ENUM +" TEXT," +
				VAR_INVOICE_STANDING_ORDER_INVOICE_TEMPLATE_ID + " TEXT,"+
				VAR_INVOICE_STANDING_ORDER_START_DATE+" TEXT,"+
				VAR_INVOICE_STATUS_ENUM + " TEXT NOT NULL)";
		db.execSQL(sqlString);

		sqlString="CREATE TABLE "+TABLE_BUSINESS_PARTNER+
				" ( "+VAR_ID+" INTEGER PRIMARY KEY,"+
				VAR_BUSINESS_PARTNER_ID+" TEXT," +
				VAR_APP_USER_ID+" TEXT," +
				VAR_BUSINESS_PARTNER_RECEIPT_NAME+" TEXT," +
				VAR_BASIC_STATUS_ENUM+" TEXT," +
				VAR_BUSINESS_PARTNER_NAME+" TEXT," +
//				VAR_BUSINESS_PARTNER_CODE+" TEXT NOT NULL," +
				VAR_INVOICE_CATEGORY_ID+" TEXT)";
		db.execSQL(sqlString);

		sqlString="CREATE TABLE "+TABLE_APP_USER+
				" ( "+VAR_ID+" INTEGER PRIMARY KEY,"+
				VAR_APP_USER_ID+" TEXT," +
				VAR_APP_USER_NAME+" TEXT," +
				VAR_EMAIL+" TEXT," +
				VAR_RIGHTS_DTOS +" TEXT," +
				VAR_APP_USER_PASSWORD+" TEXT," +
				VAR_APP_USER_LAST_UPDATE+" TEXT," +
				VAR_APP_USER_IS_LOGGED_IN +" INTEGER," +
				VAR_APP_USER_ACCESS_EXPIRATION+" TEXT," +
				VAR_APP_USER_REFRESH_TOKEN+" TEXT," +
				VAR_APP_USER_UPDATE_MODUS_TYPE +" TEXT," +
				VAR_APP_USER_ACCESS_KEY+" TEXT)";
		db.execSQL(sqlString);

		sqlString="CREATE TABLE "+TABLE_INVOICE_CATEGORY+
				" ( "+VAR_ID+" INTEGER PRIMARY KEY,"+
				VAR_INVOICE_CATEGORY_ID+" TEXT," +
				VAR_APP_USER_ID+" TEXT," +
				VAR_PARENT_INVOICE_CATEGORY_ID+" TEXT," +
				VAR_INVOICE_CATEGORY_TYPE+" TEXT," +
				VAR_BASIC_STATUS_ENUM+" TEXT," +
				VAR_INVOICE_CATEGORY_NAME+" TEXT)";
		db.execSQL(sqlString);

		sqlString="CREATE TABLE "+TABLE_INVOICE_FAILURE+
				" ( "+VAR_ID+" INTEGER PRIMARY KEY,"+
				VAR_INVOICE_FAILURE_ID+" TEXT," +
				VAR_INVOICE_FAILURE_TYPE_ENUM+" TEXT," +
				VAR_INVOICE_ID + " TEXT)";
		db.execSQL(sqlString);

		sqlString="CREATE TABLE "+TABLE_COST_DISTRIBUTION_ITEM+
				" ( "+VAR_ID+" INTEGER PRIMARY KEY,"+
				VAR_COST_DISTRIBUTION_ITEM_COST_DISTRIBUTION_ITEM_TYPE_ENUM+" TEXT," +
				VAR_COST_DISTRIBUTION_ITEM_ID+" TEXT," +
				VAR_COST_DISTRIBUTION_ITEM_VALUE+" TEXT," +
				VAR_COST_DISTRIBUTION_ITEM_MONEY_VALUE+" REAL," +
				VAR_COST_DISTRIBUTION_ITEM_COST_PAID +" REAL,"+
				VAR_PAYER_ID+" TEXT,"+
				VAR_COST_DISTRIBUTION_ITEM_PAYMENT_PERSON_TYPE_ENUM+" TEXT,"+
				VAR_COST_DISTRIBUTION_ITEM_POSITION+" INTEGER,"+
				VAR_IS_TEMPORARY+" TEXT,"+
				VAR_COST_DISTRIBUTION_ID+" TEXT,"+
				VAR_COST_DISTRIBUTION_ITEM_REMARKS +" TEXT,"+
				VAR_COST_DISTRIBUTION_ITEM_CORRECTION_STATUS +" TEXT," +
				VAR_ARTICLES_DTOS +" TEXT," +
				VAR_INVOICE_ID+" TEXT)";
		db.execSQL(sqlString);

		sqlString="CREATE TABLE "+TABLE_COST_DISTRIBUTION+
				" ( "+VAR_ID+" INTEGER PRIMARY KEY,"+
				VAR_COST_DISTRIBUTION_ID+" TEXT," +
				VAR_COST_DISTRIBUTION_NAME+" TEXT," +
				VAR_CREATED_BY_ID +" TEXT)";
		db.execSQL(sqlString);

		sqlString="CREATE TABLE "+TABLE_USER_CONTACT+
				" ( "+VAR_ID+" INTEGER PRIMARY KEY,"+
				VAR_USER_CONTACT_ID+" TEXT," +
				VAR_CONTACT_NAME+" TEXT," +
				VAR_APP_USER_ID+" TEXT," +
				VAR_APP_USER_CONTACT_ID+" TEXT," +
				VAR_BASIC_STATUS_ENUM+" TEXT," +
				VAR_IS_PROJECT+" INTEGER," +
				VAR_EMAIL +" TEXT)";
		db.execSQL(sqlString);

		sqlString="CREATE TABLE "+TABLE_BASIC_DATA+
				" ( "+VAR_ID+" INTEGER PRIMARY KEY,"+
				VAR_BASIC_DATA_ID+" TEXT," +
				VAR_BASIC_DATA_TYPE+" TEXT," +
				VAR_BASIC_DATA_SUB_TYPE+" TEXT," +
				VAR_APP_USER_ID+" TEXT," +
				VAR_BASIC_DATA_OBJECT_1_CLASS+" TEXT," +
				VAR_BASIC_DATA_OBJECT_1_ID+" TEXT," +
				VAR_BASIC_DATA_OBJECT_2_CLASS+" TEXT," +
				VAR_BASIC_DATA_OBJECT_2_ID+" TEXT," +
				VAR_BASIC_DATA_VALUE+" TEXT," +
				VAR_BASIC_DATA_NUMBER_VALUE+" REAL)";
		db.execSQL(sqlString);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		db.execSQL("DROP TABLE IF EXISTS "+TABLE_INVOICE);
		db.execSQL("DROP TABLE IF EXISTS "+TABLE_BUSINESS_PARTNER);
		db.execSQL("DROP TABLE IF EXISTS "+TABLE_APP_USER);
		db.execSQL("DROP TABLE IF EXISTS "+TABLE_INVOICE_CATEGORY);
		db.execSQL("DROP TABLE IF EXISTS "+TABLE_INVOICE_FAILURE);
		db.execSQL("DROP TABLE IF EXISTS "+TABLE_COST_DISTRIBUTION_ITEM);
		db.execSQL("DROP TABLE IF EXISTS "+TABLE_COST_DISTRIBUTION);
		db.execSQL("DROP TABLE IF EXISTS "+TABLE_USER_CONTACT);
		db.execSQL("DROP TABLE IF EXISTS "+TABLE_BASIC_DATA);
		onCreate(db);
	}

	public Long insertUpdateInvoice(Invoice newInvoice){
		List<Invoice> invoices = findInvoices(MainDatabaseHandler.VAR_INVOICE_ID, newInvoice.getInvoiceId().toString());

		Long rowId = null;

		if (!invoices.isEmpty()){
			rowId = updateInvoice(newInvoice);
		}else{
			rowId = insertInvoice(newInvoice);
		}

		if (newInvoice.getCostDistributionItemDTOs() != null && !newInvoice.getCostDistributionItemDTOs().isEmpty()){
			List<CostDistributionItem> costDistributionItems = newInvoice.getCostDistributionItemDTOs();
			if (!costDistributionItems.isEmpty()){
				List<CostDistributionItem> costDistributionItems1 = findCostDistributionItems(MainDatabaseHandler.VAR_INVOICE_ID, newInvoice.getInvoiceId().toString());
				deleteCostDistributionItems(costDistributionItems1);
			}

			for (CostDistributionItem costDistributionItem : costDistributionItems) {

				List<CostDistributionItem> costDistributionItems1 = findCostDistributionItems(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ITEM_ID, costDistributionItem.getCostDistributionItemId().toString());

				if (!costDistributionItems1.isEmpty()){
					updateCostDistributionItem(costDistributionItem);
				}else{
					insertCostDistributionItem(costDistributionItem);
				}
			}

		}

		InvoiceCategory invoiceCategory = newInvoice.getInvoiceCategoryDTO();
		if (invoiceCategory != null){
			SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE_CATEGORY);
			sqlBuilder.isEqual(MainDatabaseHandler.VAR_INVOICE_CATEGORY_ID, invoiceCategory.getInvoiceCategoryId().toString());

			List<InvoiceCategory> invoiceFailures1 = findInvoiceCategoriesSql(sqlBuilder);

			if (!invoiceFailures1.isEmpty()){
				updateInvoiceCategory(invoiceCategory);
			}else{
				insertInvoiceCategory(invoiceCategory);
			}

		}

		PaymentPersonDTO paymentRecipientDTO = newInvoice.getPaymentRecipientDTO();
		if (paymentRecipientDTO != null){
			BusinessPartner businessPartner = new BusinessPartner();
			businessPartner.setBusinessPartnerName(paymentRecipientDTO.getPaymentPersonName());
			businessPartner.setBusinessPartnerReceiptName(paymentRecipientDTO.getPaymentPersonName());
			businessPartner.setBusinessPartnerId(paymentRecipientDTO.getPaymentPersonId());
			businessPartner.setBasicStatusEnum(BasicStatusEnum.OK);
			updateBusinessPartner(businessPartner);
		}

		return rowId;
	}

	public Long insertInvoice(Invoice invoice) {

		SQLiteDatabase db = getWritableDatabase();
		Long newId = null;

		try{

			ContentValues contentValues = generateContentValuesForInvoice(invoice);

			newId = db.insert(TABLE_INVOICE, null, contentValues);

		}catch(Exception e){
			e.printStackTrace();
		}finally {

		}

		return newId;
	}

	public void insertInvoices(List<Invoice> invoices) {

		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();

		try {
			for (Invoice invoice : invoices) {  // loop through your records
				ContentValues contentValues = generateContentValuesForInvoice(invoice);
				db.insert(TABLE_INVOICE, null, contentValues);
			}

			db.setTransactionSuccessful();
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			db.endTransaction();
		}
	}

	public ContentValues generateContentValuesForInvoice(Invoice invoice){

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		ContentValues values = new ContentValues();

		if (invoice.getId() != null){
			values.put(VAR_ID, invoice.getId());
		}
		if (invoice.getSumOfInvoice() != null){
			values.put(VAR_SUM_OF_INVOICE, invoice.getSumOfInvoice().doubleValue());
		}
		if (invoice.getDateOfInvoice() != null){
			values.put(VAR_DATE_OF_INVOICE, invoice.getDateOfInvoice().getTime());
		}
		if (invoice.getInvoiceStatusEnum() != null){
			values.put(VAR_INVOICE_STATUS_ENUM, invoice.getInvoiceStatusEnum().toString());
		}
		if (invoice.getInvoiceId() != null){
			values.put(VAR_INVOICE_ID, invoice.getInvoiceId().toString());
		}
		if (invoice.getInvoiceFailureMessage() != null){
			values.put(VAR_INVOICE_INVOICE_FAILURE_MESSAGE, invoice.getInvoiceFailureMessage());
		}
		if (invoice.getPayerId() != null){
			values.put(VAR_PAYER_ID, invoice.getPayerId().toString());
		}
		if (invoice.getPayerTypeEnum() != null){
			values.put(VAR_PAYER_TYPE_ENUM, invoice.getPayerTypeEnum().toString());
		}
		if (invoice.getPaymentRecipientId() != null){
			values.put(VAR_PAYMENT_RECIPIENT_ID, invoice.getPaymentRecipientId().toString());
		}
		if (invoice.getPaymentRecipientTypeEnum() != null){
			values.put(VAR_PAYMENT_RECIPIENT_TYPE_ENUM, invoice.getPaymentRecipientTypeEnum().toString());
		}
		if (invoice.getCreatedById() != null){
			values.put(VAR_CREATED_BY_ID, invoice.getCreatedById().toString());
		}
		if (invoice.getInvoiceImageId() != null){
			values.put(VAR_INVOICE_IMAGE_ID, invoice.getInvoiceImageId().toString());
		}
		if (invoice.getStandingOrderInvoiceTemplateId() != null){
			values.put(VAR_INVOICE_STANDING_ORDER_INVOICE_TEMPLATE_ID, invoice.getStandingOrderInvoiceTemplateId().toString());
		}
		if (invoice.getStandingOrderStartDate() != null){
			values.put(VAR_INVOICE_STANDING_ORDER_START_DATE, dateFormat.format(invoice.getStandingOrderStartDate()));
		}
		if (invoice.getInvoiceCategoryDTO() != null){
			values.put(VAR_INVOICE_CATEGORY_ID, invoice.getInvoiceCategoryDTO().getInvoiceCategoryId().toString());
		}
		if (invoice.getSpecialType() != null){
			Boolean isSpecialType = invoice.getSpecialType();
			Integer isSpecialTypeNumber = 0;
			if (isSpecialType != null && isSpecialType == true){
				isSpecialTypeNumber = 1;
			}
			values.put(VAR_INVOICE_SPECIAL_TYPE, isSpecialTypeNumber);
		}
		if (invoice.getRemarks() != null){
			values.put(VAR_INVOICE_REMARKS, invoice.getRemarks());
		}
		if (invoice.getRepetitionTypeEnum() != null){
			values.put(VAR_INVOICE_REPETITION_TYPE, invoice.getRepetitionTypeEnum().name());
		}
		if (invoice.getPaymentTypeEnum() != null){
			values.put(VAR_PAYMENT_TYPE_ENUM, invoice.getPaymentTypeEnum().name());
		}
		if (invoice.getInvoiceSource() != null){
			values.put(VAR_INVOICE_SOURCE, invoice.getInvoiceSource().name());
		}
		if (invoice.getCostPaid() != null){
			values.put(VAR_INVOICE_COST_PAID, invoice.getCostPaid().doubleValue());
		}
		if (invoice.getCorrectionStatus() != null){
			values.put(VAR_INVOICE_CORRECTION_STATUS, invoice.getCorrectionStatus().toString());
		}
		if (invoice.getArticleDTOs() != null) {
			Gson gson = new GsonBuilder().create();
			Type listType = new TypeToken<List<ArticleDTO>>() {}.getType();

			String jsonString = gson.toJson(invoice.getArticleDTOs(), listType);
			values.put(VAR_ARTICLES_DTOS, jsonString);
		}

		return values;
	}

	public Long insertUpdateBasicData(BasicData basicData) {

		SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
		sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, basicData.getBasicDataType().name())
				.and()
				.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_OBJECT_1_ID, basicData.getObject1Id())
				.and()
				.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_OBJECT_1_CLASS, basicData.getObject1Class());

		List<BasicData> basicDatas = findBasicDatasSql(sqlBuilder);

		if (!basicDatas.isEmpty()){
			return updateBasicData(basicData);
		}else{
			return insertBasicData(basicData);
		}
	}
	public Long insertBasicData(BasicData basicData) {

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		SQLiteDatabase db = getWritableDatabase();
		Long newId = null;

		try{

			ContentValues values = new ContentValues();
			if (basicData.getId() != null){
				values.put(VAR_ID, basicData.getId());
			}
			if (basicData.getBasicDataId() != null){
				values.put(VAR_BASIC_DATA_ID, basicData.getBasicDataId().toString());
			}
			if (basicData.getNumberValue() != null){
				values.put(VAR_BASIC_DATA_NUMBER_VALUE, basicData.getNumberValue().doubleValue());
			}
			if (basicData.getBasicDataType() != null){
				values.put(VAR_BASIC_DATA_TYPE, basicData.getBasicDataType().toString());
			}
			if (basicData.getBasicDataSubType() != null){
				values.put(VAR_BASIC_DATA_SUB_TYPE, basicData.getBasicDataSubType().toString());
			}
			if (basicData.getAppUserId() != null){
				values.put(VAR_APP_USER_ID, basicData.getAppUserId().toString());
			}
			if (basicData.getAppUserId() != null){
				values.put(VAR_APP_USER_ID, basicData.getAppUserId().toString());
			}
			if (basicData.getObject1Id() != null){
				values.put(VAR_BASIC_DATA_OBJECT_1_ID, basicData.getObject1Id());
			}
			if (basicData.getObject1Class() != null){
				values.put(VAR_BASIC_DATA_OBJECT_1_CLASS, basicData.getObject1Class());
			}
			if (basicData.getObject2Id() != null){
				values.put(VAR_BASIC_DATA_OBJECT_2_ID, basicData.getObject2Id());
			}
			if (basicData.getObject2Class() != null){
				values.put(VAR_BASIC_DATA_OBJECT_2_CLASS, basicData.getObject2Class());
			}
			if (basicData.getValue() != null){
				values.put(VAR_BASIC_DATA_VALUE, basicData.getValue());
			}

			newId = db.insert(TABLE_BASIC_DATA, null, values);

		}catch(Exception e){
			e.printStackTrace();
		}finally {

		}

		return newId;
	}

	public Long insertAppUser(AppUser appUser) {

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		SQLiteDatabase db = getWritableDatabase();
		Long newId = null;

		try{

			ContentValues values = new ContentValues();
			if (appUser.getId() != null){
				values.put(VAR_ID, appUser.getId());
			}

			if (appUser.getAppUserId() != null){
				values.put(VAR_APP_USER_ID, appUser.getAppUserId().toString());
			}

			if (appUser.getAppUserName() != null){
				values.put(VAR_APP_USER_NAME, appUser.getAppUserName());
			}

			if (appUser.getAppUserPassword() != null){
				values.put(VAR_APP_USER_PASSWORD, appUser.getAppUserPassword());
			}

			if (appUser.getLoggedIn() != null){
				Boolean loggedIn = appUser.getLoggedIn();
				Integer isLoggedInNumber = 0;
				if (loggedIn != null && loggedIn == true){
					isLoggedInNumber = 1;
				}
				values.put(VAR_APP_USER_IS_LOGGED_IN, isLoggedInNumber);
			}

			if (appUser.getAppUserAccessKey() != null){
				values.put(VAR_APP_USER_ACCESS_KEY, appUser.getAppUserAccessKey());
			}

			if (appUser.getAppUserRefreshToken() != null){
				values.put(VAR_APP_USER_REFRESH_TOKEN, appUser.getAppUserRefreshToken());
			}

			if (appUser.getAppUserAccessExpiration() != null){
				values.put(VAR_APP_USER_ACCESS_EXPIRATION, dateFormat.format(appUser.getAppUserAccessExpiration()));
			}

			if (appUser.getEmail() != null){
				values.put(VAR_EMAIL, appUser.getEmail());
			}

			if (appUser.getLastUpdate() != null){
				values.put(VAR_APP_USER_LAST_UPDATE, dateFormat.format(appUser.getLastUpdate()));
			}

			if (appUser.getUpdateModusType() != null) {
				values.put(VAR_APP_USER_UPDATE_MODUS_TYPE, appUser.getUpdateModusType().name());
			}

			if (appUser.getAppRightDTOs() != null) {
				Gson gson = new GsonBuilder().create();
				Type listType = new TypeToken<List<AppRightDTO>>() {}.getType();

				String jsonString = gson.toJson(appUser.getAppRightDTOs(), listType);
				values.put(VAR_RIGHTS_DTOS, jsonString);
			}

			newId = db.insert(TABLE_APP_USER, null, values);

		}catch(Exception e){
			e.printStackTrace();
		}finally {

		}

		return newId;
	}

	public Long insertUserContact(UserContact userContact) {

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		SQLiteDatabase db = getWritableDatabase();
		Long newId = null;

		try{

			ContentValues values = new ContentValues();
			if (userContact.getId() != null){
				values.put(VAR_ID, userContact.getId());
			}

			if (userContact.getUserContactId() != null){
				values.put(VAR_USER_CONTACT_ID, userContact.getUserContactId().toString());
			}

			if (userContact.getAppUserId() != null){
				values.put(VAR_APP_USER_ID, userContact.getAppUserId().toString());
			}

			if (userContact.getAppUserContactId() != null){
				values.put(VAR_APP_USER_CONTACT_ID, userContact.getAppUserContactId().toString());
			}

			if (userContact.getContactName() != null){
				values.put(VAR_CONTACT_NAME, userContact.getContactName());
			}

			if (userContact.getEmail() != null){
				values.put(VAR_EMAIL, userContact.getEmail());
			}

			if (userContact.getBasicStatusEnum() != null){
				values.put(VAR_BASIC_STATUS_ENUM, userContact.getBasicStatusEnum().toString());
			}

			if (userContact.getProject() != null){
				Boolean isProject = userContact.getProject();
				Integer isProjectNumber = 0;
				if (isProject != null && isProject == true){
					isProjectNumber = 1;
				}
				values.put(VAR_IS_PROJECT, isProjectNumber);
			}

			newId = db.insert(TABLE_USER_CONTACT, null, values);

		}catch(Exception e){
			e.printStackTrace();
		}finally {

		}

		return newId;
	}

	public Long insertInvoiceCategory(InvoiceCategory invoiceCategory) {

		SQLiteDatabase db = getWritableDatabase();
		Long newId = null;

		try{
			ContentValues values = new ContentValues();
			if (invoiceCategory.getId() != null){
				values.put(VAR_ID, invoiceCategory.getId());
			}

			if (invoiceCategory.getInvoiceCategoryId() != null){
				values.put(VAR_INVOICE_CATEGORY_ID, invoiceCategory.getInvoiceCategoryId().toString());
			}

			if (invoiceCategory.getInvoiceCategoryName() != null){
				values.put(VAR_INVOICE_CATEGORY_NAME, invoiceCategory.getInvoiceCategoryName());
			}

			if (invoiceCategory.getParentInvoiceCategoryDTO() != null){
				values.put(VAR_PARENT_INVOICE_CATEGORY_ID, invoiceCategory.getParentInvoiceCategoryDTO().getInvoiceCategoryId().toString());
			}

			if (invoiceCategory.getAppUserId() != null){
				values.put(VAR_APP_USER_ID, invoiceCategory.getAppUserId().toString());
			}

			if (invoiceCategory.getBasicStatusEnum() != null){
				values.put(VAR_BASIC_STATUS_ENUM, invoiceCategory.getBasicStatusEnum().toString());
			}

			if (invoiceCategory.getInvoiceCategoryType() != null){
				values.put(VAR_INVOICE_CATEGORY_TYPE, invoiceCategory.getInvoiceCategoryType().toString());
			}

			newId = db.insert(TABLE_INVOICE_CATEGORY, null, values);
		}catch(Exception e){
			e.printStackTrace();
		}finally {

		}

		return newId;
	}

	public Long insertBusinessPartner(BusinessPartner businessPartner) {

		SQLiteDatabase db = getWritableDatabase();
		Long newId = null;

		try{
			ContentValues contentValues = generateContentValuesForBusinessPartner(businessPartner);

			newId = db.insert(TABLE_BUSINESS_PARTNER, null, contentValues);
		}catch(Exception e){
			e.printStackTrace();
		}finally {

		}

		return newId;
	}

	public Long insertInvoiceFailure(InvoiceFailure invoiceFailure) {

		SQLiteDatabase db = getWritableDatabase();
		Long newId = null;
		try{

			ContentValues values = new ContentValues();
			if (invoiceFailure.getId() != null){
				values.put(VAR_ID, invoiceFailure.getId());
			}
			if (invoiceFailure.getInvoiceFailureId() != null){
				values.put(VAR_INVOICE_FAILURE_ID, invoiceFailure.getInvoiceFailureId().toString());
			}
			if (invoiceFailure.getInvoiceFailureTypeEnum() != null){
				values.put(VAR_INVOICE_FAILURE_TYPE_ENUM, invoiceFailure.getInvoiceFailureTypeEnum().toString());
			}
			if (invoiceFailure.getInvoiceId() != null){
				values.put(VAR_INVOICE_ID, invoiceFailure.getInvoiceId().toString());
			}

			newId = db.insert(TABLE_INVOICE_FAILURE, null, values);
		}catch(Exception e){
			e.printStackTrace();
		}finally {

		}

		return newId;
	}

	public Long insertCostDistributionItem(CostDistributionItem costDistributionItem) {

		SQLiteDatabase db = getWritableDatabase();
		Long newId = null;
		try{
			ContentValues contentValues = generateContentValuesForCostDistributionItem(costDistributionItem);

			newId = db.insert(TABLE_COST_DISTRIBUTION_ITEM, null, contentValues);
		}catch(Exception e){
			e.printStackTrace();
		}finally {

		}

		return newId;
	}

	public void insertCostDistributionItems(List<CostDistributionItem> costDistributionItems) {
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();

		try {
			for (CostDistributionItem costDistributionItem : costDistributionItems) {
				ContentValues contentValues = generateContentValuesForCostDistributionItem(costDistributionItem);
				db.insert(TABLE_COST_DISTRIBUTION_ITEM, null, contentValues);
			}

			db.setTransactionSuccessful();
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			db.endTransaction();
		}
	}

	public ContentValues generateContentValuesForCostDistributionItem(CostDistributionItem costDistributionItem){

		ContentValues values = new ContentValues();
		if (costDistributionItem.getId() != null){
			values.put(VAR_ID, costDistributionItem.getId());
		}
		if (costDistributionItem.getCostDistributionItemId() != null){
			values.put(VAR_COST_DISTRIBUTION_ITEM_ID, costDistributionItem.getCostDistributionItemId().toString());
		}
		if (costDistributionItem.getCostDistributionItemTypeEnum() != null){
			values.put(VAR_COST_DISTRIBUTION_ITEM_COST_DISTRIBUTION_ITEM_TYPE_ENUM, costDistributionItem.getCostDistributionItemTypeEnum().toString());
		}
		if (costDistributionItem.getValue() != null){
			values.put(VAR_COST_DISTRIBUTION_ITEM_VALUE, costDistributionItem.getValue().doubleValue());
		}
		if (costDistributionItem.getCostPaid() != null){
			values.put(VAR_COST_DISTRIBUTION_ITEM_COST_PAID, costDistributionItem.getCostPaid().doubleValue());
		}
		if (costDistributionItem.getPayerId() != null){
			values.put(VAR_PAYER_ID, costDistributionItem.getPayerId().toString());
		}
		if (costDistributionItem.getPaymentPersonTypeEnum() != null){
			values.put(VAR_COST_DISTRIBUTION_ITEM_PAYMENT_PERSON_TYPE_ENUM, costDistributionItem.getPaymentPersonTypeEnum().toString());
		}
		if (costDistributionItem.getPosition() != null){
			values.put(VAR_COST_DISTRIBUTION_ITEM_POSITION, costDistributionItem.getPosition());
		}
		if (costDistributionItem.getInvoiceId() != null){
			values.put(VAR_INVOICE_ID, costDistributionItem.getInvoiceId().toString());
		}
		if (costDistributionItem.getCostDistributionId() != null){
			values.put(VAR_COST_DISTRIBUTION_ID, costDistributionItem.getCostDistributionId().toString());
		}
		if (costDistributionItem.getIsTemporary() != null){
			values.put(VAR_IS_TEMPORARY, costDistributionItem.getIsTemporary());
		}
		if (costDistributionItem.getMoneyValue() != null){
			values.put(VAR_COST_DISTRIBUTION_ITEM_MONEY_VALUE, costDistributionItem.getMoneyValue().doubleValue());
		}
		if (costDistributionItem.getRemarks() != null){
			values.put(VAR_COST_DISTRIBUTION_ITEM_REMARKS, costDistributionItem.getRemarks());
		}
		if (costDistributionItem.getCorrectionStatus() != null){
			values.put(VAR_COST_DISTRIBUTION_ITEM_CORRECTION_STATUS, costDistributionItem.getCorrectionStatus().toString());
		}
		if (costDistributionItem.getArticleDTOs() != null) {
			Gson gson = new GsonBuilder().create();
			Type listType = new TypeToken<List<ArticleDTO>>() {}.getType();
			String jsonString = gson.toJson(costDistributionItem.getArticleDTOs(), listType);
			values.put(VAR_ARTICLES_DTOS, jsonString);
		}

		return values;

	}

	public void insertBusinessPartners(List<BusinessPartner> businessPartners) {
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();

		try {
			for (BusinessPartner businessPartner : businessPartners) {
				ContentValues contentValues = generateContentValuesForBusinessPartner(businessPartner);
				db.insert(TABLE_BUSINESS_PARTNER, null, contentValues);
			}

			db.setTransactionSuccessful();
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			db.endTransaction();
		}
	}

	public ContentValues generateContentValuesForBusinessPartner(BusinessPartner businessPartner){

		ContentValues values = new ContentValues();

		if (businessPartner.getBusinessPartnerId() != null){
			values.put(VAR_BUSINESS_PARTNER_ID, businessPartner.getBusinessPartnerId().toString());
		}

		if (businessPartner.getBusinessPartnerName() != null){
			values.put(VAR_BUSINESS_PARTNER_NAME, businessPartner.getBusinessPartnerName());
		}

		if (businessPartner.getBusinessPartnerReceiptName() != null){
			values.put(VAR_BUSINESS_PARTNER_RECEIPT_NAME, businessPartner.getBusinessPartnerReceiptName());
		}

		if (businessPartner.getBasicStatusEnum() != null){
			values.put(VAR_BASIC_STATUS_ENUM, businessPartner.getBasicStatusEnum().toString());
		}

		if (businessPartner.getAppUserId() != null){
			values.put(VAR_APP_USER_ID, businessPartner.getAppUserId().toString());
		}

		return values;

	}

	public Long insertCostDistribution(CostDistribution costDistribution) {

		SQLiteDatabase db = getWritableDatabase();
		Long newId = null;
		try{

			ContentValues values = new ContentValues();
			if (costDistribution.getId() != null){
				values.put(VAR_ID, costDistribution.getId());
			}
			if (costDistribution.getCostDistributionId() != null){
				values.put(VAR_COST_DISTRIBUTION_ID, costDistribution.getCostDistributionId().toString());
			}
			if (costDistribution.getName() != null){
				values.put(VAR_COST_DISTRIBUTION_NAME, costDistribution.getName());
			}
			if (costDistribution.getCreatedById() != null){
				values.put(VAR_CREATED_BY_ID, costDistribution.getCreatedById().toString());
			}

			newId = db.insert(TABLE_COST_DISTRIBUTION, null, values);
		}catch(Exception e){
			e.printStackTrace();
		}finally {

		}

		return newId;
	}

	public IPaymentPerson insertNewPaymentPersonAndAddStatusHandlerObject(Context context, IPaymentPerson paymentPerson){

		StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(context);

		if (PaymentPersonTypeEnum.NEW_BUSINESS_PARTNER.equals(paymentPerson.getPaymentPersonEnum())){

			BusinessPartner businessPartner = new BusinessPartner();
			businessPartner.setBusinessPartnerName(paymentPerson.getPaymentPersonName());
			businessPartner.setBusinessPartnerReceiptName(paymentPerson.getPaymentPersonName());
			insertBusinessPartner(businessPartner);

			handler.addObject(businessPartner.getBusinessPartnerId().toString(), StatusDatabaseHandler.OBJECT_TYPE_BUSINESS_PARTNER, StatusDatabaseHandler.UPDATE_STATUS_ADD, new Date().getTime(), 1);

			paymentPerson = businessPartner;

		}

		return paymentPerson;
	}

	public Long updateAppUser(AppUser appUser) {

		if (appUser != null){
			List<AppUser> tmpAppUser = new ArrayList<AppUser>();
			tmpAppUser.add(appUser);
			deleteAppUsers(new ArrayList<AppUser>(tmpAppUser));
		}

		Long id = insertAppUser(appUser);
		return id;
	}

	public Long updateBasicData(BasicData basicData) {

		if (basicData != null){
			List<BasicData> basicDatas = new ArrayList<>();
			basicDatas.add(basicData);
			deleteBasicDatas(new ArrayList<BasicData>(basicDatas));
		}

		Long id = insertBasicData(basicData);
		return id;
	}

	public Long updateBusinessPartner(BusinessPartner businessPartner) {

		if (businessPartner != null){
			List<BusinessPartner> tmpBusinessPartner = new ArrayList<>();
			tmpBusinessPartner.add(businessPartner);
			deleteBusinessPartners(new ArrayList<BusinessPartner>(tmpBusinessPartner));
		}

		Long id = insertBusinessPartner(businessPartner);
		return id;
	}

	public Long updateCostDistributionItem(CostDistributionItem costDistributionItem) {

		if (costDistributionItem != null){
			List<CostDistributionItem> tmpCostDistributionItem = new ArrayList<>();
			tmpCostDistributionItem.add(costDistributionItem);
			deleteCostDistributionItems(new ArrayList<CostDistributionItem>(tmpCostDistributionItem));
		}

		Long id = insertCostDistributionItem(costDistributionItem);
		return id;
	}

	public Long updatePaymentItems(List<IPaymentItem> paymentItems) {

		Long id = null;

		if (paymentItems != null){

			for (IPaymentItem paymentItem : paymentItems) {
				if (paymentItem instanceof CostDistributionItem){
					CostDistributionItem costDistributionItem = (CostDistributionItem) paymentItem;

					List<CostDistributionItem> tmpCostDistributionItem = new ArrayList<>();
					tmpCostDistributionItem.add(costDistributionItem);
					deleteCostDistributionItems(new ArrayList<>(tmpCostDistributionItem));
					id = insertCostDistributionItem(costDistributionItem);
				}else if (paymentItem instanceof Invoice){
					Invoice invoice = (Invoice) paymentItem;

					List<Invoice> tmpInvoices = new ArrayList<>();
					tmpInvoices.add(invoice);
					deleteInvoices(new ArrayList<>(tmpInvoices));
					id = insertInvoice(invoice);
				}
			}
		}

		return id;
	}

	public Long updateInvoice(Invoice invoice) {
		List<Invoice> tmpInvoice = new ArrayList<Invoice>();
		tmpInvoice.add(invoice);
		deleteInvoices(new ArrayList<Invoice>(tmpInvoice));
		Long id = insertInvoice(invoice);
		return id;
	}

	public Long updateInvoiceFailure(InvoiceFailure invoiceFailure) {
		List<InvoiceFailure> tmpInvoice = new ArrayList<>();
		tmpInvoice.add(invoiceFailure);
		deleteInvoiceFailures(new ArrayList<InvoiceFailure>(tmpInvoice));
		Long id = insertInvoiceFailure(invoiceFailure);
		return id;
	}

	public Long updateInvoiceCategory(InvoiceCategory invoiceCategory) {
		List<InvoiceCategory> tmpInvoiceCategories = new ArrayList<>();
		tmpInvoiceCategories.add(invoiceCategory);
		deleteInvoiceCategories(new ArrayList<InvoiceCategory>(tmpInvoiceCategories));
		Long id = insertInvoiceCategory(invoiceCategory);
		return id;
	}

	public Long updateUserContact(UserContact userContact) {
		List<UserContact> tmpUserContacts = new ArrayList<>();
		tmpUserContacts.add(userContact);
		deleteUserContacts(new ArrayList<UserContact>(tmpUserContacts));
		Long id = insertUserContact(userContact);
		return id;
	}

	public Long updateCostDistribution(CostDistribution costDistribution) {
		List<CostDistribution> tmpCostDistribution = new ArrayList<>();
		tmpCostDistribution.add(costDistribution);
		deleteCostDistributions(new ArrayList<CostDistribution>(tmpCostDistribution));
		Long id = insertCostDistribution(costDistribution);
		return id;
	}

	public void deleteInvoices(List<Invoice> invoices)
	{
		SQLiteDatabase db = getWritableDatabase();

		try{
			if (invoices == null){
				db.delete(TABLE_INVOICE, null,null);
			}else{
				for (Invoice tmpInvoices:invoices) {
					db.delete(TABLE_INVOICE, VAR_INVOICE_ID+"=?", new String[]{tmpInvoices.getInvoiceId().toString()});
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally {

		}

	}

	public void deleteInvoicesByInternalId(List<Invoice> invoices)
	{
		SQLiteDatabase db = getWritableDatabase();

		try{
			if (invoices == null){
				db.delete(TABLE_INVOICE, null,null);
			}else{
				for (Invoice tmpInvoices:invoices) {
					Long internalId = tmpInvoices.getId();
					if (internalId != null){
						db.delete(TABLE_INVOICE, VAR_ID+"=?", new String[]{String.valueOf(internalId)});
					}

				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally {

		}

	}

	public void deleteUserContacts(List<UserContact> userContacts)
	{
		SQLiteDatabase db = getWritableDatabase();

		try{
			if (userContacts == null){
				db.delete(TABLE_USER_CONTACT, null,null);
			}else{
				for (UserContact tmpUserContact:userContacts) {
					db.delete(TABLE_USER_CONTACT, VAR_USER_CONTACT_ID+"=?", new String[]{tmpUserContact.getUserContactId().toString()});
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally {

		}

	}

	public void deleteCostDistributions(List<CostDistribution> costDistributions)
	{
		SQLiteDatabase db = getWritableDatabase();

		try{
			if (costDistributions == null){
				db.delete(TABLE_COST_DISTRIBUTION, null,null);
			}else{
				for (CostDistribution costDistributionTmp : costDistributions) {
					db.delete(TABLE_COST_DISTRIBUTION, VAR_COST_DISTRIBUTION_ID+"=?", new String[]{costDistributionTmp.getCostDistributionId().toString()});
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally {

		}

	}

	public void deleteInvoiceFailures(List<InvoiceFailure> invoiceFailures)
	{
		SQLiteDatabase db = getWritableDatabase();

		try{

			if (invoiceFailures == null){
				db.delete(TABLE_INVOICE_FAILURE, null,null);
			}else{
				for (InvoiceFailure tmpInvoiceFailures:invoiceFailures) {
					db.delete(TABLE_INVOICE_FAILURE, VAR_INVOICE_FAILURE_ID+"=?", new String[]{tmpInvoiceFailures.getInvoiceFailureId().toString()});
				}
			}

		}catch(Exception e){
			e.printStackTrace();
		}finally {

		}
	}

	public void deleteInvoiceCategories(List<InvoiceCategory> invoiceCategories)
	{
		SQLiteDatabase db = getWritableDatabase();

		try{

			if (invoiceCategories == null){
				db.delete(TABLE_INVOICE_CATEGORY, null,null);
			}else{
				for (InvoiceCategory tmpInvoiceCategory:invoiceCategories) {
					db.delete(TABLE_INVOICE_CATEGORY, VAR_INVOICE_CATEGORY_ID+"=?", new String[]{tmpInvoiceCategory.getInvoiceCategoryId().toString()});
				}
			}

		}catch(Exception e){
			e.printStackTrace();
		}finally {

		}

	}

	public void deleteBusinessPartners(List<BusinessPartner> businessPartners)
	{
		SQLiteDatabase db = getWritableDatabase();

		try{

			if (businessPartners == null){
				db.delete(TABLE_BUSINESS_PARTNER, null,null);
			}else{
				for (BusinessPartner tmpBusinessPartner : businessPartners) {
					db.delete(TABLE_BUSINESS_PARTNER, VAR_BUSINESS_PARTNER_ID+"=?", new String[]{tmpBusinessPartner.getBusinessPartnerId().toString()});
				}
			}

		}catch(Exception e){
			e.printStackTrace();
		}finally {

		}

	}

	public void deleteAppUsers(List<AppUser> appUsers)
	{

		SQLiteDatabase db = getWritableDatabase();

		try{

			if (appUsers == null){
				db.delete(TABLE_APP_USER, null,null);
			}else{
				for (AppUser tmpAppUser:appUsers) {
					if (tmpAppUser.getAppUserId() != null){
						db.delete(TABLE_APP_USER, VAR_APP_USER_ID+"=?", new String[]{tmpAppUser.getAppUserId().toString()});
					}
				}
			}

		}catch(Exception e){
			e.printStackTrace();
		}finally {

		}

	}

	public void deleteBasicDatas(List<BasicData> basicDatas) {

		SQLiteDatabase db = getWritableDatabase();
		try{
			if (basicDatas == null){
				db.delete(TABLE_BASIC_DATA, null,null);
			}else{
				for (BasicData tmpBasicData:basicDatas) {
					if (tmpBasicData.getBasicDataId() != null){
						db.delete(TABLE_BASIC_DATA, VAR_BASIC_DATA_ID+"=?", new String[]{tmpBasicData.getBasicDataId().toString()});
					}
				}
			}

		}catch(Exception e){
			e.printStackTrace();
		}finally {

		}
	}

	public void deleteCostDistributionItems(List<CostDistributionItem> costDistributionItem)
	{
		SQLiteDatabase db = getWritableDatabase();

		try{
			if (costDistributionItem == null){
				db.delete(TABLE_COST_DISTRIBUTION_ITEM, null,null);
			}else{
				for (CostDistributionItem tmpCostDistributionItem:costDistributionItem) {
					db.delete(TABLE_COST_DISTRIBUTION_ITEM, VAR_COST_DISTRIBUTION_ITEM_ID+"=?", new String[]{tmpCostDistributionItem.getCostDistributionItemId().toString()});
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally {

		}

	}

	public List<Invoice> findInvoices(String rowName, String value){
		return findInvoices(rowName, null, value);
	}

	public String createWhereClause(String table, String rowName, SQLOperatorEnum sqlOperatorEnum, List<String> values){

		String resultString = "";

		if (values.isEmpty()){
			resultString = "SELECT * FROM " + table;
		}else{

			String sqlOperatorEnumString  = null;
			if (SQLOperatorEnum.NOT.equals(sqlOperatorEnum)){
				sqlOperatorEnumString = " NOT " +rowName + "=?";
			}else{
				if (sqlOperatorEnum == null){
					sqlOperatorEnum = SQLOperatorEnum.EQUAL;
				}
				sqlOperatorEnumString = rowName + sqlOperatorEnum.getDisplayName();
			}

			String tmpResultString = sqlOperatorEnumString;
			if (values.size() > 1){
				for (int i = 1; i < values.size(); i++) {
					tmpResultString = tmpResultString + " and " + sqlOperatorEnumString;
				}
			}


			resultString = "SELECT * FROM " + table+ " WHERE " + tmpResultString;
		}

		resultString = resultString + ";";

		return resultString;
	}

	public List<Invoice> findInvoices(String rowName, SQLOperatorEnum sqlOperatorEnum, String value){
		String resultSqlString = createWhereClause(TABLE_INVOICE, rowName, sqlOperatorEnum, Collections.singletonList(value));
		return findInvoicesSql(resultSqlString, value != null ? Collections.singletonList(value) : new ArrayList<String>());
	}

	public List<Invoice> findInvoicesSql(SqlBuilder sqlBuilder){
		String sqlString = sqlBuilder.getSqlString();
		List<String> values = sqlBuilder.getValuesAsList();

		return findInvoicesSql(sqlString, values);
	}

	private List<Invoice> findInvoicesSql(String sqlString, List<String> values){

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		List<Invoice> resultInvoices = new ArrayList<Invoice>();
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c = null;

		if (values.isEmpty()){
			c = db.rawQuery(sqlString, new String[]{} );
		}else{
			c = db.rawQuery(sqlString, values.toArray(new String[0]) );
		}

		try{
			Invoice invoice = null;

			while(c.moveToNext())
			{
				invoice=new Invoice();

				boolean hasNoId = c.isNull(c.getColumnIndex(VAR_ID));
				if (!hasNoId){
					Long id = c.getLong(c.getColumnIndex(VAR_ID));
					invoice.setId(id);
				}

				boolean hasNoInvoiceId = c.isNull(c.getColumnIndex(VAR_INVOICE_ID));
				if (!hasNoInvoiceId){
					invoice.setInvoiceId(UUID.fromString(c.getString(c.getColumnIndex(VAR_INVOICE_ID))));
				}

				boolean hasNoDate = c.isNull(c.getColumnIndex(VAR_DATE_OF_INVOICE));
				if (!hasNoDate){
					invoice.setDateOfInvoice(new Date(c.getLong(c.getColumnIndex(VAR_DATE_OF_INVOICE))));
				}

				boolean hasNoSum = c.isNull(c.getColumnIndex(VAR_SUM_OF_INVOICE));
				if (!hasNoSum){
					double tmpValue = c.getDouble(c.getColumnIndex(VAR_SUM_OF_INVOICE));
					BigDecimal resultValue = BigDecimal.valueOf(tmpValue);
					invoice.setSumOfInvoice(resultValue);
				}

				boolean hasNoStatus = c.isNull(c.getColumnIndex(VAR_INVOICE_STATUS_ENUM));
				if (!hasNoStatus){
					InvoiceStatusEnum invoiceStatusEnum = InvoiceStatusEnum.valueOf(c.getString(c.getColumnIndex(VAR_INVOICE_STATUS_ENUM)));
					invoice.setInvoiceStatusEnum(invoiceStatusEnum);
				}

				boolean hasNoPayerId = c.isNull(c.getColumnIndex(VAR_PAYER_ID));
				if (!hasNoPayerId){
					invoice.setPayerId(UUID.fromString(c.getString(c.getColumnIndex(VAR_PAYER_ID))));
				}

				boolean hasNoInvoiceFailureMessage = c.isNull(c.getColumnIndex(VAR_INVOICE_INVOICE_FAILURE_MESSAGE));
				if (!hasNoInvoiceFailureMessage){
					invoice.setInvoiceFailureMessage(c.getString(c.getColumnIndex(VAR_INVOICE_INVOICE_FAILURE_MESSAGE)));
				}

				boolean hasNoPayerTypeEnum = c.isNull(c.getColumnIndex(VAR_PAYER_TYPE_ENUM));
				if (!hasNoPayerTypeEnum){
					PaymentPersonTypeEnum payerTypeEnum = PaymentPersonTypeEnum.valueOf(c.getString(c.getColumnIndex(VAR_PAYER_TYPE_ENUM)));
					invoice.setPayerTypeEnum(payerTypeEnum);
				}

				boolean hasNoPaymentRecipientId = c.isNull(c.getColumnIndex(VAR_PAYMENT_RECIPIENT_ID));
				if (!hasNoPaymentRecipientId){
					invoice.setPaymentRecipientId(UUID.fromString(c.getString(c.getColumnIndex(VAR_PAYMENT_RECIPIENT_ID))));
				}

				boolean hasNoPaymentRecipientTypeEnum = c.isNull(c.getColumnIndex(VAR_PAYMENT_RECIPIENT_TYPE_ENUM));
				if (!hasNoPaymentRecipientTypeEnum){
					PaymentPersonTypeEnum paymentPersonTypeEnum = PaymentPersonTypeEnum.valueOf(c.getString(c.getColumnIndex(VAR_PAYMENT_RECIPIENT_TYPE_ENUM)));
					invoice.setPaymentRecipientTypeEnum(paymentPersonTypeEnum);
				}

				boolean hasNoInvoiceImageId = c.isNull(c.getColumnIndex(VAR_INVOICE_IMAGE_ID));
				if (!hasNoInvoiceImageId){
					invoice.setInvoiceImageId(UUID.fromString(c.getString(c.getColumnIndex(VAR_INVOICE_IMAGE_ID))));
				}

				boolean hasNotColumn = c.isNull(c.getColumnIndex(VAR_CREATED_BY_ID));
				if (!hasNotColumn) {
					invoice.setCreatedById(UUID.fromString(c.getString(c.getColumnIndex(VAR_CREATED_BY_ID))));
				}

				hasNotColumn = c.isNull(c.getColumnIndex(VAR_INVOICE_STANDING_ORDER_INVOICE_TEMPLATE_ID));
				if (!hasNotColumn){
					invoice.setStandingOrderInvoiceTemplateId(UUID.fromString(c.getString(c.getColumnIndex(VAR_INVOICE_STANDING_ORDER_INVOICE_TEMPLATE_ID))));
				}

				hasNotColumn = c.isNull(c.getColumnIndex(VAR_INVOICE_STANDING_ORDER_START_DATE));
				if (!hasNotColumn){
					try {
						invoice.setStandingOrderStartDate(dateFormat.parse(c.getString(c.getColumnIndex(VAR_INVOICE_STANDING_ORDER_START_DATE))));
					} catch (ParseException ignored) {
					}
				}

				hasNotColumn = c.isNull(c.getColumnIndex(VAR_INVOICE_CATEGORY_ID));
				if (!hasNotColumn) {
					InvoiceCategory invoiceCategory = new InvoiceCategory();
					invoiceCategory.setInvoiceCategoryId(UUID.fromString(c.getString(c.getColumnIndex(VAR_INVOICE_CATEGORY_ID))));
					invoice.setInvoiceCategoryDTO(invoiceCategory);
				}

				hasNotColumn = c.isNull(c.getColumnIndex(VAR_INVOICE_REMARKS));
				if (!hasNotColumn) {
					invoice.setRemarks(c.getString(c.getColumnIndex(VAR_INVOICE_REMARKS)));
				}

				hasNotColumn = c.isNull(c.getColumnIndex(VAR_ARTICLES_DTOS));
				if (!hasNotColumn) {
					String json = c.getString(c.getColumnIndex(VAR_ARTICLES_DTOS));

					Type listType = new TypeToken<ArrayList<ArticleDTO>>() {
					}.getType();

					GsonBuilder builder = new GsonBuilder();
					builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
						public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
							return new Date(json.getAsJsonPrimitive().getAsLong());
						}
					});
					Gson gson = builder.create();

					List<ArticleDTO> articleDTOS = gson.fromJson(json, listType);

					invoice.setArticleDTOs(articleDTOS);
				}

				hasNotColumn = c.isNull(c.getColumnIndex(VAR_INVOICE_SPECIAL_TYPE));
				if (!hasNotColumn){
					int specialType = c.getInt(c.getColumnIndex(VAR_INVOICE_SPECIAL_TYPE));
					if (specialType== 1){
						invoice.setSpecialType(true);
					}else{
						invoice.setSpecialType(false);
					}
				}

				hasNotColumn = c.isNull(c.getColumnIndex(VAR_INVOICE_REPETITION_TYPE));
				if (!hasNotColumn){
					RepetitionTypeEnum repetitionTypeEnum = RepetitionTypeEnum.valueOf(c.getString(c.getColumnIndex(VAR_INVOICE_REPETITION_TYPE)));
					invoice.setRepetitionTypeEnum(repetitionTypeEnum);
				}

				hasNotColumn = c.isNull(c.getColumnIndex(VAR_PAYMENT_TYPE_ENUM));
				if (!hasNotColumn){
					PaymentTypeEnum paymentTypeEnum = PaymentTypeEnum.valueOf(c.getString(c.getColumnIndex(VAR_PAYMENT_TYPE_ENUM)));
					invoice.setPaymentTypeEnum(paymentTypeEnum);
				}

				hasNotColumn = c.isNull(c.getColumnIndex(VAR_INVOICE_SOURCE));
				if (!hasNotColumn){
					InvoiceSource invoiceSource = InvoiceSource.valueOf(c.getString(c.getColumnIndex(VAR_INVOICE_SOURCE)));
					invoice.setInvoiceSource(invoiceSource);
				}

				hasNotColumn = c.isNull(c.getColumnIndex(VAR_INVOICE_COST_PAID));
				if (!hasNotColumn){
					double tmpCostPaid = c.getDouble(c.getColumnIndex(VAR_INVOICE_COST_PAID));
					BigDecimal resultCostPaid = BigDecimal.valueOf(tmpCostPaid);
					invoice.setCostPaid(resultCostPaid);
				}

				hasNotColumn = c.isNull(c.getColumnIndex(VAR_INVOICE_CORRECTION_STATUS));
				if (!hasNotColumn){
					CorrectionStatus correctionStatus = CorrectionStatus.valueOf(c.getString(c.getColumnIndex(VAR_INVOICE_CORRECTION_STATUS)));
					invoice.setCorrectionStatus(correctionStatus);
				}

				resultInvoices.add(invoice);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			c.close();
		}

		return resultInvoices;
	}

	public List<UserContact> findUserContactsSql(SqlBuilder sqlBuilder){
		String sqlString = sqlBuilder.getSqlString();
		List<String> values = sqlBuilder.getValuesAsList();

		return findUserContactsSql(sqlString, values);
	}

	public List<UserContact> findUserContacts(String rowName, String value){
		String sqlString = createWhereClause(TABLE_USER_CONTACT, rowName, SQLOperatorEnum.EQUAL, Collections.singletonList(value));
		return findUserContactsSql(sqlString, value != null ? Collections.singletonList(value) : new ArrayList<String>());
	}

	private List<UserContact> findUserContactsSql(String sqlString, List<String> values){

		List<UserContact> resultUserContacts = new ArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c;

		if (values.isEmpty()){
			c = db.rawQuery(sqlString, new String[]{} );
		}else{
			c = db.rawQuery(sqlString, values.toArray(new String[0]) );
		}

		try{
			UserContact userContact;

			while(c.moveToNext())
			{
				userContact=new UserContact();

				boolean hasNoId = c.isNull(c.getColumnIndex(VAR_ID));
				if (!hasNoId){
					Long id = c.getLong(c.getColumnIndex(VAR_ID));
					userContact.setId(id);
				}

				boolean hasNoColumn = c.isNull(c.getColumnIndex(VAR_USER_CONTACT_ID));
				if (!hasNoColumn){
					userContact.setUserContactId(UUID.fromString(c.getString(c.getColumnIndex(VAR_USER_CONTACT_ID))));
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_APP_USER_ID));
				if (!hasNoColumn){
					userContact.setAppUserId(UUID.fromString(c.getString(c.getColumnIndex(VAR_APP_USER_ID))));
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_APP_USER_CONTACT_ID));
				if (!hasNoColumn){
					userContact.setAppUserContactId(UUID.fromString(c.getString(c.getColumnIndex(VAR_APP_USER_CONTACT_ID))));
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_EMAIL));
				if (!hasNoColumn){
					userContact.setEmail(c.getString(c.getColumnIndex(VAR_EMAIL)));
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_CONTACT_NAME));
				if (!hasNoColumn){
					userContact.setContactName(c.getString(c.getColumnIndex(VAR_CONTACT_NAME)));
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_BASIC_STATUS_ENUM));
				if (!hasNoColumn){
					BasicStatusEnum basicStatusEnum = BasicStatusEnum.valueOf(c.getString(c.getColumnIndex(VAR_BASIC_STATUS_ENUM)));
					userContact.setBasicStatusEnum(basicStatusEnum);
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_IS_PROJECT));
				if (!hasNoColumn){
					int isProject = c.getInt(c.getColumnIndex(VAR_IS_PROJECT));
					if (isProject== 1){
						userContact.setProject(true);
					}else{
						userContact.setProject(false);
					}
				}

				resultUserContacts.add(userContact);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			c.close();
		}

		return resultUserContacts;
	}

	public List<BasicData> findBasicDatasSql(SqlBuilder sqlBuilder){

		String sqlString = sqlBuilder.getSqlString();
		String[] values = sqlBuilder.getValues();

		List<BasicData> resultBasicDatas = new ArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c;

		if (values == null){
			c = db.rawQuery(sqlString, new String[]{} );
		}else{
			c = db.rawQuery(sqlString, values );
		}

		try{
			BasicData basicData;

			while(c.moveToNext())
			{
				basicData=new BasicData();

				boolean hasNoId = c.isNull(c.getColumnIndex(VAR_ID));
				if (!hasNoId){
					Long id = c.getLong(c.getColumnIndex(VAR_ID));
					basicData.setId(id);
				}

				boolean hasNoColumn = c.isNull(c.getColumnIndex(VAR_BASIC_DATA_ID));
				if (!hasNoColumn){
					basicData.setBasicDataId(UUID.fromString(c.getString(c.getColumnIndex(VAR_BASIC_DATA_ID))));
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_APP_USER_ID));
				if (!hasNoColumn){
					basicData.setAppUserId(UUID.fromString(c.getString(c.getColumnIndex(VAR_APP_USER_ID))));
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_BASIC_DATA_TYPE));
				if (!hasNoColumn){
					BasicDataType basicDataType = BasicDataType.valueOf(c.getString(c.getColumnIndex(VAR_BASIC_DATA_TYPE)));
					basicData.setBasicDataType(basicDataType);
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_BASIC_DATA_SUB_TYPE));
				if (!hasNoColumn){
					BasicDataSubType basicDataSubType = BasicDataSubType.valueOf(c.getString(c.getColumnIndex(VAR_BASIC_DATA_SUB_TYPE)));
					basicData.setBasicDataSubType(basicDataSubType);
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_BASIC_DATA_NUMBER_VALUE));
				if (!hasNoColumn){
					double tmpValue = c.getDouble(c.getColumnIndex(VAR_BASIC_DATA_NUMBER_VALUE));
					BigDecimal resultValue = BigDecimal.valueOf(tmpValue);
					basicData.setNumberValue(resultValue);
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_BASIC_DATA_OBJECT_1_ID));
				if (!hasNoColumn){
					basicData.setObject1Id(c.getString(c.getColumnIndex(VAR_BASIC_DATA_OBJECT_1_ID)));
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_BASIC_DATA_OBJECT_1_CLASS));
				if (!hasNoColumn){
					basicData.setObject1Class(c.getString(c.getColumnIndex(VAR_BASIC_DATA_OBJECT_1_CLASS)));
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_BASIC_DATA_OBJECT_2_ID));
				if (!hasNoColumn){
					basicData.setObject2Id(c.getString(c.getColumnIndex(VAR_BASIC_DATA_OBJECT_2_ID)));
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_BASIC_DATA_OBJECT_2_CLASS));
				if (!hasNoColumn){
					basicData.setObject2Class(c.getString(c.getColumnIndex(VAR_BASIC_DATA_OBJECT_2_CLASS)));
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_BASIC_DATA_VALUE));
				if (!hasNoColumn){
					basicData.setValue(c.getString(c.getColumnIndex(VAR_BASIC_DATA_VALUE)));
				}

				resultBasicDatas.add(basicData);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			c.close();
		}

		return resultBasicDatas;
	}

	public List<BusinessPartner> findBusinessPartners(String rowName, String value){
		String sqlString = createWhereClause(TABLE_BUSINESS_PARTNER, rowName, SQLOperatorEnum.EQUAL, Collections.singletonList(value));
		return findBusinessPartnersSql(sqlString, value != null ? Collections.singletonList(value) : new ArrayList<String>());
	}

	public List<BusinessPartner> findBusinessPartnersSql(SqlBuilder sqlBuilder){
		return findBusinessPartnersSql(sqlBuilder.getSqlString(), sqlBuilder.getValuesAsList());
	}

	private List<BusinessPartner> findBusinessPartnersSql(String sqlString, List<String> values){
		List<BusinessPartner> resultBusinessPartners = new ArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c;

		if (values.isEmpty()){
			c = db.rawQuery(sqlString, new String[]{} );
		}else{
			c = db.rawQuery(sqlString, values.toArray(new String[0]) );
		}

		try{

			BusinessPartner businessPartner;
			while(c.moveToNext())
			{
				businessPartner =new BusinessPartner();

				boolean hasNoId = c.isNull(c.getColumnIndex(VAR_ID));
				if (!hasNoId){
					Long id = c.getLong(c.getColumnIndex(VAR_ID));
					businessPartner.setId(id);
				}

				boolean hasNoBusinessPartnerId = c.isNull(c.getColumnIndex(VAR_BUSINESS_PARTNER_ID));
				if (!hasNoBusinessPartnerId){
					String businessPartnerId = c.getString(c.getColumnIndex(VAR_BUSINESS_PARTNER_ID));
					businessPartner.setBusinessPartnerId(UUID.fromString(businessPartnerId));
				}

				boolean hasNoBusinessPartnerName = c.isNull(c.getColumnIndex(VAR_BUSINESS_PARTNER_NAME));
				if (!hasNoBusinessPartnerName){
					businessPartner.setBusinessPartnerName(c.getString(c.getColumnIndex(VAR_BUSINESS_PARTNER_NAME)));
				}

				boolean hasNoBusinessPartnerReceiptName = c.isNull(c.getColumnIndex(VAR_BUSINESS_PARTNER_RECEIPT_NAME));
				if (!hasNoBusinessPartnerReceiptName){
					businessPartner.setBusinessPartnerReceiptName(c.getString(c.getColumnIndex(VAR_BUSINESS_PARTNER_RECEIPT_NAME)));
				}

				boolean hasNoInvoiceCategoryId = c.isNull(c.getColumnIndex(VAR_INVOICE_CATEGORY_ID));
				if (!hasNoInvoiceCategoryId){
					String invoiceCategoryId = c.getString(c.getColumnIndex(VAR_PARENT_INVOICE_CATEGORY_ID));
					businessPartner.setInvoiceCategoryId(UUID.fromString(invoiceCategoryId));
				}

				boolean hasNoAppUserId = c.isNull(c.getColumnIndex(VAR_APP_USER_ID));
				if (!hasNoAppUserId){
					String appUserId = c.getString(c.getColumnIndex(VAR_APP_USER_ID));
					businessPartner.setAppUserId(UUID.fromString(appUserId));
				}

				boolean hasNoStatus = c.isNull(c.getColumnIndex(VAR_BASIC_STATUS_ENUM));
				if (!hasNoStatus){
					BasicStatusEnum basicStatusEnum = BasicStatusEnum.valueOf(c.getString(c.getColumnIndex(VAR_BASIC_STATUS_ENUM)));
					businessPartner.setBasicStatusEnum(basicStatusEnum);
				}

				resultBusinessPartners.add(businessPartner);
			}

		}catch(Exception e){
			e.printStackTrace();
		}finally {
			c.close();
		}

		return resultBusinessPartners;
	}

	public List<InvoiceCategory> findInvoiceCategoriesSql(SqlBuilder sqlBuilder){

		String sqlString = sqlBuilder.getSqlString();
		String[] values = sqlBuilder.getValues();
		List<InvoiceCategory> resultInvoiceCategories = new ArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c;

		if (values == null){
			c = db.rawQuery(sqlString, new String[]{} );
		}else{
			c = db.rawQuery(sqlString, values );
		}

		try{

			InvoiceCategory invoiceCategory;
			while(c.moveToNext())
			{
				invoiceCategory =new InvoiceCategory();

				boolean hasNoId = c.isNull(c.getColumnIndex(VAR_ID));
				if (!hasNoId){
					Long id = c.getLong(c.getColumnIndex(VAR_ID));
					invoiceCategory.setId(id);
				}

				boolean hasNoColumn = c.isNull(c.getColumnIndex(VAR_INVOICE_CATEGORY_ID));
				if (!hasNoColumn){
					String invoiceCategoryId = c.getString(c.getColumnIndex(VAR_INVOICE_CATEGORY_ID));
					invoiceCategory.setInvoiceCategoryId(UUID.fromString(invoiceCategoryId));
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_INVOICE_CATEGORY_NAME));
				if (!hasNoColumn){
					invoiceCategory.setInvoiceCategoryName(c.getString(c.getColumnIndex(VAR_INVOICE_CATEGORY_NAME)));
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_APP_USER_ID));
				if (!hasNoColumn){
					String appUserId = c.getString(c.getColumnIndex(VAR_APP_USER_ID));
					invoiceCategory.setAppUserId(UUID.fromString(appUserId));
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_PARENT_INVOICE_CATEGORY_ID));
				if (!hasNoColumn){
					String parentInvoiceCategoryId = c.getString(c.getColumnIndex(VAR_PARENT_INVOICE_CATEGORY_ID));

					InvoiceCategory parentInvoiceCategory = new InvoiceCategory();
					parentInvoiceCategory.setInvoiceCategoryId(UUID.fromString(parentInvoiceCategoryId));

					invoiceCategory.setParentInvoiceCategoryDTO(parentInvoiceCategory);
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_BASIC_STATUS_ENUM));
				if (!hasNoColumn){
					BasicStatusEnum basicStatusEnum = BasicStatusEnum.valueOf(c.getString(c.getColumnIndex(VAR_BASIC_STATUS_ENUM)));
					invoiceCategory.setBasicStatusEnum(basicStatusEnum);
				}

				boolean hasNoStatus = c.isNull(c.getColumnIndex(VAR_INVOICE_CATEGORY_TYPE));
				if (!hasNoStatus){
					InvoiceCategoryType invoiceCategoryType = InvoiceCategoryType.valueOf(c.getString(c.getColumnIndex(VAR_INVOICE_CATEGORY_TYPE)));
					invoiceCategory.setInvoiceCategoryType(invoiceCategoryType);
				}

				resultInvoiceCategories.add(invoiceCategory);
			}

		}catch(Exception e){
			e.printStackTrace();
		}finally {
			c.close();
		}

		return resultInvoiceCategories;
	}

	public List<CostDistribution> findCostDistributions(){
		return findCostDistributions(null, null, new ArrayList<String>());
	}

	public List<CostDistribution> findCostDistributions(String rowName, String value){
		return findCostDistributions(rowName, SQLOperatorEnum.EQUAL, value != null ? Collections.singletonList(value) : new ArrayList<String>());
	}

	private List<CostDistribution> findCostDistributions(String rowName, SQLOperatorEnum sqlOperatorEnum, List<String> values){

		List<CostDistribution> resultCostDistributions = new ArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();

		String sqlString;
		Cursor c;
		if (values.isEmpty()){
			sqlString = "SELECT * FROM " + TABLE_COST_DISTRIBUTION;
			c = db.rawQuery(sqlString, new String[]{});
		}else{
			String sqlOperatorEnumString;
			if (SQLOperatorEnum.NOT.equals(sqlOperatorEnum)){
				sqlOperatorEnumString = " NOT " +rowName + "=?";
			}else{
				if (sqlOperatorEnum == null){
					sqlOperatorEnum = SQLOperatorEnum.EQUAL;
				}
				sqlOperatorEnumString = rowName + sqlOperatorEnum.getDisplayName();
			}

			StringBuilder resultString = new StringBuilder(sqlOperatorEnumString);
			if (values.size() > 1){
				for (int i = 1; i < values.size(); i++) {
					resultString.append(" AND ").append(sqlOperatorEnumString);
				}
			}

			sqlString = "SELECT * FROM " + TABLE_COST_DISTRIBUTION+ " WHERE "+resultString+";";

			c = db.rawQuery(sqlString, values.toArray(new String[0]));
		}

		try{
			CostDistribution costDistribution;

			while(c.moveToNext())
			{

				String valueColumn;
				costDistribution=new CostDistribution();

				boolean hasNoId = c.isNull(c.getColumnIndex(VAR_ID));
				if (!hasNoId){
					Long id = c.getLong(c.getColumnIndex(VAR_ID));
					costDistribution.setId(id);
				}

				boolean hasNoColumn = c.isNull(c.getColumnIndex(VAR_COST_DISTRIBUTION_ID));
				if (!hasNoColumn){
					costDistribution.setCostDistributionId(UUID.fromString(c.getString(c.getColumnIndex(VAR_COST_DISTRIBUTION_ID))));
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_COST_DISTRIBUTION_NAME));
				if (!hasNoColumn){
					valueColumn = c.getString(c.getColumnIndex(VAR_COST_DISTRIBUTION_NAME));
					costDistribution.setName(valueColumn);
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_CREATED_BY_ID));
				if (!hasNoColumn){
					valueColumn = c.getString(c.getColumnIndex(VAR_CREATED_BY_ID));
					costDistribution.setCreatedById(UUID.fromString(valueColumn));
				}


				resultCostDistributions.add(costDistribution);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			c.close();
		}

		return resultCostDistributions;
	}

	public List<CostDistributionItem> findCostDistributionItems(String rowName, String value){
		String sqlString = createWhereClause(TABLE_COST_DISTRIBUTION_ITEM, rowName, SQLOperatorEnum.EQUAL, Collections.singletonList(value));

		return findCostDistributionItemsSql(sqlString, value != null ? Collections.singletonList(value) : new ArrayList<String>());
	}

	public List<CostDistributionItem> findCostDistributionItemsSql(SqlBuilder sqlBuilder){
			String sqlString = sqlBuilder.getSqlString();
			List<String> values = sqlBuilder.getValuesAsList();

		return findCostDistributionItemsSql(sqlString, values);
	}

	private List<CostDistributionItem> findCostDistributionItemsSql(String sqlString, List<String> values){


		List<CostDistributionItem> resultCostDistributionItems = new ArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c;

		if (values.isEmpty()){
			c = db.rawQuery(sqlString, new String[]{} );
		}else{
			c = db.rawQuery(sqlString, values.toArray(new String[0]) );
		}

		try{

			CostDistributionItem costDistributionItem;
			while(c.moveToNext())
			{

				String valueColumn;

				costDistributionItem =new CostDistributionItem();

				boolean hasNoColumn = c.isNull(c.getColumnIndex(VAR_ID));
				if (!hasNoColumn){
					Long id = c.getLong(c.getColumnIndex(VAR_ID));
					costDistributionItem.setId(id);
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_COST_DISTRIBUTION_ITEM_ID));
				if (!hasNoColumn){
					valueColumn = c.getString(c.getColumnIndex(VAR_COST_DISTRIBUTION_ITEM_ID));
					costDistributionItem.setCostDistributionItemId(UUID.fromString(valueColumn));
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_COST_DISTRIBUTION_ITEM_COST_DISTRIBUTION_ITEM_TYPE_ENUM));
				if (!hasNoColumn){
					CostDistributionItemTypeEnum costDistributionItemTypeEnum = CostDistributionItemTypeEnum.valueOf(c.getString(c.getColumnIndex(VAR_COST_DISTRIBUTION_ITEM_COST_DISTRIBUTION_ITEM_TYPE_ENUM)));
					costDistributionItem.setCostDistributionItemTypeEnum(costDistributionItemTypeEnum);
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_COST_DISTRIBUTION_ITEM_VALUE));
				if (!hasNoColumn){
					double tmpValue = c.getDouble(c.getColumnIndex(VAR_COST_DISTRIBUTION_ITEM_VALUE));
					BigDecimal resultValue = BigDecimal.valueOf(tmpValue);
					costDistributionItem.setValue(resultValue);
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_COST_DISTRIBUTION_ITEM_COST_PAID));
				if (!hasNoColumn){
                    double tmpValue = c.getDouble(c.getColumnIndex(VAR_COST_DISTRIBUTION_ITEM_COST_PAID));
					BigDecimal resultValue = BigDecimal.valueOf(tmpValue);
					costDistributionItem.setCostPaid(resultValue);
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_PAYER_ID));
				if (!hasNoColumn){
					valueColumn = c.getString(c.getColumnIndex(VAR_PAYER_ID));
					costDistributionItem.setPayerId(UUID.fromString(valueColumn));
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_COST_DISTRIBUTION_ITEM_PAYMENT_PERSON_TYPE_ENUM));
				if (!hasNoColumn){
					PaymentPersonTypeEnum paymentPersonTypeEnum = PaymentPersonTypeEnum.valueOf(c.getString(c.getColumnIndex(VAR_COST_DISTRIBUTION_ITEM_PAYMENT_PERSON_TYPE_ENUM)));
					costDistributionItem.setPaymentPersonTypeEnum(paymentPersonTypeEnum);
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_COST_DISTRIBUTION_ITEM_POSITION));
				if (!hasNoColumn){
					Integer positionNumber = c.getInt(c.getColumnIndex(VAR_COST_DISTRIBUTION_ITEM_POSITION));
					costDistributionItem.setPosition(positionNumber);
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_INVOICE_ID));
				if (!hasNoColumn){
					valueColumn = c.getString(c.getColumnIndex(VAR_INVOICE_ID));
					costDistributionItem.setInvoiceId(UUID.fromString(valueColumn));
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_COST_DISTRIBUTION_ID));
				if (!hasNoColumn){
					valueColumn = c.getString(c.getColumnIndex(VAR_COST_DISTRIBUTION_ID));
					costDistributionItem.setCostDistributionId(UUID.fromString(valueColumn));
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_IS_TEMPORARY));
				if (!hasNoColumn){
					valueColumn = c.getString(c.getColumnIndex(VAR_IS_TEMPORARY));
					costDistributionItem.setIsTemporary(valueColumn);
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_COST_DISTRIBUTION_ITEM_MONEY_VALUE));
				if (!hasNoColumn){
					double tmpValue = c.getDouble(c.getColumnIndex(VAR_COST_DISTRIBUTION_ITEM_MONEY_VALUE));
					BigDecimal resultValue = BigDecimal.valueOf(tmpValue);
					costDistributionItem.setMoneyValue(resultValue);
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_COST_DISTRIBUTION_ITEM_REMARKS));
				if (!hasNoColumn){
					valueColumn = c.getString(c.getColumnIndex(VAR_COST_DISTRIBUTION_ITEM_REMARKS));
					costDistributionItem.setRemarks(valueColumn);
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_COST_DISTRIBUTION_ITEM_CORRECTION_STATUS));
				if (!hasNoColumn){
					CorrectionStatus correctionStatus = CorrectionStatus.valueOf(c.getString(c.getColumnIndex(VAR_COST_DISTRIBUTION_ITEM_CORRECTION_STATUS)));
					costDistributionItem.setCorrectionStatus(correctionStatus);
				}

				hasNoColumn = c.isNull(c.getColumnIndex(VAR_ARTICLES_DTOS));
				if (!hasNoColumn) {
					String json = c.getString(c.getColumnIndex(VAR_ARTICLES_DTOS));

					Type listType = new TypeToken<ArrayList<ArticleDTO>>() {
					}.getType();

					GsonBuilder builder = new GsonBuilder();
					builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
						public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
							return new Date(json.getAsJsonPrimitive().getAsLong());
						}
					});
					Gson gson = builder.create();

					List<ArticleDTO> articleDTOS = gson.fromJson(json, listType);

					costDistributionItem.setArticleDTOs(articleDTOS);
				}

				resultCostDistributionItems.add(costDistributionItem);
			}

		}catch(Exception e){
			e.printStackTrace();
		}finally {
			c.close();
		}

		return resultCostDistributionItems;
	}

	public static BasicData findBasicSearchDataOrCreate(Context context, BasicDataSubType basicDataSubType){
		MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

		AppUser currentUser = LoginUserHelper.currentLoggedInUser(context);

		SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
		sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.SEARCH.name())
				.and()
				.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_SUB_TYPE, basicDataSubType.name())
				.and()
				.isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString());

		List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

		if (!basicDatas.isEmpty()){
			return basicDatas.get(0);
		}else{
			BasicData basicData = new BasicData();
			basicData.setBasicDataType(BasicDataType.SEARCH);
			basicData.setBasicDataSubType(basicDataSubType);
			basicData.setAppUserId(currentUser.getAppUserId());
			return basicData;
		}
	}

	public static BasicData findBasicDataSearchValue(Context context, BasicDataSubType basicDataSubType){
		MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

		AppUser currentUser = LoginUserHelper.currentLoggedInUser(context);

		SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
		sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.SEARCH.name())
				.and()
				.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_SUB_TYPE, basicDataSubType.name())
				.and()
				.isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString());

		List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

		if (!basicDatas.isEmpty()){
			return basicDatas.get(0);
		}

		return null;
	}

	public static void saveBasicSearchData(Context context, BasicData basicData){
		MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

		AppUser currentUser = LoginUserHelper.currentLoggedInUser(context);

		SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
		sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.SEARCH.name())
				.and()
				.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_SUB_TYPE, basicData.getBasicDataSubType().name())
				.and()
				.isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString());

		List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

		if (!basicDatas.isEmpty()){
			mainDatabaseHandler.updateBasicData(basicData);
		}else{
			mainDatabaseHandler.insertBasicData(basicData);
		}
	}

	public static void deleteBasicData(Context context, BasicData basicData){
		MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);
		List<BasicData> basicDatas1 = new ArrayList<>();
		basicDatas1.add(basicData);
		mainDatabaseHandler.deleteBasicDatas(new ArrayList<BasicData>(basicDatas1));
	}

	public static void deleteAllBasicSearchData(Context context){

		MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);
		AppUser currentUser = LoginUserHelper.currentLoggedInUser(context);

		SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
		sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.SEARCH.name())
				.and()
				.isEqual(MainDatabaseHandler.VAR_APP_USER_ID, currentUser.getAppUserId().toString());

		List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);
		mainDatabaseHandler.deleteBasicDatas(basicDatas);
	}

	public static IPaymentPerson paymentPersonByTypeAndId(PaymentPersonTypeEnum paymentPersonTypeEnum, UUID id, Context context){
		return paymentPersonByTypeAndId(paymentPersonTypeEnum, id, context, null);
	}

	public static IPaymentPerson paymentPersonByTypeAndId(PaymentPersonTypeEnum paymentPersonTypeEnum, UUID id, Context context, String name){

		IPaymentPerson payer = null;

		if (paymentPersonTypeEnum == null){
			return null;
		}

		if (paymentPersonTypeEnum.equals(PaymentPersonTypeEnum.BUSINESS_PARTNER)) {
			MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);
			List<BusinessPartner> businessPartnerList = mainDatabaseHandler.findBusinessPartners(MainDatabaseHandler.VAR_BUSINESS_PARTNER_ID, id.toString());
			if (!businessPartnerList.isEmpty()){
				payer = businessPartnerList.get(0);
			}
		}else if (paymentPersonTypeEnum.equals(PaymentPersonTypeEnum.USER)) {
			MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);
			List<AppUser> appUserList = mainDatabaseHandler.findAppUsers(MainDatabaseHandler.VAR_APP_USER_ID, id.toString());
			if (!appUserList.isEmpty()){
				payer = appUserList.get(0);
			}
		}else if (paymentPersonTypeEnum.equals(PaymentPersonTypeEnum.CONTACT)) {
			MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);
			List<UserContact> userContactList = mainDatabaseHandler.findUserContacts(MainDatabaseHandler.VAR_USER_CONTACT_ID, id.toString());
			if (!userContactList.isEmpty()){
				payer = userContactList.get(0);
			}
		}else{
			PaymentPerson paymentPerson = new PaymentPerson();
			paymentPerson.setPaymentPersonEnum(paymentPersonTypeEnum);
			paymentPerson.setPaymentPersonId(id);
			paymentPerson.setPaymentPersonName(name);
			paymentPerson.setVirtualPayerEnum(paymentPersonTypeEnum);
			payer = paymentPerson;
		}

		return payer;
	}

	public int countQuery(SqlBuilder sqlBuilder){

		String sqlString = sqlBuilder.getSqlString();
		List<String> values = sqlBuilder.getValuesAsList();

		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c;

		if (values.isEmpty()){
			c = db.rawQuery(sqlString, new String[]{} );
		}else{
			c = db.rawQuery(sqlString, values.toArray(new String[0]) );
		}

		try{
			if (c.moveToNext()) {
				boolean hasNoDate = c.isNull(0);
				if (!hasNoDate) {
					return c.getInt(0);
				}
			}

		}catch(Exception e){
			e.printStackTrace();
		}finally {
			c.close();
		}

		return 0;
	}

	public Date dateQuery(SqlBuilder sqlBuilder){

		String sqlString = sqlBuilder.getSqlString();
		List<String> values = sqlBuilder.getValuesAsList();

		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c;

		if (values.isEmpty()){
			c = db.rawQuery(sqlString, new String[]{} );
		}else{
			c = db.rawQuery(sqlString, values.toArray(new String[0]) );
		}

		try{
			if (c.moveToNext()) {
				boolean hasNoDate = c.isNull(0);
				if (!hasNoDate) {
					return new Date(c.getLong(0));
				}
			}

		}catch(Exception e){
			e.printStackTrace();
		}finally {
			c.close();
		}

		return null;
	}

	public List<AppUser> findAppUsers(){
		String sqlString = createWhereClause(TABLE_APP_USER, null, null, new ArrayList<String>());
		return findAppUsersSql(sqlString, new ArrayList<String>());
	}

	public List<AppUser> findAppUsers(String rowName, String value){
		String sqlString = createWhereClause(TABLE_APP_USER, rowName, SQLOperatorEnum.EQUAL, Collections.singletonList(value));
		return findAppUsersSql(sqlString, value != null ? Collections.singletonList(value) : new ArrayList<String>());
	}

	public List<AppUser> findAppUsers(String rowName, String value, SQLOperatorEnum sqlOperatorEnum){
		String sqlString = createWhereClause(TABLE_APP_USER, rowName, sqlOperatorEnum, Collections.singletonList(value));
		return findAppUsersSql(sqlString, value != null ? Collections.singletonList(value) : new ArrayList<String>());
	}

	public List<AppUser> findAppUsers(SqlBuilder sqlBuilder){
		String sqlString = sqlBuilder.getSqlString();
		List<String> values = sqlBuilder.getValuesAsList();

		return findAppUsersSql(sqlString, values);
	}

	private List<AppUser> findAppUsersSql(String sqlString, List<String> values){

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		List<AppUser> resultAppUsers = new ArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c;

		if (values.isEmpty()){
			c = db.rawQuery(sqlString, new String[]{} );
		}else{
			c = db.rawQuery(sqlString, values.toArray(new String[0]) );
		}

		try{

			AppUser appUser;
			while(c.moveToNext())
			{
				appUser=new AppUser();

				boolean hasNoAppUserId = c.isNull(c.getColumnIndex(VAR_APP_USER_ID));
				if (!hasNoAppUserId){
					String appUserId = c.getString(c.getColumnIndex(VAR_APP_USER_ID));
					appUser.setAppUserId(UUID.fromString(appUserId));
				}

				boolean hasNoAppUserName = c.isNull(c.getColumnIndex(VAR_APP_USER_NAME));
				if (!hasNoAppUserName){
					appUser.setAppUserName(c.getString(c.getColumnIndex(VAR_APP_USER_NAME)));
				}

				boolean hasNoAppUserPassword = c.isNull(c.getColumnIndex(VAR_APP_USER_PASSWORD));
				if (!hasNoAppUserPassword){
					appUser.setAppUserPassword(c.getString(c.getColumnIndex(VAR_APP_USER_PASSWORD)));
				}

				boolean hasNoIsLoggedIn = c.isNull(c.getColumnIndex(VAR_APP_USER_IS_LOGGED_IN));
				if (!hasNoIsLoggedIn){
					int isLoggedIn = c.getInt(c.getColumnIndex(VAR_APP_USER_IS_LOGGED_IN));
					if (isLoggedIn== 1){
						appUser.setLoggedIn(true);
					}else{
						appUser.setLoggedIn(false);
					}
				}

				boolean hasNoAppUserAccessKey = c.isNull(c.getColumnIndex(VAR_APP_USER_ACCESS_KEY));
				if (!hasNoAppUserAccessKey){
					appUser.setAppUserAccessKey(c.getString(c.getColumnIndex(VAR_APP_USER_ACCESS_KEY)));
				}

				boolean hasNoEMail = c.isNull(c.getColumnIndex(VAR_EMAIL));
				if (!hasNoEMail){
					appUser.setEmail(c.getString(c.getColumnIndex(VAR_EMAIL)));
				}

				boolean hasNoAppUserRefreshToken = c.isNull(c.getColumnIndex(VAR_APP_USER_REFRESH_TOKEN));
				if (!hasNoAppUserRefreshToken){
					appUser.setAppUserRefreshToken(c.getString(c.getColumnIndex(VAR_APP_USER_REFRESH_TOKEN)));
				}

				boolean hasNoAppUserAccessExpiration = c.isNull(c.getColumnIndex(VAR_APP_USER_ACCESS_EXPIRATION));
				if (!hasNoAppUserAccessExpiration) {
					try {
						appUser.setAppUserAccessExpiration(dateFormat.parse(c.getString(c.getColumnIndex(VAR_APP_USER_ACCESS_EXPIRATION))));
					} catch (ParseException e) {
					}
				}

				boolean hasNoDate = c.isNull(c.getColumnIndex(VAR_APP_USER_LAST_UPDATE));
				if (!hasNoDate){
					try {
						appUser.setLastUpdate(dateFormat.parse(c.getString(c.getColumnIndex(VAR_APP_USER_LAST_UPDATE))));
					} catch (ParseException e) {
					}
				}

				boolean hasNoUpdateColumn = c.isNull(c.getColumnIndex(VAR_APP_USER_UPDATE_MODUS_TYPE));
				if (!hasNoUpdateColumn){
					UpdateModusType updateModusType = UpdateModusType.valueOf(c.getString(c.getColumnIndex(VAR_APP_USER_UPDATE_MODUS_TYPE)));
					appUser.setUpdateModusType(updateModusType);
				}

				boolean hasNoRightColumn = c.isNull(c.getColumnIndex(VAR_RIGHTS_DTOS));
				if (!hasNoRightColumn) {
					String json = c.getString(c.getColumnIndex(VAR_RIGHTS_DTOS));

					Type listType = new TypeToken<ArrayList<AppRightDTO>>() {
					}.getType();

					GsonBuilder builder = new GsonBuilder();
					builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
						public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
							return new Date(json.getAsJsonPrimitive().getAsLong());
						}
					});
					Gson gson = builder.create();

					List<AppRightDTO> appRightDTOs = gson.fromJson(json, listType);

					appUser.setAppRightDTOs(appRightDTOs);
				}

				resultAppUsers.add(appUser);
			}

		}catch(Exception e){
			e.printStackTrace();
		}finally {
			c.close();
		}

		return resultAppUsers;
	}

	public List<InvoiceFailure> findInvoiceFailures(String rowName, String value){

		List<InvoiceFailure> resultInvoiceFailures = new ArrayList<InvoiceFailure>();
		SQLiteDatabase db = this.getReadableDatabase();

		String sqlString = "";
		Cursor c = null;
		if (value == null){
			sqlString = "SELECT * FROM " + TABLE_INVOICE_FAILURE;
			c = db.rawQuery(sqlString, new String[]{});
		}else{
			sqlString = "SELECT * FROM " + TABLE_INVOICE_FAILURE+ " WHERE "+rowName+"=?;";
			c = db.rawQuery(sqlString, new String[]{value});

		}

		try{

			InvoiceFailure invoiceFailure = null;
			while(c.moveToNext())
			{
				invoiceFailure=new InvoiceFailure();

				boolean hasNoId = c.isNull(c.getColumnIndex(VAR_ID));
				if (!hasNoId){
					Long id = c.getLong(c.getColumnIndex(VAR_ID));
					invoiceFailure.setId(id);
				}

				boolean hasNoInvoiceFailureId = c.isNull(c.getColumnIndex(VAR_INVOICE_FAILURE_ID));
				if (!hasNoInvoiceFailureId){
					String id = c.getString(c.getColumnIndex(VAR_INVOICE_FAILURE_ID));
					invoiceFailure.setInvoiceFailureId(UUID.fromString(id));
				}

				boolean hasNoInvoiceFailureTypeEnum = c.isNull(c.getColumnIndex(VAR_INVOICE_FAILURE_TYPE_ENUM));
				if (!hasNoInvoiceFailureTypeEnum){
					InvoiceFailureTypeEnum invoiceFailureTypeEnum = InvoiceFailureTypeEnum.valueOf(c.getString(c.getColumnIndex(VAR_INVOICE_FAILURE_TYPE_ENUM)));
					invoiceFailure.setInvoiceFailureTypeEnum(invoiceFailureTypeEnum);
				}

				boolean hasNoInvoiceId = c.isNull(c.getColumnIndex(VAR_INVOICE_ID));
				if (!hasNoInvoiceId){
					invoiceFailure.setInvoiceId(UUID.fromString(c.getString(c.getColumnIndex(VAR_INVOICE_ID))));
				}

				resultInvoiceFailures.add(invoiceFailure);
			}

		}catch(Exception e){
			e.printStackTrace();
		}finally {
			c.close();
		}

		return resultInvoiceFailures;
	}

	public <T> void updatePerDatabaseChangesDTO(List<DatabaseChangeDTO<T>> databaseChangeDTOs, Class<T> classType, Context context) {

		MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

		if (classType.equals(Invoice.class)) {

			List<Invoice> invoicesToAdd = new ArrayList<>();
			for (DatabaseChangeDTO<T> databaseChangeDTO : databaseChangeDTOs) {
				Invoice invoice = (Invoice) databaseChangeDTO.getMappedObject();
				if (databaseChangeDTO.getRevisionType() == 0 || databaseChangeDTO.getRevisionType() == 1) {
					invoicesToAdd.add(invoice);
				} else if (databaseChangeDTO.getRevisionType() == 2) {
					List<Invoice> objects = new ArrayList<>();
					objects.add(invoice);
					deleteInvoices(objects);
				}
			}
			mainDatabaseHandler.deleteInvoices(null);
			mainDatabaseHandler.insertInvoices(invoicesToAdd);


		}else if (classType.equals(CostDistributionItem.class)) {
			List<CostDistributionItem> costDistributionItemsToAdd = new ArrayList<>();
			for (DatabaseChangeDTO<T> databaseChangeDTO : databaseChangeDTOs) {
				CostDistributionItem costDistributionItem = (CostDistributionItem) databaseChangeDTO.getMappedObject();
				if (databaseChangeDTO.getRevisionType() == 0 || databaseChangeDTO.getRevisionType() == 1) {
					costDistributionItemsToAdd.add(costDistributionItem);
				} else if (databaseChangeDTO.getRevisionType() == 2) {
					List<CostDistributionItem> objects = new ArrayList<>();
					objects.add(costDistributionItem);
					deleteCostDistributionItems(objects);
				}
			}
			mainDatabaseHandler.deleteCostDistributionItems(null);
			mainDatabaseHandler.insertCostDistributionItems(costDistributionItemsToAdd);
		}else if (classType.equals(BusinessPartner.class)) {
			List<BusinessPartner> businessPartnersToAdd = new ArrayList<>();
			for (DatabaseChangeDTO<T> databaseChangeDTO : databaseChangeDTOs) {
				BusinessPartner businessPartner = (BusinessPartner) databaseChangeDTO.getMappedObject();
				if (databaseChangeDTO.getRevisionType() == 0 || databaseChangeDTO.getRevisionType() == 1) {
					businessPartnersToAdd.add(businessPartner);
				} else if (databaseChangeDTO.getRevisionType() == 2) {
					List<BusinessPartner> objects = new ArrayList<>();
					objects.add(businessPartner);
					deleteBusinessPartners(objects);
				}
			}
			
			mainDatabaseHandler.deleteBusinessPartners(null);
			mainDatabaseHandler.insertBusinessPartners(businessPartnersToAdd);
		}
	}

	public <T> void updatePerDatabaseChangeDTO(DatabaseChangeDTO<T> databaseChangeDTO, Class<T> classType, Context context){

		if (classType.equals(Invoice.class)){
			Invoice invoice = (Invoice) databaseChangeDTO.getMappedObject();
			if (databaseChangeDTO.getRevisionType() == 0 || databaseChangeDTO.getRevisionType() == 1){
				updateInvoice(invoice);

				MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

				SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM);
				sqlBuilder.isEqual(MainDatabaseHandler.VAR_INVOICE_ID, invoice.getInvoiceId().toString());
				List<CostDistributionItem> costDistributionItems = mainDatabaseHandler.findCostDistributionItemsSql(sqlBuilder);
				deleteCostDistributionItems(costDistributionItems);

				List<CostDistributionItem> newCostDistributionItems = invoice.getCostDistributionItemDTOs();
				insertCostDistributionItems(newCostDistributionItems);

			}else if (databaseChangeDTO.getRevisionType() == 2){
				MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

				SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM);
				sqlBuilder.isEqual(MainDatabaseHandler.VAR_INVOICE_ID, invoice.getInvoiceId().toString());
				List<CostDistributionItem> costDistributionItems = mainDatabaseHandler.findCostDistributionItemsSql(sqlBuilder);
				deleteCostDistributionItems(costDistributionItems);

				List<Invoice> objects = new ArrayList<>();
				objects.add(invoice);
				deleteInvoices(objects);
			}
		}else if (classType.equals(BasicData.class)){
			BasicData basicData = (BasicData) databaseChangeDTO.getMappedObject();

			if (BasicDataType.INTERNAL_DATA.equals(basicData.getBasicDataType())
				&& BasicDataSubType.INVOICE_DELETED.equals(basicData.getBasicDataSubType())) {

				SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM);
				sqlBuilder.isEqual(MainDatabaseHandler.VAR_INVOICE_ID, basicData.getObject1Id());
				List<CostDistributionItem> costDistributionItems = findCostDistributionItemsSql(sqlBuilder);
				deleteCostDistributionItems(costDistributionItems);

				sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE);
				sqlBuilder.isEqual(MainDatabaseHandler.VAR_INVOICE_ID, basicData.getObject1Id());
				List<Invoice> invoices1 = findInvoicesSql(sqlBuilder);
				deleteInvoices(invoices1);
			}else{
				if (databaseChangeDTO.getRevisionType() == 0 || databaseChangeDTO.getRevisionType() == 1){
					updateBasicData(basicData);
				}else if (databaseChangeDTO.getRevisionType() == 2){
					List<BasicData> objects = new ArrayList<>();
					objects.add(basicData);
					deleteBasicDatas(objects);
				}
			}
		}else if (classType.equals(AppUser.class)){
			AppUser object = (AppUser) databaseChangeDTO.getMappedObject();
			if (databaseChangeDTO.getRevisionType() == 0 || databaseChangeDTO.getRevisionType() == 1){
				updateAppUser(object);
			}else if (databaseChangeDTO.getRevisionType() == 2){
				List<AppUser> objects = new ArrayList<>();
				objects.add(object);
				deleteAppUsers(objects);
			}
		}else if (classType.equals(BusinessPartner.class)){
			BusinessPartner object = (BusinessPartner) databaseChangeDTO.getMappedObject();
			if (databaseChangeDTO.getRevisionType() == 0 || databaseChangeDTO.getRevisionType() == 1){
				updateBusinessPartner(object);
			}else if (databaseChangeDTO.getRevisionType() == 2){
				List<BusinessPartner> objects = new ArrayList<>();
				objects.add(object);
				deleteBusinessPartners(objects);
			}
		}else if (classType.equals(CostDistribution.class)){
			CostDistribution object = (CostDistribution) databaseChangeDTO.getMappedObject();
			if (databaseChangeDTO.getRevisionType() == 0 || databaseChangeDTO.getRevisionType() == 1){
				updateCostDistribution(object);

				MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

				SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM);
				sqlBuilder.isEqual(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ID, object.getCostDistributionId().toString());
				List<CostDistributionItem> costDistributionItems = mainDatabaseHandler.findCostDistributionItemsSql(sqlBuilder);
				deleteCostDistributionItems(costDistributionItems);

				List<CostDistributionItem> newCostDistributionItems = object.getCostDistributionItemDTOs();
				if (newCostDistributionItems != null){
					insertCostDistributionItems(newCostDistributionItems);
				}

			}else if (databaseChangeDTO.getRevisionType() == 2){
				MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

				SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM);
				sqlBuilder.isEqual(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ID, object.getCostDistributionId().toString());
				List<CostDistributionItem> costDistributionItems = mainDatabaseHandler.findCostDistributionItemsSql(sqlBuilder);
				deleteCostDistributionItems(costDistributionItems);

				List<CostDistribution> objects = new ArrayList<>();
				objects.add(object);
				deleteCostDistributions(objects);
			}
		}else if (classType.equals(CostDistributionItem.class)){
			CostDistributionItem object = (CostDistributionItem) databaseChangeDTO.getMappedObject();
			if (databaseChangeDTO.getRevisionType() == 0 || databaseChangeDTO.getRevisionType() == 1){
				updateCostDistributionItem(object);
			}else if (databaseChangeDTO.getRevisionType() == 2){
				List<CostDistributionItem> objects = new ArrayList<>();
				objects.add(object);
				deleteCostDistributionItems(objects);
			}
		}else if (classType.equals(InvoiceCategory.class)){
			InvoiceCategory object = (InvoiceCategory) databaseChangeDTO.getMappedObject();
			if (databaseChangeDTO.getRevisionType() == 0 || databaseChangeDTO.getRevisionType() == 1){
				updateInvoiceCategory(object);
			}else if (databaseChangeDTO.getRevisionType() == 2){
				List<InvoiceCategory> objects = new ArrayList<>();
				objects.add(object);
				deleteInvoiceCategories(objects);
			}
		}else if (classType.equals(InvoiceFailure.class)){
			InvoiceFailure object = (InvoiceFailure) databaseChangeDTO.getMappedObject();
			if (databaseChangeDTO.getRevisionType() == 0 || databaseChangeDTO.getRevisionType() == 1){
				updateInvoiceFailure(object);
			}else if (databaseChangeDTO.getRevisionType() == 2){
				List<InvoiceFailure> objects = new ArrayList<>();
				objects.add(object);
				deleteInvoiceFailures(objects);
			}
		}else if (classType.equals(UserContact.class)){
			UserContact object = (UserContact) databaseChangeDTO.getMappedObject();
			if (databaseChangeDTO.getRevisionType() == 0 || databaseChangeDTO.getRevisionType() == 1){
				updateUserContact(object);
			}else if (databaseChangeDTO.getRevisionType() == 2){
				List<UserContact> objects = new ArrayList<>();
				objects.add(object);
				deleteUserContacts(objects);
			}
		}
	}
}
