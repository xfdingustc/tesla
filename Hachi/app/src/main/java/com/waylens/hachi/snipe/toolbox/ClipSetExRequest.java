package com.waylens.hachi.snipe.toolbox;

import android.os.Environment;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipSet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;


/**
 * Created by Xiaofei on 2015/8/18.
 */
public class ClipSetExRequest extends VdbRequest<ClipSet> {
    public static final int FLAG_UNKNOWN = -1;
    public static final int FLAG_CLIP_EXTRA = 1;
    public static final int FLAG_CLIP_VDB_ID = 1 << 1;
    public static final int FLAG_CLIP_DESC = 1 << 2;
    public static final int FLAG_CLIP_ATTR = 1 << 3;
    public static final int FLAG_CLIP_SIZE = 1 << 4;
    public static final int FLAG_CLIP_SCENE_DATA = 1 << 5;
    public static final int METHOD_GET = 0;
    public static final int METHOD_SET = 1;
    private final static String TAG = ClipSetExRequest.class.getSimpleName();
    private static final int UUID_LENGTH = 36;
    private final int mClipType;
    private final int mFlag;
    private final int mAttr;

    public ClipSetExRequest(int type, int flag, VdbResponse.Listener<ClipSet> listener,
                            VdbResponse.ErrorListener errorListener) {
        this(METHOD_GET, type, flag, 0, listener, errorListener);
    }

    public ClipSetExRequest(int type, int flag, int attr, VdbResponse.Listener<ClipSet> listener,
                            VdbResponse.ErrorListener errorListener) {
        this(METHOD_GET, type, flag, attr, listener, errorListener);
    }

