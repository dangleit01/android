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
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class StatusDatabaseHandler extends SQLiteOpenHelper {
    private static final String TABLE_UPDATE = "UpdateTable";
    private static final String VAR_ID = "_id";
    private static final String VAR_OBJECT_TYPE = "object_type";
    private static final String VAR_OBJECT_ID = "object_id";
    private static final String VAR_UPDATE_STATUS = "update_status";
    private static final String VAR_USER_ID = "userId";
    private static final String VAR_CRATED = "created";

    public static final int OBJECT_TYPE_PATCH_UPDATE = 17;
    public static final int OBJECT_TYPE_INVOICE_IMAGE = 1;
    public static final int OBJECT_TYPE_INVOICE = 2;
    public static final int OBJECT_TYPE_INVOICE_FAILURE = 3;
    public static final int OBJECT_TYPE_APP_USER = 4;
    public static final int OBJECT_TYPE_BUSINESS_PARTNER = 5;
    public static final int OBJECT_TYPE_COST_DISTRIBUTION_ITEM = 6;
    public static final int OBJECT_TYPE_COST_DISTRIBUTION = 7;
    public static final int OBJECT_TYPE_USER_CONTACT = 8;
    //	public static final int OBJECT_TYPE_REPORT=9;
    public static final int OBJECT_TYPE_STANDING_ORDER = 10;
    public static final int OBJECT_TYPE_INVOICE_CATEGORY = 11;
    public static final int OBJECT_TYPE_FIREBASE_TOKEN = 12;
    public static final int OBJECT_TYPE_BILLING = 13;
    public static final int OBJECT_TYPE_BASIC_DATA = 14;
    public static final int OBJECT_TYPE_PAYMENT_MISTAKE = 15;
    public static final int OBJECT_TYPE_SPEECH_RECOGNITION = 16;
    public static final int OBJECT_TYPE_BILLING_LIST_ITEMS = 18;
    public static final int OBJECT_TYPE_BUDGET = 19;


    public static final int UPDATE_STATUS_DELETE = 1;
    public static final int UPDATE_STATUS_UPDATE = 2;
    public static final int UPDATE_STATUS_ADD = 3;
    public static final int UPDATE_STATUS_GET = 5;
    public static final int UPDATE_STATUS_PUT = 4;
    public static final int UPDATE_STATUS_LOGIN = 6;
    public static final int UPDATE_STATUS_GET_LIST = 7;
    public static final int UPDATE_STATUS_DELETE_SPECIAL = 8;
    public static final int UPDATE_STATUS_GET_VALUE = 9;

    private static final String DB_NAME = "nextbillstatus";
    private static final String TABLE_STATUS = "status";
    private static final String KEY_ID = "_id";
    private static final String COL_KEY = "statusKey";
    private static final String COL_VALUE = "value";
    private static final int DB_VERSION = 2;

    private static final String ATTR_STATUS = "status";

    public static final int STATUS_RUNNING = 2;
    public static final int STATUS_UPDATE_DONE = 3;
    public static final int STATUS_MANUAL_OFFLINE = 4;
    public static final int STATUS_UNEXPECTED_OFFLINE = 5;

    private static final String TAG = "StatusDatabaseHandler";

    private static StatusDatabaseHandler mInstance = null;

    private StatusDatabaseHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static StatusDatabaseHandler getInstance(Context ctx) {

        if (mInstance == null) {
            mInstance = new StatusDatabaseHandler(ctx.getApplicationContext());
            Integer value = mInstance.getStatus();
            if (value == -1) {
                mInstance.insertStatus(STATUS_UPDATE_DONE);
            }
        }
        return mInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sqlString = "CREATE TABLE " + TABLE_STATUS +
                "(" + KEY_ID + " INTEGER PRIMARY KEY," +
                COL_KEY + " VARCHAR(255) NOT NULL," +
                COL_VALUE + " VARCHAR(255) NOT NULL" +
                ")";
        db.execSQL(sqlString);

        sqlString = "CREATE TABLE " + TABLE_UPDATE +
                "(" + VAR_ID + " INTEGER PRIMARY KEY," +
                VAR_OBJECT_ID + " TEXT," +
                VAR_OBJECT_TYPE + " INTEGER," +
                VAR_UPDATE_STATUS + " INTEGER," +
                VAR_CRATED + " LONG," +
                VAR_USER_ID + " TEXT)";
        db.execSQL(sqlString);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STATUS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_UPDATE);
        onCreate(db);
    }


    public void updateStatus(int status) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            values.put(COL_KEY, ATTR_STATUS);
            values.put(COL_VALUE, Integer.toString(status));

            int numEffected = db.update(TABLE_STATUS, values, COL_KEY + "=?", new String[]{ATTR_STATUS});
            Log.v(TAG, "::updateStatus -> numEffected=" + numEffected);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void insertStatus(int status) {

        SQLiteDatabase db = this.getWritableDatabase();

        try {

            ContentValues values = new ContentValues();
            values.put(COL_KEY, ATTR_STATUS);
            values.put(COL_VALUE, Integer.toString(status));
            db.insert(TABLE_STATUS, null, values);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public int getStatus() {
        SQLiteDatabase db = this.getReadableDatabase();
        String sqlString = "SELECT * FROM " + TABLE_STATUS + " WHERE " + COL_KEY + "=?;";
        Cursor cursor = db.rawQuery(sqlString, new String[]{ATTR_STATUS});

        int retVal = -1;
        try {

            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                retVal = Integer.valueOf(cursor.getString(2));
                return retVal;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }

        Log.v(TAG, "::getStatus -> " + retVal);
        return retVal;

    }

    public long addObject(String objectId, int objectType, int updateStatus, long created, int userId) {

        SQLiteDatabase db = getWritableDatabase();
        Long newId = null;

        try {

            ContentValues values = new ContentValues();
            values.put(VAR_OBJECT_ID, objectId);
            values.put(VAR_OBJECT_TYPE, objectType);
            values.put(VAR_UPDATE_STATUS, updateStatus);
            values.put(VAR_CRATED, created);
            values.put(VAR_USER_ID, userId);

            newId = db.insert(TABLE_UPDATE, null, values);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return newId;
    }

    public int deleteObject(int rowId) {
        SQLiteDatabase db = getWritableDatabase();
        Integer retVal = -1;

        try {

            retVal = db.delete(TABLE_UPDATE, VAR_ID + " = ?", new String[]{Integer.toString(rowId)});

        } catch (Exception e) {
            e.printStackTrace();
        }

        return retVal;
    }

    public List<UpdateObject> getUpdateObjects() {
        List<UpdateObject> uoList = new ArrayList<StatusDatabaseHandler.UpdateObject>();
        SQLiteDatabase db = getReadableDatabase();
        String sqlString = "SELECT * FROM " + TABLE_UPDATE + ";";
        Cursor cursor = db.rawQuery(sqlString, null);

        try {
            if (cursor.getCount() > 0) {
                UpdateObject tmpObject;
                while (!cursor.isLast()) {
                    cursor.moveToNext();
                    tmpObject = new UpdateObject();
                    tmpObject.dbId = cursor.getInt(0);
                    tmpObject.objectId = cursor.getString(1);
                    tmpObject.objectType = cursor.getInt(2);
                    tmpObject.updateStatus = cursor.getInt(3);
                    tmpObject.created = cursor.getLong(4);
                    tmpObject.userId = cursor.getInt(5);
                    uoList.add(tmpObject);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }

        return uoList;
    }

    public class UpdateObject {
        public int dbId;
        public String objectId;
        public int objectType;
        public int updateStatus;
        public int userId;
        public long created;

        public UpdateObject() {
        }
    }

}
