package com.waylens.hachi.ui.leaderboard;

/**
 * Created by lshw on 16/9/2.
 */

import android.animation.ObjectAnimator;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.TransitionRes;
import android.support.v4.util.Pair;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.aakira.expandablelayout.ExpandableLayoutListenerAdapter;
import com.github.aakira.expandablelayout.ExpandableLinearLayout;
import com.github.pavlospt.roundedletterview.RoundedLetterView;
import com.lzy.widget.HexagonView;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.bean.LeaderBoardItem;
import com.waylens.hachi.rest.bean.Maker;
import com.waylens.hachi.rest.bean.Model;
import com.waylens.hachi.rest.response.RaceQueryResponse;
import com.waylens.hachi.ui.activities.UserProfileActivity;
import com.waylens.hachi.ui.community.MomentActivity;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.fragments.ReSelectableTab;
import com.waylens.hachi.ui.fragments.Refreshable;
import com.waylens.hachi.ui.views.RecyclerViewExt;
import com.waylens.hachi.utils.AvatarHelper;
import com.waylens.hachi.utils.ServerErrorHelper;
import com.waylens.hachi.utils.SettingHelper;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;

import com.waylens.hachi.ui.views.loadtoast.LoadToast;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.OnClick;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class LeaderboardFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener,
    Refreshable, FragmentNavigator, ReSelectableTab {
    private static final String TAG = LeaderboardFragment.class.getSimpleName();

    private SparseArray<Transition> transitions = new SparseArray<>();

    public static final int RACE_TYPE_30MPH = 0;
    public static final int RACE_TYPE_60MPH = 1;
    public static final int RACE_TYPE_50KMH = 2;
    public static final int RACE_TYPE_100KMH = 3;

    public static final int TEST_MODE_AUTO = 0;
    public static final int TEST_MODE_COUNTDOWN = 1;


    private LeaderBoardAdapter mAdapter;
    private LinearLayoutManager mLinearLayoutManager;

    private LeaderboardFilterAdapter mModeAdapter;
    private LeaderboardFilterAdapter mSpeedAdapter;
    private LeaderboardFilterAdapter mTimeAdapter;

    private int mCurrentCursor;

    private String mMaker;

    private String mModel;

    private List<Pair<Maker, Model>> mMakerModelList;
    private int mLeaderBoardItemCount;

    private LoadToast mLoadToast;

    private long splitTime30[] = {0, 2000, 4000, 8000, 80000};
    private long splitTime60[] = {0, 3000, 3500, 4000, 5000, 7000, 10000, 100000};


    @BindView(R.id.container)
    ViewGroup rootContainer;

    @BindView(R.id.top_three)
    View topThree;

    @BindView(R.id.rlv_first)
    RoundedLetterView rlvFirst;

    @BindView(R.id.rlv_second)
    RoundedLetterView rlvSecond;

    @BindView(R.id.rlv_third)
    RoundedLetterView rlvThird;

    @BindView(R.id.btn_drop_down)
    ImageView btnDropDown;

    @BindView(R.id.ll_filter)
    ExpandableLinearLayout llFilter;

    @BindView(R.id.leaderboard_list_view)
    RecyclerViewExt mRvLeaderboardList;

    @BindView(R.id.layout_no_data)
    LinearLayout mNoDataLayout;

    @BindView(R.id.first)
    HexagonView hvFirst;

    @BindView(R.id.firstName)
    TextView firstName;

    @BindView(R.id.firstVehicle)
    TextView firstVehicle;

    @BindView(R.id.firstRaceTime)
    TextView firstRaceTime;

    @BindView(R.id.second)
    HexagonView hvSecond;

    @BindView(R.id.secondName)
    TextView secondName;

    @BindView(R.id.secondVehicle)
    TextView secondVehicle;

    @BindView(R.id.secondRaceTime)
    TextView secondRaceTime;

    @BindView(R.id.third)
    HexagonView hvThird;

    @BindView(R.id.thirdName)
    TextView thirdName;

    @BindView(R.id.thirdVehicle)
    TextView thirdVehicle;

    @BindView(R.id.thirdRaceTime)
    TextView thirdRaceTime;

    @BindView(R.id.rv_mode)
    RecyclerView rvMode;

    @BindView(R.id.rv_speed)
    RecyclerView rvSpeed;

    @BindView(R.id.rv_time)
    RecyclerView rvTime;

    @BindView(R.id.tvFilterMode)
    TextView tvFilterMode;

    @BindView(R.id.tvFilterSpeed)
    TextView tvFilterSpeed;

    @BindView(R.id.tvFilterTime)
    TextView tvFilterTime;

    @BindView(R.id.title)
    TextView title;

    @BindView(R.id.layout_your_rank)
    View yourRank;

    @BindView(R.id.tv_your_rank)
    TextView tvYourRank;

    @BindView(R.id.ll_filter_result)
    View llFilterResult;

    @BindArray(R.array.race_mode)
    String[] raceModeList;

    @BindArray(R.array.race_type_imperial)
    String[] raceTypeImperialList;

    @BindArray(R.array.race_type_metric)
    String[] raceTypeMetricList;

    @BindArray(R.array.race_time_030)
    String[] raceTime030List;

    @BindArray(R.array.race_time_060)
    String[] raceTime060List;


    @OnClick(R.id.ll_filter_result)
    public void onllFilterClicked() {
        llFilter.toggle();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
        }
        mAdapter = new LeaderBoardAdapter(getActivity());
        mLinearLayoutManager = new LinearLayoutManager(getActivity());

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_leaderboard, savedInstanceState);

        setupExpandableFilter();
        setLeaderboardItemList();
        setupLeaderBoardFilter();

        rlvFirst.setTextTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        rlvSecond.setTextTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        rlvThird.setTextTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));


        mMakerModelList = new ArrayList<>();
        mLoadToast = new LoadToast(getActivity());
