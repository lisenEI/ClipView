package com.lisen.library.clip;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.lisen.library.R;
import com.lisen.library.utils.UriUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import top.zibin.luban.Luban;

import static com.lisen.library.utils.BitmapUtil.readPictureDegree;
import static com.lisen.library.utils.BitmapUtil.rotateBitmap;

/**
 * @author lisen
 * @since 12-17-2018
 */

public class ClipActivity extends AppCompatActivity {

    /**
     * 不压缩的图片大小
     */
    private static final int MAX_IGNORE_SIZE = 1024;

    /**
     * 裁剪图片以后保存的图片名
     */
    private static final String CLIP_IMAGE_FILE_NAME = "clip_image.png";

    /**
     * 回调 intent 传的参数名
     */
    public static final String RESULT_CLIP_IMAGE_PATH = "result_clip_image_path";

    public static final String EXTRAS_CLIP_VIEW_SHADOW_COLOR = "extras_clip_view_shadow_color";
    public static final String EXTRAS_CLIP_VIEW_MAX_SCALE_TIMES = "extras_clip_view_max_scale_times";
    public static final String EXTRAS_CLIP_VIEW_CLIP_REGION_RATIO = "extras_clip_view_clip_region_ratio";
    public static final String EXTRAS_CLIP_VIEW_RECT_CLIP_REGION = "extras_clip_view_rect_clip_region";


    private ClipView mClipView;

    private ProgressBar mLoading;

    private Disposable mDisposable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clipview);

        mClipView = findViewById(R.id.clip_view);
        mLoading = findViewById(R.id.loading);

        initClipView();

        setupBitmap();
    }

    private void initClipView() {
        Intent intent = getIntent();
        int shadowColor = intent.getIntExtra(EXTRAS_CLIP_VIEW_SHADOW_COLOR, -1);
        float maxScaleTimes = intent.getFloatExtra(EXTRAS_CLIP_VIEW_MAX_SCALE_TIMES, -1);
        float clipRegionRatio = intent.getFloatExtra(EXTRAS_CLIP_VIEW_CLIP_REGION_RATIO, -1);
        boolean useRect = intent.getBooleanExtra(EXTRAS_CLIP_VIEW_RECT_CLIP_REGION, false);

        if (shadowColor != -1) mClipView.setShadowColor(shadowColor);
        if (maxScaleTimes != -1) mClipView.setMaxScaleTimes(maxScaleTimes);
        if (clipRegionRatio != -1) mClipView.setClipRegionRatio(clipRegionRatio);
        if (useRect) mClipView.useRectClipRegion();

    }

    public void rotate(View view) {
        mClipView.rotate();
    }

    public void select(View view) {
        if (mLoading.getVisibility() == View.VISIBLE) {
            return;
        }
        mLoading.setVisibility(View.VISIBLE);

        mDisposable = Observable
                .just(mClipView.clipBitmap())
                .map(bitmap -> saveBitmap(bitmap))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(path -> {
                    Intent intent = new Intent();
                    if (!TextUtils.isEmpty(path)) {
                        intent.putExtra(RESULT_CLIP_IMAGE_PATH, path);
                    }
                    setResult(RESULT_OK, intent);
                    finish();
                }, e -> {
                    Toast.makeText(this, getResources().getString(R.string.clip_view_load_bitmap_error), Toast.LENGTH_SHORT).show();
                    mLoading.setVisibility(View.GONE);
                }, () -> mLoading.setVisibility(View.GONE));
    }

    @Nullable
    private String saveBitmap(Bitmap bitmap) {
        File cacheDir = getCacheDir();
        File file = new File(cacheDir, CLIP_IMAGE_FILE_NAME);
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        } catch (FileNotFoundException e) {
            file = null;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (file == null || !file.exists()) {
            return null;
        }

        return file.getAbsolutePath();
    }

    public void back(View view) {
        finish();
    }

    private void setupBitmap() {
        Observable
                .just(getIntent().getData())
                .map(uri -> {
                    if (uri != null) {
                        String path = UriUtil.getPath(this, uri);
                        if (!TextUtils.isEmpty(path)) {
                            List<File> files = Luban.with(this).load(path).ignoreBy(MAX_IGNORE_SIZE).get();
                            if (files != null && files.size() > 0) {
                                File file = files.get(0);
                                if (file != null) {
                                    return file.getAbsolutePath();
                                }
                            }
                        }
                    }
                    return null;
                })
                .map(path -> {
                    if (!TextUtils.isEmpty(path)) {
                        Bitmap bitmap = BitmapFactory.decodeFile(path);
                        int degree = readPictureDegree(path);
                        if (degree != 0 && bitmap != null) {
                            bitmap = rotateBitmap(degree, bitmap);
                        }
                        return bitmap;
                    }
                    return null;
                })
                .filter(bitmap -> bitmap != null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bitmap -> {
                            mClipView.setBitmap(bitmap);
                            mLoading.setVisibility(View.GONE);
                        },
                        throwable -> {
                            Toast.makeText(this, getResources().getString(R.string.clip_view_load_bitmap_error), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
        }
    }
}
