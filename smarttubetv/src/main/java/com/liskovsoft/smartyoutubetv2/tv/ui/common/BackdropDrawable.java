package com.liskovsoft.smartyoutubetv2.tv.ui.common;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

/**
 * Netflix-like backdrop: the focused video's thumbnail in the top-right corner, dimmed and faded
 * into the theme background on the left (where the headers and titles are) and at the bottom
 * (where the rows are), so the image adds atmosphere without hurting readability.
 */
public class BackdropDrawable extends Drawable {
    /** Part of the screen width covered by the image (anchored to the right edge). */
    private static final float IMAGE_WIDTH_RATIO = 0.72f;
    /** How much of the image shows through over the background color (lower = darker). */
    private static final int IMAGE_ALPHA = 85;
    /** Left part of the image that fades from the background color to transparent. */
    private static final float LEFT_FADE_RATIO = 0.6f;
    /** Bottom part of the image that fades from transparent to the background color. */
    private static final float BOTTOM_FADE_RATIO = 0.55f;
    /** Top band darkened for the title bar (search, clock, account). */
    private static final float TOP_FADE_RATIO = 0.3f;

    private final Bitmap mBitmap;
    private final int mBaseColor;
    private final Paint mBasePaint = new Paint();
    private final Paint mImagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private final Paint mLeftFadePaint = new Paint(Paint.DITHER_FLAG);
    private final Paint mBottomFadePaint = new Paint(Paint.DITHER_FLAG);
    private final Paint mTopFadePaint = new Paint(Paint.DITHER_FLAG);
    private final Rect mSrcRect = new Rect();
    private final RectF mImageRect = new RectF();
    private final RectF mLeftFadeRect = new RectF();
    private final RectF mBottomFadeRect = new RectF();
    private final RectF mTopFadeRect = new RectF();
    private int mAlpha = 255;

    public BackdropDrawable(@NonNull Bitmap bitmap, int baseColor) {
        mBitmap = bitmap;
        mBaseColor = baseColor | 0xFF000000; // the gradients need an opaque base
        mBasePaint.setColor(mBaseColor);
        mSrcRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
        applyAlpha();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        float width = bounds.width();
        float imageWidth = width * IMAGE_WIDTH_RATIO;
        float imageHeight = imageWidth * mBitmap.getHeight() / mBitmap.getWidth();
        float left = bounds.right - imageWidth;
        float top = bounds.top;

        mImageRect.set(left, top, bounds.right, top + imageHeight);

        mLeftFadeRect.set(left - 1, top, left + imageWidth * LEFT_FADE_RATIO, mImageRect.bottom);
        mLeftFadePaint.setShader(new LinearGradient(mLeftFadeRect.left, 0, mLeftFadeRect.right, 0,
                new int[] {mBaseColor, withAlpha(mBaseColor, 0x99), Color.TRANSPARENT},
                new float[] {0f, 0.4f, 1f}, Shader.TileMode.CLAMP));

        mBottomFadeRect.set(left - 1, mImageRect.bottom - imageHeight * BOTTOM_FADE_RATIO, bounds.right, mImageRect.bottom + 1);
        mBottomFadePaint.setShader(new LinearGradient(0, mBottomFadeRect.top, 0, mBottomFadeRect.bottom,
                new int[] {Color.TRANSPARENT, withAlpha(mBaseColor, 0xAA), mBaseColor},
                new float[] {0f, 0.6f, 1f}, Shader.TileMode.CLAMP));

        mTopFadeRect.set(left - 1, top, bounds.right, top + bounds.height() * TOP_FADE_RATIO);
        mTopFadePaint.setShader(new LinearGradient(0, mTopFadeRect.top, 0, mTopFadeRect.bottom,
                withAlpha(mBaseColor, 0xBB), Color.TRANSPARENT, Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mBitmap.isRecycled()) {
            canvas.drawRect(getBounds(), mBasePaint);
            return;
        }

        canvas.drawRect(getBounds(), mBasePaint);
        canvas.drawBitmap(mBitmap, mSrcRect, mImageRect, mImagePaint);
        canvas.drawRect(mTopFadeRect, mTopFadePaint);
        canvas.drawRect(mBottomFadeRect, mBottomFadePaint);
        canvas.drawRect(mLeftFadeRect, mLeftFadePaint);
    }

    @Override
    public void setAlpha(int alpha) {
        if (mAlpha != alpha) {
            mAlpha = alpha;
            applyAlpha();
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        return mAlpha;
    }

    private void applyAlpha() {
        mBasePaint.setAlpha(mAlpha);
        mImagePaint.setAlpha(IMAGE_ALPHA * mAlpha / 255);
        mLeftFadePaint.setAlpha(mAlpha);
        mBottomFadePaint.setAlpha(mAlpha);
        mTopFadePaint.setAlpha(mAlpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mImagePaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return mAlpha == 255 ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT;
    }

    /**
     * Leanback's BackgroundManager copies the current background into the next activity (e.g. when a video is opened)
     * through the constant state: without it the new activity crashes.
     */
    @Override
    public ConstantState getConstantState() {
        return new ConstantState() {
            @NonNull
            @Override
            public Drawable newDrawable() {
                return new BackdropDrawable(mBitmap, mBaseColor);
            }

            @Override
            public int getChangingConfigurations() {
                return 0;
            }
        };
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
