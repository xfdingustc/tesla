package com.waylens.hachi.snipe.reative;

import android.os.Bundle;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.snipe.VdbRequestFuture;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.AddBookmarkRequest;
import com.waylens.hachi.snipe.toolbox.ClipDeleteRequest;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlExRequest;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.snipe.toolbox.DownloadUrlRequest;
import com.waylens.hachi.snipe.toolbox.GetSpaceInfoRequest;
import com.waylens.hachi.snipe.toolbox.PlaylistPlaybackUrlRequest;
import com.waylens.hachi.snipe.toolbox.RawDataBlockRequest;
import com.waylens.hachi.snipe.toolbox.RawDataBufRequest;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipDownloadInfo;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.SpaceInfo;
import com.waylens.hachi.snipe.vdb.Vdb;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataBlock;
import com.waylens.hachi.snipe.vdb.urls.PlaybackUrl;
import com.waylens.hachi.snipe.vdb.urls.PlaylistPlaybackUrl;

import java.util.concurrent.ExecutionException;

/**
 * Created by Xiaofei on 2016/10/11.
 */

class SnipeApi {
    public static ClipSet getClipSet(int type, int flag, int attr) throws ExecutionException, InterruptedException {
        VdbRequestFuture<ClipSet> future = VdbRequestFuture.newFuture();
        ClipSetExRequest request = new ClipSetExRequest(type, flag, attr, future, future);
        VdtCameraManager.getManager().getCurrentVdbRequestQueue().add(request);
        return future.get();
    }

    public static Integer deleteClip(Clip.ID clipId) throws ExecutionException, InterruptedException {
        VdbRequestFuture<Integer> future = VdbRequestFuture.newFuture();
        ClipDeleteRequest request = new ClipDeleteRequest(clipId, future, future);
        VdtCameraManager.getManager().getCurrentVdbRequestQueue().add(request);
        return future.get();
    }

    public static SpaceInfo getSpaceInfo() throws ExecutionException, InterruptedException {
        VdbRequestFuture<SpaceInfo> future = VdbRequestFuture.newFuture();
        GetSpaceInfoRequest request = new GetSpaceInfoRequest(future, future);
        VdtCameraManager.getManager().getCurrentVdbRequestQueue().add(request);
        return future.get();
    }

    public static RawDataBlock getRawDataBlock(Clip clip, int dataType, long startTime, int duration) {
        Bundle params = new Bundle();
        params.putInt(RawDataBlockRequest.PARAM_DATA_TYPE, dataType);
        params.putLong(RawDataBlockRequest.PARAM_CLIP_TIME, startTime);
        params.putInt(RawDataBlockRequest.PARAM_CLIP_LENGTH, duration);

        VdbRequestFuture<RawDataBlock> requestFuture = VdbRequestFuture.newFuture();
        RawDataBlockRequest request = new RawDataBlockRequest(clip.cid, params, requestFuture, requestFuture);
        VdtCameraManager.getManager().getCurrentVdbRequestQueue().add(request);
        try {
            return requestFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }
    public static byte[] getRawDataBuf(Clip clip, int dataType, long startTime, int duration) {
        Bundle params = new Bundle();
        params.putInt(RawDataBufRequest.PARAM_DATA_TYPE, dataType);
        params.putLong(RawDataBufRequest.PARAM_CLIP_TIME, startTime);
        params.putInt(RawDataBufRequest.PARAM_CLIP_LENGTH, duration);

        VdbRequestFuture<byte[]> requestFuture = VdbRequestFuture.newFuture();
        RawDataBufRequest request = new RawDataBufRequest(clip.cid, params, requestFuture, requestFuture);
        VdtCameraManager.getManager().getCurrentVdbRequestQueue().add(request);
        try {
            byte[] buffer = requestFuture.get();
            return buffer;
        } catch (Exception e) {
            return null;
        }
    }

    public static PlaybackUrl getClipPlaybackUrl(Clip.ID clipId, long startTime, long clipTimeMs, int maxLength) throws ExecutionException, InterruptedException {
        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlExRequest.PARAMETER_URL_TYPE, Vdb.URL_TYPE_HLS);
        parameters.putInt(ClipPlaybackUrlExRequest.PARAMETER_STREAM, Vdb.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlExRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlExRequest.PARAMETER_CLIP_TIME_MS, clipTimeMs + startTime);
        parameters.putInt(ClipPlaybackUrlExRequest.PARAMETER_CLIP_LENGTH_MS, maxLength);


        VdbRequestFuture<PlaybackUrl> requestFuture = VdbRequestFuture.newFuture();
        ClipPlaybackUrlExRequest request = new ClipPlaybackUrlExRequest(clipId, parameters, requestFuture, requestFuture);
        VdtCameraManager.getManager().getCurrentVdbRequestQueue().add(request);

        return requestFuture.get();
    }

    public static PlaylistPlaybackUrl getPlaylistPlaybackUrl(int playlistId, int startTime) throws ExecutionException, InterruptedException {
        VdbRequestFuture<PlaylistPlaybackUrl> requestFuture = VdbRequestFuture.newFuture();
        PlaylistPlaybackUrlRequest request = new PlaylistPlaybackUrlRequest(playlistId, startTime, requestFuture, requestFuture);
        VdtCameraManager.getManager().getCurrentVdbRequestQueue().add(request);
        return requestFuture.get();
    }

    public static Integer addHighlight(Clip.ID clipId, long startTimeMs, long endTimeMs) throws ExecutionException, InterruptedException {
        VdbRequestFuture<Integer> requestFuture = VdbRequestFuture.newFuture();
        AddBookmarkRequest request = new AddBookmarkRequest(clipId, startTimeMs, endTimeMs, requestFuture, requestFuture);
        VdtCameraManager.getManager().getCurrentVdbRequestQueue().add(request);
        return requestFuture.get();
    }

    public static ClipDownloadInfo getClipDownloadInfo(Clip.ID cid, long start, int length) throws ExecutionException, InterruptedException {
        VdbRequestFuture<ClipDownloadInfo> requestFuture = VdbRequestFuture.newFuture();
        DownloadUrlRequest request = new DownloadUrlRequest(cid,  start, length, requestFuture, requestFuture);
        VdtCameraManager.getManager().getCurrentVdbRequestQueue().add(request);
        return requestFuture.get();
    }
}
