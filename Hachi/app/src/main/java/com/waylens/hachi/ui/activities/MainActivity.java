package com.waylens.hachi.ui.activities;


import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.view.Menu;
import android.view.MenuItem;

import com.waylens.hachi.BuildConfig;
import com.waylens.hachi.R;
import com.waylens.hachi.gcm.RegistrationIntentService;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.fragments.CameraConnectFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.fragments.HomeFragment;
import com.waylens.hachi.ui.fragments.LiveFragment;
import com.waylens.hachi.ui.fragments.Refreshable;
import com.waylens.hachi.ui.fragments.SettingsFragment;
import com.waylens.hachi.ui.fragments.StoriesFragment;
import com.waylens.hachi.utils.PushUtils;

import butterknife.Bind;


public class MainActivity extends BaseActivity implements AppBarLayout.OnOffsetChangedListener, TabSwitchable {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int TAB_TAG_BOOKMARK = 0;
    private static final int TAB_TAG_STORIES = 1;
    private static final int TAB_TAG_LIVE_VIEW = 2;
    private static final int TAB_TAG_SOCIAL = 3;
    private static final int TAB_TAG_SETTINGS = 4;

    @Bind(R.id.main_tabs)
    TabLayout mMainTabs;

    @Bind(R.id.app_bar_layout)
    AppBarLayout mAppBarLayout;

    @Bind(R.id.root_container)
    CoordinatorLayout mRootView;

    private BottomTab mTabList[] = {
        new BottomTab(R.drawable.tab_bookmark, R.drawable.tab_bookmark_active, TAB_TAG_BOOKMARK),
        new BottomTab(R.drawable.tab_stories, R.drawable.tab_stories_active, TAB_TAG_STORIES),
        new BottomTab(R.drawable.tab_liveview, R.drawable.tab_liveview_active, TAB_TAG_LIVE_VIEW),
        new BottomTab(R.drawable.tab_social, R.drawable.tab_social_active, TAB_TAG_SOCIAL),
        new BottomTab(R.drawable.tab_settings, R.drawable.tab_settings_active, TAB_TAG_SETTINGS)
    };

    private Bundle fragmentArgs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAppBarLayout.addOnOffsetChangedListener(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mAppBarLayout.removeOnOffsetChangedListener(this);

    }

    @Override
    protected void init() {
        super.init();
        initViews();
        if (SessionManager.getInstance().isLoggedIn() && PushUtils.checkGooglePlayServices(this)) {
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void initViews() {
        setContentView(R.layout.activity_main);
        for (int i = 0; i < mTabList.length; i++) {
            TabLayout.Tab tab = mMainTabs.newTab();
            tab.setIcon(mTabList[i].normalIconRes);
            //tab.getIcon().setColorFilter(getResources().getColor(R.color.material_grey_500),
            //        PorterDuff.Mode.MULTIPLY);
            mMainTabs.addTab(tab);
        }

        mMainTabs.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                BottomTab bottomTab = mTabList[tab.getPosition()];
                if (bottomTab.activeIconRes == 0) {
                    tab.getIcon().setColorFilter(getResources().getColor(R.color.style_color_primary),
                        PorterDuff.Mode.MULTIPLY);
                } else {
                    tab.setIcon(bottomTab.activeIconRes);
                }
                switchFragment(bottomTab.tabTag);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                BottomTab bottomTab = mTabList[tab.getPosition()];
                if (bottomTab.activeIconRes == 0) {
                    tab.getIcon().setColorFilter(getResources().getColor(R.color.material_grey_500),
                        PorterDuff.Mode.MULTIPLY);
                } else {
                    tab.setIcon(bottomTab.normalIconRes);
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        if (!SessionManager.getInstance().isLoggedIn()) {
            mMainTabs.getTabAt(TAB_TAG_SOCIAL).select();
        } else {
            mMainTabs.getTabAt(TAB_TAG_LIVE_VIEW).select();
        }
    }

    void enableRefresh(boolean enabled) {
        Fragment currentFragment = getFragmentManager().findFragmentById(R.id.fragment_content);
        if (currentFragment instanceof Refreshable) {
            ((Refreshable) currentFragment).enableRefresh(enabled);
        }
    }

    private void switchFragment(int tag) {

        Fragment fragment;
        switch (tag) {
            case TAB_TAG_SOCIAL:
                fragment = new HomeFragment();
                break;
            case TAB_TAG_BOOKMARK:
                fragment = new LiveFragment();
                if (fragmentArgs != null) {
                    fragment.setArguments(fragmentArgs);
                }
                break;
            case TAB_TAG_LIVE_VIEW:
                fragment = new CameraConnectFragment();
                break;
            case TAB_TAG_STORIES:
                fragment = new StoriesFragment();
                break;
            case TAB_TAG_SETTINGS:
                //fragment = new AccountFragment();
                fragment = new SettingsFragment();
                break;
            default:
                fragment = new HomeFragment();
        }

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_content, fragment);
        fragmentTransaction.commit();
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        //mToolbar.setTitle(R.string.);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            String version = "Version: " + BuildConfig.VERSION_NAME;
            Snackbar.make(mRootView, version, Snackbar.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.root_container);
        if (fragment instanceof FragmentNavigator
            && ((FragmentNavigator) fragment).onInterceptBackPressed()) {
            return;
        }

        fragment = getFragmentManager().findFragmentById(R.id.fragment_content);
        if (fragment instanceof FragmentNavigator
            && ((FragmentNavigator) fragment).onInterceptBackPressed()) {
            return;
        }
        super.onBackPressed();
    }


    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        enableRefresh(i == 0);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void switchTab(int position, Bundle args) {
        if (position < 0 || position > mMainTabs.getTabCount()) {
            return;
        }
        fragmentArgs = args;
        mMainTabs.getTabAt(position).select();
    }

    static class BottomTab {
        int normalIconRes;
        int activeIconRes;
        int tabTag;

        public BottomTab(int normalIconRes, int activeIconRes, int tabTag) {
            this.normalIconRes = normalIconRes;
            this.activeIconRes = activeIconRes;
            this.tabTag = tabTag;
        }
    }
}
