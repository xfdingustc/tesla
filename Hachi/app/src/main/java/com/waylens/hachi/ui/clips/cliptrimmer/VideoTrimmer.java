package com.waylens.hachi.ui.clips.cliptrimmer;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;

import java.util.ArrayList;
import java.util.List;


public class VideoTrimmer extends FrameLayout {
    private static final String TAG = VideoTrimmer.class.getSimpleName();

    public enum DraggingFlag {UNKNOWN, PROGRESS, LEFT, RIGHT}

    public static final int DEFAULT_THUMB_WIDTH_DP = 16;
    public static final int DEFAULT_PROGRESS_BAR_WIDTH_DP = 8;
    public static final int DEFAULT_BORDER_WIDTH_DP = 2;

    private RecyclerView mRecyclerView;
    private VideoTrimmerController mVideoTrimmerController;
    private int mThumbWidth;
    private int mBorderWidth;
    private int mProgressBarWidth;

    boolean mIsEditing;

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
            mThumbWidth = ViewUtils.dp2px(DEFAULT_THUMB_WIDTH_DP);
            mBorderWidth = ViewUtils.dp2px(DEFAULT_BORDER_WIDTH_DP);
            mProgressBarWidth = ViewUtils.dp2px(DEFAULT_PROGRESS_BAR_WIDTH_DP);
        } else {
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.VideoTrimmer, defStyle, 0);
            mThumbWidth = a.getDimensionPixelSize(R.styleable.VideoTrimmer_thumbWidth, ViewUtils.dp2px(DEFAULT_THUMB_WIDTH_DP));
            mBorderWidth = a.getDimensionPixelSize(R.styleable.VideoTrimmer_boardWidth, ViewUtils.dp2px(DEFAULT_BORDER_WIDTH_DP));
            mProgressBarWidth = a.getDimensionPixelSize(R.styleable.VideoTrimmer_progressBarWidth, ViewUtils.dp2px(DEFAULT_PROGRESS_BAR_WIDTH_DP));
            a.recycle();
        }
        initChildren();
    }

    void initChildren() {
        mRecyclerView = new RecyclerView(getContext());
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        //layoutParams.setMargins(mThumbWidth, 0, mThumbWidth, 0);
        layoutParams.gravity = Gravity.BOTTOM;
        addView(mRecyclerView, layoutParams);
        mRecyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new TrimmerLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(layoutManager);
        mVideoTrimmerController = new VideoTrimmerController(getContext(), mThumbWidth, mBorderWidth, mProgressBarWidth);
        layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(mVideoTrimmerController, layoutParams);
        showController();
    }

    private void showController() {
        if (mIsEditing) {
            mVideoTrimmerController.setVisibility(VISIBLE);
        } else {
            mVideoTrimmerController.setVisibility(INVISIBLE);
        }
    }

    public void setEditing(boolean isEditing) {
        mIsEditing = isEditing;
        showController();
    }

    public void setBackgroundClip(VdbImageLoader imageLoader, Clip clip, int defaultHeight) {
        setBackgroundClip(imageLoader, clip, clip.getStartTimeMs(),
            clip.getStartTimeMs() + clip.getDurationMs(), defaultHeight);
    }

    public void setBackgroundClip(VdbImageLoader imageLoader, Clip clip, long startMs, long endMs,
                                  int defaultHeight) {
        if (clip == null) {
            return;
        }

        int width = mRecyclerView.getWidth();
        int height = mRecyclerView.getHeight();

        if (width == 0) {
            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            if (size.x == 0) {
                return;
            }
            width = size.x;
        }

        if (height == 0) {
            height = defaultHeight;
        }

        int imgWidth = height * 16 / 9;
        int itemCount = width / imgWidth;
        if (width % imgWidth != 0) {
            itemCount++;
        }
        if (itemCount < 2) {
            Logger.t(TAG).d("itemCount is less than 2.");
            return;
        }
        long period = (endMs - startMs) / itemCount;
        List<ClipPos> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            long posTime = startMs + period * i;
            if (posTime >= endMs) {
                posTime = endMs - 10; //magic number.
            }
            items.add(new ClipPos(clip, posTime, ClipPos.TYPE_POSTER, false));
        }
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(imageLoader, items, imgWidth, height);
        mRecyclerView.setAdapter(adapter);
        ((LinearLayoutManager) mRecyclerView.getLayoutManager()).scrollToPositionWithOffset(0, -imgWidth / 2);
    }

    public void setOnChangeListener(OnTrimmerChangeListener listener) {
        mVideoTrimmerController.setOnChangeListener(listener, this);
    }

    public void setProgress(long progress) {
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

    public double getMinValue() {
        return mVideoTrimmerController.getMinValue();
    }

    public double getMaxValue() {
        return mVideoTrimmerController.getMaxValue();
    }

    public double getRange() {
        return mVideoTrimmerController.getRange();
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


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return !mIsEditing || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!mIsEditing) {
            final int action = MotionEventCompat.getActionMasked(e);
            if (action == MotionEvent.ACTION_UP) {
                Logger.t(TAG).d("Click VideoTrimmer");
                performClick();
                return true;
            }
        }
        return super.onTouchEvent(e);
    }

    public DraggingFlag getDraggingFlag() {
        return mVideoTrimmerController.getFlag();
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

        @Override
        public boolean canScrollHorizontally() {
            return false;
        }
    }

    public static class RecyclerViewAdapter extends RecyclerView.Adapter<ItemViewHolder> {
        List<ClipPos> mItems;
        VdbImageLoader mImageLoader;
        int mItemWidth;
        int mItemHeight;

        public RecyclerViewAdapter(VdbImageLoader imageLoader, List<ClipPos> items, int itemWidth, int itemHeight) {
            mImageLoader = imageLoader;
            mItems = items;
            mItemWidth = itemWidth;
            mItemHeight = itemHeight;
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ImageView view = new ImageView(parent.getContext());
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(mItemWidth, mItemHeight);
            view.setLayoutParams(params);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, int position) {
            mImageLoader.displayVdbImage(mItems.get(position), holder.imageView, mItemWidth, mItemHeight);
        }

        @Override
        public int getItemCount() {
            if (mItems == null) {
                return 0;
            }
            return mItems.size();
        }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        public final ImageView imageView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
        }

    }
}
