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

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class IOHelper {

    public static final int STORAGE_EXTERN = 2;

    public static final String STORAGE_NAME = "nextbill";

    public static IOHelper getInstance() {
        return instance;
    }

    private static IOHelper instance = new IOHelper();
    private Context ctx;
    private int storage;

    private IOHelper() {
        this.storage = STORAGE_EXTERN;
    }

    public void setCtx(Context ctx) {
        this.ctx = ctx;
    }

    public static String getAppDirectory() {
        String url = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + STORAGE_NAME;
        try {
            createAppDir(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return url;
    }


    private static void deleteDir(File dir) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory())
                        deleteDir(file);
                    else
                        file.delete();
                }
            }
        }
        dir.delete();
    }

    public static void deleteAppDir() {
        Log.w("IOHelper", "IOHelper::deleteAppDir");
        String appDir = getAppDirectory();
        File file = new File(appDir);
        if (file != null && file.exists()) {
            deleteDir(file);
        }
    }

    public static String getImageDirectory() {
        String url = getAppDirectory() + "/images";
        try {
            createImageDir(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return url;
    }

    public static void createAppDir(String url) throws IOException {
        File appDirFile = new File(url);
        if (appDirFile.exists() == false) {
            boolean created = appDirFile.mkdirs();
            if (!created) {
                throw new IOException("Error creating Directory: " + appDirFile.getAbsolutePath());
            }
        }
    }

    public static void createImageDir(String url) throws IOException {
        File imageDirFile = new File(url);
        if (imageDirFile.exists() == false) {
            boolean created = imageDirFile.mkdirs();
            if (!created) {
                throw new IOException("Error creating Directory: " + imageDirFile.getAbsolutePath());
            }
        }
    }

    public static File getTempUploadMediaFile(String id) {
        File mediaStorageDir = new File(getImageDirectory(), "uploaded");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        return new File(mediaStorageDir.getPath() + File.separator + id + ".jpg");
    }
}
