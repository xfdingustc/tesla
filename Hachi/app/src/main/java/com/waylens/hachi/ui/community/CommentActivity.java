package com.waylens.hachi.ui.community;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.bean.Comment;
import com.waylens.hachi.rest.bean.User;
import com.waylens.hachi.rest.body.PublishCommentBody;
import com.waylens.hachi.rest.response.CommentListResponse;
import com.waylens.hachi.rest.response.MomentInfo;
import com.waylens.hachi.rest.response.PublishCommentResponse;
import com.waylens.hachi.rest.response.UserInfo;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.authorization.VerifyEmailActivity;
import com.waylens.hachi.ui.community.comment.CommentsAdapter;
import com.waylens.hachi.ui.views.AvatarView;
import com.waylens.hachi.ui.views.SendCommentButton;
import com.waylens.hachi.utils.ServerErrorHelper;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;


/**
 * Created by laina on 16/10/21.
 */

public class CommentActivity extends BaseActivity {
    public static String TAG = CommentActivity.class.getSimpleName();
    public static String EXTRA_MOMENT_ID = "extra.moment.id";

    public static int DEFAULT_COUNT = 10;

    private long mMomentId;

    private MomentInfo mMomentInfo;

    private SessionManager mSessionManager;

    private CommentsAdapter mAdapter;

    private User mReplyTo;

    private int mCurrentCursor;

    private EditText mNewCommentView;

    private BottomSheetDialog mBottomSheetDialog;

    private SendCommentButton mBtnSendComment;

    @BindView(R.id.comment_list)
    RecyclerView mCommentList;

    @BindView(R.id.current_user_avatar_view)
    AvatarView userAvatar;

    @OnClick(R.id.comment_new)
    public void onCommentNewClicked() {
        addComment();
    }

    public static void launch(Activity activity, long momentID) {
        Intent intent = new Intent(activity, CommentActivity.class);
        intent.putExtra(EXTRA_MOMENT_ID, momentID);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.activity_open, R.anim.activity_close);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        Intent intent = getIntent();
        mMomentId = intent.getLongExtra(EXTRA_MOMENT_ID, -1);
        mSessionManager = SessionManager.getInstance();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_comment);
        //queryMomentInfo();
        addComment();
        showMomentInfo();
    }

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(R.anim.activity_open, R.anim.activity_close);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void queryMomentInfo() {
        HachiService.createHachiApiService().getMomentInfo(mMomentId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext(new Action1<Response<MomentInfo>>() {
                @Override
                public void call(retrofit2.Response<MomentInfo> response) {
                    Logger.t(TAG).d("do on next");
                    if (response.isSuccessful()) {
                        MomentInfo momentInfo = response.body();
                        Logger.t(TAG).d("Moment Play Activity!");
                    } else {
                        Logger.t(TAG).d("code:" + response.code());
                        Logger.t(TAG).d("body:" + response.body());
                        Logger.t(TAG).d("error body:" + response.errorBody());
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
                    } else {
                        Logger.t(TAG).d("code:" + response.code());
                        Logger.t(TAG).d("body:" + response.body());
                        Logger.t(TAG).d("error body:" + response.errorBody());
                    }
                }
            });
    }

    private void showMomentInfo() {
        // Check if it is current user since we cannot follow ourselves
        userAvatar.loadAvatar(mSessionManager.getAvatarUrl(), mSessionManager.getUserName());
        if (isDestroyed()) {
            Logger.t(TAG).d("activity is destroyed, so we must return here");
            return;
        }
        setupCommentList();
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
                    AuthorizeActivity.launch(CommentActivity.this);
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
        if (mMomentId < 0) {
            Logger.t(TAG).d("null");
            return;
        }
        HachiService.createHachiApiService().getCommentsRx(mMomentId, cursor, DEFAULT_COUNT)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<CommentListResponse>() {
                @Override
                public void onNext(CommentListResponse commentListResponse) {
                    Logger.t(TAG).d("got response");
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


    private void publishComment(final Comment comment, final int position) {
        final PublishCommentBody publishCommentBody = new PublishCommentBody();
        publishCommentBody.momentID = mMomentId;
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
                }

                @Override
                public void onError(Throwable e) {
                    Snackbar.make(mCommentList, e.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
            });
    }

    private boolean validateComment() {
        if (TextUtils.isEmpty(mNewCommentView.getText())) {
            mBtnSendComment.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_error));
            return false;
        }
        return true;
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
                        VerifyEmailActivity.launch(CommentActivity.this);
                    }
                })
                .show();
            return false;
        }
    }
}
