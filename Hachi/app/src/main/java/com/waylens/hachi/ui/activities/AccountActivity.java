package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.ViewAnimator;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.gcm.RegistrationIntentService;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.adapters.MomentViewHolder;
import com.waylens.hachi.ui.adapters.MomentsRecyclerAdapter;
import com.waylens.hachi.ui.entities.APIFilter;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.fragments.Refreshable;
import com.waylens.hachi.ui.fragments.YouTubeFragment;
import com.waylens.hachi.ui.fragments.clipplay.MomentPlayFragment;
import com.waylens.hachi.ui.views.OnViewDragListener;
import com.waylens.hachi.ui.views.RecyclerViewExt;
import com.waylens.hachi.utils.ImageUtils;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.PushUtils;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.VolleyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2015/8/4.
 */
public class AccountActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener,
    Refreshable, MomentsRecyclerAdapter.OnMomentActionListener, OnViewDragListener {
    static final int DEFAULT_COUNT = 10;

    @Bind(R.id.avatar)
    CircleImageView mBtnAvatar;



    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @Bind(R.id.video_list_view)
    RecyclerViewExt mVideoListView;

    @Bind(R.id.refresh_layout)
    SwipeRefreshLayout mRefreshLayout;

    @OnClick(R.id.avatar)
    public void onBtnAvatarClicked() {
        if (SessionManager.getInstance().isLoggedIn()) {
            showLogoutDialog();
        } else {
            LoginActivity.launch(this);
        }
    }

    MomentsRecyclerAdapter mAdapter;

    RequestQueue mRequestQueue;

    LinearLayoutManager mLinearLayoutManager;

    int mCurrentCursor;

    MaterialDialog mLogoutDialog;

    int mHighlightColor;
    int mGreyColor;

    int mProfileStyle = 0; // 0 - list, 1 - grid

    Fragment mVideoFragment;

//    public static void launch(Activity activity) {
//        Intent intent = new Intent(activity, AccountActivity.class);
//        activity.startActivity(intent);
//    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();

    }

    @Override
    protected void init() {
        super.init();
        mHighlightColor = getResources().getColor(R.color.style_color_primary);
        mGreyColor = getResources().getColor(R.color.material_grey_500);

        mRequestQueue = VolleyUtil.newVolleyRequestQueue(this);
        mAdapter = new MomentsRecyclerAdapter(this, null, getFragmentManager(), mRequestQueue, getResources());
        mAdapter.setOnMomentActionListener(this);
        mLinearLayoutManager = new LinearLayoutManager(this);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_account);
//        mProfileListView.getBackground().setColorFilter(mHighlightColor, PorterDuff.Mode.MULTIPLY);
//        mProfileGridView.getBackground().setColorFilter(mGreyColor, PorterDuff.Mode.MULTIPLY);
        mProfileStyle = 0;
        mVideoListView.setAdapter(mAdapter);
        mVideoListView.setLayoutManager(mLinearLayoutManager);
        mRefreshLayout.setOnRefreshListener(this);
        mVideoListView.setOnLoadMoreListener(new RecyclerViewExt.OnLoadMoreListener() {
            @Override
            public void loadMore() {
                loadFeed(mCurrentCursor, false);
            }
        });
    }


    @Override
    public void onStart() {
        super.onStart();
        ImageLoader.getInstance().displayImage(
            SessionManager.getInstance().getAvatarUrl(),
            mBtnAvatar,
            ImageUtils.getAvatarOptions());
        if (SessionManager.getInstance().isLoggedIn()) {
            onRefresh();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (SessionManager.getInstance().isLoggedIn()
            && PreferenceUtils.getString(PreferenceUtils.SEND_GCM_TOKEN_SERVER, null) == null
            && PushUtils.checkGooglePlayServices(this)) {
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRequestQueue.cancelAll(new APIFilter(Constants.API_MOMENTS_ME));
    }






    void showLogoutDialog() {
        if (mLogoutDialog != null && mLogoutDialog.isShowing()) {
            return;
        }
        mLogoutDialog = new MaterialDialog.Builder(this)
            .content(R.string.confirm_logout)
            .positiveText(android.R.string.ok)
            .negativeText(android.R.string.cancel)
            .callback(new MaterialDialog.ButtonCallback() {
                @Override
                public void onPositive(MaterialDialog dialog) {
                    logout();
                }
            })
            .show();
    }

    void logout() {
        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET, Constants.API_DEVICE_DEACTIVATION,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    if (response.optBoolean("result")) {
                        PreferenceUtils.remove(PreferenceUtils.SEND_GCM_TOKEN_SERVER);
                        SessionManager.getInstance().logout();
                        mBtnAvatar.setImageResource(R.drawable.settings_profile_photo_default);
//                        mLoginStatus.setText(R.string.login);
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    ServerMessage.ErrorMsg errorMsg = ServerMessage.parseServerError(error);
                    //showMessage(errorMsg.msgResID);
//                    Snackbar.make(mLoginStatus, errorMsg.msgResID, Snackbar.LENGTH_SHORT).show();
                }
            }));
    }

    void loadFeed(int cursor, final boolean isRefresh) {
        Uri uri = Uri.parse(Constants.API_MOMENTS_ME).buildUpon()
            .appendQueryParameter(Constants.API_MOMENTS_PARAM_CURSOR, String.valueOf(cursor))
            .appendQueryParameter(Constants.API_MOMENTS_PARAM_COUNT, String.valueOf(DEFAULT_COUNT))
            .appendQueryParameter(Constants.API_MOMENTS_PARAM_ORDER, Constants.PARAM_SORT_UPLOAD_TIME)
            .build();
        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET, uri.toString(),
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
    }

    void onLoadFeedFailed(VolleyError error) {
        mRefreshLayout.setRefreshing(false);
        mVideoListView.setIsLoadingMore(false);
        ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);

//        Snackbar.make(mLoginStatus, errorInfo.msgResID, Snackbar.LENGTH_SHORT).show();
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
    public void onLikeMoment(Moment moment, boolean isCancel) {

    }

    @Override
    public void onCommentMoment(Moment moment, int position) {

    }

    @Override
    public void onUserAvatarClicked(Moment moment, int position) {

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
            mFragmentManager.beginTransaction().replace(vh.fragmentContainer.getId(), youTubeFragment).commit();
        } else {
            MomentPlayFragment videoPlayFragment = MomentPlayFragment.newInstance(moment, this);
            vh.videoFragment = videoPlayFragment;
            mVideoFragment = videoPlayFragment;
            mFragmentManager.beginTransaction().replace(vh.fragmentContainer.getId(), videoPlayFragment).commit();
        }
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


}
