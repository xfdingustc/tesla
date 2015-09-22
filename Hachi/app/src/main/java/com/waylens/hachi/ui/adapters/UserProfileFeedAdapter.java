package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.entities.Moment;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2015/9/21.
 */
public class UserProfileFeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context mContext;
    private List<Moment> mMomentList;

    public UserProfileFeedAdapter(Context context) {
        this.mContext = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_user_profile_feed, parent, false);

        return new UserProfileFeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        UserProfileFeedViewHolder viewHolder = (UserProfileFeedViewHolder)holder;
        Moment moment = mMomentList.get(position);
        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.displayImage(moment.thumbnail, viewHolder.momentCover);
    }

    @Override
    public int getItemCount() {
        if (mMomentList == null) {
            return 0;
        } else {
            return mMomentList.size();
        }
    }

    public void setMomentList(List<Moment> momentList) {
        mMomentList = momentList;
        notifyDataSetChanged();
    }


    public class UserProfileFeedViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.momentCover)
        ImageView momentCover;

        public UserProfileFeedViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
