package com.lisen.library.clip;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.lisen.library.utils.BitmapUtil;

/**
 * @author lisen
 * @since 12-14-2018
 */

public class ClipView extends AppCompatImageView implements ScaleGestureDetector.OnScaleGestureListener {

    private static final String TAG = "ClipView";

    /**
     * 默认截图框占屏幕宽的比例
     */
    private static final float DEFAULT_CLIP_REGION_RATIO = 0.94f;

    /**
     * 默认阴影颜色
     */
    private static final int DEFAULT_SHADOW_COLOR = 0xb3000000;

    /**
     * 默认最大放大倍数
     */
    private static final int DEFAULT_MAX_SCALE_TIMES = 2;

    /**
     * 初始化状态常量
     */
    public static final int STATUS_INIT = 0;

    /**
     * 图片缩放状态常量
     */
    public static final int STATUS_ZOOM = 1;

    /**
     * 图片拖动状态常量
     */
    public static final int STATUS_MOVE = 2;

    /**
     * 当前状态
     */
    private int mCurrentStatus = STATUS_INIT;

    /**
     * down 位置
     */
    private float mStartRawX, mStartRawY;

    /**
     * 上次点击位置
     */
    float mPreRawX, mPreRawY;

    /**
     * 缩放监听
     */
    private final ScaleGestureDetector mScaleGestureDetector;

    /**
     * 上次缩放比例
     */
    private float mPreScaleFactor = 1;

    /**
     * 图片原始缩放比例
     */
    private float mBaseScale;

    /**
     * 最大放大倍数
     */
    private float mMaxScaleTimes = DEFAULT_MAX_SCALE_TIMES;

    /**
     * 最大放大比例 = 图片原始缩放比例 * 最大放大倍数
     */
    private float mMaxScale;

    /**
     * 最小缩放比例
     */
    private float mMinScale;

    /**
     * 当前缩放比例
     */
    private float mCurrentScale = 1;

    /**
     * 控件宽
     */
    private int mWidth;

    /**
     * 控件高
     */
    private int mHeight;

    /**
     * 图片
     */
    private Bitmap mBitmap;

    /**
     * 图片宽
     */
    private int mBitmapWidth;

    /**
     * 图片高
     */
    private int mBitmapHeight;

    /**
     * 截图区域长度
     */
    private float mClipWidth;

    /**
     * 选择框宽度占 view 宽度的百分比
     */
    private float mClipRegionRatio = DEFAULT_CLIP_REGION_RATIO;

    /**
     * 矩阵变换
     */
    private final Matrix mImageMatrix;

    /**
     * 截图框位置
     */
    private RectF mClipRect;

    /**
     * 图片位置
     */
    RectF rectF;

    /**
     * 遮罩画笔
     */
    Paint mPaint;

    /**
     * 阴影颜色
     */
    private int mShadowColor = DEFAULT_SHADOW_COLOR;

    /**
     * 画笔宽度
     */
    private float mStrokeWidth;

    /**
     * 圆圈半径
     */
    private float mRadius;

    /**
     * 使用圆形截图框，true 圆形， false 矩形
     */
    private boolean mUseCircleClipRegion = true;

    public ClipView(Context context) {
        this(context, null);
    }

