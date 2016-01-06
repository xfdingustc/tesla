package com.transee.vdb;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import com.waylens.hachi.vdb.ClipActionInfo;
import com.waylens.hachi.vdb.GPSRawData;
import com.transee.common.TcpConnection;
import com.waylens.hachi.ui.services.download.RemuxerParams;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipDownloadInfo;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.vdb.RawData;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RemoteClip;
import com.waylens.hachi.vdb.ClipSet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class RemoteVdbClient extends VdbClient {

    static final boolean USE_FFMPEG_IFRAME_DECODER = true;
    static final boolean DEBUG = false;
    static final boolean DEBUG_ANIM = false;
    static final String TAG = "VdbClient";

    protected static final int OS_LINUX = 0;
    protected static final int OS_ANDROID = 1;
    protected static final int OS_WINDOWS = 2;
    protected static final int OS_MAC = 3;

    private static final int VDB_CMD_SIZE = 160;
    private static final int VDB_ACK_SIZE = 160;
    private static final int VDB_CMD_PORT = 8083;

    protected static final int MSG_START = 0x1000;

    protected static final int MSG_VdbReady = MSG_START + 0;
    protected static final int MSG_VdbUnmounted = MSG_START + 1;

    protected static final int MSG_ClipInfo = MSG_START + 2;
    protected static final int MSG_ClipRemoved = MSG_START + 3;

    protected static final int MSG_BufferSpaceLow = MSG_START + 4;
    protected static final int MSG_BufferFull = MSG_START + 5;
    protected static final int MSG_CopyState = MSG_START + 6;
    protected static final int MSG_RawData = MSG_START + 7;
    protected static final int MSG_PlaylistCleared = MSG_START + 8;
    protected static final int VDB_MSG_MarkLiveClipInfo = MSG_START + 32;

    protected static final int MSG_MAGIC = 0xFAFBFCFF;

    // CMD_Null
    protected static final int SUB_CMD_Null = 0;
    protected static final int SUB_CMD_HttpRemuxerDone = 1;

    private final TcpConnection mConnection;

    protected int mVersion = 0x00010000; // 16-16: major-minor
    protected int mOS = -1; // unknow
    protected boolean mHasVdbId = false;

    private byte[] mCmdBuffer = new byte[VDB_CMD_SIZE];
    private int mSendIndex;

    private byte[] mReceiveBuffer = new byte[VDB_ACK_SIZE];
    private byte[] mMsgBuffer;
    protected int mMsgSeqid;
    protected int mUser1;
    protected int mUser2;
    private int mMsgIndex;
    private int mCmdRetCode;
    private int mMsgCode;
    protected int mMsgFlags;
    protected int mCmdTag;

    private final String mTempFileDir;

    // ========================================================
    // CMD_GetVersionInfo
    // ========================================================

    @Override
    protected void cmdGetVersionInfo() throws IOException {
        writeCmdCode(CMD_GetVersionInfo, 0);
        sendCmd();
    }

    private void ackGetVersionInfo() {
        int major = readi16();
        int minor = readi16();
        mVersion = (major << 16) | (minor & 0xFFFF);
        mOS = readi16();
        mHasVdbId = (readi16() & 0x01) != 0;
    }

    // ========================================================
    // CMD_GetClipSetInfo
    // ========================================================

    @Override
    protected void cmdGetClipSetInfo(int type) throws IOException {
        writeCmdCode(CMD_GetClipSetInfo, 0);
        writei32(type);
        sendCmd();
    }

    private final void ackGetClipSetInfo() {
        if (mCmdRetCode != 0) {
            Log.e(TAG, "ackGetClipSetInfo: failed");
            return;
        }

        ClipSet clipSet = new ClipSet(Clip.CAT_REMOTE, readi32());

        int totalClips = readi32();
        readi32(); // TODO - totalLengthMs

        Clip.ID liveClipId = new Clip.ID(Clip.CAT_REMOTE, RemoteClip.TYPE_BUFFERED, readi32(), null);
        clipSet.setLiveClipId(liveClipId);

        for (int i = 0; i < totalClips; i++) {
            int clipId = readi32();
            int clipDate = readi32();
            int duration = readi32();
            RemoteClip clip = new RemoteClip(clipSet.clipType, clipId, null, clipDate, duration); // TODO

            clip.clipStartTime = readi64();
            int num_streams = readi32();
            if (num_streams > 0) {
                readStreamInfo(clip, 0);
                if (num_streams > 1) {
                    readStreamInfo(clip, 1);
                    if (num_streams > 2) {
                        skip(16 * (num_streams - 2));
                    }
                }
            }
            clipSet.addClip(clip);
        }

        mCallback.onClipSetInfoAsync(clipSet);
    }

    // ========================================================
    // CMD_GetIndexPicture
    // ========================================================

    @Override
    protected void cmdGetIndexPicture(GetClipImageRequest param) throws IOException {
        ClipPos clipPos = param.clipPos;
        int cmd = CMD_GetIndexPicture;
        if (mHasVdbId && clipPos.getType() == ClipPos.TYPE_POSTER) {
            cmd |= (1 << 16);
        }
        writeCmdCodeEx(cmd, clipPos.getType(), 0, 0);
        writei32(clipPos.cid.type);
        writei32(clipPos.cid.subType);
        writei32(clipPos.getType() | (clipPos.isLast() ? ClipPos.F_IS_LAST : 0));
        writei64(clipPos.getClipTimeMs());
        writeVdbId(param.clip.getVdbId());
        sendCmd();
    }

    private void ackGetIndexPicture() {

        if (mCmdTag == ClipPos.TYPE_ANIMATION || mCmdTag == ClipPos.TYPE_PREVIEW) {
            mQueue.animationDataReceived();
        }

        if (mCmdRetCode != 0) {
            Log.e(TAG, "ackGetIndexPicture: failed");
            return;
        }

        int clipType = readi32();
        int clipId = readi32();
        int clipDate = readi32();
        int type = readi32();
        boolean bIsLast = (type & ClipPos.F_IS_LAST) != 0;
        type &= ~ClipPos.F_IS_LAST;
        long timeMs = readi64();
        long clipStartTime = readi64();
        int clipDuration = readi32();

        int pictureSize = readi32();
        byte[] data = new byte[pictureSize];
        readByteArray(data, pictureSize);

        String vdbId = null;
        if (mHasVdbId) {
            vdbId = readStringAligned();
        }

        Clip.ID cid = new Clip.ID(Clip.CAT_REMOTE, clipType, clipId, vdbId);
        ClipPos clipPos = new ClipPos(vdbId, cid, clipDate, timeMs, type, bIsLast);
        clipPos.setRealTimeMs(clipStartTime);
        clipPos.setDuration(clipDuration);

        mCallback.onImageDataAsync(clipPos, data);
    }

    // ========================================================
    // CMD_GetPlaybackUrl
    // ========================================================

    @Override
    protected void cmdGetPlaybackUrl(PlaybackUrlRequest param) throws IOException {
        writeCmdCode(CMD_GetPlaybackUrl, 0);
        writeClipId(param.cid);
        writei32(param.mStream);
        writei32(param.mUrlType);
        writei64(param.mClipTimeMs);
        writeVdbId(param.vdbId);
        sendCmd();
    }

    private void ackGetPlaybackUrl() {
        if (mCmdRetCode != 0) {
            Log.e(TAG, "ackGetPlaybackUrl: failed");
            mCallback.onGetPlaybackUrlErrorAsync();
            return;
        }

        int clipType = readi32();
        int clipId = readi32();
        int stream = readi32();
        int urlType = readi32();
        long realTimeMs = readi64();
        int lengthMs = readi32();
        boolean bHasMore = readi32() != 0;
        String url = readString();

        String vdbId = null;
        if (mHasVdbId) {
            vdbId = readStringAligned();
        }

        Clip.ID cid = new Clip.ID(Clip.CAT_REMOTE, clipType, clipId, vdbId);
        PlaybackUrl playbackUrl = new PlaybackUrl(cid);

        playbackUrl.stream = stream;
        playbackUrl.urlType = urlType;
        playbackUrl.realTimeMs = realTimeMs;
        playbackUrl.lengthMs = lengthMs;
        playbackUrl.bHasMore = bHasMore;
        playbackUrl.url = url;
        playbackUrl.offsetMs = 0;

        mCallback.onPlaybackUrlReadyAsync(playbackUrl);
    }

    // ========================================================
    // CMD_GetDownloadUrl - obsolete
    // ========================================================

    // ========================================================
    // CMD_MarkClip
    // ========================================================

    @Override
    protected void cmdMarkClip(MarkClipRequest param) throws IOException {
        writeCmdCode(CMD_MarkClip, 0);
        writeClipId(param.cid);
        writei64(param.mStartTimeMs);
        writei64(param.mEndTimeMs);
        writeVdbId(param.vdbId);
        sendCmd();
    }

    private void ackMarkClip() {
        if (mCmdRetCode != 0) {
            Log.e(TAG, "ackMarkClip: failed");
            mCallback.onMarkClipResultAsync(eMarkClip_Error);
            return;
        }

        int error = readi32();
        mCallback.onMarkClipResultAsync(error);
    }

    // ========================================================
    // CMD_GetCopyState - obsolete
    // ========================================================

    // ========================================================
    // CMD_DeleteClip
    // ========================================================

    @Override
    protected void cmdDeleteClip(DeleteClipRequest param) throws IOException {
        writeCmdCode(CMD_DeleteClip, 0);
        writeClipId(param.cid);
        writeVdbId(param.vdbId);
        sendCmd();
    }

    private void ackDeleteClip() {
        if (mCmdRetCode != 0) {
            Log.e(TAG, "ackDeleteClip: failed");
            mCallback.onDeleteClipResultAsync(mCmdRetCode);
            return;
        }

        int result = readi32();
        mCallback.onDeleteClipResultAsync(result);
    }

    // ========================================================
    // CMD_GetRawData
    // ========================================================

    @Override
    protected void cmdGetRawData(RawDataRequest param) throws IOException {
        writeCmdCode(CMD_GetRawData, 0);
        writeClipId(param.cid);
        writei64(param.mClipTimeMs);
        writei32(param.mTypes);
        writeVdbId(param.vdbId);
        sendCmd();
    }

    private void ackGetRawData() {
        if (mCmdRetCode != 0) {
            Log.e(TAG, "apiGetRawData failed");
            return;
        }

        int clipType = readi32();
        int clipId = readi32();
        Clip.ID cid = new Clip.ID(Clip.CAT_REMOTE, clipType, clipId, null);
        RawData result = new RawData(cid);

        result.clipDate = readi32();
        while (true) {
            int dataType = readi32();
            if (dataType == 0)
                break;

            long clipTimeMs = readi64();
            int size = readi32();

            if (size > 0) {
                RawDataBlock.RawDataItem item = new RawDataBlock.RawDataItem(dataType, clipTimeMs);

                byte[] data = readByteArray(size);
                if (dataType == RawDataBlock.RawDataItem.RAW_DATA_GPS) {
                    item.object = GPSRawData.translate(data);
                }

                if (result.items == null) {
                    result.items = new ArrayList<>();
                }

                result.items.add(item);
            }
        }

        if (mHasVdbId) {
            cid.setExtra(readStringAligned());
        }

        if (result.items != null) {
            mCallback.onRawDataResultAsync(result);
        }
    }

    // ========================================================
    // CMD_SetRawDataOption
    // ========================================================

    @Override
    protected void cmdSetRawDataOption(Integer rawDataTypes) throws IOException {
        writeCmdCode(CMD_SetRawDataOption, 0);
        writei32(rawDataTypes);
        sendCmd();
    }

    private void ackSetRawDataOption() {

    }

    // ========================================================
    // CMD_GetRawDataBlock
    // ========================================================

    @Override
    protected void cmdGetRawDataBlock(RawDataBlockRequest param) throws IOException {
        writeCmdCode(CMD_GetRawDataBlock, param.bForDownload ? 1 : 0);
        writeClipId(param.cid);
        writei64(param.mClipTimeMs);
        writei32(param.mLengthMs);
        writei32(param.mDataType);
        writeVdbId(param.vdbId);
        sendCmd();
    }

    private void ackGetRawDataBlock() {
        if (mCmdRetCode != 0) {
            return;
        }

        int index = mMsgIndex;

        int clipType = readi32();
        int clipId = readi32();
        Clip.ID cid = new Clip.ID(Clip.CAT_REMOTE, clipType, clipId, null);
        RawDataBlock.RawDataBlockHeader header = new RawDataBlock.RawDataBlockHeader(cid);
        header.mClipDate = readi32();
        header.mDataType = readi32();
        header.mRequestedTimeMs = readi64();
        header.mNumItems = readi32();
        header.mDataSize = readi32();

        if (mCmdTag != 0) {

            // downloaded raw data for remuxing into mp4 file

            RawDataBlock.DownloadRawDataBlock block = new RawDataBlock.DownloadRawDataBlock(header);

            int size = mMsgIndex - index;
            mMsgIndex = index;
            block.ack_data = readByteArray(size + header.mNumItems * 8 + header.mDataSize);

            if (mHasVdbId) {
                cid.setExtra(readStringAligned());
            }

            mCallback.onDownloadRawDataBlockAsync(block);

        } else {

            RawDataBlock block = new RawDataBlock(header);

            int numItems = block.header.mNumItems;
            block.timeOffsetMs = new int[numItems];
            block.dataSize = new int[numItems];

            for (int i = 0; i < numItems; i++) {
                block.timeOffsetMs[i] = readi32();
                block.dataSize[i] = readi32();
            }

            block.data = readByteArray(block.header.mDataSize);

            if (mHasVdbId) {
                cid.setExtra(readStringAligned());
            }

            mCallback.onRawDataBlockAsync(block);
        }
    }

    // ========================================================
    // CMD_GetDownloadUrlEx
    // ========================================================

    @Override
    protected void cmdGetDownloadUrlEx(DownloadUrlExRequest param) throws IOException {
        int cmdTag = param.mbForPoster ? DOWNLOAD_FOR_IMAGE : DOWNLOAD_FOR_FILE;
        if (param.mbFirst) {
            cmdTag |= DOWNLOAD_FIRST_LOOP;
        }
        if (param.mbForPoster) {
            if (DEBUG_ANIM) {
                Log.d(TAG, "get download url");
            }
        }
        writeCmdCodeEx(CMD_GetDownloadUrlEx, cmdTag, param.mSessionCounter, 0);
        writeClipId(param.cid);
        writei64(param.mClipTimeMs);
        writei32(param.mClipLengthMs);
        writei32(param.mDownloadOpt);
        writeVdbId(param.vdbId);
        sendCmd();
    }

    private void ackGetDownloadUrlEx() {
        if (mCmdRetCode != 0) {
            Log.e(TAG, "ackGetDownloadUrl: failed");
            if ((mCmdTag & DOWNLOAD_FOR_FILE) != 0) {
                mCallback.onDownloadUrlFailedAsync();
            }
            return;
        }

        int clipType = readi32();
        int clipId = readi32();
        Clip.ID cid = new Clip.ID(Clip.CAT_REMOTE, clipType, clipId, null);
        ClipDownloadInfo clipDownloadInfo = new ClipDownloadInfo(cid);

        int download_opt = readi32();
        clipDownloadInfo.opt = download_opt;

        if ((download_opt & DOWNLOAD_OPT_MAIN_STREAM) != 0) {
            clipDownloadInfo.main.clipDate = readi32();
            clipDownloadInfo.main.clipTimeMs = readi64();
            clipDownloadInfo.main.lengthMs = readi32();
            clipDownloadInfo.main.size = readi64();
            clipDownloadInfo.main.url = readString();
        }

        if ((download_opt & DOWNLOAD_OPT_SUB_STREAM_1) != 0) {
            clipDownloadInfo.sub.clipDate = readi32();
            clipDownloadInfo.sub.clipTimeMs = readi64();
            clipDownloadInfo.sub.lengthMs = readi32();
            clipDownloadInfo.sub.size = readi64();
            clipDownloadInfo.sub.url = readString();
        }

        if ((download_opt & DOWNLOAD_OPT_INDEX_PICT) != 0) {
            int pictureSize = readi32();
            clipDownloadInfo.posterData = new byte[pictureSize];
            readByteArray(clipDownloadInfo.posterData, pictureSize);
        }

        if (mHasVdbId) {
            cid.setExtra(readStringAligned());
        }

        if ((mCmdTag & DOWNLOAD_FOR_IMAGE) != 0) {
            if (DEBUG_ANIM) {
                Log.d(TAG, "download url received");
            }
            remuxFileForImage(clipDownloadInfo, mUser1);
        } else {
            mCallback.onDownloadUrlReadyAsync(clipDownloadInfo, (mCmdTag & DOWNLOAD_FIRST_LOOP) != 0);
        }
    }

    private void remuxFileForImage(ClipDownloadInfo clipDownloadInfo, int sessionCounter) {
        HttpRemuxer remuxer = new HttpRemuxer(sessionCounter);
        remuxer.setEventListener(new HttpRemuxer.RemuxerEventListener() {
            @Override
            public void onEventAsync(HttpRemuxer remuxer, int event, int arg1, int arg2) {
                if (event == HttpRemuxer.EVENT_FINISHED) {
                    endRemux(remuxer);
                }
                if (event == HttpRemuxer.EVENT_ERROR) {
                    remuxer.setError(true);
                    endRemux(remuxer);
                }
            }
        });

        RemuxerParams params = new RemuxerParams();
        params.setInputFile(clipDownloadInfo.main.url + ",0,-1;");
        params.setInputMime("ts");
        params.setOutputFormat("mp4");
        params.setDurationMs(1);

        remuxer.setIframeOnly(true);
        if (!mQueue.startRemux(remuxer.getSessionCounter())) {
            remuxer.release();
        } else {
            if (DEBUG_ANIM) {
                Log.d(TAG, "start remux");
            }
            // TODO - use temporary file
            String outputFile = mTempFileDir + "animation.mp4";
            remuxer.run(params, outputFile);
        }
    }

    private void endRemux(HttpRemuxer remuxer) {
        if (DEBUG_ANIM) {
            Log.d(TAG, "end remux");
        }
        Request request = new Request(CMD_Null, SUB_CMD_HttpRemuxerDone);
        request.param = remuxer;
        mQueue.addRequest(request);
    }

    protected void cmdNull(int sub_cmd, Object param) throws IOException {
        if (sub_cmd == SUB_CMD_HttpRemuxerDone) {
            HttpRemuxer remuxer = (HttpRemuxer) param;
            if (!remuxer.isError()) {
                if (DEBUG_ANIM) {
                    Log.d(TAG, "start decode");
                }
                boolean result = mQueue.startDecode(remuxer.getSessionCounter());
                String outputFile = remuxer.getOutputFile();
                int counter = remuxer.getSessionCounter();
                remuxer.release();
                if (result) {
                    if (DEBUG) {
                        Log.d(TAG, "decode start ");
                    }
                    decodeFileForImage(outputFile, counter);
                }
            }
        }
    }

    private void decodeFileForImage(String filename, int sessionCounter) {
        ClipPos clipPos = mQueue.endDecode(sessionCounter);
        if (clipPos == null) {
            return;
        }

        Bitmap bitmap = null;
        if (USE_FFMPEG_IFRAME_DECODER) {
            IFrameDecoder dec = new IFrameDecoder();
            bitmap = dec.decode(filename, 0);
        } else {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(filename);
            bitmap = mmr.getFrameAtTime(0, MediaMetadataRetriever.OPTION_PREVIOUS_SYNC);
            mmr.release();
        }

        if (DEBUG_ANIM) {
            Log.d(TAG, "decode end");
        }

        if (bitmap != null) { // TODO - CoolPad 5860 cannot decode
            if (!mQueue.endDecode2(sessionCounter)) {
                bitmap.recycle();
            } else {
                // TODO - adjust clipPos.mRealTimeMs
                mCallback.onBitmapDataAsync(clipPos, bitmap);
            }
        }
    }

    @Override
    public void requestClipImage(Clip clip, ClipPos clipPos, int width, int height) {
        super.requestClipImage(clip, clipPos, width, height);
        if (clipPos.getType() == ClipPos.TYPE_ANIMATION) {
            mQueue.initAnimation(clipPos);
        }
    }

    // ========================================================
    // CMD_GetAllPlaylists
    // ========================================================

    @Override
    protected void cmdGetPlaylistSet(GetPlaylistSetInfo request) throws IOException {
        writeCmdCode(CMD_GetAllPlaylists, 0);
        writei32(request.mFlags);
        sendCmd();
    }

    private void ackGetPlaylistSet() {
        if (mCmdRetCode != 0) {
            Log.e(TAG, "ackGetPlaylistSet: failed");
            return;
        }

        PlaylistSet playlistSet = new PlaylistSet();

        playlistSet.mFlags = readi32();
        playlistSet.mNumPlaylists = readi32();

        int numPlaylists = playlistSet.mNumPlaylists;
        for (int i = 0; i < numPlaylists; i++) {
            Playlist playlist = new Playlist();
            playlist.plistId = readi32();
            playlist.mProperties = readi32();
            playlist.numClips = 0;
            skip(4);
            playlist.mTotalLengthMs = readi32();
            playlistSet.mPlaylists.add(playlist);
        }

        mCallback.onPlaylistSetInfoAsync(playlistSet);
    }

    // ========================================================
    // CMD_GetPlaylistIndexPicture
    // ========================================================

    @Override
    protected void cmdGetPlaylistIndexPicture(GetPlaylistImageRequest request) throws IOException {
        writeCmdCode(CMD_GetPlaylistIndexPicture, 0);
        writei32(request.mListType);
        writei32(request.mFlags);
        sendCmd();
    }

    private void ackGetPlaylistIndexPic() {
        if (mCmdRetCode != 0) {
            Log.e(TAG, "ackGetPlaylistIndexPic: failed");
            return;
        }

        int listType = readi32();
        skip(4); // flags - same with request.mFlags
        int clipDate = readi32();
        long timeMs = readi64();

        Clip.ID cid = new Clip.ID(Clip.CAT_REMOTE, listType, 0, null);
        ClipPos posterPoint = new ClipPos(null, cid, clipDate, timeMs, ClipPos.TYPE_POSTER, false); // TODO
        posterPoint.setRealTimeMs(timeMs);
        posterPoint.setDuration(0);

        int pictureSize = readi32();
        byte[] data = new byte[pictureSize];
        readByteArray(data, pictureSize);

        if (mHasVdbId) {
            String vdbId = readStringAligned();
            cid.setExtra(vdbId);
            posterPoint.setVdbId(vdbId);
        }

        mCallback.onPlaylistIndexPicDataAsync(posterPoint, data);
    }

    // ========================================================
    // CMD_ClearPlaylist
    // ========================================================

    @Override
    protected void cmdClearPlaylist(ClearPlaylistRequest request) throws IOException {
        writeCmdCode(CMD_ClearPlaylist, 0);
        writei32(request.mListType);
        sendCmd();
    }

    private void ackClearPlaylist() {
    }

    // ========================================================
    // CMD_InsertClip
    // ========================================================

    @Override
    protected void cmdInsertClip(InsertClipRequest request) throws IOException {
        writeCmdCode(CMD_InsertClip, 0);
        writeClipId(request.cid);
        writei64(request.mStartTimeMs);
        writei64(request.mEndTimeMs);
        writei32(request.mListType);
        writei32(request.mListPos);
        sendCmd();
    }

    private void ackInsertClip() {
        if (mCmdRetCode != 0) {
            Log.e(TAG, "ackInsertClip failed");
            mCallback.onInsertClipResultAsync(eInsertClip_Error);
            return;
        }

        int error = readi32();
        mCallback.onInsertClipResultAsync(error);
    }

    // ========================================================
    // CMD_MoveClip
    // ========================================================

    @Override
    protected void cmdMoveClip(MoveClipRequest request) throws IOException {
        writeCmdCode(CMD_MoveClip, 0);
        writeClipId(request.cid);
        writei32(request.mNewClipPos);
        sendCmd();
    }

    private void ackMoveClip() {
    }

    // ========================================================
    // CMD_GetPlaylistPlaybackUrl
    // ========================================================

    @Override
    protected void cmdGetPlaylistPlaybackUrl(GetPlaylistPlaybackUrlRequest request) throws IOException {
        writeCmdCode(CMD_GetPlaylistPlaybackUrl, 0);
        writei32(request.mListType);
        writei32(request.mPlaylistStartMs);
        writei32(request.mStream);
        writei32(request.mUrlType);
        sendCmd();
    }

    private void ackGetPlaylistPlaybackUrl() {
        if (mCmdRetCode != 0) {
            Log.e(TAG, "ackGetPlaylistPlaybackUrl: failed");
            mCallback.onGetPlaylistPlaybackUrlErrorAsync();
            return;
        }

        PlaylistPlaybackUrl playbackUrl = new PlaylistPlaybackUrl();
        playbackUrl.listType = readi32();
        playbackUrl.playlistStartTimeMs = readi32();
        playbackUrl.stream = readi32();
        playbackUrl.urlType = readi32();
        playbackUrl.lengthMs = readi32();
        playbackUrl.hasMore = readi32() != 0;
        playbackUrl.url = readString();

        mCallback.onPlaylistPlaybackUrlReadyAsync(playbackUrl);
    }

    // ========================================================
    // VDB_CMD_GetAllClipSetInfo
    // ========================================================

    @Override
    protected void cmdGetAllClipSetInfo() throws IOException {
        writeCmdCode(VDB_CMD_GetAllClipSetInfo, 0);
        sendCmd();
    }

    private void ackGetAllClipSetInfo() {
        if (mCmdRetCode != 0) {
            Log.e(TAG, "ackGetAllClipSetInfo failed");
            return;
        }

        ClipSet clipSetBuffered = new ClipSet(Clip.CAT_REMOTE, RemoteClip.TYPE_BUFFERED);
        ClipSet clipSetMarked = new ClipSet(Clip.CAT_REMOTE, RemoteClip.TYPE_MARKED);
        int totalClips = readi32();

        // vdb_clip_info_ex_t
        for (int i = 0; i < totalClips; i++) {
            int clipId = readi32();
            int clipDate = readi32();
            int clipDuration = readi32();
            long clipStartTime = readi64();
            int numStreams = readi16();
            int flags = readi16();
            int clipType = readi32At(16 * numStreams);

            RemoteClip clip = new RemoteClip(clipType, clipId, null, clipDate, clipDuration);
            clip.clipStartTime = clipStartTime;
            if (numStreams > 0) {
                readStreamInfo(clip, 0);
                if (numStreams > 1) {
                    readStreamInfo(clip, 1);
                    if (numStreams > 2) {
                        skip(16 * (numStreams - 2));
                    }
                }
            }

            skip(4); // clipType; already read
            int extra_size = readi32();

            if ((flags & RemoteClip.GET_CLIP_EXTRA) != 0) {
                int sz = RemoteClip.UUID_LEN + 3 * 4;
                skip(sz);
                extra_size -= sz;
            }
            if ((flags & RemoteClip.GET_CLIP_VDB_ID) != 0) {
                int index = mMsgIndex;
                clip.cid.setExtra(readStringAligned());
                extra_size -= mMsgIndex - index;
            }
            skip(extra_size);

            if (clipType == RemoteClip.TYPE_BUFFERED) {
                clipSetBuffered.addClip(clip);
            } else if (clipType == RemoteClip.TYPE_MARKED) {
                clipSetMarked.addClip(clip);
            }
        }

        mCallback.onClipSetInfoAsync(clipSetBuffered);
        mCallback.onClipSetInfoAsync(clipSetMarked);
    }

    // ========================================================
    // MSG_VdbReady
    // ========================================================

    private void msgVdbReady() {
        mCallback.onVdbMounted();
    }

    // ========================================================
    // MSG_VdbUnmounted
    // ========================================================

    private void msgVdbUnmounted() {
        mCallback.onVdbUnmounted();
    }

    // ========================================================
    // MSG_ClipInfo
    // ========================================================

    private void msgClipInfo(boolean bMarkLiveInfo) {
        int action = readi16();
        boolean isLive = (readi16() & CLIP_IS_LIVE) != 0;
        int clipIndex = readi32();
        // /
        int clipType = readi32();
        int clipId = readi32();
        int clipDate = readi32();
        int duration = readi32();
        RemoteClip clip = new RemoteClip(clipType, clipId, null, clipDate, duration);
        clip.index = clipIndex;
        clip.clipStartTime = readi64();
        int num_streams = readi32();
        if (num_streams > 0) {
            readStreamInfo(clip, 0);
        }
        if (num_streams > 1) {
            readStreamInfo(clip, 1);
        }

        if (!bMarkLiveInfo) {
            if (mHasVdbId) {
                clip.cid.setExtra(readStringAligned());
            }
            mCallback.onClipInfoAsync(action, isLive, clip);
            return;
        }

        // mark live clip info
        ClipActionInfo.MarkLiveInfo info = new ClipActionInfo.MarkLiveInfo();
        info.flags = readi32(); // flags, not used
        info.delay_ms = readi32();
        info.before_live_ms = readi32();
        info.after_live_ms = readi32();
        if (mHasVdbId) {
            clip.cid.setExtra(readStringAligned());
        }
        mCallback.onMarkLiveClipInfo(action, clip, info);
    }

    // ========================================================
    // MSG_ClipRemoved
    // ========================================================

    private void msgClipRemoved() {
        int clipType = readi32();
        int clipId = readi32();
        String vdbId = mHasVdbId ? readStringAligned() : null;
        Clip.ID cid = new Clip.ID(Clip.CAT_REMOTE, clipType, clipId, vdbId);
        mCallback.onClipRemovedAsync(cid);
    }

    // ========================================================
    // MSG_BufferSpaceLow
    // ========================================================

    public static class BufferSpaceLowInfo {
        // TODO
    }

    private void msgBufferSpaceLow() {
        BufferSpaceLowInfo info = new BufferSpaceLowInfo();
        mCallback.onBufferSpaceLowAsync(info);
    }

    // ========================================================
    // MSG_BufferFull
    // ========================================================

    private void msgBufferFull() {
        mCallback.onBufferFullAsync();
    }

    // ========================================================
    // MSG_CopyState
    // ========================================================

    // ========================================================
    // MSG_RawData
    // ========================================================

    private void msgRawData() {
        int dataType = readi32();
        byte[] data = readByteArray();
        mCallback.onRawDataAsync(dataType, data);
    }

    // ========================================================
    // MSG_PlaylistCleared
    // ========================================================

    private void msgPlaylistCleared() {
        int playlistId = readi32();
        mCallback.onPlaylistClearedAsync(playlistId);
    }

    // ========================================================
    // VDB_MSG_MarkLiveClipInfo
    // ========================================================

    private void msgMarkLiveClipInfo() {
        msgClipInfo(true);
    }

    // ========================================================
    // end of cmd/msg
    // ========================================================

    // constructor
    public RemoteVdbClient(Callback callback, String tempFileDir) {
        super(callback);
        mTempFileDir = tempFileDir;
        mConnection = new MyTcpConnection("vdb", null);
    }

    @Override
    public void stopImageDecoder() {
        stopDecoder();
    }

    // API
    public void start(String host) {
        mConnection.start(new InetSocketAddress(host, VDB_CMD_PORT));
    }

    // API
    public void stop() {
        mConnection.stop();
        stopDecoder();
    }

    private void stopDecoder() {
        mQueue.endAnimation();
    }

    class MyTcpConnection extends TcpConnection {

        public MyTcpConnection(String name, InetSocketAddress address) {
            super(name, address);
        }

        @Override
        public void onConnectedAsync() {
            // TODO
        }

        @Override
        public void onConnectErrorAsync() {
            RemoteVdbClient.this.mCallback.onConnectionErrorAsync();
        }

        @Override
        public void cmdLoop(Thread thread) throws IOException, InterruptedException {
            RemoteVdbClient.this.cmdLoop(thread);
        }

        @Override
        public void msgLoop(Thread thread) throws IOException, InterruptedException {
            RemoteVdbClient.this.msgLoop(thread);
        }

    }

    private final void msgLoop(Thread thread) throws IOException, InterruptedException {
        thread.setPriority(Thread.MIN_PRIORITY);
        while (!thread.isInterrupted()) {
            readMsg();
            switch (mMsgCode) {
                case CMD_Null:
                    //Logger.t(TAG).d("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFucking null");
                    break;
                case CMD_GetVersionInfo:
                    //Logger.t(TAG).d("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFCMD_GetVersionInfo");
                    ackGetVersionInfo();
                    break;
                case CMD_GetClipSetInfo:
                    //Logger.t(TAG).d("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFCMD_GetVersionInfo");
                    ackGetClipSetInfo();
                    break;
                case CMD_GetIndexPicture:
                    ackGetIndexPicture();
                    break;
                case CMD_GetPlaybackUrl:
                    ackGetPlaybackUrl();
                    break;
                case CMD_GetDownloadUrlEx:
                    ackGetDownloadUrlEx();
                    break;
                case CMD_MarkClip:
                    ackMarkClip();
                    break;
                case CMD_DeleteClip:
                    ackDeleteClip();
                    break;
                case CMD_GetRawData:
                    ackGetRawData();
                    break;
                case CMD_SetRawDataOption:
                    ackSetRawDataOption();
                    break;
                case CMD_GetRawDataBlock:
                    ackGetRawDataBlock();
                    break;

                case CMD_GetAllPlaylists:
                    ackGetPlaylistSet();
                    break;
                case CMD_GetPlaylistIndexPicture:
                    ackGetPlaylistIndexPic();
                    break;
                case CMD_ClearPlaylist:
                    ackClearPlaylist();
                    break;
                case CMD_InsertClip:
                    ackInsertClip();
                    break;
                case CMD_MoveClip:
                    ackMoveClip();
                    break;
                case CMD_GetPlaylistPlaybackUrl:
                    ackGetPlaylistPlaybackUrl();
                    break;
                case VDB_CMD_GetAllClipSetInfo:
                    ackGetAllClipSetInfo();
                    break;

                // msgs
                case MSG_VdbReady:
                    msgVdbReady();
                    break;
                case MSG_VdbUnmounted:
                    msgVdbUnmounted();
                    break;
                case MSG_ClipInfo:
                    msgClipInfo(false);
                    break;
                case MSG_ClipRemoved:
                    msgClipRemoved();
                    break;
                case MSG_BufferSpaceLow:
                    msgBufferSpaceLow();
                    break;
                case MSG_BufferFull:
                    msgBufferFull();
                    break;
                case MSG_RawData:
                    msgRawData();
                    break;
                case MSG_PlaylistCleared:
                    msgPlaylistCleared();
                    break;

                case VDB_MSG_MarkLiveClipInfo:
                    msgMarkLiveClipInfo();
                    break;

                default:
                    Log.e(TAG, "wrong msg code " + mMsgCode);
                    break;
            }
        }
    }

    private final void readMsg() throws IOException {
        mConnection.readFully(mReceiveBuffer, 0, mReceiveBuffer.length);
        mMsgBuffer = mReceiveBuffer;

        mMsgIndex = 0;
        if (readi32() != MSG_MAGIC) {
            //
        }
        mMsgSeqid = readi32(); // ++ each time, set by server
        mUser1 = readi32(); // cmd->user1
        mUser2 = readi32(); // cmd->user2
        mMsgCode = readi16(); // cmd->cmd_code
        mMsgFlags = readi16(); // cmd->cmd_flags
        mCmdTag = readi32(); // cmd->cmd_tag

        mCmdRetCode = readi32();
        int extra_bytes = readi32();
        if (extra_bytes > 0) {
            mMsgBuffer = new byte[VDB_ACK_SIZE + extra_bytes];
            System.arraycopy(mReceiveBuffer, 0, mMsgBuffer, 0, VDB_ACK_SIZE);
            mConnection.readFully(mMsgBuffer, VDB_ACK_SIZE, extra_bytes);
        }

        mMsgIndex = 32;
    }

    private final void writeCmdCode(int code, int tag) {
        mSendIndex = 0;
        writei32(code);
        writei32(tag);
        writei32(0); // user1
        writei32(0); // user2
    }

    private final void writeCmdCodeEx(int code, int tag, int user1, int user2) {
        mSendIndex = 0;
        writei32(code);
        writei32(tag);
        writei32(user1); // user1
        writei32(user2); // user2
    }

    private final void writeClipId(Clip.ID cid) {
        writei32(cid.type);
        writei32(cid.subType);
    }

    private final void writei32(int value) {
        mCmdBuffer[mSendIndex] = (byte) (value);
        mSendIndex++;
        mCmdBuffer[mSendIndex] = (byte) (value >> 8);
        mSendIndex++;
        mCmdBuffer[mSendIndex] = (byte) (value >> 16);
        mSendIndex++;
        mCmdBuffer[mSendIndex] = (byte) (value >> 24);
        mSendIndex++;
    }

    private final void writei64(long value) {
        writei32((int) value);
        writei32((int) (value >> 32));
    }

    private final void writeVdbId(String vdbId) {
        if (vdbId == null)
            return;
        int length = vdbId.length();
        // 4 + length + 0 + aligned_to_4
        int align = 0;
        if ((length + 1) % 4 != 0) {
            align = 4 - (length + 1) % 4;
        }
        // check buffer length
        if (mSendIndex + 4 + length + 1 + align > VDB_CMD_SIZE) {
            Log.w(TAG, "vdb_id is too long: " + length);
            return;
        }
        writei32(length + 1);
        for (int i = 0; i < length; i++) {
            mCmdBuffer[mSendIndex] = (byte) vdbId.charAt(i);
            mSendIndex++;
        }
        for (int i = 0; i <= align; i++) {
            mCmdBuffer[mSendIndex] = 0;
            mSendIndex++;
        }
    }

    private final byte readi8() {
        byte result = mMsgBuffer[mMsgIndex];
        mMsgIndex++;
        return result;
    }

    private final short readi16() {
        int result = (int) mMsgBuffer[mMsgIndex] & 0xFF;
        mMsgIndex++;
        result |= ((int) mMsgBuffer[mMsgIndex] & 0xFF) << 8;
        mMsgIndex++;
        return (short) result;
    }

    private final int readi32() {
        int result = (int) mMsgBuffer[mMsgIndex] & 0xFF;
        mMsgIndex++;
        result |= ((int) mMsgBuffer[mMsgIndex] & 0xFF) << 8;
        mMsgIndex++;
        result |= ((int) mMsgBuffer[mMsgIndex] & 0xFF) << 16;
        mMsgIndex++;
        result |= ((int) mMsgBuffer[mMsgIndex] & 0xFF) << 24;
        mMsgIndex++;
        return result;
    }

    private final int readi32At(int offset) {
        int index = mMsgIndex + offset;
        int result = (int) mMsgBuffer[index] & 0xFF;
        index++;
        result |= ((int) mMsgBuffer[index] & 0xFF) << 8;
        index++;
        result |= ((int) mMsgBuffer[index] & 0xFF) << 16;
        index++;
        result |= ((int) mMsgBuffer[index] & 0xFF) << 24;
        index++;
        return result;
    }

    private final void skip(int n) {
        mMsgIndex += n;
    }

    private final long readi64() {
        int lo = readi32();
        int hi = readi32();
        return ((long) hi << 32) | ((long) lo & 0xFFFFFFFFL);
    }

    private final void readByteArray(byte[] output, int size) {
        System.arraycopy(mMsgBuffer, mMsgIndex, output, 0, size);
        mMsgIndex += size;
    }

    private final String readString() {
        int size = readi32();
        String result;
        try {
            result = new String(mMsgBuffer, mMsgIndex, size - 1, "US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            result = "";
        }
        mMsgIndex += size;
        return result;
    }

    private final String readStringAligned() {
        int size = readi32();
        if (size <= 0)
            return "";
        String result;
        try {
            result = new String(mMsgBuffer, mMsgIndex, size - 1, "US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            result = "";
        }
        mMsgIndex += size;
        if ((size % 4) != 0) {
            mMsgIndex += 4 - (size % 4);
        }
        return result;
    }

    private final byte[] readByteArray(int size) {
        byte[] result = new byte[size];
        System.arraycopy(mMsgBuffer, mMsgIndex, result, 0, size);
        mMsgIndex += size;
        return result;
    }

    private final byte[] readByteArray() {
        int size = readi32();
        return readByteArray(size);
    }

    private final void sendCmd() throws IOException {
        mConnection.sendByteArray(mCmdBuffer);
    }

    private final void readStreamInfo(RemoteClip clip, int index) {
        Clip.StreamInfo info = clip.streams[index];
        info.version = readi32();
        info.video_coding = readi8();
        info.video_framerate = readi8();
        info.video_width = readi16();
        info.video_height = readi16();
        info.audio_coding = readi8();
        info.audio_num_channels = readi8();
        info.audio_sampling_freq = readi32();
    }

}
