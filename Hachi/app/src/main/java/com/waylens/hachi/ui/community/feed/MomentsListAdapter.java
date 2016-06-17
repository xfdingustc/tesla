package com.waylens.hachi.ui.community.feed;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Response;
import com.birbit.android.jobqueue.JobManager;
import com.bumptech.glide.Glide;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.social.DeleteMomentJob;
import com.waylens.hachi.bgjob.social.LikeJob;
import com.waylens.hachi.bgjob.social.ReportJob;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.UserProfileActivity;
import com.waylens.hachi.ui.community.MomentActivity;
import com.waylens.hachi.ui.entities.Moment;

import org.json.JSONObject;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;


public class MomentsListAdapter extends RecyclerView.Adapter<MomentsListAdapter.MomentViewHolder> {
    private static final String TAG = MomentsListAdapter.class.getSimpleName();
    public ArrayList<Moment> mMoments;

    private final Context mContext;

    PrettyTime mPrettyTime;


    private String mReportReason;


    public MomentsListAdapter(Context context, ArrayList<Moment> moments) {
        this.mContext = context;
        mMoments = moments;
        mPrettyTime = new PrettyTime();
        mReportReason = context.getResources().getStringArray(R.array.report_reason)[0];
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

        if (!TextUtils.isEmpty(moment.title)) {
            holder.title.setVisibility(View.VISIBLE);
            holder.title.setText(moment.title);
        } else {
            holder.title.setVisibility(View.GONE);
        }

        holder.userName.setText(moment.owner.userName + " • " + mPrettyTime.formatUnrounded(new Date(moment.uploadTime)));
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


        holder.toolbar.getMenu().clear();
        holder.toolbar.inflateMenu(R.menu.menu_moment);
        if (moment.owner.userID.equals(SessionManager.getInstance().getUserId())) {
            holder.toolbar.getMenu().removeItem(R.id.report);
        } else {
            holder.toolbar.getMenu().removeItem(R.id.delete);
        }

        holder.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.report:
                        onReportClick(moment.id);
                        break;
                    case R.id.delete:
                        onDeleteClick(moment.id);
                        break;

                }
                return true;
            }
        });
    }

    private void onReportClick(final long momentId) {
        MaterialDialog dialog = new MaterialDialog.Builder(mContext)
            .title(R.string.report)
            .items(R.array.report_reason)
            .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                @Override
                public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                    mReportReason = mContext.getResources().getStringArray(R.array.report_reason)[which];
                    return true;
                }
            })
            .positiveText(R.string.report)
            .negativeText(android.R.string.cancel)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                    doReportMoment(momentId);
                }
            })
            .show();
    }

    private void doReportMoment(long momentId) {
        JobManager jobManager = BgJobManager.getManager();
        ReportJob job = new ReportJob(momentId, mReportReason);
        jobManager.addJobInBackground(job);
    }


    private void onDeleteClick(final long momentId) {
        MaterialDialog dialog = new MaterialDialog.Builder(mContext)
            .title(R.string.delete)
            .positiveText(android.R.string.ok)
            .negativeText(android.R.string.cancel)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    doDeleteMoment(momentId);
                }
            }).show();
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

        @BindView(R.id.toolbar)
        Toolbar toolbar;

        @BindView(R.id.user_name)
        TextView userName;


        public MomentViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

        }
    }

}
