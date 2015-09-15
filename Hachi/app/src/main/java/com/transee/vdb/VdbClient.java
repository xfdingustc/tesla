package com.transee.vdb;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import com.transee.vdb.RemoteVdbClient.BufferSpaceLowInfo;
import com.transee.vdb.Vdb.MarkLiveInfo;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.DownloadInfoEx;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.vdb.RawData;
import com.waylens.hachi.vdb.RawDataBlock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

abstract public class VdbClient {

    static final boolean DEBUG = false;
    static final String TAG = "VdbClient";

    public static final int DECODE_IFRAME_DELAY = 200;

    public static final int CLIP_ACTION_CREATED = 1;
    public static final int CLIP_ACTION_CHANGED = 2;
    public static final int CLIP_ACTION_FINISHED = 3;
    public static final int CLIP_ACTION_INSERTED = 4;
    public static final int CLIP_ACTION_MOVED = 5;

    public static final int CLIP_IS_LIVE = 1;

    public static final int URL_TYPE_TS = 0;
    public static final int URL_TYPE_HLS = 1;
    public static final int URL_MUTE_AUDIO = (1 << 31);

    public static final int STREAM_MAIN = 0;
    public static final int STREAM_SUB_1 = 1;




    public static final int DOWNLOAD_OPT_MAIN_STREAM = (1 << 0);
    public static final int DOWNLOAD_OPT_SUB_STREAM_1 = (1 << 1);
    public static final int DOWNLOAD_OPT_INDEX_PICT = (1 << 2);
    public static final int DOWNLOAD_OPT_PLAYLIST = (1 << 3);
    public static final int DOWNLOAD_OPT_MUTE_AUDIO = (1 << 4);

    // cmd tag for download
    public static final int DOWNLOAD_FOR_FILE = 1 << 0;
    public static final int DOWNLOAD_FOR_IMAGE = 1 << 1;
    public static final int DOWNLOAD_FIRST_LOOP = 1 << 2;

    public static final int eInsertClip_Error = -1;
    public static final int eInsertClip_OK = 0;
    public static final int eInsertClip_UnknownStream = 4;
    public static final int eInsertClip_StreamNotMatch = 5;

    public static final int eMarkClip_Error = -1;
    public static final int eMarkClip_OK = 0;

    // public static final int URL_TYPE = URL_TYPE_TS;


    public class PlaylistPlaybackUrl {
        public int listType;
        public int playlistStartTimeMs;
        public int stream;
        public int urlType;
        public int lengthMs;
        public boolean hasMore;
        public String url;
    }

    // implemented by Vdb
    // asynchronous
    public static interface Callback {

        void onConnectionErrorAsync();

        void onVdbMounted();

        void onVdbUnmounted();

        void onClipSetInfoAsync(ClipSet clipSet);

        void onPlaylistSetInfoAsync(PlaylistSet playlistSet);

        void onImageDataAsync(ClipPos clipPos, byte[] data);

        void onBitmapDataAsync(ClipPos clipPos, Bitmap bitmap);

        void onPlaylistIndexPicDataAsync(ClipPos posterPoint, byte[] data);

        void onDownloadUrlFailedAsync();

        void onDownloadUrlReadyAsync(DownloadInfoEx downloadInfom, boolean bFirstLoop);

        void onPlaybackUrlReadyAsync(PlaybackUrl playbackUrl);

        void onGetPlaybackUrlErrorAsync();

        void onPlaylistPlaybackUrlReadyAsync(PlaylistPlaybackUrl playlistPlaybackUrl);

        void onGetPlaylistPlaybackUrlErrorAsync();

        void onMarkClipResultAsync(int error);

        void onDeleteClipResultAsync(int error);

        void onInsertClipResultAsync(int error);

        void onClipInfoAsync(int action, boolean isLive, Clip clip);

        void onMarkLiveClipInfo(int action, Clip clip, MarkLiveInfo info);

        void onClipRemovedAsync(Clip.ID cid);

        void onPlaylistClearedAsync(int playlistId);

        void onDownloadFinished(int id, String outputFile);

        void onDownloadStarted(int id);

