package com.waylens.hachi.ui.community;

/**
 * Created by lshw on 16/9/2.
 */

import android.app.Activity;
import android.content.Context;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.RequestQueue;

import com.bumptech.glide.Glide;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.RaceQueryBody;
import com.waylens.hachi.rest.response.MomentInfo;
import com.waylens.hachi.rest.response.RaceQueryResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.UserProfileActivity;
import com.waylens.hachi.ui.adapters.LeaderBoardAdapter;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.entities.User;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.fragments.Refreshable;
import com.waylens.hachi.ui.views.RecyclerViewExt;
import com.waylens.hachi.utils.VolleyUtil;


import java.io.IOException;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;


public class PerformanceTestFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener,
        Refreshable, FragmentNavigator {
    private static final String TAG = PerformanceTestFragment.class.getSimpleName();
    static final int DEFAULT_COUNT = 10;

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

    private HachiApi hachiApi;

    private RequestQueue mRequestQueue;

    private LinearLayoutManager mLinearLayoutManager;

    private int mCurrentCursor;

    private int mUnits = UNIT_ENGLISH;

    private int mRaceType = RACE_TYPE_60MPH;

    private int mLeaderBoardStart;

    private int mLeaderBoardEnd;

    private int mLeaderBoardItemCount;

    @BindView(R.id.spinner1)
    Spinner mSpinnerTestMode;

    @BindView(R.id.spinner2)
    Spinner mSpinnerRaceType;

    @BindView(R.id.leaderboard_list_view)
    RecyclerViewExt mRvLeaderboardList;

    @BindView(R.id.refresh_layout)
    SwipeRefreshLayout mRefreshLayout;

    @BindView(R.id.test_mode_pic)
    ImageView mTestModePic;

    @BindView(R.id.my_avatar)
    CircleImageView mMyAvatar;

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
        hachiApi = HachiService.createHachiApiService();
        mRequestQueue = VolleyUtil.newVolleyRequestQueue(getActivity());
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
                loadLeaderBoard(mCurrentCursor, false);
            }
        });
        ArrayAdapter<String> adapterTestMode = new ArrayAdapter<>(getActivity(), R.layout.item_spinner_test, getResources().getStringArray(R.array.test_mode));
        adapterTestMode.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerTestMode.setAdapter(adapterTestMode);
        mSpinnerTestMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int mode = -1;
                switch (position) {
                    case 0:
                        mode = TEST_MODE_COUNTDOWN;
                        mTestModePic.setImageResource(R.drawable.btn_leaderboard_count_down);

                        break;
                    case 1:
                        mode = TEST_MODE_AUTO;
                        mTestModePic.setImageResource(R.drawable.btn_leaderboard_auto);
                        break;
                    default:
                        break;
                }
                if (mode >= 0 && mode != mLeaderBoardMode) {
                    mLeaderBoardMode = mode;
                    loadLeaderBoard(0, true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        ArrayAdapter<String> adapterRaceType = new ArrayAdapter<>(getActivity(), R.layout.item_spinner_test, getResources().getStringArray(R.array.race_type_english));
        adapterRaceType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerRaceType.setAdapter(adapterRaceType);
        mSpinnerRaceType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                int type = -1;
                if (mUnits == UNIT_ENGLISH) {
                    switch (i) {
                        case 0:
                            type = RACE_TYPE_30MPH;
                            break;
                        case 1:
                            type = RACE_TYPE_60MPH;
                            break;
                        case 2:
                            type = RACE_TYPE_100KMH;
                            ArrayAdapter<String> adapterRaceType = new ArrayAdapter<>(getActivity(), R.layout.item_spinner_test, getResources().getStringArray(R.array.race_type_metric));
                            adapterRaceType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            mSpinnerRaceType.setAdapter(adapterRaceType);
                            mSpinnerRaceType.setSelection(1);
                            mUnits = UNIT_METRIC;
                            break;
                    }
                } else if(mUnits == UNIT_METRIC) {
                    switch (i) {
                        case 0:
                            type = RACE_TYPE_50KMH;
                            break;
                        case 1:
                            type = RACE_TYPE_100KMH;
                            break;
                        case 2:
                            type = RACE_TYPE_60MPH;
                            ArrayAdapter<String> adapterRaceType = new ArrayAdapter<>(getActivity(), R.layout.item_spinner_test, getResources().getStringArray(R.array.race_type_english));
                            adapterRaceType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            mSpinnerRaceType.setAdapter(adapterRaceType);
                            mSpinnerRaceType.setSelection(1);
                            mUnits = UNIT_ENGLISH;
                            break;
                        default:
                            break;
                    }

                }
                if (type != mRaceType) {
                    mRaceType = type;
                    loadLeaderBoard(0, true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mSpinnerRaceType.setSelection(1);
        mRefreshLayout.setColorSchemeResources(R.color.style_color_primary, android.R.color.holo_green_light,
                android.R.color.holo_orange_light, android.R.color.holo_red_light);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCurrentCursor = 0;
        loadLeaderBoard(mCurrentCursor, true);
    }

    private void loadLeaderBoard(int cursor, final boolean isRefresh) {
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
        Call<RaceQueryResponse> raceQueryResponseCall = hachiApi.queryRace(raceQueryBody.mode, raceQueryBody.start, raceQueryBody.end, raceQueryBody.count);
        raceQueryResponseCall.enqueue(new Callback<RaceQueryResponse>() {
            @Override
            public void onResponse(Call<RaceQueryResponse> call, retrofit2.Response<RaceQueryResponse> response) {
                mRefreshLayout.setRefreshing(false);
                if (response.isSuccessful()) {
                    Logger.t(TAG).d("get RaceQueryResponse");
/*                    try {
                        Logger.t(TAG).d(response.raw().body().string());
                    } catch (IOException e) {
                        Logger.t(TAG).d(e.getMessage());
                    }*/
                    RaceQueryResponse raceQueryResponse = response.body();
                    if (raceQueryResponse.leaderboard == null) {
                        return;
                    }
                    ArrayList<Moment> momentList = new ArrayList<>();
                    for (int i = 0; i < raceQueryResponse.leaderboard.size(); i++) {
                        Moment moment = raceQueryResponse.leaderboard.get(i).moment;
                        moment.owner = new User();
                        MomentInfo.Owner owner = raceQueryResponse.leaderboard.get(i).owner;
                        moment.owner.userID = owner.userID;
                        moment.owner.avatarUrl = owner.avatarUrl;
                        moment.owner.userName = owner.userName;
                        momentList.add(raceQueryResponse.leaderboard.get(i).moment);
                    }

                    Logger.t(TAG).d("moment list size:" + momentList.size());
                    if (isRefresh) {
                        mAdapter.setMoments(momentList, mRaceType, mLeaderBoardMode);
                        if (momentList.size() == 0) {
                            mNoDataLayout.setVisibility(View.VISIBLE);
                        } else {
                            mNoDataLayout.setVisibility(View.INVISIBLE);
                        }
                    } else {
                        mAdapter.addMoments(momentList);
                    }
                    int bestRankIndex = -1;
                    int rank = -1;
                    for (int i = 0; i < raceQueryResponse.userRankings.size(); i++) {
                        if (rank <= 0) {
                            rank = raceQueryResponse.userRankings.get(i).rank;
                            bestRankIndex = i;
                        } else {
                            if (rank > raceQueryResponse.userRankings.get(i).rank) {
                                rank = raceQueryResponse.userRankings.get(i).rank;
                                bestRankIndex = i;
                            }
                        }
                    }
                    if (rank > 0 && bestRankIndex >= 0) {
                        mMyTestLayout.setVisibility(View.VISIBLE);
                        initMyTestView(raceQueryResponse.userRankings.get(bestRankIndex));
                    } else {
                        mMyTestLayout.setVisibility(View.GONE);
                    }

                    mRvLeaderboardList.setIsLoadingMore(false);
                    mCurrentCursor += momentList.size();

                    mRvLeaderboardList.setEnableLoadMore(false);
                    mAdapter.setHasMore(false);
                } else {
                    Logger.t(TAG).d("fail to get RaceQueryResponse");
                    Logger.t(TAG).d(response.message());
                    Logger.t(TAG).d(response.code());
                }
            }

            @Override
            public void onFailure(Call<RaceQueryResponse> call, Throwable t) {
                Logger.t(TAG).d(t.getMessage());
            }
        });
    }


    private void initMyTestView(RaceQueryResponse.UserRankItem userRankItem) {
        final SessionManager sessionManager = SessionManager.getInstance();
        Glide.with(getActivity())
                .load(SessionManager.getInstance().getAvatarUrl())
                .placeholder(R.drawable.menu_profile_photo_default)
                .crossFade()
                .dontAnimate()
                .into(mMyAvatar);
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
        mMyRaceTime.setText(raceTime + "s");
        mMyRank.setText(String.valueOf(userRankItem.rank));

        mMyAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!SessionManager.getInstance().isLoggedIn()) {
                    AuthorizeActivity.launch(getActivity());
                    return;
                }
                UserProfileActivity.launch(getActivity(), sessionManager.getUserId());

            }
        });

        mMyName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!SessionManager.getInstance().isLoggedIn()) {
                    AuthorizeActivity.launch(getActivity());
                    return;
                }
                UserProfileActivity.launch(getActivity(), sessionManager.getUserId());

            }
        });

        mMyLeaderBoardPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MomentActivity.launch((BaseActivity) getActivity(), moment.id, moment.thumbnail, mMyLeaderBoardPlay);
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
}
