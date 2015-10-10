package com.waylens.hachi.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.waylens.hachi.R;

/**
 * Created by Richard on 9/28/15.
 */
public class VideoTrimmer extends FrameLayout {

    public enum DraggingFlag {UNKNOWN, PROGRESS, LEFT, RIGHT}

    public static final int DEFAULT_THUMB_WIDTH_DP = 16;
    public static final int DEFAULT_PROGRESS_BAR_WIDTH_DP = 8;
    public static final int DEFAULT_BORDER_WIDTH_DP = 2;

    private RecyclerView mRecyclerView;
    private VideoTrimmerController mVideoTrimmerController;

    private int mThumbWidth;
    private int mBorderWidth;
    private int mProgressBarWidth;

    public VideoTrimmer(Context context) {
        super(context);
        initAttributes(context, null, 0);

    }

    public VideoTrimmer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes(context, attrs, 0);
    }

    public VideoTrimmer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttributes(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public VideoTrimmer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttributes(context, attrs, defStyleRes);
    }

    void initAttributes(Context context, AttributeSet attrs, final int defStyle) {
        Resources resources = getResources();
        if (attrs == null) {
            mThumbWidth = ViewUtils.dp2px(DEFAULT_THUMB_WIDTH_DP, resources);
            mBorderWidth = ViewUtils.dp2px(DEFAULT_BORDER_WIDTH_DP, resources);
            mProgressBarWidth = ViewUtils.dp2px(DEFAULT_PROGRESS_BAR_WIDTH_DP, resources);
        } else {
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.VideoTrimmer, defStyle, 0);
            mThumbWidth = a.getDimensionPixelSize(R.styleable.VideoTrimmer_thumbWidth, ViewUtils.dp2px(DEFAULT_THUMB_WIDTH_DP, resources));
            mBorderWidth = a.getDimensionPixelSize(R.styleable.VideoTrimmer_boardWidth, ViewUtils.dp2px(DEFAULT_BORDER_WIDTH_DP, resources));
            mProgressBarWidth = a.getDimensionPixelSize(R.styleable.VideoTrimmer_progressBarWidth, ViewUtils.dp2px(DEFAULT_PROGRESS_BAR_WIDTH_DP, resources));
            a.recycle();
        }
        initChildren();
    }

    void initChildren() {
        mRecyclerView = new RecyclerView(getContext());
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(mThumbWidth, 0, mThumbWidth, 0);
        layoutParams.gravity = Gravity.BOTTOM;
        addView(mRecyclerView, layoutParams);
        mRecyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new TrimmerLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(layoutManager);
        VideoTrimmerController.RecyclerViewAdapter adapter = new VideoTrimmerController.RecyclerViewAdapter();
        mRecyclerView.setAdapter(adapter);

        mVideoTrimmerController = new VideoTrimmerController(getContext(), mThumbWidth, mBorderWidth, mProgressBarWidth);
        layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(mVideoTrimmerController, layoutParams);
    }

    public void test() {
        Log.e("test", "mRecyclerView: " + mRecyclerView.getWidth());
        Log.e("test", "width: " + (mVideoTrimmerController.getWidth() - mVideoTrimmerController.mThumbWidth * 2));
    }

    public void setOnChangeListener(OnTrimmerChangeListener listener) {
        mVideoTrimmerController.setOnChangeListener(listener, this);
    }

    public void setProgress(int progress) {
        mVideoTrimmerController.setProgress(progress);
    }

    public void setLeftValue(long value) {
        mVideoTrimmerController.setLeftValue(value);
    }

    public void setRightValue(long value) {
        mVideoTrimmerController.setRightValue(value);
    }

    public void setInitRangeValues(long left, long right) {
        mVideoTrimmerController.setInitRangeValues(left, right);
    }

    public long getLeftValue() {
        return mVideoTrimmerController.mStart;
    }

    public long getRightValue() {
        return mVideoTrimmerController.mEnd;
    }

    public long getProgress() {
        return mVideoTrimmerController.mProgress;
    }

    public interface OnTrimmerChangeListener {
        void onStartTrackingTouch(VideoTrimmer trimmer, DraggingFlag flag);

        void onProgressChanged(VideoTrimmer trimmer, DraggingFlag flag, long start, long end, long progress);

        void onStopTrackingTouch(VideoTrimmer trimmer);
    }

    static class TrimmerLayoutManager extends LinearLayoutManager {

        public TrimmerLayoutManager(Context context) {
            super(context);
        }

        public TrimmerLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        public TrimmerLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        @Override
        public boolean canScrollHorizontally() {
            return false;
        }
    }
}
