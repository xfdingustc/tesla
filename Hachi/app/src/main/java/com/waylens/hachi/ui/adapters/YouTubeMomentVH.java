package com.waylens.hachi.ui.adapters;

import android.view.View;
import android.widget.FrameLayout;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.fragments.YouTubeFragment;

import butterknife.Bind;

/**
 * Created by Richard on 8/24/15.
 */
public class YouTubeMomentVH extends MomentViewHolder {

    FrameLayout fragmentContainer;

    YouTubeFragment videoFragment;

    public YouTubeMomentVH(View itemView, FrameLayout fragmentContainer) {
        super(itemView);
        this.fragmentContainer = fragmentContainer;
    }
}