        void onDownloadError(int id);

        void onDownloadProgress(int id, int progress);

        void onRawDataResultAsync(RawData rawDataResult);

        void onRawDataAsync(int dataType, byte[] data); // live info

        void onRawDataBlockAsync(RawDataBlock block);

        void onDownloadRawDataBlockAsync(RawDataBlock.DownloadRawDataBlock block);

        void onBufferSpaceLowAsync(BufferSpaceLowInfo info);

        void onBufferFullAsync();
    }

    public static class Request {
        public final int cmd;
        public final int sub_cmd; // CMD_Null
        public Object param;

        public Request(int cmd, Object param) {
            this.cmd = cmd;
            this.sub_cmd = 0;
            this.param = param;
        }

        public Request(int cmd, int sub_cmd) {
            this.cmd = cmd;
            this.sub_cmd = sub_cmd;
        }
    }

    protected static final int CMD_Null = 0;
    protected static final int CMD_GetVersionInfo = 1;
    protected static final int CMD_GetClipSetInfo = 2;
    protected static final int CMD_GetIndexPicture = 3;
    protected static final int CMD_GetPlaybackUrl = 4;
    // protected static final int CMD_GetDownloadUrl = 5; // obsolete
    protected static final int CMD_MarkClip = 6;
    // protected static final int CMD_GetCopyState = 7; // obsolete
    protected static final int CMD_DeleteClip = 8;
    protected static final int CMD_GetRawData = 9;
    protected static final int CMD_SetRawDataOption = 10;
    protected static final int CMD_GetRawDataBlock = 11;
    protected static final int CMD_GetDownloadUrlEx = 12;

    protected static final int CMD_GetAllPlaylists = 13;
    protected static final int CMD_GetPlaylistIndexPicture = 14;
    protected static final int CMD_ClearPlaylist = 15;
    protected static final int CMD_InsertClip = 16;
    protected static final int CMD_MoveClip = 17;
    protected static final int CMD_GetPlaylistPlaybackUrl = 18;

    protected static final int CMD_GetClipExtent = 32;
    protected static final int CMD_SetClipExtent = 33;
    protected static final int VDB_CMD_GetClipSetInfoEx = 34;
    protected static final int VDB_CMD_GetAllClipSetInfo = 35;
    protected static final int VDB_CMD_GetClipInfo = 36;

    protected final Callback mCallback;
    protected final Queue mQueue;

    public VdbClient(Callback callback) {
        mCallback = callback;
        mQueue = new Queue();
    }

    public boolean isLocal() {
        return false;
    }

    public void stopImageDecoder() {
    }

    public void requestClipSetInfo(int type) {
        if (DEBUG) {
            Log.d(TAG, "requestClipSetInfo");
        }
        mQueue.addRequest(CMD_GetClipSetInfo, type);
    }

    public void requestAllClipSetInfo(PlaylistSet playlistSet) {
        if (DEBUG) {
            Log.d(TAG, "requestAllClipSetInfo");
        }
        for (Playlist playlist : playlistSet.mPlaylists) {
            if (playlist != null) {
                mQueue.addRequest(CMD_GetClipSetInfo, playlist.plistId);
            }
        }
    }

    public static class GetClipImageRequest {
        public Clip clip;
        public ClipPos clipPos;
        int width;
        int height;
    }

    public void requestClipImage(Clip clip, ClipPos clipPos, int width, int height) {
        GetClipImageRequest request = new GetClipImageRequest();
        request.clip = clip;
        request.clipPos = clipPos;
        request.width = width;
        request.height = height;
        if (clipPos.isDiscardable()) {
            mQueue.addAnimationRequest(CMD_GetIndexPicture, request);
        } else {
            mQueue.addRequest(CMD_GetIndexPicture, request);
        }
    }

    public static class PlaybackUrlRequest {
        public final String vdbId;
        public final Clip.ID cid;
        public int mStream;
        public int mUrlType;
        public long mClipTimeMs;
        public int mClipLengthMs;

        public PlaybackUrlRequest(Clip clip) {
            this.vdbId = clip.getVdbId();
            this.cid = clip.cid;
        }
    }

