package com.waylens.hachi.ui.community;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.transition.ChangeBounds;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobHelper;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.social.FollowJob;
import com.waylens.hachi.bgjob.social.event.SocialEvent;
import com.waylens.hachi.jobqueue.JobManager;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.bean.Comment;
import com.waylens.hachi.rest.bean.User;
import com.waylens.hachi.rest.body.PublishCommentBody;
import com.waylens.hachi.rest.body.SocialProvider;
import com.waylens.hachi.rest.response.CommentListResponse;
import com.waylens.hachi.rest.response.FollowInfo;
import com.waylens.hachi.rest.response.MomentInfo;
import com.waylens.hachi.rest.response.PublishCommentResponse;
import com.waylens.hachi.rest.response.UserInfo;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.UserProfileActivity;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.authorization.FacebookAuthorizeActivity;
import com.waylens.hachi.ui.authorization.GoogleAuthorizeActivity;
import com.waylens.hachi.ui.authorization.VerifyEmailActivity;
import com.waylens.hachi.ui.community.comment.CommentsAdapter;
import com.waylens.hachi.ui.dialogs.DialogHelper;
import com.waylens.hachi.ui.views.AvatarView;
import com.waylens.hachi.ui.views.SendCommentButton;
import com.waylens.hachi.utils.AnimUtils;
import com.waylens.hachi.utils.ColorUtils;
import com.waylens.hachi.utils.ServerErrorHelper;
import com.waylens.hachi.utils.TransitionHelper;
import com.waylens.hachi.utils.VersionHelper;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;
import com.waylens.hachi.view.CheckableButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/5/19.
 */
public class MomentActivity extends BaseActivity {
    private static final String TAG = MomentActivity.class.getSimpleName();

    private static final int DEFAULT_COUNT = 10;
    private static final float SCRIM_ADJUSTMENT = 0.075f;

    public static final String EXTRA_THUMBNAIL = "videoThumbnail";
    public static final String EXTRA_MOMENT_ID = "momentId";
    public static final String EXTRA_REQUEST = "request";
    public static final int REQUEST_GOOGLE_AUTHORIZE = 0x1000;
    public static final int REQUEST_FACEBOOK_AUTHORIZE = 0x1001;
    public static final int REQUEST_COMMENT = 0x2001;

    private long mMomentId;
    private MomentInfo mMomentInfo;
    private FollowInfo mFollowInfo;

    private String mThumbnail;

    private CommentsAdapter mAdapter;
    private int mCurrentCursor;

    private User mReplyTo;


    private boolean hasUpdates;

    private boolean isRequestComment = false;


    private BottomSheetDialog mBottomSheetDialog;

    private MomentPlayFragment mMomentPlayFragment;


    private SessionManager mSessionManager = SessionManager.getInstance();


