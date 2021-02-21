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

package de.nextbill.client.network;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.login.LoginException;

import de.nextbill.client.R;
import de.nextbill.client.activities.ActivityArticlesSelect;
import de.nextbill.client.activities.ActivityBilling;
import de.nextbill.client.activities.ActivityBillingCreate;
import de.nextbill.client.activities.ActivityBudget;
import de.nextbill.client.activities.ActivityInvoiceDetails;
import de.nextbill.client.activities.ActivityMain;
import de.nextbill.client.activities.ActivityMessaging;
import de.nextbill.client.activities.ActivityStandingOrder;
import de.nextbill.client.activities.BaseActivity;
import de.nextbill.client.activities.DialogPaymentPerson;
import de.nextbill.client.activities.DialogUpdateRunning;
import de.nextbill.client.database.IOHelper;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.enums.BasicDataSubType;
import de.nextbill.client.enums.BasicDataType;
import de.nextbill.client.enums.BroadcastMessageActionEnum;
import de.nextbill.client.enums.DataSyncStatusEnum;
import de.nextbill.client.enums.LoginStatusEnum;
import de.nextbill.client.enums.OnlineStatusEnum;
import de.nextbill.client.enums.PaymentTypeEnum;
import de.nextbill.client.enums.Right;
import de.nextbill.client.enums.UpdateModusType;
import de.nextbill.client.exceptions.DataSyncException;
import de.nextbill.client.exceptions.LoginDataException;
import de.nextbill.client.exceptions.OfflineException;
import de.nextbill.client.exceptions.SilentOfflineException;
import de.nextbill.client.helper.ImageHelper;
import de.nextbill.client.helper.LoginUserHelper;
import de.nextbill.client.helper.RightHelper;
import de.nextbill.client.helper.StatisticHelper;
import de.nextbill.client.model.AccessTokenResponse;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.BasicData;
import de.nextbill.client.model.BusinessPartner;
import de.nextbill.client.model.CostDistribution;
import de.nextbill.client.model.CostDistributionItem;
import de.nextbill.client.model.Invoice;
import de.nextbill.client.model.InvoiceCategory;
import de.nextbill.client.model.InvoiceFailure;
import de.nextbill.client.model.MobileDevice;
import de.nextbill.client.model.StandingOrder;
import de.nextbill.client.model.UserContact;
import de.nextbill.client.pojos.AllDataPatch;
import de.nextbill.client.pojos.BillingPayment;
import de.nextbill.client.pojos.BudgetDTO;
import de.nextbill.client.pojos.DatabaseChangeDTO;
import de.nextbill.client.pojos.MessageDTO;
import de.nextbill.client.pojos.TextRecognitionDTO;
import de.nextbill.client.utils.ArticleAnalysisData;
import de.nextbill.client.utils.BillingConfig;
import de.nextbill.client.utils.BillingListItem;
import de.nextbill.client.utils.MistakeMessage;
import de.nextbill.client.utils.SqlBuilder;

public class RequestUpdateService extends IntentService {

    public static final String TAG = "RequestUpdateService";

    public static final int REQUEST_UPDATE_PENDING = 1;
    public static final int REQUEST_LOGIN = 2;
    public static final int REQUEST_TYPE_PATCH_UPDATE = 3;
    public static final int REQUEST_TYPE_FIREBASE = 4;

    public static final String KEY_TYPE = "type";
    public static final String KEY_USERNAME = "login_username";
    public static final String KEY_PASSWORD = "login_password";
    public static final String ACTIVITY_CLASS = "activity_class";
    public static final String KEY_EXTRA = "key_extra";
    public static final String KEY_TRANSACTION_TYPE = "transaction_type";


    public RequestUpdateService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.v(TAG, "::onHandleIntent -> START " + Thread.currentThread());
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        int status = handler.getStatus();
        int type = intent.getIntExtra(KEY_TYPE, -1);

        try {

            if (status == StatusDatabaseHandler.STATUS_RUNNING && type != REQUEST_UPDATE_PENDING) {
                Log.e(TAG, "::onHandleIntent -> CANCEL, STATUS_RUNNING");
                return;
            } else if (status == StatusDatabaseHandler.STATUS_MANUAL_OFFLINE) {
                throw new SilentOfflineException();
            } else if (status == StatusDatabaseHandler.STATUS_UNEXPECTED_OFFLINE) {
                RequestManager rm = new RequestManager(getApplicationContext());
                boolean domainOnline = rm.serverOnline(getApplicationContext());
                if (!domainOnline){
                    throw new SilentOfflineException();
                }
            }

            Log.v(TAG, "::onHandleIntent -> Status = " + status);

            switch (type) {
                case -1:
                    Log.w(TAG, "No Request type defined");
                    break;
                case REQUEST_TYPE_PATCH_UPDATE:
                    handler.updateStatus(StatusDatabaseHandler.STATUS_RUNNING);
                    patchUpdate(true);
                    sendRequestDone();
                    break;
                case REQUEST_UPDATE_PENDING:
                    handler.updateStatus(StatusDatabaseHandler.STATUS_RUNNING);
                    String activityClass = intent.getStringExtra(ACTIVITY_CLASS);
                    updatePendingObjects();
                    sendRequestDone(activityClass);
                    break;
//                case REQUEST_TYPE_FIREBASE:
//                    String extraString = intent.getStringExtra(KEY_EXTRA);
//                    String transactionType = intent.getStringExtra(KEY_TRANSACTION_TYPE);
//                    if ("UPDATE".equals(transactionType)){
//                        updateFirebaseToken(extraString);
//                    }else if ("DELETE".equals(transactionType)){
//                        deleteFirebaseToken(extraString);
//                    }
//                    break;
                case REQUEST_LOGIN:
                    handler.updateStatus(StatusDatabaseHandler.STATUS_RUNNING);
                    String username = intent.getStringExtra(KEY_USERNAME);
                    String password = intent.getStringExtra(KEY_PASSWORD);
                    loginUser(username, password);
                    break;
                default:
                    break;
            }

        } catch (LoginDataException e) {
            e.printStackTrace();
            handler.updateStatus(StatusDatabaseHandler.STATUS_UPDATE_DONE);

            sendLoginMessage(e.getLoginStatusEnum());
        } catch (DataSyncException e) {
            e.printStackTrace();

            handler.updateStatus(StatusDatabaseHandler.STATUS_UPDATE_DONE);

            if (e.getDataSyncStatusEnum() != null && DataSyncStatusEnum.VERSION_CONFLICT.equals(e.getDataSyncStatusEnum())) {
                sendOnlineMessage(OnlineStatusEnum.VERSION_CONFLICT);
            }else{
                sendOnlineMessage(OnlineStatusEnum.EXCEPTION);
            }
        } catch (OfflineException e) {
            e.printStackTrace();

            handler.updateStatus(StatusDatabaseHandler.STATUS_UNEXPECTED_OFFLINE);

            sendOnlineMessage(OnlineStatusEnum.UNEXPECTED_OFFLINE);
        } catch (SilentOfflineException e) {
            e.printStackTrace();

            handler.updateStatus(StatusDatabaseHandler.STATUS_MANUAL_OFFLINE);

            sendOnlineMessage(OnlineStatusEnum.MANUAL_OFFLINE);
        } catch(Exception e){
            e.printStackTrace();

            handler.updateStatus(StatusDatabaseHandler.STATUS_UNEXPECTED_OFFLINE);
            sendOnlineMessage(OnlineStatusEnum.EXCEPTION);
        }

