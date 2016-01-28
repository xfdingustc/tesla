package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.PlaylistPlaybackUrlRequest;
import com.waylens.hachi.ui.entities.story.Story;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.PlaylistPlaybackUrl;

import java.io.IOException;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/1/26.
 */
public class StoryEditActivity extends BaseActivity {
    private static final String TAG = StoryEditActivity.class.getSimpleName();
    private static Story mSharedStory = null;
    private Story mStory;

    private MediaPlayer mPlayer;

    private VdbRequestQueue mRequestQueue;
    private VdbImageLoader mImageLoader;

    public static void launch(Activity startActivity, Story story) {
        Intent intent = new Intent(startActivity, StoryEditActivity.class);
        mSharedStory = story;
        startActivity.startActivity(intent);
    }

    @Bind(R.id.ivClipPreview)
    ImageView mIvClipPreview;

    @Bind(R.id.btnStartPreview)
    ImageButton mBtnStartPreview;

    @Bind(R.id.video_play_view)
    SurfaceView mVideoPlayView;


    @OnClick(R.id.btnStartPreview)
    public void onBtnStartPreviewClicked() {
        PlaylistPlaybackUrlRequest request = new PlaylistPlaybackUrlRequest(mStory.getPlaylist(),
            0, new VdbResponse.Listener<PlaylistPlaybackUrl>() {
            @Override
            public void onResponse(PlaylistPlaybackUrl response) {
                mPlayer = new MediaPlayer();
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mIvClipPreview.setVisibility(View.GONE);
                mBtnStartPreview.setVisibility(View.GONE);
                mPlayer.setDisplay(mVideoPlayView.getHolder());
                mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.start();
                        //mSeekBar.setMediaPlayer(mPlayer);
                        //mVideoTrimmer.setMediaPlayer(mPlayer);
                    }
                });
                try {
                    mPlayer.setDataSource(response.url);
                    mPlayer.prepareAsync();
                } catch (IOException e) {
                    Logger.e("test", "", e);
                }
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mRequestQueue.add(request);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        mStory = mSharedStory;
        mRequestQueue = Snipe.newRequestQueue();
        mImageLoader = VdbImageLoader.getImageLoader(mRequestQueue);

        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_story_edit);

        ClipSet clipSet = mStory.getClipSet();
        Clip clip = clipSet.getClip(0);

        ClipPos clipPos = new ClipPos(clip, clip.getStartTimeMs(), ClipPos.TYPE_POSTER, false);

        mImageLoader.displayVdbImage(clipPos, mIvClipPreview);
    }
}
