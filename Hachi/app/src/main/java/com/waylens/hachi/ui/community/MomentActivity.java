package com.waylens.hachi.ui.community;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.transition.ChangeBounds;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.birbit.android.jobqueue.JobManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.bgjob.BgJobHelper;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.social.DeleteCommentJob;
import com.waylens.hachi.bgjob.social.FollowJob;
import com.waylens.hachi.bgjob.social.ReportJob;
import com.waylens.hachi.bgjob.social.event.SocialEvent;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.ReportCommentBody;
import com.waylens.hachi.rest.body.SocialProvider;
import com.waylens.hachi.rest.response.FollowInfo;
import com.waylens.hachi.rest.response.MomentInfo;
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
import com.waylens.hachi.ui.entities.Comment;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.entities.User;
import com.waylens.hachi.ui.views.SendCommentButton;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.TransitionHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;
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

    public static final String EXTRA_THUMBNAIL = "thumbnail";
    public static final String EXTRA_MOMENT_ID = "momentId";
    public static final int REQUEST_GOOGLE_AUTHORIZE = 0x1000;
    public static final int REQUEST_FACEBOOK_AUTHORIZE = 0x1001;

    private long mMomentId;
    private MomentInfo mMomentInfo;
    private FollowInfo mFollowInfo;

    private String mThumbnail;

    private CommentsAdapter mAdapter;
    private int mCurrentCursor;

    private User mReplyTo;

    private String mReportReason;

    private boolean hasUpdates;


    private static final DecelerateInterpolator DECCELERATE_INTERPOLATOR = new DecelerateInterpolator();
    private static final AccelerateInterpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();
    private static final OvershootInterpolator OVERSHOOT_INTERPOLATOR = new OvershootInterpolator(4);

    private HachiApi mHachi = HachiService.createHachiApiService();
    private BottomSheetDialog mBottomSheetDialog;

    private MomentPlayFragment mMomentPlayFragment;


    private SessionManager mSessionManager = SessionManager.getInstance();


    public static void launch(Activity activity, long momentId, String thumbnail, View transitionView) {

        Intent intent = new Intent(activity, MomentActivity.class);
        intent.putExtra(EXTRA_MOMENT_ID, momentId);
        intent.putExtra(EXTRA_THUMBNAIL, thumbnail);
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
    ImageButton mBtnLike;

    @BindView(R.id.user_name)
    TextView mUserName;

    @BindView(R.id.like_count)
    TextSwitcher mTsLikeCount;

    @BindView(R.id.user_avatar)
    CircleImageView mUserAvatar;

    @BindView(R.id.current_user_avatar)
    CircleImageView mCurrentUserAvatar;

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
        doUpdateLikeStateAnimator();
        updateLikeCount();
    }

    @OnClick(R.id.user_avatar)
    public void onUserAvatarClick() {
        if (!mSessionManager.isLoggedIn()) {
            AuthorizeActivity.launch(this);
            return;

        }
        UserProfileActivity.launch(this, mMomentInfo.owner.userID);

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
                !mFollowInfo.isMyFollower, new DialogHelper.onPositiveClickListener() {
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
        mReportReason = getResources().getStringArray(R.array.report_reason)[0];
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_moment);

        queryMomentInfo();

        mMomentPlayFragment = MomentPlayFragment.newInstance(mMomentId, mThumbnail);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMomentPlayFragment.setSharedElementEnterTransition(new ChangeBounds());
        }

        getFragmentManager().beginTransaction().replace(R.id.moment_play_container, mMomentPlayFragment).commit();


