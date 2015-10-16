package com.waylens.hachi.ui.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ViewAnimator;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.adapters.CommentsRecyclerAdapter;
import com.waylens.hachi.ui.entities.User;
import com.waylens.hachi.ui.entities.Comment;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.utils.ServerMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Richard on 8/26/15.
 */
public class CommentsFragment extends BaseFragment implements CommentsRecyclerAdapter.OnCommentClickListener,
        CommentsRecyclerAdapter.OnLoadMoreListener, FragmentNavigator {

    public static final String ARG_MOMENT_ID = "arg.moment.id";
    public static final String ARG_MOMENT_POSITION = "arg.moment.mPosition";

    private static final int DEFAULT_COUNT = 10;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @Bind(R.id.comment_list)
    RecyclerView mCommentListView;

    @Bind(R.id.comment_new)
    EditText mNewCommentView;

    RequestQueue mRequestQueue;

    LinearLayoutManager mLinearLayoutManager;

    CommentsRecyclerAdapter mAdapter;
    private long mMomentID = Moment.INVALID_MOMENT_ID;

    private int mPosition = 0;

    User mReplyTo;

    boolean hasUpdates;

    int mCurrentCursor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRequestQueue = Volley.newRequestQueue(getActivity());
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mAdapter = new CommentsRecyclerAdapter(null);
        mAdapter.setOnCommentClickListener(this);
        mAdapter.setOnLoadMoreListener(this);
        Bundle args = getArguments();
        if (args != null) {
            mMomentID = args.getLong(ARG_MOMENT_ID, Moment.INVALID_MOMENT_ID);
            mPosition = args.getInt(ARG_MOMENT_POSITION, 0);
        }
        hasUpdates = false;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_comments, savedInstanceState);
        mCommentListView.setAdapter(mAdapter);
        mCommentListView.setLayoutManager(mLinearLayoutManager);
        mNewCommentView.requestFocus();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshComments();
    }

    @Override
    public void onDestroyView() {
        mRequestQueue.cancelAll(Constants.API_COMMENTS);
        super.onDestroyView();
    }

    void refreshComments() {
        mCurrentCursor = 0;
        loadComments(mCurrentCursor, true);
    }

    private void loadComments(int cursor, final boolean isRefresh) {
        if (mMomentID == Moment.INVALID_MOMENT_ID) {
            return;
        }
        String url = Constants.API_COMMENTS + String.format(Constants.API_COMMENTS_QUERY_STRING, mMomentID, cursor, DEFAULT_COUNT);
        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        onLoadCommentsSuccessful(response, isRefresh);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
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
            commentList.add(Comment.fromJson(jsonComments.optJSONObject(i)));
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

    @Override
    public void onCommentClicked(Comment comment) {
        mReplyTo = comment.author;
        mNewCommentView.setHint(getString(R.string.reply_to, comment.author.userName));
    }

    @Override
    public void loadMore() {
        loadComments(mCurrentCursor, false);
    }

    @OnClick(R.id.btn_back)
    @Override
    public void onBack() {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment_content);
        if (hasUpdates && fragment != null && fragment instanceof HomeFragment) {
            HomeFragment fg = (HomeFragment) fragment;
            fg.loadComment(mMomentID, mPosition);
        }

        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        getFragmentManager().beginTransaction().remove(this).commit();
    }
}
