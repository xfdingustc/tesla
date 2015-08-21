package com.waylens.hachi.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.waylens.hachi.R;
import com.waylens.hachi.utils.ImageUtils;

import org.ocpsoft.prettytime.Duration;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Richard on 8/21/15.
 */
public class MomentsRecyclerAdapter extends RecyclerView.Adapter<MomentViewHolder> {

    ArrayList<Moment> mMoments;

    PrettyTime mPrettyTime;

    public MomentsRecyclerAdapter(ArrayList<Moment> moments) {
        mMoments = moments;
        mPrettyTime = new PrettyTime();
    }

    public void setMoments(ArrayList<Moment> moments) {
        mMoments = moments;
        notifyDataSetChanged();
    }

    @Override
    public MomentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_feed, parent, false);
        return new MomentViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MomentViewHolder holder, int position) {
        Moment moment = mMoments.get(position);
        ImageLoader.getInstance().displayImage(moment.owner.avatarUrl, holder.userAvatar, ImageUtils.getAvatarOptions());
        holder.userName.setText(moment.owner.userName);
        holder.videoTime.setText(mPrettyTime.formatUnrounded(new Date(moment.uploadTime)));
        ImageLoader.getInstance().displayImage(moment.thumbnail, holder.videoCover, ImageUtils.getVideoOptions());
        holder.videoDuration.setText(DateUtils.formatElapsedTime(moment.duration / 1000l));
        holder.likeCount.setText("" + moment.likesCount);
        holder.videoPlayView.setVisibility(View.GONE);

    }

    @Override
    public int getItemCount() {
        if (mMoments == null) {
            return 0;
        } else {
            return mMoments.size();
        }

    }
}
