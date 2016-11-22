package com.waylens.hachi.ui.clips.editor.clipseditview;

import android.content.Context;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.format.DateUtils;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.camera.VdtCamera;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.eventbus.events.ClipSetChangeEvent;


import com.waylens.hachi.glide_snipe_integration.SnipeGlideLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipPos;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.ClipSetManager;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor;
import com.waylens.hachi.ui.views.rangbar.RangeBar;
import com.waylens.hachi.utils.ViewUtils;



import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Subscriber;


public class ClipsEditView extends LinearLayout {
    private static final String TAG = ClipsEditView.class.getSimpleName();

    public final static int POSITION_UNKNOWN = -1;

    @BindView(R.id.clip_list_view)
    RecyclerView mRecyclerView;

    @BindView(R.id.clips_count_view)
    TextView mClipsCountView;

    @BindView(R.id.clips_duration_view)
    TextView mClipDurationView;

    @BindView(R.id.range_seek_bar)
    RangeBar mRangeSeekBar;

    @BindView(R.id.trimming_bar)
    View mTrimmingBar;

    private LinearLayoutManager mLayoutManager;
    private int mClipSetIndex;

    private ClipCoverViewAdapter mClipCoverGridAdapter;
    private ItemTouchHelper mItemTouchHelper;
    private OnClipEditListener mOnClipEditListener;

    private VdtCamera mVdtCamera;
    private VdbRequestQueue mVdbRequestQueue;


    private PlayListEditor mPlayListEditor;

    private int mSelectedPosition = POSITION_UNKNOWN;

    private int mOriginalSize;

    private EventBus mEventBus = EventBus.getDefault();


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventClipSetChanged(ClipSetChangeEvent event) {
        Logger.t(TAG).d("clip set change: " + getClipSet().getCount());
        mClipCoverGridAdapter.notifyDataSetChanged();
        updateClipCount(getClipSet().getCount());
    }

    public ClipsEditView(Context context) {
        this(context, null, 0);
    }

    public ClipsEditView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClipsEditView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void setPlayListEditor(PlayListEditor playlistEditor) {
        mPlayListEditor = playlistEditor;
        setClipIndex(mPlayListEditor.getPlaylistId());
        List<Clip> clipList = getClipSet().getClipList();
        for (Clip clip : clipList) {
            //Logger.t(TAG).d(clip.editInfo.toString());
        }
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        View.inflate(context, R.layout.layout_clips_edit_view, this);
        ButterKnife.bind(this);

        mTrimmingBar.setVisibility(INVISIBLE);
        mClipsCountView.setVisibility(VISIBLE);

        mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mClipCoverGridAdapter = new ClipCoverViewAdapter(mLayoutManager);
        mRecyclerView.setAdapter(mClipCoverGridAdapter);
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mClipCoverGridAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        setClickable(true);

        mRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {
                final int action = MotionEventCompat.getActionMasked(e);
                Logger.t(TAG).d("action: " + action);
                if (action == MotionEvent.ACTION_UP) {
                    exitClipEditing();
                }
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });


        mRangeSeekBar.setThumbMoveListener(new RangeBar.OnThumbMoveListener() {
            @Override
            public void onThumbMovingStart(RangeBar rangeBar, boolean b) {

            }

            @Override
            public void onThumbMovingStop(RangeBar rangeBar, boolean b) {
                if (mOnClipEditListener != null) {
                    Clip clip = getClipSet().getClip(mSelectedPosition);
                    mOnClipEditListener.onStopTrimming(clip);
                }
                mEventBus.post(new ClipSetChangeEvent(mClipSetIndex, true));
            }
        });
        mRangeSeekBar.setOnRangeBarChangeListener(mRangeBarchangeListener);