    public static void launch(Activity activity, long momentId, String thumbnail, View transitionView) {

        Intent intent = new Intent(activity, MomentActivity.class);
        intent.putExtra(EXTRA_MOMENT_ID, momentId);
        intent.putExtra(EXTRA_THUMBNAIL, thumbnail);
        if (transitionView != null) {
            final Pair<View, String>[] pairs = TransitionHelper.createSafeTransitionParticipants(activity,
                false, new Pair<>(transitionView, activity.getString(R.string.moment_cover)));
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, pairs);
            ActivityCompat.startActivity(activity, intent, options.toBundle());
        } else {
            activity.startActivity(intent);
        }
    }

    public static void launch(Activity activity, long momentId, String thumbnail, View transitionView, int request) {
        Intent intent = new Intent(activity, MomentActivity.class);
        intent.putExtra(EXTRA_MOMENT_ID, momentId);
        intent.putExtra(EXTRA_THUMBNAIL, thumbnail);
        intent.putExtra(EXTRA_REQUEST, request);
        final Pair<View, String>[] pairs = TransitionHelper.createSafeTransitionParticipants(activity,
            false, new Pair<>(transitionView, activity.getString(R.string.moment_cover)));
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, pairs);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }


    @BindView(R.id.content_root)
    ViewSwitcher mContentRoot;

    @BindView(R.id.moment_play_container)
    View mPlayContainer;

    @BindView(R.id.momemt_title)
    TextView mMomentTitle;

    @BindView(R.id.btn_like)
    CheckableButton mBtnLike;

    @BindView(R.id.user_name)
    TextView mUserName;

    @BindView(R.id.like_count)
    TextSwitcher mTsLikeCount;

    @BindView(R.id.avatar_view)
    AvatarView avatarView;

    @BindView(R.id.current_user_avatar_view)
    AvatarView currentUserAvatarView;

    @BindView(R.id.comment_list)
    RecyclerView mCommentList;

    @BindView(R.id.add_follow)
    TextView mAddFollow;


    @BindView(R.id.btn_repost)
    ImageButton mBtnRepost;


    SendCommentButton mBtnSendComment;

    EditText mNewCommentView;

    @OnClick(R.id.btn_like)
    public void onBtnLikeClicked() {
        if (!mSessionManager.isLoggedIn()) {
            AuthorizeActivity.launch(this);
            return;
        }
        if (!checkUserVerified()) {
            return;
        }

        BgJobHelper.addLike(mMomentInfo.moment.id, mMomentInfo.moment.isLiked);
        mMomentInfo.moment.isLiked = !mMomentInfo.moment.isLiked;
        if (mMomentInfo.moment.isLiked) {
            mMomentInfo.moment.likesCount++;
        } else {
            mMomentInfo.moment.likesCount--;
        }
//        doUpdateLikeStateAnimator();
        updateLikeState();
        updateLikeCount();
    }

    @OnClick(R.id.avatar_view)
    public void onUserAvatarClick() {
        if (!mSessionManager.isLoggedIn()) {
            AuthorizeActivity.launch(this);
            return;

        }
        UserProfileActivity.launch(this, mMomentInfo.owner, avatarView);

    }


    @OnClick(R.id.add_follow)
    public void addFollow() {
        if (!mSessionManager.isLoggedIn()) {
            AuthorizeActivity.launch(this);
            return;

        }
        if (!checkUserVerified()) {
            return;
        }

        if (mFollowInfo == null) {
            updateFollowInfo(mMomentInfo.owner.userID);
            return;
        }
        if (mFollowInfo != null && mFollowInfo.isMyFollowing) {
            DialogHelper.showUnfollowConfirmDialog(this, mMomentInfo.owner.userName, mMomentInfo.owner.userID,
                !mFollowInfo.isMyFollower, new DialogHelper.OnPositiveClickListener() {
                    @Override
                    public void onPositiveClick() {
                        toggleFollowState();
                    }
                });

        } else {
            toggleFollowState();
        }


    }

    @OnClick(R.id.comment_new)
    public void onCommentNewClicked() {
        addComment();
    }

    @OnClick(R.id.btn_repost)
    public void onBtnRepostClicked() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .title(R.string.repost)
            .items(R.array.social_provider)
            .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                @Override
                public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                    return false;
                }
            })
            .negativeText(R.string.cancel)
            .positiveText(R.string.ok)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    int selectIndex = dialog.getSelectedIndex();
                    String provider;
                    if (selectIndex == 0) {
                        provider = SocialProvider.FACEBOOK;
                    } else {
                        provider = SocialProvider.YOUTUBE;
                    }

                    repost2SocialMedia(provider);

                }
            }).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventSocial(SocialEvent event) {
        switch (event.getWhat()) {
            case SocialEvent.EVENT_WHAT_REPOST:
                String provider = (String) event.getExtra();
                if (event.getResponse().result) {
                    Logger.t(TAG).d("status: " + event.getResponse().status);
                    Snackbar.make(mBtnRepost, getString(R.string.repost_success) + " " + provider, Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(mBtnRepost, R.string.repost_failed, Snackbar.LENGTH_SHORT).show();
                }
        }
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (mMomentInfo != null && mMomentInfo.owner != null && mFollowInfo == null) {
            updateFollowInfo(mMomentInfo.owner.userID);
        }
        EventBus.getDefault().register(this);
    }


    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onBackPressed() {
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setImmersiveMode(false);
        } else {
            mMomentPlayFragment.onBackPressed();
            super.onBackPressed();

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_GOOGLE_AUTHORIZE:
                if (resultCode == RESULT_OK) {
                    BgJobHelper.repost(mMomentId, SocialProvider.YOUTUBE);
                }
                break;
            case REQUEST_FACEBOOK_AUTHORIZE:
                if (resultCode == RESULT_OK) {
                    BgJobHelper.repost(mMomentId, SocialProvider.FACEBOOK);
                }
                break;
        }
    }

    @Override
    protected void init() {
        super.init();
        Intent intent = getIntent();
        mMomentId = intent.getLongExtra(EXTRA_MOMENT_ID, -1);
        mThumbnail = intent.getStringExtra(EXTRA_THUMBNAIL);

        int request = intent.getIntExtra(EXTRA_REQUEST, -1);
        if (request == REQUEST_COMMENT) {
            isRequestComment = true;
        }
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_moment);

        updateStatusBar();

        queryMomentInfo();

        mMomentPlayFragment = MomentPlayFragment.newInstance(mMomentId, mThumbnail);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMomentPlayFragment.setSharedElementEnterTransition(new ChangeBounds());
        }

        getFragmentManager().beginTransaction().replace(R.id.moment_play_container, mMomentPlayFragment).commit();
