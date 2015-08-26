package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.ui.adapters.Comment;
import com.waylens.hachi.ui.adapters.CommentsRecyclerAdapter;
import com.waylens.hachi.utils.ServerMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Richard on 8/26/15.
 */
public class CommentsFragment extends BaseFragment {

    public static final String ARG_MOMENT_ID = "arg.moment.id";
    private static final long INVALID_MOMENT_ID = -1;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @Bind(R.id.comment_list)
    RecyclerView mCommentListView;

    RequestQueue mRequestQueue;

    LinearLayoutManager mLinearLayoutManager;

    CommentsRecyclerAdapter mAdapter;
    private long mMomentID = INVALID_MOMENT_ID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRequestQueue = Volley.newRequestQueue(getActivity());
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mAdapter = new CommentsRecyclerAdapter(null);

        Bundle args = getArguments();
        if (args != null) {
            mMomentID = args.getLong(ARG_MOMENT_ID, INVALID_MOMENT_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_comments, savedInstanceState);
        mCommentListView.setAdapter(mAdapter);
        mCommentListView.setLayoutManager(mLinearLayoutManager);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        loadComments();
    }

    private void loadComments() {
        if (mMomentID == INVALID_MOMENT_ID) {
            return;
        }
        String url = String.format(Constants.API_COMMENTS, mMomentID, 0, 10);
        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        onLoadCommentsSuccessful(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        onLoadCommentsFailed(error);
                    }
                }));

    }

    void onLoadCommentsFailed(VolleyError error) {
        SparseIntArray errorInfo = ServerMessage.parseServerError(error);
        showMessage(errorInfo.get(1));
    }

    void onLoadCommentsSuccessful(JSONObject response) {
        JSONArray jsonComments = response.optJSONArray("comments");
        if (jsonComments == null) {
            return;
        }
        ArrayList<Comment> commentList = new ArrayList<>();
        for (int i = 0; i < jsonComments.length(); i++) {
            commentList.add(Comment.fromJson(jsonComments.optJSONObject(i)));
        }
        mAdapter.setComments(commentList);
        mViewAnimator.setDisplayedChild(1);
    }

    @OnClick(R.id.btn_back)
    public void test() {
        getFragmentManager().beginTransaction().remove(this).commit();
    }

}
