package com.waylens.hachi.ui.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
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
import com.waylens.hachi.ui.adapters.Moment;
import com.waylens.hachi.ui.adapters.MomentsRecyclerAdapter;
import com.waylens.hachi.utils.ServerMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2015/8/4.
 */
public class HomeFragment extends BaseFragment {

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @Bind(R.id.video_list_view)
    RecyclerView mVideoListView;

    MomentsRecyclerAdapter mAdapter;

    RequestQueue mRequestQueue;

    LinearLayoutManager mLinearLayoutManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRequestQueue = Volley.newRequestQueue(getActivity());
        mAdapter = new MomentsRecyclerAdapter(null, getFragmentManager(), mRequestQueue, getResources());
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_live, savedInstanceState);
        mVideoListView.setAdapter(mAdapter);
        mVideoListView.setLayoutManager(mLinearLayoutManager);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadFeed();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRequestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }

    void loadFeed() {
        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET, Constants.API_MOMENTS,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        onLoadFeedSuccessful(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        onLoadFeedFailed(error);
                    }
                }));
    }

    void onLoadFeedSuccessful(JSONObject response) {
        JSONArray jsonMoments = response.optJSONArray("moments");
        if (jsonMoments == null) {
            return;
        }
        ArrayList<Moment> momentList = new ArrayList<>();
        for (int i = 0; i < jsonMoments.length(); i++) {
            momentList.add(Moment.fromJson(jsonMoments.optJSONObject(i)));
        }
        mAdapter.setMoments(momentList);
        mViewAnimator.setDisplayedChild(1);
        for (int i = 0; i < momentList.size(); i++) {
            loadComment(momentList.get(i).id, i);
        }
    }

    void onLoadFeedFailed(VolleyError error) {
        SparseIntArray errorInfo = ServerMessage.parseServerError(error);
        showMessage(errorInfo.get(1));
    }

    void loadComment(final long momentID, final int position) {
        if (momentID == Moment.INVALID_MOMENT_ID) {
            return;
        }
        String url = Constants.API_COMMENTS + String.format(Constants.API_COMMENTS_QUERY_STRING, momentID, 0, 3);
        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                refreshComment(momentID, position, response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                SparseIntArray errorInfo = ServerMessage.parseServerError(error);
                showMessage(errorInfo.get(1));
            }
        }));
    }

    void refreshComment(long momentID, int position, JSONObject response) {
        JSONArray jsonComments = response.optJSONArray("comments");
        if (jsonComments == null || jsonComments.length() == 0) {
            return;
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder();

        for (int i = jsonComments.length() - 1; i >= 0; i--) {
            Comment comment = Comment.fromJson(jsonComments.optJSONObject(i));
            int start = ssb.length();
            ssb.append(comment.author.userName);
            ssb.setSpan(new StyleSpan(Typeface.BOLD), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new UserNameSpan(), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (comment.replyTo != null) {
                ssb.append(" ");
                start = ssb.length();
                ssb.append("@").append(comment.replyTo.userName);
                ssb.setSpan(new StyleSpan(Typeface.BOLD), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new UserNameSpan(), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            ssb.append(" ").append(comment.content);
            if (i > 0) {
                ssb.append("\n");
            }
        }

        mAdapter.updateMoment(ssb, position);

    }

    class UserNameSpan extends ClickableSpan {

        @Override
        public void onClick(View widget) {
            //
        }
    }
}
