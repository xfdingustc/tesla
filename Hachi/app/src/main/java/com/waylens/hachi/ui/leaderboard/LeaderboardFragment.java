package com.waylens.hachi.ui.leaderboard;

/**
 * Created by lshw on 16/9/2.
 */

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.TransitionRes;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.pavlospt.roundedletterview.RoundedLetterView;
import com.lzy.widget.HexagonView;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.bean.LeaderBoardItem;
import com.waylens.hachi.rest.bean.Maker;
import com.waylens.hachi.rest.bean.Model;
import com.waylens.hachi.rest.bean.VehicleInfo;
import com.waylens.hachi.rest.response.RaceQueryResponse;
import com.waylens.hachi.ui.activities.UserProfileActivity;
import com.waylens.hachi.ui.community.MomentActivity;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.fragments.Refreshable;
import com.waylens.hachi.ui.views.AvatarView;
import com.waylens.hachi.ui.views.RecyclerViewExt;
import com.waylens.hachi.utils.AvatarHelper;
import com.waylens.hachi.utils.ServerErrorHelper;
import com.waylens.hachi.utils.SettingHelper;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.OnClick;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class LeaderboardFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener,
    Refreshable, FragmentNavigator {
    private static final String TAG = LeaderboardFragment.class.getSimpleName();

//    public final int UNIT_ENGLISH = 0;
//    public final int UNIT_METRIC = 1;

    public static final String RACING_CD6T = "RACING_CD6T";
    public static final String RACING_CD3T = "RACING_CD3T";
    public static final String RACING_AU6T = "RACING_AU6T";
    public static final String RACING_AU3T = "RACING_AU3T";

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

//    private int mUnits = UNIT_ENGLISH;

//    private int mRaceType = RACE_TYPE_30MPH;

//    private int mLeaderBoardStart;

//    private int mLeaderBoardEnd;

    private Long mUpper;

    private Long mLower;

    private VehicleInfo myVehicleInfo;

    private String mMaker;

    private String mModel;

    private List<Pair<Maker, Model>> mMakerModelList;

    private int mLeaderBoardItemCount;

    private List<View> popupViews = new ArrayList<>();

    private long splitTime30[] = {0, 2000, 4000, 8000, 80000};
    private long splitTime60[] = {0, 3000, 3500, 4000, 5000, 7000, 10000, 100000};
    private String headers[] = {"mode", "type", "All"};


    @BindView(R.id.header_view)
    View headerView;

    @BindView(R.id.container)
    ViewGroup rootContainer;

    @BindView(R.id.top_three)
    View topThree;

    @BindView(R.id.main_layout)
    FrameLayout mLayoutMain;

    @BindView(R.id.rlv_first)
    RoundedLetterView rlvFirst;

    @BindView(R.id.rlv_second)
    RoundedLetterView rlvSecond;

    @BindView(R.id.rlv_third)
    RoundedLetterView rlvThird;

    @BindView(R.id.btn_drop_down)
    ImageButton btnDropDown;

    @BindView(R.id.ll_filter)
    ViewGroup llFilter;

    @BindView(R.id.leaderboard_list_view)
    RecyclerViewExt mRvLeaderboardList;

    @BindView(R.id.refresh_layout)
    SwipeRefreshLayout mRefreshLayout;

    @BindView(R.id.my_avatar)
    AvatarView mMyAvatar;

    @BindView(R.id.my_name)
    TextView mMyName;

    @BindView(R.id.my_rank)
    TextView mMyRank;

    @BindView(R.id.my_vehicle_info)
    TextView mMyVehicleInfo;

    @BindView(R.id.my_race_time)
    TextView mMyRaceTime;

    @BindView(R.id.my_leaderboard_play)
    ImageView mMyLeaderBoardPlay;

    @BindView(R.id.layout_my_test)
    LinearLayout mMyTestLayout;

    @BindView(R.id.layout_no_data)
    LinearLayout mNoDataLayout;

    @BindView(R.id.clipMask)
    View clickMask;

    @BindView(R.id.first)
    HexagonView hvFirst;

    @BindView(R.id.firstName)
    TextView firstName;

    @BindView(R.id.firstVehicle)
    TextView firstVehicle;

    @BindView(R.id.second)
    HexagonView hvSecond;

    @BindView(R.id.secondName)
    TextView secondName;

    @BindView(R.id.secondVehicle)
    TextView secondVehicle;

    @BindView(R.id.third)
    HexagonView hvThird;

    @BindView(R.id.thirdName)
    TextView thirdName;

    @BindView(R.id.thirdVehicle)
    TextView thirdVehicle;

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
        if (llFilter.getVisibility() != View.VISIBLE) {
            TransitionManager.beginDelayedTransition(rootContainer,
                getTransition(R.transition.auto));
            llFilter.setVisibility(View.VISIBLE);
            btnDropDown.setRotation(180);
        } else {
            TransitionManager.beginDelayedTransition(rootContainer,
                getTransition(R.transition.auto));
            llFilter.setVisibility(View.GONE);
            btnDropDown.setRotation(0);

        }
    }


    public static LeaderboardFragment newInstance(int tag) {

        Bundle args = new Bundle();
        LeaderboardFragment fragment = new LeaderboardFragment();
        fragment.setArguments(args);
        return fragment;
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
        mRvLeaderboardList.setAdapter(mAdapter);
        mRvLeaderboardList.setLayoutManager(mLinearLayoutManager);
        mRefreshLayout.setOnRefreshListener(this);
        mRvLeaderboardList.setOnLoadMoreListener(new RecyclerViewExt.OnLoadMoreListener() {
            @Override
            public void loadMore() {
                //loadLeaderBoard(mCurrentCursor, false);
            }
        });

        setupLeaderBoardFilter();

        rlvFirst.setTextTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        rlvSecond.setTextTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        rlvThird.setTextTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));

        mRefreshLayout.setColorSchemeResources(R.color.style_color_accent, android.R.color.holo_green_light,
            android.R.color.holo_orange_light, android.R.color.holo_red_light);
        mMakerModelList = new ArrayList<>();

        mRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                ViewUtils.setPaddingTop(mLayoutMain, getToolbar().getHeight() + topThree.getHeight());
            }
        });
        return view;
    }

    private void setupLeaderBoardFilter() {
        setupModeGroudRv();
        setupSpeedGroupRv();
        setupTimeGroupRv(true);

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
        mSpeedAdapter.setSelected(getString(SettingHelper.isMetricUnit() ? R.string.kmh50 : R.string.mph30));
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
            mRefreshLayout.setRefreshing(true);
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
        Logger.t(TAG).d("mUpper" + mUpper + "mLower" + mLower);
        HachiService.createHachiApiService().queryRaceRx(mode,
            queryStart, queryEnd, mUpper, mLower, mMaker, mModel, mLeaderBoardItemCount)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<RaceQueryResponse>() {
                @Override
                public void onNext(RaceQueryResponse raceQueryResponse) {
                    mRefreshLayout.setRefreshing(false);
                    onHandleRaceQuery(raceQueryResponse, isRefresh);
                }

                @Override
                public void onError(Throwable e) {
                    mRefreshLayout.setRefreshing(false);
                    ServerErrorHelper.showErrorMessage(mRootView, e);
                }
            });

    }

    private void onHandleRaceQuery(RaceQueryResponse raceQueryResponse, boolean isRefresh) {
        if (llFilter.getVisibility() == View.VISIBLE) {
            TransitionManager.beginDelayedTransition(rootContainer,
                getTransition(R.transition.auto));
            llFilter.setVisibility(View.GONE);
            mRvLeaderboardList.scrollTo(0, 0);
            btnDropDown.setRotation(0);
        }
        if (raceQueryResponse.leaderboard == null) {
            return;
        }

        if (isRefresh) {
            List<LeaderBoardItem> leaderBoardItems = raceQueryResponse.leaderboard;
            int itemSize = leaderBoardItems.size();
            if (itemSize > 0) {
                setTopThreeUserInfo(leaderBoardItems.get(0), hvFirst, firstName, firstVehicle);
            }

            if (itemSize > 1) {
                setTopThreeUserInfo(leaderBoardItems.get(1), hvSecond, secondName, secondVehicle);
            }

            if (itemSize > 2) {
                setTopThreeUserInfo(leaderBoardItems.get(2), hvThird, thirdName, thirdVehicle);
            }

            if (itemSize > 3) {

                mAdapter.setMoments(raceQueryResponse.leaderboard.subList(3, itemSize), getRaceType(), mModeAdapter.getSelectedIndex());
            }
            if (raceQueryResponse.leaderboard.size() == 0) {
                mNoDataLayout.setVisibility(View.VISIBLE);
            } else {
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
                if (rank < raceQueryResponse.userRankings.get(i).rank) {
                    rank = raceQueryResponse.userRankings.get(i).rank;
                    bestRankIndex = i;
                }
            }
        }
        if (rank > 0 && bestRankIndex >= 0) {
//            mMyTestLayout.setVisibility(View.VISIBLE);
            initMyTestView(raceQueryResponse.userRankings.get(bestRankIndex));
            String maker = raceQueryResponse.userRankings.get(bestRankIndex).vehicle.vehicleMaker;
            String model = raceQueryResponse.userRankings.get(bestRankIndex).vehicle.vehicleModel;
            if (maker != null && model != null) {
                myVehicleInfo = new VehicleInfo();
                myVehicleInfo.vehicleMaker = maker;
                myVehicleInfo.vehicleModel = model;
            }
        } else {
            mMyTestLayout.setVisibility(View.GONE);
        }
        mRvLeaderboardList.setIsLoadingMore(false);
        mCurrentCursor += raceQueryResponse.leaderboard.size();

        mRvLeaderboardList.setEnableLoadMore(false);
        mAdapter.setHasMore(false);
    }


    private void setTopThreeUserInfo(final LeaderBoardItem item, final HexagonView avatarView, TextView tvUserName, TextView vehicleInfo) {
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


    private void initMyTestView(RaceQueryResponse.UserRankItem userRankItem) {
//        final SessionManager sessionManager = SessionManager.getInstance();
//        mMyAvatar.loadAvatar(sessionManager.getAvatarUrl(), sessionManager.getUserName());
//        mMyName.setText(sessionManager.getUserName());
//        if (userRankItem.vehicle.vehicleMaker != null) {
//            mMyVehicleInfo.setText(userRankItem.vehicle.vehicleMaker + " " + userRankItem.vehicle.vehicleModel + " " + userRankItem.vehicle.vehicleYear);
//        }
//        final MomentInfo.MomentBasicInfo moment = userRankItem.moment;
//        double raceTime = 0.0;
//        switch (mRaceType) {
//            case LeaderboardFragment.RACE_TYPE_30MPH:
//                if (mLeaderBoardMode == LeaderboardFragment.TEST_MODE_AUTO) {
//                    raceTime = (double) (moment.momentTimingInfo.t3_2) / 1000;
//                } else if (mLeaderBoardMode == LeaderboardFragment.TEST_MODE_COUNTDOWN) {
//                    raceTime = (double) (moment.momentTimingInfo.t3_1) / 1000;
//                }
//                break;
//            case LeaderboardFragment.RACE_TYPE_50KMH:
//                if (mLeaderBoardMode == LeaderboardFragment.TEST_MODE_AUTO) {
//                    raceTime = (double) (moment.momentTimingInfo.t4_2) / 1000;
//                } else if (mLeaderBoardMode == LeaderboardFragment.TEST_MODE_COUNTDOWN) {
//                    raceTime = (double) (moment.momentTimingInfo.t4_1) / 1000;
//                }
//                break;
//            case LeaderboardFragment.RACE_TYPE_60MPH:
//                if (mLeaderBoardMode == LeaderboardFragment.TEST_MODE_AUTO) {
//                    raceTime = (double) (moment.momentTimingInfo.t5_2) / 1000;
//                } else if (mLeaderBoardMode == LeaderboardFragment.TEST_MODE_COUNTDOWN) {
//                    raceTime = (double) (moment.momentTimingInfo.t5_1) / 1000;
//                }
//                break;
//            case LeaderboardFragment.RACE_TYPE_100KMH:
//                if (mLeaderBoardMode == LeaderboardFragment.TEST_MODE_AUTO) {
//                    raceTime = (double) (moment.momentTimingInfo.t6_2) / 1000;
//                } else if (mLeaderBoardMode == LeaderboardFragment.TEST_MODE_COUNTDOWN) {
//                    raceTime = (double) (moment.momentTimingInfo.t6_1) / 1000;
//                }
//                break;
//            default:
//                break;
//        }
//        NumberFormat formatter = new DecimalFormat("#0.00");
//        mMyRaceTime.setText(String.format(getString(R.string.race_time), formatter.format(raceTime)));
//        mMyRank.setText(String.valueOf(userRankItem.rank));
//
//        mMyAvatar.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!SessionManager.getInstance().isLoggedIn()) {
//                    AuthorizeActivity.launch(getActivity());
//                    return;
//                }
////                UserProfileActivity.launch(getActivity(), sessionManager.getUserId(), mMyAvatar);
//
//            }
//        });
//
//        mMyName.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!SessionManager.getInstance().isLoggedIn()) {
//                    AuthorizeActivity.launch(getActivity());
//                    return;
//                }
////                UserProfileActivity.launch(getActivity(), sessionManager.getUserId(), mMyAvatar);
//
//            }
//        });
//
//        mMyLeaderBoardPlay.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                MomentActivity.launch(getActivity(), moment.id, moment.thumbnail, mMyLeaderBoardPlay);
//            }
//        });


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
        if (mRefreshLayout != null) {
            mRefreshLayout.setEnabled(enabled);
        }
    }


    @Override
    public boolean onInterceptBackPressed() {
        Logger.t(TAG).d("back pressed");
        return false;
    }

    @Override
    public void onSelected() {

    }

    @Override
    public void onDeselected() {

    }

    @Override
    public void setupToolbar() {
        getToolbar().setTitle(R.string.leaderboard);
        super.setupToolbar();
    }

    private Transition getTransition(@TransitionRes int transitionId) {
        android.transition.Transition transition = transitions.get(transitionId);
        if (transition == null) {
            transition = TransitionInflater.from(getActivity()).inflateTransition(transitionId);
            transitions.put(transitionId, transition);
        }
        return transition;
    }
}
