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

package de.nextbill.client.helper;

import android.content.Context;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.enums.BasicStatusEnum;
import de.nextbill.client.enums.LoginStatusEnum;
import de.nextbill.client.exceptions.AccessExpirationException;
import de.nextbill.client.exceptions.LoginDataException;
import de.nextbill.client.exceptions.NoUserAndRefreshTokenFoundException;
import de.nextbill.client.model.AppUser;
import de.nextbill.client.model.UserContact;
import de.nextbill.client.utils.SqlBuilder;


public class LoginUserHelper {

    public static AppUser currentLoggedInUser(Context context){
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);
        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_APP_USER);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_APP_USER_IS_LOGGED_IN, "1");

        List<AppUser> appUserList = mainDatabaseHandler.findAppUsers(sqlBuilder);
        if (!appUserList.isEmpty() && appUserList.get(0).getAppUserAccessExpiration().after(new Date())){
            return appUserList.get(0);
        }

        return null;
    }

    public static void hasCurrentUserValidOAuthData(Context context) throws NoUserAndRefreshTokenFoundException, AccessExpirationException, LoginDataException {
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);
        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_APP_USER);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_APP_USER_IS_LOGGED_IN, "1");

        List<AppUser> appUserList = mainDatabaseHandler.findAppUsers(sqlBuilder);

        if (appUserList.isEmpty()){
            logoutAllUsers(context);
            throw new NoUserAndRefreshTokenFoundException("");
        }

        AppUser appUser = appUserList.get(0);

        if (appUser.getAppUserAccessExpiration() == null || appUser.getAppUserAccessExpiration().before(new Date())){

            if (appUser.getAppUserRefreshToken() != null){
                logoutAllUsers(context);
                throw new AccessExpirationException("", appUser);
            }else{
                logoutAllUsers(context);
                throw new NoUserAndRefreshTokenFoundException("");
            }

        }

        if (appUser.getAppUserAccessKey() == null){
            throw new LoginDataException(LoginStatusEnum.ACCESS_TOKEN_INVALID);
        }
    }

    public static void logoutAllUsers(Context context){
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);
        List<AppUser> appUserList = mainDatabaseHandler.findAppUsers();

        for (AppUser appUser : appUserList) {
            appUser.setLoggedIn(false);
            mainDatabaseHandler.updateAppUser(appUser);
        }
    }

    public static List<String> idsOfUserContactsWithCurrentUser(Context context){
        MainDatabaseHandler mainDatabaseHandler = MainDatabaseHandler.getInstance(context);

        List<String> ids = new ArrayList<>();

        AppUser appUser = LoginUserHelper.currentLoggedInUser(context);

        SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_USER_CONTACT);
        sqlBuilder.isEqual(MainDatabaseHandler.VAR_APP_USER_CONTACT_ID, appUser.getAppUserId().toString())
                .and()
                .isNotNull(MainDatabaseHandler.VAR_APP_USER_CONTACT_ID)
                .and()
                .startBracket()
                .isEqual(MainDatabaseHandler.VAR_BASIC_STATUS_ENUM, BasicStatusEnum.OK.name())
                .or()
                .isNull(MainDatabaseHandler.VAR_BASIC_STATUS_ENUM)
                .endBracket();

        List<UserContact> userContacts = mainDatabaseHandler.findUserContactsSql(sqlBuilder);
        for (UserContact userContact : userContacts) {
            ids.add(userContact.getUserContactId().toString());
        }

        return ids;
    }

    public static String encodePassword(String password)  {
//        SecureRandom random = new SecureRandom();
//        byte[] salt = new byte[16];
//        random.nextBytes(salt);

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update("randomSalt".getBytes());

        byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));

        return new String(hashedPassword);
    }

    public static boolean passwordMatches(String password, String hash) {
//        SecureRandom random = new SecureRandom();
//        byte[] salt = new byte[16];
//        random.nextBytes(salt);

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update("randomSalt".getBytes());

        byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));

        return new String(hashedPassword).equals(hash);
    }
}
