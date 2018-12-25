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
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.lisen.library.clip.ClipActivity.EXTRAS_CLIP_VIEW_CLIP_REGION_RATIO;
import static com.lisen.library.clip.ClipActivity.EXTRAS_CLIP_VIEW_MAX_SCALE_TIMES;
import static com.lisen.library.clip.ClipActivity.EXTRAS_CLIP_VIEW_RECT_CLIP_REGION;
import static com.lisen.library.clip.ClipActivity.EXTRAS_CLIP_VIEW_SHADOW_COLOR;
import static com.lisen.library.clip.ClipActivity.RESULT_CLIP_IMAGE_PATH;

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
     * 拍照所得相片路径
     */
    private File mCameraFile = null;

    /**
     * 裁切照片存储路径
     */
    private File mCutFile = null;

    private static final String CAMERA_NAME = "camera.png";

    private static final String CUT_IMAGE_NAME = "cut_image.png";

    private Builder mBuilder;

    public ClipManager(@Nullable Context context, Builder builder) {
        init(context, builder);
    }

    public void init(@Nullable Context context, Builder builder) {
        mBuilder = builder;

        File cacheDir = new File(context.getCacheDir(), builder.getExternalFilesPath());
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
    public void openGallery(@NonNull Activity activity) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        activity.startActivityForResult(intent, PHOTO_IMG);
    }

    /**
     * 相机
     */
    public void openCamera(@NonNull Activity activity) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri uri = getUri(activity, mCameraFile);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        activity.startActivityForResult(intent, CAMERA_IMG);
    }

    /**
     * 裁切图片
     */
    private void cutImage(Activity activity, Uri uri, int requestCode) {
        if (mBuilder.isUseDefaultCrop()) {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                //将存储图片的uri读写权限授权给剪裁工具应用
                List<ResolveInfo> resInfoList = activity.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    activity.grantUriPermission(packageName, cutUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cutUri);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
            intent.putExtra("outputQuality", 10);

            activity.startActivityForResult(intent, CUT_IMG);
        } else {
            Intent clipIntent = new Intent(activity, ClipActivity.class);
            if (requestCode == CAMERA_IMG) {
                uri = Uri.fromFile(mCameraFile);
            }
            clipIntent.setData(uri);
            clipIntent.putExtra(EXTRAS_CLIP_VIEW_SHADOW_COLOR, mBuilder.getShadowColor());
            clipIntent.putExtra(EXTRAS_CLIP_VIEW_CLIP_REGION_RATIO, mBuilder.getClipRegionRatio());
            clipIntent.putExtra(EXTRAS_CLIP_VIEW_MAX_SCALE_TIMES, mBuilder.getMaxScaleTimes());
            clipIntent.putExtra(EXTRAS_CLIP_VIEW_RECT_CLIP_REGION, mBuilder.isUseRect());
            activity.startActivityForResult(clipIntent, CUT_IMG);
        }
    }

    private Uri getUri(@NonNull Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return FileProvider.getUriForFile(context, mBuilder.getAuthority(), file);
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
                    if (data == null) {
                        mBuilder.getClipCallback().onError("result intent null");
                        return;
                    }
                    uri = data.getData();
                    if (uri == null) {
                        mBuilder.getClipCallback().onError("uri is null");
                        return;
                    }
                    cutImage(activity, uri, requestCode);
                    break;
                // 如果是调用相机拍照时
                case CAMERA_IMG:
                    uri = getUri(activity, mCameraFile);
                    if (uri == null) {
                        mBuilder.getClipCallback().onError("uri is null");
                        return;
                    }
                    cutImage(activity, uri, requestCode);
                    break;
                // 取得裁剪后的图片
                case CUT_IMG:
                    if (mBuilder.isUseDefaultCrop()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(mCutFile.getAbsolutePath());
                        mBuilder.getClipCallback().onSuccess(bitmap);
                    } else if (data != null) {
                        String path = data.getStringExtra(RESULT_CLIP_IMAGE_PATH);
                        if (!TextUtils.isEmpty(path)) {
                            Bitmap bitmap = BitmapFactory.decodeFile(path);
                            if (bitmap != null) {
                                mBuilder.getClipCallback().onSuccess(bitmap);
                            } else {
                                mBuilder.getClipCallback().onError("decodeFile error");
                            }
                        } else {
                            mBuilder.getClipCallback().onError("image path is null");
                        }
                    } else {
                        mBuilder.getClipCallback().onError("unknown error");
                    }
                    break;
                default:
                    mBuilder.getClipCallback().onError("unknown error");
                    break;
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            mBuilder.getClipCallback().onCancel();
        } else {
            mBuilder.getClipCallback().onError("unknown error");
        }
    }

    public void setUseDefaultCrop(boolean useDefaultCrop) {
        mBuilder.useDefaultCrop(useDefaultCrop);
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


    public static class Builder {
        /**
         * android.support.v4.content.FileProvider 设置的 Authorities
         */
        private String authority;

        /**
         * 图片存储路径
         */
        private String externalFilesPath;

        /**
         * 使用系统截图还是自定义截图
         */
        private boolean useDefaultCrop;

        /**
         * 自定义截图的遮罩颜色
         */
        private int shadowColor = -1;

        /**
         * 自定义截图图片可放大倍数
         */
        private float maxScaleTimes = -1;

        /**
         * 自定义截图框占屏幕宽的百分比
         */
        private float clipRegionRatio = -1;

        /**
         * 是否使用矩形截图框（默认圆形）
         */
        private boolean useRect;

        /**
         * 截图回调
         */
        ClipCallback clipCallback;

        /**
         * android.support.v4.content.FileProvider 设置的 Authorities
         *
         * @param authority
         */
        public Builder authority(String authority) {
            this.authority = authority;
            return this;
        }

        /**
         * 图片存储路径
         *
         * @param externalFilesPath 比如 pictures
         * @return
         */
        public Builder externalFilesPath(String externalFilesPath) {
            this.externalFilesPath = externalFilesPath;
            return this;
        }

        /**
         * 使用系统截图还是自定义截图
         *
         * @param useDefaultCrop true 默认，false 自定义
         */
        public Builder useDefaultCrop(boolean useDefaultCrop) {
            this.useDefaultCrop = useDefaultCrop;
            return this;
        }

        /**
         * 自定义截图的遮罩颜色
         *
         * @param shadowColor
         * @return
         */
        public Builder shadowColor(@ColorInt int shadowColor) {
            this.shadowColor = shadowColor;
            return this;
        }

        /**
         * 自定义截图图片可放大倍数
         *
         * @param maxScaleTimes must > 0
         * @return
         */
        public Builder maxScaleTimes(float maxScaleTimes) {
            this.maxScaleTimes = maxScaleTimes;
            return this;
        }

        /**
         * 自定义截图框占屏幕宽的百分比
         *
         * @param clipRegionRatio (0,1]
         * @return
         */
        public Builder clipRegionRatio(float clipRegionRatio) {
            this.clipRegionRatio = clipRegionRatio;
            return this;
        }

        /**
         * 是否使用矩形截图框（默认圆形）
         *
         * @param useRect true 矩形 false 圆形
         * @return
         */
        public Builder useRect(boolean useRect) {
            this.useRect = useRect;
            return this;
        }

        /**
         * 截图回调
         *
         * @param clipCallback
         * @return
         */
        public Builder clipCallback(ClipCallback clipCallback) {
            this.clipCallback = clipCallback;
            return this;
        }

        public Builder build() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (TextUtils.isEmpty(authority)) {
                    throw new IllegalArgumentException("authority is null");
                }
            }
            return this;
        }

        public String getAuthority() {
            return authority;
        }

        public String getExternalFilesPath() {
            return externalFilesPath;
        }

        public int getShadowColor() {
            return shadowColor;
        }

        public float getMaxScaleTimes() {
            return maxScaleTimes;
        }

        public float getClipRegionRatio() {
            return clipRegionRatio;
        }

        public boolean isUseRect() {
            return useRect;
        }

        public boolean isUseDefaultCrop() {
            return useDefaultCrop;
        }

        public ClipCallback getClipCallback() {
            return clipCallback;
        }
    }

}
