package com.waylens.hachi.ui.activities;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.gcm.RegistrationIntentService;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.VideoFragment;
import com.waylens.hachi.ui.fragments.CameraConnectFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.fragments.MomentFragment;
import com.waylens.hachi.ui.fragments.SettingsFragment;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.PushUtils;

import butterknife.Bind;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2016/1/15.
 */
public class MainActivity extends BaseActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private ActionBarDrawerToggle mDrawerToggle;

    private Bundle fragmentArgs;

    public static final int TAB_TAG_VIDEO = 0;
    public static final int TAB_TAG_LIVE_VIEW = 1;
    public static final int TAB_TAG_MOMENTS = 2;
    public static final int TAB_TAG_SETTINGS = 3;

    private int mCurrentNavMenuId;


    private BiMap<Integer, Integer> mMenuId2Tab = HashBiMap.create();

    private int[] mToolbarTitles = new int[]{
        R.string.video,
        R.string.live_view,
        R.string.moments,
        R.string.settings
    };

    private BaseFragment[] mFragmentList = new BaseFragment[]{
        new VideoFragment(),
        new CameraConnectFragment(),
        new MomentFragment(),
        new SettingsFragment()
    };

    private Fragment mCurrentFragment = null;


    private SessionManager mSessionManager = SessionManager.getInstance();

    @Bind(R.id.drawerLayout)
    DrawerLayout mDrawerLayout;

    @Bind(R.id.root_container)
    CoordinatorLayout mRootView;

    @Bind(R.id.navView)
    NavigationView mNavView;

    @Bind(R.id.tabs)
    TabLayout mTabLayout;


    CircleImageView mUserAvatar;
    TextView mUsername;
    TextView mEmail;

    private Snackbar mReturnSnackBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Override
    protected void onResume() {
        super.onResume();
        refressNavHeaderView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mCurrentFragment != null) {
            return mCurrentFragment.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void init() {
        super.init();

        mMenuId2Tab.put(R.id.moments, TAB_TAG_MOMENTS);
        mMenuId2Tab.put(R.id.setting, TAB_TAG_SETTINGS);
        mMenuId2Tab.put(R.id.video, TAB_TAG_VIDEO);
        mMenuId2Tab.put(R.id.liveView, TAB_TAG_LIVE_VIEW);

        initViews();
        if (mSessionManager.isLoggedIn() && PushUtils.checkGooglePlayServices(this)) {
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }


    private void initViews() {
        setContentView(R.layout.activity_main);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setupActionBarToggle();
        setupNavigationView();


        if (!mSessionManager.isLoggedIn()) {
            switchFragment(TAB_TAG_MOMENTS);
        } else {
            switchFragment(TAB_TAG_LIVE_VIEW);

        }


        // update user profile;
        if (mSessionManager.isLoggedIn()) {
            Logger.t(TAG).d("mUserAvatar: " + mUserAvatar + " url: " + mSessionManager.getAvatarUrl());
            ImageLoader.getInstance().displayImage(mSessionManager.getAvatarUrl(), mUserAvatar);
            mUsername.setText(mSessionManager.getUserName());

        }
    }


    public void switchFragment(int tag) {
        int menuId = mMenuId2Tab.inverse().get(tag);

        // When init current menu id is 0. so here must check if MenuItem is null
        MenuItem item = mNavView.getMenu().findItem(mCurrentNavMenuId);
        if (item != null) {
            item.setChecked(false);
        }


        mCurrentNavMenuId = menuId;
        mNavView.getMenu().findItem(mCurrentNavMenuId).setChecked(true);


        Toolbar toolbar = getToolbar();
        if (toolbar != null) {
            toolbar.setTitle(mToolbarTitles[tag]);
        }


        BaseFragment fragment = mFragmentList[tag];

        if (tag == TAB_TAG_MOMENTS || tag == TAB_TAG_VIDEO) {
            mTabLayout.setVisibility(View.VISIBLE);
            if (fragment.getViewPager() != null) {
                mTabLayout.setupWithViewPager(fragment.getViewPager());
            }
        } else {
            mTabLayout.setVisibility(View.GONE);
        }

        mToolbar.getMenu().clear();
        if (tag == TAB_TAG_VIDEO) {
            mToolbar.inflateMenu(R.menu.menu_video_fragment);
        }

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (mCurrentFragment == null) {
            transaction.add(R.id.fragment_content, fragment).commit();
            mCurrentFragment = fragment;
        } else {
            if (!fragment.isAdded()) {
                transaction.hide(mCurrentFragment).add(R.id.fragment_content, fragment).commit();
            } else {
                transaction.hide(mCurrentFragment).show(fragment).commit();
            }
            mCurrentFragment = fragment;
        }
    }




    private void setupNavigationView() {
        mUserAvatar = (CircleImageView) mNavView.getHeaderView(0).findViewById(R.id.civUserAvatar);
        mUsername = (TextView) mNavView.getHeaderView(0).findViewById(R.id.tvUserName);
        mEmail = (TextView) mNavView.getHeaderView(0).findViewById(R.id.tvEmail);
        mNavView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                switch (item.getItemId()) {
//                    case R.id.changeTheme:
//                        onToggleAppThemeClicked();
//
//                        break;
                    default:
                        mDrawerLayout.closeDrawers();
                        if (item.getItemId() == mCurrentNavMenuId) {
                            return true;
                        }
                        int tab = mMenuId2Tab.get(item.getItemId());
                        switchFragment(tab);
                }

                return true;
            }


        });

        mUserAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onUserAvatarClicked();
            }
        });
    }

    private void refressNavHeaderView() {
        if (mSessionManager.isLoggedIn()) {
            mUsername.setText(mSessionManager.getUserName());

        } else {
            mUsername.setText(getText(R.string.click_2_login));
        }
    }

    private void onUserAvatarClicked() {
        if (mSessionManager.isLoggedIn()) {
            UserProfileActivity.launch(this, mSessionManager.getUserId());
        } else {
            LoginActivity.launch(this);
        }
    }

    private void onToggleAppThemeClicked() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content(getText(R.string.change_theme_hint))
            .negativeText(android.R.string.cancel)
            .positiveText(android.R.string.ok)
            .callback(new MaterialDialog.ButtonCallback() {
                @Override
                public void onPositive(MaterialDialog dialog) {
                    super.onPositive(dialog);
                    toggleAppTheme();
                    finish();
                }
            })
            .show();

    }

    private void toggleAppTheme() {
        String appTheme = PreferenceUtils.getString(PreferenceUtils.APP_THEME, "dark");
        if (appTheme.equals("dark")) {
            getApplication().setTheme(R.style.LightTheme);
            PreferenceUtils.putString(PreferenceUtils.APP_THEME, "light");

        } else {
            getApplication().setTheme(R.style.DarkTheme);
            PreferenceUtils.putString(PreferenceUtils.APP_THEME, "dark");
        }

    }


    private void setupActionBarToggle() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string
            .drawer_open, R.string.drawer_close);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
            return;
        }
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


        if (mReturnSnackBar != null && mReturnSnackBar.isShown()) {
            super.onBackPressed();
        } else {
            mReturnSnackBar = Snackbar.make(mDrawerLayout, getText(R.string.backpressed_hint),
                Snackbar.LENGTH_LONG);
            mReturnSnackBar.show();
        }


    }
}
