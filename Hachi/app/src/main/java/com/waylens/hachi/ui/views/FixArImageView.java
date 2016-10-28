package com.waylens.hachi.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.waylens.hachi.R;

/**
 * Created by Xiaofei on 2016/10/28.
 */

public class FixArImageView extends ImageView {
    private int mXRatio = 16;
    private int mYRatio = 9;

    public FixArImageView(Context context) {
        this(context, null);
    }

    public FixArImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FixArImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FixArImageView);
        mXRatio = a.getDimensionPixelSize(R.styleable.FixArImageView_fariv_xratio, mXRatio);
        mYRatio = a.getDimensionPixelSize(R.styleable.FixArImageView_fariv_yratio, mYRatio);
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width_pixel_size = MeasureSpec.getSize(widthMeasureSpec);
        int height_measure_mode = MeasureSpec.getMode(heightMeasureSpec);

        int height_pixel_size = width_pixel_size * mYRatio / mXRatio;
        int new_height_measure_spec = MeasureSpec.makeMeasureSpec(height_pixel_size,
            height_measure_mode);
        super.onMeasure(widthMeasureSpec, new_height_measure_spec);


    }
}
