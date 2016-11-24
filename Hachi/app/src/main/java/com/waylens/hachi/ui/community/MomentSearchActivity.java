package com.waylens.hachi.ui.community;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.TransitionRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.response.MomentListResponse;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.settings.myvideo.MomentItemAdapter;
import com.waylens.hachi.ui.transitions.CircularReveal;
import com.waylens.hachi.utils.ImeUtils;
import com.waylens.hachi.utils.TransitionHelper;
import com.waylens.hachi.utils.TransitionUtils;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/8/3.
 */
public class MomentSearchActivity extends BaseActivity {
    private static final String TAG = MomentSearchActivity.class.getSimpleName();
    private static final String EXTRA_QUERY = "extra_query";
    private MomentItemAdapter mVideoItemAdapter;


//    private SearchHistoryTable mHistoryDatabase;

//    private String mQuery;

    public static void launch(Activity activity, View transitionView) {
        Intent intent = new Intent(activity, MomentSearchActivity.class);
        final Pair<View, String>[] pairs = TransitionHelper.createSafeTransitionParticipants(activity,
            false, new Pair<>(transitionView, activity.getString(R.string.trans_search)));
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, pairs);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @BindView(R.id.container)
    ViewGroup container;

    @BindView(android.R.id.empty)
    ProgressBar progress;

    @BindView(R.id.search_view)
    SearchView searchView;

    @BindView(R.id.moment_list)
    RecyclerView mMomentList;

    @BindView(R.id.searchback)
    ImageButton searchBack;

    @OnClick(R.id.searchback)
    public void dismiss() {
        searchBack.setBackground(null);
        finishAfterTransition();
    }

    private TextView noResults;


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

        mMomentList.setLayoutManager(new LinearLayoutManager(this));
        mVideoItemAdapter = new MomentItemAdapter(this);
        mMomentList.setAdapter(mVideoItemAdapter);

        setupSearchView();
        setupTransitions();

    }



    private void setupSearchView() {
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        searchView.setImeOptions(searchView.getImeOptions() | EditorInfo.IME_ACTION_SEARCH |
            EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                try {
                    queryMoments(URLEncoder.encode(query, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                if (TextUtils.isEmpty(query)) {
                    clearResults();
                }
                return true;
            }
        });

    }

    private void setupTransitions() {
        setEnterSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onSharedElementStart(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
                if (sharedElements != null && !sharedElements.isEmpty()) {
                    View searchIcon = sharedElements.get(0);
                    if (searchIcon.getId() != R.id.searchback) {
                        return;
                    }
                    int centerX = (searchIcon.getLeft() + searchIcon.getRight()) / 2;
                    CircularReveal hideResults = (CircularReveal) TransitionUtils.findTransition((TransitionSet)getWindow().getReturnTransition(), CircularReveal.class, R.id.results_container);
                    if (hideResults != null) {
                        hideResults.setCenter(new Point(centerX, 0));
                    }
                }
            }
        });

        getWindow().getEnterTransition().addListener(new TransitionUtils.TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(Transition transition) {
                searchView.requestFocus();
                ImeUtils.showIme(searchView);
            }
        });
    }

    private void queryMoments(String query) {
        clearResults();
        progress.setVisibility(View.VISIBLE);
        ImeUtils.hideIme(searchView);
        searchView.clearFocus();

        HachiService.createHachiApiService().searchMomentRx(query, 20)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<MomentListResponse>() {
                @Override
                public void onNext(MomentListResponse momentListResponse) {

                    if (momentListResponse.moments.size() > 0) {
                        TransitionManager.beginDelayedTransition(container,
                            getTransition(R.transition.search_show_results));
                        progress.setVisibility(View.GONE);
                        mMomentList.setVisibility(View.VISIBLE);
                        mVideoItemAdapter.setUploadedMomentList(momentListResponse.moments);
                        setNoResultsVisibility(View.GONE);
                    } else {
                        TransitionManager.beginDelayedTransition(
                            container, getTransition(R.transition.auto));
                        progress.setVisibility(View.GONE);
                        setNoResultsVisibility(View.VISIBLE);
                        mMomentList.setVisibility(View.GONE);
                    }
                }
            });
    }

    private void clearResults() {
        TransitionManager.beginDelayedTransition(container, getTransition(R.transition.auto));
        mVideoItemAdapter.clear();
        mMomentList.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
        setNoResultsVisibility(View.GONE);
    }

    private void setNoResultsVisibility(int visibility) {
        if (visibility == View.VISIBLE) {
            if (noResults == null) {
                noResults = (TextView) ((ViewStub) findViewById(R.id.stub_no_search_results)).inflate();
                noResults.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        searchView.setQuery("", false);
                        searchView.requestFocus();
                        ImeUtils.showIme(searchView);
                    }
                });
            }
            String message = String.format(
                getString(R.string.no_search_results), searchView.getQuery().toString());
            SpannableStringBuilder ssb = new SpannableStringBuilder(message);
            ssb.setSpan(new StyleSpan(Typeface.ITALIC),
                message.indexOf('“') + 1,
                message.length() - 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            noResults.setText(ssb);
        }
        if (noResults != null) {
            noResults.setVisibility(visibility);
        }
    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().inflateMenu(R.menu.menu_search);
    }






}
