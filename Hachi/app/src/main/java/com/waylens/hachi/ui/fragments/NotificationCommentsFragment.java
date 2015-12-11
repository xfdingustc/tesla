package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
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
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.entities.APIFilter;
import com.waylens.hachi.ui.entities.CommentEvent;
import com.waylens.hachi.ui.adapters.NotificationCommentsAdapter;
import com.waylens.hachi.ui.views.RecyclerViewExt;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.VolleyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Richard on 9/6/15.
 */
public class NotificationCommentsFragment extends BaseFragment implements RecyclerViewExt.OnLoadMoreListener,
        FragmentNavigator{

    private static final int DEFAULT_COUNT = 10;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @Bind(R.id.refresh_layout)
    SwipeRefreshLayout mRefreshLayout;

    @Bind(R.id.comment_list)
    RecyclerViewExt mNotificationListView;

    RequestQueue mRequestQueue;

    int mCurrentCursor;

    NotificationCommentsAdapter mAdapter;

    LinearLayoutManager mLayoutManager;

    ArrayList<Long> mUnreadEventIDs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRequestQueue = VolleyUtil.newVolleyRequestQueue(getActivity());
        mAdapter = new NotificationCommentsAdapter(null);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mUnreadEventIDs = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_notification_comments, savedInstanceState);
        mNotificationListView.setAdapter(mAdapter);
        mNotificationListView.setLayoutManager(mLayoutManager);
        mNotificationListView.setOnLoadMoreListener(this);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadNotifications(0, true);
            }
        });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadNotifications(0, true);
        View appBarLayout = ((BaseActivity)getActivity()).getAppBarLayout();
        if (appBarLayout != null) {
            appBarLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        View appBarLayout = ((BaseActivity)getActivity()).getAppBarLayout();
        if (appBarLayout != null) {
            appBarLayout.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onDestroyView() {
        mRequestQueue.cancelAll(new APIFilter(Constants.API_NOTIFICATIONS_COMMENTS));
        super.onDestroyView();
    }

    @OnClick(R.id.btn_back)
    public void onBack() {
        getFragmentManager().beginTransaction().remove(this).commit();
    }

    @Override
    public boolean onInterceptBackPressed() {
        onBack();
        return true;
    }

    void loadNotifications(int cursor, final boolean isRefresh) {
        String url = Constants.API_NOTIFICATIONS_COMMENTS + String.format(Constants.API_QS_COMMON, cursor, DEFAULT_COUNT);
        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        onLoadCommentsSuccessful(response, isRefresh);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        onLoadNotificationFailed(error);
                    }
                }));
    }

    void onLoadNotificationFailed(VolleyError error) {
        ServerMessage.ErrorMsg errorMsg = ServerMessage.parseServerError(error);
        showMessage(errorMsg.msgResID);
        if (mRefreshLayout != null) {
            mRefreshLayout.setRefreshing(false);
        }
        mNotificationListView.setIsLoadingMore(false);
    }

    void onLoadCommentsSuccessful(JSONObject response, boolean isRefresh) {
        if (mRefreshLayout != null) {
            mRefreshLayout.setRefreshing(false);
        }

        JSONArray jsonNotifications = response.optJSONArray("notifications");
        if (jsonNotifications == null) {
            return;
        }
        ArrayList<CommentEvent> commentEvents = new ArrayList<>();
        for (int i = 0; i < jsonNotifications.length(); i++) {
            CommentEvent commentEvent = CommentEvent.fromJson(jsonNotifications.optJSONObject(i));
            commentEvents.add(commentEvent);
            mUnreadEventIDs.add(commentEvent.eventID);
        }

        mAdapter.addNotifications(commentEvents, isRefresh);

        if (mViewAnimator != null) {
            mViewAnimator.setDisplayedChild(1);
        }

        mCurrentCursor = response.optInt("nextCursor");
        mNotificationListView.setEnableLoadMore(mCurrentCursor > 0);
        mNotificationListView.setIsLoadingMore(false);
        markRead();
    }

    @Override
    public void loadMore() {
        loadNotifications(mCurrentCursor, false);
    }

    void markRead() {
        JSONArray ids = new JSONArray(mUnreadEventIDs);
        JSONObject params = new JSONObject();
        try {
            JSONArray eventTypes = new JSONArray();
            eventTypes.put(Constants.EventType.COMMENT_MOMENT.name());
            eventTypes.put(Constants.EventType.REFER_USER.name());
            params.put("eventTypes", eventTypes);
            params.put("eventIDs", ids);
        } catch (Exception e) {
            Log.e("test", "", e);
        }
        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.POST, Constants.API_COMMENTS_MARK_READ, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response.optBoolean("result")) {
                            mUnreadEventIDs.clear();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ServerMessage.ErrorMsg errorMsg = ServerMessage.parseServerError(error);
                        Log.e("test", "MSG: " + getString(errorMsg.msgResID));
                    }
                }));
    }
}
