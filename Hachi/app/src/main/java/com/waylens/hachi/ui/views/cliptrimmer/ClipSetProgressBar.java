package com.waylens.hachi.ui.views.cliptrimmer;

import android.content.Context;
import android.graphics.Point;
import android.support.design.widget.BottomSheetDialog;
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

import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.ClipSelectEvent;
import com.waylens.hachi.eventbus.events.ClipSetPosChangeEvent;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.ui.views.Progressive;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipFragment;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetPos;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;


/**
 * VideoPlayerProgressBar
 * Created by Richard on 9/21/15.
 */
public class ClipSetProgressBar extends FrameLayout  {
    private static final String TAG = ClipSetProgressBar.class.getSimpleName();
    private RecyclerView mRecyclerView;
    private ThumbnailListAdapter mAdapter;


    private LinearLayoutManager mLayoutManager;

    private RecyclerView.OnScrollListener mScrollListener;
    private OnBookmarkClickListener mBookmarkClickListener;


    private int mScreenWidth;
    private VdbImageLoader mVdbImageLoader;

    private EventBus mEventBus = EventBus.getDefault();

    private volatile int mScrollState = RecyclerView.SCROLL_STATE_IDLE;


    private ClipSet mClipSet;
    private ClipSet mBookmarkClipSet;

    public ClipSetProgressBar(Context context) {
        super(context);

    }

    public ClipSetProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public ClipSetProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    public interface OnBookmarkClickListener {
        void onBookmarkClick(Clip clip);
    }


    public void init(VdbImageLoader vdbImageLoader, OnBookmarkClickListener listener) {
        mEventBus.register(this);
        mBookmarkClickListener = listener;
        mScreenWidth = getScreenWidth();

        mRecyclerView = new RecyclerView(getContext());
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        addView(mRecyclerView, layoutParams);

        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setItemViewCacheSize(4);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mVdbImageLoader = vdbImageLoader;
        mAdapter = new ThumbnailListAdapter();
        mRecyclerView.setAdapter(mAdapter);

        mScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {


                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {


                }

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int cellWidth = getCellUnit();
                int length = getLength();
                if (cellWidth == 0 || length == 0) {
                    return;
                }

                ClipSetPos clipSetPos = getCurrentClipSetPos();
                if (clipSetPos != null) {
                    ClipSetPosChangeEvent event = new ClipSetPosChangeEvent(clipSetPos, TAG);
                    mEventBus.post(event);
                }


            }
        };