    public ClipView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClipView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mImageMatrix = new Matrix();
        mClipRect = new RectF();
        rectF = new RectF();
        setScaleType(ScaleType.MATRIX);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mShadowColor);
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        super.layout(l, t, r, b);
        if (mWidth != r - l) {
            mWidth = r - l;
            mHeight = b - t;
            mClipWidth = Math.min(mWidth, mHeight) * mClipRegionRatio;

            mStrokeWidth = mWidth + mHeight;
            mRadius = (mClipWidth + mStrokeWidth) / 2;
            mPaint.setStrokeWidth(mStrokeWidth);

            mClipRect.left = (mWidth - mClipWidth) / 2;
            mClipRect.top = (mHeight - mClipWidth) / 2;
            mClipRect.right = mClipRect.left + mClipWidth;
            mClipRect.bottom = mClipRect.top + mClipWidth;

            if (mBitmap != null) {
                reset();
            }
        }
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        scale(detector);
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mPreScaleFactor = detector.getScaleFactor();
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mStartRawX = event.getRawX();
                mStartRawY = event.getRawY();
                mPreRawX = mStartRawX;
                mPreRawY = mStartRawY;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() == 1) {
                    mCurrentStatus = STATUS_MOVE;
                } else if (event.getPointerCount() == 2) {
                    mCurrentStatus = STATUS_ZOOM;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mCurrentStatus == STATUS_INIT && event.getPointerCount() == 1) {
                    mCurrentStatus = STATUS_MOVE;
                }
                if (mCurrentStatus == STATUS_MOVE && event.getPointerCount() == 1) {
                    translate(event);
                    mPreRawX = event.getRawX();
                    mPreRawY = event.getRawY();
                }
                break;
            case MotionEvent.ACTION_UP:
                mCurrentStatus = STATUS_INIT;
                break;
        }
        return mScaleGestureDetector.onTouchEvent(event);
    }

    /**
     * 位移
     *
     * @param event
     */
    private void translate(MotionEvent event) {
        float moveX = event.getRawX();
        float moveY = event.getRawY();
        // 获取手指移动的距离
        float dx = moveX - mPreRawX;
        float dy = moveY - mPreRawY;

        RectF rect = getMatrixRectF();
        if (dx > 0) {
            //右移，检查左边界
            float left = rect.left;
            if (left + dx > mClipRect.left) {
                dx = mClipRect.left - left;
            }
        } else {
            //左移，检查右边界
            float right = rect.right;
            if (right + dx < mClipRect.right) {
                dx = mClipRect.right - right;
            }
        }

        if (dy > 0) {
            //下移，检查上边界
            float top = rect.top;
            if (top + dy > mClipRect.top) {
                dy = mClipRect.top - top;
            }
        } else {
            //上移，检查下边界
            float bottom = rect.bottom;
            if (bottom + dy < mClipRect.bottom) {
                dy = mClipRect.bottom - bottom;
            }
        }
        mImageMatrix.postTranslate(dx, dy);
        setImageMatrix(mImageMatrix);
    }

    /**
     * 缩放
     *
     * @param detector
     */
    private void scale(ScaleGestureDetector detector) {
        float currentScaleFactor = detector.getScaleFactor();
        float scaleFactor = currentScaleFactor / mPreScaleFactor;
        mPreScaleFactor = currentScaleFactor;
        float willScale = mCurrentScale * scaleFactor;
        if (willScale < mMinScale) {
            scaleFactor = mMinScale / mCurrentScale;
        }
        if (willScale > mMaxScale) {
            scaleFactor = mMaxScale / mCurrentScale;
        }
        mImageMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());

        borderCheck();

        setImageMatrix(mImageMatrix);
        mCurrentScale = mCurrentScale * scaleFactor;
    }

    /**
     * 缩放边界检查
     */
    private void borderCheck() {

        float dx = 0;
        float dy = 0;

        RectF rect = getMatrixRectF();
        float left = rect.left;
        float right = rect.right;
        float top = rect.top;
        float bottom = rect.bottom;

        if (left > mClipRect.left) {
            dx = mClipRect.left - left;
        }
        if (right < mClipRect.right) {
            dx = mClipRect.right - right;
        }

        if (top > mClipRect.top) {
            dy = mClipRect.top - top;
        }

        if (bottom < mClipRect.bottom) {
            dy = mClipRect.bottom - bottom;
        }

        mImageMatrix.postTranslate(dx, dy);
    }

    /**
     * 获取图片缩放后的宽高
     *
     * @return
     */
    private RectF getMatrixRectF() {
        Matrix matrix = mImageMatrix;
        rectF.set(0, 0, mBitmapWidth, mBitmapHeight);
        matrix.mapRect(rectF);
        return rectF;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mClipWidth > 0) {
            if (mUseCircleClipRegion) {
                canvas.drawCircle(mWidth / 2, mHeight / 2, mRadius, mPaint);
            } else {
                float d = mStrokeWidth / 2;
                canvas.drawRect(mClipRect.left - d, mClipRect.top - d, mClipRect.right + d, mClipRect.bottom + d, mPaint);

            }
        }
    }

    /**
     * 设置图片
     *
     * @param bitmap
     */
    public void setBitmap(@NonNull Bitmap bitmap) {
        mBitmap = bitmap;
        mBitmapWidth = mBitmap.getWidth();
        mBitmapHeight = mBitmap.getHeight();
        setImageBitmap(bitmap);

        if (mWidth != 0) {
            reset();
        }
    }

    /**
     * 设置遮罩颜色
     *
     * @param color
     */
    public void setShadowColor(@ColorInt int color) {
        mShadowColor = color;
        mPaint.setColor(mShadowColor);
    }

    /**
     * 设置最大放大倍数
     *
     * @param maxScaleTimes
     */
    public void setMaxScaleTimes(float maxScaleTimes) {
        if (maxScaleTimes <= 0) {
            return;
        }
        mMaxScaleTimes = maxScaleTimes;
    }

    /**
     * 设置截图区域占屏宽百分比
     *
     * @param clipRegionRatio
     */
    public void setClipRegionRatio(float clipRegionRatio) {
        if (clipRegionRatio <= 0 || clipRegionRatio > 1) {
            return;
        }

        mClipRegionRatio = clipRegionRatio;
    }

    /**
     * 使用矩形截图框，默认是圆形
     */
    public void useRectClipRegion() {
        mUseCircleClipRegion = false;
    }

    /**
     * 获取选择框内的截图
     *
     * @return
     */
    public Bitmap clipBitmap() {
        RectF rect = getMatrixRectF();
        float left = mClipRect.left - rect.left;
        float top = mClipRect.top - rect.top;
        left = left / mCurrentScale;
        top = top / mCurrentScale;
        float width = mClipWidth / mCurrentScale;
        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(mBitmap, (int) left, (int) top, (int) width, (int) width);
        } catch (IllegalArgumentException e) {
        } catch (OutOfMemoryError e) {
        }
        return bitmap;
    }

    /**
     * 图片旋转
     */
    public void rotate() {
        Bitmap rotateBitmap = BitmapUtil.rotateBitmap(90, mBitmap);
        if (rotateBitmap == null) {
            Log.e(TAG, "rotate: error");
            return;
        }
        mBitmap = rotateBitmap;

        setBitmap(mBitmap);
    }

    /**
     * 重置所有配置
     */
    private void reset() {
        if (mBitmapWidth >= mBitmapHeight) {
            mBaseScale = 1f * mWidth / mBitmapWidth;
            if (mBitmapHeight * mBaseScale < mClipWidth) {
                mBaseScale = mClipWidth / mBitmapHeight;
            }
            mMinScale = mClipWidth / mBitmapHeight;
        } else {
            mBaseScale = 1f * mHeight / mBitmapHeight;
            if (mBitmapWidth * mBaseScale < mClipWidth) {
                mBaseScale = mClipWidth / mBitmapWidth;
            }
            mMinScale = mClipWidth / mBitmapWidth;
        }
        mMaxScale = mBaseScale * mMaxScaleTimes;
        mCurrentScale = mBaseScale;
        mImageMatrix.reset();
        mImageMatrix.postTranslate(mWidth / 2 - mBitmapWidth / 2, mHeight / 2 - mBitmapHeight / 2);
        mImageMatrix.postScale(mBaseScale, mBaseScale, mWidth / 2, mHeight / 2);
        setImageMatrix(mImageMatrix);
    }
}