        mVdtCamera = VdtCameraManager.getManager().getCurrentCamera();
        if (mVdtCamera != null) {
            mVdbRequestQueue = mVdtCamera.getRequestQueue();//Snipe.newRequestQueue(getActivity(), mVdtCamera);
        }
    }

    private RangeBar.OnRangeBarChangeListener mRangeBarchangeListener = new RangeBar.OnRangeBarChangeListener() {
        @Override
        public void onRangeChangeListener(RangeBar rangeBar, int i, int i1, String s, String s1) {
            if (mSelectedPosition == -1) {
                return;
            }
//            Logger.t(TAG).d("on range change listener");
            Clip clip = getClipSet().getClip(mSelectedPosition);
            clip.editInfo.selectedStartValue = rangeBar.getLeftIndex();
            clip.editInfo.selectedEndValue = rangeBar.getRightIndex();
            updateClipDuration(clip);
            mEventBus.post(new ClipSetChangeEvent(ClipSetManager.CLIP_SET_TYPE_ENHANCE, false));
        }
    };


    private ClipSet getClipSet() {
        return ClipSetManager.getManager().getClipSet(mPlayListEditor.getPlaylistId());
    }


    public void setClipIndex(int clipSetIndex) {
        if (mClipCoverGridAdapter != null) {
            mClipSetIndex = clipSetIndex;
            mClipCoverGridAdapter.notifyDataSetChanged();
            updateClipCount(getClipSet().getCount());
        }
    }

    public void setOnClipEditListener(OnClipEditListener listener) {
        mOnClipEditListener = listener;
    }


    public boolean appendSharableClips(List<Clip> clips) {
        if (!checkIfResolutionUnity(clips)) {
            return false;
        }
        if (mClipCoverGridAdapter != null && getClipSet() != null && clips != null) {
            getClipSet().getClipList().addAll(clips);
            int size = clips.size();
            mClipCoverGridAdapter.notifyItemRangeInserted(getClipSet().getCount() - size, size);
            updateClipCount(getClipSet().getCount());
            if (mOnClipEditListener != null) {
                mOnClipEditListener.onClipsAppended(clips, mClipCoverGridAdapter.getItemCount());
            }
            Logger.t(TAG).d("post event");
            mEventBus.post(new ClipSetChangeEvent(mClipSetIndex, true));
        }

        return true;
    }


    private boolean checkIfResolutionUnity(List<Clip> clipList) {
        int firstClipWidth;
        int firstClipHeight;
        int startIndex;
        if (getClipSet() == null || getClipSet().getCount() == 0) {
            firstClipWidth = clipList.get(0).streams[0].video_width;
            firstClipHeight = clipList.get(0).streams[0].video_height;
            startIndex = 1;
        } else {
            firstClipWidth = getClipSet().getClip(0).streams[0].video_width;
            firstClipHeight = getClipSet().getClip(0).streams[0].video_height;
            startIndex = 0;
        }

        for (int i = startIndex; i < clipList.size(); i++) {

            Clip clip = clipList.get(i);
            Logger.t(TAG).d("orign width: " + firstClipWidth + " add Clip: " + clip.streams[0].video_width);
            if (clip.streams[0].video_width != firstClipWidth || clip.streams[0].video_height != firstClipHeight) {
                return false;
            }
        }

        return true;
    }

    public int getSelectedPosition() {
        return mSelectedPosition;
    }


    private void internalOnExitEditing() {
        Logger.t(TAG).d("internalOnExitEditing");
        mSelectedPosition = -1;
        mTrimmingBar.setVisibility(INVISIBLE);
        mClipsCountView.setVisibility(VISIBLE);
        if (mOnClipEditListener != null) {
            mOnClipEditListener.onExitEditing();
        }
        Logger.t(TAG).d("internalOnExitEditing exit");
    }

    private void internalOnSelectClip(int selectedPosition, Clip clip) {
        mTrimmingBar.setVisibility(VISIBLE);
        mClipsCountView.setVisibility(INVISIBLE);


        mRangeSeekBar.setOnRangeBarChangeListener(null);
//        Logger.t(TAG).d("tickStart: " + clip.getStartTimeMs() + " tickEnd: " + clip.getEndTimeMs());
//        Logger.t(TAG).d("startValue: " + clip.editInfo.selectedStartValue + " endValue: " + clip.editInfo.selectedEndValue);
//        mRangeSeekBar.setTickStart(0);
//        mRangeSeekBar.setTickEnd(clip.editInfo.maxExtensibleValue / 1000);
//        mRangeSeekBar.setTickStart(clip.editInfo.minExtensibleValue / 1000);
        mRangeSeekBar.setTicks((int)clip.getStartTimeMs() ,
            (int)clip.getEndTimeMs(),
            (int) clip.editInfo.selectedStartValue,
            (int) clip.editInfo.selectedEndValue);

//        mRangeSeekBar.setRangePinsByValue((int) clip.editInfo.selectedStartValue / 1000, (int) clip.editInfo.selectedEndValue / 1000);

        updateClipDuration(clip);
        if (mOnClipEditListener != null) {
            mOnClipEditListener.onClipSelected(selectedPosition, clip);
        }
        mRangeSeekBar.setOnRangeBarChangeListener(mRangeBarchangeListener);
    }

    private void updateClipDuration(Clip clip) {
        mClipDurationView.setText(DateUtils.formatElapsedTime(clip.editInfo.getSelectedLength() /
            1000));
    }

    private void internalOnClipMoved(int fromPosition, int toPosition) {
        if (mOnClipEditListener != null) {
            mOnClipEditListener.onClipMoved(fromPosition, toPosition, getClipSet().getClip(toPosition));
        }

        mEventBus.post(new ClipSetChangeEvent(mClipSetIndex, true));
    }

    private void internalOnClipRemoved(int position) {
        updateClipCount(getClipSet().getCount());
        if (mOnClipEditListener != null) {
            mOnClipEditListener.onClipRemoved(mClipCoverGridAdapter.getItemCount());
        }

        if (mSelectedPosition == position) {
            internalOnExitEditing();
        }
    }

    private void exitClipEditing() {
        if (mSelectedPosition == -1) {
            return;
        }
        View child = mLayoutManager.findViewByPosition(mSelectedPosition);

        internalOnExitEditing();
    }

    private void updateClipCount(int clipCount) {
        mClipsCountView.setText(getResources().getQuantityString(
            R.plurals.numbers_of_clips, clipCount, clipCount));
    }



    private class ClipCoverViewAdapter extends RecyclerView.Adapter<ClipViewHolder> implements ItemTouchListener {


        ClipCoverViewAdapter(LinearLayoutManager layoutManager) {
            mLayoutManager = layoutManager;
        }

        @Override
        public ClipViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.items_clips_edit, parent, false);
            return new ClipViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ClipViewHolder holder, final int position) {
            final Clip clip = getClipSet().getClip(position);

            ClipPos clipPos = new ClipPos(clip);
            Glide.with(getContext())
                .using(new SnipeGlideLoader(mVdbRequestQueue))
                .load(clipPos)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .crossFade()
                .placeholder(R.drawable.icon_video_default_2)
                .into(holder.clipThumbnail);

            holder.itemView.setTag(holder);
            holder.itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSelectedPosition == holder.getAdapterPosition()) {
                        holder.selectMask.setVisibility(GONE);
//                        layoutTransition(holder, false);
                        internalOnExitEditing();
                        return;
                    }
                    if (mSelectedPosition != -1) {
                        View view = mLayoutManager.findViewByPosition(mSelectedPosition);
                        if (view != null) {

                            Object tag = view.getTag();
                            if (tag instanceof ClipViewHolder) {
//                                layoutTransition((ClipViewHolder) tag, false);
                                ((ClipViewHolder)tag).selectMask.setVisibility(GONE);
                            }
                        }

                    }

                    holder.selectMask.setVisibility(VISIBLE);
