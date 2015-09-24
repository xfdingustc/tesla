package com.waylens.hachi.snipe.toolbox;

import android.os.Bundle;

import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.PlaybackUrl;

/**
 * Created by Richard on 9/22/15.
 */
public class ClipPlaybackUrlExRequest extends ClipPlaybackUrlRequest {

    public static final String PARAMETER_CLIP_LENGTH_MS = "clip_length_ms";

    public ClipPlaybackUrlExRequest(Clip clip, Bundle parameters, VdbResponse.Listener<PlaybackUrl> listener, VdbResponse.ErrorListener errorListener) {
        this(0, clip, parameters, listener, errorListener);
    }

    public ClipPlaybackUrlExRequest(int method, Clip clip, Bundle parameters, VdbResponse.Listener<PlaybackUrl> listener, VdbResponse.ErrorListener errorListener) {
        super(method, clip, parameters, listener, errorListener);
    }

    @Override
    protected VdbCommand createVdbCommand() {
        int stream = mParameters.getInt(PARAMETER_STREAM);
        int urlType = mParameters.getInt(PARAMETER_URL_TYPE);
        boolean muteAudio = mParameters.getBoolean(PARAMETER_MUTE_AUDIO);
        long clipTimeMs = mParameters.getLong(PARAMETER_CLIP_TIME_MS);
        int clipLengthMs = mParameters.getInt(PARAMETER_CLIP_LENGTH_MS);
        mVdbCommand = VdbCommand.Factory.createCmdGetClipPlaybackUrl(mClip, stream, urlType,
                muteAudio, clipTimeMs, clipLengthMs);
        return mVdbCommand;
    }
}
