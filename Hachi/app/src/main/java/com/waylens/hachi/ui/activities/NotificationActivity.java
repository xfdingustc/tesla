package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.View;
import android.widget.ViewAnimator;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.ui.adapters.NotificationAdapter;
import com.waylens.hachi.ui.entities.CommentEvent;
import com.waylens.hachi.ui.entities.FollowEvent;
import com.waylens.hachi.ui.entities.LikeEvent;
import com.waylens.hachi.ui.entities.NotificationEvent;
import com.waylens.hachi.ui.fragments.NotificationCommentsFragment;
import com.waylens.hachi.ui.fragments.NotificationLikesFragment;
import com.waylens.hachi.ui.views.RecyclerViewExt;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.VolleyUtil;

import org.json.JSONArray;
import org.json.JSONObject;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import butterknife.BindView;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func0;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

/**
 * Created by xiaofei on 2015/8/4.
 */
public class NotificationActivity extends BaseActivity implements RecyclerViewExt.OnLoadMoreListener{
    public static final String TAG = NotificationActivity.class.getSimpleName();
    public static final int DEFAULT_COUNT = 10;

    @BindView(R.id.notification_view_animator)
    ViewAnimator mNotificationViewAnimator;

    @BindView(R.id.notification_list)
    RecyclerViewExt mRvNotificationList;

    @BindView(R.id.refresh_layout)
    SwipeRefreshLayout mRefreshLayout;

    private RequestQueue mRequestQueue;

    private Map<Long, NotiEvent> mUnreadEventMap;

    private ArrayList<CommentEvent> mCommentList;

    private ArrayList<LikeEvent> mLikeList;

    private ArrayList<FollowEvent> mFollowList;

    private int mCommentCursor = 0;

    private int mLikeCursor = 0;

    private int mFollowCursor = 0;

    ArrayList<NotificationEvent> mNotificationList;

