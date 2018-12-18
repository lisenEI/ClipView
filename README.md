# ClipView
ClipView 是一个图片裁剪工具，支持配置项
* [x] 截图框为圆形或方形
* [x] 截图框大小
* [x] 遮罩颜色
* [x] 图片缩放
* [x] 图片位移
* [x] 设置最大放大倍数
* [x] 使用系统截图框 or 自定义截图框


## 效果图如下
<img src="steps/1.png" width="150" hegiht="60" align=center />
<img src="steps/2.png" width="150" hegiht="60" align=center />
<img src="steps/3.png" width="150" hegiht="60" align=center />
<img src="steps/4.png" width="150" hegiht="60" align=center />

## 使用方法
1. 初始化 ClipManager
```java
mClipManager = new ClipManager(this);
```
2. 初始化 ClipCallback，用于监听截图回调
```java
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
```
3. 设置是否使用系统截图框（false 自定义，true 系统截图框）
```java
            mClipManager.setUserDefaultCrop(false);
```
4. 拍照
```java
 mClipManager.openCamera(MainActivity.this, mClipCallback);
```
5. 或打开相册
```java
mClipManager.openGallery(MainActivity.this, mClipCallback);
```
6. 在 onActivityResult 调用 ClipManager 的 onActivityResult 方法
```java
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //这里监听回调
        mClipManager.onActivityResult(this, requestCode, resultCode, data);
    }
```

注意事项：
* Android 6.0 以上手机需要增加权限配置
* 需要配置 FileProvider
```html
        <provider
            android:name="android.support.v4.content.FileProvider"
            android:authorities="com.clipview.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
```
* 配置 file_provider 要和上面的 authorities 保持一致
```html
<string name="file_provider">com.clipview.fileprovider</string>
```

