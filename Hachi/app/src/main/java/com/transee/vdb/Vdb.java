package com.transee.vdb;

import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;

import com.transee.common.ByteStream;
import com.transee.common.GPSPath;
import com.transee.common.GPSRawData;
import com.transee.vdb.RemoteVdbClient.BufferSpaceLowInfo;
import com.transee.vdb.VdbClient.PlaylistPlaybackUrl;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.DownloadInfoEx;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.vdb.RawData;
import com.waylens.hachi.vdb.RawDataBlock;

abstract public class Vdb {

    static final boolean DEBUG = false;
    static final String TAG = "Vdb";

    public static final int VERSION_1_0 = 0; // initial version

    // implemented by CameraVideoActivity
    public interface Callback {

        void onConnectError(Vdb vdb);

        void onVdbMounted(Vdb vdb);

        void onVdbUnmounted(Vdb vdb);

        void onClipSetInfo(Vdb vdb, ClipSet clipSet);

        void onPlaylistSetInfo(Vdb vdb, PlaylistSet playlistSet);

        void onPlaylistChanged(Vdb vdb, Playlist playlist);

        void onImageData(Vdb vdb, ClipPos clipPos, byte[] data);

        void onBitmapData(Vdb vdb, ClipPos clipPos, Bitmap bitmap);

        void onPlaylistIndexPicData(Vdb vdb, ClipPos clipPos, byte[] data);

        void onPlaybackUrlReady(Vdb vdb, PlaybackUrl playbackUrl);

        void onGetPlaybackUrlError(Vdb vdb);

        void onPlaylistPlaybackUrlReady(Vdb vdb, PlaylistPlaybackUrl playlistPlaybackUrl);

        void onGetPlaylistPlaybackUrlError(Vdb vdb);

        void onMarkClipResult(Vdb vdb, int error);

        void onDeleteClipResult(Vdb vdb, int error);

        void onInsertClipResult(Vdb vdb, int error);

        void onClipCreated(Vdb vdb, boolean isLive, Clip clip);

        void onClipChanged(Vdb vdb, boolean isLive, Clip clip);

        void onClipFinished(Vdb vdb, boolean isLive, Clip clip);

        void onClipInserted(Vdb vdb, boolean isLive, Clip clip);

        void onClipMoved(Vdb vdb, boolean isLive, Clip clip);

        void onClipRemoved(Vdb vdb, Clip.ID cid);

        void onPlaylistCleared(Vdb vdb, int plistId);

        void onRawDataResult(Vdb vdb, RawData rawDataResult);

        void onRawDataBlock(Vdb vdb, RawDataBlock block);

        void onDownloadRawDataBlock(Vdb vdb, RawDataBlock.DownloadRawDataBlock block);

        void onGPSSegment(Vdb vdb, GPSPath.Segment segment);

        void onDownloadUrlFailed(Vdb vdb);

        void onDownloadUrlReady(Vdb vdb, DownloadInfoEx downloadInfo, boolean bFirstLoop);

        void onDownloadStarted(Vdb vdb, int id);

        void onDownloadFinished(Vdb vdb, Clip oldClip, Clip newClip);

        void onDownloadError(Vdb vdb, int id);

        void onDownloadProgress(Vdb vdb, int id, int progress);
    }

    protected int mVersion = VERSION_1_0;
    protected final Handler mHandler;
    protected final Callback mCallback;

    public static class MarkLiveInfo {
        int flags;
        int delay_ms;
        int before_live_ms;
        int after_live_ms;
    }

    // will run on caller's thread
    public Vdb(Callback callback) {
        mHandler = new Handler();
        mCallback = callback;
    }

    // API
    public int getVersion() {
        return mVersion;
    }

    // API
    public abstract boolean isLocal();

    // API
    public abstract void start(String hostString);

    // API
    public abstract void stop();

    // API
    public abstract ClipSet getClipSet(int clipType);

    // API
    public abstract PlaylistSet getPlaylistSet();

    // API
    public abstract VdbClient getClient();

    protected abstract void onVdbMounted();

    protected abstract void onVdbUnmounted();

    // PlaylistSet info is received
    protected void handlePlaylistSetInfo(PlaylistSet playlistSet) {
        PlaylistSet myPlaylistSet = getPlaylistSet();
        if (myPlaylistSet != null) {
            myPlaylistSet.set(playlistSet);
            mCallback.onPlaylistSetInfo(this, myPlaylistSet);
            // request all clipsets of all playlists
            getClient().requestAllClipSetInfo(myPlaylistSet);
        }
    }

    // ClipSet info is received
    protected void handleClipSetInfo(ClipSet clipSet) {
        ClipSet myClipSet = getClipSet(clipSet.clipType);
        if (myClipSet != null) {
            myClipSet.set(clipSet);
            mCallback.onClipSetInfo(this, myClipSet);
            return;
        }
        PlaylistSet playlistSet = getPlaylistSet();
        if (playlistSet != null) {
            Playlist playlist = playlistSet.findPlaylist(clipSet.clipType);
            if (playlist != null) {
                playlist.setClipSet(clipSet);
                mCallback.onPlaylistChanged(this, playlist);
                return;
            }
        }
        Log.d(TAG, "unknown clipset: " + clipSet.clipCat);
    }

