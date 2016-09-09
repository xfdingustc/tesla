package com.waylens.hachi.ui.community;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.lapism.searchview.SearchAdapter;
import com.lapism.searchview.SearchHistoryTable;
import com.lapism.searchview.SearchItem;
import com.lapism.searchview.SearchView;
import com.waylens.hachi.R;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.UserProfileActivity;
import com.waylens.hachi.ui.adapters.SimpleFragmentPagerAdapter;
import com.waylens.hachi.ui.community.feed.FeedFragment;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.activities.NotificationActivity;
import com.waylens.hachi.utils.VolleyUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;


/**
 * Created by Xiaofei on 2016/1/22.
 */
public class CommunityFragment extends BaseFragment implements FragmentNavigator{
    private static final String TAG = CommunityFragment.class.getSimpleName();

    private SimpleFragmentPagerAdapter mFeedPageAdapter;

    private SearchHistoryTable mHistoryDatabase;

    @BindView(R.id.viewpager)
    ViewPager mViewPager;

    @BindView(R.id.searchView)
    SearchView mSearchView;




    @Override
    protected String getRequestTag() {
        return TAG;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_moment, savedInstanceState);
        setupSearchView();
        setupViewPager();
        return view;
    }

    private void setupSearchView() {
        mHistoryDatabase = new SearchHistoryTable(getActivity());
        mHistoryDatabase.setHistorySize(5);
        mSearchView.setVersion(SearchView.VERSION_MENU_ITEM);
        mSearchView.setVersionMargins(SearchView.VERSION_MARGINS_MENU_ITEM);
        mSearchView.setHint(R.string.search_hint);
        mSearchView.setTextSize(16);
        mSearchView.setDivider(false);
        mSearchView.setVoice(true);
        mSearchView.setAnimationDuration(SearchView.ANIMATION_DURATION);
        mSearchView.setShadowColor(ContextCompat.getColor(getActivity(), R.color.search_shadow_layout));
        mSearchView.setTheme(SearchView.THEME_DARK, true);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mHistoryDatabase.addItem(new SearchItem(query));
                MomentSearchActivity.launch(getActivity(), query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });


        List<SearchItem> suggestionsList = new ArrayList<>();


        SearchAdapter searchAdapter = new SearchAdapter(getActivity(), suggestionsList);
        searchAdapter.setOnItemClickListener(new SearchAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                TextView textView = (TextView) view.findViewById(R.id.textView_item_text);
                String query = textView.getText().toString();
//                    getData(query, position);
                MomentSearchActivity.launch(getActivity(), query);
            }
        });
        mSearchView.setAdapter(searchAdapter);
    }

    @Override
    public void setupToolbar() {
        getToolbar().setTitle(R.string.moments);
        super.setupToolbar();
    }


    @Override
    public void onResume() {
        super.onResume();
        getToolbar().getMenu().clear();
        getToolbar().inflateMenu(R.menu.menu_community);
        if (!SessionManager.getInstance().isLoggedIn()) {
            getToolbar().getMenu().removeItem(R.id.my_notification);
        }
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.search:
//                        MomentSearchActivity.launch(getActivity());
                        mSearchView.open(true);
                        return false;
                    case R.id.my_notification:
                        NotificationActivity.launch(getActivity());
                        return false;
                    default:
                        return false;
                }

            }
        });
    }

    @Override
    public boolean onInterceptBackPressed() {
        Fragment fragment = mFeedPageAdapter.getItem(mViewPager.getCurrentItem());
        if (fragment instanceof FragmentNavigator) {
            return ((FragmentNavigator)fragment).onInterceptBackPressed();
        }
        return false;
    }


    private void setupViewPager() {
        mFeedPageAdapter = new FeedPageAdapter(getChildFragmentManager());
        mFeedPageAdapter.addFragment(FeedFragment.newInstance(FeedFragment.FEED_TAG_MY_FEED), getString(R.string.my_feed));
        mFeedPageAdapter.addFragment(FeedFragment.newInstance(FeedFragment.FEED_TAG_LATEST), getString(R.string.latest));
        mFeedPageAdapter.addFragment(FeedFragment.newInstance(FeedFragment.FEED_TAG_STAFF_PICKS), getString(R.string
            .staff_picks));
        mFeedPageAdapter.addFragment(PerformanceTestFragment.newInstance(0), getString(R.string.performance_test));
        mViewPager.setAdapter(mFeedPageAdapter);



        //mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        getTablayout().setupWithViewPager(mViewPager);

        if (SessionManager.getInstance().isLoggedIn()) {
            mViewPager.setCurrentItem(0);
        } else {
            mViewPager.setCurrentItem(4);
        }

    }

    public void notifyDateChanged() {
        if (mFeedPageAdapter != null) {
            mFeedPageAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SearchView.SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
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
