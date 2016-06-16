package com.waylens.hachi.ui.clips.share;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.waylens.hachi.R;

import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/6/16.
 */
public class ShareContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public String getMomentTitle() {
        return "xfding";
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_share_content, parent, false);

        return new SharedContentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 1;
    }


    public static class SharedContentViewHolder extends RecyclerView.ViewHolder {

        public SharedContentViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
