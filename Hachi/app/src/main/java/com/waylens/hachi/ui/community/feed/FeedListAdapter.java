package com.waylens.hachi.ui.community.feed;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.pavlospt.roundedletterview.RoundedLetterView;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobHelper;
import com.waylens.hachi.rest.bean.VehicleInfo;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.UserProfileActivity;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.community.MomentActivity;
import com.waylens.hachi.ui.community.MomentChangeEvent;
import com.waylens.hachi.ui.community.MomentEditActivity;
import com.waylens.hachi.ui.community.PhotoViewActivity;
import com.waylens.hachi.ui.dialogs.DialogHelper;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.entities.MomentPicture;
import com.waylens.hachi.ui.entities.moment.MomentAbstract;
import com.waylens.hachi.ui.entities.moment.MomentEx;
import com.waylens.hachi.ui.views.AvatarView;
import com.waylens.hachi.utils.PlaceHolderHelper;
import com.waylens.hachi.utils.PrettyTimeUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/9/13.
 */
public class FeedListAdapter extends AbsMomentListAdapter {
    private static final String TAG = MomentsListAdapter.class.getSimpleName();


    private final Context mContext;

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
        EventBus.getDefault().register(this);

    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_VIEW_TYPE_MOMENT) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed, parent, false);
            return new MomentViewHolder(itemView);
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
            onBindMomentViewHolder((MomentViewHolder) holder, momentPosition);
        } else {
            onBindLoadingViewHolder((LoadingViewHolder) holder, position);
        }

    }


    private void onBindMomentViewHolder(final MomentViewHolder holder, final int position) {
        final MomentEx momentEx = mMoments.get(position);
        final MomentAbstract moment = momentEx.moment;
//        Logger.t(TAG).d("moment avatar: " + moment.owner.avatarUrl + " position: " + position);

        bindMomentBasicInfo(holder, position);
        bindMomentExtraInfo(holder, position);
        bindMomentCommentInfo(holder, position);


        if (!TextUtils.isEmpty(moment.momentType) && moment.momentType.equals("PICTURE")) {
//            Logger.t(TAG).d("picture: " + momentEx.pictureUrls.get(0).toString());
            if (momentEx.pictureUrls != null && !momentEx.pictureUrls.isEmpty()) {
                MomentPicture momentPicture = momentEx.pictureUrls.get(0);
                final String cover;
                if (!TextUtils.isEmpty(momentPicture.bigThumbnail)) {
                    cover = momentPicture.bigThumbnail;
                } else if (!TextUtils.isEmpty(momentPicture.smallThumbnail)) {
                    cover = momentPicture.smallThumbnail;
                } else {
                    cover = momentPicture.original;
                }
                Glide.with(mContext)
                    .load(cover)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(PlaceHolderHelper.getMomentThumbnailPlaceHolder())
                    .crossFade()
                    .into(holder.videoCover);
                holder.videoCover.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PhotoViewActivity.launch((BaseActivity) mContext, momentEx, cover, position);
                    }
                });
            } else {
                holder.videoCover.setOnClickListener(null);
            }
            holder.videoDuration.setVisibility(View.GONE);
            holder.imageMoment.setVisibility(View.VISIBLE);

        } else {
            Glide.with(mContext)
                .load(moment.thumbnail)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(PlaceHolderHelper.getMomentThumbnailPlaceHolder())
                .crossFade()
                .into(holder.videoCover);
            holder.videoDuration.setVisibility(View.VISIBLE);
            holder.imageMoment.setVisibility(View.GONE);
            holder.videoDuration.setText(DateUtils.formatElapsedTime(moment.duration / 1000l));
            holder.videoCover.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MomentActivity.launch((BaseActivity) mContext, moment.id, moment.thumbnail, holder.videoCover);
                }
            });
        }


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

                if (momentEx.moment.isPictureMoment()) {
                    popupMenu.getMenu().removeItem(R.id.edit);
                }
                popupMenu.getMenu().removeItem(R.id.edit);
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
//                                onEditClick(moment.id, moment.title, holder);
                                break;

                        }
                        return true;
                    }
                });
                popupMenu.show();
            }
        });

    }

    private void bindMomentCommentInfo(MomentViewHolder holder, int position) {
        final MomentEx momentEx = mMoments.get(position);
        holder.commentUser1.setVisibility(View.GONE);
        holder.commentUser2.setVisibility(View.GONE);
        holder.commentUser3.setVisibility(View.GONE);
        if (momentEx.lastComments.size() == 0) {
            holder.separator.setVisibility(View.GONE);
            holder.bottomPadding.setVisibility(View.GONE);
        } else {
            holder.separator.setVisibility(View.VISIBLE);
            holder.bottomPadding.setVisibility(View.VISIBLE);
        }


        if (momentEx.lastComments.size() > 0) {
            holder.commentUser1.setVisibility(View.VISIBLE);
            holder.commentUser1.setText(getCommentString(momentEx.lastComments.get(0).author.userName, momentEx.lastComments.get(0).content));
        }

        if (momentEx.lastComments.size() > 1) {
            holder.commentUser2.setVisibility(View.VISIBLE);
            holder.commentUser2.setText(getCommentString(momentEx.lastComments.get(1).author.userName, momentEx.lastComments.get(1).content));
        }

        if (momentEx.lastComments.size() > 2) {
            holder.commentUser3.setVisibility(View.VISIBLE);
            holder.commentUser3.setText(getCommentString(momentEx.lastComments.get(2).author.userName, momentEx.lastComments.get(2).content));
        }
    }


    private void bindMomentBasicInfo(final MomentViewHolder holder, int position) {
        final MomentEx momentEx = mMoments.get(position);
        final MomentAbstract moment = momentEx.moment;
        holder.avatarView.loadAvatar(momentEx.owner.avatarUrl, momentEx.owner.userName);

        if (!TextUtils.isEmpty(moment.title)) {
            holder.title.setVisibility(View.VISIBLE);
            holder.title.setText(moment.title);
        } else {
            holder.title.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(momentEx.owner.userName)) {
            holder.userName.setText(momentEx.owner.userName);
        }
        if (moment.isRecommended) {
            holder.recommend.setVisibility(View.VISIBLE);
            holder.follow.setVisibility(View.VISIBLE);
            holder.follow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (holder.follow.isActivated()) {
                        BgJobHelper.followUser(momentEx.owner.userID, false);
                        holder.follow.setText(R.string.follow);
                        holder.follow.setActivated(false);
                        isRecommendFollowed = false;
                    } else {
                        BgJobHelper.followUser(momentEx.owner.userID, true);
                        holder.follow.setText(R.string.following);
                        holder.follow.setActivated(true);
                        isRecommendFollowed = true;
                    }
                }
            });
            if (!isRecommendFollowed) {
                holder.follow.setActivated(false);
                holder.follow.setText(R.string.follow);
            } else {
                holder.follow.setActivated(true);
                holder.follow.setText(R.string.following);
            }


        } else {
            holder.recommend.setVisibility(View.GONE);
            holder.follow.setVisibility(View.GONE);

        }

        holder.avatarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!SessionManager.getInstance().isLoggedIn()) {
                    AuthorizeActivity.launch((Activity) mContext);
                    return;
                }
                UserProfileActivity.launch((Activity) mContext, momentEx.owner, holder.avatarView, holder.itemView);

            }
        });

        if (moment.isLiked) {
            holder.btnLike.setImageResource(R.drawable.ic_favorite);
        } else {
            holder.btnLike.setImageResource(R.drawable.ic_favorite_border);
        }

        holder.tsLikeCounter.setCurrentText(Integer.toString(moment.likesCount));

        holder.btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.tsLikeCounter.setCurrentText(Integer.toString(moment.likesCount));
                if (!moment.isLiked) {
                    holder.tsLikeCounter.setText(Integer.toString(moment.likesCount + 1));
                    moment.likesCount++;
                    holder.btnLike.setImageResource(R.drawable.ic_favorite);
                } else {
                    holder.tsLikeCounter.setText(Integer.toString(moment.likesCount - 1));
                    moment.likesCount--;
                    holder.btnLike.setImageResource(R.drawable.ic_favorite_border);
                }
                BgJobHelper.addLike(moment.id, moment.isLiked);
                moment.isLiked = !moment.isLiked;

            }
        });

        holder.btnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(moment.momentType) && !moment.momentType.equals("PICTURE")) {
                    MomentActivity.launch((BaseActivity) mContext, moment.id, moment.thumbnail, holder.videoCover,
                        MomentActivity.REQUEST_COMMENT);
                }
            }
        });

        holder.commentCount.setText(Integer.toString(moment.commentsCount));
    }

    private void bindMomentExtraInfo(MomentViewHolder holder, int position) {
        final MomentEx momentEx = mMoments.get(position);
        final MomentAbstract moment = momentEx.moment;
        StringBuilder stringBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(momentEx.moment.momentVehicleInfo.vehicleModel)) {
            VehicleInfo vehicleInfo = momentEx.moment.momentVehicleInfo;
            holder.carInfo.setVisibility(View.VISIBLE);
            holder.carModel.setText(vehicleInfo.toString());
        } else {
            holder.carInfo.setVisibility(View.GONE);
        }
        if (moment.withGeoTag && !TextUtils.isEmpty(momentEx.moment.place.toString())) {
            stringBuilder.append(momentEx.moment.place.toString()).append(" • ");
        }

        stringBuilder.append(PrettyTimeUtils.getTimeAgo(moment.uploadTime));
        holder.place.setText(stringBuilder.toString());

        if (moment.isRacingMoment()) {
            holder.racingInfo.setVisibility(View.VISIBLE);
            holder.raceType.setTitleText(moment.getRaceType());
            holder.raceTime.setText(moment.getRaceTime());
        } else {
            holder.racingInfo.setVisibility(View.GONE);
        }
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
        DialogHelper.showDeleteMomentConfirmDialog(mContext, momentId, new DialogHelper.OnPositiveClickListener() {
            @Override
            public void onPositiveClick() {
                notifyItemRemoved(position);
                mMoments.remove(position);
            }
        });

    }

    private SpannableStringBuilder getCommentString(String userName, String comment) {
        String content = mContext.getString(R.string.comment_in_feed, userName, comment);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(content);
        spannableStringBuilder.setSpan(new StyleSpan(Typeface.BOLD), 0, userName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//        spannableStringBuilder.setSpan(new ForegroundColorSpan(Color.WHITE), 0, userName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableStringBuilder;
    }

    private void onEditClick(Moment moment, final MomentViewHolder holder) {
        MomentEditActivity.launch((Activity) mContext, moment, holder.videoCover);
    }


    @Override
    public int getItemCount() {
        return mMoments.size() + 1;
    }


    public static class MomentViewHolder extends RecyclerView.ViewHolder {


        @BindView(R.id.avatar_view)
        AvatarView avatarView;

        @BindView(R.id.follow)
        Button follow;


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

        @BindView(R.id.recommend)
        ImageView recommend;

        @BindView(R.id.place)
        TextView place;


        @BindView(R.id.comment_user1)
        TextView commentUser1;

        @BindView(R.id.comment_user2)
        TextView commentUser2;

        @BindView(R.id.comment_user3)
        TextView commentUser3;


        @BindView(R.id.btn_like)
        ImageButton btnLike;

        @BindView(R.id.ts_like_counter)
        TextSwitcher tsLikeCounter;

        @BindView(R.id.tv_comment_count)
        TextView commentCount;

        @BindView(R.id.btn_comment)
        ImageButton btnComment;

        @BindView(R.id.separator)
        View separator;

        @BindView(R.id.bottom_padding)
        View bottomPadding;

        @BindView(R.id.car_info)
        View carInfo;

        @BindView(R.id.car_model)
        TextView carModel;

        @BindView(R.id.racing_info)
        View racingInfo;

        @BindView(R.id.race_type)
        RoundedLetterView raceType;

        @BindView(R.id.race_time)
        TextView raceTime;

        @BindView(R.id.image_moment)
        ImageView imageMoment;

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
