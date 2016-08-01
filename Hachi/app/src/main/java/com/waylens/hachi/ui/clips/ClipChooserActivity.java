package com.waylens.hachi.ui.clips;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.MenuItemSelectEvent;

import com.waylens.hachi.ui.activities.BaseActivity;
import com.xfdingustc.snipe.vdb.Clip;


import org.greenrobot.eventbus.EventBus;


public class ClipChooserActivity extends BaseActivity {
    private EventBus mEventBus = EventBus.getDefault();

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
        setContentView(R.layout.activity_single_fragment);
        setupToolbar();
        Fragment fragment = ClipGridListFragment.newInstance(Clip.TYPE_MARKED, true, true);

        getFragmentManager().beginTransaction().add(R.id.fragment_content, fragment).commit();
    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.add_more_clips);
        getToolbar().setNavigationIcon(R.drawable.navbar_close);
        getToolbar().getMenu().clear();
        getToolbar().inflateMenu(R.menu.menu_add_clip);
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_to_enhance:
                        mEventBus.post(new MenuItemSelectEvent(item.getItemId()));
                        break;
                }

                return true;
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
//        mEventBus.register(this);
    }


    @Override
    public void onStop() {
        super.onStop();
//        mEventBus.unregister(this);
    }


}
