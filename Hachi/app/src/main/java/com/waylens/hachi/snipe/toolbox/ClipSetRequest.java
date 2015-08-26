package com.waylens.hachi.snipe.toolbox;

import android.os.Bundle;


import com.orhanobut.logger.Logger;
import com.transee.vdb.Clip;
import com.transee.vdb.ClipSet;
import com.transee.vdb.RemoteClip;
import com.transee.vdb.SimpleClipSet;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;

/**
 * Created by Xiaofei on 2015/8/18.
 */
public class ClipSetRequest extends VdbRequest<ClipSet> {
    private final static String TAG = ClipSetRequest.class.getSimpleName();
    private final VdbResponse.Listener<ClipSet> mListener;

    public static final int METHOD_GET = 0;
    public static final int METHOD_SET = 1;
    public static final String PARAMETER_TYPE = "type";

    private final Bundle mParameters;

    public ClipSetRequest(int method, Bundle parameters, VdbResponse.Listener<ClipSet> listener,
                          VdbResponse
                              .ErrorListener errorListener) {
        super(method, errorListener);
        this.mListener = listener;
        this.mParameters = parameters;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        switch (mMethod) {
            case METHOD_GET:
                int type = mParameters.getInt(PARAMETER_TYPE);
                mVdbCommand = VdbCommand.Factory.createCmdGetClipSetInfo(type);
                break;
            case METHOD_SET:
                break;
            default:
                break;
        }

        return mVdbCommand;
    }

    @Override
    protected VdbResponse<ClipSet> parseVdbResponse(VdbAcknowledge response) {
        switch (mMethod) {
            case METHOD_GET:
                return parseGetClipSetResponse(response);
            case METHOD_SET:
                break;
        }
        return null;
    }

    @Override
    protected void deliverResponse(ClipSet response) {
        mListener.onResponse(response);
    }

    private VdbResponse<ClipSet> parseGetClipSetResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            Logger.t(TAG).e("ackGetClipSetInfo: failed");
            return null;
        }

        SimpleClipSet clipSet = new SimpleClipSet(Clip.CAT_REMOTE, response.readi32());

        int totalClips = response.readi32();

        response.readi32(); // TODO - totalLengthMs

        Clip.ID liveClipId = new Clip.ID(Clip.CAT_REMOTE, RemoteClip.TYPE_BUFFERED, response.readi32(), null);
        clipSet.setLiveClipId(liveClipId);

        for (int i = 0; i < totalClips; i++) {
            int clipId = response.readi32();
            int clipDate = response.readi32();
            RemoteClip clip = new RemoteClip(clipSet.clipType, clipId, null, clipDate); // TODO
            clip.clipLengthMs = response.readi32();
            clip.clipStartTime = response.readi64();
            int num_streams = response.readi32();
            if (num_streams > 0) {
                readStreamInfo(clip, 0, response);
                if (num_streams > 1) {
                    readStreamInfo(clip, 1, response);
                    if (num_streams > 2) {
                        response.skip(16 * (num_streams - 2));
                    }
                }
            }
            clipSet.addClip(clip);
        }
        return VdbResponse.success((ClipSet) clipSet);
    }

    private final void readStreamInfo(RemoteClip clip, int index, VdbAcknowledge response) {
        Clip.StreamInfo info = clip.streams[index];
        info.version = response.readi32();
        info.video_coding = response.readi8();
        info.video_framerate = response.readi8();
        info.video_width = response.readi16();
        info.video_height = response.readi16();
        info.audio_coding = response.readi8();
        info.audio_num_channels = response.readi8();
        info.audio_sampling_freq = response.readi32();
    }


}
