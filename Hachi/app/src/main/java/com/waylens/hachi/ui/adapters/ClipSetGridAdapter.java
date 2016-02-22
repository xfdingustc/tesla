package com.waylens.hachi.ui.adapters;

import android.content.Context;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.waylens.hachi.R;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/2/19.
 */
public class ClipSetGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context mContext;
    private final OnClipClickListener mClipClickListener;
    private ClipSet mClipSet;

    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mVdbImageLoader;

    public interface OnClipClickListener {
        void onClipClicked(Clip clip);
    }

    public ClipSetGridAdapter(Context context, ClipSet clipSet, OnClipClickListener listener) {
        this.mContext = context;
        this.mClipSet = clipSet;
        this.mClipClickListener = listener;
        mVdbRequestQueue = Snipe.newRequestQueue(mContext);
        mVdbImageLoader = VdbImageLoader.getImageLoader(mVdbRequestQueue);
    }



    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_clip_set_grid, parent, false);
        return new ClipSetGridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ClipSetGridViewHolder viewHolder = (ClipSetGridViewHolder)holder;
        Clip clip = mClipSet.getClip(position);
        ClipPos clipPos  = new ClipPos(clip);
        mVdbImageLoader.displayVdbImage(clipPos, viewHolder.ivClipCover);

        viewHolder.ivClipCover.setTag(viewHolder);
        viewHolder.ivClipCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClipClickListener == null) {
                    return;
                }

                ClipSetGridViewHolder holder = (ClipSetGridViewHolder)v.getTag();
                Clip clip = mClipSet.getClip(holder.getAdapterPosition());

                mClipClickListener.onClipClicked(clip);

            }
        });
    }

    @Override
    public int getItemCount() {
        return mClipSet == null ? 0 : mClipSet.getCount();
    }


    public static class ClipSetGridViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.ivClipCover)
        ImageView ivClipCover;

        public ClipSetGridViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
