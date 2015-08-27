package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.R;
import com.waylens.hachi.vdb.ClipFragment;
import com.waylens.hachi.vdb.ClipPos;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2015/8/26.
 */
public class ClipFragmentRvAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Clip mClip;
    private final Context mContext;
    private final VdbRequestQueue mRequestQueue;
    private final VdbImageLoader mVdbImageLoader;
    private List<ClipFragment> mClipFragments;

    private static final int FRAGMENT_INTERVAL = 1000;

    public ClipFragmentRvAdapter(Context context, Clip clip, VdbRequestQueue queue) {
        this.mContext = context;
        this.mClip = clip;
        this.mClipFragments = mClip.getFragments(FRAGMENT_INTERVAL);
        this.mRequestQueue = queue;
        this.mVdbImageLoader = new VdbImageLoader(mRequestQueue);
    }
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_clip_fragment, parent, false);
        return new ClipFragmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ClipFragment fragment = mClipFragments.get(position);
        ClipFragmentViewHolder viewHolder = (ClipFragmentViewHolder)holder;
        ClipPos clipPos = fragment.getClipPos();
        mVdbImageLoader.displayVdbImage(clipPos, viewHolder.mIvClipFragment);

    }

    @Override
    public int getItemCount() {
        return mClipFragments.size();
    }


    public class ClipFragmentViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.ivClipFragment)
        ImageView mIvClipFragment;

        public ClipFragmentViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
