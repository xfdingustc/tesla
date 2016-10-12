package com.waylens.hachi.ui.community.feed;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.android.volley.VolleyError;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.response.MomentListResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.community.event.ScrollEvent;
import com.waylens.hachi.ui.entities.moment.MomentEx;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.fragments.Refreshable;
import com.waylens.hachi.ui.views.RecyclerViewExt;
import com.waylens.hachi.utils.ServerMessage;
import com.xfdingustc.rxutils.library.RxBus;

import butterknife.BindView;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/9/13.
 */
public class FeedFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener,
    Refreshable, FragmentNavigator {
    private static final String TAG = FeedFragment.class.getSimpleName();

    @Override
    protected String getRequestTag() {
        return TAG;
    }


    static final int DEFAULT_COUNT = 10;


    private static final int CHILD_SIGNUP_ENTRY = 0;
    private static final int CHILD_MOMENTS = 1;


    private FeedListAdapter mAdapter;


    private LinearLayoutManager mLinearLayoutManager;


    private long mCurrentCursor;



    @BindView(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @BindView(R.id.video_list_view)
    RecyclerViewExt mRvVideoList;

    @BindView(R.id.refresh_layout)
    SwipeRefreshLayout mRefreshLayout;


    @OnClick(R.id.btn_sign_up)
    public void onBtnSignupClicked() {
        AuthorizeActivity.launchForResult(getActivity(), MainActivity.REQUEST_CODE_SIGN_UP_FROM_MOMENTS);
    }


    public static FeedFragment newInstance() {
        Bundle args = new Bundle();
        FeedFragment fragment = new FeedFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new FeedListAdapter(getActivity());
        mLinearLayoutManager = new LinearLayoutManager(getActivity());

    }


    @Override
    public void onStart() {
        super.onStart();
        if (!SessionManager.getInstance().isLoggedIn()) {
            mViewAnimator.setDisplayedChild(CHILD_SIGNUP_ENTRY);
            Logger.t(TAG).d("show sign up entry");
        } else {
            if (mViewAnimator.getDisplayedChild() == CHILD_SIGNUP_ENTRY) {
                mViewAnimator.setDisplayedChild(CHILD_MOMENTS);
                Logger.t(TAG).d("show loading progress");
                onRefresh();
            }
        }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_feed, savedInstanceState);
        Logger.t(TAG).d("create feed view");
        mRvVideoList.setAdapter(mAdapter);
        mRvVideoList.setLayoutManager(mLinearLayoutManager);
        mRefreshLayout.setOnRefreshListener(this);
        mRvVideoList.setOnLoadMoreListener(new RecyclerViewExt.OnLoadMoreListener() {
            @Override
            public void loadMore() {
                loadFeed(mCurrentCursor, false);
            }
        });


        mRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.windowBackgroundDark);
        mRefreshLayout.setColorSchemeResources(R.color.style_color_accent, android.R.color.holo_green_light,
            android.R.color.holo_orange_light, android.R.color.holo_red_light);


        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCurrentCursor = 0;

        if (SessionManager.getInstance().isLoggedIn()) {
            loadFeed(mCurrentCursor, true);
        }
    }


    private void loadFeed(long cursor, final boolean isRefresh) {
        HachiApi hachiApi = HachiService.createHachiApiService();



        Observable<MomentListResponse> feedMoment = hachiApi.getMyFeed(cursor, DEFAULT_COUNT, Constants.PARAM_SORT_UPLOAD_TIME, true);

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
                });
            feedObservable = Observable.zip(recommendMoment, feedMoment, new Func2<MomentListResponse, MomentListResponse, MomentListResponse>() {
                @Override
                public MomentListResponse call(MomentListResponse recommendMoment, MomentListResponse feedMoment) {
                    feedMoment.moments.addAll(0, recommendMoment.moments);
                    return feedMoment;
                }
            });
        }


        feedObservable.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<MomentListResponse>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    Logger.t(TAG).d(e.toString());
                }

                @Override
                public void onNext(MomentListResponse momentListResponse) {
                    if (isRefresh) {
                        RxBus.getDefault().post(new ScrollEvent(false));
                    }
                    onLoadFeedSuccessful(momentListResponse, isRefresh);
                }
            });

    }


    private void onLoadFeedSuccessful(MomentListResponse momentList, boolean isRefresh) {
        mRefreshLayout.setRefreshing(false);


        if (isRefresh) {
            mAdapter.setMoments(momentList.moments);
        } else {
            mAdapter.addMoments(momentList.moments);
        }

        mRvVideoList.setIsLoadingMore(false);
        mCurrentCursor += momentList.moments.size();
        if (momentList.nextCursor != 0) {
            mAdapter.setHasMore(true);
            mCurrentCursor = momentList.nextCursor;
        } else {
            mRvVideoList.setEnableLoadMore(false);
            mAdapter.setHasMore(false);
        }

    }

    private void onLoadFeedFailed(VolleyError error) {
        mRefreshLayout.setRefreshing(false);
        mRvVideoList.setIsLoadingMore(false);
        ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
        showMessage(errorInfo.msgResID);
    }


    @Override
    public void onRefresh() {
        mCurrentCursor = 0;
        mRvVideoList.setEnableLoadMore(true);

        if (SessionManager.getInstance().isLoggedIn()) {
            loadFeed(mCurrentCursor, true);
        }
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