//                    layoutTransition(holder, true);
                    mSelectedPosition = holder.getAdapterPosition();
                    internalOnSelectClip(mSelectedPosition, clip);
                }
            });
        }

        @Override
        public int getItemCount() {
            if (getClipSet() == null) {
                return 0;
            } else {
                return getClipSet().getCount();
            }
        }

        @Override
        public void onViewAttachedToWindow(ClipViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            if (holder.getAdapterPosition() == mSelectedPosition) {
                holder.selectMask.setVisibility(VISIBLE);
            } else {
                holder.selectMask.setVisibility(GONE);
            }
        }

        @Override
        public void onViewDetachedFromWindow(ClipViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
            holder.itemView.setTag(null);
        }

        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
            updateSelectedPosition(fromPosition, toPosition);
            Collections.swap(getClipSet().getClipList(), fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        private void updateSelectedPosition(int from, int to) {
            if (from == mSelectedPosition) {
                mSelectedPosition = to;
            } else if (to == mSelectedPosition) {
                mSelectedPosition = from;
            }
        }

        @Override
        public void onItemMoved(int fromPosition, int toPosition) {
            internalOnClipMoved(fromPosition, toPosition);
        }

        @Override
        public void onItemDismiss(int position) {
            mPlayListEditor.removeRx(position)
                .subscribe(new Subscriber<Void>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Void aVoid) {

                    }
                });
            internalOnClipRemoved(position);
            notifyItemRemoved(position);
        }
    }

    public class ClipViewHolder extends RecyclerView.ViewHolder implements ItemViewHolderListener {
        @BindView(R.id.clip_thumbnail)
        ImageView clipThumbnail;

        @BindView(R.id.select_mask)
        View selectMask;


        public ClipViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        public void onItemSelected() {
            //cardView.setCardElevation(12.0f);
//            layoutTransition(this, true);
        }

        @Override
        public void onItemClear() {
            //cardView.setCardElevation(defaultElevation);
//            layoutTransition(this, false);
        }
    }

    public interface OnClipEditListener {
        void onClipSelected(int position, Clip clip);

        void onClipMoved(int fromPosition, int toPosition, Clip clip);

        void onClipsAppended(List<Clip> clips, int clipCount);

        void onClipRemoved(int clipCount);

        void onExitEditing();


        void onStopTrimming(Clip clip);
    }
}
