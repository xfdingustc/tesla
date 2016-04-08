package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ViewAnimator;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.adapters.CommentsRecyclerAdapter;
import com.waylens.hachi.ui.entities.Comment;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.entities.User;
import com.waylens.hachi.utils.ServerMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/3/9.
 */
public class CommentsActivity extends BaseActivity implements CommentsRecyclerAdapter.OnCommentClickListener,
    CommentsRecyclerAdapter.OnLoadMoreListener {
    private static final String TAG = CommentsActivity.class.getSimpleName();
    private static final int DEFAULT_COUNT = 10;

    public static final String ARG_MOMENT_ID = "arg.moment.id";
    public static final String ARG_MOMENT_POSITION = "arg.moment.mPosition";


    private CommentsRecyclerAdapter mAdapter;
    private long mMomentID = Moment.INVALID_MOMENT_ID;

    private int mPosition = 0;

    User mReplyTo;

    boolean hasUpdates;

    int mCurrentCursor;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @Bind(R.id.comment_list)
    RecyclerView mCommentListView;

    @Bind(R.id.comment_new)
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
            mNewCommentView.setHint(R.string.add_a_comment);
        }
        int position = mAdapter.addComment(comment);
        mCommentListView.scrollToPosition(position);
        mNewCommentView.setText("");
        publishComment(comment, position);
    }

    private void publishComment(final Comment comment, final int position) {
        JSONObject params = new JSONObject();
        try {
            params.put("momentID", mMomentID);
            params.put("content", comment.content);
            if (comment.replyTo != null) {
                params.put("replyTo", comment.replyTo.userID);
            }
        } catch (JSONException e) {
            Log.e("test", "", e);
        }

        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.POST, Constants.API_COMMENTS, params,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    long commentID = response.optLong("commentID");
                    mAdapter.updateCommentID(position, commentID);
                    if (!hasUpdates) {
                        hasUpdates = true;
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
                    showMessage(errorInfo.msgResID);
                }
            }).setTag(Constants.API_COMMENTS));
    }


    public static void launch(Activity activity, long id, int position) {
        Intent intent = new Intent(activity, CommentsActivity.class);
        intent.putExtra(ARG_MOMENT_ID, id);
        intent.putExtra(ARG_MOMENT_POSITION, position);
        activity.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        Intent intent = getIntent();
        mMomentID = intent.getLongExtra(ARG_MOMENT_ID, Moment.INVALID_MOMENT_ID);
        mPosition = intent.getIntExtra(ARG_MOMENT_POSITION, 0);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_comments);
        mCommentListView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new CommentsRecyclerAdapter(null);
        mAdapter.setOnCommentClickListener(this);
        mAdapter.setOnLoadMoreListener(this);
        mCommentListView.setAdapter(mAdapter);
        mNewCommentView.requestFocus();
    }

    @Override
    public void setupToolbar() {
        mToolbar.setTitle(R.string.comments);
        mToolbar.setNavigationIcon(R.drawable.navbar_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        super.setupToolbar();
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshComments();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mRequestQueue.cancelAll(Constants.API_COMMENTS);
    }

    void refreshComments() {
        mCurrentCursor = 0;
        loadComments(mCurrentCursor, true);


    }

    void showMessage(int resId) {
        //Should not call this method if UI has been already destroyed.
        try {
            Snackbar.make(mViewAnimator, resId, Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("test", "", e);
        }
    }

    private void loadComments(int cursor, final boolean isRefresh) {
        if (mMomentID == Moment.INVALID_MOMENT_ID) {
            return;
        }


        String url = Constants.API_COMMENTS + String.format(Constants.API_COMMENTS_QUERY_STRING, mMomentID, cursor, DEFAULT_COUNT);
        Logger.t(TAG).d("load commens: " + url);
        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET, url,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(final JSONObject response) {
                    //Logger.t(TAG).d("get response " + response.toString());

                    onLoadCommentsSuccessful(response, isRefresh);

                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Logger.t(TAG).d("Error");
                    onLoadCommentsFailed(error);
                }
            }).setTag(Constants.API_COMMENTS));

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

        if (mViewAnimator.getDisplayedChild() == 0) {
            mViewAnimator.setDisplayedChild(1);
        }
    }

    @Override
    public void onCommentClicked(Comment comment) {
        mReplyTo = comment.author;
        mNewCommentView.setHint(getString(R.string.reply_to, comment.author.userName));
    }

    @Override
    public void loadMore() {

    }
}
