package com.lisen.library.utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

/**
 * @author lisen
 * @since 12-18-2018
 */

public class PermissionUtil {
    private static final int REQUEST_PERMISSION_CODE = 1111;

    public static void checkPermissions(Activity activity, String[] permissions) {
        ActivityCompat.requestPermissions(
                activity,
                permissions,
                REQUEST_PERMISSION_CODE
        );
    }

    public static void onRequestPermissionsResult(Activity activity, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CODE && grantResults.length > 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(activity, "需要同意权限", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
