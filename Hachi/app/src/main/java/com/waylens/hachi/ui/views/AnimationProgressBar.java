package com.waylens.hachi.ui.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.waylens.hachi.R;

/**
 * Created by Xiaofei on 2016/8/25.
 */
public class AnimationProgressBar extends FrameLayout {
    private static final String TAG = AnimationProgressBar.class.getSimpleName();

    private ProgressBar mProgressBar;

    private ImageView mIndicator;

    TranslateAnimation animation;

    public AnimationProgressBar(Context context) {
        this(context, null);
    }

    public AnimationProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnimationProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AnimationProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        View.inflate(context, R.layout.animation_progress_view, this);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mIndicator = (ImageView) findViewById(R.id.indicator);

    }


    public void setProgress(int progress) {
        mProgressBar.setProgress(progress);
        if (mIndicator.getAnimation() != null) {
            restartAnimation();
        }
    }

    public void setSecondaryProgress(int secondProgress) {
        mProgressBar.setSecondaryProgress(secondProgress);
        if (mIndicator.getAnimation() != null) {
            restartAnimation();
        }
    }

    public int getProgress() {
        return mProgressBar.getProgress();
    }

    public Drawable getProgressDrawable() {
        return mProgressBar.getProgressDrawable();
    }

    public int getSecondProgress() {
        return mProgressBar.getSecondaryProgress();
    }

    public void setMax(int max) {
        mProgressBar.setMax(max);
    }




    public void showIndicator(boolean show) {
        if (show) {

            mIndicator.setVisibility(VISIBLE);
            int firstProgress = mProgressBar.getWidth() * mProgressBar.getProgress() / mProgressBar.getMax() - mIndicator.getWidth();
            int secondProgress = mProgressBar.getWidth() * mProgressBar.getSecondaryProgress() / mProgressBar.getMax() - mIndicator.getWidth();
            Log.d(TAG, "first: " + firstProgress + " second: " + secondProgress + " indicator: " + mIndicator.getWidth());
            animation = new TranslateAnimation(firstProgress, secondProgress, 0, 0);
            animation.setDuration(2000);
            animation.setRepeatCount(-1);
            animation.setRepeatMode(Animation.INFINITE);


            mIndicator.startAnimation(animation);
        } else {
            mIndicator.clearAnimation();
            mIndicator.setVisibility(INVISIBLE);
        }
    }


    public boolean isAnimating() {
        return mIndicator.getAnimation() != null;
    }


    private void restartAnimation() {
        showIndicator(false);
        showIndicator(true);
    }
}
