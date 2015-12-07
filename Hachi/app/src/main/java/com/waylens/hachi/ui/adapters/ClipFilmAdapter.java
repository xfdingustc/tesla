package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.graphics.Point;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.waylens.hachi.R;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.views.VideoTrimmer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Richard on 11/26/15.
 */
public class ClipFilmAdapter extends RecyclerView.Adapter<ClipEditViewHolder> {

    public static final int DEFAULT_HEIGHT = 64;

    private ClipSet mClipSet;

    private VdbImageLoader mImageLoader;

    public OnEditClipListener mOnEditClipListener;

    public ClipFilmAdapter() {
        mImageLoader = new VdbImageLoader(Snipe.newRequestQueue());
    }

    public void setClipSet(ClipSet clipSet) {
        mClipSet = clipSet;
        notifyDataSetChanged();
    }

    @Override
    public ClipEditViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_clip_film,
                parent, false);
        return new ClipEditViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ClipEditViewHolder holder, final int position) {
        final Clip clip = mClipSet.getClip(position);
        holder.isEditing = false;

        int width = holder.clipFilm.getWidth();
        int height = holder.clipFilm.getHeight();

        Context context = holder.clipFilm.getContext();
        if (width == 0) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            if (size.x == 0) {
                return;
            }
            width = size.x;
        }
        if (height == 0) {
            height = ViewUtils.dp2px(64, holder.clipFilm.getResources());
        }
        int imgWidth = height * 16 / 9;
        int itemCount = width / imgWidth;
        if (width % imgWidth != 0) {
            itemCount++;
        }
        long period = clip.getDurationMs() / (itemCount - 1);
        List<ClipPos> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            long posTime = clip.getStartTimeMs() + period * i;
            if (posTime >= (clip.getStartTimeMs() + clip.getDurationMs())) {
                posTime = posTime - 10; //magic number.
            }
            items.add(new ClipPos(clip, posTime, ClipPos.TYPE_POSTER, false));
        }

        ClipThumbnailAdapter clipThumbnailAdapter = new ClipThumbnailAdapter(mImageLoader, items, imgWidth, height);
        holder.clipFilm.setLayoutManager(new ThumbnailLayoutManager(context));
        holder.clipFilm.setAdapter(clipThumbnailAdapter);

        /*
        holder.clipFilm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startEditing(holder, clip);
                if (mOnEditClipListener != null) {
                    mOnEditClipListener.onEditClip(clip, holder, position);
                }
            }
        });
        */
    }

    public void startEditing(final ClipEditViewHolder holder, final Clip clip) {
        holder.isEditing = true;
        holder.editorView.setVisibility(View.VISIBLE);
        holder.mVideoTrimmer.setBackgroundClip(mImageLoader, clip, ViewUtils.dp2px(64, holder.itemView.getResources()));
        ClipPos clipPos = new ClipPos(clip, clip.getStartTimeMs(), ClipPos.TYPE_POSTER, false);
        mImageLoader.displayVdbImage(clipPos, holder.mVideoCover);
        holder.mVideoCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishEditing(holder);
                notifyItemChanged(holder.getAdapterPosition());
            }
        });

        holder.mVideoTrimmer.setOnChangeListener(new VideoTrimmer.OnTrimmerChangeListener() {
            @Override
            public void onStartTrackingTouch(VideoTrimmer trimmer, VideoTrimmer.DraggingFlag flag) {
                if (mOnEditClipListener != null) {
                    mOnEditClipListener.onStartDragging();
                }
            }

            @Override
            public void onProgressChanged(VideoTrimmer trimmer, VideoTrimmer.DraggingFlag flag, long start, long end, long progress) {
                if (holder.mVideoCover != null) {
                    ClipPos clipPos = new ClipPos(clip, progress, ClipPos.TYPE_POSTER, false);
                    mImageLoader.displayVdbImage(clipPos, holder.mVideoCover, true, false);
                }
            }

            @Override
            public void onStopTrackingTouch(VideoTrimmer trimmer) {
                if (mOnEditClipListener != null) {
                    mOnEditClipListener.onStopDragging();
                }
            }
        });

        holder.clipFilm.setVisibility(View.GONE);
    }

    void finishEditing(ClipEditViewHolder holder) {
        if (holder == null) {
            return;
        }
        if (holder.isEditing) {
            holder.isEditing = false;
            holder.editorView.setVisibility(View.GONE);
            holder.clipFilm.setVisibility(View.VISIBLE);
        }
        holder.mVideoTrimmer.setOnChangeListener(null);
        holder.mVideoCover.setOnClickListener(null);

    }

    @Override
    public int getItemCount() {
        if (mClipSet == null) {
            return 0;
        } else {
            return mClipSet.getCount();
        }
    }

    @Override
    public void onViewAttachedToWindow(final ClipEditViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.clipFilm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                Clip clip = mClipSet.getClip(position);
                startEditing(holder, clip);
                if (mOnEditClipListener != null) {
                    mOnEditClipListener.onEditClip(clip, holder, position);
                }
            }
        });
    }

    @Override
    public void onViewDetachedFromWindow(ClipEditViewHolder holder) {
        finishEditing(holder);
        holder.clipFilm.setOnClickListener(null);
        super.onViewDetachedFromWindow(holder);

    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        mOnEditClipListener = null;
        mImageLoader = null;
        super.onDetachedFromRecyclerView(recyclerView);
    }

    public interface OnEditClipListener {
        void onEditClip(Clip clip, ClipEditViewHolder holder, int position);

        void onStartDragging();

        void onStopDragging();


    }

    static class ThumbnailLayoutManager extends LinearLayoutManager {

        public ThumbnailLayoutManager(Context context) {
            super(context);
            setOrientation(LinearLayoutManager.HORIZONTAL);
        }

        @Override
        public boolean canScrollHorizontally() {
            return false;
        }
    }
}
