package com.waylens.hachi.ui.community.feed;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.response.MomentListResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.community.event.MomentModifyEvent;
import com.waylens.hachi.ui.community.event.ScrollEvent;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.entities.moment.MomentEx;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.fragments.Refreshable;
import com.waylens.hachi.ui.views.RecyclerViewExt;
import com.waylens.hachi.utils.ServerErrorHelper;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.ThemeHelper;
import com.waylens.hachi.utils.VolleyUtil;
import com.xfdingustc.rxutils.library.RxBus;
import com.xfdingustc.rxutils.library.SimpleSubscribe;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;


public class MomentListFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener,
    Refreshable, FragmentNavigator {
    private static final String TAG = MomentListFragment.class.getSimpleName();
    static final int DEFAULT_COUNT = 10;

    static final String TAG_REQUEST_MY_FEED = "TAG_request.my.feed";
    static final String TAG_REQUEST_STAFF_PICKS = "TAG_request.staff.picks";

    private static final String FEED_TAG = "feed_tag";


    public static final int FEED_TAG_NEW_FEED = 1;
    public static final int FEED_TAG_LATEST = 2;
    public static final int FEED_TAG_STAFF_PICKS = 4;

    private static final int CHILD_SIGNUP_ENTRY = 0;
    private static final int CHILD_MOMENTS = 1;

    private Subscription mSubscription;

    private AbsMomentListAdapter mAdapter;

    private LinearLayoutManager mLinearLayoutManager;

    private long mCurrentCursor;

    private int mFeedTag;

    @BindView(R.id.video_list_view)
    RecyclerViewExt mRvVideoList;

    @BindView(R.id.refresh_layout)
    SwipeRefreshLayout mRefreshLayout;

    @BindView(R.id.view_animator)
    ViewAnimator mViewAnimator;


    @OnClick(R.id.btn_sign_up)
    public void onBtnSignupClicked() {
        AuthorizeActivity.launchForResult(getActivity(), MainActivity.REQUEST_CODE_SIGN_UP_FROM_MOMENTS);
    }




    public static MomentListFragment newInstance(int tag) {
        Bundle args = new Bundle();
        args.putInt(FEED_TAG, tag);
        MomentListFragment fragment = new MomentListFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mFeedTag = arguments.getInt(FEED_TAG, FEED_TAG_LATEST);
        }
        if (mFeedTag == FEED_TAG_NEW_FEED) {
            mAdapter = new FeedListAdapter(getActivity());
        } else {
            mAdapter = new MomentsListAdapter(getActivity());
        }
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        initEventHandler();
    }

    private void initEventHandler() {
        mSubscription = RxBus.getDefault().toObserverable(MomentModifyEvent.class)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleSubscribe<MomentModifyEvent>() {
                    @Override
                    public void onNext(MomentModifyEvent momentModifyEvent) {
                        handleMomentModifyEvent(momentModifyEvent);
                    }
                });
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_moment_list, savedInstanceState);
        mRvVideoList.setAdapter(mAdapter);
        mRvVideoList.setLayoutManager(mLinearLayoutManager);
        mRefreshLayout.setOnRefreshListener(this);

        if (ThemeHelper.isDarkTheme()) {
            mRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.windowBackgroundDark);
        }
        mRefreshLayout.setColorSchemeResources(R.color.style_color_accent, android.R.color.holo_green_light,
            android.R.color.holo_orange_light, android.R.color.holo_red_light);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (needSignin() && !SessionManager.getInstance().isLoggedIn()) {
            mViewAnimator.setDisplayedChild(CHILD_SIGNUP_ENTRY);
            Logger.t(TAG).d("show sign up entry");
        } else {
            if (mViewAnimator.getDisplayedChild() == CHILD_SIGNUP_ENTRY) {
                mViewAnimator.setDisplayedChild(CHILD_MOMENTS);
//                Logger.t(TAG).d("show loading progress");
                onRefresh();
            }
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCurrentCursor = 0;
        loadFeed(mCurrentCursor, true);
    }

    private void handleMomentModifyEvent(MomentModifyEvent event) {
        switch (event.eventType) {
            case MomentModifyEvent.LIKE_EVENT:
                Boolean like = (Boolean) event.what;
                int index = event.momentIndex;

                break;
            case MomentModifyEvent.COMMENT_EVENT:

                break;
            case MomentModifyEvent.DELETE_EVENT:

                break;
            case MomentModifyEvent.EDIT_EVENT:

                break;
            default:

        }
    }

    private void loadFeed(long cursor, final boolean isRefresh) {
        getMomentListObservable(cursor)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<MomentListResponse>() {
                @Override
                public void onNext(MomentListResponse momentListResponse) {
                    if (isRefresh) {
                        RxBus.getDefault().post(new ScrollEvent(false));
                    }
                    onLoadFeedSuccessful(momentListResponse, isRefresh);
                }

                @Override
                public void onError(Throwable e) {
                    onLoadMomentFailed(e);
                }
            });


    }

    @Override
    protected String getRequestTag() {
        switch (mFeedTag) {
            case FEED_TAG_STAFF_PICKS:
                return TAG_REQUEST_STAFF_PICKS;
            default:
                return "";
        }
    }

    private Observable<MomentListResponse> getMomentListObservable(long cursor) {
        HachiApi hachiApi = HachiService.createHachiApiService();
        switch (mFeedTag) {
            case FEED_TAG_LATEST:
                return hachiApi.getAllMomentsRx(cursor, DEFAULT_COUNT, "uploadtime_desc", null, true);
            case FEED_TAG_STAFF_PICKS:
                return hachiApi.getAllMomentsRx(cursor, DEFAULT_COUNT, "uploadtime_desc", "featured", true);
            case FEED_TAG_NEW_FEED:
                Observable<MomentListResponse> feedMoment = hachiApi.getMyFeed(cursor, DEFAULT_COUNT, Constants.PARAM_SORT_UPLOAD_TIME, true)
                    .subscribeOn(Schedulers.newThread());

                Observable<MomentListResponse> feedObservable;

                if (cursor != 0) {
                    feedObservable = feedMoment;
                } else {
                    Observable<MomentListResponse> recommendMoment = hachiApi.getRecommendedMomentsRx(1)
                        .map(new Func1<MomentListResponse, MomentListResponse>() {
                            @Override
                            public MomentListResponse call(MomentListResponse momentListResponse2) {
                                for (MomentEx momentEx : momentListResponse2.moments) {
                                    momentEx.moment.isRecommended = true;
                                }
                                return momentListResponse2;
                            }
                        })
                        .subscribeOn(Schedulers.newThread());
                    feedObservable = Observable.zip(recommendMoment, feedMoment, new Func2<MomentListResponse, MomentListResponse, MomentListResponse>() {
                        @Override
                        public MomentListResponse call(MomentListResponse recommendMoment, MomentListResponse feedMoment) {
                            feedMoment.moments.addAll(0, recommendMoment.moments);
                            return feedMoment;
                        }
                    });
                }
                return feedObservable;
        }

        return null;
    }

    private void onLoadFeedSuccessful(MomentListResponse response, boolean isRefresh) {
        mRefreshLayout.setRefreshing(false);
        if (isRefresh) {
            mAdapter.setMoments(response.moments);
        } else {
            mAdapter.addMoments(response.moments);
        }

        mRvVideoList.setIsLoadingMore(false);
        mCurrentCursor += response.moments.size();

        boolean hasMore;
        if (mFeedTag == FEED_TAG_NEW_FEED) {
            hasMore = (response.nextCursor != 0);
            mCurrentCursor = response.nextCursor;
        } else {
            hasMore = response.hasMore;
        }

        if (!hasMore) {
            mRvVideoList.setEnableLoadMore(false);
            mRvVideoList.setOnLoadMoreListener(null);
            mAdapter.setHasMore(false);
        } else {
            mAdapter.setHasMore(true);
            mRvVideoList.setOnLoadMoreListener(new RecyclerViewExt.OnLoadMoreListener() {
                @Override
                public void loadMore() {
                    loadFeed(mCurrentCursor, false);
                }
            });
        }

    }

    private void onLoadMomentFailed(Throwable e) {
        mRefreshLayout.setRefreshing(false);
        mRvVideoList.setIsLoadingMore(false);
        ServerErrorHelper.showErrorMessage(mRootView, e);
    }


    @Override
    public void onRefresh() {
        mCurrentCursor = 0;
        mRvVideoList.setEnableLoadMore(true);
        loadFeed(mCurrentCursor, true);
    }

    @Override
    public void enableRefresh(boolean enabled) {
        if (mRefreshLayout != null) {
            mRefreshLayout.setEnabled(enabled);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!mSubscription.isUnsubscribed()) {
            mSubscription.unsubscribe();
        }
    }

    public boolean needSignin() {
        return mFeedTag == FEED_TAG_NEW_FEED;
    }


    @Override
    public boolean onInterceptBackPressed() {
        Logger.t(TAG).d("back pressed");
        return false;
    }


}
