package com.waylens.hachi.ui.adapters;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.waylens.hachi.R;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * ViewHolder for Clip
 * Created by Richard on 8/10/15.
 */
public class ClipViewHolder extends RecyclerView.ViewHolder {
    @Bind(R.id.video_desc)
    TextView videoDesc;
    @Bind(R.id.video_time)
    TextView videoTime;
    @Bind(R.id.video_duration)
    TextView videoDuration;
    @Bind(R.id.video_play_view)
    SurfaceView videoPlayView;
    @Bind(R.id.video_control)
    View videoControl;
    @Bind(R.id.video_cover)
    ImageView videoCover;
    @Bind(R.id.video_loading)
    ProgressBar progressBar;

    public ClipViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
