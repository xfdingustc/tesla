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
public class YouTubeFragment extends YouTubePlayerFragment implements
        OnInitializedListener, YouTubePlayer.OnFullscreenListener {

    private YouTubePlayer player;

    private String videoId;

    private boolean mIsFullScreen;

    public static YouTubeFragment fullScreenFragment;

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
        if (!restored && videoId != null) {
            player.cueVideo(videoId);
            Log.e("test", "onInitializationSuccess");
        }
        player.setOnFullscreenListener(this);
        player.setPlayerStateChangeListener(new PlayerStateListener());

    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        this.player = null;
        Log.e("test", "onInitializationFailure");
    }

    @Override
    public void onFullscreen(boolean isFullscreen) {
        mIsFullScreen = isFullscreen;
        if (isFullscreen) {
            fullScreenFragment = this;
        } else {
            fullScreenFragment = null;
        }
    }

    public boolean isFullScreen() {
        return mIsFullScreen;
    }

    public void setFullScreen(boolean fullscreen) {
        if (player != null) {
            player.setFullscreen(fullscreen);
        }
    }

    class PlayerStateListener implements YouTubePlayer.PlayerStateChangeListener {

        @Override
        public void onLoading() {
            //
        }

        @Override
        public void onLoaded(String s) {
            player.play();
        }

        @Override
        public void onAdStarted() {

        }

        @Override
        public void onVideoStarted() {

        }

        @Override
        public void onVideoEnded() {

        }

        @Override
        public void onError(YouTubePlayer.ErrorReason errorReason) {

        }
    }
}
