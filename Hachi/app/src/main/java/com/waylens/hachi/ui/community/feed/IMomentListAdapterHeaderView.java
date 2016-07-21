package com.waylens.hachi.ui.community.feed;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Xiaofei on 2016/7/21.
 */
public interface IMomentListAdapterHeaderView {

    RecyclerView.ViewHolder getHeaderViewHolder(ViewGroup parent);

    void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder);
}
