package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.LoginActivity;
import com.waylens.hachi.ui.adapters.Comment;
import com.waylens.hachi.ui.adapters.Moment;
import com.waylens.hachi.ui.adapters.MomentsRecyclerAdapter;
import com.waylens.hachi.ui.views.RecyclerViewExt;
import com.waylens.hachi.utils.ServerMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2015/8/4.
 */
public class HomeFragment extends BaseFragment implements MomentsRecyclerAdapter.OnCommentMomentListener,
        MomentsRecyclerAdapter.OnLikeMomentListener, SwipeRefreshLayout.OnRefreshListener, Refreshable {

    static final int DEFAULT_COUNT = 10;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @Bind(R.id.video_list_view)
    RecyclerViewExt mVideoListView;

    @Bind(R.id.refresh_layout)
    SwipeRefreshLayout mRefreshLayout;

    MomentsRecyclerAdapter mAdapter;

    RequestQueue mRequestQueue;

    LinearLayoutManager mLinearLayoutManager;

    int mCurrentCursor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRequestQueue = Volley.newRequestQueue(getActivity());
        mAdapter = new MomentsRecyclerAdapter(null, getFragmentManager(), mRequestQueue, getResources());
        mAdapter.setOnCommentMomentListener(this);
        mAdapter.setOnLikeMomentListener(this);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_home, savedInstanceState);
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
    public void onStart() {
        super.onStart();
        mCurrentCursor = 0;
        loadFeed(mCurrentCursor, true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRequestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }

    void loadFeed(int cursor, final boolean isRefresh) {
        String url = Constants.API_MOMENTS + String.format(Constants.API_QS_MOMENTS, Constants.PARAM_SORT_UPLOAD_TIME, cursor, DEFAULT_COUNT);
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
                }));
    }

    void onLoadFeedSuccessful(JSONObject response, boolean isRefresh) {
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

        for (int i = 0; i < momentList.size(); i++) {
            loadComment(momentList.get(i).id, i);
        }
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
        }));
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
    public void onCommentMoment(Moment moment, int position) {
        if (!SessionManager.getInstance().isLoggedIn()) {
            LoginActivity.launch(getActivity());
            return;
        }
        CommentsFragment fragment = new CommentsFragment();
        Bundle args = new Bundle();
        args.putLong(CommentsFragment.ARG_MOMENT_ID, moment.id);
        args.putInt(CommentsFragment.ARG_MOMENT_POSITION, position);
        fragment.setArguments(args);
        getFragmentManager().beginTransaction().add(R.id.root_container, fragment).commit();
    }

    @Override
    public void onLikeMoment(Moment moment, boolean isCancel) {
        if (!SessionManager.getInstance().isLoggedIn()) {
            LoginActivity.launch(getActivity());
            return;
        }
        likeVideo(moment, isCancel);
    }

    void likeVideo(final Moment moment, final boolean isCancel) {
        JSONObject params = new JSONObject();
        try {
            params.put("momentID", moment.id);
            params.put("cancel", isCancel);
        } catch (JSONException e) {
            Log.e("test", "", e);
        }

        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.POST, Constants.API_MOMENT_LIKE,
                params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        moment.isLiked = !isCancel;
                        moment.likesCount = response.optInt("count");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
                        showMessage(errorInfo.msgResID);
                        Log.e("test", "Error: " + error);
                    }
                }));
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
}