//
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

        CircleImageView avatar = (CircleImageView) view.findViewById(R.id.current_user_avatar);
        Glide.with(this)
            .load(mSessionManager.getAvatarUrl())
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.menu_profile_photo_default)
            .dontAnimate()
            .into(avatar);


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
                    User basicUserInfo = new User();
                    basicUserInfo.avatarUrl = SessionManager.getInstance().getAvatarUrl();
                    basicUserInfo.userName = SessionManager.getInstance().getUserName();
                    basicUserInfo.userID = SessionManager.getInstance().getUserId();
                    comment.author = basicUserInfo;
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

        mHachi.getMomentInfo(mMomentId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext(new Action1<retrofit2.Response<MomentInfo>>() {
                @Override
                public void call(retrofit2.Response<MomentInfo> response) {
                    Logger.t(TAG).d("do on next");
                    if (response.isSuccessful()) {
                        MomentInfo momentInfo = response.body();
                        mMomentPlayFragment.doGaugeSetting(momentInfo, null);
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
            .subscribe(new rx.Observer<retrofit2.Response<MomentInfo> >() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    Logger.t(TAG).d(e.getMessage());

                }

                @Override
                public void onNext(retrofit2.Response<MomentInfo> response) {
                    if (response.isSuccessful() ) {
                        Logger.t(TAG).d("code:" + response.code());
                        Logger.t(TAG).d("body:" + response.body());
                        MomentInfo momentInfo = response.body();
                        mMomentInfo = momentInfo;
                        showMomentInfo();
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

        Glide.with(MomentActivity.this)
            .load(mSessionManager.getAvatarUrl())
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.menu_profile_photo_default)
            .dontAnimate()
            .into(mCurrentUserAvatar);


        Glide.with(MomentActivity.this)
            .load(mMomentInfo.owner.avatarUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.menu_profile_photo_default)
            .dontAnimate()
            .into(mUserAvatar);

        if (isDestroyed()) {
            Logger.t(TAG).d("activity is destroyed, so we must return here");
            return;
        }


        mContentRoot.showNext();
        setupCommentList();
    }

    private void updateFollowInfo(String userID) {
        Logger.t(TAG).d("load userId: " + userID);
        Call<FollowInfo> followInfoCall = mHachi.getFollowInfo(userID);
        followInfoCall.enqueue(new Callback<FollowInfo>() {
            @Override
            public void onResponse(Call<FollowInfo> call, retrofit2.Response<FollowInfo> response) {
//                Logger.t(TAG).d(response.body().toString());
                mFollowInfo = response.body();
                updateFollowTextView();

            }

            @Override
            public void onFailure(Call<FollowInfo> call, Throwable t) {

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


    private void doUpdateLikeStateAnimator() {

        AnimatorSet animatorSet = new AnimatorSet();

        ObjectAnimator rotationAnim = ObjectAnimator.ofFloat(mBtnLike, "rotation", 0f, 360f);
        rotationAnim.setDuration(300);
        rotationAnim.setInterpolator(ACCELERATE_INTERPOLATOR);

        ObjectAnimator bounceAnimX = ObjectAnimator.ofFloat(mBtnLike, "scaleX", 0.2f, 1f);
        bounceAnimX.setDuration(300);
        bounceAnimX.setInterpolator(OVERSHOOT_INTERPOLATOR);

        ObjectAnimator bounceAnimY = ObjectAnimator.ofFloat(mBtnLike, "scaleY", 0.2f, 1f);
        bounceAnimY.setDuration(300);
        bounceAnimY.setInterpolator(OVERSHOOT_INTERPOLATOR);
        bounceAnimY.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                updateLikeState();
            }
        });

        animatorSet.play(rotationAnim);
        animatorSet.play(bounceAnimX).with(bounceAnimY).after(rotationAnim);


        animatorSet.start();

    }

    private void updateLikeState() {
        if (mMomentInfo.moment.isLiked) {
            //vh.btnLike.setImageResource(R.drawable.social_like_click);
            mBtnLike.setImageResource(R.drawable.ic_favorite);
        } else {
            mBtnLike.setImageResource(R.drawable.ic_favorite_border);
        }
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
        mAdapter = new CommentsAdapter(null);
        mAdapter.setOnCommentClickListener(new CommentsAdapter.OnCommentClickListener() {
            @Override
            public void onCommentClicked(final Comment comment, final int position) {
                if (!mSessionManager.isLoggedIn()) {
                    AuthorizeActivity.launch(MomentActivity.this);
                    return;
                }
                if (!checkUserVerified()) {
                    return;
                }
//                final MaterialSimpleListAdapter adapter = new MaterialSimpleListAdapter(MomentActivity.this);
//                adapter.add(new MaterialSimpleListItem.Builder(MomentActivity.this)
//                    .content(R.string.reply)
//                    .icon(R.drawable.comment_reply)
//                    .backgroundColor(getResources().getColor(R.color.material_grey_800))
//                    .build());
//                MaterialSimpleListItem item = null;
//
//                if (comment.author.userID.equals(SessionManager.getInstance().getUserId())) {
//                    item = new MaterialSimpleListItem.Builder(MomentActivity.this)
//                        .content(R.string.delete)
//                        .icon(R.drawable.btn_edit_action_delete)
//                        .backgroundColor(getResources().getColor(R.color.material_grey_800))
//                        .build();
//                } else {
//                    item = new MaterialSimpleListItem.Builder(MomentActivity.this)
//                        .content(R.string.report)
//                        .icon(R.drawable.comment_report)
//                        .backgroundColor(getResources().getColor(R.color.material_grey_800))
//                        .build();
//
//                }
//                adapter.add(item);
//
//                new MaterialDialog.Builder(MomentActivity.this)
//                    .title(R.string.comment)
//                    .adapter(adapter, new MaterialDialog.ListCallback() {
//                        @Override
//                        public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
//                            switch (which) {
//                                case 0:
//                                    mReplyTo = comment.author;
//                                    addComment();
//                                    if (mNewCommentView != null) {
//                                        mNewCommentView.setHint(getString(R.string.reply_to, comment.author.userName));
//                                    }
//                                    break;
//                                case 1:
//                                    if (!comment.author.userID.equals(SessionManager.getInstance().getUserId())) {
//                                        reportComment(comment);
//                                    } else {
//                                        doDeleteComment(comment);
//                                        mAdapter.removeComment(position);
//                                    }
//                                    break;
//                                default:
//                                    break;
//                            }
//                            dialog.dismiss();
//                        }
//                    })
//                    .show();
            }

            public void reportComment(final Comment comment) {
                new MaterialDialog.Builder(MomentActivity.this)
                    .title(R.string.report)
                    .items(R.array.report_reason)
                    .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                            return true;
                        }
                    })
                    .positiveText(R.string.report)
                    .negativeText(R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            int index = dialog.getSelectedIndex();
                            mReportReason = getResources().getStringArray(R.array.report_reason)[index];
                            Logger.t(TAG).d("report reason:" + mReportReason + "index:" + index);
                            doReportComment(comment);
                        }
                    })
                    .show();

            }

            @Override
            public void onCommentLongClicked(final Comment comment, final int position) {
//                BottomSheet builder = new BottomSheet.Builder(MomentActivity.this)
//                    .sheet(R.menu.menu_report_comment)
//                    .darkTheme()
//                    .listener(new MenuItem.OnMenuItemClickListener() {
//                        @Override
//                        public boolean onMenuItemClick(MenuItem item) {
//                            switch (item.getItemId()) {
//                                case R.id.report:
//
//                                    MaterialDialog dialog = new MaterialDialog.Builder(MomentActivity.this)
//                                        .name(R.string.report)
//                                        .items(R.array.report_reason)
//                                        .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
//                                            @Override
//                                            public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
//                                                mReportReason = getResources().getStringArray(R.array.report_reason)[which];
//                                                return true;
//                                            }
//                                        })
//                                        .positiveText(R.string.report)
//                                        .negativeText(android.R.string.cancel)
//                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
//                                            @Override
//                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
//                                                doReportComment(comment);
//                                            }
//                                        })
//                                        .show();
//
//
//                                    break;
//                            }
//                            return true;
//                        }
//                    })
//                    .build();
//
//                builder.show();

            }
        });
        mAdapter.setOnLoadMoreListener(new CommentsAdapter.OnLoadMoreListener() {
            @Override
            public void loadMore() {
                loadComments(mCurrentCursor, false);
            }
        });
        mCommentList.setAdapter(mAdapter);
        mCommentList.setItemAnimator(new DefaultItemAnimator());
        refreshComments();
    }


    private void refreshComments() {
        mCurrentCursor = 0;
        loadComments(mCurrentCursor, true);


    }

    private void loadComments(int cursor, final boolean isRefresh) {
        Logger.t(TAG).d("loadcomment  " + mMomentInfo + " moment id: " + mMomentInfo.moment.id);
        if (mMomentInfo == null || mMomentInfo.moment.id == Moment.INVALID_MOMENT_ID) {
            Logger.t(TAG).d("null");
            return;
        }

        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_COMMENTS + String.format(Constants.API_COMMENTS_QUERY_STRING,
                mMomentInfo.moment.id, cursor, DEFAULT_COUNT))
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Logger.t(TAG).json(response.toString());
                    onLoadCommentsSuccessful(response, isRefresh);
                }
            })
            .errorListener(new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    onLoadCommentsFailed(error);
                }
            })
            .build();

        mRequestQueue.add(request);


    }

    private void doReportComment(Comment comment) {

        JobManager jobManager = BgJobManager.getManager();
        ReportCommentBody reportCommentBody = new ReportCommentBody();
        reportCommentBody.commentID = comment.commentID;
        reportCommentBody.reason = mReportReason;
        reportCommentBody.detail = "";
        Logger.t(TAG).d(mReportReason);
        ReportJob job = new ReportJob(reportCommentBody, ReportJob.REPORT_TYPE_COMMENT);
        jobManager.addJobInBackground(job);
        Snackbar.make(mCommentList, "Report comment successfully", Snackbar.LENGTH_LONG).show();

/*        String url = Constants.API_REPORT;
        final JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("commentID", comment.commentID);
            requestBody.put("reason", mReportReason);

            Logger.t(TAG).json(requestBody.toString());
            AuthorizedJsonRequest request = new AuthorizedJsonRequest(Request.Method.POST, url, requestBody, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Logger.t(TAG).json(response.toString());
                    Snackbar.make(mCommentList, "Report comment successfully", Snackbar.LENGTH_LONG).show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Logger.t(TAG).d(error.toString());
                }
            });

            mRequestQueue.add(request);
        } catch (JSONException e) {
            e.printStackTrace();
        }*/
    }

    private void doDeleteComment(Comment comment) {
        JobManager jobManager = BgJobManager.getManager();
        DeleteCommentJob job = new DeleteCommentJob(comment.commentID);
        jobManager.addJobInBackground(job);
    }


    private void showMessage(int resId) {
        //Should not call this method if UI has been already destroyed.
        try {
            Snackbar.make(mCommentList, resId, Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            Logger.t(TAG).e(e.toString());
        }
    }

    private void onLoadCommentsFailed(VolleyError error) {
        ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
        showMessage(errorInfo.msgResID);
    }

    private void onLoadCommentsSuccessful(JSONObject response, boolean isRefresh) {
        JSONArray jsonComments = response.optJSONArray("comments");
        if (jsonComments == null) {
            return;
        }
        ArrayList<Comment> commentList = new ArrayList<>();
        for (int i = 0; i < jsonComments.length(); i++) {
            Comment comment = Comment.fromJson(jsonComments.optJSONObject(i));
            commentList.add(comment);
//            Logger.t(TAG).d("Add comment: " + comment.toString());
        }

        boolean hasMore = response.optBoolean("hasMore");
        mAdapter.setIsLoadMore(false);

        if (isRefresh) {
            mAdapter.setComments(commentList, hasMore);
        } else {
            mAdapter.addComments(commentList, hasMore);
        }

        mCurrentCursor += commentList.size();

//        if (mViewAnimator.getDisplayedChild() == 0) {
//            mViewAnimator.setDisplayedChild(1);
//        }
    }


    private void publishComment(final Comment comment, final int position) {
        JSONObject params = new JSONObject();
        try {
            params.put("momentID", mMomentInfo.moment.id);
            params.put("content", comment.content);

        } catch (JSONException e) {
            Logger.t(TAG).e(e.toString());
        }

        AuthorizedJsonRequest.Builder requestBuilder = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_COMMENTS)
            .postBody("momentID", mMomentInfo.moment.id)
            .postBody("content", comment.content)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    long commentID = response.optLong("commentID");
                    mAdapter.updateCommentID(position, commentID);
                    if (!hasUpdates) {
                        hasUpdates = true;
                    }
                }
            })
            .errorListener(new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
                    showMessage(errorInfo.msgResID);
                }
            });

        if (comment.replyTo != null) {
            requestBuilder.postBody("replyTo", comment.replyTo.userID);
        }

        mRequestQueue.add(requestBuilder.build());

    }

    private boolean checkUserPermission() {
        if (!checkUserLoggedIn()) {
            return false;
        }
        if (!checkUserVerified()) {
            return false;
        }
        return true;
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

            Call<UserInfo> userInfoCall = mHachi.getUserInfo(mSessionManager.getUserId());
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
}
