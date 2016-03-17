package com.waylens.hachi.ui.views.cliptrimmer;

import android.content.Context;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.ui.views.Progressive;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;

import java.util.ArrayList;

/**
 * VideoPlayerProgressBar
 * Created by Richard on 9/21/15.
 */
public class ClipSetProgressBar extends FrameLayout implements Progressive {

    public RecyclerView mRecyclerView;
    private MarkView mMarkView;
    private SelectView mSelectingView;


    private LinearLayoutManager mLayoutManager;

    private RecyclerView.OnScrollListener mScrollListener;

    private OnSeekBarChangeListener mOnSeekBarChangeListener;

    private int mScreenWidth;

    private volatile int mScrollState = RecyclerView.SCROLL_STATE_IDLE;

    private long mVideoLength;
    private MediaPlayer mPlayer;
    private boolean mDragging;
    private ProgressHandler mHandler;
    private long mMinValue;
    private long mMaxValue;
    private boolean mIsSelectMode;

    private int mStartSelectPosition;
    private int mEndSelectPosition;
    private Clip mSelectedClip ;

    public ClipSetProgressBar(Context context) {
        super(context);
        initChildren();
    }

    public ClipSetProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initChildren();
    }

    public ClipSetProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initChildren();
    }

//    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//    public ClipSetProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//        initChildren();
//    }

    private void initChildren() {
        mScreenWidth = getScreenWidth();

        mRecyclerView = new RecyclerView(getContext());
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.BOTTOM;
        addView(mRecyclerView, layoutParams);

        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(mLayoutManager);


        mScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    mDragging = true;
                    if (mOnSeekBarChangeListener != null) {
                        mOnSeekBarChangeListener.onStartTrackingTouch(ClipSetProgressBar.this);
                    }

                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mDragging = false;
                    if (mPlayer != null) {
                        updateProgress();
                    }
                    if (mOnSeekBarChangeListener != null) {
                        mOnSeekBarChangeListener.onStopTrackingTouch(ClipSetProgressBar.this);
                    }
                }

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int cellWidth = getCellUnit();
                int length = getLength();
                if (cellWidth == 0
                    || length == 0
                    || mVideoLength == 0
                    || mOnSeekBarChangeListener == null) {
                    return;
                }

                long progress = getCurrentTime();
                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener.onProgressChanged(ClipSetProgressBar.this, progress, true);
                }

                if (mIsSelectMode) {
                    mEndSelectPosition = getCurrentOffset();
                    mSelectingView.setWidth(mEndSelectPosition - mStartSelectPosition);
                }
            }
        };

        mRecyclerView.addOnScrollListener(mScrollListener);

        mMarkView = new MarkView(getContext(), ViewUtils.dp2px(1, getResources()));
        layoutParams = new LayoutParams(ViewUtils.dp2px(21, getResources()), ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        addView(mMarkView, layoutParams);
    }




    public void setClipSet(ClipSet clipSet, VdbImageLoader imageLoader) {
        mVideoLength = clipSet.getTotalLengthMs();
        int itemHeight = mRecyclerView.getHeight();
        if (mRecyclerView.getHeight() == 0) {
            itemHeight = ViewUtils.dp2px(64, getResources());
        }
        int itemWidth = (int) (itemHeight * 16.0f / 9);

        RecyclerListAdapter adapter = new RecyclerListAdapter(imageLoader, clipSet, mScreenWidth,
            itemWidth, itemHeight);
        mRecyclerView.setAdapter(adapter);
    }

    public void toggleSelectMode(boolean isSelectMode) {
        this.mIsSelectMode = isSelectMode;
        if (mIsSelectMode == true) {

            // add SelectView;
            mSelectingView = new SelectView(getContext());
            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
            addView(mSelectingView, layoutParams);
            mStartSelectPosition = mEndSelectPosition = getCurrentOffset();
        } else {
            removeView(mSelectingView);
        }
    }

    public int getCurrentOffset() {
        int cellWidth = getCellUnit();
        int centerPos = mScreenWidth / 2;
        View view = mRecyclerView.findChildViewUnder(centerPos, 0);
        int position = mLayoutManager.getPosition(view);
        int offset = (position - 1) * cellWidth + (centerPos - view.getLeft());
        return offset;
    }

    private long getTimeByOffset(int offset) {
        return offset * mVideoLength / getLength();
    }

    private long getCurrentTime() {
        return getTimeByOffset(getCurrentOffset());
    }

    public long getSelectStartTimeMs() {
        return getTimeByOffset(mStartSelectPosition);
    }

    public long getSelectEndTimeMs() {
        return getTimeByOffset(mEndSelectPosition);
    }



    int getScreenWidth() {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    int getLength() {
        View view = mRecyclerView.getChildAt(1);
        return view.getWidth() * (mRecyclerView.getAdapter().getItemCount() - 2);
    }

    int getCellUnit() {
        View view = mRecyclerView.getChildAt(1);
        return view.getWidth();
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        mOnSeekBarChangeListener = listener;
    }

    public void setMediaPlayer(MediaPlayer player) {
        if (mHandler != null) {
            mHandler.stop();
        }

        if (player != null) {
            mHandler = new ProgressHandler(this);
            mHandler.start();
        }
        mPlayer = player;
    }

    @Override
    public int updateProgress() {
        int timeOffset;
        try {
            timeOffset = mPlayer.getCurrentPosition();
        } catch (IllegalStateException e) {
            Log.e("test", "", e);
            return 0;
        }
        double offset = (1.0f * timeOffset) / mVideoLength * getLength();

        int cellWidth = getCellUnit();
        if (cellWidth == 0) {
            return (int) offset;
        }
        int position = (int) offset / cellWidth;
        int remainder = (int) offset % cellWidth;
        mLayoutManager.scrollToPositionWithOffset(position + 1, mScreenWidth / 2 - remainder);
        return (int) offset;
    }

    public int setProgress(int currentPosition, int duration) {
        double offset = (1.0f * currentPosition - mMinValue) / duration * getLength();
        int cellWidth = getCellUnit();
        if (cellWidth == 0) {
            return (int) offset;
        }
        int position = (int) offset / cellWidth;
        int remainder = (int) offset % cellWidth;
        mLayoutManager.scrollToPositionWithOffset(position + 1, mScreenWidth / 2 - remainder);
        return (int) offset;
    }

    @Override
    public boolean isInProgress() {
        try {
            return !mDragging && mPlayer != null && mPlayer.isPlaying();
        } catch (Exception e) {
            Log.e("test", "", e);
            return false;
        }
    }

    public void setInitRangeValues(long minClipStartTimeMs, long maxClipEndTimeMs) {
        mMinValue = minClipStartTimeMs;
        mMaxValue = maxClipEndTimeMs;
    }





    public static class RecyclerListAdapter extends RecyclerView.Adapter<ItemViewHolder> {
        private static final int TYPE_NORMAL = 0;
        private static final int TYPE_HEADER_TAIL = 1;

        private static final int DEFAULT_PERIOD_MS = 1000 * 10;

        VdbImageLoader mImageLoader;
        int mScreenWidth;
        private ClipSet mClipSet;
        int mItemWidth;
        int mItemHeight;
        ArrayList<ClipPos> mItems = new ArrayList<>();

        public RecyclerListAdapter(VdbImageLoader imageLoader, ClipSet clipSet, int
            screenWidth, int itemWidth, int itemHeight) {
            mImageLoader = imageLoader;
            mScreenWidth = screenWidth;
            mClipSet = clipSet;
            mItemWidth = itemWidth;
            mItemHeight = itemHeight;
            generateClipPosList();
        }

        void generateClipPosList() {
            mItems.clear();
            for (Clip clip : mClipSet.getClipList()) {
                int itemCount = clip.getDurationMs() / DEFAULT_PERIOD_MS;
                if (clip.getDurationMs() % DEFAULT_PERIOD_MS != 0) {
                    itemCount++;
                }

                long endMs = clip.getStartTimeMs() + clip.getDurationMs();

                for (int i = 0; i < itemCount; i++) {
                    long posTime = clip.getStartTimeMs() + DEFAULT_PERIOD_MS * i;
                    if (posTime >= endMs) {
                        posTime = endMs - 10; //magic number.
                    }
                    mItems.add(new ClipPos(clip, posTime, ClipPos.TYPE_POSTER, false));
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0 || position == (mItems.size() + 1)) {
                return TYPE_HEADER_TAIL;
            } else {
                return TYPE_NORMAL;
            }
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == TYPE_NORMAL) {
                view = new ImageView(parent.getContext());
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(mItemWidth, mItemHeight);
                view.setLayoutParams(params);
            } else {
                view = new ImageView(parent.getContext());
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(mScreenWidth / 2, ViewGroup.LayoutParams.MATCH_PARENT);
                view.setLayoutParams(params);
            }
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, int position) {
            if (getItemViewType(position) == TYPE_NORMAL) {
                mImageLoader.displayVdbImage(mItems.get(position - 1), holder.imageView, mItemWidth, mItemHeight);
            }
        }

        @Override
        public int getItemCount() {
            return mItems.size() + 2;
        }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        public final ImageView imageView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
        }

    }

    public interface OnSeekBarChangeListener {
        void onStartTrackingTouch(ClipSetProgressBar progressBar);

        void onProgressChanged(ClipSetProgressBar progressBar, long progress, boolean fromUser);

        void onStopTrackingTouch(ClipSetProgressBar progressBar);
    }

}
