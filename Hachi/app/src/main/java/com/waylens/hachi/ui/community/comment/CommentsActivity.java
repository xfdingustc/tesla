package com.waylens.hachi.ui.community.comment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.entities.Comment;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.entities.User;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/3/9.
 */
public class CommentsActivity extends BaseActivity {
    private static final String TAG = CommentsActivity.class.getSimpleName();


    public static final String ARG_MOMENT_ID = "arg.moment.id";
    public static final String ARG_MOMENT_POSITION = "arg.moment.mPosition";


    private long mMomentID = Moment.INVALID_MOMENT_ID;

    private int mPosition = 0;


    boolean hasUpdates;


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
//        if (mReplyTo != null) {
//            comment.replyTo = mReplyTo;
//            mReplyTo = null;
//            mNewCommentView.setHint(R.string.add_one_comment);
//        }
//        int position = mAdapter.addComment(comment);
//        mCommentListView.scrollToPosition(position);
        mNewCommentView.setText("");
//        publishComment(comment, position);
    }

//    private void publishComment(final Comment comment, final int position) {
//        JSONObject params = new JSONObject();
//        try {
//            params.put("momentID", mMomentID);
//            params.put("content", comment.content);
//            if (comment.replyTo != null) {
//                params.put("replyTo", comment.replyTo.userID);
//            }
//        } catch (JSONException e) {
//            Log.e("test", "", e);
//        }
//
//        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.POST, Constants.API_COMMENTS, params,
//            new Response.Listener<JSONObject>() {
//                @Override
//                public void onResponse(JSONObject response) {
//                    long commentID = response.optLong("commentID");
//                    mAdapter.updateCommentID(position, commentID);
//                    if (!hasUpdates) {
//                        hasUpdates = true;
//                    }
//                }
//            },
//            new Response.ErrorListener() {
//                @Override
//                public void onErrorResponse(VolleyError error) {
//                    ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
//                    showMessage(errorInfo.msgResID);
//                }
//            }).setTag(Constants.API_COMMENTS));
//    }


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

        mNewCommentView.requestFocus();
    }

    @Override
    public void setupToolbar() {
        getToolbar().setTitle(R.string.comment);
        getToolbar().setNavigationIcon(R.drawable.navbar_back);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        super.setupToolbar();
    }


    @Override
    protected void onStop() {
        super.onStop();
        mRequestQueue.cancelAll(Constants.API_COMMENTS);
    }



}
