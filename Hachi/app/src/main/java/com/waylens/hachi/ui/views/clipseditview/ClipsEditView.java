package com.waylens.hachi.ui.views.clipseditview;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.ui.entities.SharableClip;
import com.waylens.hachi.vdb.ClipPos;

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * //
 * Created by Richard on 2/23/16.
 */
public class ClipsEditView extends RelativeLayout implements View.OnClickListener,
        RecyclerView.OnItemTouchListener, RangeSeekBar.OnRangeSeekBarChangeListener<Long> {
    final static float HALF_ALPHA = 0.5f;
    final static float FULL_ALPHA = 1.0f;

    @Bind(R.id.clip_list_view)
    RecyclerView mRecyclerView;

    @Bind(R.id.clips_count_view)
    TextView mClipsCountView;

    @Bind(R.id.clips_duration_view)
    TextView mClipDurationView;

    @Bind(R.id.range_seek_bar)
    RangeSeekBar<Long> mRangeSeekBar;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    LinearLayoutManager mLayoutManager;
    List<SharableClip> mSharableClips;
    RecyclerViewAdapter mAdapter;
    ItemTouchHelper mItemTouchHelper;
    OnClipEditListener mOnClipEditListener;

    int selectedPosition = -1;

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
        View.inflate(context, R.layout.layout_clips_edit_view, this);
        ButterKnife.bind(this, this);

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

    @Override
    public void onRangeSeekBarValuesChanged(RangeSeekBar<Long> rangeSeekBar,
                                            RangeSeekBar.Thumb pressedThumb,
                                            Long value) {
        if (selectedPosition == -1) {
            return;
        }
        SharableClip sharableClip = mSharableClips.get(selectedPosition);
        if (pressedThumb == RangeSeekBar.Thumb.MIN) {
            sharableClip.selectedStartValue = value;
        } else {
            sharableClip.selectedEndValue = value;
        }
        updateClipDuration(sharableClip);
        if (mOnClipEditListener != null) {
            mOnClipEditListener.onTrimming(pressedThumb.ordinal(), value);
        }
    }

    @Override
    public void onStopTrackingTouch(RangeSeekBar<Long> rangeSeekBar) {
        if (mOnClipEditListener != null) {
            mOnClipEditListener.onStopTrimming();
        }
    }

    public void setSharableClips(List<SharableClip> sharableClips) {
        if (mAdapter != null && sharableClips != null) {
            mSharableClips = sharableClips;
            mAdapter.notifyDataSetChanged();
            updateClipCount(sharableClips.size());
        }
    }

    public void setOnClipEditListener(OnClipEditListener listener) {
        mOnClipEditListener = listener;
    }


    public void appendSharableClip(SharableClip sharableClip) {
        ArrayList<SharableClip> sharableClips = new ArrayList<>();
        sharableClips.add(sharableClip);
        appendSharableClips(sharableClips);
    }

    public void appendSharableClips(List<SharableClip> sharableClips) {
        if (mAdapter != null && mSharableClips != null && sharableClips != null) {
            mSharableClips.addAll(sharableClips);
            int size = sharableClips.size();
            mAdapter.notifyItemRangeInserted(mSharableClips.size() - size, size);
            if (mOnClipEditListener != null) {
                mOnClipEditListener.onClipsAppended(sharableClips);
            }
        }
    }

    void internalOnExitEditing() {
        selectedPosition = -1;
        mViewAnimator.setDisplayedChild(0);
        if (mOnClipEditListener != null) {
            mOnClipEditListener.onExitEditing();
        }
    }

    void internalOnSelectClip(int selectedPosition, SharableClip sharableClip) {
        mViewAnimator.setDisplayedChild(1);
        mRangeSeekBar.setRangeValues(sharableClip.minExtensibleValue, sharableClip.maxExtensibleValue);
        mRangeSeekBar.setSelectedMinValue(sharableClip.selectedStartValue);
        mRangeSeekBar.setSelectedMaxValue(sharableClip.selectedEndValue);
        updateClipDuration(sharableClip);
        if (mOnClipEditListener != null) {
            mOnClipEditListener.onClipSelected(selectedPosition, sharableClip);
        }
    }

    private void updateClipDuration(SharableClip sharableClip) {
        mClipDurationView.setText(DateUtils.formatElapsedTime(sharableClip.getSelectedLength() / 1000));
    }

    void internalOnClipMoved(int fromPosition, int toPosition) {
        if (mOnClipEditListener != null) {
            mOnClipEditListener.onClipMoved(fromPosition, toPosition);
        }
    }

    void internalOnClipRemoved(int position) {
        updateClipCount(mSharableClips.size());
        if (mOnClipEditListener != null) {
            mOnClipEditListener.onClipRemoved(position);
        }

        if (selectedPosition == position) {
            internalOnExitEditing();
        }
    }

    void exitClipEditing() {
        if (selectedPosition == -1) {
            return;
        }
        View child = mLayoutManager.findViewByPosition(selectedPosition);
        if (child != null) {
            child.setAlpha(HALF_ALPHA);
        }
        internalOnExitEditing();
    }

    void updateClipCount(int clipCount) {
        mClipsCountView.setText(getResources().getQuantityString(
                R.plurals.numbers_of_clips, clipCount, clipCount));
    }

    class RecyclerViewAdapter extends RecyclerView.Adapter<VH> implements ItemTouchListener {
        VdbImageLoader mImageLoader;

        RecyclerViewAdapter(LinearLayoutManager layoutManager) {
            mLayoutManager = layoutManager;
            mImageLoader = VdbImageLoader.getImageLoader(Snipe.newRequestQueue());
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.items_clips_edit, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(final VH holder, final int position) {
            final SharableClip sharableClip = mSharableClips.get(position);
            ClipPos clipPos = new ClipPos(sharableClip.clip);
            mImageLoader.displayVdbImage(clipPos, holder.clipThumbnail);
            holder.itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectedPosition == holder.getAdapterPosition()) {
                        holder.itemView.setAlpha(HALF_ALPHA);
                        internalOnExitEditing();
                        return;
                    }
                    if (selectedPosition != -1) {
                        View view = mLayoutManager.findViewByPosition(selectedPosition);
                        if (view != null) {
                            view.setAlpha(HALF_ALPHA);
                        }
                    }

                    holder.itemView.setAlpha(FULL_ALPHA);
                    selectedPosition = holder.getAdapterPosition();
                    internalOnSelectClip(selectedPosition, sharableClip);
                }
            });
        }

        @Override
        public int getItemCount() {
            if (mSharableClips == null) {
                return 0;
            } else {
                return mSharableClips.size();
            }
        }

        @Override
        public void onViewAttachedToWindow(VH holder) {
            super.onViewAttachedToWindow(holder);
            if (holder.getAdapterPosition() == selectedPosition) {
                holder.itemView.setAlpha(FULL_ALPHA);
            } else {
                holder.itemView.setAlpha(HALF_ALPHA);
            }
        }

        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
            Collections.swap(mSharableClips, fromPosition, toPosition);
            if (fromPosition == selectedPosition) {
                selectedPosition = toPosition;
            }
            internalOnClipMoved(fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        @Override
        public void onItemDismiss(int position) {
            mSharableClips.remove(position);
            internalOnClipRemoved(position);
            notifyItemRemoved(position);
        }
    }

    class VH extends RecyclerView.ViewHolder implements ItemViewHolderListener {
        ImageView clipThumbnail;

        float defaultElevation = 6.0f;

        public VH(View itemView) {
            super(itemView);
            clipThumbnail = (ImageView) itemView.findViewById(R.id.clip_thumbnail);
            defaultElevation = ((CardView) itemView).getCardElevation();
        }

        @Override
        public void onItemSelected() {
            ((CardView) itemView).setCardElevation(12.0f);
        }

        @Override
        public void onItemClear() {
            ((CardView) itemView).setCardElevation(defaultElevation);
        }
    }

    public interface OnClipEditListener {
        void onClipSelected(int position, SharableClip sharableClip);

        void onClipMoved(int fromPosition, int toPosition);

        void onClipsAppended(List<SharableClip> sharableClips);

        void onClipRemoved(int position);

        void onExitEditing();

        void onStartTrimming();

        void onTrimming(int flag, long value);

        void onStopTrimming();
    }
}
