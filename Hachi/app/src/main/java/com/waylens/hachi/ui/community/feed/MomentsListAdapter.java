package com.waylens.hachi.ui.community.feed;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
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

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.birbit.android.jobqueue.JobManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.social.DeleteMomentJob;
import com.waylens.hachi.bgjob.social.LikeJob;
import com.waylens.hachi.bgjob.social.ReportJob;
import com.waylens.hachi.rest.body.ReportMomentBody;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.UserProfileActivity;
import com.waylens.hachi.ui.community.MomentActivity;
import com.waylens.hachi.ui.entities.Moment;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;


public class MomentsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = MomentsListAdapter.class.getSimpleName();

    private static final int ITEM_VIEW_TYPE_MOMENT = 0;
    private static final int ITEM_VIEW_TYPE_TAIL = 1;
    private List<Moment> mMoments = new ArrayList<>();

    private final Context mContext;

    private PrettyTime mPrettyTime;


    private String mReportReason;
    private boolean mHasMore = true;


    public MomentsListAdapter(Context context) {
        this.mContext = context;
        mPrettyTime = new PrettyTime();
        mReportReason = context.getResources().getStringArray(R.array.report_reason)[0];
    }


    public void setMoments(List<Moment> moments) {
        mMoments = moments;
        notifyDataSetChanged();
    }


    public void addMoments(List<Moment> moments) {
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

    @Override
    public int getItemViewType(int position) {
        if (position < mMoments.size()) {
            return ITEM_VIEW_TYPE_MOMENT;
        } else {
            return ITEM_VIEW_TYPE_TAIL;
        }
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
            onBindMomentViewHolder((MomentViewHolder)holder, position);
        } else {
            onBindLoadingViewHolder((LoadingViewHolder)holder, position);
        }

    }



    private void onBindMomentViewHolder(final MomentViewHolder holder, final int position) {
        final Moment moment = mMoments.get(position);
//        Logger.t(TAG).d("moment avatar: " + moment.owner.avatarUrl + " position: " + position);
        Glide.with(mContext)
            .load(moment.owner.avatarUrl)
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

        if (!TextUtils.isEmpty(moment.owner.userName)) {
            holder.userName.setText(moment.owner.userName + " • " + mPrettyTime.formatUnrounded(new Date(moment.uploadTime)));
        } else {
            holder.userName.setText(mPrettyTime.formatUnrounded(new Date(moment.uploadTime)));
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
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(mContext, holder.btnMore, Gravity.END);
                popupMenu.getMenuInflater().inflate(R.menu.menu_moment, popupMenu.getMenu());
                if (moment.owner.userID.equals(SessionManager.getInstance().getUserId())) {
                    popupMenu.getMenu().removeItem(R.id.report);
                } else {
                    popupMenu.getMenu().removeItem(R.id.delete);
                }
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.report:
                                onReportClick(moment.id);
                                break;
                            case R.id.delete:
                                onDeleteClick(moment.id, position);
                                break;

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
        MaterialDialog dialog = new MaterialDialog.Builder(mContext)
            .title(R.string.report)
            .items(R.array.report_reason)
            .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                @Override
                public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                    return true;
                }
            })
            .positiveText(R.string.report)
            .negativeText(R.string.cancel)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    int index = dialog.getSelectedIndex();
                    mReportReason = mContext.getResources().getStringArray(R.array.report_reason)[index];
                    doReportMoment(momentId);
                }
            })
            .show();
    }

    private void doReportMoment(long momentId) {
        JobManager jobManager = BgJobManager.getManager();
        ReportMomentBody reportMomentBody = new ReportMomentBody();
        reportMomentBody.momentID = momentId;
        reportMomentBody.reason = mReportReason;
        reportMomentBody.detail = "";

        ReportJob job = new ReportJob(reportMomentBody, ReportJob.REPORT_TYPE_MOMENT);
        jobManager.addJobInBackground(job);
    }


    private void onDeleteClick(final long momentId, final int position) {
        MaterialDialog dialog = new MaterialDialog.Builder(mContext)
            .content(R.string.delete_this_video)
            .positiveText(R.string.ok)
            .negativeText(R.string.cancel)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    mMoments.remove(position);
//                    notifyItemRangeRemoved(position, 1);
                    notifyDataSetChanged();
                    doDeleteMoment(momentId);
                }
            }).show();

    }


    @Override
    public int getItemCount() {
        return mMoments.size() + 1;
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

    private void doDeleteMoment(long momentId) {
        JobManager jobManager = BgJobManager.getManager();
        DeleteMomentJob job = new DeleteMomentJob(momentId);
        jobManager.addJobInBackground(job);


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
