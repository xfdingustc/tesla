package com.waylens.hachi.ui.activities;


import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.view.Menu;
import android.view.MenuItem;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.fragments.AccountFragment;
import com.waylens.hachi.ui.fragments.HomeFragment;
import com.waylens.hachi.ui.fragments.LiveFragment;
import com.waylens.hachi.ui.fragments.NotificationFragment;

import butterknife.Bind;

public class MainActivity extends BaseActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String TAB_HOME_TAG = "home";
    private static final String TAB_LIVE_TAG = "live";
    private static final String TAB_HIGHLIGHTS_TAG = "highlights";
    private static final String TAB_NOTIFICATIONS_TAG = "notification";
    private static final String TAB_ACCOUNT_TAG = "account";

    @Bind(R.id.main_tabs)
    TabLayout mMainTabs;


    private class BottomTab {
        private int mIconRes;
        private String mTabTag;

        public BottomTab(int iconRes, String tabTag) {
            this.mIconRes = iconRes;
            this.mTabTag = tabTag;
        }
    }

    private BottomTab mTabList[] = {
        new BottomTab(R.drawable.ic_home, TAB_HOME_TAG),
        new BottomTab(R.drawable.ic_live, TAB_LIVE_TAG),
        new BottomTab(R.drawable.ic_highlights, TAB_HIGHLIGHTS_TAG),
        new BottomTab(R.drawable.ic_notifications, TAB_NOTIFICATIONS_TAG),
        new BottomTab(R.drawable.ic_account, TAB_ACCOUNT_TAG)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_main);
        for (int i = 0; i < mTabList.length; i++) {
            TabLayout.Tab tab = mMainTabs.newTab();
            tab.setIcon(mTabList[i].mIconRes);
            tab.getIcon().setColorFilter(getResources().getColor(R.color.material_grey_500),
                PorterDuff.Mode.MULTIPLY);
            mMainTabs.addTab(tab);
        }

        mMainTabs.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tab.getIcon().setColorFilter(getResources().getColor(R.color.style_color_primary),
                    PorterDuff.Mode.MULTIPLY);
                if (tab.getPosition() == 2) {
                    startActivity(new Intent(MainActivity.this, CameraListActivity.class));
                } else {
                    String tag = mTabList[tab.getPosition()].mTabTag;
                    switchFragment(tag);
                }

                tab.getIcon().setColorFilter(getResources().getColor(R.color.style_color_primary),
                    PorterDuff.Mode.MULTIPLY);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                tab.getIcon().setColorFilter(getResources().getColor(R.color.material_grey_500),
                    PorterDuff.Mode.MULTIPLY);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        setDefaultFirstFragment(TAB_HOME_TAG);
        TabLayout.Tab tab = mMainTabs.getTabAt(0);
        tab.getIcon().setColorFilter(getResources().getColor(R.color.style_color_primary),
            PorterDuff.Mode.MULTIPLY);
    }

    private void setDefaultFirstFragment(String tag) {
        switchFragment(tag);
    }

    private void switchFragment(String tag) {
        Fragment fragment;
        if (tag.equals(TAB_HOME_TAG)) {
            fragment = new HomeFragment();
        } else if (tag.equals(TAB_LIVE_TAG)) {
            fragment = new LiveFragment();
        } else if (tag.equals(TAB_NOTIFICATIONS_TAG)) {
            fragment = new NotificationFragment();
        } else {
            fragment = new AccountFragment();
        }
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_content, fragment);
        fragmentTransaction.addToBackStack(null);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.root_container);
        if (fragment != null) {
            getFragmentManager().beginTransaction().remove(fragment).commit();
        } else {
            super.onBackPressed();
        }
    }
}
