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
import com.waylens.hachi.ui.community.event.ScrollEvent;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.fragments.Refreshable;
import com.waylens.hachi.ui.views.RecyclerViewExt;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.VolleyUtil;
import com.xfdingustc.rxutils.library.RxBus;
import com.xfdingustc.rxutils.library.SimpleSubscribe;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.BindView;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class MomentListFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener,
    Refreshable, FragmentNavigator {
    private static final String TAG = MomentListFragment.class.getSimpleName();
    static final int DEFAULT_COUNT = 10;

    static final String TAG_REQUEST_MY_FEED = "TAG_request.my.feed";
    static final String TAG_REQUEST_STAFF_PICKS = "TAG_request.staff.picks";

    private static final String FEED_TAG = "feed_tag";


    public static final int FEED_TAG_LATEST = 2;
    public static final int FEED_TAG_STAFF_PICKS = 4;


    private MomentsListAdapter mAdapter;


    private LinearLayoutManager mLinearLayoutManager;


    private int mCurrentCursor;

    private int mFeedTag;


    @BindView(R.id.video_list_view)
    RecyclerViewExt mRvVideoList;

    @BindView(R.id.refresh_layout)
    SwipeRefreshLayout mRefreshLayout;


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
        mAdapter = new MomentsListAdapter(getActivity());
        mLinearLayoutManager = new LinearLayoutManager(getActivity());

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_moment_list, savedInstanceState);
        mRvVideoList.setAdapter(mAdapter);
        mRvVideoList.setLayoutManager(mLinearLayoutManager);
        mRefreshLayout.setOnRefreshListener(this);


        mRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.windowBackgroundDark);
        mRefreshLayout.setColorSchemeResources(R.color.style_color_accent, android.R.color.holo_green_light,
            android.R.color.holo_orange_light, android.R.color.holo_red_light);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCurrentCursor = 0;
        loadFeed(mCurrentCursor, true);

    }


    private void loadFeed(int cursor, final boolean isRefresh) {
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

    private Observable<MomentListResponse> getMomentListObservable(int cursor) {
        HachiApi hachiApi = HachiService.createHachiApiService();
        switch (mFeedTag) {
            case FEED_TAG_LATEST:
                return hachiApi.getAllMomentsRx(cursor, DEFAULT_COUNT, "uploadtime_desc", null, true);
            case FEED_TAG_STAFF_PICKS:
                return hachiApi.getAllMomentsRx(cursor, DEFAULT_COUNT, "uploadtime_desc", "featured", true);
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
        if (!response.hasMore) {
            mRvVideoList.setEnableLoadMore(false);
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
        Snackbar.make(mRootView, e.getMessage(), Snackbar.LENGTH_SHORT).show();
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
    public boolean onInterceptBackPressed() {
        Logger.t(TAG).d("back pressed");
        return false;
    }


}