    public void requestClipPlaybackUrl(int urlType, Clip clip, int stream, boolean bMuteAudio, long clipTimeMs,
                                       int clipLengthMs) {
        PlaybackUrlRequest request = new PlaybackUrlRequest(clip);
        request.mStream = stream;
        request.mUrlType = bMuteAudio ? urlType | URL_MUTE_AUDIO : urlType;
        request.mClipTimeMs = clipTimeMs;
        request.mClipLengthMs = clipLengthMs;
        mQueue.addRequest(CMD_GetPlaybackUrl, request);
    }

    public static class MarkClipRequest {
        public final String vdbId;
        public final Clip.ID cid;
        public long mStartTimeMs;
        public long mEndTimeMs;

        public MarkClipRequest(Clip clip) {
            this.vdbId = clip.getVdbId();
            this.cid = clip.cid;
        }
    }

    public void requestMarkClip(Clip clip, long startTimeMs, long endTimeMs) {
        MarkClipRequest request = new MarkClipRequest(clip);
        request.mStartTimeMs = startTimeMs;
        request.mEndTimeMs = endTimeMs;
        mQueue.addRequest(CMD_MarkClip, request);
    }

    public static class DeleteClipRequest {
        public final String vdbId;
        public final Clip.ID cid;

        public DeleteClipRequest(Clip clip) {
            this.vdbId = clip.getVdbId();
            this.cid = clip.cid;
        }
    }

    public void requestDeleteClip(Clip clip) {
        DeleteClipRequest request = new DeleteClipRequest(clip);
        mQueue.addRequest(CMD_DeleteClip, request);
    }

    public static class RawDataRequest {
        public String vdbId;
        public Clip.ID cid;
        public long mClipTimeMs;
        public int mTypes;

        public RawDataRequest(Clip clip, long clipTimeMs, int types) {
            this.vdbId = clip.getVdbId();
            this.cid = clip.cid;
            mClipTimeMs = clipTimeMs;
            mTypes = types;
        }

        public void set(Clip clip, long clipTimeMs, int types) {
            this.vdbId = clip.getVdbId();
            this.cid = clip.cid;
            mClipTimeMs = clipTimeMs;
            mTypes = types;
        }
    }

    public void requestRawData(Clip clip, long clipTimeMs, int types) {
        mQueue.addRawDataRequest(clip, clipTimeMs, types);
    }

    public void requestSetDrawDataOption(int rawDataTypes) {
        mQueue.addRequest(CMD_SetRawDataOption, rawDataTypes);
    }

    public static class RawDataBlockRequest {
        public final String vdbId;
        public final Clip.ID cid;
        public final boolean bForDownload;
        public long mClipTimeMs;
        public int mLengthMs;
        public int mDataType;

        public RawDataBlockRequest(String vdbId, Clip.ID cid, boolean bForDownload) {
            this.vdbId = vdbId;
            this.cid = cid;
            this.bForDownload = bForDownload;
        }
    }

    public void requestRawDataBlock(String vdbId, Clip.ID cid, long clipTimeMs, int lengthMs, int dataType,
                                    boolean bForDownload) {
        RawDataBlockRequest request = new RawDataBlockRequest(vdbId, cid, bForDownload);
        request.mClipTimeMs = clipTimeMs;
        request.mLengthMs = lengthMs;
        request.mDataType = dataType;
        mQueue.addRequest(CMD_GetRawDataBlock, request);
    }

    public static class DownloadUrlExRequest {
        public final String vdbId;
        public final Clip.ID cid;
        public final boolean mbFirst;
        public long mClipTimeMs;
        public int mClipLengthMs;
        public int mDownloadOpt;
        public boolean mbForPoster;
        public int mSessionCounter;

        public DownloadUrlExRequest(String vdbId, Clip.ID cid, boolean bFirst) {
            this.vdbId = vdbId;
            this.cid = cid;
            this.mbFirst = bFirst;
        }
    }

