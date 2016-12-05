package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.WindowManager;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.gcm.RegistrationIntentService;
import com.waylens.hachi.ui.clips.ClipVideoFragment;
import com.waylens.hachi.ui.community.CommunityFragment;
import com.waylens.hachi.ui.fragments.ReSelectableTab;
import com.waylens.hachi.ui.leaderboard.LeaderboardFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.liveview.LiveViewActivity;
import com.waylens.hachi.ui.settings.AccountFragment;
import com.waylens.hachi.view.bottombar.BottomBar;
import com.waylens.hachi.view.bottombar.OnCenterTabClickListener;
import com.waylens.hachi.view.bottombar.OnTabReselectListener;
import com.waylens.hachi.view.bottombar.OnTabSelectListener;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;


/**
 * Created by Xiaofei on 2016/1/15.
 */
public class MainActivity extends BaseActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
//    private ActionBarDrawerToggle mDrawerToggle;


    public static final int TAB_TAG_MOMENTS = 0;
    public static final int TAB_TAG_LEADERBOARD = 1;
    public static final int TAB_TAG_VIDEO = 2;
    public static final int TAB_TAG_ACCOUNT = 3;


    public static final int REQUEST_CODE_SIGN_UP_FROM_MOMENTS = 100;


    private Map<Integer, Integer> mTab2MenuId = new HashMap<>();

    private Fragment[] mFragmentList = new Fragment[]{
        new CommunityFragment(),
        new LeaderboardFragment(),
        new ClipVideoFragment(),
        new AccountFragment(),

    };

    private Fragment mCurrentFragment = null;


    @BindView(R.id.bottomBar)
    BottomBar bottomBar;


    private boolean mIsRestored;

    private Snackbar mReturnSnackBar;

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsRestored = savedInstanceState != null;
        init();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            hideSystemUI(true);
//            mBottomBar.setVisibility(View.GONE);
        } else {
            hideSystemUI(false);
//            mBottomBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void init() {
        super.init();

        mTab2MenuId.put(TAB_TAG_MOMENTS, R.id.moments);
        mTab2MenuId.put(TAB_TAG_ACCOUNT, R.id.setting);
        mTab2MenuId.put(TAB_TAG_VIDEO, R.id.video);
        mTab2MenuId.put(TAB_TAG_LEADERBOARD, R.id.leaderboard);


        initViews();

        RegistrationIntentService.launch(this);

    }


    private void initViews() {
        setContentView(R.layout.activity_main);

        bottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(@IdRes int tabId) {
                switch (tabId) {
                    case R.id.moments:
                        switchFragment(TAB_TAG_MOMENTS);
                        break;
                    case R.id.account:
                        switchFragment(TAB_TAG_ACCOUNT);
                        break;
                    case R.id.video:
                        switchFragment(TAB_TAG_VIDEO);
                        break;
                    case R.id.leaderboard:
                        switchFragment(TAB_TAG_LEADERBOARD);
                        break;
                }
            }
        });

        bottomBar.setOnTabReselectListener(new OnTabReselectListener() {
            @Override
            public void onTabReSelected(@IdRes int tabId) {
                if (mCurrentFragment instanceof ReSelectableTab) {
                    ((ReSelectableTab)mCurrentFragment).onReSelected();
                }
            }
        });

        bottomBar.setOnCenterTabClickListener(new OnCenterTabClickListener() {
            @Override
            public void onCenterTabClick() {
                LiveViewActivity.launch(MainActivity.this);
            }
        });

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

//        mNavView.getMenu().findItem(mCurrentNavMenuId).setChecked(true);
        if (mCurrentFragment != null && mCurrentFragment instanceof FragmentNavigator) {
            ((FragmentNavigator) mCurrentFragment).onDeselected();
        }
        Fragment fragment = mFragmentList[tag];
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        boolean mUseHideAndShow = true;

        if (mCurrentFragment == null) {
            transaction.add(R.id.fragment_content, fragment).commit();

        } else {
            if (!fragment.isAdded()) {
                transaction.hide(mCurrentFragment).add(R.id.fragment_content, fragment).commit();
            } else {
                transaction.hide(mCurrentFragment).show(fragment).commit();
            }


        }
        mCurrentFragment = fragment;

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_UP_FROM_MOMENTS:
                if (resultCode == RESULT_OK && (mCurrentFragment instanceof CommunityFragment)) {
                    Logger.t(TAG).e("notifyDateChanged");
                    ((CommunityFragment) mCurrentFragment).notifyDateChanged();
                }
                break;
//            case SearchView.SPEECH_REQUEST_CODE:
//                if (resultCode == RESULT_OK && (mCurrentFragment instanceof CommunityFragment)) {
//                    mCurrentFragment.onActivityResult(requestCode, resultCode, data);
//                }
//                break;


            default:
                super.onActivityResult(requestCode, resultCode, data);
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
            mReturnSnackBar = Snackbar.make(bottomBar, getText(R.string.backpressed_hint), Snackbar.LENGTH_LONG);
            mReturnSnackBar.show();
        }
    }
}
