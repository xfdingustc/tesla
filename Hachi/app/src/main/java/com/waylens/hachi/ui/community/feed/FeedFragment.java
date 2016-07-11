package com.waylens.hachi.ui.community.feed;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.fragments.Refreshable;
import com.waylens.hachi.ui.views.RecyclerViewExt;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.VolleyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;


public class FeedFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener,
    Refreshable, FragmentNavigator {
    private static final String TAG = FeedFragment.class.getSimpleName();
    static final int DEFAULT_COUNT = 10;

    static final String TAG_REQUEST_MY_FEED = "TAG_request.my.feed";
    static final String TAG_REQUEST_ME = "TAG_request.me";
    static final String TAG_REQUEST_MY_LIKE = "TAG_request.my.like";
    static final String TAG_REQUEST_STAFF_PICKS = "TAG_request.staff.picks";

    private static final String FEED_TAG = "feed_tag";

    private static final int CHILD_SIGNUP_ENTRY = 0;
    private static final int CHILD_LOADING_PROGRESS = 1;
    private static final int CHILD_MOMENTS = 2;

    public static final int FEED_TAG_MY_FEED = 0;
    public static final int FEED_TAG_ME = 1;
    public static final int FEED_TAG_LATEST = 2;
    public static final int FEED_TAG_STAFF_PICKS = 4;
    public static final int FEED_TAG_ALL = 5;


    private MomentsListAdapter mAdapter;

    private RequestQueue mRequestQueue;

    private LinearLayoutManager mLinearLayoutManager;


    int mCurrentCursor;

    private int mFeedTag;

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




    public static FeedFragment newInstance(int tag) {

        Bundle args = new Bundle();
        args.putInt(FEED_TAG, tag);
        FeedFragment fragment = new FeedFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mFeedTag = arguments.getInt(FEED_TAG, FEED_TAG_MY_FEED);
        }
        mRequestQueue = VolleyUtil.newVolleyRequestQueue(getActivity());
        mAdapter = new MomentsListAdapter(getActivity(), null);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());

    }


    @Override
    public void onStop() {
        super.onStop();
        mRequestQueue.cancelAll(getRequestTag());
    }

    @Override
    public void onStart() {
        super.onStart();
//        Logger.t(TAG).d("onStart is calling");
//        Logger.t(TAG).d(Integer.toString(mFeedTag));
//        Logger.t(TAG).d(Boolean.toString(isLoginRequired()));
//        Logger.t(TAG).d(Boolean.toString(SessionManager.getInstance().isLoggedIn()));
        if (this.isLoginRequired() && !SessionManager.getInstance().isLoggedIn()) {
            mViewAnimator.setDisplayedChild(CHILD_SIGNUP_ENTRY);
            Logger.t(TAG).d("show sign up entry");
        }
        if (this.isLoginRequired() && SessionManager.getInstance().isLoggedIn()) {
            if (mViewAnimator.getDisplayedChild() == CHILD_SIGNUP_ENTRY) {
                mViewAnimator.setDisplayedChild(CHILD_LOADING_PROGRESS);
                Logger.t(TAG).d("show loading progress");
                onRefresh();

            }

        }
    }



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_feed, savedInstanceState);
        mRvVideoList.setAdapter(mAdapter);
        mRvVideoList.setLayoutManager(mLinearLayoutManager);
        mRefreshLayout.setOnRefreshListener(this);
        mRvVideoList.setOnLoadMoreListener(new RecyclerViewExt.OnLoadMoreListener() {
            @Override
            public void loadMore() {
                loadFeed(mCurrentCursor, false);
            }
        });
        if (this.isLoginRequired()) {
            mViewAnimator.setDisplayedChild(CHILD_SIGNUP_ENTRY);
        } else {
            mViewAnimator.setDisplayedChild(CHILD_LOADING_PROGRESS);
        }

        mRefreshLayout.setColorSchemeResources(R.color.style_color_primary, android.R.color.holo_green_light,
            android.R.color.holo_orange_light, android.R.color.holo_red_light);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCurrentCursor = 0;
        if (!this.isLoginRequired()) {
            loadFeed(mCurrentCursor, true);
        }
        if (this.isLoginRequired() && SessionManager.getInstance().isLoggedIn()) {
            loadFeed(mCurrentCursor, true);
        }
    }


    private void loadFeed(int cursor, final boolean isRefresh) {
        String url = getFeedURL(cursor);
        if (url == null) {
            return;
        }
        Logger.t(TAG).d("Load url: " + url);
        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET, url,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    onLoadFeedSuccessful(response, isRefresh);
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    onLoadFeedFailed(error);
                }
            }).setTag(getRequestTag()));
    }

    private String getRequestTag() {
        switch (mFeedTag) {
            case FEED_TAG_MY_FEED:
                return TAG_REQUEST_MY_FEED;
            case FEED_TAG_ME:
                return TAG_REQUEST_ME;
            case FEED_TAG_STAFF_PICKS:
                return TAG_REQUEST_STAFF_PICKS;
            default:
                return "";
        }
    }

    private String getFeedURL(int cursor) {
        String url = null;
        switch (mFeedTag) {
            case FEED_TAG_MY_FEED:
                url = Constants.API_MOMENTS_MY_FEED;
                break;
            case FEED_TAG_ME:
                url = Constants.API_MOMENTS_ME;
                break;
            case FEED_TAG_LATEST:
                url = Constants.API_MOMENTS;
                break;
            case FEED_TAG_STAFF_PICKS:
                url = Constants.API_MOMENTS_FEATURED;
                break;
            case FEED_TAG_ALL:
                url = Constants.API_MOMENTS;
                break;
        }
        if (url != null) {
            Uri uri = Uri.parse(url).buildUpon()
                .appendQueryParameter(Constants.API_MOMENTS_PARAM_CURSOR, String.valueOf(cursor))
                .appendQueryParameter(Constants.API_MOMENTS_PARAM_COUNT, String.valueOf(DEFAULT_COUNT))
                .appendQueryParameter(Constants.API_MOMENTS_PARAM_ORDER, Constants.PARAM_SORT_UPLOAD_TIME)
                .build();
            return uri.toString();
        } else {
            return null;
        }
    }

    private void onLoadFeedSuccessful(JSONObject response, boolean isRefresh) {
        mRefreshLayout.setRefreshing(false);
//        Logger.t(TAG).json(response.toString());
        JSONArray jsonMoments = response.optJSONArray("moments");
        if (jsonMoments == null) {
            return;
        }
        ArrayList<Moment> momentList = new ArrayList<>();
        for (int i = 0; i < jsonMoments.length(); i++) {
            momentList.add(Moment.fromJson(jsonMoments.optJSONObject(i)));
        }
        if (isRefresh) {
            mAdapter.setMoments(momentList);
        } else {
            mAdapter.addMoments(momentList);
        }

        mRvVideoList.setIsLoadingMore(false);
        mCurrentCursor += momentList.size();
        if (!response.optBoolean("hasMore")) {
            mRvVideoList.setEnableLoadMore(false);
        }

        if (mViewAnimator.getDisplayedChild() == CHILD_LOADING_PROGRESS) {
            mViewAnimator.setDisplayedChild(CHILD_MOMENTS);
        }

        /*TODO disable it? Richard
        for (int i = 0; i < momentList.size(); i++) {
            loadComment(momentList.get(i).id, i);
        }*/
    }

    void onLoadFeedFailed(VolleyError error) {
        mRefreshLayout.setRefreshing(false);
        mRvVideoList.setIsLoadingMore(false);
        ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
        showMessage(errorInfo.msgResID);
    }


    @Override
    public void onRefresh() {
        mCurrentCursor = 0;
        mRvVideoList.setEnableLoadMore(true);
        if (!this.isLoginRequired()) {
            loadFeed(mCurrentCursor, true);
        }
        if (this.isLoginRequired() && SessionManager.getInstance().isLoggedIn()) {
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


    public boolean isLoginRequired() {
        /*Bundle args = getArguments();
        if (args == null) {
            return false;
        }*/
        int tag = mFeedTag;
        switch (tag) {
            case FEED_TAG_MY_FEED:
                return true;
            case FEED_TAG_ME:
                return true;
            case FEED_TAG_STAFF_PICKS:
                return false;
            default:
                return false;
        }
    }

}