    public void requestClipDownloadUrlForPoster(Clip clip, long startTimeMs) {
        DownloadUrlExRequest request = new DownloadUrlExRequest(clip.getVdbId(), clip.cid, false);
        request.mClipTimeMs = startTimeMs;
        request.mClipLengthMs = 10;
        request.mDownloadOpt = DOWNLOAD_OPT_MAIN_STREAM;
        request.mbForPoster = true;
        mQueue.addRequest(CMD_GetDownloadUrlEx, request);
    }

    public void requestClipDownloadUrl(Clip clip, long startTimeMs, int lengthMs, boolean bFirst) {
        DownloadUrlExRequest request = new DownloadUrlExRequest(clip.getVdbId(), clip.cid, bFirst);
        request.mClipTimeMs = startTimeMs;
        request.mClipLengthMs = lengthMs;
        request.mDownloadOpt = DOWNLOAD_OPT_MAIN_STREAM | DOWNLOAD_OPT_SUB_STREAM_1 | DOWNLOAD_OPT_INDEX_PICT;
        request.mbForPoster = false;
        mQueue.addRequest(CMD_GetDownloadUrlEx, request);
    }

    public void requestPlaylistDownloadUrl(String vdbId, int plistId, int startMs, int lengthMs, boolean bMuteAudio,
                                           boolean bFirst) {
        Clip.ID cid = new Clip.ID(Clip.CAT_REMOTE, plistId, 0, vdbId); // TODO
        DownloadUrlExRequest request = new DownloadUrlExRequest(null, cid, bFirst);
        request.mClipTimeMs = startMs;
        request.mClipLengthMs = lengthMs;
        request.mDownloadOpt = DOWNLOAD_OPT_MAIN_STREAM | DOWNLOAD_OPT_SUB_STREAM_1 | DOWNLOAD_OPT_INDEX_PICT
            | DOWNLOAD_OPT_PLAYLIST;
        if (bMuteAudio) {
            request.mDownloadOpt |= DOWNLOAD_OPT_MUTE_AUDIO;
        }
        mQueue.addRequest(CMD_GetDownloadUrlEx, request);
    }

    public static class GetPlaylistSetInfo {
        public int mFlags;

        public GetPlaylistSetInfo(int flags) {
            mFlags = flags;
        }
    }

    public void requestPlaylistSetInfo(int flags) {
        if (DEBUG) {
            Log.d(TAG, "requestPlaylistSetInfo");
        }
        GetPlaylistSetInfo request = new GetPlaylistSetInfo(flags);
        mQueue.addRequest(CMD_GetAllPlaylists, request);
    }

    public static class GetPlaylistImageRequest {
        public int mListType;
        public int mFlags;

        public GetPlaylistImageRequest(int listType, int flags) {
            mListType = listType;
            mFlags = flags;
        }
    }

    public void requestPlaylistIndexImage(int listType, int flags) {
        if (DEBUG) {
            Log.d(TAG, "requestPlaylistImage");
        }
        GetPlaylistImageRequest request = new GetPlaylistImageRequest(listType, flags);
        mQueue.addRequest(CMD_GetPlaylistIndexPicture, request);
    }

    public static class ClearPlaylistRequest {
        public int mListType;
    }

    public void requestClearPlaylist(int listType) {
        ClearPlaylistRequest request = new ClearPlaylistRequest();
        request.mListType = listType;
        mQueue.addRequest(CMD_ClearPlaylist, request);
    }

    public static class InsertClipRequest {
        public final Clip.ID cid;
        public long mStartTimeMs;
        public long mEndTimeMs;
        public int mListType;
        public int mListPos;

        public InsertClipRequest(Clip.ID cid) {
            this.cid = cid;
        }
    }

    public void requestInsertClip(Clip.ID cid, long startTimeMs, long endTimeMs, int listType, int listPos) {
        InsertClipRequest request = new InsertClipRequest(cid);
        request.mStartTimeMs = startTimeMs;
        request.mEndTimeMs = endTimeMs;
        request.mListType = listType;
        request.mListPos = listPos;
        mQueue.addRequest(CMD_InsertClip, request);
    }

    public static class MoveClipRequest {
        public final Clip.ID cid;
        public int mNewClipPos;

        public MoveClipRequest(Clip.ID cid) {
            this.cid = cid;
        }
    }

