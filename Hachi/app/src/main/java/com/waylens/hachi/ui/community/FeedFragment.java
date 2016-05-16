package com.waylens.hachi.ui.community;

import android.app.Fragment;
import android.app.FragmentManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.text.SpannableStringBuilder;
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
import com.waylens.hachi.ui.activities.UserProfileActivity;
import com.waylens.hachi.ui.entities.Comment;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.fragments.MomentPlayFragment;
import com.waylens.hachi.ui.fragments.Refreshable;
import com.waylens.hachi.ui.fragments.YouTubeFragment;
import com.waylens.hachi.ui.views.OnViewDragListener;
import com.waylens.hachi.ui.views.RecyclerViewExt;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.VolleyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.BindView;


/**
 * Created by Xiaofei on 2015/8/4.
 */
public class FeedFragment extends BaseFragment implements MomentsListAdapter.OnMomentActionListener,
    SwipeRefreshLayout.OnRefreshListener, Refreshable, FragmentNavigator, OnViewDragListener {
    private static final String TAG = FeedFragment.class.getSimpleName();
    static final int DEFAULT_COUNT = 10;

    static final String TAG_REQUEST_MY_FEED = "TAG_request.my.feed";
    static final String TAG_REQUEST_ME = "TAG_request.me";
    static final String TAG_REQUEST_MY_LIKE = "TAG_request.my.like";
    static final String TAG_REQUEST_STAFF_PICKS = "TAG_request.staff.picks";

    private static final String FEED_TAG = "feed_tag";

    public static final int FEED_TAG_MY_FEED = 0;
    public static final int FEED_TAG_ME = 1;
    public static final int FEED_TAG_LIKES = 2;
    public static final int FEED_TAG_STAFF_PICKS = 3;
    public static final int FEED_TAG_ALL = 4;

    @BindView(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @BindView(R.id.video_list_view)
    RecyclerViewExt mVideoListView;

    @BindView(R.id.refresh_layout)
    SwipeRefreshLayout mRefreshLayout;

    MomentsListAdapter mAdapter;

    RequestQueue mRequestQueue;

    LinearLayoutManager mLinearLayoutManager;

    Fragment mVideoFragment;

    int mCurrentCursor;

    private int mFeedTag;

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
        mAdapter = new MomentsListAdapter(getActivity(), null, getFragmentManager(), mRequestQueue, getResources());
        mAdapter.setOnMomentActionListener(this);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();


//        mCurrentCursor = 0;
//        loadFeed(mCurrentCursor, true);
    }

    @Override
    public void onStop() {
        super.onStop();
        mRequestQueue.cancelAll(getRequestTag());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_feed, savedInstanceState);
        mVideoListView.setAdapter(mAdapter);
        mVideoListView.setLayoutManager(mLinearLayoutManager);
        mRefreshLayout.setOnRefreshListener(this);
        mVideoListView.setOnLoadMoreListener(new RecyclerViewExt.OnLoadMoreListener() {
            @Override
            public void loadMore() {
                loadFeed(mCurrentCursor, false);
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCurrentCursor = 0;
        loadFeed(mCurrentCursor, true);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();

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

    String getRequestTag() {
        switch (mFeedTag) {
            case FEED_TAG_MY_FEED:
                return TAG_REQUEST_MY_FEED;
            case FEED_TAG_ME:
                return TAG_REQUEST_ME;
            case FEED_TAG_LIKES:
                return TAG_REQUEST_MY_LIKE;
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
            case FEED_TAG_LIKES:
                url = Constants.API_MOMENTS_MY_LIKE;
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

        mVideoListView.setIsLoadingMore(false);
        mCurrentCursor += momentList.size();
        if (!response.optBoolean("hasMore")) {
            mVideoListView.setEnableLoadMore(false);
        }

        if (mViewAnimator.getDisplayedChild() == 0) {
            mViewAnimator.setDisplayedChild(1);
        }

        /*TODO disable it? Richard
        for (int i = 0; i < momentList.size(); i++) {
            loadComment(momentList.get(i).id, i);
        }*/
    }

    void onLoadFeedFailed(VolleyError error) {
        mRefreshLayout.setRefreshing(false);
        mVideoListView.setIsLoadingMore(false);
        ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
        showMessage(errorInfo.msgResID);
    }

    public void loadComment(final long momentID, final int position) {
        if (momentID == Moment.INVALID_MOMENT_ID) {
            return;
        }
        String url = Constants.API_COMMENTS + String.format(Constants.API_COMMENTS_QUERY_STRING, momentID, 0, 3);
        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                refreshComment(momentID, position, response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
                showMessage(errorInfo.msgResID);
            }
        }).setTag(getRequestTag()));
    }

    void refreshComment(long momentID, int position, JSONObject response) {
        JSONArray jsonComments = response.optJSONArray("comments");
        if (jsonComments == null || jsonComments.length() == 0) {
            return;
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder();

        for (int i = jsonComments.length() - 1; i >= 0; i--) {
            Comment comment = Comment.fromJson(jsonComments.optJSONObject(i));
            ssb.append(comment.toSpannable());
            if (i > 0) {
                ssb.append("\n");
            }
        }
        mAdapter.updateMoment(ssb, position);
    }


    @Override
    public void onRefresh() {
        mCurrentCursor = 0;
        mVideoListView.setEnableLoadMore(true);
        loadFeed(mCurrentCursor, true);
    }

    @Override
    public void enableRefresh(boolean enabled) {
        if (mRefreshLayout != null) {
            mRefreshLayout.setEnabled(enabled);
        }
    }

    @Override
    public void onRequestVideoPlay(MomentViewHolder vh, Moment moment, int position) {
        FragmentManager mFragmentManager = getFragmentManager();
        if (mVideoFragment != null) {
            mFragmentManager.beginTransaction().remove(mVideoFragment).commit();
            mVideoFragment = null;
        }

        if (moment.type == Moment.TYPE_YOUTUBE) {
            YouTubeFragment youTubeFragment = YouTubeFragment.newInstance();
            youTubeFragment.setVideoId(moment.videoID);
            vh.videoFragment = youTubeFragment;
            mVideoFragment = youTubeFragment;
//            mFragmentManager.beginTransaction().replace(vh.fragmentContainer.getId(), youTubeFragment).commit();
        } else {
            MomentPlayFragment videoPlayFragment = MomentPlayFragment.newInstance(moment, this);
            vh.videoFragment = videoPlayFragment;
            mVideoFragment = videoPlayFragment;
            mFragmentManager.beginTransaction().replace(vh.fragmentContainer.getId(), videoPlayFragment).commit();
        }
        //vh.videoControl.setVisibility(View.GONE);
    }

    @Override
    public boolean onInterceptBackPressed() {
        if (YouTubeFragment.fullScreenFragment != null) {
            YouTubeFragment.fullScreenFragment.setFullScreen(false);
            return true;
        }

//        if (VideoPlayFragment.fullScreenPlayer != null) {
//            VideoPlayFragment.fullScreenPlayer.setFullScreen(false);
//            return true;
//        }
        return false;
    }

    @Override
    public void onStartDragging() {
        mVideoListView.setLayoutFrozen(true);
        mRefreshLayout.setEnabled(false);
    }

    @Override
    public void onStopDragging() {
        mVideoListView.setLayoutFrozen(false);
        mRefreshLayout.setEnabled(true);
    }

    public boolean isLoginRequired() {
        Bundle args = getArguments();
        if (args == null) {
            return false;
        }
        int tag = mFeedTag = args.getInt(FEED_TAG, FEED_TAG_MY_FEED);
        switch (tag) {
            case FEED_TAG_MY_FEED:
                return true;
            case FEED_TAG_ME:
                return true;
            case FEED_TAG_LIKES:
                return true;
            case FEED_TAG_STAFF_PICKS:
                return false;
            default:
                return false;
        }
    }
}
