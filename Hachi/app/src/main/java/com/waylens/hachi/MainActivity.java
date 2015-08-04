package com.waylens.hachi;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.transee.viditcam.app.CameraListActivity;
import com.waylens.camera.CameraDiscovery;

import butterknife.Bind;

public class MainActivity extends BaseActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    @Bind(R.id.main_tabs)
    TabLayout mMainTabs;

    private class BottomTab {
        private int mIconRes;
        private int mTabTitleRes;

        public BottomTab(int iconRes, int tabTitleRes) {
            this.mIconRes = iconRes;
            this.mTabTitleRes = tabTitleRes;
        }
    }

    private BottomTab mTabList[] = {
        new BottomTab(R.drawable.ic_home, R.string.home),
        new BottomTab(R.drawable.ic_live, R.string.live),
        new BottomTab(R.drawable.ic_highlights, R.string.hightlights),
        new BottomTab(R.drawable.ic_notifications, R.string.notification),
        new BottomTab(R.drawable.ic_account, R.string.account)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //discoveryCamera();
    }


    /**
     * This method is used to demonstrate the usage of CameraDiscovery.
     * <p/>
     * TODO: removed it [Richard]
     */
    private void discoveryCamera() {
        final TextView textView = (TextView) findViewById(R.id.tv_test);
        textView.setTextColor(Color.RED);
        final Handler handler = new Handler();

        CameraDiscovery.discoverCameras(this, new CameraDiscovery.Callback() {
            @Override
            public void onCameraFound(final NsdServiceInfo cameraService) {
                Log.e("test", "Name: " + cameraService.getServiceName());
                Log.e("test", "Port: " + cameraService.getPort());
                Log.e("test", "Host: " + cameraService.getHost());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(cameraService.getServiceName() + ":" + cameraService.getPort());
                    }
                });
            }

            @Override
            public void onError(int errorCode) {
                Log.e("test", "errorCode: " + errorCode);
            }
        });
    }

    private void initViews() {
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

                if (tab.getPosition() == 2) {
                    startActivity(new Intent(MainActivity.this, CameraListActivity.class));
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
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        //mToolbar.setTitle(R.string.);
    }

    @Override
    protected void onStop() {
        CameraDiscovery.stopDiscovery();
        super.onStop();
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
