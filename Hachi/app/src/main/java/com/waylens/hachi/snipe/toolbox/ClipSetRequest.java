package com.waylens.hachi.snipe.toolbox;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;

/**
 * Created by Xiaofei on 2015/8/18.
 */
public class ClipSetRequest extends VdbRequest<ClipSet> {
    private final static String TAG = ClipSetRequest.class.getSimpleName();
    private static final int UUID_LENGTH = 36;

    public static final int FLAG_UNKNOWN = -1;
    public static final int FLAG_CLIP_EXTRA = 1;
    public static final int FLAG_CLIP_VDB_ID = 1 << 1;

    public static final int METHOD_GET = 0;
    public static final int METHOD_SET = 1;



    private final int mClipType;
    private final int mFlag;

    public ClipSetRequest(int method, int type, int flag, VdbResponse.Listener<ClipSet> listener,
                          VdbResponse.ErrorListener errorListener) {
        super(method, listener, errorListener);
        this.mClipType = type;
        this.mFlag = flag;
    }

    public ClipSetRequest(int type, int flag, VdbResponse.Listener<ClipSet> listener,
                          VdbResponse.ErrorListener errorListener) {
        this(METHOD_GET, type, flag, listener, errorListener);
    }

    @Override
    protected VdbCommand createVdbCommand() {
        switch (mMethod) {
            case METHOD_GET:
                mVdbCommand = VdbCommand.Factory.createCmdGetClipSetInfo(mClipType, mFlag);
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

    private VdbResponse<ClipSet> parseGetClipSetResponse(VdbAcknowledge response) {
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
            Clip clip = new Clip(clipSet.getType(), clipId, null, clipDate, duration);

            clip.setStartTimeMs(response.readi64());
            int numStreams = response.readi16();
            int flag = response.readi16();
            //Log.e("test", "Flag: " + flag);

            if (numStreams > 0) {
                readStreamInfo(clip, 0, response);
                if (numStreams > 1) {
                    readStreamInfo(clip, 1, response);
                    if (numStreams > 2) {
                        response.skip(16 * (numStreams - 2));
                    }
                }
            }
            response.readi32(); //int clipType
            response.readi32(); //int extraSize
            if (flag == FLAG_CLIP_EXTRA) {
                String guid = new String(response.readByteArray(UUID_LENGTH));
                clip.cid.setExtra(guid);

                response.readi32(); //int ref_clip_date
                clip.gmtOffset = response.readi32();
                int realClipId = response.readi32(); //int real_clip_id
                clip.realCid = new Clip.ID(Clip.TYPE_BUFFERED, realClipId, guid);


            } else if (flag == FLAG_CLIP_VDB_ID) {
                //TODO VDB_ID
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