    public void requestMoveClip(Clip.ID cid, int newClipPos) {
        MoveClipRequest request = new MoveClipRequest(cid);
        request.mNewClipPos = newClipPos;
        mQueue.addRequest(CMD_MoveClip, request);
    }

    public static class GetPlaylistPlaybackUrlRequest {
        public int mListType;
        public int mPlaylistStartMs;
        public int mStream;
        public int mUrlType;
    }

    public void requestPlaylistPlaybackUrl(int urlType, int listType, int playlistStartMs, int stream,
                                           boolean bMuteAudio) {
        GetPlaylistPlaybackUrlRequest request = new GetPlaylistPlaybackUrlRequest();
        request.mListType = listType;
        request.mPlaylistStartMs = playlistStartMs;
        request.mStream = stream;
        request.mUrlType = bMuteAudio ? urlType | URL_MUTE_AUDIO : urlType;
        mQueue.addRequest(CMD_GetPlaylistPlaybackUrl, request);
    }

    public void requestAllClipSetInfo() {
        mQueue.addRequest(VDB_CMD_GetAllClipSetInfo, null);
    }

    // ----------------------------------------------------------------------------

    abstract protected void cmdNull(int sub_cmd, Object param) throws IOException;

    abstract protected void cmdGetVersionInfo() throws IOException;

    abstract protected void cmdGetClipSetInfo(int type) throws IOException;

    abstract protected void cmdGetIndexPicture(GetClipImageRequest param) throws IOException;

    abstract protected void cmdGetPlaybackUrl(PlaybackUrlRequest param) throws IOException;

    abstract protected void cmdMarkClip(MarkClipRequest param) throws IOException;

    abstract protected void cmdDeleteClip(DeleteClipRequest param) throws IOException;

    abstract protected void cmdGetRawData(RawDataRequest param) throws IOException;

    abstract protected void cmdSetRawDataOption(Integer rawDataTypes) throws IOException;

    abstract protected void cmdGetRawDataBlock(RawDataBlockRequest param) throws IOException;

    abstract protected void cmdGetDownloadUrlEx(DownloadUrlExRequest param) throws IOException;

    abstract protected void cmdGetPlaylistSet(GetPlaylistSetInfo request) throws IOException;

    abstract protected void cmdGetPlaylistIndexPicture(GetPlaylistImageRequest request) throws IOException;

    abstract protected void cmdClearPlaylist(ClearPlaylistRequest request) throws IOException;

    abstract protected void cmdInsertClip(InsertClipRequest request) throws IOException;

    abstract protected void cmdMoveClip(MoveClipRequest request) throws IOException;

    abstract protected void cmdGetPlaylistPlaybackUrl(GetPlaylistPlaybackUrlRequest request) throws IOException;

    abstract protected void cmdGetAllClipSetInfo() throws IOException;

    protected final void cmdLoop(Thread thread) throws IOException, InterruptedException {
        while (!thread.isInterrupted()) {
            Request request = mQueue.getRequest();
            switch (request.cmd) {
                case CMD_Null:
                    cmdNull(request.sub_cmd, request.param);
                    break;

                case CMD_GetVersionInfo:
                    cmdGetVersionInfo();
                    break;
                case CMD_GetClipSetInfo:
                    cmdGetClipSetInfo((Integer) request.param);
                    break;
                case CMD_GetIndexPicture:
                    cmdGetIndexPicture((GetClipImageRequest) request.param);
                    break;
                case CMD_GetPlaybackUrl:
                    cmdGetPlaybackUrl((PlaybackUrlRequest) request.param);
                    break;
                case CMD_MarkClip:
                    cmdMarkClip((MarkClipRequest) request.param);
                    break;
                case CMD_DeleteClip:
                    cmdDeleteClip((DeleteClipRequest) request.param);
                    break;
                case CMD_GetRawData:
                    cmdGetRawData((RawDataRequest) request.param);
                    break;
                case CMD_SetRawDataOption:
                    cmdSetRawDataOption((Integer) request.param);
                    break;
                case CMD_GetRawDataBlock:
                    cmdGetRawDataBlock((RawDataBlockRequest) request.param);
                    break;
                case CMD_GetDownloadUrlEx:
                    cmdGetDownloadUrlEx((DownloadUrlExRequest) request.param);
                    break;

                case CMD_GetAllPlaylists:
                    cmdGetPlaylistSet((GetPlaylistSetInfo) request.param);
                    break;
                case CMD_GetPlaylistIndexPicture:
                    cmdGetPlaylistIndexPicture((GetPlaylistImageRequest) request.param);
                    break;
                case CMD_ClearPlaylist:
                    cmdClearPlaylist((ClearPlaylistRequest) request.param);
                    break;
                case CMD_InsertClip:
                    cmdInsertClip((InsertClipRequest) request.param);
                    break;
                case CMD_MoveClip:
                    cmdMoveClip((MoveClipRequest) request.param);
                    break;
                case CMD_GetPlaylistPlaybackUrl:
                    cmdGetPlaylistPlaybackUrl((GetPlaylistPlaybackUrlRequest) request.param);
                    break;

                case VDB_CMD_GetAllClipSetInfo:
                    cmdGetAllClipSetInfo();
                    break;

                default:
                    Log.e(TAG, "unknown cmd code " + request.cmd);
                    break;
            }
        }
    }

