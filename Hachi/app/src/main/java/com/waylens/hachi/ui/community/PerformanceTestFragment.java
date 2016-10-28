package com.waylens.hachi.ui.community;

/**
 * Created by lshw on 16/9/2.
 */

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.bean.LeaderBoardItem;
import com.waylens.hachi.rest.bean.Maker;
import com.waylens.hachi.rest.bean.Model;
import com.waylens.hachi.rest.bean.VehicleInfo;
import com.waylens.hachi.rest.body.RaceQueryBody;
import com.waylens.hachi.rest.response.MomentInfo;
import com.waylens.hachi.rest.response.RaceQueryResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.adapters.LeaderBoardAdapter;
import com.waylens.hachi.ui.adapters.ListDropDownAdapter;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.fragments.Refreshable;
import com.waylens.hachi.ui.views.AvatarView;
import com.waylens.hachi.ui.views.DropDownMenu;
import com.waylens.hachi.ui.views.RecyclerViewExt;
import com.waylens.hachi.utils.ServerErrorHelper;
import com.waylens.hachi.utils.ThemeHelper;
import com.waylens.hachi.utils.ViewUtils;
import com.xfdingustc.rxutils.library.SimpleSubscribe;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class PerformanceTestFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener,
    Refreshable, FragmentNavigator {
    private static final String TAG = PerformanceTestFragment.class.getSimpleName();

    public final int UNIT_ENGLISH = 0;
    public final int UNIT_METRIC = 1;

    public static final String RACING_CD6T = "RACING_CD6T";
    public static final String RACING_CD3T = "RACING_CD3T";
    public static final String RACING_AU6T = "RACING_AU6T";
    public static final String RACING_AU3T = "RACING_AU3T";


    public static final int RACE_TYPE_30MPH = 0;
    public static final int RACE_TYPE_60MPH = 1;
    public static final int RACE_TYPE_50KMH = 2;
    public static final int RACE_TYPE_100KMH = 3;

    public static final int TEST_MODE_AUTO = 0;
    public static final int TEST_MODE_COUNTDOWN = 1;

    private int mLeaderBoardMode = TEST_MODE_AUTO;

    private LeaderBoardAdapter mAdapter;
    private LinearLayoutManager mLinearLayoutManager;


    private int mCurrentCursor;

    private int mUnits = UNIT_ENGLISH;

    private int mRaceType = RACE_TYPE_30MPH;

    private int mLeaderBoardStart;

    private int mLeaderBoardEnd;

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

    private ListDropDownAdapter modeAdapter;

    private ListDropDownAdapter typeAdapterEnglish;

    private ListDropDownAdapter typeAdapterMetric;

    private ListDropDownAdapter modelAdapter;

    private ListDropDownAdapter groupAdapter60;

    private ListDropDownAdapter groupAdapter30;

    private ListDropDownAdapter currentGroupAdapter;

    @BindView(R.id.main_layout)
    FrameLayout mLayoutMain;

    @BindView(R.id.dropDownMenu)
    DropDownMenu mDropDownMenu;

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

    @OnClick(R.id.clipMask)
    public void onClickMaskClicked() {
        mDropDownMenu.closeMenu();
    }


    public static PerformanceTestFragment newInstance(int tag) {

        Bundle args = new Bundle();
        PerformanceTestFragment fragment = new PerformanceTestFragment();
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


    @Override
    public void onStart() {
        super.onStart();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_performance_test, savedInstanceState);
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


        if (ThemeHelper.isDarkTheme()) {
            mRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.windowBackgroundDark);
        }
        mRefreshLayout.setColorSchemeResources(R.color.style_color_accent, android.R.color.holo_green_light,
            android.R.color.holo_orange_light, android.R.color.holo_red_light);
        mMakerModelList = new ArrayList<>();

        mRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                ViewUtils.setPaddingTop(mLayoutMain, getToolbar().getHeight() + mDropDownMenu.getHeight());
            }
        });
        return view;
    }

    private void setupLeaderBoardFilter() {
        final ListView modeView = new ListView(getActivity());
        modeView.setDividerHeight(0);
        modeAdapter = new ListDropDownAdapter(getActivity(), Arrays.asList(getResources().getStringArray(R.array.test_mode)));
        modeView.setAdapter(modeAdapter);


        final ListView typeView = new ListView(getActivity());
        typeView.setDividerHeight(0);
        typeAdapterEnglish = new ListDropDownAdapter(getActivity(), Arrays.asList(getResources().getStringArray(R.array.race_type_english)));
        typeView.setAdapter(typeAdapterEnglish);
        typeAdapterMetric = new ListDropDownAdapter(getActivity(), Arrays.asList(getResources().getStringArray(R.array.race_type_metric)));
        groupAdapter30 = new ListDropDownAdapter(getActivity(), Arrays.asList(getResources().getStringArray(R.array.race_time_030)));
        groupAdapter60 = new ListDropDownAdapter(getActivity(), Arrays.asList(getResources().getStringArray(R.array.race_time_060)));
        currentGroupAdapter = groupAdapter30;
        final ListView groupView = new ListView(getActivity());
        groupView.setDividerHeight(0);
        groupView.setAdapter(currentGroupAdapter);

        popupViews.add(modeView);
        popupViews.add(typeView);
        popupViews.add(groupView);

        TextView contentView = new TextView(getActivity());
        contentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        contentView.setAlpha(0);

        mDropDownMenu.setDropDownMenu(Arrays.asList(headers), popupViews, contentView);
        mDropDownMenu.setTabTextAt((String) modeAdapter.getItem(1), 0);
        mDropDownMenu.setOnMenuOpenClicked(new DropDownMenu.OnMenuOpenListener() {
            @Override
            public void onMenuOpen() {
                clickMask.setVisibility(View.VISIBLE);
            }

            @Override
            public void onMenuClosed() {
                clickMask.setVisibility(View.GONE);
            }
        });
        modeAdapter.setCheckItem(1);

        modeView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                modeAdapter.setCheckItem(position);
                Logger.t(TAG).d(modeAdapter.getItem(position));
                mDropDownMenu.setTabText((String) modeAdapter.getItem(position));
                mDropDownMenu.closeMenu();
                int mode = -1;
                switch (position) {
                    case 0:
                        mode = TEST_MODE_COUNTDOWN;
                        mDropDownMenu.setImageHeader(getResources().getDrawable(R.drawable.btn_leaderboard_count_down));
                        break;
                    case 1:
                        mode = TEST_MODE_AUTO;
                        mDropDownMenu.setImageHeader(getResources().getDrawable(R.drawable.btn_leaderboard_auto));
                        break;
                    default:
                        break;
                }
                if (mode >= 0 && mode != mLeaderBoardMode) {
                    mLeaderBoardMode = mode;
                    loadLeaderBoard(0, true);
                }
            }
        });

        mDropDownMenu.setTabTextAt((String) typeAdapterEnglish.getItem(0), 1);
        typeAdapterEnglish.setCheckItem(0);
        typeView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                int type = -1;
                if (mUnits == UNIT_ENGLISH) {
                    switch (position) {
                        case 0:
                            type = RACE_TYPE_30MPH;
                            break;
                        case 1:
                            type = RACE_TYPE_60MPH;
                            break;
                        case 2:
                            type = RACE_TYPE_50KMH;
                            typeView.setAdapter(typeAdapterMetric);
                            typeAdapterMetric.setCheckItem(0);
                            mDropDownMenu.setTabText((String) typeAdapterMetric.getItem(0));
                            mUnits = UNIT_METRIC;
                            break;
                    }
                    if (position < 2) {
                        typeAdapterEnglish.setCheckItem(position);
                        mDropDownMenu.setTabText((String) typeAdapterEnglish.getItem(position));
                    }
                } else if (mUnits == UNIT_METRIC) {
                    switch (position) {
                        case 0:
                            type = RACE_TYPE_50KMH;
                            break;
                        case 1:
                            type = RACE_TYPE_100KMH;
                            break;
                        case 2:
                            type = RACE_TYPE_30MPH;
                            typeView.setAdapter(typeAdapterEnglish);
                            typeAdapterEnglish.setCheckItem(0);
                            mDropDownMenu.setTabText((String) typeAdapterEnglish.getItem(0));
                            mUnits = UNIT_ENGLISH;
                            break;
                        default:
                            break;
                    }
                    if (position < 2) {
                        typeAdapterMetric.setCheckItem(position);
                        mDropDownMenu.setTabText((String) typeAdapterMetric.getItem(position));
                    }
                }
                if (type != mRaceType) {
                    if (position == 1) {
                        currentGroupAdapter = groupAdapter60;
                    } else {
                        currentGroupAdapter = groupAdapter30;
                    }
                    groupView.setAdapter(currentGroupAdapter);
                    currentGroupAdapter.setCheckItem(0);
                    mDropDownMenu.setTabTextAt((String) currentGroupAdapter.getItem(0), 2);
                    mUpper = null;
                    mLower = null;
                    mRaceType = type;
                    loadLeaderBoard(0, true);
                }
                mDropDownMenu.closeMenu();
            }
        });

        mDropDownMenu.setTabTextAt((String) groupAdapter30.getItem(0), 2);

        groupView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0) {
                    mUpper = null;
                    mLower = null;
                } else if (mRaceType == RACE_TYPE_30MPH || mRaceType == RACE_TYPE_50KMH) {
                    mLower = splitTime30[i - 1];
                    mUpper = splitTime30[i];
                } else {
                    mLower = splitTime60[i - 1];
                    mUpper = splitTime60[i];
                }
                currentGroupAdapter.setCheckItem(i);
                mDropDownMenu.setTabText((String) currentGroupAdapter.getItem(i));
                mDropDownMenu.closeMenu();
                loadLeaderBoard(0, true);
                onRefresh();
            }
        });
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
        final RaceQueryBody raceQueryBody = new RaceQueryBody();
        raceQueryBody.mode = mLeaderBoardMode;
        if (mLeaderBoardMode == TEST_MODE_AUTO) {
            mLeaderBoardStart = 2;
        } else if (mLeaderBoardMode == TEST_MODE_COUNTDOWN) {
            mLeaderBoardStart = 1;
        }
        raceQueryBody.start = mLeaderBoardStart;
        switch (mRaceType) {
            case RACE_TYPE_30MPH:
                mLeaderBoardEnd = 3;
                break;
            case RACE_TYPE_60MPH:
                mLeaderBoardEnd = 5;
                break;
            case RACE_TYPE_50KMH:
                mLeaderBoardEnd = 4;
                break;
            case RACE_TYPE_100KMH:
                mLeaderBoardEnd = 6;
                break;
            default:
                break;
        }
        raceQueryBody.end = mLeaderBoardEnd;
        raceQueryBody.count = mLeaderBoardItemCount = 100;
        Logger.t(TAG).d("mUpper" + mUpper + "mLower" + mLower);
        HachiService.createHachiApiService().queryRaceRx(raceQueryBody.mode,
            raceQueryBody.start, raceQueryBody.end, mUpper, mLower, mMaker, mModel, raceQueryBody.count)
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
        if (raceQueryResponse.leaderboard == null) {
            return;
        }

        if (isRefresh) {

            mAdapter.setMoments(raceQueryResponse.leaderboard, mRaceType, mLeaderBoardMode);
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


    private void initMyTestView(RaceQueryResponse.UserRankItem userRankItem) {
        final SessionManager sessionManager = SessionManager.getInstance();
        mMyAvatar.loadAvatar(sessionManager.getAvatarUrl(), sessionManager.getUserName());
        mMyName.setText(sessionManager.getUserName());
        if (userRankItem.vehicle.vehicleMaker != null) {
            mMyVehicleInfo.setText(userRankItem.vehicle.vehicleMaker + " " + userRankItem.vehicle.vehicleModel + " " + userRankItem.vehicle.vehicleYear);
        }
        final MomentInfo.MomentBasicInfo moment = userRankItem.moment;
        double raceTime = 0.0;
        switch (mRaceType) {
            case PerformanceTestFragment.RACE_TYPE_30MPH:
                if (mLeaderBoardMode == PerformanceTestFragment.TEST_MODE_AUTO) {
                    raceTime = (double) (moment.momentTimingInfo.t3_2) / 1000;
                } else if (mLeaderBoardMode == PerformanceTestFragment.TEST_MODE_COUNTDOWN) {
                    raceTime = (double) (moment.momentTimingInfo.t3_1) / 1000;
                }
                break;
            case PerformanceTestFragment.RACE_TYPE_50KMH:
                if (mLeaderBoardMode == PerformanceTestFragment.TEST_MODE_AUTO) {
                    raceTime = (double) (moment.momentTimingInfo.t4_2) / 1000;
                } else if (mLeaderBoardMode == PerformanceTestFragment.TEST_MODE_COUNTDOWN) {
                    raceTime = (double) (moment.momentTimingInfo.t4_1) / 1000;
                }
                break;
            case PerformanceTestFragment.RACE_TYPE_60MPH:
                if (mLeaderBoardMode == PerformanceTestFragment.TEST_MODE_AUTO) {
                    raceTime = (double) (moment.momentTimingInfo.t5_2) / 1000;
                } else if (mLeaderBoardMode == PerformanceTestFragment.TEST_MODE_COUNTDOWN) {
                    raceTime = (double) (moment.momentTimingInfo.t5_1) / 1000;
                }
                break;
            case PerformanceTestFragment.RACE_TYPE_100KMH:
                if (mLeaderBoardMode == PerformanceTestFragment.TEST_MODE_AUTO) {
                    raceTime = (double) (moment.momentTimingInfo.t6_2) / 1000;
                } else if (mLeaderBoardMode == PerformanceTestFragment.TEST_MODE_COUNTDOWN) {
                    raceTime = (double) (moment.momentTimingInfo.t6_1) / 1000;
                }
                break;
            default:
                break;
        }
        NumberFormat formatter = new DecimalFormat("#0.00");
        mMyRaceTime.setText(String.format(getString(R.string.race_time), formatter.format(raceTime)));
        mMyRank.setText(String.valueOf(userRankItem.rank));

        mMyAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!SessionManager.getInstance().isLoggedIn()) {
                    AuthorizeActivity.launch(getActivity());
                    return;
                }
//                UserProfileActivity.launch(getActivity(), sessionManager.getUserId(), mMyAvatar);

            }
        });

        mMyName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!SessionManager.getInstance().isLoggedIn()) {
                    AuthorizeActivity.launch(getActivity());
                    return;
                }
//                UserProfileActivity.launch(getActivity(), sessionManager.getUserId(), mMyAvatar);

            }
        });

        mMyLeaderBoardPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MomentActivity.launch(getActivity(), moment.id, moment.thumbnail, mMyLeaderBoardPlay);
            }
        });


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
    public void setupToolbar() {
        getToolbar().setTitle(R.string.leaderboard);
        super.setupToolbar();
    }
}
