package com.waylens.hachi.ui.clips.upload;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.waylens.hachi.ui.clips.cliptrimmer.VideoTrimmer;

import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/6/17.
 */
public class VideoItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }


    public class VideoItemViewHolder extends RecyclerView.ViewHolder {

        public VideoItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