    // ----------------------------------------------------------------------------

    // this queue holds all requests sent by caller
    static class Queue {

        static final int kNumPendingAnimation = 1;

        private LinkedList<Request> mRequestQueue = new LinkedList<Request>();
        private boolean mbThreadWaiting = false;
        private Request mAnimationRequest;
        private RawDataRequest mRawDataRequest;

        private int mNumPendingAnimation;

        public static final int ANIM_STATE_IDLE = 0;
        public static final int ANIM_STATE_STARTING = 1;
        public static final int ANIM_STATE_WAIT_URL = 2;
        public static final int ANIM_STATE_REMUXING = 3;
        public static final int ANIM_STATE_DECODING = 4;

        // decode bitmap from remote I-frame
        private int mAnimState = ANIM_STATE_IDLE;
        private long mAnimRequestTime;
        private ClipPos mAnimClipPos;
        private int mAnimCounter;

        private final void notifyClient() {
            if (mbThreadWaiting) {
                mbThreadWaiting = false;
                notifyAll();
            }
        }

        synchronized public void addAnimationRequest(int cmd, GetClipImageRequest request) {
            if (mAnimationRequest == null) {
                mAnimationRequest = new Request(cmd, request);
                notifyClient();
            } else {
                mAnimationRequest.param = request;
                // client was already notified
            }
        }

        synchronized public void addRawDataRequest(Clip clip, long clipTimeMs, int types) {
            if (mRawDataRequest == null) {
                mRawDataRequest = new RawDataRequest(clip, clipTimeMs, types);
                notifyClient();
            } else {
                mRawDataRequest.set(clip, clipTimeMs, types);
                // client was already notified
            }
        }

        synchronized public void animationDataReceived() {
            if (mNumPendingAnimation > 0 && --mNumPendingAnimation < kNumPendingAnimation) {
                notifyClient();
            }
        }

        // any state -> waiting
        synchronized public void initAnimation(ClipPos clipPos) {
            if (DEBUG) {
                if (mAnimState != ANIM_STATE_IDLE && mAnimState != ANIM_STATE_STARTING) {
                    Log.d(TAG, "initAnimation, prev state: " + mAnimState);
                }
            }
            mAnimState = ANIM_STATE_STARTING;
            mAnimRequestTime = SystemClock.uptimeMillis();
            mAnimClipPos = clipPos;
            mAnimCounter++;
            notifyAll();
        }

        // any state -> idle
        synchronized public void endAnimation() {
            if (DEBUG) {
                Log.d(TAG, "endAnimation, prev: " + mAnimState);
            }
            mAnimState = ANIM_STATE_IDLE;
        }

