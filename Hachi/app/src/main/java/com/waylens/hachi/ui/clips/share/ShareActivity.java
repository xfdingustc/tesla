package com.waylens.hachi.ui.clips.share;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.clips.ClipPlayActivity;
import com.waylens.hachi.ui.clips.EnhanceFragment;
import com.waylens.hachi.ui.clips.EnhancementActivity;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor2;
import com.waylens.hachi.ui.helpers.MomentShareHelper;

import org.json.JSONObject;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/6/16.
 */
public class ShareActivity extends ClipPlayActivity {
    private static final String TAG = ShareActivity.class.getSimpleName();
    private static final String EXTRA_PLAYLIST_ID = "playlist_id";

    private int mPlayListId;

    private ShareContentAdapter mAdapter;

    private MomentShareHelper mShareHelper;

    @BindView(R.id.share_description)
    RecyclerView mShareRv;



    @BindView(R.id.collapse_toolbar)
    CollapsingToolbarLayout mCollapseToolbar;

    public static void launch(Activity activity, int playListId) {
        Intent intent = new Intent(activity, ShareActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playListId);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Override
    protected void init() {
        super.init();
        mPlayListId = getIntent().getIntExtra(EXTRA_PLAYLIST_ID, -1);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_share);
        setupToolbar();
        mPlaylistEditor = new PlayListEditor2(mVdbRequestQueue, mPlayListId);
        mPlaylistEditor.reconstruct();
        embedVideoPlayFragment();
        mShareRv.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new ShareContentAdapter();
        mShareRv.setAdapter(mAdapter);
    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.share);

        getToolbar().setTitleTextColor(getResources().getColor(R.color.app_text_color_primary));
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        getToolbar().inflateMenu(R.menu.menu_share2);
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.share:
                        doShareMoment();
                        break;
                }
                return true;
            }
        });

        mCollapseToolbar.setTitleEnabled(false);
    }

    private void doShareMoment() {
//        mShareHelper = new MomentShareHelper(this, mVdbRequestQueue, this);
        String title = mAdapter.getMomentTitle();
        String[] tags = new String[]{"Shanghai", "car"};
        Activity activity = this;
        int audioID = EnhanceFragment.DEFAULT_AUDIO_ID;
        JSONObject gaugeSettings = null;
        if (activity instanceof EnhancementActivity) {
            audioID = ((EnhancementActivity) activity).getAudioID();
            gaugeSettings = ((EnhancementActivity) activity).getGaugeSettings();
        }

        Logger.t(TAG).d("share title: " + title);
//        mShareHelper.shareMoment(PLAYLIST_SHARE, title, tags, mSocialPrivacy, audioID, gaugeSettings, mIsFacebookShareChecked);
    }
}
