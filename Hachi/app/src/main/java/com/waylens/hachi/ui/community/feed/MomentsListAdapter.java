package com.waylens.hachi.ui.community.feed;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.birbit.android.jobqueue.JobManager;
import com.bumptech.glide.Glide;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.social.LikeJob;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.UserProfileActivity;
import com.waylens.hachi.ui.community.MomentActivity;
import com.waylens.hachi.ui.entities.Moment;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;


public class MomentsListAdapter extends RecyclerView.Adapter<MomentViewHolder> {
    private static final String TAG = MomentsListAdapter.class.getSimpleName();
    public ArrayList<Moment> mMoments;

    private final Context mContext;

    PrettyTime mPrettyTime;


    OnMomentActionListener mOnMomentActionListener;


    public MomentsListAdapter(Context context, ArrayList<Moment> moments) {
        this.mContext = context;
        mMoments = moments;
        mPrettyTime = new PrettyTime();
    }


    public void setMoments(ArrayList<Moment> moments) {
        mMoments = moments;
        notifyDataSetChanged();
    }

    public Moment getMomemnt(int position) {
        return mMoments.get(position);
    }

    public void addMoments(ArrayList<Moment> moments) {
        if (mMoments == null) {
            mMoments = new ArrayList<>();
        }
        int start = mMoments.size();
        int count = moments.size();
        mMoments.addAll(moments);
        notifyItemRangeInserted(start, count);
    }

    public void setOnMomentActionListener(OnMomentActionListener listener) {
        mOnMomentActionListener = listener;
    }

    @Override
    public MomentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_moment, parent, false);
        return new MomentViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MomentViewHolder holder, final int position) {
        final Moment moment = mMoments.get(position);
        Logger.t(TAG).d("moment avatar: " + moment.owner.avatarUrl + " position: " + position);
        Glide.with(mContext)
            .load(moment.owner.avatarUrl)
            .placeholder(R.drawable.waylens_logo_76x86)
            .crossFade()
            .dontAnimate()
            .into(holder.userAvatar);
        holder.userName.setText(moment.owner.userName);


        holder.videoTime.setText(mPrettyTime.formatUnrounded(new Date(moment.uploadTime)));
        if (!TextUtils.isEmpty(moment.title)) {
            holder.title.setVisibility(View.VISIBLE);
            holder.title.setText(moment.title);
        } else {
            holder.title.setVisibility(View.GONE);
        }
        Glide.with(mContext).load(moment.thumbnail).crossFade().into(holder.videoCover);
        holder.videoDuration.setText(DateUtils.formatElapsedTime(moment.duration / 1000l));
        holder.userAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserProfileActivity.launch((Activity) mContext, moment.owner.userID, holder.userAvatar);
            }
        });

        holder.videoCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MomentActivity.launch((BaseActivity) mContext, moment.id, moment.thumbnail, holder.videoCover);
            }
        });


        holder.btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnMomentActionListener != null) {
                    mOnMomentActionListener.onMoreClick(v, position);
                }
            }
        });
    }


//    @Override
//    public void onViewAttachedToWindow(MomentViewHolder holder) {
//        super.onViewAttachedToWindow(holder);
//        holder.videoControl.setVisibility(View.VISIBLE);
//    }


    @Override
    public int getItemCount() {
        if (mMoments == null) {
            return 0;
        } else {
            return mMoments.size();
        }

    }

    public void updateMoment(Spannable spannedComments, int position) {
        Moment moment = mMoments.get(position);
        moment.comments = spannedComments;
        notifyItemChanged(position);
    }


    private void doAddLike(final MomentViewHolder vh, final Moment moment) {
        boolean isCancel = moment.isLiked;
        JobManager jobManager = BgJobManager.getManager();
        LikeJob job = new LikeJob(moment.id, isCancel);
        jobManager.addJobInBackground(job);
        moment.isLiked = !moment.isLiked;
//        doUpdateLikeStateAnimator(vh, moment);
    }

    public interface OnMomentActionListener {
        void onRequestVideoPlay(MomentViewHolder vh, Moment moment, int position);

        void onMoreClick(View v, int position);
    }
}
