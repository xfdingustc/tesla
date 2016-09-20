package com.waylens.hachi.ui.clips;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
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

    private static final String EXTRA_IS_ADD_MORE = "is_add_more";

    private boolean mIsAddMore;

    public static void launch(Activity activity, boolean isAddMore) {
        Intent intent = new Intent(activity, ClipChooserActivity.class);
        intent.putExtra(EXTRA_IS_ADD_MORE, isAddMore);
        activity.startActivity(intent);
    }


    public static void launch(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, ClipChooserActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();

    }

    @Override
    protected void init() {
        super.init();
        Intent intent = getIntent();
        mIsAddMore = intent.getBooleanExtra(EXTRA_IS_ADD_MORE, true);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_single_fragment);
        setupToolbar();


        Fragment fragment = ClipGridListFragment.newInstance(Clip.TYPE_MARKED, true, mIsAddMore, !mIsAddMore);

        getFragmentManager().beginTransaction().add(R.id.fragment_content, fragment).commit();
    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        if (mIsAddMore) {
            getToolbar().setTitle(R.string.add_more_clips);
        } else {
            getToolbar().setTitle(R.string.choose_clips);
        }
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
}
