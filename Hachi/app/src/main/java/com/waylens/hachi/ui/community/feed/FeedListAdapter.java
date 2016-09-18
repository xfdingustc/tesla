package com.waylens.hachi.ui.community.feed;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobHelper;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.UserProfileActivity;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.community.MomentActivity;
import com.waylens.hachi.ui.community.MomentChangeEvent;
import com.waylens.hachi.ui.dialogs.DialogHelper;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.entities.moment.MomentAbstract;
import com.waylens.hachi.ui.entities.moment.MomentEx;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2016/9/13.
 */
public class FeedListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = MomentsListAdapter.class.getSimpleName();

    private static final int ITEM_VIEW_TYPE_MOMENT = 0;
    private static final int ITEM_VIEW_TYPE_TAIL = 1;
    private static final int ITEM_VIEW_TYPE_HEADER = 2;

    private List<MomentEx> mMoments = new ArrayList<>();

    private final Context mContext;

    private PrettyTime mPrettyTime;


    private boolean mHasMore = true;

    private IMomentListAdapterHeaderView mHeaderView = null;


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMomentChanged(MomentChangeEvent event) {
        for (int i = 0; i < mMoments.size(); i++) {
            MomentEx moment = mMoments.get(i);
            if (moment.moment.id == event.getMomentId()) {
                moment.moment.title = event.getMomentUpdateBody().title;
                moment.moment.description = event.getMomentUpdateBody().description;
                notifyItemChanged(i);
                break;
            }
        }
    }

    public FeedListAdapter(Context context) {
        this.mContext = context;
        mPrettyTime = new PrettyTime();

        EventBus.getDefault().register(this);

    }


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

    public void setHeaderView(IMomentListAdapterHeaderView headerView) {
        mHeaderView = headerView;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (mHeaderView == null) {
            if (position < mMoments.size()) {
                return ITEM_VIEW_TYPE_MOMENT;
            } else {
                return ITEM_VIEW_TYPE_TAIL;
            }
        } else {
            if (position == 0) {
                return ITEM_VIEW_TYPE_HEADER;
            } else if (position <= mMoments.size()) {
                return ITEM_VIEW_TYPE_MOMENT;
            } else {
                return ITEM_VIEW_TYPE_TAIL;
            }
        }

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_VIEW_TYPE_MOMENT) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed, parent, false);
            return new MomentViewHolder(itemView);
        } else if (viewType == ITEM_VIEW_TYPE_HEADER) {
            return mHeaderView.getHeaderViewHolder(parent);
        } else {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(itemView);
        }

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        if (viewType == ITEM_VIEW_TYPE_MOMENT) {
            int momentPosition = position;
            if (mHeaderView != null) {
                momentPosition -= 1;
            }
            onBindMomentViewHolder((MomentViewHolder) holder, momentPosition);
            onBindMomentViewHolder((MomentViewHolder) holder, momentPosition);
        } else if (viewType == ITEM_VIEW_TYPE_HEADER) {
            mHeaderView.onBindHeaderViewHolder(holder);
        } else {
            onBindLoadingViewHolder((LoadingViewHolder) holder, position);
        }

    }


    private void onBindMomentViewHolder(final MomentViewHolder holder, final int position) {
        final MomentEx momentEx = mMoments.get(position);
        final MomentAbstract moment = momentEx.moment;
//        Logger.t(TAG).d("moment avatar: " + moment.owner.avatarUrl + " position: " + position);
        Glide.with(mContext)
            .load(momentEx.owner.avatarUrl)
            .placeholder(R.drawable.menu_profile_photo_default)
            .crossFade()
            .dontAnimate()
            .into(holder.userAvatar);

        if (!TextUtils.isEmpty(moment.title)) {
            holder.title.setVisibility(View.VISIBLE);
            holder.title.setText(moment.title);
        } else {
            holder.title.setVisibility(View.GONE);
        }

        holder.uploadTime.setText(mPrettyTime.formatUnrounded(new Date(moment.uploadTime)));

        if (!TextUtils.isEmpty(momentEx.owner.userName)) {
            holder.userName.setText(momentEx.owner.userName);
        } else {
            holder.userName.setText(mPrettyTime.formatUnrounded(new Date(moment.uploadTime)));
        }

        String placeInfo = momentEx.moment.place.city + " " + momentEx.moment.place.region + " " + momentEx.moment.place.country;

        if (TextUtils.isEmpty(momentEx.moment.place.city) && TextUtils.isEmpty(momentEx.moment.place.region) && TextUtils.isEmpty(momentEx.moment.place.country)) {
            holder.place.setVisibility(View.GONE);
        } else {
            holder.place.setVisibility(View.VISIBLE);
            holder.place.setText(placeInfo.trim());
        }


        holder.commentUser1.setVisibility(View.GONE);
        holder.commentUser2.setVisibility(View.GONE);
        holder.commentUser3.setVisibility(View.GONE);
        holder.commentContent1.setVisibility(View.GONE);
        holder.commentContent2.setVisibility(View.GONE);
        holder.commentContent3.setVisibility(View.GONE);


        if (momentEx.lastComments.size() == 0) {
            holder.separator.setVisibility(View.GONE);
        } else {
            holder.separator.setVisibility(View.VISIBLE);
        }

        if (momentEx.lastComments.size() > 0) {
            holder.commentUser1.setVisibility(View.VISIBLE);
            holder.commentContent1.setVisibility(View.VISIBLE);
            holder.commentUser1.setText(momentEx.lastComments.get(0).author.userName);
            holder.commentContent1.setText(momentEx.lastComments.get(0).content);
        }

        if (momentEx.lastComments.size() > 1) {
            holder.commentUser2.setVisibility(View.VISIBLE);
            holder.commentContent2.setVisibility(View.VISIBLE);
            holder.commentUser2.setText(momentEx.lastComments.get(1).author.userName);
            holder.commentContent2.setText(momentEx.lastComments.get(1).content);
        }

        if (momentEx.lastComments.size() > 2) {
            holder.commentUser3.setVisibility(View.VISIBLE);
            holder.commentContent3.setVisibility(View.VISIBLE);
            holder.commentUser3.setText(momentEx.lastComments.get(2).author.userName);
            holder.commentContent3.setText(momentEx.lastComments.get(2).content);
        }


        Glide.with(mContext)
            .load(moment.thumbnail)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .crossFade()
            .into(holder.videoCover);
        holder.videoDuration.setText(DateUtils.formatElapsedTime(moment.duration / 1000l));
        holder.userAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!SessionManager.getInstance().isLoggedIn()) {
                    AuthorizeActivity.launch((Activity) mContext);
                    return;
                }
                UserProfileActivity.launch((Activity) mContext, momentEx.owner.userID);

            }
        });

        holder.videoCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MomentActivity.launch((BaseActivity) mContext, moment.id, moment.thumbnail, holder.videoCover);
            }
        });

        if (moment.isLiked) {
            holder.btnLike.setImageResource(R.drawable.social_like_click);
        } else {
            holder.btnLike.setImageResource(R.drawable.social_like);
        }

        holder.tsLikeCounter.setCurrentText(Integer.toString(moment.likesCount));

        holder.btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.tsLikeCounter.setCurrentText(Integer.toString(moment.likesCount));
                if (!moment.isLiked) {
                    holder.tsLikeCounter.setText(Integer.toString(moment.likesCount + 1));
                    moment.likesCount++;
                    holder.btnLike.setImageResource(R.drawable.social_like_click);
                } else {
                    holder.tsLikeCounter.setText(Integer.toString(moment.likesCount - 1));
                    moment.likesCount--;
                    holder.btnLike.setImageResource(R.drawable.social_like);
                }
                BgJobHelper.addLike(moment.id, moment.isLiked);
                moment.isLiked = !moment.isLiked;

            }
        });

        holder.commentCount.setText(Integer.toString(moment.commentsCount));


        holder.btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(mContext, holder.btnMore, Gravity.END);
                popupMenu.getMenuInflater().inflate(R.menu.menu_moment, popupMenu.getMenu());
                if (momentEx.owner.userID.equals(SessionManager.getInstance().getUserId())) {
                    popupMenu.getMenu().removeItem(R.id.report);
                } else {
                    popupMenu.getMenu().removeItem(R.id.delete);
                    popupMenu.getMenu().removeItem(R.id.edit);
                }
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.report:
                                onReportClick(moment.id);
                                break;
                            case R.id.delete:
                                onDeleteClick(moment.id, holder.getAdapterPosition());
                                break;
                            case R.id.edit:
//                                onEditClick(moment, holder);

                        }
                        return true;
                    }
                });
                popupMenu.show();
            }
        });

    }


    private void onBindLoadingViewHolder(LoadingViewHolder holder, int position) {
        if (mHasMore) {
            holder.viewAnimator.setDisplayedChild(0);
        } else {
            holder.viewAnimator.setDisplayedChild(1);
        }
    }

    private void onReportClick(final long momentId) {
        if (!SessionManager.getInstance().isLoggedIn()) {
            AuthorizeActivity.launch((Activity) mContext);
            return;
        }
        if (!SessionManager.checkUserVerified(mContext)) {
            return;
        }
        DialogHelper.showReportMomentDialog(mContext, momentId);
    }


    private void onDeleteClick(final long momentId, final int position) {
        DialogHelper.showDeleteMomentConfirmDialog(mContext, momentId, new DialogHelper.onPositiveClickListener() {
            @Override
            public void onPositiveClick() {
                notifyItemRemoved(position);
                mMoments.remove(position);
            }
        });

    }

    private void onEditClick(Moment moment, final MomentViewHolder holder) {
//        MomentEditActivity.launch((Activity)mContext, moment, holder.videoCover);
    }


    @Override
    public int getItemCount() {
        return mMoments.size() + 1 + (mHeaderView == null ? 0 : 1);
    }


    public static class MomentViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.user_avatar)
        CircleImageView userAvatar;

        @BindView(R.id.title)
        TextView title;


        @BindView(R.id.video_duration)
        TextView videoDuration;

        @BindView(R.id.video_cover)
        ImageView videoCover;

        @BindView(R.id.btn_more)
        ImageButton btnMore;

        @BindView(R.id.user_name)
        TextView userName;

        @BindView(R.id.upload_time)
        TextView uploadTime;

        @BindView(R.id.place)
        TextView place;


        @BindView(R.id.comment_user1)
        TextView commentUser1;

        @BindView(R.id.comment_user2)
        TextView commentUser2;

        @BindView(R.id.comment_user3)
        TextView commentUser3;

        @BindView(R.id.comment_content1)
        TextView commentContent1;

        @BindView(R.id.comment_content2)
        TextView commentContent2;

        @BindView(R.id.comment_content3)
        TextView commentContent3;

        @BindView(R.id.btn_like)
        ImageButton btnLike;

        @BindView(R.id.ts_like_counter)
        TextSwitcher tsLikeCounter;

        @BindView(R.id.tv_comment_count)
        TextView commentCount;

        @BindView(R.id.separator)
        View separator;


        public MomentViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

        }
    }

    public static class LoadingViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.view_animator)
        ViewAnimator viewAnimator;

        public LoadingViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

}
