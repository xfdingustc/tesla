package com.waylens.hachi.snipe.toolbox;

import android.os.Bundle;

import com.orhanobut.logger.Logger;
import com.transee.vdb.VdbClient;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.PlaybackUrl;

/**
 * Created by Xiaofei on 2015/8/27.
 */
public class ClipPlaybackUrlRequest extends VdbRequest<PlaybackUrl> {
    private static final String TAG = ClipPlaybackUrlRequest.class.getSimpleName();
    private final VdbResponse.Listener<PlaybackUrl> mListener;
    protected final Bundle mParameters;
    protected final Clip mClip;

    public static final String PARAMETER_STREAM = "stream";
    public static final String PARAMETER_URL_TYPE = "url_type";
    public static final String PARAMETER_MUTE_AUDIO = "mute_audio";
    public static final String PARAMETER_CLIP_TIME_MS = "clip_time_ms";

    public ClipPlaybackUrlRequest(Clip clip, Bundle parameters, VdbResponse.Listener<PlaybackUrl>
        listener, VdbResponse.ErrorListener errorListener) {
        this(0, clip, parameters, listener, errorListener);
    }

    public ClipPlaybackUrlRequest(int method, Clip clip, Bundle parameters, VdbResponse.Listener<PlaybackUrl>
        listener, VdbResponse.ErrorListener errorListener) {
        super(method, errorListener);
        this.mClip = clip;
        this.mParameters = parameters;
        this.mListener = listener;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        int stream = mParameters.getInt(PARAMETER_STREAM);
        int urlType = mParameters.getInt(PARAMETER_URL_TYPE);
        boolean muteAudio = mParameters.getBoolean(PARAMETER_MUTE_AUDIO);
        long clipTimeMs = mParameters.getLong(PARAMETER_CLIP_TIME_MS);
        mVdbCommand = VdbCommand.Factory.createCmdGetClipPlaybackUrl(mClip, stream, urlType,
            muteAudio, clipTimeMs, 0);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<PlaybackUrl> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            Logger.t(TAG).e("ackGetPlaybackUrl: failed");
            return null;
        }

        int clipType = response.readi32();
        int clipId = response.readi32();
        int stream = response.readi32();
        int urlType = response.readi32();
        long realTimeMs = response.readi64();
        int lengthMs = response.readi32();
        boolean bHasMore = response.readi32() != 0;
        String url = response.readString();

        String vdbId = null;

        Clip.ID cid = new Clip.ID(Clip.CAT_REMOTE, clipType, clipId, vdbId);
        PlaybackUrl playbackUrl = new PlaybackUrl(cid);

        playbackUrl.stream = stream;
        playbackUrl.urlType = urlType;
        playbackUrl.realTimeMs = realTimeMs;
        playbackUrl.lengthMs = lengthMs;
        playbackUrl.bHasMore = bHasMore;
        playbackUrl.url = url;
        playbackUrl.offsetMs = 0;

        return VdbResponse.success(playbackUrl);
    }

    @Override
    protected void deliverResponse(PlaybackUrl response) {
        mListener.onResponse(response);
    }
}
