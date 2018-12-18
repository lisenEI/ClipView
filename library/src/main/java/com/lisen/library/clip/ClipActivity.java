package com.lisen.library.clip;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.lisen.library.R;

import java.io.ByteArrayOutputStream;

import static com.lisen.library.utils.BitmapUtil.getBitmap;

/**
 * @author lisen
 * @since 12-17-2018
 */

public class ClipActivity extends AppCompatActivity {

    private ClipView mClipView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clipview);

        mClipView = findViewById(R.id.clip_view);

        Uri uri = getIntent().getData();

        if (uri != null) {
            Bitmap bitmap = getBitmap(this, uri);
            if (bitmap != null) {
                mClipView.setBitmap(bitmap);
            }
        } else {
            finish();
        }
    }

    public void rotate(View view) {
        mClipView.rotate();
    }

    public void select(View view) {
        Bitmap clipBitmap = mClipView.clipBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        clipBitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
        byte[] bitmapByte = baos.toByteArray();

        Intent intent = new Intent();
        intent.putExtra("bitmap", bitmapByte);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void back(View view) {
        finish();
    }
}
