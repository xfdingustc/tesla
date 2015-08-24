package com.waylens.hachi.ui.adapters;

import android.view.SurfaceView;
import android.view.View;
import android.widget.ProgressBar;

import com.waylens.hachi.R;

import butterknife.Bind;

/**
 * Created by Richard on 8/24/15.
 */
public class WaylensMomentVH extends MomentViewHolder {

    @Bind(R.id.video_play_view)
    SurfaceView videoPlayView;

    @Bind(R.id.video_control)
    View videoControl;

    @Bind(R.id.video_loading)
    ProgressBar progressBar;

    public WaylensMomentVH(View itemView) {
        super(itemView);
    }
}