    public ClipSetExRequest(int method, int type, int flag, int attr, VdbResponse.Listener<ClipSet> listener,
                            VdbResponse.ErrorListener errorListener) {
        super(method, listener, errorListener);
        this.mClipType = type;
        this.mAttr = attr;
        this.mFlag = flag;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        switch (mMethod) {
            case METHOD_GET:
                mVdbCommand = VdbCommand.Factory.createCmdGetClipSetInfoEx(mClipType, mFlag);
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
        //createFileWithByte(response.mReceiveBuffer);
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
            long startTimeMs = response.readi64();
            Clip clip = new Clip(clipSet.getType(), clipId, null, clipDate, startTimeMs, duration);

            int numStreams = response.readi16();
            int flag = response.readi16();
//            Logger.t(TAG).d("Flag: " + flag);

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
            int extraSize1 = response.readi32(); //int extraSize

            int offsetSize = 0;

            if ((flag & FLAG_CLIP_EXTRA) > 0) {
                String guid = new String(response.readByteArray(UUID_LENGTH));
                clip.cid.setExtra(guid);

                response.readi32(); //int ref_clip_date
                clip.gmtOffset = response.readi32();
                int realClipId = response.readi32(); //int real_clip_id
                clip.realCid = new Clip.ID(Clip.TYPE_BUFFERED, realClipId, guid);

                offsetSize += UUID_LENGTH + 3 * 4;
//              response.skip(extraSize - offsetSize);


            }

            if ((flag & FLAG_CLIP_VDB_ID) > 0) {
                String extraString = new String();
                offsetSize += response.readStringAlignedReturnSize(extraString);

                clip.cid.setExtra(extraString);
                response.skip(offsetSize);
            }

            if ((flag & FLAG_CLIP_DESC) > 0) {
                do {
                    int fcc = response.readi32();
/*                  Logger.t(TAG).d((fcc >> 24) & 0xff);
                    Logger.t(TAG).d((fcc >> 16) & 0xff);
                    Logger.t(TAG).d((fcc >> 8) & 0xff);
                    Logger.t(TAG).d(fcc & 0xff);*/
                    offsetSize += 4;
                    if (fcc == 0)
                        break;
                    int dataSize = response.readi32();
                    int alignSize = 0;
                    offsetSize += 4;
                    //Logger.t(TAG).d(fcc + " + " + (('0' << 24) + ('N' << 16) + ('I' << 8) + 'V'));
                    if (fcc == (('0' << 24) + ('N' << 16) + ('I' << 8) + 'V')) {
                        //Logger.t(TAG).d("dataSize:" + dataSize);
                        String vin = null;
                        try {
                            vin = new String(response.readByteArray(dataSize), "US-ASCII");
                        } catch (UnsupportedEncodingException e) {
                            Logger.t(TAG).d(e.getMessage());
                        }
                        clip.setVin(vin);
                        Logger.t(TAG).d(vin);
                        offsetSize += ((dataSize + 3) / 4) * 4;
                        alignSize = ((dataSize + 3) /4 ) * 4;
                        //Logger.t(TAG).d("offset size:" + offsetSize);
                        response.skip(alignSize - dataSize);
                    } else {
                        response.skip(dataSize);
                        alignSize = ((dataSize + 3) / 4) * 4;
                        offsetSize += ((dataSize + 3) / 4) * 4;
                        //Logger.t(TAG).d("offset size:" + offsetSize);
                        response.skip(alignSize - dataSize);
                    }
                } while (true);
            }
            boolean attrMatch = true;
            if ((flag & FLAG_CLIP_ATTR) > 0) {
                //Logger.t(TAG).d("flag : " + flag );
                int attr = response.readi32();
                offsetSize += 4;
                if ((attr & mAttr) <= 0) {
                    attrMatch = false;
                }
            }

            if ((flag & FLAG_CLIP_SCENE_DATA) > 0) {
                int dataSize = response.readi32();
                offsetSize += dataSize + 4;
                int fcc = response.readi32();
                if (fcc == (('C' << 24) + ('D' << 16) + ('6' << 8) + 'T')) {
                    int datasize = response.readi32();
                    clip.typeRace |= Clip.TYPE_RACE;
                    clip.typeRace |= Clip.TYPE_RACE_CD6T;
                    clip.raceTimingPoints = new ArrayList<>(6);
                    long utc_sec_start = response.readui32();
                    long utc_usec_start = response.readui32();

                    long utc_msec_start = utc_usec_start / 1000 + utc_sec_start * 1000;
                    Logger.t(TAG).d("utc_sec_start" + utc_sec_start + "utc_usec_start" + utc_usec_start);
                    long usec_offset_move_begin = response.readui32();
                    long usec_offset_30mph = response.readui32();
                    long usec_offset_50kmh = response.readui32();
                    long usec_offset_60mph = response.readui32();
                    long usec_offset_100kmh = response.readui32();

                    clip.raceTimingPoints.add(0, utc_usec_start / 1000 + utc_sec_start * 1000);
                    clip.raceTimingPoints.add(1, usec_offset_move_begin / 1000 + utc_msec_start);
                    clip.raceTimingPoints.add(2, usec_offset_30mph / 1000 + utc_msec_start);
                    clip.raceTimingPoints.add(3, usec_offset_50kmh / 1000 + utc_msec_start);
                    clip.raceTimingPoints.add(4, usec_offset_60mph / 1000 + utc_msec_start);
                    clip.raceTimingPoints.add(5, usec_offset_100kmh / 1000 + utc_msec_start);
                } else if (fcc == (('C' << 24) + ('D' << 16) + ('3' << 8) + 'T')){
                    int datasize = response.readi32();
                    clip.typeRace |= Clip.TYPE_RACE;
                    clip.typeRace |= Clip.TYPE_RACE_CD3T;
                    clip.raceTimingPoints = new ArrayList<>(6);
                    long utc_sec_start = response.readui32();
                    long utc_usec_start = response.readui32();
                    long utc_msec_start = utc_usec_start / 1000 + utc_sec_start * 1000;
                    Logger.t(TAG).d("utc_sec_start" + utc_sec_start + "utc_usec_start" + utc_usec_start);
                    long usec_offset_move_begin = response.readui32();
                    long usec_offset_30mph = response.readui32();
                    long usec_offset_50kmh = response.readui32();
                    long usec_offset_60mph = -1;
                    long usec_offset_100kmh = -1;

                    clip.raceTimingPoints.add(0, utc_usec_start / 1000 + utc_sec_start * 1000);
                    clip.raceTimingPoints.add(1, usec_offset_move_begin / 1000 + utc_msec_start);
                    clip.raceTimingPoints.add(2, usec_offset_30mph / 1000 + utc_msec_start);
                    clip.raceTimingPoints.add(3, usec_offset_50kmh / 1000 + utc_msec_start);
                    clip.raceTimingPoints.add(4, (long)-1);
                    clip.raceTimingPoints.add(5, (long)-1);
                } else if (fcc == (('A' << 24) + ('U' << 16) + ('6' << 8) + 'T')) {
                    int datasize = response.readi32();
                    clip.typeRace |= Clip.TYPE_RACE;
                    clip.typeRace |= Clip.TYPE_RACE_AU6T;
                    clip.raceTimingPoints = new ArrayList<>(6);
                    long utc_sec_start = -1;
                    long utc_usec_start = -1;
//                    Logger.t(TAG).d("utc_sec_start" + utc_sec_start + "utc_usec_start" + utc_usec_start);
                    long utc_sec_move_begin = response.readui32();
                    long utc_usec_move_begin = response.readui32();
                    long utc_msec_move_begin = utc_usec_move_begin / 1000 + utc_sec_move_begin * 1000;
                    long usec_offset_30mph = response.readui32();
                    long usec_offset_50kmh = response.readui32();
                    long usec_offset_60mph = response.readui32();
                    long usec_offset_100kmh = response.readui32();

                    clip.raceTimingPoints.add(0, (long)-1);
                    clip.raceTimingPoints.add(1, utc_usec_move_begin / 1000 + utc_sec_move_begin * 1000);
                    clip.raceTimingPoints.add(2, usec_offset_30mph / 1000 + utc_msec_move_begin);
                    clip.raceTimingPoints.add(3, usec_offset_50kmh / 1000 + utc_msec_move_begin);
                    clip.raceTimingPoints.add(4, usec_offset_60mph / 1000 + utc_msec_move_begin);
                    clip.raceTimingPoints.add(5, usec_offset_100kmh / 1000 + utc_msec_move_begin);

                } else if (fcc == (('A' << 24) + ('U' << 16) + ('3' << 8) + 'T')) {
                    int datasize = response.readi32();
                    clip.typeRace |= Clip.TYPE_RACE;
                    clip.typeRace |= Clip.TYPE_RACE_AU3T;
                    clip.raceTimingPoints = new ArrayList<>(6);
                    long utc_sec_start = -1;
                    long utc_usec_start = -1;
                    Logger.t(TAG).d("utc_sec_start" + utc_sec_start + "utc_usec_start" + utc_usec_start);
                    long utc_sec_move_begin = response.readui32();
                    long utc_usec_move_begin = response.readui32();
                    long utc_msec_move_begin = utc_usec_move_begin / 1000 + utc_sec_move_begin * 1000;
                    long usec_offset_30mph = response.readui32();
                    long usec_offset_50kmh = response.readui32();
                    long usec_offset_60mph = -1;
                    long usec_offset_100kmh = -1;

                    clip.raceTimingPoints.add(0, (long)-1);
                    clip.raceTimingPoints.add(1, utc_usec_move_begin / 1000 + utc_sec_move_begin * 1000);
                    clip.raceTimingPoints.add(2, usec_offset_30mph / 1000 + utc_msec_move_begin);
                    clip.raceTimingPoints.add(3, usec_offset_50kmh / 1000 + utc_msec_move_begin);
                    clip.raceTimingPoints.add(4, (long)-1);
                    clip.raceTimingPoints.add(5, (long)-1);
                } else if (fcc == (('L' << 24) + ('A' << 16) + ('P' << 8) + 'T')) {
                    int datasize = response.readi32();
                    double latitude = response.readLEDouble();
                    double longtitude = response.readLEDouble();
                    long utc_time = response.readui32();
                    long utc_time_usec = response.readui32();
                    clip.lapTimerData = new Clip.LapTimerData(latitude, longtitude, utc_time, utc_time_usec);
                    Logger.t(TAG).d("lapTimer" + clip.lapTimerData.toString());
                }
            }

            if (attrMatch) {
                clipSet.addClip(clip);
            }

            //response.skip(extraSize - offsetSize);
        }
        return VdbResponse.success(clipSet);
    }

    private String fileName = "response_byte.txt";

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

    private void createFileWithByte(byte[] bytes) {
        File file = new File(Environment.getExternalStorageDirectory(),
                fileName);
        FileOutputStream outputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        try {

            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            outputStream = new FileOutputStream(file);
            bufferedOutputStream = new BufferedOutputStream(outputStream);
            bufferedOutputStream.write(bytes);
            bufferedOutputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }


}
