package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.graphics.Point;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
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

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Richard on 11/26/15.
 */
public class ClipFilmAdapter extends RecyclerView.Adapter<ClipFilmAdapter.ViewHolder> {

    public static final int DEFAULT_HEIGHT = 64;

    private ClipSet mClipSet;

    private VdbImageLoader mImageLoader;

    public ClipFilmAdapter() {
        mImageLoader = new VdbImageLoader(Snipe.newRequestQueue());
    }

    public void setClipSet(ClipSet clipSet) {
        mClipSet = clipSet;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_clip_film,
                parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Clip clip = mClipSet.getClip(position);
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
            height = ViewUtils.dp2px(DEFAULT_HEIGHT, holder.clipFilm.getResources());
        }
        int imgWidth = height * 16 / 9;
        int itemCount = width / imgWidth;
        if (width % imgWidth != 0) {
            itemCount++;
        }
        long period = clip.getDurationMs()/ (itemCount - 1);
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
    }

    @Override
    public int getItemCount() {
        if (mClipSet == null) {
            return 0;
        } else {
            return mClipSet.getCount();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.clip_film_view)
        RecyclerView clipFilm;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
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
