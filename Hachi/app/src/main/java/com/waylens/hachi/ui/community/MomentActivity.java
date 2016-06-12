package com.waylens.hachi.ui.community;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.birbit.android.jobqueue.JobManager;
import com.bumptech.glide.Glide;
import com.cocosw.bottomsheet.BottomSheet;
import com.orhanobut.logger.Logger;
import com.rest.HachiApi;
import com.rest.HachiService;
import com.rest.response.FollowInfo;
import com.rest.response.MomentInfo;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.social.FollowJob;
import com.waylens.hachi.bgjob.social.LikeJob;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.UserProfileActivity;
import com.waylens.hachi.ui.community.comment.CommentsAdapter;
import com.waylens.hachi.ui.entities.Comment;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.entities.User;
import com.waylens.hachi.utils.ServerMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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
    public static final String EXTRA_IMAGE = "MomentActivity:image";

    private static final int DEFAULT_COUNT = 10;
    private long mMomentId;
    public static final String EXTRA_THUMBNAIL = "thumbnail";
    public static final String EXTRA_MOMENT_ID = "momentId";

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

    public static void launch(Activity activity, long momentId, String thumbnail, View transitionView) {
        ActivityOptionsCompat options = ActivityOptionsCompat
            .makeSceneTransitionAnimation(activity, transitionView, EXTRA_IMAGE);
        Intent intent = new Intent(activity, MomentActivity.class);
        intent.putExtra(EXTRA_MOMENT_ID, momentId);
        intent.putExtra(EXTRA_THUMBNAIL, thumbnail);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @BindView(R.id.comment_new)
    EditText mNewCommentView;

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

    @BindView(R.id.comment_list)
    RecyclerView mCommentList;

    @BindView(R.id.add_follow)
    TextView mAddFollow;

    @BindView(R.id.video_thumbnail)
    ImageView mVideoThumbnail;

    @OnClick(R.id.btn_like)
    public void onBtnLikeClicked() {
        boolean isCancel = mMomentInfo.moment.isLiked;
        JobManager jobManager = BgJobManager.getManager();
        LikeJob job = new LikeJob(mMomentInfo.moment.id, isCancel);
        jobManager.addJobInBackground(job);
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
        UserProfileActivity.launch(this, mMomentInfo.owner.userID);
    }


    @OnClick(R.id.btn_send)
    public void sendComment() {
        if (TextUtils.isEmpty(mNewCommentView.getText())) {
            return;
        }
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
            mNewCommentView.setHint(R.string.add_one_comment);
        }
        int position = mAdapter.addComment(comment);
        mCommentList.scrollToPosition(position);
        mNewCommentView.setText("");
        publishComment(comment, position);
    }

    @OnClick(R.id.add_follow)
    public void addFollow() {
        JobManager jobManager = BgJobManager.getManager();
        mFollowInfo.isMyFollowing = !mFollowInfo.isMyFollowing;
        FollowJob job = new FollowJob(mMomentInfo.owner.userID, mFollowInfo.isMyFollowing);
        jobManager.addJobInBackground(job);

        updateFollowTextView();
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshComments();
    }

    @Override
    public void onBackPressed() {
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setImmersiveMode(false);
        } else {
            super.onBackPressed();
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
        Glide.with(this).load(mThumbnail).into(mVideoThumbnail);
        ViewCompat.setTransitionName(mVideoThumbnail, EXTRA_IMAGE);

        queryMomentInfo();
    }

    private void queryMomentInfo() {

        mHachi.getMomentInfoRx(mMomentId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext(new Action1<MomentInfo>() {
                @Override
                public void call(MomentInfo momentInfo) {
                    updateFollowInfo(momentInfo.owner.userID);
                }
            })
            .subscribe(new rx.Observer<MomentInfo>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(MomentInfo momentInfo) {
                    mMomentInfo = momentInfo;
                    Logger.t(TAG).d("moment info: " + momentInfo.moment.toString());

                    if (TextUtils.isEmpty(mMomentInfo.moment.title)) {
                        mMomentTitle.setText("No Title");
                    } else {
                        mMomentTitle.setText(mMomentInfo.moment.title);
                    }

                    if (mMomentInfo.owner != null) {
                        mUserName.setText(mMomentInfo.owner.userName);
                    }


                    updateLikeState();

                    mTsLikeCount.setCurrentText(String.valueOf(mMomentInfo.moment.likesCount));


                    Glide.with(MomentActivity.this).load(mMomentInfo.owner.avatarUrl).crossFade().into(mUserAvatar);

                    MomentPlayFragment fragment = MomentPlayFragment.newInstance(mMomentInfo);

                    getFragmentManager().beginTransaction().replace(R.id.moment_play_container, fragment).commit();

                    setupCommentList();
                }
            });

    }

    private void updateFollowInfo(String userID) {
        Logger.t(TAG).d("load userId: " + userID);
        Call<FollowInfo> followInfoCall = mHachi.getFollowInfo(userID);
        followInfoCall.enqueue(new Callback<FollowInfo>() {
            @Override
            public void onResponse(Call<FollowInfo> call, retrofit2.Response<FollowInfo> response) {
                Logger.t(TAG).d(response.body().toString());
                mFollowInfo = response.body();
                updateFollowTextView();

            }

            @Override
            public void onFailure(Call<FollowInfo> call, Throwable t) {

            }
        });

    }

    private void updateFollowTextView() {
        if (mFollowInfo.isMyFollowing) {
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
            mBtnLike.setImageResource(R.drawable.social_like_click);
        } else {
            mBtnLike.setImageResource(R.drawable.social_like);
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
            public void onCommentClicked(Comment comment) {
                mReplyTo = comment.author;
                mNewCommentView.setHint(getString(R.string.reply_to, comment.author.userName));
            }

            @Override
            public void onCommentLongClicked(final Comment comment) {
                BottomSheet builder = new BottomSheet.Builder(MomentActivity.this)
                    .sheet(R.menu.menu_report_comment)
                    .darkTheme()
                    .listener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.report:

                                    MaterialDialog dialog = new MaterialDialog.Builder(MomentActivity.this)
                                        .title(R.string.report)
                                        .items(R.array.report_reason)
                                        .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                                            @Override
                                            public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                                mReportReason = getResources().getStringArray(R.array.report_reason)[which];
                                                return true;
                                            }
                                        })
                                        .positiveText(R.string.report)
                                        .negativeText(android.R.string.cancel)
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                doReportComment(comment);
                                            }
                                        })
                                        .show();


                                    break;
                            }
                            return true;
                        }
                    })
                    .build();

                builder.show();

            }
        });
        mAdapter.setOnLoadMoreListener(new CommentsAdapter.OnLoadMoreListener() {
            @Override
            public void loadMore() {

            }
        });
        mCommentList.setAdapter(mAdapter);
    }


    private void refreshComments() {
        mCurrentCursor = 0;
        loadComments(mCurrentCursor, true);


    }

    private void loadComments(int cursor, final boolean isRefresh) {
        if (mMomentInfo == null || mMomentInfo.moment.id == Moment.INVALID_MOMENT_ID) {
            return;
        }

        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_COMMENTS + String.format(Constants.API_COMMENTS_QUERY_STRING,
                mMomentInfo.moment.id, cursor, DEFAULT_COUNT))
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
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
        String url = Constants.API_REPORT;
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
        }

    }

    void showMessage(int resId) {
        //Should not call this method if UI has been already destroyed.
        try {
            Snackbar.make(mCommentList, resId, Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            Logger.t(TAG).e(e.toString());
        }
    }

    void onLoadCommentsFailed(VolleyError error) {
        ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
        showMessage(errorInfo.msgResID);
    }

    void onLoadCommentsSuccessful(JSONObject response, boolean isRefresh) {
        JSONArray jsonComments = response.optJSONArray("comments");
        if (jsonComments == null) {
            return;
        }
        ArrayList<Comment> commentList = new ArrayList<>();
        for (int i = jsonComments.length() - 1; i >= 0; i--) {
            Comment comment = Comment.fromJson(jsonComments.optJSONObject(i));
            commentList.add(comment);
            Logger.t(TAG).d("Add comment: " + comment.toString());
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
}
