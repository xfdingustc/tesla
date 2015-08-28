package com.waylens.hachi.snipe.toolbox;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.DownloadInfoEx;

/**
 * Created by Xiaofei on 2015/8/27.
 */
public class DownloadUrlRequest extends VdbRequest<DownloadInfoEx> {
    private static final String TAG = DownloadUrlRequest.class.getSimpleName();

    private final VdbResponse.Listener<DownloadInfoEx> mListener;
    private final Clip mClip;

    public static final int DOWNLOAD_OPT_MAIN_STREAM = (1 << 0);
    public static final int DOWNLOAD_OPT_SUB_STREAM_1 = (1 << 1);
    public static final int DOWNLOAD_OPT_INDEX_PICT = (1 << 2);
    public static final int DOWNLOAD_OPT_PLAYLIST = (1 << 3);
    public static final int DOWNLOAD_OPT_MUTE_AUDIO = (1 << 4);

    public DownloadUrlRequest(Clip clip, VdbResponse.Listener<DownloadInfoEx> listener, VdbResponse.ErrorListener errorListener) {
        this(0, clip, listener, errorListener);
    }

    public DownloadUrlRequest(int method, Clip clip, VdbResponse.Listener<DownloadInfoEx> listener, VdbResponse.ErrorListener errorListener) {
        super(method, errorListener);
        this.mClip = clip;
        this.mListener = listener;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        int downloadOption = DOWNLOAD_OPT_MAIN_STREAM;
        mVdbCommand = VdbCommand.Factory.createCmdGetClipDownloadUrl(mClip, mClip.getStartTime(),
            mClip.getStartTime() + mClip.clipLengthMs, downloadOption, true);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<DownloadInfoEx> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            Logger.t(TAG).e("ackGetDownloadUrl: failed: " + response.getRetCode());

            //return null;
        }

        int clipType = response.readi32();
        int clipId = response.readi32();
        Clip.ID cid = new Clip.ID(Clip.CAT_REMOTE, clipType, clipId, null);
        DownloadInfoEx downloadInfo = new DownloadInfoEx(cid);

        int download_opt = response.readi32();
        downloadInfo.opt = download_opt;

        if ((download_opt & DOWNLOAD_OPT_MAIN_STREAM) != 0) {
            downloadInfo.main.clipDate = response.readi32();
            downloadInfo.main.clipTimeMs = response.readi64();
            downloadInfo.main.lengthMs = response.readi32();
            downloadInfo.main.size = response.readi64();
            downloadInfo.main.url = response.readString();
        }

        if ((download_opt & DOWNLOAD_OPT_SUB_STREAM_1) != 0) {
            downloadInfo.sub.clipDate = response.readi32();
            downloadInfo.sub.clipTimeMs = response.readi64();
            downloadInfo.sub.lengthMs = response.readi32();
            downloadInfo.sub.size = response.readi64();
            downloadInfo.sub.url = response.readString();
        }

        if ((download_opt & DOWNLOAD_OPT_INDEX_PICT) != 0) {
            int pictureSize = response.readi32();
            downloadInfo.posterData = new byte[pictureSize];
            response.readByteArray(downloadInfo.posterData, pictureSize);
        }


        return VdbResponse.success(downloadInfo);
    }

    @Override
    protected void deliverResponse(DownloadInfoEx response) {
        mListener.onResponse(response);
    }
}
