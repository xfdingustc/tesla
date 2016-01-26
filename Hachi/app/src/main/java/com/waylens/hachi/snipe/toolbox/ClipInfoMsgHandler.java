package com.waylens.hachi.snipe.toolbox;

import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbMessageHandler;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipActionInfo;

/**
 * Created by Richard on 12/28/15.
 */
public class ClipInfoMsgHandler extends VdbMessageHandler<ClipActionInfo> {

    public static final int CLIP_IS_LIVE = 1;

    public ClipInfoMsgHandler(VdbResponse.Listener<ClipActionInfo> listener, VdbResponse.ErrorListener errorListener) {
        super(VdbCommand.Factory.MSG_ClipInfo, listener, errorListener);
    }

    public ClipInfoMsgHandler(int msgCode, VdbResponse.Listener<ClipActionInfo> listener, VdbResponse.ErrorListener errorListener) {
        super(msgCode, listener, errorListener);
    }

    @Override
    protected VdbResponse<ClipActionInfo> parseVdbResponse(VdbAcknowledge response) {
        int action = response.readi16();
        boolean isLive = (response.readi16() & CLIP_IS_LIVE) != 0;
        int clipIndex = response.readi32();
        // /
        int clipType = response.readi32();
        int clipId = response.readi32();
        int clipDate = response.readi32();
        int duration = response.readi32();
        Clip clip = new Clip(clipType, clipId, null, clipDate, duration);
        clip.index = clipIndex;
        clip.setStartTimeMs(response.readi64());
        int num_streams = response.readi32();
        for (int i = 0; i < num_streams; i++) {
            readStreamInfo(response, clip, i);
        }

        // TODO READ VdbID
        // if (mHasVdbId) {
        //    clip.cid.setExtra(readStringAligned());
        //}

        return VdbResponse.success(new ClipActionInfo(action, isLive, clip));
    }

    void readStreamInfo(VdbAcknowledge response, Clip clip, int index) {
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
