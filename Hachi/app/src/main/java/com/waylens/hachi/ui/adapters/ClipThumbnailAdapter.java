package com.waylens.hachi.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.utils.TimeMonitor;
import com.waylens.hachi.vdb.ClipPos;

import java.util.List;

/**
 * Created by Richard on 11/27/15.
 */
public class ClipThumbnailAdapter extends RecyclerView.Adapter<ClipThumbnailAdapter.ViewHolder> {

    List<ClipPos> mItems;
    VdbImageLoader mImageLoader;
    int mItemWidth;
    int mItemHeight;

    public ClipThumbnailAdapter(VdbImageLoader imageLoader, List<ClipPos> items, int itemWidth, int itemHeight) {
        mImageLoader = imageLoader;
        mItems = items;
        mItemWidth = itemWidth;
        mItemHeight = itemHeight;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ImageView view = new ImageView(parent.getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(mItemWidth, mItemHeight);
        view.setLayoutParams(params);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        mImageLoader.displayVdbImage(mItems.get(position), holder.imageView, mItemWidth, mItemHeight);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        TimeMonitor.reset("ClipThumbnailAdapter");
    }

    @Override
    public int getItemCount() {
        if (mItems == null) {
            return 0;
        }
        return mItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView imageView;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
        }
    }
}