    protected void handleClipCreated(boolean isLive, Clip clip) {
        if (DEBUG) {
            Log.d(TAG, "clip created");
        }
        ClipSet clipSet = getClipSet(clip.cid.type);
        if (clipSet != null) {
            clipSet.insertClipById(clip);
            mCallback.onClipCreated(this, isLive, clip);
        }
    }

    // length or startTime changed
    protected void handleClipChanged(boolean isLive, Clip clip) {
        ClipSet clipSet = getClipSet(clip.cid.type);
        if (clipSet != null) {
            clipSet.clipChanged(clip, isLive, false);
            mCallback.onClipChanged(this, isLive, clip);
        }
    }

    protected void handleClipFinished(boolean isLive, Clip clip) {
        if (DEBUG) {
            Log.d(TAG, "clip finished");
        }
        ClipSet clipSet = getClipSet(clip.cid.type);
        if (clipSet != null) {
            clipSet.clipChanged(clip, isLive, true);
            mCallback.onClipFinished(this, isLive, clip);
        }
    }

    protected void handleClipInserted(boolean isLive, Clip clip) {
        PlaylistSet playlistSet = getPlaylistSet();
        if (playlistSet != null) {
            Playlist playlist = playlistSet.findPlaylist(clip.cid.type);
            if (playlist != null) {
                playlist.insertClip(clip);
            }
            mCallback.onClipInserted(this, isLive, clip);
        }
    }

    protected void handleClipMoved(boolean isLive, Clip clip) {
        PlaylistSet playlistSet = getPlaylistSet();
        if (playlistSet != null) {
            Playlist playlist = playlistSet.findPlaylist(clip.cid.type);
            if (playlist != null) {
                playlist.moveClip(clip);
            }
            mCallback.onClipMoved(this, isLive, clip);
        }
    }

    protected void handleClipRemoved(Clip.ID cid) {
        ClipSet clipSet = getClipSet(cid.type);
        if (clipSet != null) {
            clipSet.removeClip(cid);
            mCallback.onClipRemoved(this, cid);
        } else {
            PlaylistSet playlistSet = getPlaylistSet();
            if (playlistSet != null) {
                Playlist playlist = playlistSet.findPlaylist(cid.type);
                if (playlist != null) {
                    if (playlist.removeClip(cid)) {
                        mCallback.onClipRemoved(this, cid);
                    }
                }
            }
        }
    }

    protected void handlePlaylistCleared(int plistId) {
        PlaylistSet playlistSet = getPlaylistSet();
        if (playlistSet != null) {
            playlistSet.clearPlaylist(plistId);
            mCallback.onPlaylistCleared(this, plistId);
        }
    }

    protected void handleDownloadStarted(int id) {
    }

    protected void handleDownloadFinished(int id, String outputFile) {
    }

    protected void handleDownloadError(int id) {
    }

    protected void handleDownloadProgress(int id, int progress) {
    }

    protected void handleGPSDataBlock(RawDataBlock block) {
        RawDataBlock.RawDataBlockHeader header = block.header;

        if (header.mNumItems <= 0) {
            return;
        }

        GPSRawData.Coord coord = new GPSRawData.Coord();
        int n = header.mNumItems;
        byte[] data = block.data;

        double[] latArray = new double[n];
        double[] lngArray = new double[n];
        byte[] sepArray = new byte[n];
        long startTimeMs = header.mRequestedTimeMs + block.timeOffsetMs[0];

        int offset = 0;
        for (int i = 0; i < n; i++) {
            coord.lat = coord.lat_orig = ByteStream.readDouble(data, offset + 8);
            coord.lng = coord.lng_orig = ByteStream.readDouble(data, offset + 16);

            GPSRawData.GMS84ToGCJ02(coord);

            latArray[i] = coord.lat;
            lngArray[i] = coord.lng;
            sepArray[i] = block.dataSize[i] == 0 ? (byte) 1 : (byte) 0;

            offset += block.dataSize[i];
        }

        GPSPath.Segment segment = new GPSPath.Segment(header, latArray, lngArray, sepArray, startTimeMs);
        sendGPSSegment(segment);
    }

