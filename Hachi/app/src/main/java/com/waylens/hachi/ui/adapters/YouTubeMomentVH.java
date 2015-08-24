package com.waylens.hachi.ui.adapters;

import android.view.View;
import android.widget.FrameLayout;

/**
 * Created by Richard on 8/24/15.
 */
public class YouTubeMomentVH extends MomentViewHolder {

    FrameLayout fragmentContainer;
    public YouTubeMomentVH(View itemView, FrameLayout fragmentContainer) {
        super(itemView);
        this.fragmentContainer = fragmentContainer;
    }
}
