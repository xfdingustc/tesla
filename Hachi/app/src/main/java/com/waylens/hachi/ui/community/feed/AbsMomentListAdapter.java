package com.waylens.hachi.ui.community.feed;

import android.support.v7.widget.RecyclerView;

import com.waylens.hachi.ui.entities.moment.MomentEx;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2016/10/20.
 */

public abstract class AbsMomentListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    protected static final int ITEM_VIEW_TYPE_MOMENT = 0;
    protected static final int ITEM_VIEW_TYPE_TAIL = 1;

    protected boolean mHasMore = true;

    protected List<MomentEx> mMoments = new ArrayList<>();




    public void setMoments(List<MomentEx> moments) {
        mMoments = moments;
        notifyDataSetChanged();
    }


    public void addMoments(List<MomentEx> moments) {
        if (mMoments == null) {
            mMoments = new ArrayList<>();
        }
        int start = mMoments.size();
        int count = moments.size();
        mMoments.addAll(moments);
        notifyItemRangeInserted(start, count);
    }

    public void setHasMore(boolean hasMore) {
        mHasMore = hasMore;
        notifyItemChanged(mMoments.size());
    }

    @Override
    public int getItemViewType(int position) {
        if (position < mMoments.size()) {
            return ITEM_VIEW_TYPE_MOMENT;
        } else {
            return ITEM_VIEW_TYPE_TAIL;
        }

    }
}
