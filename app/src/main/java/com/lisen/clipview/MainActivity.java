package com.lisen.clipview;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.Toast;

import com.lisen.clipview.popwindow.PWTakePhoto;
import com.lisen.library.clip.ClipManager;
import com.lisen.library.utils.PermissionUtil;

import static com.lisen.library.utils.PermissionUtil.checkPermissions;

public class MainActivity extends AppCompatActivity {

    private ImageView mPhoto;

    private ClipManager mClipManager;

    private static String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    private ClipManager.ClipCallback mClipCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 检查权限
        checkPermissions(this, permissions);

        //初始化 ClipManager
        mClipManager = new ClipManager(this);
        //截图回调
        mClipCallback = new ClipManager.ClipCallback() {
            @Override
            public void onSuccess(@Nullable Bitmap bitmap) {
                mPhoto.setImageBitmap(bitmap);
            }

            @Override
            public void onError(String msg) {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {
                Toast.makeText(MainActivity.this, "cancel", Toast.LENGTH_SHORT).show();
            }
        };

        mPhoto = findViewById(R.id.iv_photo);

        findViewById(R.id.bt).setOnClickListener(v -> {
            //使用自定义截图框
            mClipManager.setUserDefaultCrop(false);
            takePhoto();
        });

        findViewById(R.id.bt_system).setOnClickListener(v -> {
            //使用系统截图框
            mClipManager.setUserDefaultCrop(true);
            takePhoto();
        });
    }

    private void takePhoto() {
        new PWTakePhoto(this)
                .setOnClickListener(v -> {
                    switch (v.getId()) {
                        case PWTakePhoto.R_ID_BTN_0:
                            //拍照
                            mClipManager.openCamera(MainActivity.this, mClipCallback);
                            break;
                        case PWTakePhoto.R_ID_BTN_1:
                            //打开相册
                            mClipManager.openGallery(MainActivity.this, mClipCallback);
                            break;
                        default:
                            break;
                    }
                }).showBottom(getWindow().getDecorView());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //这里监听回调
        mClipManager.onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //检查权限
        PermissionUtil.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }
}
