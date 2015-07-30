package com.waylens.hachi;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.view.Menu;
import android.view.MenuItem;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends BaseActivity {

    @Bind(R.id.main_tabs)
    TabLayout mMainTabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
    }

    private void initViews() {
        mMainTabs.addTab(mMainTabs.newTab().setIcon(R.drawable.ic_home));
        mMainTabs.addTab(mMainTabs.newTab().setIcon(R.drawable.ic_live));
        mMainTabs.addTab(mMainTabs.newTab().setIcon(R.drawable.ic_highlights));
        mMainTabs.addTab(mMainTabs.newTab().setIcon(R.drawable.ic_notifications));
        mMainTabs.addTab(mMainTabs.newTab().setIcon(R.drawable.ic_account));
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
}