//
    }

    private void updateStatusBar() {
        Glide.with(this)
            .load(mThumbnail)
            .asBitmap()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .priority(Priority.IMMEDIATE)
            .into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                    calcStatusBarColor(resource);

                }


            });

    }

    private void addComment() {
        if (!mSessionManager.isLoggedIn()) {
            AuthorizeActivity.launch(this);
            return;
        }
        if (!checkUserVerified()) {
            return;
        }
        showBottomSheetDialog();
    }


    private void showBottomSheetDialog() {
        mBottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_add_comment, null);
        mNewCommentView = (EditText) view.findViewById(R.id.comment_edit);

        AvatarView avatar = (AvatarView) view.findViewById(R.id.current_user_avatar);
        avatar.loadAvatar(mSessionManager.getAvatarUrl(), mSessionManager.getUserName());

        mBottomSheetDialog.setContentView(view);

        mNewCommentView.setFocusable(true);
        mNewCommentView.setFocusableInTouchMode(true);
        mNewCommentView.requestFocus();

        mBtnSendComment = (SendCommentButton) view.findViewById(R.id.btn_send);

        mBtnSendComment.setOnSendClickListener(new SendCommentButton.OnSendClickListener() {
            @Override
            public void onSendClickListener(View v) {
                if (validateComment()) {
                    Comment comment = new Comment();
                    comment.content = mNewCommentView.getText().toString();
                    comment.createTime = System.currentTimeMillis();
                    User user = new User();
                    user.avatarUrl = SessionManager.getInstance().getAvatarUrl();
                    user.userName = SessionManager.getInstance().getUserName();
                    user.userID = SessionManager.getInstance().getUserId();
                    comment.author = user;
                    if (mReplyTo != null) {
                        comment.replyTo = mReplyTo;
                        mReplyTo = null;
                        mNewCommentView.setHint(R.string.add_public_comment);
                    }
                    int position = mAdapter.addComment(comment);
                    mCommentList.scrollToPosition(position);
                    mNewCommentView.setText("");
                    mBtnSendComment.setCurrentState(SendCommentButton.STATE_DONE);
                    publishComment(comment, position);
                    mBottomSheetDialog.dismiss();
                }
            }
        });


        mBottomSheetDialog.show();
        mBottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mBottomSheetDialog = null;
            }
        });

        mNewCommentView.post(new Runnable() {
            @Override
            public void run() {
                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(mNewCommentView, 0);
            }
        });


    }

    private void repost2SocialMedia(String provider) {
        if (provider.equals(SocialProvider.YOUTUBE)) {
            if (!mSessionManager.isYoutubeLinked()) {
                GoogleAuthorizeActivity.launch(this, REQUEST_GOOGLE_AUTHORIZE);
                return;
            }
        } else if (provider.equals(SocialProvider.FACEBOOK)) {
            if (!mSessionManager.isFacebookLinked()) {
                FacebookAuthorizeActivity.launch(this, REQUEST_FACEBOOK_AUTHORIZE);
                return;
            }
        }


        BgJobHelper.repost(mMomentId, provider);
    }

    private void toggleFollowState() {
        JobManager jobManager = BgJobManager.getManager();
        FollowJob job = new FollowJob(mMomentInfo.owner.userID, !mFollowInfo.isMyFollowing);
        jobManager.addJobInBackground(job);
        if (!mFollowInfo.isMyFollowing) {
            mFollowInfo.followers--;
        } else {
            mFollowInfo.followers++;
        }
        mFollowInfo.isMyFollowing = !mFollowInfo.isMyFollowing;
        updateFollowTextView();
    }

    private boolean validateComment() {
        if (TextUtils.isEmpty(mNewCommentView.getText())) {
            mBtnSendComment.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_error));
            return false;
        }
        return true;
    }


    private void queryMomentInfo() {

        HachiService.createHachiApiService().getMomentInfo(mMomentId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext(new Action1<retrofit2.Response<MomentInfo>>() {
                @Override
                public void call(retrofit2.Response<MomentInfo> response) {
                    Logger.t(TAG).d("do on next");
                    if (response.isSuccessful()) {
                        MomentInfo momentInfo = response.body();
                        //mMomentPlayFragment.doGaugeSetting(momentInfo, null);
                        Logger.t(TAG).d("Moment Play Activity!");
                        updateFollowInfo(momentInfo.owner.userID);
                    } else {
                        Logger.t(TAG).d("code:" + response.code());
                        Logger.t(TAG).d("body:" + response.body());
                        Logger.t(TAG).d("error body:" + response.errorBody());
                        Snackbar.make(mPlayContainer, getString(R.string.moment_lost), Snackbar.LENGTH_LONG).show();
                        TimerTask timerTask = new TimerTask() {
                            @Override
                            public void run() {
                                finish();
                            }
                        };
                        Timer timer = new Timer();
                        timer.schedule(timerTask, 2 * 1000);
                    }
                }
            })
            .subscribe(new rx.Observer<retrofit2.Response<MomentInfo>>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    Logger.t(TAG).d(e.getMessage());

                }

                @Override
                public void onNext(retrofit2.Response<MomentInfo> response) {
                    if (response.isSuccessful()) {
                        Logger.t(TAG).d("code:" + response.code());
                        Logger.t(TAG).d("body:" + response.body());
                        MomentInfo momentInfo = response.body();
                        mMomentInfo = momentInfo;
                        showMomentInfo();
                        if (isRequestComment) {
                            addComment();
                        }
                    } else {
                        Logger.t(TAG).d("code:" + response.code());
                        Logger.t(TAG).d("body:" + response.body());
                        Logger.t(TAG).d("error body:" + response.errorBody());
                    }
                }
            });

    }

    private void showMomentInfo() {
        Logger.t(TAG).d("moment info: " + mMomentInfo.moment.toString());

        if (TextUtils.isEmpty(mMomentInfo.moment.title)) {
            mMomentTitle.setText("No Title");
        } else {
            mMomentTitle.setText(mMomentInfo.moment.title);
        }

        if (mMomentInfo.owner != null) {
            mUserName.setText(mMomentInfo.owner.userName);
        }

        // Check if it is current user since we cannot follow ourselves
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isLoggedIn() && sessionManager.getUserId().equals(mMomentInfo.owner.userID)) {
            mAddFollow.setVisibility(View.GONE);
            mBtnRepost.setVisibility(View.VISIBLE);
        }


        updateLikeState();

        mTsLikeCount.setCurrentText(String.valueOf(mMomentInfo.moment.likesCount));

        currentUserAvatarView.loadAvatar(mSessionManager.getAvatarUrl(), mSessionManager.getUserName());
        avatarView.loadAvatar(mMomentInfo.owner.avatarUrl, mMomentInfo.owner.userName);

        if (isDestroyed()) {
            Logger.t(TAG).d("activity is destroyed, so we must return here");
            return;
        }


        mContentRoot.showNext();
        setupCommentList();
    }

    private void updateFollowInfo(String userID) {
        Logger.t(TAG).d("load userId: " + userID);
        HachiService.createHachiApiService().getFollowInfoRx(userID)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<FollowInfo>() {
                @Override
                public void onNext(FollowInfo followInfo) {
                    mFollowInfo = followInfo;
                    updateFollowTextView();
                }
            });
    }

    private void updateFollowTextView() {
        if (mFollowInfo != null && mFollowInfo.isMyFollowing) {
            mAddFollow.setText(R.string.unfollow);
        } else {
            mAddFollow.setText(R.string.follow);
        }
    }




    private void updateLikeState() {
        mBtnLike.setChecked(mMomentInfo.moment.isLiked);
    }

    private void updateLikeCount() {
        int fromValue;
        if (mMomentInfo.moment.isLiked) {
            fromValue = mMomentInfo.moment.likesCount - 1;
        } else {
            fromValue = mMomentInfo.moment.likesCount + 1;
        }

        mTsLikeCount.setCurrentText(String.valueOf(fromValue));

        String toValue = String.valueOf(mMomentInfo.moment.likesCount);
        mTsLikeCount.setText(toValue);

    }

    private void setupCommentList() {
        mCommentList.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new CommentsAdapter(mCommentList, this, null);
        mAdapter.setOnCommentClickListener(new CommentsAdapter.OnCommentClickListener() {

            @Override
            public void onReplyClicked(Comment comment) {
                mReplyTo = comment.author;
                addComment();
                if (mNewCommentView != null) {
                    mNewCommentView.setHint(getString(R.string.reply_to, comment.author.userName));
                }
            }

            @Override
            public void onCommentClicked(final Comment comment, final int position) {
                if (!mSessionManager.isLoggedIn()) {
                    AuthorizeActivity.launch(MomentActivity.this);
                    return;
                }
                if (!checkUserVerified()) {
                    return;
                }
            }
        });
        mAdapter.setOnLoadMoreListener(new CommentsAdapter.OnLoadMoreListener() {
            @Override
            public void loadMore() {
                loadComments(mCurrentCursor, false);
            }
        });
        mCommentList.setAdapter(mAdapter);
        refreshComments();
    }


    private void refreshComments() {
        mCurrentCursor = 0;
        loadComments(mCurrentCursor, true);


    }

    private void loadComments(int cursor, final boolean isRefresh) {
//        Logger.t(TAG).d("loadcomment  " + mMomentInfo + " moment id: " + mMomentInfo.moment.id);
        if (mMomentInfo == null || mMomentInfo.moment.id == MomentInfo.MomentBasicInfo.INVALID_MOMENT_ID) {
            Logger.t(TAG).d("null");
            return;
        }


        HachiService.createHachiApiService().getCommentsRx(mMomentInfo.moment.id, cursor, DEFAULT_COUNT)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<CommentListResponse>() {
                @Override
                public void onNext(CommentListResponse commentListResponse) {
                    onLoadCommentsSuccessful(commentListResponse, isRefresh);
                }

                @Override
                public void onError(Throwable e) {
                    ServerErrorHelper.showErrorMessage(mCommentList, e);
                }
            });
    }

    private void onLoadCommentsSuccessful(CommentListResponse response, boolean isRefresh) {
        mAdapter.setIsLoadMore(false);

        if (isRefresh) {
            mAdapter.setComments(response.comments, response.hasMore);
        } else {
            mAdapter.addComments(response.comments, response.hasMore);
        }

        mCurrentCursor += response.comments.size();
    }


    private void publishComment(final Comment comment, final int position) {
        final PublishCommentBody publishCommentBody = new PublishCommentBody();
        publishCommentBody.momentID = mMomentInfo.moment.id;
        publishCommentBody.content = comment.content;
        if (comment.replyTo != null) {
            publishCommentBody.replyTo = comment.replyTo.userID;
        }

        HachiService.createHachiApiService().publishCommentRx(publishCommentBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<PublishCommentResponse>() {
                @Override
                public void onNext(PublishCommentResponse publishCommentResponse) {
                    mAdapter.updateCommentID(position, publishCommentBody.momentID);
                    if (!hasUpdates) {
                        hasUpdates = true;
                    }
                }

                @Override
                public void onError(Throwable e) {
                    Snackbar.make(mCommentList, e.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
            });

    }

    private boolean checkUserPermission() {
        if (!checkUserLoggedIn()) {
            return false;
        }
        return checkUserVerified();
    }

    private boolean checkUserLoggedIn() {
        if (mSessionManager.isLoggedIn()) {
            return true;
        } else {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("Please log in!")
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .show();
            return false;
        }
    }

    private boolean checkUserVerified() {

        if (mSessionManager.isVerified()) {
            return true;
        } else {

            Call<UserInfo> userInfoCall = HachiService.createHachiApiService().getUserInfo(mSessionManager.getUserId());
            userInfoCall.enqueue(new Callback<UserInfo>() {
                @Override
                public void onResponse(Call<UserInfo> call, retrofit2.Response<UserInfo> response) {
                    if (response.body() != null) {
                        mSessionManager = SessionManager.getInstance();
                        mSessionManager.setIsVerified(response.body().isVerified);
                        Logger.t(TAG).d("isVerified = " + response.body().isVerified, this);

                    }
                }

                @Override
                public void onFailure(Call<UserInfo> call, Throwable t) {

                }
            });
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content(R.string.verify_email_address)
                .positiveText(R.string.verify)
                .negativeText(R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        VerifyEmailActivity.launch(MomentActivity.this);
                    }
                })
                .show();
            return false;
        }
    }

    private void calcStatusBarColor(final Bitmap resource) {
        final int twentyFourDip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        if (VersionHelper.isGreaterThanLollipop()) {
            Palette.from(resource)
                .maximumColorCount(3)
                .clearFilters()
                .setRegion(0, 0, resource.getWidth() - 1, twentyFourDip)
                .generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        boolean isDark;
                        int lightness = ColorUtils.isDark(palette);
                        if (lightness == ColorUtils.LIGHTNESS_UNKNOWN) {
                            isDark = ColorUtils.isDark(resource, resource.getWidth() / 2, 0);
                        } else {
                            isDark = lightness == ColorUtils.IS_DARK;
                        }

                        int statusBarColor = getWindow().getStatusBarColor();
                        final Palette.Swatch topColor = ColorUtils.getMostPopulousSwatch(palette);
                        if (topColor != null &&
                            (isDark || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
                            statusBarColor = ColorUtils.scrimify(topColor.getRgb(),
                                isDark, SCRIM_ADJUSTMENT);
                            // set a light status bar on M+
                            if (!isDark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                                ViewUtils.setLightStatusBar(imageView);
                            }
                        }
                        if (statusBarColor != getWindow().getStatusBarColor()) {
//                            imageView.setScrimColor(statusBarColor);
                            ValueAnimator statusBarColorAnim = ValueAnimator.ofArgb(
                                getWindow().getStatusBarColor(), statusBarColor);
                            statusBarColorAnim.addUpdateListener(new ValueAnimator
                                .AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    getWindow().setStatusBarColor(
                                        (int) animation.getAnimatedValue());
                                }
                            });
                            statusBarColorAnim.setDuration(1000L);
                            statusBarColorAnim.setInterpolator(
                                AnimUtils.getFastOutSlowInInterpolator(MomentActivity.this));
                            statusBarColorAnim.start();
                        }
                    }
                });
        }
    }


}
