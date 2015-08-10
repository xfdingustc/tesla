package com.waylens.hachi.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.waylens.hachi.R;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * ViewHolder for Clip
 * Created by Richard on 8/10/15.
 */
public class ClipViewHolder extends RecyclerView.ViewHolder {
    @Bind(R.id.video_cover)
    View videoCover;
    @Bind(R.id.video_desc)
    TextView videoDesc;
    @Bind(R.id.video_time)
    TextView videoTime;
    @Bind(R.id.video_duration)
    TextView videoDuration;

    public ClipViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
