package com.waylens.hachi.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.entities.SharableClip;
import com.waylens.hachi.ui.views.CameraVideoView;
import com.waylens.hachi.ui.views.VideoPlayView;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.ui.views.cliptrimmer.VideoTrimmer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * ClipFilmAdapter
 * Created by Richard on 11/26/15.
 */
public class ClipFilmAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int DEFAULT_HEIGHT = 64;

    static GregorianCalendar mCalendar = new GregorianCalendar();

    private List<SharableClip> mSharableClips;

    List<ClipFilmItem> items = new ArrayList<>();

    VdbRequestQueue mVdbRequestQueue;

    private VdbImageLoader mImageLoader;

    public OnEditClipListener mOnEditClipListener;

    VideoTrimmer.DraggingFlag mDraggingFlag;

    public ClipFilmAdapter() {
        mVdbRequestQueue = Snipe.newRequestQueue();
        mImageLoader = VdbImageLoader.getImageLoader(mVdbRequestQueue);
    }

    public void setClipSet(List<SharableClip> sharableClips) {
        if (sharableClips == null) {
            items.clear();
            notifyDataSetChanged();
            return;
        }
        mSharableClips = sharableClips;
        processClipSet();
        notifyDataSetChanged();
    }

    void processClipSet() {
        if (mSharableClips == null) {
            return;
        }

        Collections.sort(mSharableClips, new ClipComparator(false));
        items.clear();
        ClipFilmItem sectionItem = null;
        for (SharableClip sharableClip : mSharableClips) {
            if (sectionItem == null || !isSameDate(sharableClip.clip)) {
                mCalendar.setTimeInMillis(sharableClip.clip.getStandardClipDate());
                mCalendar.set(Calendar.HOUR_OF_DAY, 0);
                mCalendar.set(Calendar.MINUTE, 0);
                mCalendar.set(Calendar.SECOND, 0);
                mCalendar.set(Calendar.MILLISECOND, 0);
                sectionItem = ClipFilmItem.sectionItem(sharableClip);
                items.add(sectionItem);
                items.add(ClipFilmItem.normalClipItem(sharableClip));
            } else {
                items.add(ClipFilmItem.normalClipItem(sharableClip));
            }
            sectionItem.sectionCount++;
        }
    }

    boolean isSameDate(Clip clip) {
        long delta = clip.getStandardClipDate() - mCalendar.getTimeInMillis();
        return delta >= 0 && delta < 3600 * 1000 * 24;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ClipFilmItem.TYPE_NORMAL) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_clip_film,
                    parent, false);
            return new ClipEditViewHolder(itemView);
        } else {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_clip_film_section,
                    parent, false);
            return new SectionViewHolder(itemView);
        }

    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, final int position) {
        ClipFilmItem item = items.get(position);
        final SharableClip sharableClip = item.sharableClip;
        if (getItemViewType(position) == ClipFilmItem.TYPE_NORMAL) {
            final ClipEditViewHolder holder = (ClipEditViewHolder) viewHolder;
            holder.durationView.setText(DateUtils.formatElapsedTime(sharableClip.clip.getDurationMs() / 1000l));
            holder.videoTrimmer.setBackgroundClip(mImageLoader,
                    sharableClip.clip,
                    ViewUtils.dp2px(64, holder.videoTrimmer.getResources()));
            holder.cameraVideoView.initVideoPlay(mVdbRequestQueue, sharableClip);
            holder.btnEnhance.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnEditClipListener != null) {
                        mOnEditClipListener.onEnhanceClip(sharableClip);
                    }
                }
            });
            holder.btnShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnEditClipListener != null) {
                        mOnEditClipListener.onShareClip(sharableClip);
                    }
                }
            });
            holder.cameraVideoView.setOnProgressListener(new VideoPlayView.OnProgressListener() {
                @Override
                public void onProgress(int position, int duration) {
                    //Log.e("test", String.format("position[%d], duration[%d]", position, duration));
                    holder.videoTrimmer.setProgress(position);
                }
            });
        } else {
            SectionViewHolder holder = (SectionViewHolder) viewHolder;
            holder.clipDateView.setText(sharableClip.clip.getDateString());
            holder.clipCountView.setText(String.valueOf(item.sectionCount));
        }
    }

    @Override
    public int getItemCount() {
        if (items == null) {
            return 0;
        } else {
            return items.size();
        }
    }

    @Override
    public void onViewAttachedToWindow(final RecyclerView.ViewHolder viewHolder) {
        super.onViewAttachedToWindow(viewHolder);
        if (viewHolder instanceof ClipEditViewHolder) {
            final ClipEditViewHolder holder = (ClipEditViewHolder) viewHolder;
            final int position = holder.getAdapterPosition();
            final SharableClip sharableClip = items.get(position).sharableClip;
            holder.videoTrimmer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnEditClipListener != null) {
                        mOnEditClipListener.onEditClip(sharableClip, holder, position);
                    }
                }
            });
            holder.videoTrimmer.setOnChangeListener(new VideoTrimmer.OnTrimmerChangeListener() {
                @Override
                public void onStartTrackingTouch(VideoTrimmer trimmer, VideoTrimmer.DraggingFlag flag) {
                    if (mOnEditClipListener != null) {
                        mOnEditClipListener.onStartDragging();
                    }
                    if (holder.cameraVideoView.isPlaying()) {
                        holder.cameraVideoView.pause();
                    }
                }

                @Override
                public void onProgressChanged(VideoTrimmer trimmer, VideoTrimmer.DraggingFlag flag, long start, long end, long progress) {
                    if (holder.cameraVideoView != null) {
                        ClipPos clipPos = new ClipPos(sharableClip.clip.getVdbId(),
                                sharableClip.realCid, sharableClip.clip.clipDate, progress, ClipPos.TYPE_POSTER, false);
                        holder.cameraVideoView.updateThumbnail(clipPos);
                    }
                    sharableClip.selectedStartValue = start;
                    sharableClip.selectedEndValue = end;
                    sharableClip.currentPosition = progress;
                    mDraggingFlag = flag;
                }

                @Override
                public void onStopTrackingTouch(VideoTrimmer trimmer) {
                    if (mOnEditClipListener != null) {
                        mOnEditClipListener.onStopDragging();
                    }

                    int seekToPos = (int) (trimmer.getProgress() - trimmer.getLeftValue());
                    if (mDraggingFlag == VideoTrimmer.DraggingFlag.LEFT
                            || mDraggingFlag == VideoTrimmer.DraggingFlag.RIGHT) {
                        sharableClip.selectedStartValue = trimmer.getLeftValue();
                        sharableClip.selectedEndValue = trimmer.getRightValue();
                        holder.cameraVideoView.updateSharableClip(sharableClip, seekToPos);
                    }
                    holder.cameraVideoView.seekTo(seekToPos);
                    //Log.e("test", String.format("Progress[%d],start[%d], end[%d] ", trimmer.getProgress(), trimmer.getLeftValue(), trimmer.getRightValue()));
                }
            });
            holder.cameraVideoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.stopEditing();
                }
            });
        }
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof ClipEditViewHolder) {
            final ClipEditViewHolder holder = (ClipEditViewHolder) viewHolder;
            holder.stopEditing();
            holder.videoTrimmer.setOnClickListener(null);
        }
        super.onViewDetachedFromWindow(viewHolder);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        mOnEditClipListener = null;
        mImageLoader = null;
        super.onDetachedFromRecyclerView(recyclerView);
    }

    public interface OnEditClipListener {
        void onEditClip(SharableClip sharableClip, ClipEditViewHolder holder, int position);

        void onEnhanceClip(SharableClip sharableClip);

        void onShareClip(SharableClip sharableClip);

        void onStartDragging();

        void onStopDragging();
    }

    public static class ClipEditViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.clip_film_view)
        public VideoTrimmer videoTrimmer;

        @Bind(R.id.video_editor)
        public CameraVideoView cameraVideoView;

        @Bind(R.id.video_duration)
        public TextView durationView;

        @Bind(R.id.control_panel)
        public View controlPanel;

        @Bind(R.id.btn_enhance)
        View btnEnhance;

        @Bind(R.id.btn_share)
        View btnShare;

        public ClipEditViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void startEditing(SharableClip sharableClip) {
            cameraVideoView.setVisibility(View.VISIBLE);
            videoTrimmer.setInitRangeValues(sharableClip.minExtensibleValue, sharableClip.maxExtensibleValue);
            videoTrimmer.setLeftValue(sharableClip.selectedStartValue);
            videoTrimmer.setRightValue(sharableClip.selectedEndValue);
            videoTrimmer.setProgress(sharableClip.selectedStartValue);
            videoTrimmer.setEditing(true);
            controlPanel.setVisibility(View.VISIBLE);
            durationView.setVisibility(View.INVISIBLE);
        }

        public void stopEditing() {
            videoTrimmer.setEditing(false);
            durationView.setVisibility(View.VISIBLE);
            cameraVideoView.cleanup();
            cameraVideoView.setVisibility(View.GONE);
            controlPanel.setVisibility(View.GONE);
        }
    }

    public static class SectionViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.clip_date)
        TextView clipDateView;

        @Bind(R.id.clip_count)
        TextView clipCountView;

        public SectionViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    static class ClipComparator implements Comparator<SharableClip> {

        boolean mIsAsc;

        public ClipComparator(boolean isAsc) {
            mIsAsc = isAsc;
        }

        @Override
        public int compare(SharableClip lhs, SharableClip rhs) {
            if (mIsAsc) {
                return lhs.clip.clipDate - rhs.clip.clipDate;
            } else {
                return rhs.clip.clipDate - lhs.clip.clipDate;
            }
        }

        @Override
        public boolean equals(Object object) {
            return (object instanceof ClipComparator) && (((ClipComparator) object).mIsAsc == mIsAsc);
        }
    }

    static class ClipFilmItem {
        public static final int TYPE_NORMAL = 0;
        public static final int TYPE_SECTION = 1;
        public int type;
        public SharableClip sharableClip;
        public int sectionCount;

        ClipFilmItem(int type, SharableClip clip) {
            this.type = type;
            this.sharableClip = clip;
        }

        public static ClipFilmItem normalClipItem(SharableClip sharableClip) {
            return new ClipFilmItem(TYPE_NORMAL, sharableClip);
        }

        public static ClipFilmItem sectionItem(SharableClip firstClip) {
            return new ClipFilmItem(TYPE_SECTION, firstClip);
        }
    }
}
