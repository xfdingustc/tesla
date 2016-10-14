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
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.pavlospt.roundedletterview.RoundedLetterView;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
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
import com.waylens.hachi.utils.AvatarHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;


public class MomentsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
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
            MomentAbstract moment = mMoments.get(i).moment;
            if (moment.id == event.getMomentId()) {
                moment.title = event.getMomentUpdateBody().title;
                moment.description = event.getMomentUpdateBody().description;
                notifyItemChanged(i);
                break;
            }
        }
    }

    public MomentsListAdapter(Context context) {
        this.mContext = context;
        mPrettyTime = new PrettyTime(Locale.US);

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
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_moment, parent, false);
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
        final MomentAbstract momentAbstract = momentEx.moment;

        String avatar = momentEx.owner.avatarUrl;
        if (!TextUtils.isEmpty(avatar) && !avatar.equals(Constants.DEFAULT_AVATAR)) {
            Glide.with(mContext)
                .load(momentEx.owner.avatarUrl)
                .placeholder(R.drawable.ic_account_circle_placeholder)
                .crossFade()
                .dontAnimate()
                .into(holder.userAvatar);
            holder.vaAvatar.setDisplayedChild(0);
        } else {
            holder.vaAvatar.setDisplayedChild(1);
            holder.nameAvatarView.setBackgroundColor(mContext.getResources().getColor(AvatarHelper.getRandomAvatarBackgroundColor()));
            holder.nameAvatarView.setTitleText(momentEx.owner.userName.substring(0, 1).toUpperCase());
        }

        if (!TextUtils.isEmpty(momentAbstract.title)) {
            holder.title.setText(momentAbstract.title);
        } else {
            holder.title.setText(R.string.no_title);
        }

        if (!TextUtils.isEmpty(momentEx.owner.userName)) {
            holder.userName.setText(momentEx.owner.userName + " • " + mPrettyTime.formatUnrounded(new Date(momentAbstract.uploadTime)));
        } else {
            holder.userName.setText(mPrettyTime.formatUnrounded(new Date(momentAbstract.uploadTime)));
        }

        if (momentAbstract.momentVehicleInfo != null && !TextUtils.isEmpty(momentAbstract.momentVehicleInfo.vehicleModel)) {
            VehicleInfo vehicleInfo = momentAbstract.momentVehicleInfo;
            holder.carInfo.setVisibility(View.VISIBLE);
            holder.carInfo.setText(vehicleInfo.toString());
        } else {
            holder.carInfo.setVisibility(View.GONE);
        }

        holder.videoDuration.setText(DateUtils.formatElapsedTime(momentAbstract.duration / 1000l));
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
        if (!TextUtils.isEmpty(momentAbstract.momentType) && momentAbstract.momentType.equals("PICTURE")) {
            final List<MomentPicture> momentPictures = momentEx.pictureUrls;
            if (!momentPictures.isEmpty()) {
                MomentPicture momentPicture = momentPictures.get(0);
                final String cover;
                if (!TextUtils.isEmpty(momentPicture.bigThumbnail)) {
                    cover = momentPicture.bigThumbnail;
                } else if (!TextUtils.isEmpty(momentPicture.smallThumbnail)){
                    cover = momentPicture.smallThumbnail;
                } else {
                    cover = momentPicture.original;
                }
                Logger.t(TAG).d("picture: " + momentPictures.get(0).toString());
                Glide.with(mContext)
                    .load(cover)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .crossFade()
                    .into(holder.videoCover);
                holder.videoCover.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PhotoViewActivity.launch((BaseActivity)mContext, cover);
                    }
                });
            } else {
                holder.videoCover.setOnClickListener(null);
            }

            holder.videoDuration.setVisibility(View.GONE);

        } else {
            Glide.with(mContext)
                .load(momentAbstract.thumbnail)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .crossFade()
                .into(holder.videoCover);
            holder.videoCover.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MomentActivity.launch((BaseActivity) mContext, momentAbstract.id, momentAbstract.thumbnail, holder.videoCover);
                }
            });
            holder.videoDuration.setVisibility(View.VISIBLE);
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
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.report:
                                onReportClick(momentAbstract.id);
                                break;
                            case R.id.delete:
                                onDeleteClick(momentAbstract.id, holder.getAdapterPosition());
                                break;
                            case R.id.edit:
                                onEditClick(momentAbstract, holder);

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

    private void onEditClick(MomentAbstract moment, final MomentViewHolder holder) {
        MomentEditActivity.launch((Activity)mContext, moment.id, moment.title, holder.videoCover);
    }


    @Override
    public int getItemCount() {
        return mMoments.size() + 1 + (mHeaderView == null ? 0 : 1);
    }





    public static class MomentViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.vs_avatar)
        ViewAnimator vaAvatar;

        @BindView(R.id.user_avatar)
        CircleImageView userAvatar;

        @BindView(R.id.rlv_name_view)
        RoundedLetterView nameAvatarView;

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

        @BindView(R.id.car_info)
        TextView carInfo;


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
