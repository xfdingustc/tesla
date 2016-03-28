package com.waylens.hachi.ui.views.cliptrimmer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.ui.views.Progressive;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipFragment;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;

import java.util.ArrayList;
import java.util.List;

/**
 * VideoPlayerProgressBar
 * Created by Richard on 9/21/15.
 */
public class ClipSetProgressBar extends FrameLayout implements Progressive {
    private static final String TAG = ClipSetProgressBar.class.getSimpleName();
    public RecyclerView mRecyclerView;
    private MarkView mMarkView;
    private SelectView mSelectingView;

    private BookmarkView mBookmarkView;

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
    private Clip mSelectedClip;
    private ClipSet mClipSet;
    private ClipSet mBookmarkClipSet;

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

        mBookmarkView = new BookmarkView(getContext());
        layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(mBookmarkView, layoutParams);


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

                mBookmarkView.mOffset = getWidth() / 2 - getCurrentOffset();
//                Logger.t("FFFFF").d("Offset: " + getCurrentOffset());

                mBookmarkView.invalidate();
            }
        };

        mRecyclerView.addOnScrollListener(mScrollListener);

        mMarkView = new MarkView(getContext(), ViewUtils.dp2px(1, getResources()));
        layoutParams = new LayoutParams(ViewUtils.dp2px(21, getResources()), ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        addView(mMarkView, layoutParams);
    }


    public void setClipSet(ClipSet clipSet, VdbImageLoader imageLoader) {
        mClipSet = clipSet;
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

    public void setBookmarkClipSet(ClipSet clipSet) {
        this.mBookmarkClipSet = clipSet;
        mBookmarkView.invalidate();
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

    private int getOffsetByTime(long time) {
        //int clipIndex = mClipSet.findClipIndex(clip.cid);
        double offset = (1.0f * time) / mVideoLength * getLength();

        return (int) offset;

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


    @SuppressLint("ViewConstructor")
    private class BookmarkView extends View {
        private Paint mPaintMark;

        private int mOffset = getWidth() / 2;

        public BookmarkView(Context context) {
            super(context);
            mPaintMark = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaintMark.setColor(Color.RED);
            mPaintMark.setAlpha(125);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (mBookmarkClipSet == null) {
                return;
            }
            for (int i = 0; i < mBookmarkClipSet.getCount(); i++) {
                Clip clip = mBookmarkClipSet.getClip(i);
                //ClipPos startClipPos = mClipSet.findClipPosByAbsTime(clip.getStartTimeMs());
                //ClipPos endClipPos = mClipSet.findClipPosByAbsTime(clip.getEndTimeMs());

                Clip realClip = mClipSet.findClip(clip.realCid);

                Rect rect = new Rect(mOffset + getOffsetByTime(clip.getStartTimeMs()), 0,
                    mOffset + getOffsetByTime(clip.getEndTimeMs()), getHeight());

                canvas.drawRect(rect, mPaintMark);


                Logger.t("FFFFF").d("bookstartTime: " + clip.getStartTimeMs());
                //Logger.t("FFFFF").d("bookendTime: " + clip.getEndTimeMs());
            }
        }


        private void getBookmarkClipRe(Clip clip) {
            Clip realClip = mClipSet.findClip(clip.realCid);
//            Logger.t("FFFFF").d("realClip startTime: " + realClip.getStartTimeMs());
//            Logger.t("FFFFF").d("realClip endTime: " + realClip.getEndTimeMs());
        }
    }


    public class RecyclerListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_NORMAL = 0;
        private static final int TYPE_HEADER_TAIL = 1;

        private static final int DEFAULT_PERIOD_MS = 1000 * 10;

        VdbImageLoader mImageLoader;
        int mScreenWidth;
        private ClipSet mClipSet;
        int mItemWidth;
        int mItemHeight;
        //ArrayList<ClipPos> mItems = new ArrayList<>();
        private int mClipFragmentDruation = DEFAULT_PERIOD_MS;

        private class CellItem {
            static final int ITEM_TYPE_CLIP_FRAGMENT = 0;
            static final int ITEM_TYPE_DIVIDER = 1;
            static final int ITEM_TYPE_MARGIN = 2;
            int type;
            Object item;
        }

        List<CellItem> mItems = new ArrayList<>();

        public RecyclerListAdapter(VdbImageLoader imageLoader, ClipSet clipSet, int screenWidth, int itemWidth, int itemHeight) {
            mImageLoader = imageLoader;
            mScreenWidth = screenWidth;
            mClipSet = clipSet;
            mItemWidth = itemWidth;
            mItemHeight = itemHeight;
            generateClipPosList();
        }

        void generateClipPosList() {
            mItems.clear();
            List<Clip> clipList = mClipSet.getClipList();


            CellItem headerMarginItem = new CellItem();
            headerMarginItem.type = CellItem.ITEM_TYPE_MARGIN;
            mItems.add(headerMarginItem);

            for (int i = 0; i < clipList.size(); i++) {
                Clip clip = clipList.get(i);
                int itemCount = clip.getDurationMs() / mClipFragmentDruation;
                if (clip.getDurationMs() % mClipFragmentDruation != 0) {
                    itemCount++;
                }

                long endMs = clip.getStartTimeMs() + clip.getDurationMs();

                for (int j = 0; j < itemCount; j++) {
                    long startTime = clip.getStartTimeMs() + mClipFragmentDruation * j;
                    long posTime = clip.getStartTimeMs() + mClipFragmentDruation * (j + 1);
                    if (startTime >= endMs) {
                        break;
                    }

                    if (posTime >= endMs) {
                        posTime = endMs - 10; //magic number.
                    }

                    CellItem oneItem = new CellItem();
                    oneItem.type = CellItem.ITEM_TYPE_CLIP_FRAGMENT;
                    oneItem.item = new ClipFragment(clip, startTime, posTime);
                    mItems.add(oneItem);
                }

                if (i != (clipList.size() - 1)) {
                    CellItem dividerItem = new CellItem();
                    dividerItem.type = CellItem.ITEM_TYPE_DIVIDER;
                    mItems.add(dividerItem);
                } else {
                    CellItem tailItem = new CellItem();
                    tailItem.type = CellItem.ITEM_TYPE_MARGIN;
                    mItems.add(tailItem);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            CellItem cellItem = mItems.get(viewType);
            if (cellItem.type == CellItem.ITEM_TYPE_CLIP_FRAGMENT) {
                ClipFragment clipFragment = (ClipFragment)cellItem.item;
                ImageView imageView = new ImageView(parent.getContext());
                int imageViewWidth = mRecyclerView.getHeight() * 16 / 9;

                imageViewWidth = imageViewWidth * clipFragment.getDurationMs() / mClipFragmentDruation;
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(imageViewWidth, ViewGroup.LayoutParams.MATCH_PARENT);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setLayoutParams(params);

                return new ItemViewHolder(imageView);
            } else if (cellItem.type == CellItem.ITEM_TYPE_DIVIDER){
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                View view = inflater.inflate(R.layout.item_clip_fragment_divider, parent, false);
                return new DividerViewHolder(view);
            } else  {
                View view = new View(parent.getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mScreenWidth/2, ViewGroup.LayoutParams.MATCH_PARENT);
                view.setLayoutParams(params);
                return new MarginViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            CellItem clipFragmentItem = mItems.get(position);
            if (clipFragmentItem.type == CellItem.ITEM_TYPE_CLIP_FRAGMENT) {
                ItemViewHolder viewHolder = (ItemViewHolder)holder;
                ClipFragment clipFragment = (ClipFragment)clipFragmentItem.item;
                ClipPos clipPos = new ClipPos(clipFragment.getClip(), clipFragment.getStartTimeMs(), ClipPos.TYPE_POSTER, false);
                mImageLoader.displayVdbImage(clipPos, viewHolder.clipFragmentThumbnail, mItemWidth, mItemHeight);
            }

        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

//        @Bind(R.id.root)
//        FrameLayout rootView;

        //@Bind(R.id.clipFragmentThumbnail)
        ImageView clipFragmentThumbnail;

        public ItemViewHolder(View itemView) {
            super(itemView);
            //ButterKnife.bind(this, itemView);
            clipFragmentThumbnail = (ImageView)itemView;
        }

    }

    public static class DividerViewHolder extends RecyclerView.ViewHolder {

        public DividerViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class MarginViewHolder extends RecyclerView.ViewHolder {

        public MarginViewHolder(View itemView) {
            super(itemView);
        }
    }

    public interface OnSeekBarChangeListener {
        void onStartTrackingTouch(ClipSetProgressBar progressBar);

        void onProgressChanged(ClipSetProgressBar progressBar, long progress, boolean fromUser);

        void onStopTrackingTouch(ClipSetProgressBar progressBar);
    }

}
