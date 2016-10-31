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
import com.waylens.hachi.rest.bean.VehicleInfo;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.UserProfileActivity;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.authorization.LoginActivity;
import com.waylens.hachi.ui.community.MomentActivity;
import com.waylens.hachi.ui.community.MomentChangeEvent;
import com.waylens.hachi.ui.community.MomentEditActivity;
import com.waylens.hachi.ui.community.PhotoViewActivity;
import com.waylens.hachi.ui.dialogs.DialogHelper;
import com.waylens.hachi.ui.entities.MomentPicture;
import com.waylens.hachi.ui.entities.moment.MomentAbstract;
import com.waylens.hachi.ui.entities.moment.MomentEx;
import com.waylens.hachi.ui.views.AvatarView;
import com.waylens.hachi.utils.PlaceHolderHelper;
import com.waylens.hachi.utils.PrettyTimeUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MomentsListAdapter extends AbsMomentListAdapter {
    private static final String TAG = MomentsListAdapter.class.getSimpleName();




    private final Context mContext;



    private boolean mShowLoading = true;


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
        this(context, true);
    }

    public MomentsListAdapter(Context context, boolean showLoading) {
        this.mContext = context;
        EventBus.getDefault().register(this);
        mShowLoading = showLoading;
    }





    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_VIEW_TYPE_MOMENT) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_moment, parent, false);
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
        final MomentAbstract momentAbstract = momentEx.moment;


        holder.avatarView.loadAvatar(momentEx.owner.avatarUrl, momentEx.owner.userName);


        StringBuilder secondDesBuilder = new StringBuilder();
        secondDesBuilder.append(momentEx.owner.userName);

        if (momentAbstract.withGeoTag && !TextUtils.isEmpty(momentEx.moment.place.toString())) {
            secondDesBuilder.append(" • ").append(momentEx.moment.place.toString());
        }


        if (!TextUtils.isEmpty(momentAbstract.title)) {
            holder.title.setText(momentAbstract.title);
            String timeAgo = PrettyTimeUtils.getTimeAgo(momentAbstract.uploadTime);
            secondDesBuilder.append(" • ").append(timeAgo);
        } else {
            SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy");
            holder.title.setText(format.format(momentAbstract.createTime));
        }

        holder.userName.setText(secondDesBuilder.toString());

        if (momentAbstract.momentVehicleInfo != null && !TextUtils.isEmpty(momentAbstract.momentVehicleInfo.vehicleModel)) {
            VehicleInfo vehicleInfo = momentAbstract.momentVehicleInfo;
            holder.carInfo.setVisibility(View.VISIBLE);
            holder.carModel.setText(vehicleInfo.toString());
        } else {
            holder.carInfo.setVisibility(View.GONE);
        }

        if (momentAbstract.isRacingMoment()) {
            holder.racingInfo.setVisibility(View.VISIBLE);
            holder.raceType.setTitleText(momentAbstract.getRaceType());
            holder.raceTime.setText(momentAbstract.getRaceTime());
        } else {
            holder.racingInfo.setVisibility(View.GONE);
        }

        holder.videoDuration.setText(DateUtils.formatElapsedTime(momentAbstract.duration / 1000l));
        holder.avatarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!SessionManager.getInstance().isLoggedIn()) {
//                    AuthorizeActivity.launch((Activity) mContext);
//                    return;
                    LoginActivity.launch((Activity)mContext, holder.avatarView);
                    return;
                }
                UserProfileActivity.launch((Activity) mContext, momentEx.owner, holder.avatarView);

            }
        });
        if (momentAbstract.isPictureMoment()) {
            final List<MomentPicture> momentPictures = momentEx.pictureUrls;
            if (!momentPictures.isEmpty()) {
                final String cover= momentPictures.get(0).getMomentPicturlUrl();
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
                .load(momentAbstract.thumbnail)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(PlaceHolderHelper.getMomentThumbnailPlaceHolder())
                .crossFade()
                .into(holder.videoCover);
            holder.videoCover.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MomentActivity.launch((BaseActivity) mContext, momentAbstract.id, momentAbstract.thumbnail, holder.videoCover);
                }
            });
            holder.videoDuration.setVisibility(View.VISIBLE);
            holder.imageMoment.setVisibility(View.GONE);
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
        DialogHelper.showDeleteMomentConfirmDialog(mContext, momentId, new DialogHelper.OnPositiveClickListener() {
            @Override
            public void onPositiveClick() {
                notifyItemRemoved(position);
                mMoments.remove(position);
            }
        });
    }

    private void onEditClick(MomentAbstract moment, final MomentViewHolder holder) {
        MomentEditActivity.launch((Activity) mContext, moment.id, moment.title, holder.videoCover);
    }


    @Override
    public int getItemCount() {
        return mMoments.size() + (mShowLoading ? 1 : 0);
    }

    public int getDataItemCount() {
        return mMoments.size();
    }

    public static class MomentViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.avatar_view)
        AvatarView avatarView;

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
        View carInfo;

        @BindView(R.id.car_model)
        TextView carModel;

        @BindView(R.id.racing_info)
        View racingInfo;

        @BindView(R.id.race_time)
        TextView raceTime;

        @BindView(R.id.race_type)
        RoundedLetterView raceType;

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
