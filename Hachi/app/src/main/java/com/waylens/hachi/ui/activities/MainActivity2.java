package com.waylens.hachi.ui.activities;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.gcm.RegistrationIntentService;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.fragments.CameraConnectFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.fragments.HomeFragment;
import com.waylens.hachi.ui.fragments.LiveFragment;
import com.waylens.hachi.ui.fragments.SettingsFragment;
import com.waylens.hachi.ui.fragments.StoriesFragment;
import com.waylens.hachi.utils.PushUtils;

import butterknife.Bind;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2016/1/15.
 */
public class MainActivity2 extends BaseActivity {
    private static final String TAG = MainActivity2.class.getSimpleName();
    private ActionBarDrawerToggle mDrawerToggle;

    private Bundle fragmentArgs;

    public static final int TAB_TAG_BOOKMARK = 0;
    public static final int TAB_TAG_STORIES = 1;
    public static final int TAB_TAG_LIVE_VIEW = 2;
    public static final int TAB_TAG_SOCIAL = 3;
    public static final int TAB_TAG_SETTINGS = 4;

    private int mCurrentNavMenuId;

    private SessionManager mSessionManager = SessionManager.getInstance();

    @Bind(R.id.drawerLayout)
    DrawerLayout mDrawerLayout;

    @Bind(R.id.root_container)
    CoordinatorLayout mRootView;

    @Bind(R.id.navView)
    NavigationView mNavView;

    CircleImageView mUserAvatar;
    TextView mUsername;
    TextView mEmail;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        initViews();
        if (mSessionManager.isLoggedIn() && PushUtils.checkGooglePlayServices(this)) {
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }

    private void initViews() {
        setContentView(R.layout.activity_main2);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setupActionBarToggle();
        setupNavigationView();



        if (!mSessionManager.isLoggedIn()) {
            switchFragment(TAB_TAG_SOCIAL);
            mCurrentNavMenuId = R.id.social;
        } else {
            switchFragment(TAB_TAG_LIVE_VIEW);
            mCurrentNavMenuId = R.id.liveView;
        }

        // update user profile;
        if (mSessionManager.isLoggedIn()) {
            Logger.t(TAG).d("mUserAvatar: " + mUserAvatar + " url: " + mSessionManager.getAvatarUrl());
            ImageLoader.getInstance().displayImage(mSessionManager.getAvatarUrl(), mUserAvatar);
            mUsername.setText(mSessionManager.getUserName());
            
        }
    }


    public void switchFragment(int tag) {

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


    private void setupNavigationView() {
        mUserAvatar = (CircleImageView)mNavView.getHeaderView(0).findViewById(R.id.civUserAvatar);
        mUsername = (TextView)mNavView.getHeaderView(0).findViewById(R.id.tvUserName);
        mEmail = (TextView)mNavView.getHeaderView(0).findViewById(R.id.tvEmail);
        mNavView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                mDrawerLayout.closeDrawers();
                if (item.getItemId() == mCurrentNavMenuId) {
                    return true;
                }

                mNavView.getMenu().findItem(mCurrentNavMenuId).setChecked(false);
                item.setChecked(true);
                mCurrentNavMenuId = item.getItemId();

                switch (item.getItemId()) {
                    case R.id.setting:
                        switchFragment(TAB_TAG_SETTINGS);
                        break;
                    case R.id.social:
                        switchFragment(TAB_TAG_SOCIAL);
                        break;
                    case R.id.bookmark:
                        switchFragment(TAB_TAG_BOOKMARK);
                        break;
                    case R.id.stories:
                        switchFragment(TAB_TAG_STORIES);
                        break;
                    case R.id.liveView:
                        switchFragment(TAB_TAG_LIVE_VIEW);
                        break;
                }
                return true;
            }
        });
    }


    private void setupActionBarToggle() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string
                .drawer_open, R.string.drawer_close);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

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
}
