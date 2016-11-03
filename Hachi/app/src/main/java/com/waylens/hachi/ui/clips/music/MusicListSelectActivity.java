package com.waylens.hachi.ui.clips.music;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.utils.rxjava.RxBus;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;


import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by Xiaofei on 2016/8/17.
 */
public class MusicListSelectActivity extends BaseActivity {
    private static final String TAG = MusicListSelectActivity.class.getSimpleName();

    public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 100;

    public Subscription mRxSubscription;

    public static void launchForResult(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, MusicListSelectActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!mRxSubscription.isUnsubscribed()) {
            mRxSubscription.unsubscribe();
        }
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            super.onBackPressed();
        } else {
            getFragmentManager().popBackStack();
            getToolbar().setTitle(R.string.add_music);
        }
    }

    @Override
    protected void init() {
        super.init();
        initViews();
        initEventHandler();
    }


    private void initViews() {
        setContentView(R.layout.activity_music_list_select);

        setupToolbar();

        MusicCategoryFragment fragment = new MusicCategoryFragment();
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    private void initEventHandler() {
        mRxSubscription = RxBus.getDefault().toObserverable(MusicCategorySelectEvent.class)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<MusicCategorySelectEvent>() {
                @Override
                public void onNext(MusicCategorySelectEvent musicCategorySelectEvent) {
                    getToolbar().setTitle(musicCategorySelectEvent.category.category);
                }
            });
    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.add_music);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }


}
