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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;


public class ImageHelper {

    public static Bitmap decodeFile(String pathName, int dstWidth, int dstHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);
        options.inJustDecodeBounds = false;
        options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, dstWidth, dstHeight);
        Bitmap unscaledBitmap = BitmapFactory.decodeFile(pathName, options);

        return unscaledBitmap;
    }

    public static int calculateSampleSize(int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
            final float srcAspect = (float)srcWidth / (float)srcHeight;
            final float dstAspect = (float)dstWidth / (float)dstHeight;

            if (srcAspect > dstAspect) {
                return srcWidth / dstWidth;
            } else {
                return srcHeight / dstHeight;
            }
    }

    public static Bitmap createScaledBitmap(int dstWidth, int dstHeight, Bitmap unscaledBitmap) {
        Rect srcRect = calculateSrcRect(unscaledBitmap.getWidth(), unscaledBitmap.getHeight());
        Rect dstRect = calculateDstRect(unscaledBitmap.getWidth(), unscaledBitmap.getHeight(), dstWidth, dstHeight);
        Bitmap scaledBitmap = Bitmap.createBitmap(dstRect.width(), dstRect.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(scaledBitmap);
        canvas.drawBitmap(unscaledBitmap, srcRect, dstRect, new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;
    }

    public static Rect calculateSrcRect(int srcWidth, int srcHeight) {
        return new Rect(0, 0, srcWidth, srcHeight);
    }

    public static Rect calculateDstRect(int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
        final float srcAspect = (float)srcWidth / (float)srcHeight;
        final float dstAspect = (float)dstWidth / (float)dstHeight;

        if (srcAspect > dstAspect) {
            return new Rect(0, 0, dstWidth, (int)(dstWidth / srcAspect));
        } else {
            return new Rect(0, 0, (int)(dstHeight * srcAspect), dstHeight);
        }
    }
}
