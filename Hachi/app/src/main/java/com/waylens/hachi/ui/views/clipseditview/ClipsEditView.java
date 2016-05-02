package com.waylens.hachi.ui.views.clipseditview;

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

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.util.Collections;
import java.util.List;


import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * //
 * Created by Richard on 2/23/16.
 */
public class ClipsEditView extends LinearLayout implements View.OnClickListener,
        RecyclerView.OnItemTouchListener, RangeSeekBar.OnRangeSeekBarChangeListener<Long> {
    private static final String TAG = ClipsEditView.class.getSimpleName();
    final static float HALF_ALPHA = 0.5f;
    final static float FULL_ALPHA = 1.0f;

    public final static int POSITION_UNKNOWN = -1;

    @BindView(R.id.clip_list_view)
    RecyclerView mRecyclerView;

    @BindView(R.id.clips_count_view)
    TextView mClipsCountView;

    @BindView(R.id.clips_duration_view)
    TextView mClipDurationView;

    @BindView(R.id.range_seek_bar)
    RangeSeekBar<Long> mRangeSeekBar;

    @BindView(R.id.trimming_bar)
    View mTrimmingBar;

    LinearLayoutManager mLayoutManager;
    private int mClipSetIndex;
    RecyclerViewAdapter mAdapter;
    ItemTouchHelper mItemTouchHelper;
    OnClipEditListener mOnClipEditListener;

    private VdtCamera mVdtCamera;
    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mVdbImageLoader;

    int mSelectedPosition = POSITION_UNKNOWN;

    int mOriginalSize;

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

    void init(Context context) {
        setOrientation(VERTICAL);
        View.inflate(context, R.layout.layout_clips_edit_view, this);
        ButterKnife.bind(this, this);

        mTrimmingBar.setVisibility(INVISIBLE);
        mClipsCountView.setVisibility(VISIBLE);

        mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new RecyclerViewAdapter(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        setClickable(true);
        setOnClickListener(this);
        mRecyclerView.addOnItemTouchListener(this);
        mRangeSeekBar.setNotifyWhileDragging(true);
        mRangeSeekBar.setOnRangeSeekBarChangeListener(this);


        mVdtCamera = VdtCameraManager.getManager().getCurrentCamera();
        if (mVdtCamera != null) {
            mVdbRequestQueue = mVdtCamera.getRequestQueue();//Snipe.newRequestQueue(getActivity(), mVdtCamera);
            mVdbImageLoader = VdbImageLoader.getImageLoader(mVdbRequestQueue);
        }
    }

    @Override
    public void onClick(View v) {
        //TODO
        //exitClipEditing();
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        //TODO
        //return null == recyclerView.findChildViewUnder(motionEvent.getX(), motionEvent.getY());
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        final int action = MotionEventCompat.getActionMasked(e);
        if (action == MotionEvent.ACTION_UP) {
            exitClipEditing();
        }
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        //
    }

    @Override
    public void onStartTrackingTouch(RangeSeekBar<Long> rangeSeekBar) {
        if (mOnClipEditListener != null) {
            mOnClipEditListener.onStartTrimming();
        }
    }

    private ClipSet getClipSet() {
        return ClipSetManager.getManager().getClipSet(mClipSetIndex);
    }

    @Override
    public void onRangeSeekBarValuesChanged(RangeSeekBar<Long> rangeSeekBar,
                                            RangeSeekBar.Thumb pressedThumb,
                                            Long value) {
        if (mSelectedPosition == -1) {
            return;
        }
        Clip clip = getClipSet().getClip(mSelectedPosition);
        if (pressedThumb == RangeSeekBar.Thumb.MIN) {
            clip.editInfo.selectedStartValue = value;
        } else {
            clip.editInfo.selectedEndValue = value;
        }
        updateClipDuration(clip);
        if (mOnClipEditListener != null) {
            mOnClipEditListener.onTrimming(clip, pressedThumb.ordinal(), value);
        }
    }

    @Override
    public void onStopTrackingTouch(RangeSeekBar<Long> rangeSeekBar) {
        if (mOnClipEditListener != null) {
            Clip clip = getClipSet().getClip(mSelectedPosition);
            mOnClipEditListener.onStopTrimming(clip);
        }
    }

    public void setClipIndex(int clipSetIndex) {
        if (mAdapter != null) {
            mClipSetIndex = clipSetIndex;
            mAdapter.notifyDataSetChanged();
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
        if (mAdapter != null && getClipSet() != null && clips != null) {
            getClipSet().getClipList().addAll(clips);
            int size = clips.size();
            mAdapter.notifyItemRangeInserted(getClipSet().getCount() - size, size);
            updateClipCount(getClipSet().getCount());
            if (mOnClipEditListener != null) {
                mOnClipEditListener.onClipsAppended(clips, mAdapter.getItemCount());
            }
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


    void internalOnExitEditing() {
        mSelectedPosition = -1;
        mTrimmingBar.setVisibility(INVISIBLE);
        mClipsCountView.setVisibility(VISIBLE);
        if (mOnClipEditListener != null) {
            mOnClipEditListener.onExitEditing();
        }
    }

    void internalOnSelectClip(int selectedPosition, Clip clip) {
        mTrimmingBar.setVisibility(VISIBLE);
        mClipsCountView.setVisibility(INVISIBLE);
        mRangeSeekBar.setRangeValues(clip.editInfo.minExtensibleValue, clip.editInfo.maxExtensibleValue);
        mRangeSeekBar.setSelectedMinValue(clip.editInfo.selectedStartValue);
        mRangeSeekBar.setSelectedMaxValue(clip.editInfo.selectedEndValue);
        updateClipDuration(clip);
        if (mOnClipEditListener != null) {
            mOnClipEditListener.onClipSelected(selectedPosition, clip);
        }
    }

    private void updateClipDuration(Clip clip) {
        mClipDurationView.setText(DateUtils.formatElapsedTime(clip.editInfo.getSelectedLength() /
                1000));
    }

    void internalOnClipMoved(int fromPosition, int toPosition) {
        if (mOnClipEditListener != null) {
            mOnClipEditListener.onClipMoved(fromPosition, toPosition, getClipSet().getClip(toPosition));
        }
    }

    void internalOnClipRemoved(Clip clip, int position) {
        updateClipCount(getClipSet().getCount());
        if (mOnClipEditListener != null) {
            mOnClipEditListener.onClipRemoved(clip, position, mAdapter.getItemCount());
        }

        if (mSelectedPosition == position) {
            internalOnExitEditing();
        }
    }

    void exitClipEditing() {
        if (mSelectedPosition == -1) {
            return;
        }
        View child = mLayoutManager.findViewByPosition(mSelectedPosition);
        if (child != null) {
            child.setAlpha(HALF_ALPHA);
        }
        internalOnExitEditing();
    }

    void updateClipCount(int clipCount) {
        mClipsCountView.setText(getResources().getQuantityString(
                R.plurals.numbers_of_clips, clipCount, clipCount));
    }

    void layoutTransition(VH holder, boolean isSelected) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            TransitionManager.beginDelayedTransition(holder.cardView);
        }
        ViewGroup.LayoutParams lp = holder.cardView.getLayoutParams();
        if (isSelected) {
            if (mOriginalSize == 0) {
                mOriginalSize = holder.cardView.getWidth();
            }
            int newSize = ViewUtils.dp2px(112, getResources());
            lp.width = newSize;
            lp.height = newSize;
        } else {
            if (mOriginalSize == 0) {
                mOriginalSize = ViewUtils.dp2px(80, getResources());
            }
            lp.width = mOriginalSize;
            lp.height = mOriginalSize;
        }
        holder.cardView.setLayoutParams(lp);
    }

    class RecyclerViewAdapter extends RecyclerView.Adapter<VH> implements ItemTouchListener {


        RecyclerViewAdapter(LinearLayoutManager layoutManager) {
            mLayoutManager = layoutManager;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.items_clips_edit, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(final VH holder, final int position) {
            final Clip clip = getClipSet().getClip(position);
            ClipPos clipPos = new ClipPos(clip);
            mVdbImageLoader.displayVdbImage(clipPos, holder.clipThumbnail);
            holder.itemView.setTag(holder);
            holder.itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSelectedPosition == holder.getAdapterPosition()) {
                        holder.itemView.setAlpha(HALF_ALPHA);
                        layoutTransition(holder, false);
                        internalOnExitEditing();
                        return;
                    }
                    if (mSelectedPosition != -1) {
                        View view = mLayoutManager.findViewByPosition(mSelectedPosition);
                        if (view != null) {
                            view.setAlpha(HALF_ALPHA);
                            Object tag = view.getTag();
                            if (tag instanceof VH) {
                                layoutTransition((VH) tag, false);
                            }
                        }

                    }

                    holder.itemView.setAlpha(FULL_ALPHA);
                    layoutTransition(holder, true);
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
        public void onViewAttachedToWindow(VH holder) {
            super.onViewAttachedToWindow(holder);
            if (holder.getAdapterPosition() == mSelectedPosition) {
                holder.itemView.setAlpha(FULL_ALPHA);
            } else {
                holder.itemView.setAlpha(HALF_ALPHA);
            }
        }

        @Override
        public void onViewDetachedFromWindow(VH holder) {
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

        void updateSelectedPosition(int from, int to) {
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
            Clip clip = getClipSet().getClipList().remove(position);
            internalOnClipRemoved(clip, position);
            notifyItemRemoved(position);
        }
    }

    class VH extends RecyclerView.ViewHolder implements ItemViewHolderListener {
        ImageView clipThumbnail;
        CardView cardView;
        float defaultElevation = 6.0f;

        public VH(View itemView) {
            super(itemView);
            clipThumbnail = (ImageView) itemView.findViewById(R.id.clip_thumbnail);
            cardView = (CardView) itemView.findViewById(R.id.card_view);
            defaultElevation = cardView.getCardElevation();
        }

        @Override
        public void onItemSelected() {
            //cardView.setCardElevation(12.0f);
            layoutTransition(this, true);
        }

        @Override
        public void onItemClear() {
            //cardView.setCardElevation(defaultElevation);
            layoutTransition(this, false);
        }
    }

    public interface OnClipEditListener {
        void onClipSelected(int position, Clip clip);

        void onClipMoved(int fromPosition, int toPosition, Clip clip);

        void onClipsAppended(List<Clip> clips, int clipCount);

        void onClipRemoved(Clip clip, int position, int clipCount);

        void onExitEditing();

        void onStartTrimming();

        void onTrimming(Clip clip, int flag, long value);

        void onStopTrimming(Clip clip);
    }
}