//        mLoadToast.setText(getString(R.string.loading));

        return view;
    }

    @Override
    public void onReSelected() {
        loadLeaderBoard(mCurrentCursor, true);
    }

    private void setupExpandableFilter() {
        llFilter.collapse();
        llFilter.setInterpolator(new FastOutSlowInInterpolator());
        llFilter.setListener(new ExpandableLayoutListenerAdapter() {
            @Override
            public void onPreOpen() {
                super.onPreOpen();
                ObjectAnimator dropDownAnimator = ObjectAnimator.ofFloat(btnDropDown, View.ROTATION, 0, 180)
                    .setDuration(300);
                dropDownAnimator.setInterpolator(new FastOutSlowInInterpolator());
                dropDownAnimator.start();
            }

            @Override
            public void onPreClose() {
                super.onPreClose();
                if (btnDropDown.getRotation() == 180) {
                    ObjectAnimator dropDownAnimator = ObjectAnimator.ofFloat(btnDropDown, View.ROTATION, 180, 0)
                        .setDuration(300);
                    dropDownAnimator.setInterpolator(new FastOutSlowInInterpolator());
                    dropDownAnimator.start();
                }
            }
        });
    }

    private void setLeaderboardItemList() {
//        mRefreshLayout.setColorSchemeResources(R.color.style_color_accent, android.R.color.holo_green_light,
//            android.R.color.holo_orange_light, android.R.color.holo_red_light);
        mRvLeaderboardList.setAdapter(mAdapter);
        mRvLeaderboardList.setLayoutManager(mLinearLayoutManager);
//        mRefreshLayout.setOnRefreshListener(this);
        mRvLeaderboardList.setOnLoadMoreListener(new RecyclerViewExt.OnLoadMoreListener() {
            @Override
            public void loadMore() {
                //loadLeaderBoard(mCurrentCursor, false);
            }
        });
        mRvLeaderboardList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    llFilter.collapse();
                }
            }
        });
    }

    private void setupLeaderBoardFilter() {
        setupModeGroudRv();
        setupSpeedGroupRv();
        setupTimeGroupRv(false);

    }

    private void setupModeGroudRv() {
        rvMode.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        mModeAdapter = new LeaderboardFilterAdapter(getActivity(), raceModeList, new LeaderboardFilterAdapter.OnFilterItemClickListener() {
            @Override
            public void onFilterItemClick(int position, String filter) {
                tvFilterMode.setText(filter);
                loadLeaderBoard(0, true);
            }
        });
        mModeAdapter.setSelected(getString(R.string.auto_mode));
        rvMode.setAdapter(mModeAdapter);

    }

    private void setupSpeedGroupRv() {
        rvSpeed.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        mSpeedAdapter = new LeaderboardFilterAdapter(getActivity(), getSpeedStringList(), new LeaderboardFilterAdapter.OnFilterItemClickListener() {
            @Override
            public void onFilterItemClick(int position, String filter) {
                tvFilterSpeed.setText(filter);
                boolean isTime030 = position == 0 ? true : false;
                setupTimeGroupRv(isTime030);
                loadLeaderBoard(0, true);
            }
        });
        mSpeedAdapter.setSelected(getString(SettingHelper.isMetricUnit() ? R.string.kmh100 : R.string.mph60));
        tvFilterSpeed.setText(getString(SettingHelper.isMetricUnit() ? R.string.kmh100 : R.string.mph60));
        rvSpeed.setAdapter(mSpeedAdapter);
    }

    private void setupTimeGroupRv(boolean isTime030) {
        rvTime.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        String[] timeGroupList = isTime030 ? raceTime030List : raceTime060List;
        mTimeAdapter = new LeaderboardFilterAdapter(getActivity(), timeGroupList, new LeaderboardFilterAdapter.OnFilterItemClickListener() {
            @Override
            public void onFilterItemClick(int position, String filter) {
                tvFilterTime.setText(filter);
                loadLeaderBoard(0, true);
            }
        });
        mTimeAdapter.setSelected(getString(R.string.all));
        tvFilterTime.setText(getString(R.string.all));
        rvTime.setAdapter(mTimeAdapter);
    }

    private String[] getSpeedStringList() {
        return SettingHelper.isMetricUnit() ? raceTypeMetricList : raceTypeImperialList;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCurrentCursor = 0;
        loadLeaderBoard(mCurrentCursor, true);
    }

    private void loadLeaderBoard(int cursor, final boolean isRefresh) {
        if (isRefresh) {
            mLoadToast.show();
        }
//        final RaceQueryBody raceQueryBody = new RaceQueryBody();
        int queryStart = getRaceMode();
        int queryEnd = 0;
        int mode = mModeAdapter.getSelectedIndex();


        if (SettingHelper.isMetricUnit()) {
            switch (mSpeedAdapter.getSelectedIndex()) {
                case 0:
                    queryEnd = 4;
                    break;
                case 1:
                    queryEnd = 6;
                    break;
            }
        } else {
            switch (mSpeedAdapter.getSelectedIndex()) {
                case 0:
                    queryEnd = 3;
                    break;
                case 1:
                    queryEnd = 5;
                    break;
            }
        }

        mLeaderBoardItemCount = 100;
//        Logger.t(TAG).d("mUpper" + getTimeGroupUpper() + "mLower" + getTimeGroupLower());
        HachiService.createHachiApiService().queryRaceRx(mode,
            queryStart, queryEnd, getTimeGroupUpper(), getTimeGroupLower(), mMaker, mModel, mLeaderBoardItemCount)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<RaceQueryResponse>() {
                @Override
                public void onNext(RaceQueryResponse raceQueryResponse) {
                    mLoadToast.success();
                    onHandleRaceQuery(raceQueryResponse, isRefresh);
                }

                @Override
                public void onError(Throwable e) {
                    Logger.t(TAG).d("load error");
                    mLoadToast.error();
                    ServerErrorHelper.showErrorMessage(mRootView, e);
                }
            });
    }

    private void onHandleRaceQuery(RaceQueryResponse raceQueryResponse, boolean isRefresh) {
        llFilter.collapse();
        if (raceQueryResponse.leaderboard == null) {
            return;
        }
        Iterator<LeaderBoardItem> it = raceQueryResponse.leaderboard.iterator();
        while (it.hasNext()) {
            LeaderBoardItem item = it.next();
            if (MomentRaceTimeHelper.getRaceTime(item.moment, getRaceType(), mModeAdapter.getSelectedIndex()) <= 0) {
                it.remove();
            }
        }

        if (isRefresh) {
            List<LeaderBoardItem> leaderBoardItems = raceQueryResponse.leaderboard;
            int itemSize = leaderBoardItems.size();
            clearTopThreeUserInfo(hvFirst, firstName, firstVehicle, firstRaceTime);
            clearTopThreeUserInfo(hvSecond, secondName, secondVehicle, secondRaceTime);
            clearTopThreeUserInfo(hvThird, thirdName, thirdVehicle, thirdRaceTime);
            if (itemSize > 0) {
                setTopThreeUserInfo(leaderBoardItems.get(0), hvFirst, firstName, firstVehicle, firstRaceTime);
            }

            if (itemSize > 1) {
                setTopThreeUserInfo(leaderBoardItems.get(1), hvSecond, secondName, secondVehicle, secondRaceTime);
            }

            if (itemSize > 2) {
                setTopThreeUserInfo(leaderBoardItems.get(2), hvThird, thirdName, thirdVehicle, thirdRaceTime);
            }

            if (raceQueryResponse.leaderboard.size() <= 3) {
                mAdapter.clear();
                mNoDataLayout.setVisibility(View.VISIBLE);
            } else {
                mAdapter.setMoments(raceQueryResponse.leaderboard.subList(3, itemSize), getRaceType(), mModeAdapter.getSelectedIndex());
                mNoDataLayout.setVisibility(View.INVISIBLE);
            }
        } else {
            mAdapter.addMoments(raceQueryResponse.leaderboard);
        }
        int bestRankIndex = -1;
        int rank = -1;
        for (int i = 0; i < raceQueryResponse.userRankings.size(); i++) {
            if (rank < 0) {
                rank = raceQueryResponse.userRankings.get(i).rank;
                bestRankIndex = i;
            } else {
                if (rank > raceQueryResponse.userRankings.get(i).rank) {
                    rank = raceQueryResponse.userRankings.get(i).rank;
                    bestRankIndex = i;
                }
            }
        }
        Logger.t(TAG).d("rank: " + rank + " bestRankIndex: " + bestRankIndex);

        initMyRankView(rank, bestRankIndex);
        mRvLeaderboardList.setIsLoadingMore(false);
        mCurrentCursor += raceQueryResponse.leaderboard.size();

        mRvLeaderboardList.setEnableLoadMore(false);
        mAdapter.setHasMore(false);
    }


    private void clearTopThreeUserInfo(final HexagonView avatarView, TextView tvUserName, TextView vehicleInfo, TextView raceTime) {
        avatarView.setImageDrawable(null);
        avatarView.setText(null);
        tvUserName.setText(null);
        vehicleInfo.setText(null);
        raceTime.setText(null);
    }


    private void setTopThreeUserInfo(final LeaderBoardItem item, final HexagonView avatarView,
                                     TextView tvUserName, TextView vehicleInfo, TextView raceTime) {
        String avatarUrl = item.owner.avatarUrl;
        if (!TextUtils.isEmpty(avatarUrl) && !avatarUrl.equals(Constants.DEFAULT_AVATAR)) {
            Glide.with(this)
                .load(item.owner.avatarUrl)
                .crossFade()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(avatarView);
            avatarView.setText(null);
        } else {
            String userName = item.owner.userName;
            avatarView.setFillColor(getResources().getColor(AvatarHelper.getAvatarBackgroundColor(userName)));
            avatarView.setImageDrawable(null);
            avatarView.setText(userName.substring(0, 1).toUpperCase());
        }

        avatarView.setOnHexagonClickListener(new HexagonView.OnHexagonViewClickListener() {
            @Override
            public void onClick(View view) {
                MomentActivity.launch(getActivity(), item.moment.id, item.moment.thumbnail, null);
            }
        });

        tvUserName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UserProfileActivity.launch(getActivity(), item.owner, null);
            }
        });


        tvUserName.setText(item.owner.userName);
        if (!TextUtils.isEmpty(item.moment.momentVehicleInfo.toString())) {
            vehicleInfo.setText(item.moment.momentVehicleInfo.toString());
            vehicleInfo.setVisibility(View.VISIBLE);
        } else {
            vehicleInfo.setVisibility(View.INVISIBLE);
        }


        NumberFormat formatter = new DecimalFormat("#0.00");
        raceTime.setText(String.format(getString(R.string.race_time), formatter.format(MomentRaceTimeHelper.getRaceTime(item.moment, getRaceType(), mModeAdapter.getSelectedIndex()))));

    }


    private int getRaceType() {
        if (SettingHelper.isMetricUnit()) {
            switch (mSpeedAdapter.getSelectedIndex()) {
                case 0:
                    return RACE_TYPE_50KMH;
                case 1:
                    return RACE_TYPE_100KMH;

            }
        } else {
            switch (mSpeedAdapter.getSelectedIndex()) {
                case 0:
                    return RACE_TYPE_30MPH;
                case 1:
                    return RACE_TYPE_60MPH;
            }
        }

        return RACE_TYPE_50KMH;
    }

    private int getRaceMode() {
        if (mModeAdapter.getSelectedIndex() == TEST_MODE_AUTO) {
            return 2;
        } else {
            return 1;
        }
    }

    private Long getTimeGroupUpper() {
        if (mTimeAdapter.getSelectedIndex() == 0) {
            return null;
        } else if (getRaceType() == RACE_TYPE_30MPH || getRaceType() == RACE_TYPE_50KMH) {
            return splitTime30[mTimeAdapter.getSelectedIndex()];
        } else {
            return splitTime60[mTimeAdapter.getSelectedIndex()];
        }
    }

    private Long getTimeGroupLower() {
        if (mTimeAdapter.getSelectedIndex() == 0) {
            return null;
        } else if (getRaceType() == RACE_TYPE_30MPH || getRaceType() == RACE_TYPE_50KMH) {
            return splitTime30[mTimeAdapter.getSelectedIndex() - 1];
        } else {
            return splitTime60[mTimeAdapter.getSelectedIndex() - 1];
        }
    }


    private void initMyRankView(int rank, int bestRankIndex) {
        if (rank > 0 && bestRankIndex >= 0) {
            title.setVisibility(View.GONE);
            yourRank.setVisibility(View.VISIBLE);
            tvYourRank.setText(getString(R.string.rank, rank));
        } else {
            title.setVisibility(View.VISIBLE);
            yourRank.setVisibility(View.GONE);
        }
    }


    @Override
    protected String getRequestTag() {
        return TAG;
    }


    @Override
    public void onRefresh() {
        mCurrentCursor = 0;
        mRvLeaderboardList.setEnableLoadMore(true);
        loadLeaderBoard(mCurrentCursor, true);

    }

    @Override
    public void enableRefresh(boolean enabled) {
//        if (mRefreshLayout != null) {
//            mRefreshLayout.setEnabled(enabled);
//        }
    }


    @Override
    public boolean onInterceptBackPressed() {
        Logger.t(TAG).d("back pressed");
        return false;
    }

    @Override
    public void onSelected() {
        Logger.t(TAG).d("leader board select");
        if (mLoadToast != null) {
            mLoadToast.setFragmentVisibility(true);
        }
    }

    @Override
    public void onDeselected() {
        Logger.t(TAG).d("leader board deselect");
        mLoadToast.setFragmentVisibility(false);
    }


    private Transition getTransition(@TransitionRes int transitionId) {
        Transition transition = transitions.get(transitionId);
        if (transition == null) {
            transition = TransitionInflater.from(getActivity()).inflateTransition(transitionId);
            transitions.put(transitionId, transition);
        }
        return transition;
    }


}
