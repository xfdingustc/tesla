package com.waylens.hachi.ui.adapters;

import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;

import butterknife.Bind;

/**
 * Created by Richard on 8/24/15.
 */
public class WaylensMomentVH extends MomentViewHolder {

    @Bind(R.id.video_container)
    FrameLayout videoContainer;

    @Bind(R.id.video_loading)
    ProgressBar progressBar;

    SurfaceView videoPlayView;

    public WaylensMomentVH(View itemView) {
        super(itemView);
    }
}
