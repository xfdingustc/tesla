package com.waylens.hachi.ui.community;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ViewAnimator;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.clips.upload.VideoItemAdapter;
import com.waylens.hachi.ui.entities.Moment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/8/3.
 */
public class MomentSearchActivity extends BaseActivity {
    private static final String TAG = MomentSearchActivity.class.getSimpleName();
    private VideoItemAdapter mVideoItemAdapter;

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, MomentSearchActivity.class);
        activity.startActivity(intent);
    }

    @BindView(R.id.search_view)
    MaterialSearchView mSearchView;

    @BindView(R.id.moment_list)
    RecyclerView mMomentList;

    @BindView(R.id.view_animator)
    ViewAnimator mViewAnimator;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_search);
        setupToolbar();

        mMomentList.setLayoutManager(new LinearLayoutManager(this));
        mVideoItemAdapter = new VideoItemAdapter(this);
        mMomentList.setAdapter(mVideoItemAdapter);

        mSearchView.setVoiceSearch(true);
        mSearchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mSearchView.closeSearch();
                queryMoments(query.trim());
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        mSearchView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mSearchView.showSearch();
            }
        }, 200);
    }

    private void queryMoments(String query) {
        mViewAnimator.setVisibility(View.VISIBLE);
        mViewAnimator.setDisplayedChild(0);
        final String requestUrl = Constants.API_MOMENTS_SEARCH + "?key=" + query + "&count=20";
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(requestUrl)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    mViewAnimator.setDisplayedChild(1);
                    Logger.t(TAG).json(response.toString());
                    JSONArray jsonMoments = response.optJSONArray("moments");
                    if (jsonMoments == null) {
                        return;
                    }
                    ArrayList<Moment> momentList = new ArrayList<>();
                    for (int i = 0; i < jsonMoments.length(); i++) {
                        JSONObject jsonObject = jsonMoments.optJSONObject(i);
                        if (jsonObject != null) {
                            JSONObject momentObject = jsonObject.optJSONObject("moment");
                            if (momentObject != null) {
                                momentList.add(Moment.fromJson(momentObject));
                            }
                        }

//                        Logger.t(TAG).d();
                    }
                    if (momentList.size() == 0) {
                        mViewAnimator.setDisplayedChild(2);
                    } else {
                        mVideoItemAdapter.setUploadedMomentList(momentList);
                    }
                }
            })
            .errorListener(new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }).build();
        mRequestQueue.add(request);
    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.search_hint);
        getToolbar().inflateMenu(R.menu.menu_search);
        mSearchView.setMenuItem(getToolbar().getMenu().findItem(R.id.action_search));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MaterialSearchView.REQUEST_VOICE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && matches.size() > 0) {
                String searchWrd = matches.get(0);
                if (!TextUtils.isEmpty(searchWrd)) {
                    mSearchView.setQuery(searchWrd, false);
                }
            }

            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }





}
