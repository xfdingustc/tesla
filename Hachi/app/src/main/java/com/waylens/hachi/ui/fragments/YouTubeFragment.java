package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.util.Log;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.waylens.hachi.app.Constants;

/**
 * Created by Richard on 8/24/15.
 */
public class YouTubeFragment extends YouTubePlayerFragment implements OnInitializedListener {

    private YouTubePlayer player;

    private String videoId;

    public static YouTubeFragment newInstance() {
        return new YouTubeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initialize(Constants.DEVELOPER_KEY, this);
    }

    @Override
    public void onDestroy() {
        if (player != null) {
            player.release();
        }
        super.onDestroy();
    }

    public void setVideoId(String videoId) {
        if (videoId != null && !videoId.equals(this.videoId)) {
            this.videoId = videoId;
            if (player != null) {
                player.cueVideo(videoId);
            }
        }
    }

    public void pause() {
        if (player != null) {
            player.pause();
        }
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean restored) {
        player = youTubePlayer;
        player.setPlayerStyle(YouTubePlayer.PlayerStyle.MINIMAL);
        //player.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_CUSTOM_LAYOUT);
        //player.setOnFullscreenListener((VideoListDemoActivity) getActivity());
        if (!restored && videoId != null) {
            player.cueVideo(videoId);
        }
        Log.e("test", "onInitializationSuccess");
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        this.player = null;
        Log.e("test", "onInitializationFailure");
    }
}