        // wait url -> remuxing
        synchronized public boolean startRemux(int sessionCounter) {
            if (mAnimState != ANIM_STATE_WAIT_URL || sessionCounter != mAnimCounter) {
                if (DEBUG) {
                    Log.d(TAG, "startRemuxer() should cancel, state: " + mAnimState + ", counter: " + sessionCounter
                        + "," + mAnimCounter);
                }
                return false;
            }

            if (DEBUG) {
                Log.d(TAG, "startRemux go");
            }

            mAnimState = ANIM_STATE_REMUXING;
            return true;
        }

        // remuxing -> decoding
        synchronized public boolean startDecode(int sessionCounter) {
            if (mAnimState != ANIM_STATE_REMUXING || sessionCounter != mAnimCounter) {
                if (DEBUG) {
                    Log.d(TAG, "startDecode() should cancel, state: " + mAnimState + ", counter: " + sessionCounter
                        + "," + mAnimCounter);
                }
                return false;
            }

            if (DEBUG) {
                Log.d(TAG, "startDecode go");
            }

            mAnimState = ANIM_STATE_DECODING;
            return true;
        }

        // decoding -> decoding
        synchronized public ClipPos endDecode(int counter) {
            if (mAnimState != ANIM_STATE_DECODING || counter != mAnimCounter) {
                if (DEBUG) {
                    Log.d(TAG, "endDecode() should cancel, state: " + mAnimState + ", counter: " + counter + ","
                        + mAnimCounter);
                }
                return null;
            }

            if (DEBUG) {
                Log.d(TAG, "endDecode ok");
            }

            // mAnimState = ANIM_STATE_IDLE;
            return mAnimClipPos;
        }

        synchronized public boolean endDecode2(int counter) {
            if (mAnimState != ANIM_STATE_DECODING || counter != mAnimCounter) {
                if (DEBUG) {
                    Log.d(TAG, "endDecode2() should cancel, state: " + mAnimState + ", counter: " + counter + ","
                        + mAnimCounter);
                }
                return false;
            }

            if (DEBUG) {
                Log.d(TAG, "endDecode2 ok");
            }

            mAnimState = ANIM_STATE_IDLE;
            return true;
        }

        synchronized public final void addRequest(Request request) {
            mRequestQueue.addLast(request);
            notifyClient();
        }

        synchronized public final void addRequest(int cmd, Object param) {
            mRequestQueue.addLast(new Request(cmd, param));
            notifyClient();
        }

        // wait and get a request until the thread is interrupted
        synchronized public Request getRequest() throws InterruptedException {
            while (true) {
                // check the queue
                if (mRequestQueue.size() > 0) {
                    return mRequestQueue.removeFirst();
                }

                // animation request
                if (mAnimationRequest != null) {
                    if (mNumPendingAnimation < kNumPendingAnimation) {
                        Request result = mAnimationRequest;
                        mAnimationRequest = null;
                        mNumPendingAnimation++;
                        return result;
                    }
                }

                // raw data request
                if (mRawDataRequest != null) {
                    Request result = new Request(CMD_GetRawData, mRawDataRequest);
                    mRawDataRequest = null;
                    return result;
                }

                // check animation
                if (mAnimState == ANIM_STATE_STARTING) {
                    int elapsed = (int) (SystemClock.uptimeMillis() - mAnimRequestTime);
                    if (elapsed < DECODE_IFRAME_DELAY) {
                        mbThreadWaiting = true;
                        wait(DECODE_IFRAME_DELAY - elapsed);
                        continue;
                    }

                    // starting -> wait url
                    mAnimState = ANIM_STATE_WAIT_URL;

                    // compose request for url (.ts)
                    DownloadUrlExRequest request = new DownloadUrlExRequest(mAnimClipPos.vdbId, mAnimClipPos.cid, false);
                    request.mClipTimeMs = mAnimClipPos.getClipTimeMs();
                    request.mClipLengthMs = 1; // no use
                    request.mDownloadOpt = DOWNLOAD_OPT_MAIN_STREAM;
                    request.mbForPoster = true;
                    request.mSessionCounter = mAnimCounter;

                    // trigger client thread to fetch download url
                    return new Request(CMD_GetDownloadUrlEx, request);
                }

                // no request, so wait
                mbThreadWaiting = true;
                wait();
            }
        }
    }

}
