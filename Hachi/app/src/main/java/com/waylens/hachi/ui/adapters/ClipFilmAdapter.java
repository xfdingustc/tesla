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
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.ui.fragments.ClipEditFragment;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Richard on 11/26/15.
 */
public class ClipFilmAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int DEFAULT_HEIGHT = 64;

    static GregorianCalendar mCalendar = new GregorianCalendar();

    private ClipSet mClipSet;

    ArrayList<ClipFilmItem> items = new ArrayList<>();

    private VdbImageLoader mImageLoader;

    public OnEditClipListener mOnEditClipListener;

    public ClipFilmAdapter() {
        mImageLoader = new VdbImageLoader(Snipe.newRequestQueue());
    }

    public void setClipSet(ClipSet clipSet) {
        mClipSet = clipSet;
        processClipSet();
        notifyDataSetChanged();
    }

    void processClipSet() {
        if (mClipSet == null) {
            return;
        }

        List<Clip> clips = mClipSet.getInternalList();
        Collections.sort(clips, new ClipComparator(false));
        items.clear();
        ClipFilmItem sectionItem = null;
        for (Clip clip : clips) {
            if (sectionItem == null || !isSameDate(clip)) {
                mCalendar.setTimeInMillis(clip.getStandardClipDate());
                mCalendar.set(Calendar.HOUR_OF_DAY, 0);
                mCalendar.set(Calendar.MINUTE, 0);
                mCalendar.set(Calendar.SECOND, 0);
                mCalendar.set(Calendar.MILLISECOND, 0);
                sectionItem = ClipFilmItem.sectionItem(clip);
                items.add(sectionItem);
                items.add(ClipFilmItem.normalClipItem(clip));
            } else {
                items.add(ClipFilmItem.normalClipItem(clip));
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
        if (getItemViewType(position) == ClipFilmItem.TYPE_NORMAL) {
            final Clip clip = item.clip;
            ClipEditViewHolder holder = (ClipEditViewHolder) viewHolder;
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
                    posTime = clip.getStartTimeMs() + clip.getDurationMs() - 10; //magic number.
                }
                items.add(new ClipPos(clip, posTime, ClipPos.TYPE_POSTER, false));
            }

            ClipThumbnailAdapter clipThumbnailAdapter = new ClipThumbnailAdapter(mImageLoader, items, imgWidth, height);
            ThumbnailLayoutManager layoutManager = new ThumbnailLayoutManager(context);
            holder.clipFilm.setLayoutManager(layoutManager);
            holder.clipFilm.setAdapter(clipThumbnailAdapter);
            layoutManager.scrollToPositionWithOffset(0, - imgWidth / 2);
        } else {
            SectionViewHolder holder = (SectionViewHolder) viewHolder;
            holder.clipDateView.setText(item.clip.getDateString());
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
            holder.clipFilm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = holder.getAdapterPosition();
                    Clip clip = items.get(position).clip;
                    if (mOnEditClipListener != null) {
                        mOnEditClipListener.onEditClip(clip, holder, position);
                    }
                }
            });
        }
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof ClipEditViewHolder) {
            final ClipEditViewHolder holder = (ClipEditViewHolder) viewHolder;
            holder.editorView.setVisibility(View.GONE);
            holder.clipFilm.setVisibility(View.VISIBLE);
            holder.clipFilm.setOnClickListener(null);
            if (holder.clipEditFragment != null) {
                holder.clipEditFragment.getFragmentManager().beginTransaction().remove(holder.clipEditFragment).commit();
                holder.clipEditFragment = null;
            }
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
        void onEditClip(Clip clip, ClipEditViewHolder holder, int position);
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

    public static class ClipEditViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.clip_film_view)
        public RecyclerView clipFilm;

        @Bind(R.id.video_editor)
        public View editorView;

        public ClipEditFragment clipEditFragment;

        public ClipEditViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            editorView.setId(ViewUtils.generateViewId());
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

    static class ClipComparator implements Comparator<Clip> {

        boolean mIsAsc;

        public ClipComparator(boolean isAsc) {
            mIsAsc = isAsc;
        }

        @Override
        public int compare(Clip lhs, Clip rhs) {
            if (mIsAsc) {
                return lhs.clipDate - rhs.clipDate;
            } else {
                return rhs.clipDate - lhs.clipDate;
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
        public Clip clip;
        public int sectionCount;

        ClipFilmItem(int type, Clip clip) {
            this.type = type;
            this.clip = clip;
        }

        public static ClipFilmItem normalClipItem(Clip clip) {
            return new ClipFilmItem(TYPE_NORMAL, clip);
        }

        public static ClipFilmItem sectionItem(Clip firstClip) {
            return new ClipFilmItem(TYPE_SECTION, firstClip);
        }
    }
}
