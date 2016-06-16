package com.waylens.hachi.ui.clips.preview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.clips.ClipPlayActivity;
import com.waylens.hachi.ui.clips.enhance.EnhanceActivity2;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor2;
import com.waylens.hachi.ui.clips.share.ShareActivity;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;

import java.util.ArrayList;

/**
 * Created by Xiaofei on 2016/6/16.
 */
public class PreviewActivity extends ClipPlayActivity {

    private Clip mClip;

    public static final int PLAYLIST_INDEX = 0x100;


    public static void launch(Activity activity, ArrayList<Clip> clip) {
        Intent intent = new Intent(activity, PreviewActivity.class);
        intent.putParcelableArrayListExtra("clip", clip);
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
        ArrayList<Clip> clip = getIntent().getParcelableArrayListExtra("clip");

        mClip = clip.get(0);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_preview);
        setupToolbar();
        buildPlaylist();
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().inflateMenu(R.menu.menu_clip_play_fragment);
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_to_share:
                        ShareActivity.launch(PreviewActivity.this, mPlaylistEditor.getPlaylistId());
                        break;
                    case R.id.menu_to_enhance:
                        EnhanceActivity2.launch(PreviewActivity.this, mPlaylistEditor.getPlaylistId());
                        break;
                    case R.id.menu_to_modify:
                        break;
                    case R.id.menu_to_delete:
                        break;
                }

                return true;
            }
        });
    }

    private void buildPlaylist() {
        mPlaylistEditor = new PlayListEditor2(mVdbRequestQueue, PLAYLIST_INDEX);
        mPlaylistEditor.build(mClip, new PlayListEditor2.OnBuildCompleteListener() {
            @Override
            public void onBuildComplete(ClipSet clipSet) {
                embedVideoPlayFragment();
            }
        });
    }


}
