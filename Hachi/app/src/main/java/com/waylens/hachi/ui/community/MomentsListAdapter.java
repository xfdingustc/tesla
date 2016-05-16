package com.waylens.hachi.ui.community;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
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
import com.waylens.hachi.ui.activities.CommentsActivity;
import com.waylens.hachi.ui.activities.UserProfileActivity;
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

    private static final DecelerateInterpolator DECCELERATE_INTERPOLATOR = new DecelerateInterpolator();
    private static final AccelerateInterpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();
    private static final OvershootInterpolator OVERSHOOT_INTERPOLATOR = new OvershootInterpolator(4);


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
        holder.descView.setText(moment.description);
        ImageLoader.getInstance().displayImage(moment.thumbnail, holder.videoCover, ImageUtils.getVideoOptions());
        holder.videoDuration.setText(DateUtils.formatElapsedTime(moment.duration / 1000l));
        holder.userAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userId = moment.owner.userID;
                UserProfileActivity.launch(mContext, userId);
            }
        });

        updateLikeState(holder, moment);


        holder.btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doAddLike(holder, moment);
            }
        });

        holder.btnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommentsActivity.launch((Activity) mContext, moment.id, 0);
            }
        });


        holder.videoControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnMomentActionListener != null) {
                    mOnMomentActionListener.onRequestVideoPlay(holder, moment, position);
                }
            }
        });
    }


    private void doUpdateLikeStateAnimator(final MomentViewHolder holder, final Moment moment) {

        AnimatorSet animatorSet = new AnimatorSet();

        ObjectAnimator rotationAnim = ObjectAnimator.ofFloat(holder.btnLike, "rotation", 0f, 360f);
        rotationAnim.setDuration(300);
        rotationAnim.setInterpolator(ACCELERATE_INTERPOLATOR);

        ObjectAnimator bounceAnimX = ObjectAnimator.ofFloat(holder.btnLike, "scaleX", 0.2f, 1f);
        bounceAnimX.setDuration(300);
        bounceAnimX.setInterpolator(OVERSHOOT_INTERPOLATOR);

        ObjectAnimator bounceAnimY = ObjectAnimator.ofFloat(holder.btnLike, "scaleY", 0.2f, 1f);
        bounceAnimY.setDuration(300);
        bounceAnimY.setInterpolator(OVERSHOOT_INTERPOLATOR);
        bounceAnimY.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                updateLikeState(holder, moment);
            }
        });

        animatorSet.play(rotationAnim);
        animatorSet.play(bounceAnimX).with(bounceAnimY).after(rotationAnim);


        animatorSet.start();

    }

    private void updateLikeState(MomentViewHolder holder, final Moment moment) {
        if (moment.isLiked) {
            //vh.btnLike.setImageResource(R.drawable.social_like_click);
            holder.btnLike.setImageResource(R.drawable.social_like_click);
        } else {
            holder.btnLike.setImageResource(R.drawable.social_like);
        }
    }

    @Override
    public void onViewAttachedToWindow(MomentViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.videoControl.setVisibility(View.VISIBLE);
    }

    @Override
    public void onViewDetachedFromWindow(MomentViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder.videoFragment != null) {
            mFragmentManager.beginTransaction().remove(holder.videoFragment).commit();
        }

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
        doUpdateLikeStateAnimator(vh, moment);
    }

    public interface OnMomentActionListener {
        void onRequestVideoPlay(MomentViewHolder vh, Moment moment, int position);
    }
}
