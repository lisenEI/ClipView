package com.lisen.library.clip;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;

import com.lisen.library.R;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author lisen
 * @since 12-18-2018
 */

public class ClipManager {
    /**
     * 相册
     */
    public static final int CAMERA_IMG = 1;

    /**
     * 拍照
     */
    public static final int PHOTO_IMG = 2;

    /**
     * 裁剪
     */
    public static final int CUT_IMG = 3;

    /**
     * 是否使用系统默认裁剪
     */
    private boolean mUserDefaultCrop;

    /**
     * 拍照所得相片路径
     */
    private File mCameraFile = null;

    /**
     * 裁切照片存储路径
     */
    private File mCutFile = null;

    private static final String CAMERA_NAME = "camera.png";

    private static final String CUT_IMAGE_NAME = "cut_image.png";

    private ClipCallback mClipCallback;


    public ClipManager(Context context) {
        init(context);
    }

    public void init(@Nullable Context context) {
        File cacheDir = new File(context.getCacheDir(), "images");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        mCameraFile = new File(cacheDir, CAMERA_NAME);

        mCutFile = new File(cacheDir, CUT_IMAGE_NAME);

        if (!mCameraFile.exists()) {
            try {
                mCameraFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!mCutFile.exists()) {
            try {
                mCutFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 相册
     */
    public void openGallery(@NonNull Activity activity, @NonNull ClipCallback callback) {
        mClipCallback = callback;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        activity.startActivityForResult(intent, PHOTO_IMG);
    }

    /**
     * 相机
     */
    public void openCamera(@NonNull Activity activity, @NonNull ClipCallback callback) {
        mClipCallback = callback;
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri uri = getUri(activity, mCameraFile);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        } else {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        }
        activity.startActivityForResult(intent, CAMERA_IMG);
    }

    /**
     * 裁切图片
     */
    private void cutImage(Activity activity, Uri uri, int requestCode) {
        if (mUserDefaultCrop) {
            //使用系统自带的图片裁剪
            Intent intent = new Intent("com.android.camera.action.CROP", null);
            intent.setDataAndType(uri, "image/*");
            //可裁剪
            intent.putExtra("crop", "true");
            //宽高比
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            //裁剪宽高
            intent.putExtra("outputX", 320);
            intent.putExtra("outputY", 320);
            intent.putExtra("noFaceDetection", true);
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", true);
            intent.putExtra("return-data", false);
            Uri cutUri = getUri(activity, mCutFile);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                //将存储图片的uri读写权限授权给剪裁工具应用
                List<ResolveInfo> resInfoList = activity.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    activity.grantUriPermission(packageName, cutUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                intent.putExtra(MediaStore.EXTRA_OUTPUT, cutUri);
            } else {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, cutUri);
            }
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
            intent.putExtra("outputQuality", 10);
            intent.putExtra("noFaceDetaction", true);

            activity.startActivityForResult(intent, CUT_IMG);
        } else {
            Intent clipIntent = new Intent(activity, ClipActivity.class);
            if (requestCode == CAMERA_IMG) {
                uri = Uri.fromFile(mCameraFile);
            }
            clipIntent.setData(uri);
            activity.startActivityForResult(clipIntent, CUT_IMG);
        }
    }

    /**
     * 设置是否使用系统截图框
     *
     * @param userDefaultCrop true 系统 false 自定义
     */
    public void setUserDefaultCrop(boolean userDefaultCrop) {
        mUserDefaultCrop = userDefaultCrop;
    }

    private Uri getUri(@NonNull Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, context.getResources().getString(R.string.file_provider), file);
        } else {
            return Uri.fromFile(file);
        }
    }

    /**
     * 处理 result
     *
     * @param activity
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri;
            switch (requestCode) {
                // 如果是直接从相册获取
                case PHOTO_IMG:
                    uri = data.getData();
                    if (uri == null) {
                        mClipCallback.onError("获取 data 为空");
                        return;
                    }
                    cutImage(activity, uri, requestCode);
                    break;
                // 如果是调用相机拍照时
                case CAMERA_IMG:
                    uri = getUri(activity, mCameraFile);
                    if (uri == null) {
                        mClipCallback.onError("获取 uri 为空");
                        return;
                    }
                    cutImage(activity, uri, requestCode);
                    break;
                // 取得裁剪后的图片
                case CUT_IMG:
                    if (mUserDefaultCrop) {
                        Bitmap bitmap = BitmapFactory.decodeFile(mCutFile.getAbsolutePath());
                        mClipCallback.onSuccess(bitmap);
                    } else {
                        byte[] bis = data.getByteArrayExtra("bitmap");
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bis, 0, bis.length);
                        mClipCallback.onSuccess(bitmap);
                    }
                    break;
                default:
                    mClipCallback.onError("未知错误");
                    break;
            }
        } else {
            mClipCallback.onCancel();
        }
    }

    /**
     * 截图回调
     */
    public interface ClipCallback {
        /**
         * 成功
         *
         * @param bitmap
         */
        void onSuccess(@Nullable Bitmap bitmap);

        /**
         * 失败
         *
         * @param msg
         */
        void onError(String msg);

        /**
         * 取消
         */
        void onCancel();
    }

}