        Log.v(TAG, "::onHandleIntent -> END " + Thread.currentThread());
    }

    private void sendRequestDone() {
        sendRequestDone(null);
    }

    private void sendRequestDone(String activityClass) {
        Log.v(TAG, TAG + "::sendRequestDone -> START ");
        Intent updateDoneIntent = new Intent();
        updateDoneIntent.setAction(RequestManager.BC_REQUEST_DONE);
        if (activityClass != null) {
            updateDoneIntent.putExtra("ACTIVITY_CLASS_NAME", activityClass);
        }
        sendBroadcast(updateDoneIntent);
    }

    private void sendLoginMessage(LoginStatusEnum loginStatusEnum) {
        Intent startIntent = new Intent();
        startIntent.setAction(RequestManager.BC_LOGIN);
        startIntent.putExtra("LOGIN_STATUS_ENUM", loginStatusEnum.name());
        sendBroadcast(startIntent);
    }

    private void sendOnlineMessage(OnlineStatusEnum onlineStatusEnum) {
        Intent startIntent = new Intent();
        startIntent.setAction(RequestManager.BC_ONLINE);
        startIntent.putExtra("ONLINE_STATUS_ENUM", onlineStatusEnum.name());
        sendBroadcast(startIntent);
    }

    private void sendMessage(Class activityClassName, BroadcastMessageActionEnum broadcastMessageActionEnum, String message) {
        Intent doneIntent = new Intent();
        doneIntent.setAction(RequestManager.BC_SEND_MESSAGE);
        doneIntent.putExtra("ACTIVITY_CLASS_NAME", activityClassName.getSimpleName());
        doneIntent.putExtra("ACTION", broadcastMessageActionEnum.name());
        doneIntent.putExtra("PROGRESS_MESSAGE", message);
        sendBroadcast(doneIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "::onDestroy -> START " + Thread.currentThread());
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        int status = handler.getStatus();
        if (status != StatusDatabaseHandler.STATUS_MANUAL_OFFLINE) {
            handler.updateStatus(StatusDatabaseHandler.STATUS_UPDATE_DONE);
        }
        stopSelf();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int retVal = super.onStartCommand(intent, flags, startId);
        Log.v(TAG, "::onStartCommand -> START " + Thread.currentThread());
        return retVal;
    }

    private void updatePendingObjects() throws LoginDataException, OfflineException, LoginException, SilentOfflineException, DataSyncException {

        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        List<StatusDatabaseHandler.UpdateObject> updateObjects = handler.getUpdateObjects();

        for (StatusDatabaseHandler.UpdateObject tmpUpdateObject : updateObjects) {
            if (tmpUpdateObject.objectType == StatusDatabaseHandler.OBJECT_TYPE_PATCH_UPDATE) {
                handler.deleteObject(tmpUpdateObject.dbId);
                patchUpdate(false);
            }else if(tmpUpdateObject.objectType == StatusDatabaseHandler.OBJECT_TYPE_INVOICE) {
                if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_ADD) {
                    insertInvoice(tmpUpdateObject);
                } else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_PUT) {
                    updateInvoice(tmpUpdateObject);
                } else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_DELETE) {
                    deleteInvoice(tmpUpdateObject);
                } else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_GET) {
                    getInvoice(tmpUpdateObject);
                }
            } else if (tmpUpdateObject.objectType == StatusDatabaseHandler.OBJECT_TYPE_INVOICE_FAILURE) {
                if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_DELETE) {
                    deleteInvoiceFailure(tmpUpdateObject);
                }
            } else if (tmpUpdateObject.objectType == StatusDatabaseHandler.OBJECT_TYPE_INVOICE_IMAGE) {
                if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_ADD) {
                    uploadInvoiceImageOcr(tmpUpdateObject);
                } else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_PUT) {
                    uploadInvoiceImage(tmpUpdateObject);
                } else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_GET) {
                    downloadInvoiceImage(tmpUpdateObject);
                } else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_DELETE) {
                    deleteInvoiceImage(tmpUpdateObject);
                } else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_GET_VALUE) {
                    analyzeArticle(tmpUpdateObject);
                }
            } else if (tmpUpdateObject.objectType == StatusDatabaseHandler.OBJECT_TYPE_BUSINESS_PARTNER) {
                if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_ADD) {
                    insertBusinessPartner(tmpUpdateObject);
                } else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_DELETE) {
                    deleteBusinessPartner(tmpUpdateObject);
                }
            } else if (tmpUpdateObject.objectType == StatusDatabaseHandler.OBJECT_TYPE_COST_DISTRIBUTION_ITEM) {
                if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_ADD) {
                    insertCostDistributionItem(tmpUpdateObject);
                } else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_PUT) {
                    updateCostDistributionItem(tmpUpdateObject);
                } else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_DELETE) {
                    deleteCostDistributionItem(tmpUpdateObject);
                }
            } else if (tmpUpdateObject.objectType == StatusDatabaseHandler.OBJECT_TYPE_COST_DISTRIBUTION) {
                if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_ADD) {
                    insertCostDistribution(tmpUpdateObject);
                } else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_PUT) {
                    updateCostDistribution(tmpUpdateObject);
                } else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_DELETE) {
                    deleteCostDistribution(tmpUpdateObject);
                }
            } else if (tmpUpdateObject.objectType == StatusDatabaseHandler.OBJECT_TYPE_APP_USER) {
                if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_GET) {
                    getAppUserByEmail(tmpUpdateObject);
                }
            } else if (tmpUpdateObject.objectType == StatusDatabaseHandler.OBJECT_TYPE_USER_CONTACT) {
                if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_ADD) {
                    insertUserContact(tmpUpdateObject);
                } else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_DELETE) {
                    deleteUserContact(tmpUpdateObject);
                } else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_PUT) {
                    updateUserContact(tmpUpdateObject);
                }
            } else if (tmpUpdateObject.objectType == StatusDatabaseHandler.OBJECT_TYPE_INVOICE_CATEGORY) {
                if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_ADD) {
                    insertInvoiceCategory(tmpUpdateObject);
                } else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_DELETE) {
                    deleteInvoiceCategory(tmpUpdateObject);
                }
            } else if (tmpUpdateObject.objectType == StatusDatabaseHandler.OBJECT_TYPE_STANDING_ORDER) {
                if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_ADD) {
                    insertStandingOrder(tmpUpdateObject);
                } else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_PUT) {
                    updateStandingOrder(tmpUpdateObject);
                } else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_DELETE_SPECIAL) {
                    deleteStandingOrderByCreatedInvoice(tmpUpdateObject);
                } else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_DELETE) {
                    deleteStandingOrderByInvoiceTemplate(tmpUpdateObject);
                }
            } else if (tmpUpdateObject.objectType == StatusDatabaseHandler.OBJECT_TYPE_BILLING) {
                if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_ADD) {
                    generateBilling(tmpUpdateObject);
                }else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_UPDATE) {
                    updatePaymentStatus(tmpUpdateObject);
                }else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_DELETE) {
                    deleteBilling(tmpUpdateObject);
                }
            }else if (tmpUpdateObject.objectType == StatusDatabaseHandler.OBJECT_TYPE_BASIC_DATA) {
                if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_GET_LIST) {
                    refreshMessageBasicData(tmpUpdateObject);
                } else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_DELETE) {
                    deleteBasicData(tmpUpdateObject);
                }
            }else if (tmpUpdateObject.objectType == StatusDatabaseHandler.OBJECT_TYPE_BILLING_LIST_ITEMS){
                if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_GET) {
                    getBillingListItems(tmpUpdateObject);
                }
            }else if (tmpUpdateObject.objectType == StatusDatabaseHandler.OBJECT_TYPE_BUDGET){
                if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_GET_LIST) {
                    getBudgetListItems(tmpUpdateObject);
                }else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_ADD) {
                    insertBudget(tmpUpdateObject);
                }else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_PUT) {
                    updateBudget(tmpUpdateObject);
                }else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_DELETE) {
                    deleteBudget(tmpUpdateObject);
                }
            }else if (tmpUpdateObject.objectType == StatusDatabaseHandler.OBJECT_TYPE_PAYMENT_MISTAKE){
                if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_ADD){
                    generateMistakeMessage(tmpUpdateObject);
                }else if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_DELETE){
                    deleteMistakeMessage(tmpUpdateObject);
                }
            }else if (tmpUpdateObject.objectType == StatusDatabaseHandler.OBJECT_TYPE_SPEECH_RECOGNITION){
                if (tmpUpdateObject.updateStatus == StatusDatabaseHandler.UPDATE_STATUS_ADD){
                    uploadSpeechRecognition(tmpUpdateObject);
                }
            }
        }

    }

    public void generateInvoiceImages(String invoiceId) {

        boolean successfull = false;
        File inputFile = IOHelper.getTempUploadMediaFile(invoiceId);
        try {

            if (!inputFile.exists()) {
                throw new IOException();
            }

            IOHelper ioHelper = IOHelper.getInstance();
            ioHelper.setCtx(getApplicationContext());
            String pathTmp = ioHelper.getImageDirectory();

            File wifiFile = new File(pathTmp + "/" + invoiceId + "_uploadWifi");

            File normalFile = new File(pathTmp + "/" + invoiceId + "_uploadNormal");

            Bitmap unscaledBitmap = ImageHelper.decodeFile(inputFile.getAbsolutePath(), 2500, 2500);
            Bitmap scaledBitmap = ImageHelper.createScaledBitmap(2500, 2500, unscaledBitmap);
            OutputStream os = new BufferedOutputStream(new FileOutputStream(wifiFile));
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 96, os);
            os.close();

            unscaledBitmap = ImageHelper.decodeFile(inputFile.getAbsolutePath(), 2000, 2000);
            scaledBitmap = ImageHelper.createScaledBitmap(2000, 2000, unscaledBitmap);
            os = new BufferedOutputStream(new FileOutputStream(normalFile));
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, os);
            os.close();

            successfull = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (successfull){
            if (inputFile.exists()){
                inputFile.delete();
            }
        }else{
            sendMessage(ActivityInvoiceDetails.class, BroadcastMessageActionEnum.ERROR, "image");
        }
    }

    public void getBillingListItems(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws OfflineException, DataSyncException, LoginDataException {

        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());

        try{
            RequestManager rm = new RequestManager(this.getApplicationContext());

            List<BillingListItem> billingListItems = rm.billingListItems();

            List<BillingListItem> returnBillingListItems = new ArrayList<>();
            for (BillingListItem billingListItem : billingListItems) {
                if (billingListItem.getSumToBePaid() != null && billingListItem.getSumToBePaid().compareTo(new BigDecimal(0)) != 0){
                    returnBillingListItems.add(billingListItem);
                }
            }

            Gson gson = new GsonBuilder()
                    .setDateFormat("dd MMM yyyy HH:mm:ss").create();
            String jsonString = gson.toJson(returnBillingListItems);

            sendMessage(ActivityBilling.class, BroadcastMessageActionEnum.READY, jsonString);
        } catch (Exception e) {
            sendMessage(ActivityBilling.class, BroadcastMessageActionEnum.ERROR, "error");
            throw e;
        } finally {
            handler.deleteObject(tmpUpdateObject.dbId);
        }

    }

    public void getBudgetListItems(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws OfflineException, LoginDataException, DataSyncException {

        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());

        try{
            RequestManager rm = new RequestManager(this.getApplicationContext());

            List<BudgetDTO> budgetDTOS = rm.budgets();

            Gson gson = new GsonBuilder().create();
            String jsonString = gson.toJson(budgetDTOS);

            sendMessage(ActivityBudget.class, BroadcastMessageActionEnum.READY, jsonString);
        } catch (Exception e){
            sendMessage(ActivityBudget.class, BroadcastMessageActionEnum.ERROR, "");
            throw e;
        } finally {
            handler.deleteObject(tmpUpdateObject.dbId);
        }
    }

    public void isNotificationNecessary(final Set<String> messageIds){

        new Thread(new Runnable() {
            public void run() {

                try{

                    MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());

                    SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
                    sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.MESSAGE.name())
                            .or()
                            .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.BUDGET_MESSAGE.name())
                            .or()
                            .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.CHANGED_MESSAGE.name())
                            .or()
                            .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.MISTAKE_MESSAGE.name());
                    List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

                    for (BasicData basicData : basicDatas) {
                        GsonBuilder builder = new GsonBuilder();
                        Gson gson = builder.create();
                        MessageDTO messageDTO = gson.fromJson(basicData.getValue(), MessageDTO.class);

                        if (!messageIds.contains(basicData.getObject1Id() + "_" + messageDTO.getMessage())) {
                            sendNotification(messageDTO.getSubject(), messageDTO.getMessage());
                        }
                    }
                }catch (Exception e){

                }
            }
        }).start();
    }

    public void calculateRestSum(){

        new Thread(new Runnable() {
            public void run() {

                try{
                    AppUser appUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());

                    if (appUser == null) {
                        sendMessage(BaseActivity.class, BroadcastMessageActionEnum.ERROR, appUser.getAppUserId().toString());
                        return;
                    }

                    BigDecimal restSumForMonth = StatisticHelper.restSumForMonth(getApplicationContext()).setScale(0, RoundingMode.HALF_EVEN);

                    MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());

                    SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
                    sqlBuilder.isEqual(MainDatabaseHandler.VAR_APP_USER_ID, appUser.getAppUserId().toString())
                            .and()
                            .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.STATISTIC.name())
                            .and()
                            .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_SUB_TYPE, BasicDataSubType.CALCULATED_PROFIT_FOR_MONTH.name());
                    List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

                    BasicData basicData = null;
                    if (!basicDatas.isEmpty()){
                        basicData = basicDatas.get(0);
                        basicData.setNumberValue(restSumForMonth);
                        mainDatabaseHandler.updateBasicData(basicData);
                    }else{
                        basicData = new BasicData();
                        basicData.setNumberValue(restSumForMonth);
                        basicData.setAppUserId(appUser.getAppUserId());
                        basicData.setBasicDataType(BasicDataType.STATISTIC);
                        basicData.setBasicDataSubType(BasicDataSubType.CALCULATED_PROFIT_FOR_MONTH);
                        mainDatabaseHandler.insertBasicData(basicData);
                    }

                    sendMessage(BaseActivity.class, BroadcastMessageActionEnum.REFRESH, appUser.getAppUserId().toString());
                }catch (Exception e){

                }
            }
        }).start();
    }

    private void deleteInvoice(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        rm.deleteInvoice(UUID.fromString(tmpUpdateObject.objectId));

        calculateRestSum();
        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void updateInvoice(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        List<Invoice> invoices = mainDatabaseHandler.findInvoices(MainDatabaseHandler.VAR_INVOICE_ID, tmpUpdateObject.objectId);

        if (invoices.isEmpty()) {
            handler.deleteObject(tmpUpdateObject.dbId);
            Log.e(TAG, "Invoice update: No Invoice found!");
            return;
        }

        Invoice tmpInvoice = invoices.get(0);

        rm.updateInvoice(tmpInvoice);

        calculateRestSum();

        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void insertInvoice(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        List<Invoice> invoices = mainDatabaseHandler.findInvoices(MainDatabaseHandler.VAR_INVOICE_ID, tmpUpdateObject.objectId);

        if (invoices.isEmpty()) {
            handler.deleteObject(tmpUpdateObject.dbId);
            Log.e(TAG, "Invoice insert: No Invoice found!");
            return;
        }

        Invoice tmpInvoice = invoices.get(0);

        rm.insertInvoice(tmpInvoice);

        calculateRestSum();

        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void refreshMessageBasicData(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());
        try {
            RequestManager rm = new RequestManager(this.getApplicationContext());

            List<BasicData> basicDatas = rm.basicDataList();

            SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
            sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.MESSAGE.name());

            List<BasicData> basicDatasToDelete = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);
            mainDatabaseHandler.deleteBasicDatas(basicDatasToDelete);
            for (BasicData basicData : basicDatas) {
                if (BasicDataType.MESSAGE.equals(basicData.getBasicDataType())){
                    mainDatabaseHandler.insertUpdateBasicData(basicData);
                }
            }

            sendMessage(ActivityMessaging.class, BroadcastMessageActionEnum.REFRESH, "");
        } catch (Exception e){
            sendMessage(ActivityMessaging.class, BroadcastMessageActionEnum.ERROR, "");
            throw e;
        } finally {
            handler.deleteObject(tmpUpdateObject.dbId);
        }
    }


    private void getInvoice(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {

        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());

        if (tmpUpdateObject.objectId == null){
            handler.deleteObject(tmpUpdateObject.dbId);
            return;
        }

        getInvoice(tmpUpdateObject.objectId);

        sendMessage(ActivityMain.class, BroadcastMessageActionEnum.REFRESH, "");

        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void getInvoice(String invoiceId) throws LoginDataException, OfflineException, DataSyncException {
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        Invoice newInvoice = rm.getInvoice(UUID.fromString(invoiceId));

        mainDatabaseHandler.insertUpdateInvoice(newInvoice);
    }

    private void getAppUserByEmail(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());

        try {
            RequestManager rm = new RequestManager(this.getApplicationContext());

            if (tmpUpdateObject.objectId == null || tmpUpdateObject.objectId.equals("")) {
                handler.deleteObject(tmpUpdateObject.dbId);
                Log.e(TAG, "AppUser get: No E-Mail-Address found!");
                return;
            }

            AppUser appUser = rm.getAppUserByEmail(tmpUpdateObject.objectId);

            if (appUser != null) {
                mainDatabaseHandler.updateAppUser(appUser);
                sendMessage(DialogPaymentPerson.class, BroadcastMessageActionEnum.FOUND, appUser.getAppUserId().toString());
            } else {
                sendMessage(DialogPaymentPerson.class, BroadcastMessageActionEnum.NOT_FOUND, "");
            }

        } catch (Exception e){
            sendMessage(DialogPaymentPerson.class, BroadcastMessageActionEnum.ERROR, "");
            throw e;
        } finally {
            handler.deleteObject(tmpUpdateObject.dbId);
        }
    }

    private void updateCostDistributionItem(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        List<CostDistributionItem> costDistributionItems = mainDatabaseHandler.findCostDistributionItems(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ITEM_ID, tmpUpdateObject.objectId);

        if (costDistributionItems.isEmpty()) {
            handler.deleteObject(tmpUpdateObject.dbId);
            Log.e(TAG, "CostDistributionItem update: No CostDistributionItem found!");
            return;
        }

        CostDistributionItem tmpCostDistributionItem = costDistributionItems.get(0);

        rm.updateCostDistributionItem(tmpCostDistributionItem);

        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void updateCostDistribution(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        List<CostDistribution> costDistributions = mainDatabaseHandler.findCostDistributions(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ID, tmpUpdateObject.objectId);

        if (costDistributions.isEmpty()) {
            handler.deleteObject(tmpUpdateObject.dbId);
            Log.e(TAG, "CostDistribution update: No CostDistribution found!");
            return;
        }

        CostDistribution tmpCostDistribution = costDistributions.get(0);

        rm.updateCostDistribution(tmpCostDistribution);

        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void updateUserContact(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        List<UserContact> userContacts = mainDatabaseHandler.findUserContacts(MainDatabaseHandler.VAR_USER_CONTACT_ID, tmpUpdateObject.objectId);

        if (userContacts.isEmpty()) {
            handler.deleteObject(tmpUpdateObject.dbId);
            Log.e(TAG, "UserContact update: No UserContact found!");
            return;
        }

        UserContact tmpUserContact = userContacts.get(0);

        rm.updateUserContact(tmpUserContact);

        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void insertBudget(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
                try {
                    return simpleDateFormat.parse(json.getAsJsonPrimitive().getAsString());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        Gson gson = builder.create();
        BudgetDTO budgetDTO = gson.fromJson(tmpUpdateObject.objectId, BudgetDTO.class);

        budgetDTO.setPaymentTypeEnum(PaymentTypeEnum.NOT_DEFINED.equals(budgetDTO.getPaymentTypeEnum()) ? null : budgetDTO.getPaymentTypeEnum());

        rm.insertBudget(budgetDTO);

        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void updateBudget(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
                try {
                    return simpleDateFormat.parse(json.getAsJsonPrimitive().getAsString());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        Gson gson = builder.create();
        BudgetDTO budgetDTO = gson.fromJson(tmpUpdateObject.objectId, BudgetDTO.class);

        budgetDTO.setPaymentTypeEnum(PaymentTypeEnum.NOT_DEFINED.equals(budgetDTO.getPaymentTypeEnum()) ? null : budgetDTO.getPaymentTypeEnum());

        rm.updateBudget(budgetDTO);

        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void updatePaymentStatus(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException, LoginException, SilentOfflineException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        Type listType = new TypeToken<BillingPayment>() {
        }.getType();
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
                try {
                    return simpleDateFormat.parse(json.getAsJsonPrimitive().getAsString());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        Gson gson = builder.create();
        BillingPayment billingPayment = gson.fromJson(tmpUpdateObject.objectId, listType);

        rm.updatePaymentStatus(billingPayment);

        handler.deleteObject(tmpUpdateObject.dbId);

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
        AppUser currentUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());
        currentUser.setUpdateModusType(UpdateModusType.PATCH_UPDATE);
        mainDatabaseHandler.updateAppUser(currentUser);
    }

    private void insertCostDistributionItem(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        List<CostDistributionItem> costDistributionItems = mainDatabaseHandler.findCostDistributionItems(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ITEM_ID, tmpUpdateObject.objectId);

        if (costDistributionItems.isEmpty()) {
            handler.deleteObject(tmpUpdateObject.dbId);
            Log.e(TAG, "CostDistributionItem insert: No CostDistributionItem found!");
            return;
        }

        CostDistributionItem tmpCostDistributionItem = costDistributionItems.get(0);

        rm.insertCostDistributionItem(tmpCostDistributionItem);

        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void insertCostDistribution(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        List<CostDistribution> costDistributions = mainDatabaseHandler.findCostDistributions(MainDatabaseHandler.VAR_COST_DISTRIBUTION_ID, tmpUpdateObject.objectId);

        if (costDistributions.isEmpty()) {
            handler.deleteObject(tmpUpdateObject.dbId);
            Log.e(TAG, "CostDistribution insert: No CostDistribution found!");
            return;
        }

        CostDistribution tmpCostDistribution = costDistributions.get(0);

        rm.insertCostDistribution(tmpCostDistribution);

        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void insertUserContact(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        List<UserContact> userContacts = mainDatabaseHandler.findUserContacts(MainDatabaseHandler.VAR_USER_CONTACT_ID, tmpUpdateObject.objectId);

        if (userContacts.isEmpty()) {
            handler.deleteObject(tmpUpdateObject.dbId);
            Log.e(TAG, "UserContact insert: No UserContact found!");
            return;
        }

        UserContact tmpUserContact = userContacts.get(0);

        rm.insertUserContact(tmpUserContact);

        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void insertInvoiceCategory(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE_CATEGORY);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_INVOICE_CATEGORY_ID, tmpUpdateObject.objectId);

        List<InvoiceCategory> invoiceCategories = mainDatabaseHandler.findInvoiceCategoriesSql(sqlBuilder);

        if (invoiceCategories.isEmpty()) {
            handler.deleteObject(tmpUpdateObject.dbId);
            Log.e(TAG, "InvoiceCategory insert: No InvoiceCategory found!");
            return;
        }

        InvoiceCategory tmpInvoiceCategory = invoiceCategories.get(0);

        rm.insertInvoiceCategory(tmpInvoiceCategory);

        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void insertBusinessPartner(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        List<BusinessPartner> businessPartners = mainDatabaseHandler.findBusinessPartners(MainDatabaseHandler.VAR_BUSINESS_PARTNER_ID, tmpUpdateObject.objectId);

        if (businessPartners.isEmpty()) {
            handler.deleteObject(tmpUpdateObject.dbId);
            Log.e(TAG, "BusinessPartner insert: No BusinessPartner found!");
            return;
        }

        BusinessPartner tmpBusinessPartner = businessPartners.get(0);

        rm.insertBusinessPartner(tmpBusinessPartner);

        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void deleteMistakeMessage(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        rm.deleteMistakeMessage(UUID.fromString(tmpUpdateObject.objectId));

        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void generateMistakeMessage(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        Type listType = new TypeToken<MistakeMessage>() {
        }.getType();
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        MistakeMessage mistakeMessage = gson.fromJson(tmpUpdateObject.objectId, listType);

        rm.generateMistakeMessage(mistakeMessage);

        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void deleteBilling(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        rm.deleteBilling(UUID.fromString(tmpUpdateObject.objectId));

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
        AppUser currentUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());
        currentUser.setUpdateModusType(UpdateModusType.PATCH_UPDATE);
        mainDatabaseHandler.updateAppUser(currentUser);

        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void generateBilling(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException, LoginException, SilentOfflineException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());

        try {
            RequestManager rm = new RequestManager(this.getApplicationContext());

            Type listType = new TypeToken<BillingConfig>() {
            }.getType();
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
                public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
                    try {
                        return simpleDateFormat.parse(json.getAsJsonPrimitive().getAsString());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            });
            Gson gson = builder.create();
            BillingConfig billingConfig = gson.fromJson(tmpUpdateObject.objectId, listType);

            rm.generateBilling(billingConfig);

            handler.deleteObject(tmpUpdateObject.dbId);

            MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
            AppUser currentUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());
            currentUser.setUpdateModusType(UpdateModusType.PATCH_UPDATE);
            mainDatabaseHandler.updateAppUser(currentUser);

            sendMessage(ActivityBillingCreate.class, BroadcastMessageActionEnum.READY, "");
        } catch (Exception e) {
            sendMessage(ActivityBillingCreate.class, BroadcastMessageActionEnum.ERROR, "");
            throw e;
        } finally {
            handler.deleteObject(tmpUpdateObject.dbId);
        }
    }

    private void insertStandingOrder(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());

        try {
            RequestManager rm = new RequestManager(this.getApplicationContext());

            Type listType = new TypeToken<StandingOrder>() {
            }.getType();
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
                public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MM yyyy HH:mm:ss");
                    try {
                        return simpleDateFormat.parse(json.getAsJsonPrimitive().getAsString());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            });
            Gson gson = builder.create();
            StandingOrder standingOrder = gson.fromJson(tmpUpdateObject.objectId, listType);

            rm.insertStandingOrder(standingOrder);

            sendMessage(ActivityInvoiceDetails.class, BroadcastMessageActionEnum.READY, "StandingOrderCreated");
            sendMessage(ActivityStandingOrder.class, BroadcastMessageActionEnum.READY, "StandingOrderCreated");

            handler.deleteObject(tmpUpdateObject.dbId);
        } catch (Exception e) {
            sendMessage(ActivityInvoiceDetails.class, BroadcastMessageActionEnum.ERROR, "StandingOrderCreated");
            sendMessage(ActivityStandingOrder.class, BroadcastMessageActionEnum.ERROR, "StandingOrderCreated");
            throw e;
        }
    }

    private void deleteStandingOrderFromInvoices(UUID invoiceTemplateId){

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());

        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_INVOICE_STANDING_ORDER_INVOICE_TEMPLATE_ID, invoiceTemplateId.toString());

        List<Invoice> invoices1 = mainDatabaseHandler.findInvoicesSql(sqlBuilder);

        for (Invoice invoice : invoices1) {
            invoice.setStandingOrderInvoiceTemplateId(null);
            mainDatabaseHandler.updateInvoice(invoice);
        }
    }

    private void updateStandingOrder(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());

        try {
            RequestManager rm = new RequestManager(this.getApplicationContext());

            Type listType = new TypeToken<StandingOrder>() {
            }.getType();
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
                public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MM yyyy HH:mm:ss");
                    try {
                        return simpleDateFormat.parse(json.getAsJsonPrimitive().getAsString());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            });
            Gson gson = builder.create();
            StandingOrder standingOrder = gson.fromJson(tmpUpdateObject.objectId, listType);

            rm.updateStandingOrder(standingOrder);

            sendMessage(ActivityStandingOrder.class, BroadcastMessageActionEnum.READY, "StandingOrderUpdated");

            handler.deleteObject(tmpUpdateObject.dbId);
        } catch (Exception e) {
            sendMessage(ActivityStandingOrder.class, BroadcastMessageActionEnum.ERROR, "StandingOrderUpdated");
            throw e;
        }
    }

    private void deleteStandingOrderByCreatedInvoice(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());

        try {
            RequestManager rm = new RequestManager(this.getApplicationContext());

            rm.deleteStandingOrderByCreatedInvoice(UUID.fromString(tmpUpdateObject.objectId));

            sendMessage(ActivityInvoiceDetails.class, BroadcastMessageActionEnum.READY, "StandingOrderDeleted");

            handler.deleteObject(tmpUpdateObject.dbId);
        } catch (Exception e) {
            sendMessage(ActivityInvoiceDetails.class, BroadcastMessageActionEnum.ERROR, "StandingOrderDeleted");
            throw e;
        }
    }

    private void deleteStandingOrderByInvoiceTemplate(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());

        try {
            RequestManager rm = new RequestManager(this.getApplicationContext());

            rm.deleteStandingOrderByInvoiceTemplate(UUID.fromString(tmpUpdateObject.objectId));

            deleteStandingOrderFromInvoices(UUID.fromString(tmpUpdateObject.objectId));

            sendMessage(ActivityInvoiceDetails.class, BroadcastMessageActionEnum.READY, "StandingOrderDeleted");
            sendMessage(ActivityStandingOrder.class, BroadcastMessageActionEnum.READY, "StandingOrderDeleted");

            handler.deleteObject(tmpUpdateObject.dbId);
        } catch (Exception e) {
            sendMessage(ActivityInvoiceDetails.class, BroadcastMessageActionEnum.ERROR, "StandingOrderDeleted");
            sendMessage(ActivityStandingOrder.class, BroadcastMessageActionEnum.ERROR, "StandingOrderDeleted");
            throw e;
        }
    }

    private void deleteInvoiceFailure(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());

        RequestManager rm = new RequestManager(this.getApplicationContext());

        rm.deleteInvoiceFailure(UUID.fromString(tmpUpdateObject.objectId));
        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void deleteBudget(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        rm.deleteBudget(UUID.fromString(tmpUpdateObject.objectId));
        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void deleteCostDistributionItem(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        rm.deleteCostDistributionItem(UUID.fromString(tmpUpdateObject.objectId));
        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void deleteBusinessPartner(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        rm.deleteBusinessPartner(UUID.fromString(tmpUpdateObject.objectId));
        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void deleteUserContact(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        rm.deleteUserContact(UUID.fromString(tmpUpdateObject.objectId));
        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void deleteInvoiceCategory(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        rm.deleteInvoiceCategory(UUID.fromString(tmpUpdateObject.objectId));
        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void deleteBasicData(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        rm.deleteBasicData(UUID.fromString(tmpUpdateObject.objectId));
        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void deleteCostDistribution(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        rm.deleteCostDistribution(UUID.fromString(tmpUpdateObject.objectId));
        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void deleteInvoiceImage(StatusDatabaseHandler.UpdateObject tmpUpdateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        RequestManager rm = new RequestManager(this.getApplicationContext());

        rm.deleteInvoiceImage(UUID.fromString(tmpUpdateObject.objectId));
        handler.deleteObject(tmpUpdateObject.dbId);
    }

    private void downloadInvoiceImage(StatusDatabaseHandler.UpdateObject updateObject) throws LoginDataException, OfflineException, DataSyncException {
        downloadInvoiceImage(updateObject.objectId, updateObject);
    }

    private void downloadInvoiceImage(String invoiceId, StatusDatabaseHandler.UpdateObject updateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());

        try {
            RequestManager rm = new RequestManager(this.getApplicationContext());

            MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());

            List<Invoice> invoices = mainDatabaseHandler.findInvoices(MainDatabaseHandler.VAR_INVOICE_ID, invoiceId);

            Invoice tmpInvoice = null;
            if (invoices.isEmpty()) {
                Log.e(TAG, "Image download: No Invoice found!");
                throw new DataSyncException();
            }

            tmpInvoice = invoices.get(0);

            if (tmpInvoice.getInvoiceImageId() == null) {
                Log.e(TAG, "Image download: No InvoiceImageId found!");
                throw new DataSyncException();
            }

            rm.downloadInvoiceImage(tmpInvoice, getApplicationContext());

            mainDatabaseHandler.updateInvoice(tmpInvoice);

            sendMessage(ActivityInvoiceDetails.class, BroadcastMessageActionEnum.REFRESH, "image");
        } catch (IOException e) {
            sendMessage(ActivityInvoiceDetails.class, BroadcastMessageActionEnum.ERROR, "image");
            throw new DataSyncException();
        } catch (DataSyncException e) {
            sendMessage(ActivityInvoiceDetails.class, BroadcastMessageActionEnum.ERROR, "image");
        } catch (Exception e) {
            sendMessage(ActivityInvoiceDetails.class, BroadcastMessageActionEnum.ERROR, "image");
            throw e;
        } finally {
            if (updateObject != null) handler.deleteObject(updateObject.dbId);

        }
    }

    private void analyzeArticle(StatusDatabaseHandler.UpdateObject updateObject) throws LoginDataException, OfflineException, DataSyncException {
        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());

        try{
            RequestManager rm = new RequestManager(this.getApplicationContext());

            Type listType = new TypeToken<ArticleAnalysisData>() {
            }.getType();
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            ArticleAnalysisData articleAnalysisData = gson.fromJson(updateObject.objectId, listType);

            String analyzedArticleJson = rm.analyzeArticle(articleAnalysisData);
            if (analyzedArticleJson != null && !analyzedArticleJson.equals("")){
                sendMessage(ActivityArticlesSelect.class, BroadcastMessageActionEnum.READY, analyzedArticleJson);
            }else{
                sendMessage(ActivityArticlesSelect.class, BroadcastMessageActionEnum.ERROR, "");
            }
        }catch (Exception e) {
            sendMessage(ActivityArticlesSelect.class, BroadcastMessageActionEnum.ERROR, "");
            throw e;
        }finally {
            handler.deleteObject(updateObject.dbId);
        }
    }

    private void uploadInvoiceImage(StatusDatabaseHandler.UpdateObject updateObject) throws LoginDataException, OfflineException, DataSyncException {

        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());

        File imageFile = null;
        try {
            RequestManager rm = new RequestManager(this.getApplicationContext());

            MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());

            generateInvoiceImages(updateObject.objectId);

            List<Invoice> invoices = mainDatabaseHandler.findInvoices(MainDatabaseHandler.VAR_INVOICE_ID, updateObject.objectId);

            if (invoices.isEmpty()) {
                handler.deleteObject(updateObject.dbId);
                Log.e(TAG, "Image upload: No Invoice found!");
                throw new DataSyncException();
            }

            Invoice tmpInvoice = invoices.get(0);

            IOHelper ioHelper = IOHelper.getInstance();
            ioHelper.setCtx(getApplicationContext());
            String pathTmp = IOHelper.getImageDirectory();

            File wifiFile = new File(pathTmp + "/" + tmpInvoice.getInvoiceId().toString() + "_uploadWifi");

            File normalFile = new File(pathTmp + "/" + tmpInvoice.getInvoiceId().toString() + "_uploadNormal");

            if (NetworkUtils.getConnectivityStatus(getApplicationContext()) == NetworkUtils.TYPE_WIFI){
                imageFile = wifiFile;
            }else if (NetworkUtils.getConnectivityStatus(getApplicationContext()) == NetworkUtils.TYPE_MOBILE){
                imageFile = normalFile;
            }else{
                imageFile = normalFile;
            }

            if (!imageFile.exists()){
                handler.deleteObject(updateObject.dbId);
                if (normalFile.exists())  normalFile.delete();
                if (wifiFile.exists())  wifiFile.delete();
                throw new DataSyncException();
            }

            Invoice createdInvoice = rm.uploadInvoiceImage(tmpInvoice, imageFile, false);

            mainDatabaseHandler.insertUpdateInvoice(createdInvoice);
            if (normalFile.exists())  normalFile.delete();
            if (wifiFile.exists())  wifiFile.delete();

            handler.deleteObject(updateObject.dbId);

            imageFile.delete();

            downloadInvoiceImage(createdInvoice.getInvoiceId().toString(), null);
        } catch (Exception e) {
            handler.deleteObject(updateObject.dbId);
            if (imageFile != null) {
                imageFile.delete();
            }
            sendMessage(ActivityInvoiceDetails.class, BroadcastMessageActionEnum.ERROR, "image");
            throw e;
        }

    }

    private void uploadInvoiceImageOcr(StatusDatabaseHandler.UpdateObject updateObject) throws LoginDataException, OfflineException, DataSyncException, LoginException, SilentOfflineException {

        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());

        try {
            RequestManager rm = new RequestManager(this.getApplicationContext());

            MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());

            generateInvoiceImages(updateObject.objectId);

            List<Invoice> invoices = mainDatabaseHandler.findInvoices(MainDatabaseHandler.VAR_INVOICE_ID, updateObject.objectId);

            if (invoices.isEmpty()) {
                handler.deleteObject(updateObject.dbId);
                Log.e(TAG, "Image upload: No Invoice found!");
                throw new DataSyncException();
            }

            Invoice tmpInvoice = invoices.get(0);

            IOHelper ioHelper = IOHelper.getInstance();
            ioHelper.setCtx(getApplicationContext());
            String pathTmp = ioHelper.getImageDirectory();

            File imageFile = null;

            File wifiFile = new File(pathTmp + "/" + tmpInvoice.getInvoiceId().toString() + "_uploadWifi");

            File normalFile = new File(pathTmp + "/" + tmpInvoice.getInvoiceId().toString() + "_uploadNormal");

            if (NetworkUtils.getConnectivityStatus(getApplicationContext()) != NetworkUtils.TYPE_WIFI){
                imageFile = wifiFile;
                normalFile.delete();
            }else if (NetworkUtils.getConnectivityStatus(getApplicationContext()) != NetworkUtils.TYPE_MOBILE){
                imageFile = normalFile;
                wifiFile.delete();
            }

            if (!imageFile.exists()){
                handler.deleteObject(updateObject.dbId);
                throw new DataSyncException();
            }

            Invoice invoiceCreated = rm.uploadInvoiceImage(tmpInvoice, imageFile, true);

            if (invoiceCreated != null) {
                mainDatabaseHandler.insertUpdateInvoice(invoiceCreated);

                if (invoiceCreated.getInvoiceFailureMessage() != null){
                    handler.deleteObject(updateObject.dbId);

                    AppUser appUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());
                    if (appUser != null){
                        appUser.setUpdateModusType(UpdateModusType.PATCH_UPDATE);
                        mainDatabaseHandler.updateAppUser(appUser);
                    }
                }
            }

            imageFile.delete();

            handler.deleteObject(updateObject.dbId);
            sendMessage(ActivityMain.class, BroadcastMessageActionEnum.REFRESH, "");
        } catch (DataSyncException e) {
            handler.deleteObject(updateObject.dbId);
            sendMessage(ActivityInvoiceDetails.class, BroadcastMessageActionEnum.ERROR, "image_internal");
            throw e;
        } catch (Exception e) {
            sendMessage(ActivityInvoiceDetails.class, BroadcastMessageActionEnum.ERROR, "image");
            throw e;
        }

    }

    private void uploadSpeechRecognition(StatusDatabaseHandler.UpdateObject updateObject) throws LoginDataException, OfflineException, DataSyncException, LoginException, SilentOfflineException {

        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());

        try {
            RequestManager rm = new RequestManager(this.getApplicationContext());

            if (updateObject.objectId == null){
                handler.deleteObject(updateObject.dbId);
                throw new DataSyncException();
            }

            UUID generatedId = UUID.randomUUID();

            TextRecognitionDTO textRecognitionDTO = new TextRecognitionDTO();
            textRecognitionDTO.setTextToRecognize(updateObject.objectId);

            Invoice invoice = rm.uploadSpeechRecognition(generatedId, textRecognitionDTO);

            mainDatabaseHandler.insertUpdateInvoice(invoice);

            handler.deleteObject(updateObject.dbId);
            sendMessage(ActivityMain.class, BroadcastMessageActionEnum.REFRESH, "");

            AppUser appUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());
            if (appUser != null){
                appUser.setUpdateModusType(UpdateModusType.PATCH_UPDATE);
                mainDatabaseHandler.updateAppUser(appUser);
            }
        } catch (Exception e) {
            sendMessage(ActivityInvoiceDetails.class, BroadcastMessageActionEnum.ERROR, "speech");
            throw e;
        }

    }

    private void updateFirebaseToken(String firebaseTokenJson) throws LoginDataException, OfflineException, DataSyncException {

        RequestManager rm = new RequestManager(this.getApplicationContext());

        Type listType = new TypeToken<MobileDevice>() {}.getType();
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        MobileDevice mobileDevice = gson.fromJson(firebaseTokenJson, listType);

        rm.updateFirebaseToken(mobileDevice);
    }

    private void deleteFirebaseToken(String deviceId) throws OfflineException, DataSyncException, LoginDataException {
        RequestManager rm = new RequestManager();
        rm.deleteFirebaseToken(deviceId);
    }

    private void loginUser(String username, String password) throws OfflineException, DataSyncException, LoginDataException {

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());

        try {
            RequestManager rm = new RequestManager();
            AccessTokenResponse accessTokenResponse = rm.loginUser(username, password);

            LoginUserHelper.logoutAllUsers(getApplicationContext());

            boolean userInDb = false;
            List<AppUser> appUsers = mainDatabaseHandler.findAppUsers(MainDatabaseHandler.VAR_EMAIL, username);
            AppUser appUser;
            if (appUsers.isEmpty()) {
                appUser = new AppUser();
            } else {
                appUser = appUsers.get(0);
                userInDb = true;
            }

            Date now = new Date();
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(now);
            calendar.add(Calendar.SECOND, Integer.valueOf(accessTokenResponse.getExpires_in()));
            calendar.add(Calendar.HOUR, -96);
//            calendar.add(Calendar.DAY_OF_YEAR, -100);
            Date futureTime = calendar.getTime();

            appUser.setAppUserAccessExpiration(futureTime);
            appUser.setAppUserRefreshToken(accessTokenResponse.getRefresh_token());
            appUser.setAppUserId(UUID.fromString(accessTokenResponse.getUserId()));
            appUser.setEmail(username);
            if (!userInDb){
                appUser.setAppUserPassword(null);
            }
            appUser.setAppUserAccessKey(accessTokenResponse.getAccess_token());
            appUser.setLoggedIn(true);

            mainDatabaseHandler.updateAppUser(appUser);
            sendLoginMessage(LoginStatusEnum.LOGIN_SUCCESSFUL);
        } catch (Exception e) {
            throw e;
        }
    }

    private void deleteDatabaseData() {
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());

        mainDatabaseHandler.deleteAppUsers(null);
        mainDatabaseHandler.deleteUserContacts(null);
        mainDatabaseHandler.deleteInvoiceCategories(null);
        mainDatabaseHandler.deleteBusinessPartners(null);
        mainDatabaseHandler.deleteInvoices(null);
        mainDatabaseHandler.deleteInvoiceFailures(null);
        mainDatabaseHandler.deleteCostDistributionItems(null);
        mainDatabaseHandler.deleteCostDistributions(null);
        mainDatabaseHandler.deleteBasicDatas(null);
    }

    private void patchUpdate(boolean updatePendingObjects) throws LoginDataException, OfflineException, LoginException, SilentOfflineException, DataSyncException {
        if (updatePendingObjects){
            updatePendingObjects();
        }

        Log.e(TAG, TAG + "::patchUpdate -> START ");
        RequestManager rm = new RequestManager(getApplicationContext());

        StatusDatabaseHandler statusDatabaseHandler = StatusDatabaseHandler.getInstance(getApplicationContext());

        AppUser appUserLoggedIn = LoginUserHelper.currentLoggedInUser(getApplicationContext());
        if (appUserLoggedIn == null){
            throw new LoginException();
        }

        UpdateModusType updateModusType = appUserLoggedIn.getUpdateModusType();

        int status = statusDatabaseHandler.getStatus();
        if (status == StatusDatabaseHandler.STATUS_MANUAL_OFFLINE) {
            throw new SilentOfflineException();
        }

        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(this.getApplicationContext());

        boolean domainOnline = rm.serverOnline(getApplicationContext());
        if (!domainOnline){
            throw new OfflineException();
        }

        if (updatePendingObjects){
            for (StatusDatabaseHandler.UpdateObject tmpUpdateObject : statusDatabaseHandler.getUpdateObjects()) {
                statusDatabaseHandler.deleteObject(tmpUpdateObject.dbId);
            }
        }

        try{
            String accessKey = appUserLoggedIn.getAppUserAccessKey();
            String refreshToken = appUserLoggedIn.getAppUserRefreshToken();
            Date expirationDate = appUserLoggedIn.getAppUserAccessExpiration();
            Date lastUpdate = appUserLoggedIn.getLastUpdate();

            AppUser tmpAppUser = rm.getAppUser(appUserLoggedIn);
            tmpAppUser.setAppUserRefreshToken(refreshToken);
            tmpAppUser.setAppUserAccessKey(accessKey);
            tmpAppUser.setAppUserAccessExpiration(expirationDate);
            tmpAppUser.setLastUpdate(lastUpdate);
            tmpAppUser.setAppUserPassword(appUserLoggedIn.getAppUserPassword());
            tmpAppUser.setLoggedIn(true);

            final RequestManager.ProgressListener progressListener = new RequestManager.ProgressListener() {
                boolean firstUpdate = true;

                @Override public void update(long bytesRead, long contentLength, boolean done) {
                    if (done) {
                        sendMessage(DialogUpdateRunning.class,BroadcastMessageActionEnum.PROGRESS,"Download abgeschlossen");
                    } else {
                        if (firstUpdate) {
                            firstUpdate = false;
                            if (contentLength != -1) {
                                System.out.format("content-length: %d\n", contentLength);
                            }
                        }

                        BigDecimal bytesReadBd = new BigDecimal(bytesRead);
                        BigDecimal kilobytes = bytesReadBd.divide(new BigDecimal(1000),RoundingMode.HALF_EVEN).setScale(0,RoundingMode.HALF_EVEN);
                        sendMessage(DialogUpdateRunning.class,BroadcastMessageActionEnum.PROGRESS,"Heruntergeladen: "+kilobytes + " KB");
                    }
                }
            };

            sendMessage(DialogUpdateRunning.class,BroadcastMessageActionEnum.PROGRESS,"Lade Benutzerdaten herunter...");

            AllDataPatch allDataPatch = rm.allDataPatchList(lastUpdate, progressListener);

            Date lastModifiedDate = new Date();
            if (allDataPatch.getLastModifiedDateFromServer() != null){
                lastModifiedDate = allDataPatch.getLastModifiedDateFromServer();
            }
            tmpAppUser.setLastUpdate(lastModifiedDate);
            mainDatabaseHandler.updateAppUser(tmpAppUser);

            SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
            sqlBuilder.isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.MESSAGE.name())
                    .or()
                    .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.BUDGET_MESSAGE.name())
                    .or()
                    .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.CHANGED_MESSAGE.name())
                    .or()
                    .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.MISTAKE_MESSAGE.name());

            Set<String> messageIds = new HashSet<>();
            for (BasicData basicData : mainDatabaseHandler.findBasicDatasSql(sqlBuilder)) {
                GsonBuilder builder = new GsonBuilder();
                Gson gson = builder.create();
                MessageDTO messageDTO = gson.fromJson(basicData.getValue(), MessageDTO.class);

                messageIds.add(basicData.getObject1Id() + "_" + messageDTO.getMessage());
            }

            if (UpdateModusType.FULL_UPDATE.equals(updateModusType)){
                deleteDatabaseData();
            }

            Context context = this.getApplicationContext();

            for (DatabaseChangeDTO<AppUser> appUserDatabaseChangeDTO : allDataPatch.getAppUserDTOs()) {
                if (appUserLoggedIn.getAppUserId().equals(appUserDatabaseChangeDTO.getMappedObject().getAppUserId())){
                    appUserDatabaseChangeDTO.setMappedObject(tmpAppUser);
                }

                mainDatabaseHandler.updatePerDatabaseChangeDTO(appUserDatabaseChangeDTO, AppUser.class, context);
            }

            AppUser appUserLoggedInTmp = LoginUserHelper.currentLoggedInUser(getApplicationContext());
            if (appUserLoggedInTmp == null){
                throw new LoginDataException(LoginStatusEnum.NO_LOGIN_DATA);
            }

            boolean canUseAndroid = RightHelper.hasUserRight(getApplicationContext(), Right.CAN_USE_ANDROID);
            boolean isEnabled = RightHelper.hasUserRight(getApplicationContext(), Right.ENABLED);
            if (!canUseAndroid || !isEnabled) {
                LoginUserHelper.logoutAllUsers(getApplicationContext());
                sendMessage(DialogUpdateRunning.class,BroadcastMessageActionEnum.ERROR,"Sie haben keine Berechtigung zur Nutzung der Android-App.\nBitte wenden Sie sich an Ihren Administrator.");
                return;
            }

            int counter = 1;

            sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_BASIC_DATA);
            sqlBuilder
                    .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.MESSAGE.name())
                    .or()
                    .isEqual(MainDatabaseHandler.VAR_BASIC_DATA_TYPE, BasicDataType.BUDGET_MESSAGE.name());
            List<BasicData> basicDatas = mainDatabaseHandler.findBasicDatasSql(sqlBuilder);

            mainDatabaseHandler.deleteBasicDatas(basicDatas);
            for (DatabaseChangeDTO<BasicData> appUserDatabaseChangeDTO : allDataPatch.getBasicDataDTOs()) {
                sendMessage(DialogUpdateRunning.class,BroadcastMessageActionEnum.PROGRESS,"Speichere Basisdaten...");
                mainDatabaseHandler.updatePerDatabaseChangeDTO(appUserDatabaseChangeDTO, BasicData.class, context);
            }
            for (DatabaseChangeDTO<InvoiceCategory> appUserDatabaseChangeDTO : allDataPatch.getInvoiceCategorieDTOs()) {
                sendMessage(DialogUpdateRunning.class,BroadcastMessageActionEnum.PROGRESS,"Speichere Kategorien...");
                mainDatabaseHandler.updatePerDatabaseChangeDTO(appUserDatabaseChangeDTO, InvoiceCategory.class, context);
            }
            for (DatabaseChangeDTO<InvoiceFailure> appUserDatabaseChangeDTO : allDataPatch.getInvoiceFailureDTOs()) {
                sendMessage(DialogUpdateRunning.class,BroadcastMessageActionEnum.PROGRESS,"Speichere Rechnungsfehler...");
                mainDatabaseHandler.updatePerDatabaseChangeDTO(appUserDatabaseChangeDTO, InvoiceFailure.class, context);
            }
            for (DatabaseChangeDTO<CostDistribution> appUserDatabaseChangeDTO : allDataPatch.getCostDistributionDTOs()) {
                sendMessage(DialogUpdateRunning.class,BroadcastMessageActionEnum.PROGRESS,"Speichere Verteilungen...");
                mainDatabaseHandler.updatePerDatabaseChangeDTO(appUserDatabaseChangeDTO, CostDistribution.class, context);
            }

            for (DatabaseChangeDTO<UserContact> appUserDatabaseChangeDTO : allDataPatch.getUserContactDTOs()) {
                sendMessage(DialogUpdateRunning.class,BroadcastMessageActionEnum.PROGRESS,"Speichere Kontakte...");
                mainDatabaseHandler.updatePerDatabaseChangeDTO(appUserDatabaseChangeDTO, UserContact.class, context);
            }

            if (lastUpdate == null){
                sendMessage(DialogUpdateRunning.class,BroadcastMessageActionEnum.PROGRESS,"Speichere Unternehmen...");
                mainDatabaseHandler.updatePerDatabaseChangesDTO(allDataPatch.getBusinessPartnerDTOs(), BusinessPartner.class, context);
                sendMessage(DialogUpdateRunning.class,BroadcastMessageActionEnum.PROGRESS,"Speichere Rechnungen...");
                mainDatabaseHandler.updatePerDatabaseChangesDTO(allDataPatch.getInvoiceDTOs(), Invoice.class, context);
                sendMessage(DialogUpdateRunning.class,BroadcastMessageActionEnum.PROGRESS,"Speichere Kostenverteilungen...");
                mainDatabaseHandler.updatePerDatabaseChangesDTO(allDataPatch.getCostDistributionItemDTOs(), CostDistributionItem.class, context);
            }else{
                counter = 1;
                for (DatabaseChangeDTO<BusinessPartner> appUserDatabaseChangeDTO : allDataPatch.getBusinessPartnerDTOs()) {
                    sendMessage(DialogUpdateRunning.class,BroadcastMessageActionEnum.PROGRESS,"Speichere Unternehmen... " + counter + " / " + allDataPatch.getBusinessPartnerDTOs().size());
                    mainDatabaseHandler.updatePerDatabaseChangeDTO(appUserDatabaseChangeDTO, BusinessPartner.class, context);
                }
                counter = 1;
                for (DatabaseChangeDTO<CostDistributionItem> appUserDatabaseChangeDTO : allDataPatch.getCostDistributionItemDTOs()) {
                    sendMessage(DialogUpdateRunning.class,BroadcastMessageActionEnum.PROGRESS,"Speichere Kostenverteilungen... " + counter + " / " + allDataPatch.getCostDistributionItemDTOs().size());
                    mainDatabaseHandler.updatePerDatabaseChangeDTO(appUserDatabaseChangeDTO, CostDistributionItem.class, context);
                    counter++;
                }
                counter = 1;
                for (DatabaseChangeDTO<Invoice> appUserDatabaseChangeDTO : allDataPatch.getInvoiceDTOs()) {
                    sendMessage(DialogUpdateRunning.class,BroadcastMessageActionEnum.PROGRESS,"Speichere Rechnungen... " + counter + " / " + allDataPatch.getInvoiceDTOs().size());
                    mainDatabaseHandler.updatePerDatabaseChangeDTO(appUserDatabaseChangeDTO, Invoice.class, context);
                    counter++;
                }
            }

            appUserLoggedInTmp = LoginUserHelper.currentLoggedInUser(getApplicationContext());
            if (appUserLoggedInTmp == null){
                throw new LoginDataException(LoginStatusEnum.NO_LOGIN_DATA);
            }

            calculateRestSum();
            isNotificationNecessary(messageIds);
        }catch (OfflineException e){
            e.printStackTrace();
            throw new OfflineException();
        }catch (Exception e){
            e.printStackTrace();
            deleteDatabaseData();
            throw new DataSyncException();
        }
    }

    private void sendNotification(String subject, String messageBody) {
        Intent intent = new Intent(this, ActivityMain.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.money)
                .setContentTitle(subject)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

}
