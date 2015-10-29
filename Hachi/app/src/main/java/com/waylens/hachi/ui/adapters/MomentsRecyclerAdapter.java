package com.waylens.hachi.ui.adapters;

import android.app.FragmentManager;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.utils.ImageUtils;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Richard on 8/21/15.
 */
public class MomentsRecyclerAdapter extends RecyclerView.Adapter<MomentViewHolder> {

    public ArrayList<Moment> mMoments;

    PrettyTime mPrettyTime;
    FragmentManager mFragmentManager;
    RequestQueue mRequestQueue;
    Resources mResources;
    OnMomentActionListener mOnMomentActionListener;

    public MomentsRecyclerAdapter(ArrayList<Moment> moments, FragmentManager fm, RequestQueue requestQueue, Resources resources) {
        mMoments = moments;
        mPrettyTime = new PrettyTime();
        mFragmentManager = fm;
        mRequestQueue = requestQueue;
        mResources = resources;
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
    public int getItemViewType(int position) {
        if (mMoments == null) {
            return 0;
        }
        return mMoments.get(position).type;
    }


    @Override
    public MomentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_feed, parent, false);
        return new MomentViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MomentViewHolder holder, final int position) {
        final Moment moment = mMoments.get(position);

        ImageLoader.getInstance().displayImage(moment.owner.avatarUrl, holder.userAvatar, ImageUtils.getAvatarOptions());
        holder.userName.setText(moment.owner.userName);
        holder.videoTime.setText(mPrettyTime.formatUnrounded(new Date(moment.uploadTime)));
        ImageLoader.getInstance().displayImage(moment.thumbnail, holder.videoCover, ImageUtils.getVideoOptions());
        holder.videoDuration.setText(DateUtils.formatElapsedTime(moment.duration / 1000l));

        holder.userAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnMomentActionListener != null) {
                    mOnMomentActionListener.onUserAvatarClicked(moment, position);
                }
            }
        });

        updateLikeState(holder, moment);
        updateLikeCount(holder, moment);

        holder.btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnMomentActionListener != null) {
                    mOnMomentActionListener.onLikeMoment(moment, moment.isLiked);
                }
                moment.isLiked = !moment.isLiked;
                updateLikeState(holder, moment);
                if (moment.isLiked) {
                    moment.likesCount++;
                } else {
                    moment.likesCount--;
                }
                updateLikeCount(holder, moment);
            }
        });

        holder.btnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnMomentActionListener != null) {
                    mOnMomentActionListener.onCommentMoment(moment, position);
                }
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

        updateCommentCount(holder, moment);
        if (moment.comments != null) {
            holder.commentView.setText(moment.comments);
            holder.commentContainer.setVisibility(View.VISIBLE);
        } else {
            holder.commentContainer.setVisibility(View.GONE);
        }
    }

    void updateLikeState(MomentViewHolder vh, Moment moment) {
        if (moment.isLiked) {
            vh.btnLike.setImageResource(R.drawable.feed_button_like_active);
        } else {
            vh.btnLike.setImageResource(R.drawable.feed_button_like);
        }
    }

    void updateLikeCount(MomentViewHolder vh, Moment moment) {
        if (moment.likesCount == 0) {
            vh.likeCount.setText(mResources.getString(R.string.zero_likes));
        } else {
            vh.likeCount.setText(mResources.getQuantityString(R.plurals.number_of_likes,
                    moment.likesCount,
                    moment.likesCount));
        }
    }

    void updateCommentCount(MomentViewHolder vh, Moment moment) {
        if (moment.commentsCount == 0) {
            vh.commentIcon.setVisibility(View.GONE);
            vh.commentCountView.setVisibility(View.GONE);
        } else {
            vh.commentIcon.setVisibility(View.VISIBLE);
            vh.commentCountView.setVisibility(View.VISIBLE);
            vh.commentCountView.setText(mResources.getQuantityString(
                    R.plurals.number_of_comments, moment.commentsCount, moment.commentsCount));
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

    public interface OnMomentActionListener {
        void onLikeMoment(Moment moment, boolean isCancel);

        void onCommentMoment(Moment moment, int position);

        void onUserAvatarClicked(Moment moment, int position);

        void onRequestVideoPlay(MomentViewHolder vh, Moment moment, int position);
    }
}
