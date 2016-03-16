package com.waylens.hachi.ui.activities;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.gcm.RegistrationIntentService;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.fragments.MomentFragment;
import com.waylens.hachi.ui.fragments.SettingsFragment;
import com.waylens.hachi.ui.fragments.VideoFragment;
import com.waylens.hachi.ui.fragments.camerapreview.CameraPreviewFragment;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.PushUtils;

import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2016/1/15.
 */
public class MainActivity extends BaseActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
//    private ActionBarDrawerToggle mDrawerToggle;

    public static final int TAB_TAG_VIDEO = 0;
    public static final int TAB_TAG_LIVE_VIEW = 1;
    public static final int TAB_TAG_MOMENTS = 2;
    public static final int TAB_TAG_SETTINGS = 3;

    private int mCurrentNavMenuId;

    //private BiMap<Integer, Integer> mMenuId2Tab = HashBiMap.create();
    private Map<Integer, Integer> mMenuId2Tab = new HashMap<>();
    private Map<Integer, Integer> mTab2MenuId = new HashMap<>();

    private BaseFragment[] mFragmentList = new BaseFragment[]{
            new VideoFragment(),
            new CameraPreviewFragment(),
            new MomentFragment(),
            new SettingsFragment()
    };

    private BaseFragment mCurrentFragment = null;
    private SessionManager mSessionManager = SessionManager.getInstance();

    @Bind(R.id.drawerLayout)
    DrawerLayout mDrawerLayout;

    @Bind(R.id.navView)
    NavigationView mNavView;


    private CircleImageView mUserAvatar;
    private TextView mUsername;
    private TextView mEmail;

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
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
//            mTabLayout.setVisibility(View.GONE);
//            mToolbar.setVisibility(View.GONE);
            hideSystemUI(true);

        } else {
//            mTabLayout.setVisibility(View.VISIBLE);
//            mToolbar.setVisibility(View.VISIBLE);
            hideSystemUI(false);
        }
    }

    @Override
    protected void init() {
        super.init();

        mMenuId2Tab.put(R.id.moments, TAB_TAG_MOMENTS);
        mMenuId2Tab.put(R.id.setting, TAB_TAG_SETTINGS);
        mMenuId2Tab.put(R.id.video, TAB_TAG_VIDEO);
        mMenuId2Tab.put(R.id.liveView, TAB_TAG_LIVE_VIEW);

        mTab2MenuId.put(TAB_TAG_MOMENTS, R.id.moments);
        mTab2MenuId.put(TAB_TAG_SETTINGS, R.id.setting);
        mTab2MenuId.put(TAB_TAG_VIDEO, R.id.video);
        mTab2MenuId.put(TAB_TAG_LIVE_VIEW, R.id.liveView);

        initViews();
        if (mSessionManager.isLoggedIn() && PushUtils.checkGooglePlayServices(this)) {
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }


    private void initViews() {
        setContentView(R.layout.activity_main);

        setupNavigationView();

        if (mSessionManager.isLoggedIn()) {
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

    private void hideSystemUI(boolean hide) {
        if (hide) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getWindow().setAttributes(lp);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        } else {
            WindowManager.LayoutParams attr = getWindow().getAttributes();
            attr.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setAttributes(attr);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }

    public void switchFragment(int tag) {
        int menuId = mTab2MenuId.get(tag);

        // When init current menu id is 0. so here must check if MenuItem is null
        MenuItem item = mNavView.getMenu().findItem(mCurrentNavMenuId);
        if (item != null) {
            item.setChecked(false);
        }


        mCurrentNavMenuId = menuId;
        mNavView.getMenu().findItem(mCurrentNavMenuId).setChecked(true);

        BaseFragment fragment = mFragmentList[tag];


        /*
         * Here we have to go through this detach, replace, attach, and addToBackStack way,
         * to solve the ChildFragmentManager/ViewPager bug of Android
         * https://code.google.com/p/android/issues/detail?id=42601
         */
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        boolean mUseHideAndShow = true;
        if (mUseHideAndShow) {
            if (mCurrentFragment == null) {
                transaction.add(R.id.fragment_content, fragment).commit();

            } else {
                if (!fragment.isAdded()) {
                    transaction.hide(mCurrentFragment).add(R.id.fragment_content, fragment).commit();
                } else {
                    transaction.hide(mCurrentFragment).show(fragment).commit();
                }
            }
        } else {
            if (mCurrentFragment != null) {
                transaction.detach(mCurrentFragment);
            }
            transaction.replace(R.id.fragment_content, fragment)
                    .attach(fragment)
                    //.addToBackStack(null)
                    .commit();
        }
        mCurrentFragment = fragment;
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

    public void showDrawer() {
        mDrawerLayout.openDrawer(Gravity.LEFT);
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