        mRecyclerView.addOnScrollListener(mScrollListener);

    }


    public void setClipSet(ClipSet clipSet, ClipSet bookmarkClipSet) {
        mClipSet = clipSet;
        mBookmarkClipSet = bookmarkClipSet;
        mAdapter.generateClipPosList();
        mAdapter.notifyDataSetChanged();
    }

    private int getCellUnit() {
        View view = mRecyclerView.getChildAt(1);
        return view.getWidth();
    }

    public ClipPos getCurrentClipPos() {
        return mClipSet.getClipPosByClipSetPos(getCurrentClipSetPos());
    }

    public ClipSetPos getCurrentClipSetPos() {
        int centerPos = mScreenWidth / 2;
        View view = mRecyclerView.findChildViewUnder(centerPos, 0);
        int position = mLayoutManager.getPosition(view);


        ThumbnailListAdapter.CellItem cellItem;

        // scroll to the end:
        if (position == mAdapter.getItemCount() - 1) {
            cellItem = mAdapter.getCellItem(position - 1);
            ClipFragment clipFragment = (ClipFragment) cellItem.item;
            ClipSetPos clipSetPos = new ClipSetPos(cellItem.clipIndex, clipFragment.getEndTimeMs());
            return clipSetPos;
        }
        cellItem = mAdapter.getCellItem(position);
        if (cellItem != null && cellItem.type == ThumbnailListAdapter.CellItem.ITEM_TYPE_CLIP_FRAGMENT) {
            ClipFragment clipFragment = (ClipFragment) cellItem.item;
            int offset = centerPos - view.getLeft();
            int timeOffset = offset * clipFragment.getDurationMs() / view.getWidth();
            //ClipPos clipPos = new ClipPos(clipFragment.getClip(), clipFragment.getStartTimeMs() + timeOffset);
            int clipIndex = cellItem.clipIndex;
            long clipTimeMs = clipFragment.getStartTimeMs() + timeOffset;
            ClipSetPos clipSetPos = new ClipSetPos(clipIndex, clipTimeMs);
            return clipSetPos;
        }
        return null;
    }



    private int getLength() {
        View view = mRecyclerView.getChildAt(1);
        return view.getWidth() * (mRecyclerView.getAdapter().getItemCount() - 2);
    }

    private int getScreenWidth() {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }






    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventClipPosSetChangeMainThread(ClipSetPosChangeEvent event) {
        if (event.getBroadcaster().equals(TAG)) {
            return;
        }
        ClipSetPos clipSetPos = event.getClipSetPos();
        List<ThumbnailListAdapter.CellItem> cellItems = mAdapter.getCellItemList();
        for (int i = 0; i < cellItems.size(); i++) {
            ThumbnailListAdapter.CellItem cellItem = cellItems.get(i);
            if (cellItem.type == ThumbnailListAdapter.CellItem.ITEM_TYPE_CLIP_FRAGMENT) {

                ClipFragment clipFragment = (ClipFragment) cellItem.item;
                if (cellItem.clipIndex == clipSetPos.getClipIndex()) {

                    if (clipFragment.getStartTimeMs() <= clipSetPos.getClipTimeMs() && clipSetPos.getClipTimeMs() <= clipFragment.getEndTimeMs()) {
//                        Logger.t(TAG).d("clipPosTime  " + clipSetPos.getClipTimeMs());
                        int offset = mAdapter.getPosOffset(clipSetPos.getClipTimeMs() - clipFragment.getStartTimeMs());
//                        Logger.t(TAG).d("find clip Set position:  " + i + " Offset" + offset);
                        mLayoutManager.scrollToPositionWithOffset(i, mScreenWidth / 2 - offset);
                        break;
                    }


                }
            }

        }
    }


    public class ThumbnailListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int DEFAULT_PERIOD_MS = 1000 * 60;

        private int mClipFragmentDruation = DEFAULT_PERIOD_MS;

        public class CellItem {
            static final int ITEM_TYPE_CLIP_FRAGMENT = 0;
            static final int ITEM_TYPE_DIVIDER = 1;
            static final int ITEM_TYPE_MARGIN = 2;
            int type;
            int clipIndex;
            Object item;
        }

        List<CellItem> mItems = new ArrayList<>();

        private int mCellWidth;


        private void generateClipPosList() {
            mItems.clear();
            if (mClipSet == null || mBookmarkClipSet == null) {
                return;
            }

            List<Clip> clipList = mClipSet.getClipList();


            CellItem headerMarginItem = new CellItem();
            headerMarginItem.type = CellItem.ITEM_TYPE_MARGIN;
            mItems.add(headerMarginItem);

            for (int i = 0; i < clipList.size(); i++) {
                Clip clip = clipList.get(i);
//                Logger.t(TAG).d("one clip: " + clip.getStartTimeMs() + " ~ " + clip.getEndTimeMs());
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
                    oneItem.clipIndex = i;

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

        public CellItem getCellItem(int position) {
            return mItems.get(position);
        }

        public List<CellItem> getCellItemList() {
            return mItems;
        }

        private int getPosOffset(long timeOffset) {
            return (int) (mCellWidth * timeOffset / mClipFragmentDruation);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            CellItem cellItem = mItems.get(viewType);
            if (cellItem.type == CellItem.ITEM_TYPE_CLIP_FRAGMENT) {
                ClipFragment clipFragment = (ClipFragment) cellItem.item;

                FrameLayout frameLayout = new FrameLayout(parent.getContext());
                frameLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

                ImageView imageView = new ImageView(parent.getContext());

                mCellWidth = (mRecyclerView.getHeight() - ViewUtils.dp2px(8, getResources())) * 16 / 9;

                int imageViewWidth = getPosOffset(clipFragment.getDurationMs());

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(imageViewWidth, ViewGroup.LayoutParams.MATCH_PARENT);
                params.gravity = Gravity.CENTER_VERTICAL;
                params.topMargin = params.bottomMargin = ViewUtils.dp2px(4, getResources());
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setLayoutParams(params);

                imageView.setTag("thumbnail");
                frameLayout.addView(imageView);


//                Logger.t(TAG).d("create one Fragment: " + clipFragment.getStartTimeMs() + " ~ " + clipFragment.getEndTimeMs());

                BookmarkView bookmarkView = new BookmarkView(context);
                //bookmarkView.setBackgroundColor(0xFF7AD502);
                //bookmarkView.setAlpha(0.3f);
                bookmarkView.setTag("bookmark");
                bookmarkView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//                bookmarkView.setVisibility(GONE);
                frameLayout.addView(bookmarkView);

                return new ItemViewHolder(frameLayout);


            } else if (cellItem.type == CellItem.ITEM_TYPE_DIVIDER) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                View view = inflater.inflate(R.layout.item_clip_fragment_divider, parent, false);
                return new DividerViewHolder(view);
            } else {
                View view = new View(parent.getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mScreenWidth / 2, ViewGroup.LayoutParams.MATCH_PARENT);
                view.setLayoutParams(params);
                return new MarginViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            CellItem clipFragmentItem = mItems.get(position);
            if (clipFragmentItem.type == CellItem.ITEM_TYPE_CLIP_FRAGMENT) {
                ItemViewHolder viewHolder = (ItemViewHolder) holder;
                ClipFragment clipFragment = (ClipFragment) clipFragmentItem.item;
                ClipPos clipPos = new ClipPos(clipFragment.getClip(), clipFragment.getStartTimeMs());
                mVdbImageLoader.displayVdbImage(clipPos, viewHolder.clipFragmentThumbnail, false, true);

                setupBookmarkView(clipFragment, viewHolder.bookmarkView);
            }

        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        private View.OnClickListener mOnBookmarkViewClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Clip clip = (Clip)v.getTag();

                mEventBus.post(new ClipSelectEvent(clip));
            }
        };

        private void setupBookmarkView(ClipFragment clipFragment, BookmarkView bookmarkView) {
            List<Clip> bookmarkList = mBookmarkClipSet.getClipList();

            for (int i = 0; i < bookmarkList.size(); i++) {
                Clip clip = bookmarkList.get(i);


                if (clip.realCid.equals(clipFragment.getClip().cid)) {
//                    Logger.t(TAG).d("bookmark " + clip.getStartTimeMs() + " ~ " + clip.getEndTimeMs() + " clipFragment: " + clipFragment.getStartTimeMs() + " ~" + clipFragment.getEndTimeMs());
                    if (clipFragment.getStartTimeMs() <= clip.getStartTimeMs() && clip.getStartTimeMs() <= clipFragment.getEndTimeMs()) {
                        long endTims = Math.min(clipFragment.getEndTimeMs(), clip.getEndTimeMs());
                        long bookmarkDurationInItem = endTims - clip.getStartTimeMs();
                        int bookmarkWidth = getPosOffset(bookmarkDurationInItem);
                        int marginLeft = getPosOffset(clip.getStartTimeMs() - clipFragment.getStartTimeMs());
//                                Logger.t(TAG).d("Left Duration in this cell: " + bookmarkDurationInItem);
                        FrameLayout.LayoutParams bookmarkLayoutParasm;
                        if (clipFragment.getEndTimeMs() <= clip.getEndTimeMs()) {
                            bookmarkLayoutParasm = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        } else {
                            bookmarkLayoutParasm = new FrameLayout.LayoutParams(bookmarkWidth, ViewGroup.LayoutParams.MATCH_PARENT);
                        }
                        bookmarkLayoutParasm.leftMargin = marginLeft;
                        bookmarkView.addBookmark(clip, bookmarkLayoutParasm, mOnBookmarkViewClickListener);

                    } else if (clip.getStartTimeMs() <= clipFragment.getStartTimeMs() && clip.getEndTimeMs() >= clipFragment.getEndTimeMs()) {
                        FrameLayout.LayoutParams bookmarkLayoutParasm = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//                                Logger.t(TAG).d("Duration in this cell: " + clipFragment.getDurationMs());
                        bookmarkView.addBookmark(clip, bookmarkLayoutParasm, mOnBookmarkViewClickListener);

                    } else if (clipFragment.getStartTimeMs() <= clip.getEndTimeMs() && clip.getEndTimeMs() <= clipFragment.getEndTimeMs()) {
                        long startTimeMs = Math.max(clipFragment.getStartTimeMs(), clip.getStartTimeMs());
                        long bookmarkDurationInItem = clip.getEndTimeMs() - startTimeMs;
                        int bookmarkWidth = getPosOffset(bookmarkDurationInItem);
                        int marginLeft = getPosOffset(startTimeMs - clipFragment.getStartTimeMs());
                        FrameLayout.LayoutParams bookmarkLayoutParasm = new FrameLayout.LayoutParams(bookmarkWidth, ViewGroup.LayoutParams.MATCH_PARENT);
                        bookmarkLayoutParasm.leftMargin = marginLeft;
//                                Logger.t(TAG).d("Right Duration in this cell: " + bookmarkDurationInItem);
                        bookmarkView.addBookmark(clip, bookmarkLayoutParasm, mOnBookmarkViewClickListener);

                    }
                }

            }
        }
    }


    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        ImageView clipFragmentThumbnail;

        BookmarkView bookmarkView;

        public ItemViewHolder(View itemView) {
            super(itemView);

            FrameLayout container = (FrameLayout) itemView;
            clipFragmentThumbnail = (ImageView) container.findViewWithTag("thumbnail");
            bookmarkView = (BookmarkView) container.findViewWithTag("bookmark");

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


}
