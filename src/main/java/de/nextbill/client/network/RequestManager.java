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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.nextbill.client.BuildConfig;
import de.nextbill.client.activities.ActivityMain;
import de.nextbill.client.activities.BaseActivity;
import de.nextbill.client.database.IOHelper;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.enums.DataSyncStatusEnum;
import de.nextbill.client.enums.LoginStatusEnum;
import de.nextbill.client.exceptions.AccessExpirationException;
import de.nextbill.client.exceptions.DataSyncException;
import de.nextbill.client.exceptions.LoginDataException;
import de.nextbill.client.exceptions.NoUserAndRefreshTokenFoundException;
import de.nextbill.client.exceptions.OfflineException;
import de.nextbill.client.helper.LoginUserHelper;
import de.nextbill.client.model.AccessTokenResponse;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.BasicData;
import de.nextbill.client.model.BusinessPartner;
import de.nextbill.client.model.CostDistribution;
import de.nextbill.client.model.CostDistributionItem;
import de.nextbill.client.model.Invoice;
import de.nextbill.client.model.InvoiceCategory;
import de.nextbill.client.model.MobileDevice;
import de.nextbill.client.model.StandingOrder;
import de.nextbill.client.model.UserContact;
import de.nextbill.client.model.VersionDTO;
import de.nextbill.client.pojos.AllDataPatch;
import de.nextbill.client.pojos.BillingPayment;
import de.nextbill.client.pojos.BudgetDTO;
import de.nextbill.client.pojos.ListOfJson;
import de.nextbill.client.pojos.TextRecognitionDTO;
import de.nextbill.client.utils.ArticleAnalysisData;
import de.nextbill.client.utils.BillingConfig;
import de.nextbill.client.utils.BillingListItem;
import de.nextbill.client.utils.MistakeMessage;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpDate;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

public class RequestManager {
    private static final String TAG = "RequestManager";

    public static final String BC_REQUEST_DONE = "de.nextbill.client.REQUEST_DONE";
    public static final String BC_LOGIN = "de.nextbill.client.LOGIN";
    public static final String BC_ONLINE = "de.nextbill.client.ONLINE";
    public static final String BC_SEND_MESSAGE = "de.nextbill.client.SEND_MESSAGE";

    private static String API_DOMAIN = "";

    private static String API_URL = "https://" + API_DOMAIN + "/api/";
    private static String API_AUTH_URL = "https://" + API_DOMAIN + "/";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    final MediaType MEDIA_TYPE_IMAGE = MediaType.parse("image/*");

    private String accessToken;

    public RequestManager() {

    }

    public RequestManager(Context context) throws LoginDataException, OfflineException, DataSyncException {

        try {

            LoginUserHelper.hasCurrentUserValidOAuthData(context);

        } catch (NoUserAndRefreshTokenFoundException e) {

            throw new LoginDataException(LoginStatusEnum.NO_LOGIN_DATA);

        } catch (AccessExpirationException e) {

            if (!serverOnline(context)){
                throw new OfflineException();
            }

            AppUser appUser = e.getAppUser();

            AccessTokenResponse accessTokenResponse = refreshAccessToken(appUser.getAppUserRefreshToken());

            appUser.setAppUserRefreshToken(accessTokenResponse.getRefresh_token());
            appUser.setAppUserAccessKey(accessTokenResponse.getAccess_token());

            Date now = new Date();
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(now);
            calendar.add(Calendar.SECOND, Integer.valueOf(accessTokenResponse.getExpires_in()));
            calendar.add(Calendar.HOUR, -96);
//                calendar.add(Calendar.DAY_OF_YEAR, -100);
            Date futureTime = calendar.getTime();

            appUser.setAppUserAccessExpiration(futureTime);

            MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);
            mainDatabaseHandler.updateAppUser(appUser);
        }

