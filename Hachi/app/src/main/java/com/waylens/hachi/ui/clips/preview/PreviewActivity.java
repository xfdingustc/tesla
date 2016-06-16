package com.waylens.hachi.ui.clips.preview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.clips.player.ClipPlayFragment;
import com.waylens.hachi.ui.clips.player.ClipUrlProvider;
import com.waylens.hachi.ui.clips.player.PlaylistEditor;
import com.waylens.hachi.ui.clips.player.PlaylistUrlProvider;
import com.waylens.hachi.ui.clips.player.UrlProvider;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor2;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;
import com.waylens.hachi.vdb.Playlist;

import java.util.ArrayList;

/**
 * Created by Xiaofei on 2016/6/16.
 */
public class PreviewActivity extends BaseActivity {

    private Clip mClip;

    public static final int PLAYLIST_INDEX = 0x100;

    private PlayListEditor2 mPlaylistEditor;

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
        buildPlaylist();
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

    private void embedVideoPlayFragment() {

        UrlProvider vdtUriProvider = new PlaylistUrlProvider(mVdbRequestQueue, mPlaylistEditor.getPlaylistId());

        ClipPlayFragment mClipPlayFragment = ClipPlayFragment.newInstance(mVdtCamera, PLAYLIST_INDEX, vdtUriProvider, ClipPlayFragment.ClipMode.MULTI);

        getFragmentManager().beginTransaction().replace(R.id.player_fragment_content, mClipPlayFragment).commit();
    }
}
