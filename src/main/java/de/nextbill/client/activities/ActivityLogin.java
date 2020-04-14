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

package de.nextbill.client.activities;


import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.nextbill.client.R;
import de.nextbill.client.breceiver.RequestListener;
import de.nextbill.client.breceiver.RequestServiceBroadcastReceiver;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.enums.BroadcastMessageActionEnum;
import de.nextbill.client.enums.LoginStatusEnum;
import de.nextbill.client.enums.OnlineStatusEnum;
import de.nextbill.client.enums.UpdateModusType;
import de.nextbill.client.helper.LoginUserHelper;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.network.RequestManager;
import de.nextbill.client.network.RequestUpdateService;
import de.nextbill.client.utils.SqlBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static de.nextbill.client.activities.BaseActivity.LAST_LOGGED_IN_USER;
import static de.nextbill.client.activities.BaseActivity.SERVER_URL;

public class ActivityLogin extends AppCompatActivity{

    private static final String TAG = "ActivityLogin";

    private RequestServiceBroadcastReceiver updateRequestReceiver;

    EditText editTextUserLogin;
    EditText editTextPasswordLogin;
    EditText editTextServer;
    ImageView btnSearchIp;

    Button loginButton;

    TextView textView37;

    ProgressBar progressBar;

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getLayoutInflater().inflate(R.layout.activity_login, frameLayout);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_login);

        editTextUserLogin = (EditText) findViewById(R.id.editTextUserLogin);
        editTextPasswordLogin = (EditText) findViewById(R.id.editTextPasswordLogin);
        textView37 = (TextView) findViewById(R.id.textView37);
        editTextServer = (EditText) findViewById(R.id.editTextServer);
        btnSearchIp = (ImageView) findViewById(R.id.btnSearchIp);

        loginButton = (Button) findViewById(R.id.buttonLogin);

        progressBar = (ProgressBar) findViewById(R.id.progressBarLoadData);

        SharedPreferences settings = getSharedPreferences(ActivityMain.PREF_NAME, 0);
        String fi = settings.getString(SERVER_URL, "");
        if(!fi.equals("")) {
            editTextServer.setText(fi);
        }else{
            findServerIp();
        }

        btnSearchIp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findServerIp();
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
                handler.updateStatus(StatusDatabaseHandler.STATUS_UPDATE_DONE);

                showInfotextAndProgress(true);

                RequestManager.setApiUrl(editTextServer.getText().toString().trim());

                lockInputs(true);

                textView37.setText("Authentifizierung...");
                startLoginRequest(editTextUserLogin.getText().toString().trim(), editTextPasswordLogin.getText().toString());
            }
        });

        updateRequestReceiver = new RequestServiceBroadcastReceiver(new RequestListener() {

            @Override
            public void requestDone(String activityClassName) {
                showInfotextAndProgress(false);

                MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
                AppUser currentUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());
                if (currentUser != null) {
                    currentUser.setUpdateModusType(UpdateModusType.NO_UPDATE);
                    mainDatabaseHandler.updateAppUser(currentUser);
                }

                Intent intent = getIntent();
                setResult(Activity.RESULT_OK, intent);
                finish();
            }

            @Override
            public void generalMessage(String activityClassName, BroadcastMessageActionEnum broadcastMessageActionEnum, String message) {
                if (activityClassName.equals(DialogUpdateRunning.class.getSimpleName())){
                    if (BroadcastMessageActionEnum.PROGRESS.equals(broadcastMessageActionEnum)) {
                        showInfotextAndProgress(true);
                        textView37.setText(message);
                    }else if (BroadcastMessageActionEnum.ERROR.equals(broadcastMessageActionEnum)) {
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                        showInfotextAndProgress(false);
                        lockInputs(false);
                    }
                }
            }

            @Override
            public void loginMessage(LoginStatusEnum loginStatusEnum) {
                if (LoginStatusEnum.ACCESS_TOKEN_INVALID.equals(loginStatusEnum)) {
                    showInfotextAndProgress(false);
                    lockInputs(false);
                    Toast.makeText(getApplicationContext(), "Authentifizierungsfehler!", Toast.LENGTH_LONG).show();
                }else if (LoginStatusEnum.NO_LOGIN_DATA.equals(loginStatusEnum)) {
                    showInfotextAndProgress(false);
                    lockInputs(false);
                }else if (LoginStatusEnum.BAD_CREDENTIALS.equals(loginStatusEnum)){
                    showInfotextAndProgress(false);
                    lockInputs(false);
                    Toast.makeText(getApplicationContext(), "Benutzername und/oder Passwort falsch!", Toast.LENGTH_SHORT).show();
                }else if (LoginStatusEnum.LOGIN_SUCCESSFUL.equals(loginStatusEnum)){

                    MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
                    AppUser currentUser = LoginUserHelper.currentLoggedInUser(getApplicationContext());

//                    MyFirebaseInstanceIDService.sendRegistrationToServer(getApplicationContext());

                    textView37.setVisibility(View.VISIBLE);

                    SharedPreferences settings = getSharedPreferences(ActivityMain.PREF_NAME, 0);
                    SharedPreferences.Editor sEditor = settings.edit();

                    String userServerName = editTextServer.getText().toString().trim() + "_" + editTextUserLogin.getText().toString();

                    String fi = settings.getString(LAST_LOGGED_IN_USER, "");

                    if(!fi.equals("") && fi.equals(userServerName)) {
                        textView37.setText("Aktualisiere Daten...");
                        currentUser.setUpdateModusType(UpdateModusType.PATCH_UPDATE);
                    }else{
                        textView37.setText("Starte Download...");
                        currentUser.setUpdateModusType(UpdateModusType.FULL_UPDATE);
                    }

                    sEditor.putString(SERVER_URL, editTextServer.getText().toString().trim());
                    sEditor.putString(LAST_LOGGED_IN_USER, userServerName);
                    sEditor.commit();

                    currentUser.setAppUserPassword(LoginUserHelper.encodePassword(editTextPasswordLogin.getText().toString()));
                    mainDatabaseHandler.updateAppUser(currentUser);

                    Intent rIntent = new Intent(getApplicationContext(), RequestUpdateService.class);
                    rIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_TYPE_PATCH_UPDATE);
                    startService(rIntent);
                }
            }

            @Override
            public void onlineStatusMessage(OnlineStatusEnum onlineStatusEnum) {

                if (OnlineStatusEnum.MANUAL_OFFLINE.equals(onlineStatusEnum) || OnlineStatusEnum.UNEXPECTED_OFFLINE.equals(onlineStatusEnum)){
                    MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(getApplicationContext());
                    SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_APP_USER);
                    sqlBuilder.isEqual(MainDatabaseHandler.VAR_EMAIL, editTextUserLogin.getText().toString().trim());

                    List<AppUser> appUserList = mainDatabaseHandler.findAppUsers(sqlBuilder);

                    if (!appUserList.isEmpty()){

                        SharedPreferences settings = getSharedPreferences(ActivityMain.PREF_NAME, 0);

                        String userServerName = editTextServer.getText().toString().trim() + "_" + editTextUserLogin.getText().toString();
                        String fi = settings.getString(LAST_LOGGED_IN_USER, "");

                        if(!fi.equals("") && fi.equals(userServerName)) {
                            AppUser appUser = appUserList.get(0);

                            String passwordUserInput = editTextPasswordLogin.getText().toString();
                            String passwordHashUserDb = appUser.getAppUserPassword();

                            if (LoginUserHelper.passwordMatches(passwordUserInput, passwordHashUserDb)){

                                showInfotextAndProgress(false);

                                appUser.setLoggedIn(true);
                                appUser.setUpdateModusType(UpdateModusType.NO_UPDATE);
                                mainDatabaseHandler.updateAppUser(appUser);

                                Intent intent = getIntent();
                                setResult(Activity.RESULT_OK, intent);
                                finish();

                                Toast.makeText(getApplicationContext(), "Sie wurden offline eingeloggt!", Toast.LENGTH_SHORT).show();

                                return;
                            }
                        }

                    }
                }

                Toast.makeText(getApplicationContext(), "Keine Internet-Verbindung!\nSynchronistation erst sobald wieder online.", Toast.LENGTH_SHORT).show();

                lockInputs(false);
                showInfotextAndProgress(false);
            }

        });

        sendMessageInternal(ActivityMain.class, BroadcastMessageActionEnum.INTERNAL, "clear");

        LoginUserHelper.logoutAllUsers(getApplicationContext());
    }

    public void findServerIp(){
        new FindServerService().execute();
        lockInputs(true);
        showInfotextAndProgress(true);
        textView37.setText("Suche nach NextBill-Server im Netzwerk...");
    }

    private void sendMessageInternal(Class activityClassName, BroadcastMessageActionEnum broadcastMessageActionEnum, String message) {
        Intent doneIntent = new Intent();
        doneIntent.setAction(RequestManager.BC_SEND_MESSAGE);
        doneIntent.putExtra("ACTIVITY_CLASS_NAME", activityClassName.getSimpleName());
        doneIntent.putExtra("ACTION", broadcastMessageActionEnum.name());
        doneIntent.putExtra("PROGRESS_MESSAGE", message);
        sendBroadcast(doneIntent);
    }

    public void startLoginRequest(String username, String password){
        Intent sIntent = new Intent(getApplicationContext(), RequestUpdateService.class);
        sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_LOGIN);
        sIntent.putExtra(RequestUpdateService.KEY_USERNAME, username);
        sIntent.putExtra(RequestUpdateService.KEY_PASSWORD, password);
        getApplicationContext().startService(sIntent);
    }

    public void showInfotextAndProgress(boolean show) {
        if (show){
            progressBar.setVisibility(View.VISIBLE);
            textView37.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.GONE);
        }else{
            progressBar.setVisibility(View.INVISIBLE);
            textView37.setVisibility(View.INVISIBLE);
            loginButton.setVisibility(View.VISIBLE);
        }
    }

    public void lockInputs(boolean lock) {

        boolean lockTmp = !lock;

        editTextUserLogin.setEnabled(lockTmp);
        editTextPasswordLogin.setEnabled(lockTmp);
        editTextServer.setEnabled(lockTmp);
        btnSearchIp.setEnabled(lockTmp);
    }

    @Override
    public void onBackPressed(){
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(RequestManager.BC_LOGIN);
        filter.addAction(RequestManager.BC_ONLINE);
        filter.addAction(RequestManager.BC_SEND_MESSAGE);
        filter.addAction(RequestManager.BC_REQUEST_DONE);
        registerReceiver(updateRequestReceiver, filter);

        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        handler.updateStatus(StatusDatabaseHandler.STATUS_UPDATE_DONE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(updateRequestReceiver);
    }

    public class FindServerService extends AsyncTask<String, Integer, List<String>> {

        @Override
        protected List<String> doInBackground(String... strings) {

            String wlanIp = getWifiApIpAddressOfWlan();

            if (wlanIp == null) {
                return new ArrayList<>();
            }

            String[] wlanIpSplitted = wlanIp.split("\\.");

            String firstPartOfWlanIp = wlanIpSplitted[0] + "." + wlanIpSplitted[1] + "." + wlanIpSplitted[2] + ".";

            String serverIp = findIp(firstPartOfWlanIp);

            if (serverIp == null) {
                return new ArrayList<>();
            }

            List<String> objects = new ArrayList<>();
            objects.add(serverIp);

            return objects;
        }

        public String findIp(String prefixIp) {


            for (int i = 0; i <= 110; i++ ) {
                String tryIp = prefixIp + i;
                String result = sendRequest(tryIp, 25);
                if (result != null) {
                    return result;
                }
            }

            for (int i = 0; i <= 110; i++ ) {
                String tryIp = prefixIp + i;
                String result = sendRequest(tryIp, 90);
                if (result != null) {
                    return result;
                }
            }

            return null;
        }

        public String sendRequest(String tryIp, Integer units) {
            try {

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(units, TimeUnit.MILLISECONDS)
                        .writeTimeout(units, TimeUnit.MILLISECONDS)
                        .readTimeout(units, TimeUnit.MILLISECONDS)
                        .build();

                Request request = new Request.Builder()
                        .url("http://" + tryIp + ":8010/online/check")
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()){
                    return tryIp;
                }
            } catch (MalformedURLException e) {
            } catch (IOException e) {
            }

            return null;
        }

        public String getWifiApIpAddressOfWlan() {
            try {
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                        .hasMoreElements();) {
                    NetworkInterface intf = en.nextElement();
                    if (intf.getName().contains("wlan")) {
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
                                .hasMoreElements();) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress()
                                    && (inetAddress.getAddress().length == 4)) {
                                return inetAddress.getHostAddress();
                            }
                        }
                    }
                }
            } catch (SocketException ex) {

            }
            return null;
        }

        @Override
        protected void onPostExecute(List<String> objects) {
            if (!objects.isEmpty()) {
                String foundIp = "http://" + objects.get(0) + ":8010";
                editTextServer.setText(foundIp);
                Log.w(TAG, "Found ip :" + foundIp);
                editTextUserLogin.requestFocus();

                Toast.makeText(getApplicationContext(), "NextBill-Server erfolgreich gefunden!", Toast.LENGTH_LONG).show();
            } else{
                Log.w(TAG, "No Ip found!");
                String foundIp = "http://"+getWifiApIpAddressOfWlan()+":8010";
                editTextServer.setText(foundIp);
                editTextServer.selectAll();

                Toast.makeText(getApplicationContext(), "NextBill-Server nicht gefunden! Bitte manuell eintragen.", Toast.LENGTH_LONG).show();
            }

            lockInputs(false);
            showInfotextAndProgress(false);
        }
    }
}
