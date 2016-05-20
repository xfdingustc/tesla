package com.waylens.hachi.ui.community.feed;

import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.birbit.android.jobqueue.JobManager;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.social.LikeJob;
import com.waylens.hachi.ui.activities.UserProfileActivity;
import com.waylens.hachi.ui.community.MomentActivity;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.entities.User;
import com.waylens.hachi.utils.ImageUtils;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;


public class MomentsListAdapter extends RecyclerView.Adapter<MomentViewHolder> {

    public ArrayList<Moment> mMoments;

    private final Context mContext;

    PrettyTime mPrettyTime;
    FragmentManager mFragmentManager;
    RequestQueue mRequestQueue;
    Resources mResources;
    OnMomentActionListener mOnMomentActionListener;

    private User mUser;




    public MomentsListAdapter(Context context, ArrayList<Moment> moments, FragmentManager fm, RequestQueue requestQueue, Resources resources) {
        this.mContext = context;
        mMoments = moments;
        mPrettyTime = new PrettyTime();
        mFragmentManager = fm;
        mRequestQueue = requestQueue;
        mResources = resources;
    }

    public void setUserInfo(User user) {
        mUser = user;
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
        if (moment.owner != null && moment.owner.avatarUrl != null) {
            ImageLoader.getInstance().displayImage(moment.owner.avatarUrl, holder.userAvatar, ImageUtils.getAvatarOptions());
            holder.userName.setText(moment.owner.userName);
        } else if (mUser != null) {
            ImageLoader.getInstance().displayImage(mUser.avatarUrl, holder.userAvatar, ImageUtils.getAvatarOptions());
            holder.userName.setText(mUser.userName);
        }

        holder.videoTime.setText(mPrettyTime.formatUnrounded(new Date(moment.uploadTime)));
        if (moment.description != null && !moment.description.isEmpty()) {
            holder.descView.setVisibility(View.VISIBLE);
            holder.descView.setText(moment.description);
        } else {
            holder.descView.setVisibility(View.GONE);
        }
        ImageLoader.getInstance().displayImage(moment.thumbnail, holder.videoCover, ImageUtils.getVideoOptions());
        holder.videoDuration.setText(DateUtils.formatElapsedTime(moment.duration / 1000l));
        holder.userAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userId = moment.owner.userID;
                UserProfileActivity.launch(mContext, userId);
            }
        });

        holder.videoCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MomentActivity.launch(mContext, moment);
            }
        });

//        updateLikeState(holder, moment);


//        holder.btnLike.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                doAddLike(holder, moment);
//            }
//        });
//
//        holder.btnComment.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                CommentsActivity.launch((Activity) mContext, moment.id, 0);
//            }
//        });
//
//
//        holder.videoControl.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (mOnMomentActionListener != null) {
//                    mOnMomentActionListener.onRequestVideoPlay(holder, moment, position);
//                }
//            }
//        });

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
    public void onViewDetachedFromWindow(MomentViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
//        if (holder.videoFragment != null) {
//            mFragmentManager.beginTransaction().remove(holder.videoFragment).commit();
//        }

        mRequestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return request.getOriginUrl().startsWith(Constants.API_MOMENT_PLAY);
            }
        });

    }

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
        LikeJob job = new LikeJob(moment, isCancel);
        jobManager.addJobInBackground(job);
        moment.isLiked = !moment.isLiked;
//        doUpdateLikeStateAnimator(vh, moment);
    }

    public interface OnMomentActionListener {
        void onRequestVideoPlay(MomentViewHolder vh, Moment moment, int position);

        void onMoreClick(View v, int position);
    }
}