package com.waylens.hachi.ui.adapters;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.content.Context;
import android.support.v7.widget.PopupMenu;
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
import com.waylens.hachi.R;
import com.waylens.hachi.rest.response.RaceQueryResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.UserProfileActivity;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.community.MomentActivity;
import com.waylens.hachi.ui.community.MomentEditActivity;
import com.waylens.hachi.ui.community.PerformanceTestFragment;
import com.waylens.hachi.ui.dialogs.DialogHelper;
import com.waylens.hachi.ui.entities.Moment;
import org.greenrobot.eventbus.EventBus;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by lshw on 16/9/2.
 */
public class LeaderBoardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{


    private static final String TAG = LeaderBoardAdapter.class.getSimpleName();

    private static final int ITEM_VIEW_TYPE_MOMENT = 0;
    private static final int ITEM_VIEW_TYPE_TAIL = 1;

    private List<Moment> mMoments = new ArrayList<>();

    private final Context mContext;

    private int mRaceType;

    private int mTestMode;

    private boolean mHasMore = true;

    public LeaderBoardAdapter(Context context) {
        this.mContext = context;

    }


    public void setMoments(List<Moment> moments, int raceType, int testMode) {
        mMoments = moments;
        mRaceType = raceType;
        mTestMode = testMode;
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
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leader_board, parent, false);
            return new LeaderBoardItemViewHolder(itemView);
        } else {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leader_board_tail, parent, false);
            return new LoadingViewHolder(itemView);
        }

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        if (viewType == ITEM_VIEW_TYPE_MOMENT) {
            int momentPosition = position;
            onBindLeaderBoardItemViewHolder((LeaderBoardItemViewHolder) holder, momentPosition);
        } else {
            onBindLoadingViewHolder((LoadingViewHolder) holder, position);
        }

    }


    private void onBindLeaderBoardItemViewHolder(final LeaderBoardItemViewHolder holder, final int position) {
        final Moment moment = mMoments.get(position);
//        Logger.t(TAG).d("moment avatar: " + moment.owner.avatarUrl + " position: " + position);
        Glide.with(mContext)
                .load(moment.owner.avatarUrl)
                .placeholder(R.drawable.menu_profile_photo_default)
                .crossFade()
                .dontAnimate()
                .into(holder.userAvatar);
        holder.userName.setText(moment.owner.userName);
        if (moment.momentVehicleInfo.vehicleMaker != null) {
            holder.vehicleInfo.setText(moment.momentVehicleInfo.vehicleMaker + " " + moment.momentVehicleInfo.vehicleModel + " " + moment.momentVehicleInfo.vehicleYear);
        }
        double raceTime = 0.0;
        switch (mRaceType) {
            case PerformanceTestFragment.RACE_TYPE_30MPH:
                if (mTestMode == PerformanceTestFragment.TEST_MODE_AUTO) {
                    raceTime = (double) (moment.momentTimingInfo.t3_2) / 1000;
                } else if (mTestMode == PerformanceTestFragment.TEST_MODE_COUNTDOWN) {
                    raceTime = (double) (moment.momentTimingInfo.t3_1) / 1000;
                }
                break;
            case PerformanceTestFragment.RACE_TYPE_50KMH:
                if (mTestMode == PerformanceTestFragment.TEST_MODE_AUTO) {
                    raceTime = (double) (moment.momentTimingInfo.t4_2) / 1000;
                } else if (mTestMode == PerformanceTestFragment.TEST_MODE_COUNTDOWN) {
                    raceTime = (double) (moment.momentTimingInfo.t4_1) / 1000;
                }
                break;
            case PerformanceTestFragment.RACE_TYPE_60MPH:
                if (mTestMode == PerformanceTestFragment.TEST_MODE_AUTO) {
                    raceTime = (double) (moment.momentTimingInfo.t5_2) / 1000;
                } else if (mTestMode == PerformanceTestFragment.TEST_MODE_COUNTDOWN) {
                    raceTime = (double) (moment.momentTimingInfo.t5_1) / 1000;
                }
                break;
            case PerformanceTestFragment.RACE_TYPE_100KMH:
                if (mTestMode == PerformanceTestFragment.TEST_MODE_AUTO) {
                    raceTime = (double) (moment.momentTimingInfo.t6_2) / 1000;
                } else if (mTestMode == PerformanceTestFragment.TEST_MODE_COUNTDOWN) {
                    raceTime = (double) (moment.momentTimingInfo.t6_1) / 1000;
                }
                break;
            default:
                break;
        }
        holder.raceTime.setText(raceTime + "s");
        holder.userRank.setText(String.valueOf(position + 1));
        if (position >= 0 && position <= 2 && position < mMoments.size()) {
            holder.userRank.setBackground(mContext.getResources().getDrawable(R.drawable.chip_shape_top));
        }

        holder.userAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!SessionManager.getInstance().isLoggedIn()) {
                    AuthorizeActivity.launch((Activity) mContext);
                    return;
                }
                UserProfileActivity.launch((Activity) mContext, moment.owner.userID);

            }
        });
        holder.userName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!SessionManager.getInstance().isLoggedIn()) {
                    AuthorizeActivity.launch((Activity) mContext);
                    return;
                }
                UserProfileActivity.launch((Activity) mContext, moment.owner.userID);

            }
        });

        holder.leaderboardPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MomentActivity.launch((BaseActivity) mContext, moment.id, moment.thumbnail, holder.leaderboardPlay);
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

    @Override
    public int getItemCount() {
        if (mMoments.size()  == 100) {
            return mMoments.size() + 1;
        } else {
            return mMoments.size();
        }
    }


    public static class LeaderBoardItemViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.user_avatar)
        CircleImageView userAvatar;

        @BindView(R.id.user_name)
        TextView userName;

        @BindView(R.id.vehicle_info)
        TextView vehicleInfo;

        @BindView(R.id.race_time)
        TextView raceTime;

        @BindView(R.id.leaderboard_play)
        ImageView leaderboardPlay;

        @BindView(R.id.user_rank)
        TextView userRank;


        public LeaderBoardItemViewHolder(View itemView) {
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
