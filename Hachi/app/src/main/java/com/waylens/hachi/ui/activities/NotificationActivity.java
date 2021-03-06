package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.rest.IHachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.bean.Notification;
import com.waylens.hachi.rest.response.NotificationResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.adapters.NotificationAdapter;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.views.RecyclerViewExt;
import com.waylens.hachi.utils.ServerErrorHelper;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.VolleyUtil;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import butterknife.BindView;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func4;
import rx.schedulers.Schedulers;

/**
 * Created by xiaofei on 2015/8/4.
 */
public class NotificationActivity extends BaseActivity {
    public static final String TAG = NotificationActivity.class.getSimpleName();
    public static final int DEFAULT_COUNT = 20;

    @BindView(R.id.notification_list)
    RecyclerViewExt mRvNotificationList;

    @BindView(R.id.refresh_layout)
    SwipeRefreshLayout mRefreshLayout;

    private RequestQueue mRequestQueue;

    private Map<Long, NotiEvent> mUnreadEventMap;

    private long mCommentCursor = 0;

    private long mLikeCursor = 0;

    private long mFollowCursor = 0;

    private long mShareCursor = 0;

    private NotificationAdapter mAdapter;

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SessionManager.getInstance().isLoggedIn()) {
            AuthorizeActivity.launch(this);
            finish();
        }
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
        mAdapter = new NotificationAdapter(new ArrayList<Notification>(), this, new NotificationAdapter.OnListItemClickListener() {
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

        mRvNotificationList.setLayoutManager(new LinearLayoutManager(this));

        mRefreshLayout.setColorSchemeResources(R.color.style_color_accent, android.R.color.holo_green_light,
            android.R.color.holo_orange_light, android.R.color.holo_red_light);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadNotificationsRx(true);
            }
        });
    }


    @Override
    public void onStart() {
        super.onStart();
        mRefreshLayout.setRefreshing(true);
        loadNotificationsRx(true);
    }

    private void loadNotificationsRx(final boolean isRefresh) {
        IHachiApi hachiApi = HachiService.createHachiApiService();
        Observable<NotificationResponse> commentObservable = hachiApi.getCommentNotificationRx(mCommentCursor, DEFAULT_COUNT)
            .doOnNext(new Action1<NotificationResponse>() {
                @Override
                public void call(NotificationResponse notificationResponse) {
                    mCommentCursor = notificationResponse.nextCursor;
                    for (Notification notification : notificationResponse.notifications) {
                        notification.notificationType = Notification.NOTIFICATION_TYPE_COMMENT;
                    }
                }
            });

        Observable<NotificationResponse> likeObservable = hachiApi.getLikeNotificationRx(mLikeCursor, DEFAULT_COUNT)
            .doOnNext(new Action1<NotificationResponse>() {
                @Override
                public void call(NotificationResponse notificationResponse) {
                    mLikeCursor = notificationResponse.nextCursor;
                    for (Notification notification : notificationResponse.notifications) {
                        notification.notificationType = Notification.NOTIFICATION_TYPE_LIKE;
                    }
                }
            });
        Observable<NotificationResponse> followObservable = hachiApi.getFollowNotificationRx(mCommentCursor, DEFAULT_COUNT)
            .doOnNext(new Action1<NotificationResponse>() {
                @Override
                public void call(NotificationResponse notificationResponse) {
                    mFollowCursor = notificationResponse.nextCursor;
                    for (Notification notification : notificationResponse.notifications) {
                        notification.notificationType = Notification.NOTIFICATION_TYPE_FOLLOW;
                    }
                }
            });

        Observable<NotificationResponse> shareObservable = hachiApi.getShareNotificationRx(mShareCursor, DEFAULT_COUNT)
            .doOnNext(new Action1<NotificationResponse>() {
                @Override
                public void call(NotificationResponse notificationResponse) {
                    mShareCursor = notificationResponse.nextCursor;
                    for (Notification notification : notificationResponse.notifications) {
                        notification.notificationType = Notification.NOTIFICATION_TYPE_SHARE;
                    }
                }
            });

        Observable.zip(commentObservable, likeObservable, followObservable, shareObservable,
            new Func4<NotificationResponse, NotificationResponse, NotificationResponse, NotificationResponse, NotificationResponse>() {

                @Override
                public NotificationResponse call(NotificationResponse comment, NotificationResponse like, NotificationResponse follow, NotificationResponse share) {
                    NotificationResponse zipedResponse = new NotificationResponse();
                    zipedResponse.notifications = new ArrayList<>();
                    zipedResponse.notifications.addAll(comment.notifications);
                    zipedResponse.notifications.addAll(like.notifications);
                    zipedResponse.notifications.addAll(follow.notifications);
                    zipedResponse.notifications.addAll(share.notifications);
                    Collections.sort(zipedResponse.notifications, new Comparator<Notification>() {
                        @Override
                        public int compare(Notification o1, Notification o2) {
                            if (o1.getCreateTime() <= o2.getCreateTime()) {
                                return 1;
                            } else {
                                return -1;
                            }
                        }
                    });
                    return zipedResponse;
                }
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<NotificationResponse>() {
                @Override
                public void onNext(NotificationResponse notificationResponse) {
                    mRefreshLayout.setRefreshing(false);
                    mAdapter.addNotifications(notificationResponse.notifications, isRefresh);

                    mRvNotificationList.setIsLoadingMore(false);
                    if (mCommentCursor > 0 || mFollowCursor > 0 || mLikeCursor > 0) {
                        mRvNotificationList.setEnableLoadMore(true);
                        mAdapter.setHasMore(true);
                        mRvNotificationList.setOnLoadMoreListener(new RecyclerViewExt.OnLoadMoreListener() {
                            @Override
                            public void loadMore() {
                                loadNotificationsRx(false);
                            }
                        });
                    } else {
                        mAdapter.setHasMore(false);
                    }
                }

                @Override
                public void onError(Throwable e) {
                    mRefreshLayout.setRefreshing(false);
                    ServerErrorHelper.showErrorMessage(mRvNotificationList, e);
                }
            });

    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();

        getToolbar().setNavigationIcon(R.drawable.ic_arrow_back);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.launch(NotificationActivity.this);
                finish();
            }
        });
        getToolbar().setTitle(R.string.notification);

    }

    void markRead() {
        /*JSONArray ids = new JSONArray();
        JSONArray types = new JSONArray();
        JSONObject params = new JSONObject();
        final ArrayList<Long> markReadIDs = new ArrayList<>();
        for (Map.Entry<Long, NotiEvent> entry : mUnreadEventMap.entrySet()) {
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
                        for (long eventID : markReadIDs) {
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
            }));*/
    }

    public class NotiEvent {
        public boolean isRead;
        public Constants.EventType eventType;
    }

    @Override
    public void onBackPressed() {
        MainActivity.launch(this);
        finish();
    }
}
