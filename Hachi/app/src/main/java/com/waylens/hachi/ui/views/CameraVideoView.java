package com.waylens.hachi.ui.views;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.transee.vdb.VdbClient;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlRequest;
import com.waylens.hachi.ui.entities.SharableClip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.PlaybackUrl;

/**
 * Created by Richard on 1/18/16.
 */
public class CameraVideoView extends VideoPlayView {

    SharableClip mSharableClip;

    VdbRequestQueue mVdbRequestQueue;

    VdbImageLoader mVdbImageLoader;

    PlaybackUrl mPlaybackUrl;

    public CameraVideoView(Context context) {
        this(context, null, 0);
    }

    public CameraVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void initVideoPlay(VdbRequestQueue vdbRequestQueue, SharableClip sharableClip) {
        mVdbRequestQueue = vdbRequestQueue;
        mSharableClip = sharableClip;
        mVdbImageLoader = VdbImageLoader.getImageLoader(mVdbRequestQueue);
        ClipPos clipPos = new ClipPos(mSharableClip.clip.getVdbId(),
                mSharableClip.realCid,
                mSharableClip.clip.clipDate,
                mSharableClip.clip.getStartTimeMs(),
                ClipPos.TYPE_POSTER, false);
        mVdbImageLoader.displayVdbImage(clipPos, videoCover);
        mBtnPlay.setVisibility(VISIBLE);
    }

    public void updateThumbnail(ClipPos clipPos) {
        if (videoCover == null) {
            return;
        }
        if (videoCover.getVisibility() != VISIBLE) {
            videoCover.setVisibility(VISIBLE);
        }
        mVdbImageLoader.displayVdbImage(clipPos, videoCover, true, false);
    }

    @Override
    protected void onClickPlayButton() {
        if (mPlaybackUrl == null) {
            loadPlayURL();
        } else {
            playVideo();
        }
    }

    void loadPlayURL() {
        mBtnPlay.setVisibility(INVISIBLE);
        if (mLoadingIcon.getVisibility() != View.VISIBLE) {
            mLoadingIcon.setVisibility(View.VISIBLE);
        }
        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_URL_TYPE, VdbClient.URL_TYPE_HLS);
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_STREAM, VdbClient.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, mSharableClip.clip.getStartTimeMs());

        ClipPlaybackUrlRequest request = new ClipPlaybackUrlRequest(mSharableClip.bufferedCid, parameters, new VdbResponse.Listener<PlaybackUrl>() {
            @Override
            public void onResponse(PlaybackUrl playbackUrl) {
                mPlaybackUrl = playbackUrl;
                setSource(playbackUrl.url, VideoPlayView.STATE_PLAYING);
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Log.e("test", "", error);
            }
        });

        mVdbRequestQueue.add(request);
    }
}
