package com.waylens.hachi.snipe.toolbox;

import android.util.Log;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;

/**
 * Created by Xiaofei on 2016/3/29.
 */
public class ClipSetRequest extends VdbRequest<ClipSet> {
    private static final String TAG = ClipSetRequest.class.getSimpleName();
    private final int mClipType;

    public ClipSetRequest(int clipType, VdbResponse.Listener<ClipSet> listener, VdbResponse.ErrorListener errorListener) {
        super(0, listener, errorListener);
        this.mClipType = clipType;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        mVdbCommand = VdbCommand.Factory.createCmdGetClipSetInfo(mClipType);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<ClipSet> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            Logger.t(TAG).e("ackGetClipSetInfo: failed");
            return null;
        }

        ClipSet clipSet = new ClipSet(response.readi32());

        int totalClips = response.readi32();
        response.readi32(); // TODO - totalLengthMs

        Clip.ID liveClipId = new Clip.ID(Clip.TYPE_BUFFERED, response.readi32(), null);
        clipSet.setLiveClipId(liveClipId);

        for (int i = 0; i < totalClips; i++) {
            int clipId = response.readi32();
            int clipDate = response.readi32();
            int duration = response.readi32();
            long startTime = response.readi64();
            Clip clip = new Clip(clipSet.getType(), clipId, null, clipDate, startTime, duration);


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

        return VdbResponse.success(clipSet);
    }

    private void readStreamInfo(Clip clip, int index, VdbAcknowledge response) {
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
