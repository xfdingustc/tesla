package com.waylens.hachi.ui.community;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
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
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.birbit.android.jobqueue.JobManager;
import com.cocosw.bottomsheet.BottomSheet;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.social.LikeJob;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.UserProfileActivity;
import com.waylens.hachi.ui.community.comment.CommentsAdapter;
import com.waylens.hachi.ui.entities.Comment;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.entities.User;
import com.waylens.hachi.ui.views.OnViewDragListener;
import com.waylens.hachi.utils.ImageUtils;
import com.waylens.hachi.utils.ServerMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2016/5/19.
 */
public class MomentActivity extends BaseActivity {
    private static final String TAG = MomentActivity.class.getSimpleName();
    private static final int DEFAULT_COUNT = 10;
    private Moment mMoment;

    private CommentsAdapter mAdapter;
    private int mCurrentCursor;

    private User mReplyTo;

    private String mReportReason;

    private boolean hasUpdates;

    private static final DecelerateInterpolator DECCELERATE_INTERPOLATOR = new DecelerateInterpolator();
    private static final AccelerateInterpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();
    private static final OvershootInterpolator OVERSHOOT_INTERPOLATOR = new OvershootInterpolator(4);

    public static void launch(Context activity, Moment moment) {
        Intent intent = new Intent(activity, MomentActivity.class);
        intent.putExtra("moment", moment);
        activity.startActivity(intent);
    }


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

    @OnClick(R.id.btn_like)
    public void onBtnLikeClicked() {
        boolean isCancel = mMoment.isLiked;
        JobManager jobManager = BgJobManager.getManager();
        LikeJob job = new LikeJob(mMoment, isCancel);
        jobManager.addJobInBackground(job);
        mMoment.isLiked = !mMoment.isLiked;
        if (mMoment.isLiked) {
            mMoment.likesCount++;
        } else {
            mMoment.likesCount--;
        }
        doUpdateLikeStateAnimator();
        updateLikeCount();
    }

    @OnClick(R.id.user_avatar)
    public void onUserAvatarClick() {
        UserProfileActivity.launch(this, mMoment.owner.userID);
    }

    @BindView(R.id.comment_new)
    EditText mNewCommentView;


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
    protected void init() {
        super.init();
        Intent intent = getIntent();
        mMoment = (Moment) intent.getSerializableExtra("moment");
        Logger.t(TAG).d("moment: " + mMoment.toString());
        mReportReason = getResources().getStringArray(R.array.report_reason)[0];
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_moment);
        if (mMoment.title == null || mMoment.title.isEmpty()) {
            mMomentTitle.setText("No Title");
        } else {
            mMomentTitle.setText(mMoment.title);
        }
        mUserName.setText(mMoment.owner.userName);
        updateLikeState();

        mTsLikeCount.setCurrentText(String.valueOf(mMoment.likesCount));

        mImageLoader.displayImage(mMoment.owner.avatarUrl, mUserAvatar, ImageUtils.getAvatarOptions());


        MomentPlayFragment fragment = MomentPlayFragment.newInstance(mMoment);

        getFragmentManager().beginTransaction().replace(R.id.moment_play_container, fragment).commit();

        setupCommentList();
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
        if (mMoment.isLiked) {
            //vh.btnLike.setImageResource(R.drawable.social_like_click);
            mBtnLike.setImageResource(R.drawable.social_like_click);
        } else {
            mBtnLike.setImageResource(R.drawable.social_like);
        }
    }

    private void updateLikeCount() {
        int fromValue;
        if (mMoment.isLiked) {
            fromValue = mMoment.likesCount - 1;
        } else {
            fromValue = mMoment.likesCount + 1;
        }

        mTsLikeCount.setCurrentText(String.valueOf(fromValue));

        String toValue = String.valueOf(mMoment.likesCount);
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
        if (mMoment == null || mMoment.id == Moment.INVALID_MOMENT_ID) {
            return;
        }

        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_COMMENTS + String.format(Constants.API_COMMENTS_QUERY_STRING, mMoment.id, cursor, DEFAULT_COUNT))
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
            params.put("momentID", mMoment.id);
            params.put("content", comment.content);

        } catch (JSONException e) {
            Logger.t(TAG).e(e.toString());
        }

        AuthorizedJsonRequest.Builder requestBuilder = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_COMMENTS)
            .postBody("momentID", mMoment.id)
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
