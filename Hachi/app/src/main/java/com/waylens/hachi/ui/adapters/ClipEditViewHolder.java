package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.graphics.Point;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.waylens.hachi.R;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.ui.views.OnViewDragListener;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.views.VideoTrimmer;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Richard on 12/4/15.
 */
public class ClipEditViewHolder extends RecyclerView.ViewHolder {
    @Bind(R.id.clip_film_view)
    public RecyclerView clipFilm;

    @Bind(R.id.video_editor)
    public View editorView;

    @Bind(R.id.video_trimmer)
    public VideoTrimmer mVideoTrimmer;

    @Bind(R.id.video_cover)
    public ImageView mVideoCover;

    public boolean isEditing;

    public ClipEditViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