        AppUser appUser = LoginUserHelper.currentLoggedInUser(context);
        if (appUser == null){
            throw new DataSyncException();
        }
        accessToken = appUser.getAppUserAccessKey();

    }

    public static void setApiUrl(String apiIp) {
        API_URL = apiIp + "/api/";
        API_DOMAIN = apiIp;
        API_AUTH_URL = apiIp + "/";
    }

    public boolean serverOnline(Context context) throws DataSyncException {
        String requestString = API_AUTH_URL + "online";
        Log.i(TAG, TAG + "::serverOnline -> url=" + requestString);

        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .writeTimeout(3, TimeUnit.SECONDS)
                    .readTimeout(3, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(requestString + "?currentAndroidVersion=" + BuildConfig.VERSION_NAME)
                    .build();

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()){
                return false;
            }

            String responseString = response.body().string();

            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            VersionDTO versionDTO = gson.fromJson(responseString, VersionDTO.class);

            SharedPreferences settings = context.getSharedPreferences(ActivityMain.PREF_NAME, 0);
            SharedPreferences.Editor sEditor = settings.edit();
            sEditor.putBoolean(BaseActivity.MAIL_SENT_ACTIVE, versionDTO.getMailSentActive() != null ? versionDTO.getMailSentActive() : false );
            sEditor.commit();

            if (versionDTO.getBreakingChangeForAndroid() != null && versionDTO.getBreakingChangeForAndroid()) {
                throw new DataSyncException(DataSyncStatusEnum.VERSION_CONFLICT);
            }

        } catch (MalformedURLException e) {
            throw new DataSyncException();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    private byte[] sendSimpleGetRequestForBytes(String urlAppend) throws OfflineException, DataSyncException, LoginDataException {
        Log.i(TAG, TAG + "::sendSimplePostRequest -> urlAppend=" + urlAppend);

        try {
            OkHttpClient client = new OkHttpClient();
            URL url = new URL(API_URL + urlAppend);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            Response response = client.newCall(request).execute();

            if (response.code() == 401) {
                throw new LoginDataException(LoginStatusEnum.ACCESS_TOKEN_INVALID);
            }else if (!response.isSuccessful() && response.code() != 404){
                throw new OfflineException();
            }

            return response.body().bytes();
        } catch (MalformedURLException e) {
            throw new DataSyncException();
        } catch (IOException e) {
            throw new OfflineException();
        }
    }

    private String sendSimplePostRequest(String urlAppend, Object object) throws OfflineException, DataSyncException, LoginDataException {
        Log.i(TAG, TAG + "::sendSimplePostRequest -> urlAppend=" + urlAppend);

        try{

            Gson gson = new GsonBuilder().setDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz").create();
            String jsonString = gson.toJson(object);

            OkHttpClient client = new OkHttpClient();
            URL url = new URL(API_URL + urlAppend);
            Request.Builder request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken);

            if (object != null){
                RequestBody body = RequestBody.create(JSON, jsonString);
                request.post(body);
            }else{
                RequestBody reqbody = RequestBody.create(null, new byte[0]);
                request.method("POST",reqbody).header("Content-Length", "0");
            }

            Response response = client.newCall(request.build()).execute();

            if (response.code() == 401) {
                throw new LoginDataException(LoginStatusEnum.ACCESS_TOKEN_INVALID);
            }else if (!response.isSuccessful() && response.code() != 404){
                throw new OfflineException();
            }

            return response.body().string();
        } catch (MalformedURLException e) {
            throw new DataSyncException();
        } catch (IOException e) {
            throw new OfflineException();
        }
    }

    private String sendSimplePutRequest(String urlAppend, Object object) throws OfflineException, DataSyncException, LoginDataException {
        Log.i(TAG, TAG + "::sendSimplePutRequest -> urlAppend=" + urlAppend);
        try{

            Gson gson = new GsonBuilder().setDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz").create();
            String jsonString = gson.toJson(object);

            OkHttpClient client = new OkHttpClient();
            URL url = new URL(API_URL + urlAppend);
            RequestBody body = RequestBody.create(JSON, jsonString);
            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            Response response = client.newCall(request).execute();

            if (response.code() == 401) {
                throw new LoginDataException(LoginStatusEnum.ACCESS_TOKEN_INVALID);
            }else if (!response.isSuccessful() && response.code() != 404){
                throw new OfflineException();
            }

            return response.body().string();
        } catch (MalformedURLException e) {
            throw new DataSyncException();
        } catch (IOException e) {
            throw new OfflineException();
        }
    }

    private String sendSimpleDeleteRequest(String urlAppend) throws OfflineException, DataSyncException, LoginDataException {
        return sendSimpleDeleteRequest(urlAppend, true);
    }

    private String sendSimpleDeleteRequest(String urlAppend, boolean useAccessToken) throws OfflineException, DataSyncException, LoginDataException {
        Log.i(TAG, TAG + "::sendSimpleDeleteRequest -> urlAppend=" + urlAppend);

        try{
            OkHttpClient client = new OkHttpClient();
            URL url = new URL(API_URL + urlAppend);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(url);

            if (useAccessToken){
                requestBuilder = requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
            }
            requestBuilder = requestBuilder.delete();

            Response response = client.newCall(requestBuilder.build()).execute();

            if (response.code() == 401) {
                throw new LoginDataException(LoginStatusEnum.ACCESS_TOKEN_INVALID);
            }else if (!response.isSuccessful() && response.code() != 404){
                throw new OfflineException();
            }

            return response.body().string();
        } catch (MalformedURLException e) {
            throw new DataSyncException();
        } catch (IOException e) {
            throw new OfflineException();
        }
    }

    private <T> List<T> sendSimpleGetRequestForList(String urlAppend, Class<T> clazz) throws OfflineException, DataSyncException, LoginDataException {

        String returnBody = sendSimpleGetRequest(urlAppend, null);

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                return new Date(json.getAsJsonPrimitive().getAsLong());
            }
        });

        Gson gson = builder.create();

        return gson.fromJson(returnBody, new ListOfJson<T>(clazz));
    }

    private <T> T sendSimpleGetRequestForObject(String urlAppend, Class<T> clazz) throws OfflineException, DataSyncException, LoginDataException {

        String jsonBody = sendSimpleGetRequest(urlAppend, null);

        GsonBuilder builder = new GsonBuilder();

        // Register an adapter to manage the date types as long values
        builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                return new Date(json.getAsJsonPrimitive().getAsLong());
            }
        });

        Gson gson = builder.create();

        return gson.fromJson(jsonBody, clazz);
    }

    private String sendSimpleGetRequest(String urlAppend, final ProgressListener progressListener) throws OfflineException, DataSyncException, LoginDataException {
        Log.i(TAG, TAG + "::sendSimpleGetRequest -> urlAppend=" + urlAppend);

        try{
            OkHttpClient client = null;
            if (progressListener != null) {
                client = new OkHttpClient.Builder()
                        .addNetworkInterceptor(new Interceptor() {
                            @Override public Response intercept(Interceptor.Chain chain) throws IOException {
                                Response originalResponse = chain.proceed(chain.request());
                                return originalResponse.newBuilder()
                                        .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                                        .build();
                            }
                        })
                        .build();
            }else {
                client = new OkHttpClient();
            }

            URL url = new URL(urlAppend);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            Response response = client.newCall(request).execute();

            if (response.code() == 401) {
                throw new LoginDataException(LoginStatusEnum.ACCESS_TOKEN_INVALID);
            }else if (!response.isSuccessful() && response.code() != 404){
                throw new OfflineException();
            }

            return response.body().string();
        } catch (MalformedURLException e) {
            throw new DataSyncException();
        } catch (IOException e) {
            throw new OfflineException();
        }
    }

    private <T> T sendSimplePatchRequest(String urlAppend, Date afterDate, final ProgressListener progressListener, Class<T> clazz) throws OfflineException, DataSyncException, LoginDataException {
        Log.i(TAG, TAG + "::sendSimplePatchRequest -> urlAppend=" + urlAppend);

        try{
            OkHttpClient client = null;
            if (progressListener != null) {
                client = new OkHttpClient.Builder()
                        .addNetworkInterceptor(new Interceptor() {
                            @Override public Response intercept(Interceptor.Chain chain) throws IOException {
                                Response originalResponse = chain.proceed(chain.request());
                                return originalResponse.newBuilder()
                                        .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                                        .build();
                            }
                        })
                        .connectTimeout(2, TimeUnit.MINUTES)
                        .readTimeout(2, TimeUnit.MINUTES)
                        .build();
            }else {
                client = new OkHttpClient.Builder()
                        .connectTimeout(2, TimeUnit.MINUTES)
                        .readTimeout(2, TimeUnit.MINUTES)
                        .build();
            }

            RequestBody body = RequestBody.create(null, new byte[]{});

            URL url = new URL(urlAppend);
            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .patch(body);

            if (afterDate != null){
                builder.addHeader("If-Modified-Since", HttpDate.format(afterDate));
            }

            Request request = builder.build();

            Response response = client.newCall(request).execute();

            Log.i(TAG, TAG + "::sendSimplePatchRequest, end");
            if (response.code() == 401) {
                throw new LoginDataException(LoginStatusEnum.ACCESS_TOKEN_INVALID);
            }else if (!response.isSuccessful() && response.code() != 404){
                throw new OfflineException();
            }

            String bodyString = response.body().string();

            GsonBuilder gsonBuilder = new GsonBuilder();

            gsonBuilder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
                public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    return new Date(json.getAsJsonPrimitive().getAsLong());
                }
            });

            Gson gson = gsonBuilder.create();

            return gson.fromJson(bodyString, clazz);
        } catch (MalformedURLException e) {
            throw new DataSyncException();
        } catch (IOException e) {
            throw new OfflineException();
        }
    }

    private String sendAccessTokenPostRequest(String username, String password) throws OfflineException, DataSyncException, LoginDataException {

        try {

            Log.e(TAG, "Try to get access-token...");

            OkHttpClient client = new OkHttpClient();

            RequestBody formBody = new FormBody.Builder()
                    .add("grant_type", "password")
                    .add("client_id", "nextbillMobileClient")
                    .add("client_secret", "defaultNextBillSecret")
                    .add("scope", "openid")
                    .add("username", username)
                    .add("password", password)
                    .build();

            Request request = new Request.Builder()
                    .url(API_AUTH_URL + "oauth/token")
                    .addHeader("Authorization", "Basic bmV4dGJpbGxNb2JpbGVDbGllbnQ6ZGVmYXVsdE5leHRCaWxsU2VjcmV0")
                    .post(formBody)
                    .build();

            Response response = client.newCall(request).execute();

            if (response.code() == 401 || response.code() == 400) {
                throw new LoginDataException(LoginStatusEnum.BAD_CREDENTIALS);
            }else if (response.code() != 200) {
                throw new LoginDataException(LoginStatusEnum.ACCESS_TOKEN_INVALID);
            }

            return response.body().string();
        } catch (MalformedURLException | ProtocolException ex) {
            throw new DataSyncException();
        } catch (IOException e) {
            throw new OfflineException();
        }
    }

    private String sendRefreshTokenPostRequest(String refreshToken) throws OfflineException, DataSyncException, LoginDataException {
        try {

            Log.e(TAG, "Try to get access-token via refresh-token...");

            OkHttpClient client = new OkHttpClient();

            RequestBody formBody = new FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("client_id", "nextbillMobileClient")
                    .add("client_secret", "defaultNextBillSecret")
                    .add("refresh_token", refreshToken)
                    .build();

            Request request = new Request.Builder()
                    .url(API_AUTH_URL + "oauth/token")
                    .addHeader("Authorization", "Basic bmV4dGJpbGxNb2JpbGVDbGllbnQ6ZGVmYXVsdE5leHRCaWxsU2VjcmV0")
                    .post(formBody)
                    .build();

            Response response = client.newCall(request).execute();

            if (response.code() != 200) {
                throw new LoginDataException(LoginStatusEnum.ACCESS_TOKEN_INVALID);
            }

            return response.body().string();
        } catch (MalformedURLException | ProtocolException ex) {
            throw new DataSyncException();
        } catch (IOException e) {
            throw new LoginDataException(LoginStatusEnum.ACCESS_TOKEN_INVALID);
        }
    }

    public List<BillingListItem> billingListItems() throws OfflineException, DataSyncException, LoginDataException {
        String reqString = API_URL + "billings/billingListItems";
        return sendSimpleGetRequestForList(reqString, BillingListItem.class);
    }

    public List<BudgetDTO> budgets() throws OfflineException, DataSyncException, LoginDataException {
        String reqString = API_URL + "budgets";
        return sendSimpleGetRequestForList(reqString, BudgetDTO.class);
    }

    public List<BasicData> basicDataList() throws OfflineException, DataSyncException, LoginDataException {
        String reqString = API_URL + "basicdata";
        return sendSimpleGetRequestForList(reqString, BasicData.class);
    }

    public AllDataPatch allDataPatchList(Date afterDate, ProgressListener progressListener) throws OfflineException, DataSyncException, LoginDataException, LoginDataException {
        String reqString = API_URL + "mobileDevices/data";
        return sendSimplePatchRequest(reqString, afterDate, progressListener, AllDataPatch.class);
    }

    protected void updateInvoice(Invoice invoice) throws OfflineException, DataSyncException, LoginDataException {
        sendSimplePutRequest("invoices/" + invoice.getInvoiceId().toString(), invoice);
    }

    protected void updateCostDistribution(CostDistribution costDistribution) throws OfflineException, DataSyncException, LoginDataException {
        sendSimplePutRequest("costdistributions/" + costDistribution.getCostDistributionId().toString(), costDistribution);
    }

    protected void updateStandingOrder(StandingOrder standingOrder) throws OfflineException, DataSyncException, LoginDataException {
        sendSimplePutRequest("standingOrders/invoiceTemplates/" + standingOrder.getInvoiceTemplateId().toString(),  standingOrder);
    }

    protected void updateUserContact(UserContact userContact) throws OfflineException, DataSyncException, LoginDataException {
        sendSimplePutRequest("usercontacts/" + userContact.getUserContactId().toString(), userContact);
    }

    protected void updateBudget(BudgetDTO budgetDTO) throws OfflineException, DataSyncException, LoginDataException{
        sendSimplePutRequest("budgets/" + budgetDTO.getBudgetId().toString(), budgetDTO);
    }

    protected void updateCostDistributionItem(CostDistributionItem costDistributionItem) throws OfflineException, DataSyncException, LoginDataException{
        sendSimplePutRequest("costdistributionitems/" + costDistributionItem.getCostDistributionItemId().toString(), costDistributionItem);
    }

    protected void updatePaymentStatus(BillingPayment billingPayment) throws OfflineException, DataSyncException, LoginDataException{
        sendSimplePostRequest("billings/" + billingPayment.getBillingId().toString() + "/payment", billingPayment);
    }

    protected void insertCostDistributionItem(CostDistributionItem costDistributionItem) throws OfflineException, DataSyncException, LoginDataException{
        sendSimplePostRequest("costdistributionitems", costDistributionItem);
    }

    protected void insertUserContact(UserContact userContact) throws OfflineException, DataSyncException, LoginDataException{
        sendSimplePostRequest("usercontacts", userContact);
    }

    protected void insertBudget(BudgetDTO budget) throws OfflineException, DataSyncException, LoginDataException{
        sendSimplePostRequest("budgets", budget);
    }

    protected void insertCostDistribution(CostDistribution costDistribution) throws OfflineException, DataSyncException, LoginDataException{
        sendSimplePostRequest("costdistributions", costDistribution);
    }

    protected void insertInvoice(Invoice invoice) throws OfflineException, DataSyncException, LoginDataException{
        sendSimplePostRequest("invoices", invoice);
    }

    protected Invoice getInvoice(UUID invoiceId) throws OfflineException, DataSyncException, LoginDataException{
        return sendSimpleGetRequestForObject(API_URL + "invoices/" + invoiceId.toString(), Invoice.class);
    }

    protected AppUser getAppUserByEmail(String email) throws OfflineException, DataSyncException, LoginDataException{
        String returnString = sendSimplePostRequest("appusers/findByEmail",  email);
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.fromJson(returnString, AppUser.class);
    }

    protected void insertStandingOrder(StandingOrder standingOrder) throws OfflineException, DataSyncException, LoginDataException{
        sendSimplePostRequest("standingOrders",  standingOrder);
    }

    protected void deleteStandingOrderByCreatedInvoice(UUID invoiceId) throws OfflineException, DataSyncException, LoginDataException{
        sendSimpleDeleteRequest("standingOrders/deleteByCreatedInvoice/" + invoiceId.toString());
    }

    protected void deleteStandingOrderByInvoiceTemplate(UUID invoiceTemplateId) throws OfflineException, DataSyncException, LoginDataException{
        sendSimpleDeleteRequest("standingOrders/deleteByInvoiceTemplate/" + invoiceTemplateId.toString());
    }

    protected void generateBilling(BillingConfig billingConfig) throws OfflineException, DataSyncException, LoginDataException{
        sendSimplePostRequest("billings/payment",  billingConfig);
    }

    protected void generateMistakeMessage(MistakeMessage mistakeMessage) throws OfflineException, DataSyncException, LoginDataException{
        sendSimplePostRequest("invoices/" + mistakeMessage.getInvoiceId().toString() + "/mistake", mistakeMessage);
    }

    protected AppUser getAppUser(AppUser appUser) throws OfflineException, DataSyncException, LoginDataException{
        return sendSimpleGetRequestForObject(API_URL + "appusers/" + appUser.getAppUserId().toString(), AppUser.class);
    }

    protected void insertInvoiceCategory(InvoiceCategory invoiceCategory) throws OfflineException, DataSyncException, LoginDataException {
        sendSimplePostRequest("invoicecategories", invoiceCategory);
    }

    protected String analyzeArticle(ArticleAnalysisData articleAnalysisData) throws OfflineException, DataSyncException, LoginDataException {
        return sendSimplePostRequest("/service/upload/"+ articleAnalysisData.getInvoiceId()+"/articleAnalysis?x="+ articleAnalysisData.getX()+"&y="+ articleAnalysisData.getY(), null);
    }

    protected Invoice uploadSpeechRecognition(UUID generatedId, TextRecognitionDTO textRecognitionDTO) throws OfflineException, DataSyncException, LoginDataException{
        String resposne = sendSimplePostRequest("service/upload/mic/"+generatedId.toString(), textRecognitionDTO);
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                return new Date(json.getAsJsonPrimitive().getAsLong());
            }
        });
        Gson gson = builder.create();
        return gson.fromJson(resposne, Invoice.class);
    }

    protected void insertBusinessPartner(BusinessPartner businessPartner) throws OfflineException, DataSyncException, LoginDataException{
        sendSimplePostRequest("businesspartners", businessPartner);
    }

    protected void deleteInvoiceCategory(UUID invoiceCategoryId) throws OfflineException, DataSyncException, LoginDataException{
        sendSimpleDeleteRequest("invoicecategories/" + invoiceCategoryId.toString());
    }

    protected void deleteBudget(UUID budgetId) throws OfflineException, DataSyncException, LoginDataException{
        sendSimpleDeleteRequest("budgets/" + budgetId.toString());
    }

    protected void deleteBasicData(UUID basicDataId) throws OfflineException, DataSyncException, LoginDataException{
        sendSimpleDeleteRequest("basicdata/" + basicDataId.toString());
    }

    protected void deleteInvoiceFailure(UUID invoiceFailureId) throws OfflineException, DataSyncException, LoginDataException{
        sendSimpleDeleteRequest("invoicefailures/" + invoiceFailureId.toString());
    }

    protected void deleteCostDistributionItem(UUID costDistributionItemId) throws OfflineException, DataSyncException, LoginDataException{
        sendSimpleDeleteRequest("costdistributionitems/" + costDistributionItemId.toString());
    }

    protected void deleteBusinessPartner(UUID businessPartnerId) throws OfflineException, DataSyncException, LoginDataException{
        sendSimpleDeleteRequest("businesspartners/" + businessPartnerId.toString());
    }

    protected void deleteMistakeMessage(UUID basicDataId) throws OfflineException, DataSyncException, LoginDataException{
        sendSimpleDeleteRequest("invoices/" + basicDataId.toString() + "/problemSolved");
    }

    protected void deleteBilling(UUID billingId) throws OfflineException, DataSyncException, LoginDataException{
        sendSimpleDeleteRequest("billings/" + billingId.toString());
    }

    protected void deleteUserContact(UUID userContactId) throws OfflineException, DataSyncException, LoginDataException{
        sendSimpleDeleteRequest("usercontacts/" + userContactId.toString());
    }

    protected void deleteCostDistribution(UUID costDistributionId) throws OfflineException, DataSyncException, LoginDataException{
        sendSimpleDeleteRequest("costdistributions/" + costDistributionId.toString());
    }

    protected void deleteInvoiceImage(UUID invoiceImageId) throws OfflineException, DataSyncException, LoginDataException{
        sendSimpleDeleteRequest("invoices/image/" + invoiceImageId.toString());
    }

    protected void deleteInvoice(UUID invoiceId) throws OfflineException, DataSyncException, LoginDataException{
        sendSimpleDeleteRequest("invoices/" + invoiceId.toString());
    }

    protected void updateFirebaseToken(MobileDevice mobileDevice) throws OfflineException, DataSyncException, LoginDataException{
        sendSimplePostRequest("mobileDevices", mobileDevice);
    }

    protected void deleteFirebaseToken(String mobileDeviceId) throws OfflineException, DataSyncException, LoginDataException {
        try{
            sendSimpleDeleteRequest("mobileDevices/delete/" + mobileDeviceId, false);
        }catch(Exception e){

        }

    }

    protected void downloadInvoiceImage(Invoice invoice, Context context) throws IOException, OfflineException, DataSyncException, LoginDataException {

        byte[] imageBytes = null;
        if (invoice.getInvoiceId() != null && invoice.getInvoiceImageId() != null){
            imageBytes = sendSimpleGetRequestForBytes("invoices/image/"+invoice.getInvoiceImageId().toString());

            if (imageBytes != null && imageBytes.length > 0){

                IOHelper ioHelper = IOHelper.getInstance();
                ioHelper.setCtx(context);

                String pathTmp = ioHelper.getImageDirectory() + "/" + invoice.getInvoiceId().toString() + "_thumbnail";
                File imageFile = new File(pathTmp);
                if (imageFile.exists()){
                    imageFile.delete();
                }

                FileOutputStream fos = new FileOutputStream(pathTmp);
                fos.write(imageBytes);
                fos.close();
            }

        }
    }

    public AccessTokenResponse loginUser(String username, String password) throws OfflineException, DataSyncException, LoginDataException {

        this.accessToken = null;

        String response = sendAccessTokenPostRequest(username, password);

        Type listType = new TypeToken<AccessTokenResponse>() {}.getType();

        GsonBuilder builder = new GsonBuilder();

        // Register an adapter to manage the date types as long values
        builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                return new Date(json.getAsJsonPrimitive().getAsLong());
            }
        });

        Gson gson = builder.create();

        return gson.fromJson(response, listType);
    }

    public AccessTokenResponse refreshAccessToken(String refreshToken)
            throws OfflineException, DataSyncException, LoginDataException {

        String response = sendRefreshTokenPostRequest(refreshToken);

        Type listType = new TypeToken<AccessTokenResponse>() {
        }.getType();

        GsonBuilder builder = new GsonBuilder();

        // Register an adapter to manage the date types as long values
        builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                return new Date(json.getAsJsonPrimitive().getAsLong());
            }
        });

        Gson gson = builder.create();

        return gson.fromJson(response, listType);
    }

    protected Invoice uploadInvoiceImage(Invoice invoice, File imageFile, boolean useOcr)  throws OfflineException, DataSyncException {
        String iFileName = "nextbill_temp.jpg";

        try {
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension("jpg");

            String urlString = "";
            if (useOcr){
                urlString = API_URL + "service/upload/"+invoice.getInvoiceId().toString();
            }else{
                urlString = API_URL + "invoices/image/"+invoice.getInvoiceId().toString();
            }

            Log.e(TAG, "Headers are written");

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();

            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("fileUpload", iFileName,
                            RequestBody.create(MediaType.parse(mime), imageFile))
                    .build();

            Request request = new Request.Builder()
                    .url(urlString)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .post(requestBody)
                    .build();

            Response response = client.newCall(request).execute();

            Log.e(TAG,"File Sent, Response: " + response.code());

            if (response.code() == 500) {
                throw new DataSyncException();
            }else if (response.code() != 200 && response.code() != 201 && response.code() != 202 && response.code() != 204) {
                throw new OfflineException();
            }

            String s = response.body().string();

            if (!s.equals("")){

                GsonBuilder builder = new GsonBuilder();

                // Register an adapter to manage the date types as long values
                builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
                    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        return new Date(json.getAsJsonPrimitive().getAsLong());
                    }
                });

                Gson gson = builder.create();

                return gson.fromJson(s, Invoice.class);
            }

            return null;
        } catch (MalformedURLException ex) {
            throw new DataSyncException();
        } catch (ProtocolException e) {
            throw new DataSyncException();
        } catch (IOException e) {
            e.printStackTrace();
            throw new OfflineException();
        }
    }

    public interface RequestListener {
        void onProgress(String message);
    }

    private static class ProgressResponseBody extends ResponseBody {

        private final ResponseBody responseBody;
        private final ProgressListener progressListener;
        private BufferedSource bufferedSource;

        ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener) {
            this.responseBody = responseBody;
            this.progressListener = progressListener;
        }

        @Override public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override public long contentLength() {
            return responseBody.contentLength();
        }

        @Override public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override public long read(Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    // read() returns the number of bytes read, or -1 if this source is exhausted.
                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                    progressListener.update(totalBytesRead, responseBody.contentLength(), bytesRead == -1);
                    return bytesRead;
                }
            };
        }
    }

    interface ProgressListener {
        void update(long bytesRead, long contentLength, boolean done);
    }
}