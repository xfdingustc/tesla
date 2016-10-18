package com.waylens.hachi.ui.community;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.lapism.searchview.SearchAdapter;
import com.lapism.searchview.SearchHistoryTable;
import com.lapism.searchview.SearchItem;
import com.lapism.searchview.SearchView;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.response.MomentListResponse;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.settings.myvideo.MomentItemAdapter;
import com.waylens.hachi.ui.entities.Moment;
import com.xfdingustc.rxutils.library.SimpleSubscribe;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/8/3.
 */
public class MomentSearchActivity extends BaseActivity {
    private static final String TAG = MomentSearchActivity.class.getSimpleName();
    private static final String EXTRA_QUERY = "extra_query";
    private MomentItemAdapter mVideoItemAdapter;

    private SearchHistoryTable mHistoryDatabase;

    private String mQuery;

    public static void launch(Activity activity, String query) {
        Intent intent = new Intent(activity, MomentSearchActivity.class);
        intent.putExtra(EXTRA_QUERY, query);
        activity.startActivity(intent);
    }

    @BindView(R.id.searchView)
    SearchView mSearchView;

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
        mQuery = getIntent().getStringExtra(EXTRA_QUERY);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_search);
//        setupToolbar();

        mMomentList.setLayoutManager(new LinearLayoutManager(this));
        mVideoItemAdapter = new MomentItemAdapter(this);
        mMomentList.setAdapter(mVideoItemAdapter);

        setupSearchView();

        mSearchView.setQuery(mQuery);
    }

    private void setupSearchView() {
        mHistoryDatabase = new SearchHistoryTable(this);
        mSearchView.setVersion(SearchView.VERSION_TOOLBAR);
        mSearchView.setVersionMargins(SearchView.VERSION_MARGINS_TOOLBAR_BIG);
        mSearchView.setTextSize(16);
        mSearchView.setHint(R.string.search_hint);
        mSearchView.setTextInput(mQuery);
        mSearchView.setDivider(false);
        mSearchView.setVoice(true);
        mSearchView.setAnimationDuration(SearchView.ANIMATION_DURATION);
        mSearchView.setShadowColor(ContextCompat.getColor(this, R.color.search_shadow_layout));
        mSearchView.setTheme(SearchView.THEME_DARK, true);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String newQuery = null;
                try {
                    mHistoryDatabase.addItem(new SearchItem(query));
                    newQuery = URLEncoder.encode(query, "UTF-8");
                    queryMoments(newQuery);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                // mSearchView.close(false);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });


        List<SearchItem> suggestionsList = new ArrayList<>();


        SearchAdapter searchAdapter = new SearchAdapter(this, suggestionsList);
        searchAdapter.setOnItemClickListener(new SearchAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                TextView textView = (TextView) view.findViewById(R.id.textView_item_text);
                String query = textView.getText().toString();
                try {
                    String newQuery = URLEncoder.encode(query, "UTF-8");
                    queryMoments(newQuery);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            }
        });
        mSearchView.setAdapter(searchAdapter);
    }

    private void queryMoments(String query) {
        mSearchView.close(true);
        mViewAnimator.setVisibility(View.VISIBLE);
        mViewAnimator.setDisplayedChild(0);


        HachiService.createHachiApiService().searchMomentRx(query, 20)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<MomentListResponse>() {
                @Override
                public void onNext(MomentListResponse momentListResponse) {
                    mViewAnimator.setDisplayedChild(1);

                    if (momentListResponse.moments.size() == 0) {
                        mViewAnimator.setDisplayedChild(2);
                    } else {
                        mVideoItemAdapter.setUploadedMomentList(momentListResponse.moments);
                    }
                }
            });
    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
//        getToolbar().setTitle(R.string.search_hint);
        getToolbar().inflateMenu(R.menu.menu_search);
//        mSearchView.setMenuItem(getToolbar().getMenu().findItem(R.id.action_search));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SearchView.SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && results.size() > 0) {
                String searchWrd = results.get(0);
                if (!TextUtils.isEmpty(searchWrd)) {
                    mSearchView.setQuery(searchWrd);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


}
