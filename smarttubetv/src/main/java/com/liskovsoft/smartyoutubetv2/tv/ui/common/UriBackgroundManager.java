package com.liskovsoft.smartyoutubetv2.tv.ui.common;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.BackgroundManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.PlaybackActivity;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class UriBackgroundManager {
    private static final String TAG = UriBackgroundManager.class.getSimpleName();
    private static final int BACKGROUND_UPDATE_DELAY_MS = 300;
    /** Wait for the focus to settle so fast scrolling doesn't load every thumbnail. */
    private static final int BACKDROP_UPDATE_DELAY_MS = 450;
    private static final int BACKDROP_WIDTH = 640;
    private static final int BACKDROP_HEIGHT = 360;
    /** How much the default image shows over the theme background color. */
    private static final int AMBIENT_ALPHA = 190;
    private Uri mBackgroundURI;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Runnable mBackgroundTask;
    private BackgroundManager mBackgroundManager;
    private final Activity mActivity;
    private final Handler mHandler;
    private int mBackgroundColor = -1;
    private final boolean mBackdropEnabled;
    private final Runnable mBackdropTask = this::loadBackdrop;
    private String mPendingBackdropUrl;
    private String mBackdropUrl;
    private BackdropDrawable mBackdrop;
    private Drawable mAmbient;
    private boolean mIsAmbientShown;

    public UriBackgroundManager(Activity activity) {
        mActivity = activity;
        mHandler = new Handler();
        // The player draws the video behind its UI: no backdrops there
        mBackdropEnabled = !(activity instanceof PlaybackActivity);
        prepareBackgroundManager();
        showRestingBackground();
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(mActivity);
        mBackgroundManager.attach(mActivity.getWindow());
        mDefaultBackground = ContextCompat.getDrawable(mActivity, Helpers.getThemeAttr(mActivity, R.attr.shelfBackground));
        mBackgroundTask = new UpdateBackgroundTask();
        mMetrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void startBackgroundTimer(Uri backgroundURI) {
        mBackgroundURI = backgroundURI;
        mHandler.removeCallbacks(mBackgroundTask);
        mHandler.postDelayed(mBackgroundTask, BACKGROUND_UPDATE_DELAY_MS);
    }

    public void startBackgroundTimer(String bgImageUrl) {
        if (bgImageUrl != null) {
            startBackgroundTimer(Uri.parse(bgImageUrl));
        }
    }

    /**
     * Netflix-like backdrop with the thumbnail of the focused video (browse, channel, search...).
     */
    public void setBackgroundFrom(Video item) {
        if (!mBackdropEnabled || item == null || mBackgroundManager == null) {
            return;
        }

        // Settings > User interface: may be disabled on slow TV boxes
        if (!isBackdropAllowed()) {
            clearBackdrop();
            return;
        }

        String url = item.getCardImageUrl();

        if (url == null) {
            clearBackdrop(); // no thumbnail: default image
            return;
        }

        if (url.equals(mPendingBackdropUrl)) {
            return;
        }

        mPendingBackdropUrl = url;
        mHandler.removeCallbacks(mBackdropTask);

        if (url.equals(mBackdropUrl)) {
            return; // already shown
        }

        mHandler.postDelayed(mBackdropTask, BACKDROP_UPDATE_DELAY_MS);
    }

    /**
     * Back to the resting background, without video (sections like Settings, no thumbnail...).
     */
    public void clearBackdrop() {
        mHandler.removeCallbacks(mBackdropTask);
        mPendingBackdropUrl = null;

        // Also refresh when the lite mode was switched on/off in the meantime
        if (mBackdrop != null || mIsAmbientShown != isAmbientAllowed()) {
            mBackdrop = null;
            mBackdropUrl = null;

            if (mBackgroundManager != null) {
                showRestingBackground();
            }
        }
    }

    /**
     * Default SuperTube image (no video focused yet), or the plain theme color in the player and in lite mode.
     */
    private void showRestingBackground() {
        if (isAmbientAllowed()) {
            Drawable ambient = getAmbient();

            if (ambient != null) {
                mIsAmbientShown = true;
                mBackgroundManager.setDrawable(ambient);
                return;
            }
        }

        mIsAmbientShown = false;
        setDefaultBackground();
    }

    private Drawable getAmbient() {
        if (mAmbient == null) {
            try {
                Bitmap bitmap = BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.supertube_ambient);

                if (bitmap == null) {
                    return null;
                }

                BitmapDrawable image = new BitmapDrawable(mActivity.getResources(), bitmap);
                image.setAlpha(AMBIENT_ALPHA);
                image.setFilterBitmap(true);
                image.setDither(true);
                mAmbient = new LayerDrawable(new Drawable[] {new ColorDrawable(getBaseColor() | 0xFF000000), image});
            } catch (OutOfMemoryError e) {
                return null; // low memory TV: plain color
            }
        }

        return mAmbient;
    }

    private boolean isLiteMode() {
        return MainUIData.instance(mActivity).isLiteModeEnabled();
    }

    private boolean isAmbientAllowed() {
        return mBackdropEnabled && !isLiteMode();
    }

    private boolean isBackdropAllowed() {
        return isAmbientAllowed() && !MainUIData.instance(mActivity).isUiTweakEnabled(MainUIData.UI_TWEAK_DISABLE_VIDEO_BACKDROP);
    }

    private void loadBackdrop() {
        final String url = mPendingBackdropUrl;

        if (url == null || mBackgroundManager == null || mActivity.isFinishing()) {
            return;
        }

        Glide.with(mActivity)
                .asBitmap()
                .load(url)
                .apply(new RequestOptions().centerCrop())
                .into(new SimpleTarget<Bitmap>(BACKDROP_WIDTH, BACKDROP_HEIGHT) {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                        if (mBackgroundManager == null || !url.equals(mPendingBackdropUrl)) {
                            return; // focus moved on or the activity is gone
                        }

                        // Own copy: Glide may recycle its bitmap while the background still draws it
                        Bitmap bitmap = resource.copy(resource.getConfig() != null ? resource.getConfig() : Bitmap.Config.ARGB_8888, false);

                        if (bitmap == null) {
                            return;
                        }

                        mBackdropUrl = url;
                        mBackdrop = new BackdropDrawable(bitmap, getBaseColor());
                        mBackgroundManager.setDrawable(mBackdrop);
                    }
                });
    }

    private int getBaseColor() {
        return mDefaultBackground instanceof ColorDrawable ? ((ColorDrawable) mDefaultBackground).getColor() : Color.BLACK;
    }

    public void onStart() {
        if (mBackdrop != null) {
            mBackgroundManager.setDrawable(mBackdrop);
        } else if (mBackgroundURI != null) {
            showBackground(mBackgroundURI.toString());
        } else if (mBackgroundColor != -1) {
            setBackgroundColor(mBackgroundColor);
        } else {
            showRestingBackground();
        }
    }

    public void onStop() {
        mBackgroundManager.release();
    }

    public void onDestroy() {
        mHandler.removeCallbacks(mBackgroundTask);
        mHandler.removeCallbacks(mBackdropTask);
        mBackdrop = null;
        mAmbient = null;
        mBackgroundManager = null;
    }

    public void removeBackground() {
        mBackgroundManager.setDrawable(null);
    }

    public void setDefaultBackground() {
        mBackgroundManager.setDrawable(mDefaultBackground);
    }

    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
        mBackgroundManager.setColor(color);
    }

    private class UpdateBackgroundTask implements Runnable {
        @Override
        public void run() {
            if (mBackgroundURI != null) {
                showBackground(mBackgroundURI.toString());
            }
        }
    }

    public BackgroundManager getBackgroundManager() {
        return mBackgroundManager;
    }

    public void showBackground(String uri) {
        View videoView = mActivity.findViewById(R.id.video_surface);

        if (videoView != null) {
            videoView.setVisibility(uri == null ? View.VISIBLE : View.INVISIBLE);
        }

        if (uri == null) {
            removeBackground();
            return;
        }

        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;

        RequestOptions options = ViewUtil.glideOptions()
                .centerCrop()
                .error(mDefaultBackground);

        Glide.with(mActivity)
                .asBitmap()
                .load(uri)
                .apply(options)
                .into(new SimpleTarget<Bitmap>(width, height) {
                    @Override
                    public void onResourceReady(
                            @NonNull Bitmap resource,
                            Transition<? super Bitmap> transition) {
                        mBackgroundManager.setBitmap(resource);
                    }
                });
    }

    public void showBackgroundColor(int colorResId) {
        View videoView = mActivity.findViewById(R.id.video_surface);

        if (videoView != null) {
            videoView.setVisibility(colorResId == -1 ? View.VISIBLE : View.INVISIBLE);
        }

        if (colorResId == -1) {
            removeBackground();
            return;
        }

        if (mBackgroundManager != null) {
            mBackgroundManager.setColor(ContextCompat.getColor(mActivity, colorResId));
        }
    }
}
