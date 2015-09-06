package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.waylens.hachi.ui.adapters.Moment;
import com.waylens.hachi.ui.adapters.Notification;
import com.waylens.hachi.ui.adapters.NotificationCommentsAdapter;
import com.waylens.hachi.utils.ServerMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Richard on 9/6/15.
 */
public class NotificationCommentsFragment extends BaseFragment {

    private static final int DEFAULT_COUNT = 10;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @Bind(R.id.refresh_layout)
    SwipeRefreshLayout mRefreshLayout;

    @Bind(R.id.comment_list)
    RecyclerView mNotificationListView;

    RequestQueue mRequestQueue;

    int mCurrentCursor;

    NotificationCommentsAdapter mAdapter;

    LinearLayoutManager mLayoutManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRequestQueue = Volley.newRequestQueue(getActivity());
        mAdapter = new NotificationCommentsAdapter(null);
        mLayoutManager = new LinearLayoutManager(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_notification_comments, savedInstanceState);
        mNotificationListView.setAdapter(mAdapter);
        mNotificationListView.setLayoutManager(mLayoutManager);
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
    }

    @OnClick(R.id.btn_back)
    public void back() {
        getFragmentManager().beginTransaction().remove(this).commit();
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
    }

    void onLoadCommentsSuccessful(JSONObject response, boolean isRefresh) {
        if (mRefreshLayout != null) {
            mRefreshLayout.setRefreshing(false);
        }

        JSONArray jsonNotifications = response.optJSONArray("notifications");
        if (jsonNotifications == null) {
            return;
        }
        ArrayList<Notification> notifications = new ArrayList<>();
        for (int i = 0; i < jsonNotifications.length(); i++) {
            notifications.add(Notification.fromJson(jsonNotifications.optJSONObject(i)));
        }

        mAdapter.setNotifications(notifications);
        if (mViewAnimator != null) {
            mViewAnimator.setDisplayedChild(1);
        }

    }
}
