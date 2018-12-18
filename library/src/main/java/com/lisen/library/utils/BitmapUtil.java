package com.lisen.library.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.IOException;

import static com.lisen.library.utils.UriUtil.getImageAbsolutePath;

/**
 * @author lisen
 * @since 12-18-2018
 */

public class BitmapUtil {
    /**
     * 根据 uri 读取 bitmap
     *
     * @param context
     * @param uri
     * @return
     */
    public static Bitmap getBitmap(Context context, Uri uri) {
        String path = getImageAbsolutePath(context, uri);
        int degree = readPictureDegree(path);
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        if (degree != 0) {
            bitmap = rotateBitmap(degree, bitmap);
        }
        return bitmap;
    }

    /**
     * 获取图片信息
     *
     * @param path
     * @return
     */
    public static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    /**
     * 图片旋转
     *
     * @param angle
     * @param bitmap
     * @return
     */
    public static Bitmap rotateBitmap(int angle, Bitmap bitmap) {
        // 旋转图片
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        // 创建新的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap,
                0,
                0,
                bitmap.getWidth(),
                bitmap.getHeight(),
                matrix,
                true);
        return resizedBitmap;
    }
}