    private void sendGPSSegment(final GPSPath.Segment segment) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mCallback.onGPSSegment(Vdb.this, segment);
            }
        });
    }

    // Async -> Sync
    protected class VdbClientCallback implements VdbClient.Callback {

        @Override
        public void onConnectionErrorAsync() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onConnectError(Vdb.this);
                }
            });
        }

        @Override
        public void onVdbMounted() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Vdb.this.onVdbMounted();
                    mCallback.onVdbMounted(Vdb.this);
                }
            });
        }

        @Override
        public void onVdbUnmounted() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Vdb.this.onVdbUnmounted();
                    mCallback.onVdbUnmounted(Vdb.this);
                }
            });
        }

        @Override
        public void onClipSetInfoAsync(final ClipSet clipSet) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleClipSetInfo(clipSet);
                }
            });
        }

        @Override
        public void onPlaylistSetInfoAsync(final PlaylistSet playlistSet) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    handlePlaylistSetInfo(playlistSet);
                }
            });
        }

        @Override
        public void onImageDataAsync(final ClipPos clipPos, final byte[] data) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onImageData(Vdb.this, clipPos, data);
                }
            });
        }

        @Override
        public void onBitmapDataAsync(final ClipPos clipPos, final Bitmap bitmap) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onBitmapData(Vdb.this, clipPos, bitmap);
                }
            });
        }

        @Override
        public void onPlaylistIndexPicDataAsync(final ClipPos clipPos, final byte[] data) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onPlaylistIndexPicData(Vdb.this, clipPos, data);
                }
            });
        }

        @Override
        public void onPlaylistClearedAsync(final int plistId) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    handlePlaylistCleared(plistId);
                }
            });
        }

        @Override
        public void onDownloadUrlFailedAsync() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onDownloadUrlFailed(Vdb.this);
                }
            });
        }

        @Override
        public void onDownloadUrlReadyAsync(final DownloadInfoEx downloadInfo, final boolean bFirstLoop) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onDownloadUrlReady(Vdb.this, downloadInfo, bFirstLoop);
                }
            });
        }

        @Override
        public void onPlaybackUrlReadyAsync(final PlaybackUrl playbackUrl) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onPlaybackUrlReady(Vdb.this, playbackUrl);
                }
            });
        }

        @Override
        public void onGetPlaybackUrlErrorAsync() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onGetPlaybackUrlError(Vdb.this);
                }
            });
        }

        @Override
        public void onPlaylistPlaybackUrlReadyAsync(final PlaylistPlaybackUrl playlistPlaybackUrl) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onPlaylistPlaybackUrlReady(Vdb.this, playlistPlaybackUrl);
                }
            });
        }

        @Override
        public void onGetPlaylistPlaybackUrlErrorAsync() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onGetPlaylistPlaybackUrlError(Vdb.this);
                }
            });
        }

        @Override
        public void onMarkClipResultAsync(final int error) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onMarkClipResult(Vdb.this, error);
                }
            });
        }

        @Override
        public void onDeleteClipResultAsync(final int error) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onDeleteClipResult(Vdb.this, error);
                }
            });
        }

        @Override
        public void onInsertClipResultAsync(final int error) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onInsertClipResult(Vdb.this, error);
                }
            });
        }

        @Override
        public void onRawDataResultAsync(final RawData rawDataResult) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onRawDataResult(Vdb.this, rawDataResult);
                }
            });
        }

        @Override
        public void onClipInfoAsync(final int action, final boolean isLive, final Clip clip) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    switch (action) {
                        case VdbClient.CLIP_ACTION_CREATED:
                            handleClipCreated(isLive, clip);
                            break;

                        case VdbClient.CLIP_ACTION_CHANGED:
                            handleClipChanged(isLive, clip);
                            break;

                        case VdbClient.CLIP_ACTION_FINISHED:
                            handleClipFinished(isLive, clip);
                            break;

                        case VdbClient.CLIP_ACTION_INSERTED:
                            handleClipInserted(isLive, clip);
                            break;

                        case VdbClient.CLIP_ACTION_MOVED:
                            handleClipMoved(isLive, clip);
                            break;

                        default:
                            break;
                    }
                }
            });
        }

        @Override
        public void onMarkLiveClipInfo(int action, Clip clip, MarkLiveInfo info) {
            onClipInfoAsync(action, false, clip);
        }

        @Override
        public void onClipRemovedAsync(final Clip.ID cid) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleClipRemoved(cid);
                }
            });
        }

        @Override
        public void onBufferSpaceLowAsync(BufferSpaceLowInfo info) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                }
            });
        }

        @Override
        public void onBufferFullAsync() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                }
            });
        }

        @Override
        public void onRawDataAsync(int dataType, byte[] data) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // TODO
                }
            });
        }

        @Override
        public void onRawDataBlockAsync(final RawDataBlock block) {
            if (block.header.mDataType == RawDataBlock.RAW_DATA_GPS) {
                handleGPSDataBlock(block);
            } else {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onRawDataBlock(Vdb.this, block);
                    }
                });
            }
        }

        @Override
        public void onDownloadRawDataBlockAsync(final RawDataBlock.DownloadRawDataBlock block) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onDownloadRawDataBlock(Vdb.this, block);
                }
            });
        }

        @Override
        public void onDownloadStarted(final int id) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleDownloadStarted(id);
                }
            });
        }

        @Override
        public void onDownloadFinished(final int id, final String outputFile) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleDownloadFinished(id, outputFile);
                }
            });
        }

        @Override
        public void onDownloadError(final int id) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleDownloadError(id);
                }
            });
        }

        @Override
        public void onDownloadProgress(final int id, final int progress) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleDownloadProgress(id, progress);
                }
            });
        }

    }

}