    NotificationAdapter mAdapter;

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRequestQueue = VolleyUtil.newVolleyRequestQueue(this);
        mUnreadEventMap = new ConcurrentHashMap<>();
        init();
    }

    @Override
    protected void init() {
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_notification);
        setupToolbar();
        Logger.t(TAG).d("initial view");
        mAdapter = new NotificationAdapter(null, this, new NotificationAdapter.OnListItemClickListener() {
            @Override
            public void onItemClicked(long eventID) {
                NotiEvent notiEvent = mUnreadEventMap.get(eventID);
                if (notiEvent != null) {
                    notiEvent.isRead = true;
                    mUnreadEventMap.put(eventID, notiEvent);
                }
                markRead();
            }
        });
        mRvNotificationList.setAdapter(mAdapter);
        mRvNotificationList.setOnLoadMoreListener(this);
        mRvNotificationList.setLayoutManager(new LinearLayoutManager(this));
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadNotifications(true);
            }
        });
    }


    @Override
    public void onStart() {
        super.onStart();
        loadNotifications(true);
    }

    private Observable<Void> LoadComments(boolean isRefresh) {
        if (isRefresh) {mCommentCursor = 0;}
        final String qs = String.format(Constants.API_QS_COMMON, mCommentCursor, DEFAULT_COUNT);
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                try {
                    if (mCommentCursor < 0) {
                        subscriber.onCompleted();
                        return;
                    }
                    RequestFuture<JSONObject > future = RequestFuture.newFuture();
                    AuthorizedJsonRequest request = new AuthorizedJsonRequest(Request.Method.GET,
                            Constants.API_NOTIFICATIONS_COMMENTS + qs, future, future);
                    mRequestQueue.add(request);
                    JSONObject response = future.get();

                    Logger.t(TAG).d("load comments successfully");

                    JSONArray jsonNotifications = response.optJSONArray("notifications");
                    if (jsonNotifications == null) {
                        return;
                    }
                    mCommentList = new ArrayList<>();
                    for (int i = 0; i < jsonNotifications.length(); i++) {
                        CommentEvent commentEvent = CommentEvent.fromJson(jsonNotifications.optJSONObject(i));
                        mCommentList.add(commentEvent);
                        NotiEvent notiEvent = new NotiEvent();
                        notiEvent.isRead = commentEvent.isRead;
                        notiEvent.eventType = Constants.EventType.COMMENT_MOMENT;
                        if (!notiEvent.isRead) {
                            mUnreadEventMap.put(commentEvent.eventID, notiEvent);
                        }
                    }
                    int nextCursor = response.optInt("nextCursor");
                    Logger.t(TAG).d("next comment cursor " + nextCursor);
                    if (nextCursor != 0) {
                        mCommentCursor = nextCursor;
                    } else {
                        mCommentCursor = -1;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    Logger.t(TAG).d(e.getMessage());
                }
                subscriber.onCompleted();

            }
        }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<Void> LoadLikes(boolean isRefresh) {
        if (isRefresh) {mLikeCursor = 0;}
        final String qs = String.format(Constants.API_QS_COMMON, mLikeCursor, DEFAULT_COUNT);
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                try {
                    if (mLikeCursor < 0) {
                        subscriber.onCompleted();
                        return;
                    }
                    RequestFuture<JSONObject > future = RequestFuture.newFuture();
                    AuthorizedJsonRequest request = new AuthorizedJsonRequest(Request.Method.GET,
                            Constants.API_NOTIFICATIONS_LIKES + qs, future, future);
                    mRequestQueue.add(request);
                    JSONObject response = future.get();
                    Logger.t(TAG).d("load likes successfully");
                    JSONArray jsonNotifications = response.optJSONArray("notifications");
                    if (jsonNotifications == null) {
                        return;
                    }
                    mLikeList = new ArrayList<>();
                    for (int i = 0; i < jsonNotifications.length(); i++) {
                        LikeEvent likeEvent = LikeEvent.fromJson(jsonNotifications.optJSONObject(i));
                        mLikeList.add(likeEvent);
                        NotiEvent notiEvent = new NotiEvent();
                        notiEvent.isRead = likeEvent.isRead;
                        notiEvent.eventType = Constants.EventType.LIKE_MOMENT;
                        if (!notiEvent.isRead) {
                            mUnreadEventMap.put(likeEvent.eventID, notiEvent);
                        }
                    }
                    int nextCursor = response.optInt("nextCursor");
                    Logger.t(TAG).d("next like cursor: " + nextCursor);
                    if (nextCursor != 0) {
                        mLikeCursor = nextCursor;
                    } else {
                        mLikeCursor = -1;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    Logger.t(TAG).d(e.getMessage());
                }
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<Void> LoadFollows(boolean isRefresh) {
        if (isRefresh) {   mFollowCursor = 0; }
        final String qs = String.format(Constants.API_QS_COMMON, mFollowCursor, DEFAULT_COUNT);
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                try {
                    if (mFollowCursor < 0) {
                        subscriber.onCompleted();
                        return;
                    }
                    RequestFuture<JSONObject > future = RequestFuture.newFuture();
                    AuthorizedJsonRequest request = new AuthorizedJsonRequest(Request.Method.GET,
                            Constants.API_NOTIFICATIONS_FOLLOWS + qs, future, future);
                    mRequestQueue.add(request);
                    JSONObject response = future.get();
                    Logger.t(TAG).d("load follows successfully");
                    JSONArray jsonNotifications = response.optJSONArray("notifications");
                    if (jsonNotifications == null) {
                        return;
                    }
                    mFollowList = new ArrayList<>();
                    for (int i = 0; i < jsonNotifications.length(); i++) {
                        FollowEvent followEvent = FollowEvent.fromJson(jsonNotifications.optJSONObject(i));
                        mFollowList.add(followEvent);
                        NotiEvent notiEvent = new NotiEvent();
                        notiEvent.isRead = followEvent.isRead;
                        notiEvent.eventType = Constants.EventType.FOLLOW_USER;
                        if (!notiEvent.isRead) {
                            mUnreadEventMap.put(followEvent.eventID, notiEvent);
                        }
                    }
                    int nextCursor = response.optInt("nextCursor");
                    Logger.t(TAG).d("next follow cursor: " + nextCursor);
                    if (nextCursor != 0) {
                        mFollowCursor = nextCursor;
                    } else {
                        mFollowCursor = -1;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    Logger.t(TAG).d(e.getMessage());
                }
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }

    private void loadNotifications(final boolean isRefresh) {
        Observable.merge(LoadComments(isRefresh), LoadLikes(isRefresh), LoadFollows(isRefresh))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Void>() {
                @Override
                public void onCompleted() {
                    if (mRefreshLayout != null) {
                        mRefreshLayout.setRefreshing(false);
                    }
                    Logger.t(TAG).d("on completed!");
                    mNotificationList = new ArrayList<>();
                    mNotificationList.addAll(mCommentList);
                    mNotificationList.addAll(mLikeList);
                    mNotificationList.addAll(mFollowList);
                    mCommentList.clear();
                    mLikeList.clear();
                    mFollowList.clear();
                    Logger.t(TAG).d("notification list length = " + mNotificationList.size());
                    mAdapter.addNotifications(mNotificationList, isRefresh);
                    if (mNotificationViewAnimator != null) {
                        mNotificationViewAnimator.setDisplayedChild(1);
                    }

                    Logger.t(TAG).d("c f l: " + mCommentCursor + " " + mFollowCursor + " " + mLikeCursor);
                    mRvNotificationList.setEnableLoadMore(Math.max(mCommentCursor > mFollowCursor ?
                            mCommentCursor : mFollowCursor, mLikeCursor) > 0);
                    mRvNotificationList.setIsLoadingMore(false);

                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(Void aVoid) {

                }
            });

/*        String qs = String.format(Constants.API_QS_COMMON, 0, 0);
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
                }).setTag(TAG));

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
                }).setTag(TAG));

        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET,
                Constants.API_NOTIFICATIONS_FOLLOWS + qs,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        onLoadFollowsSuccessful(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        onLoadNotificationFailed(error);
                    }
                }).setTag(TAG));*/
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();

        getToolbar().setNavigationIcon(R.drawable.navbar_back);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        getToolbar().setTitle(R.string.notification);

    }

    @Override
    public void loadMore() {
        loadNotifications(false);
    }

    void markRead() {
        JSONArray ids = new JSONArray();
        JSONArray types = new JSONArray();
        JSONObject params = new JSONObject();
        final ArrayList<Long> markReadIDs = new ArrayList<>();
        for (Map.Entry<Long, NotiEvent> entry:mUnreadEventMap.entrySet()) {
            if (entry.getValue().isRead) {
                markReadIDs.add(entry.getKey());
                ids.put(entry.getKey());
                types.put(entry.getValue().eventType.name());
            }
        }
        try {
            params.put("eventTypes", types);
            params.put("eventIDs", ids);
        } catch (Exception e) {
            Logger.t(TAG).d(e.getMessage());
        }
        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.POST, Constants.API_COMMENTS_MARK_READ, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response.optBoolean("result")) {
                            for(long eventID : markReadIDs) {
                                mUnreadEventMap.remove(eventID);
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ServerMessage.ErrorMsg errorMsg = ServerMessage.parseServerError(error);
                    }
                }));
    }

    public class NotiEvent {
        public boolean isRead;
        public Constants.EventType eventType;
    }

}