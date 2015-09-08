package com.waylens.hachi.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.utils.ServerMessage;

import org.json.JSONObject;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2015/8/4.
 */
public class NotificationFragment extends BaseFragment {

    @Bind(R.id.comments_unread)
    View commentsUnreadView;

    @Bind(R.id.likes_unread)
    View likesUnreadView;

    RequestQueue mRequestQueue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRequestQueue = Volley.newRequestQueue(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createFragmentView(inflater, container, R.layout.fragment_notification,
                savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshNotifications();
    }

    @OnClick(R.id.row_comments)
    public void clickCommentsRow() {
        getFragmentManager().beginTransaction().replace(R.id.root_container, new NotificationCommentsFragment()).commit();
    }

    @OnClick(R.id.row_likes)
    public void clickLikes() {
        getFragmentManager().beginTransaction().replace(R.id.root_container, new NotificationLikesFragment()).commit();
    }

    private void refreshNotifications() {
        String qs = String.format(Constants.API_QS_COMMON, 0, 0);
        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET,
                Constants.API_NOTIFICATIONS_COMMENTS + qs,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        onLoadCommentsSuccessful(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        onLoadNotificationFailed(error);
                    }
                }));

        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET,
                Constants.API_NOTIFICATIONS_LIKES + qs,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        onLoadLikesSuccessful(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        onLoadNotificationFailed(error);
                    }
                }));
    }

    void onLoadLikesSuccessful(JSONObject response) {
        if (likesUnreadView == null) {
            return;
        }
        int likesCount = response.optInt("unreadCount");
        likesUnreadView.setVisibility(likesCount > 0 ? View.VISIBLE : View.GONE);
    }

    void onLoadNotificationFailed(VolleyError error) {
        ServerMessage.ErrorMsg errorMsg = ServerMessage.parseServerError(error);
        showMessage(errorMsg.msgResID);
    }

    void onLoadCommentsSuccessful(JSONObject response) {
        if (commentsUnreadView == null) {
            return;
        }
        int unreadCount = response.optInt("unreadCount");
        commentsUnreadView.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
    }
}
