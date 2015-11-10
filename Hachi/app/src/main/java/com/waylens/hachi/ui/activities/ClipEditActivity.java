package com.waylens.hachi.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.orhanobut.logger.Logger;
import com.transee.vdb.RemuxHelper;
import com.transee.vdb.RemuxerParams;
import com.transee.vdb.VdbClient;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.hardware.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipExtentGetRequest;
import com.waylens.hachi.snipe.toolbox.ClipExtentUpdateRequest;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlExRequest;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlRequest;
import com.waylens.hachi.snipe.toolbox.DownloadUrlRequest;
import com.waylens.hachi.utils.ImageUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipExtent;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.DownloadInfo;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RemoteClip;
import com.waylens.hachi.views.VideoPlayerProgressBar;
import com.waylens.hachi.views.VideoTrimmer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2015/8/14.
 */
public class ClipEditActivity extends BaseActivity {
    private static final String TAG = ClipEditActivity.class.getSimpleName();

    private static final int MAX_EXTENSION = 1000 * 30;

    @Bind(R.id.ivPreviewPicture)
    ImageView mIvPreviewPicture;

    @Bind(R.id.btnPlay)
    ImageButton mBtnPlay;

    @Bind(R.id.video_seek_bar)
    VideoPlayerProgressBar mSeekBar;

    @Bind(R.id.video_trimmer)
    VideoTrimmer mVideoTrimmer;

    @Bind(R.id.btn_update_bookmark)
    ImageButton mBtnUpdateBookMark;

    @Bind(R.id.range_seek_bar_container)
    View mRangeSeekBarContainer;

    @Bind(R.id.btn_share)
    View mBtnShare;

    @Bind(R.id.btnDownload)
    Button mBtnDownload;

    @Bind(R.id.video_play_view)
    SurfaceView mVideoPlayView;

    private static VdtCamera mSharedCamera;
    private static Clip mSharedClip;

    private Clip mClip;

    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mVdbImageLoader;

    long oldPosition;

    long startPlayTime;

    long oldStartValue;
    long oldEndValue;

    ClipExtent mClipExtent;
    private MediaPlayer mPlayer;


    public static void launch(Context context, Clip clip) {
        Intent intent = new Intent(context, ClipEditActivity.class);
        mSharedClip = clip;
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mPlayer != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mPlayer.reset();
                    mPlayer.release();
                }
            }).start();
        }
    }

    @OnClick(R.id.btnPlay)
    public void onBtnPlayClicked() {
        ClipPlaybackActivity.launch(this, mClip);
        //preparePlayback();
    }

    @OnClick(R.id.btnDownload)
    public void onBtnDownload() {
        //downloadClip();

    }

    @OnClick(R.id.btn_share)
    public void shareVideo() {
        playVideo();
    }

    void playVideo() {
        if (mPlayer != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mSeekBar.setMediaPlayer(null);
                    mPlayer.reset();
                    mPlayer.release();
                }
            }).start();
        }

        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_URL_TYPE, VdbClient.URL_TYPE_HLS);
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_STREAM, VdbClient.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, mClip.getStartTime());

        ClipPlaybackUrlRequest request = new ClipPlaybackUrlRequest(mClip, parameters, new VdbResponse.Listener<PlaybackUrl>() {
            @Override
            public void onResponse(PlaybackUrl playbackUrl) {
                mPlayer = new MediaPlayer();
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mIvPreviewPicture.setVisibility(View.GONE);
                mBtnPlay.setVisibility(View.GONE);
                mPlayer.setDisplay(mVideoPlayView.getHolder());
                mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.start();
                        //mSeekBar.setMediaPlayer(mPlayer);
                        mVideoTrimmer.setMediaPlayer(mPlayer);
                    }
                });
                try {
                    mPlayer.setDataSource(playbackUrl.url);
                    mPlayer.prepareAsync();
                } catch (IOException e) {
                    Log.e("test", "", e);
                }
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                //
            }
        });

        mVdbRequestQueue.add(request);
    }

    void shareMoment() {
        JSONObject params = new JSONObject();
        try {
            params.put("title", "Moment from Android");
            JSONObject raw = new JSONObject();
            raw.put("guid", mClip.getVdbId());
            JSONArray rawArray = new JSONArray();
            rawArray.put(raw);
            params.put("rawData", rawArray);
            JSONObject fragment = new JSONObject();
            fragment.put("guid", mClip.getVdbId());
            fragment.put("clipCaptureTime", mClip.clipDate * 1000l);
            fragment.put("beginTime", (int) mClip.getStartTime());
            fragment.put("offset", 0);
            fragment.put("duration", mClip.clipLengthMs);
            JSONArray fragments = new JSONArray();
            fragments.put(fragment);
            params.put("fragments", fragments);
        } catch (JSONException e) {
            Log.e("test", "", e);
        }
        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.POST, Constants.API_MOMENTS, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e("test", "Response: " + response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("test", "", error);
                    }
                }));
    }

    @Override
    protected void init() {
        super.init();
        mClip = mSharedClip;
        mVdbRequestQueue = Snipe.newRequestQueue(this);
        mVdbImageLoader = new VdbImageLoader(mVdbRequestQueue);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_clip_editor);
        final ClipPos clipPos = new ClipPos(mClip, mClip.getStartTime(), ClipPos.TYPE_POSTER, false);
        mVdbImageLoader.displayVdbImage(clipPos, mIvPreviewPicture);

        initSeekBar();
        if (mClip.cid.type == RemoteClip.TYPE_BUFFERED) {
            mSeekBar.setVisibility(View.VISIBLE);
            mRangeSeekBarContainer.setVisibility(View.GONE);
            //mBtnShare.setVisibility(View.GONE);
        } else {
            getClipExtent();
            mRangeSeekBarContainer.setVisibility(View.VISIBLE);
            mVideoTrimmer.setVisibility(View.INVISIBLE);
            mSeekBar.setVisibility(View.VISIBLE);
            //mBtnShare.setVisibility(View.VISIBLE);
        }
    }

    private void initSeekBar() {
        final ClipPos clipPos = new ClipPos(mClip, mClip.getStartTime(), ClipPos.TYPE_POSTER, false);
        mSeekBar.setClip(mClip, mVdbImageLoader);
        mSeekBar.setOnSeekBarChangeListener(new VideoPlayerProgressBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(VideoPlayerProgressBar progressBar) {
                Log.e("test", "Start dragging....");
            }

            @Override
            public void onProgressChanged(VideoPlayerProgressBar progressBar, long progress, boolean fromUser) {
                Log.e("test", "Progress: " + progress);
                if (Math.abs(oldPosition - progress) > 1000) {
                    refreshThumbnail(mClip.getStartTime() + progress, clipPos);
                    oldPosition = progress;
                } else {
                    Log.e("test", "Progress: skipped");
                }
            }

            @Override
            public void onStopTrackingTouch(VideoPlayerProgressBar progressBar) {
                Log.e("test", "Stop dragging....");
            }
        });
    }

    void refreshThumbnail(long clipTimeMs, ClipPos clipPos) {
        clipPos.setClipTimeMs(clipTimeMs);
        mVdbImageLoader.displayVdbImage(clipPos, mIvPreviewPicture, true);
    }

    private void getClipExtent() {
        mVdbRequestQueue.add(new ClipExtentGetRequest(mClip, new VdbResponse.Listener<ClipExtent>() {
            @Override
            public void onResponse(ClipExtent clipExtent) {
                if (clipExtent != null) {
                    mClipExtent = clipExtent;
                    initRangeSeekBar();
                }
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Log.e("test", "", error);
            }
        }));
    }

    @SuppressWarnings("deprecation")
    void initRangeSeekBar() {
        if (mVideoTrimmer == null) {
            return;
        }
        long minValue = mClipExtent.clipStartTimeMs - MAX_EXTENSION;
        if (minValue < mClipExtent.minClipStartTimeMs) {
            minValue = mClipExtent.minClipStartTimeMs;
        }
        long maxValue = mClipExtent.clipEndTimeMs + MAX_EXTENSION;
        if (maxValue > mClipExtent.maxClipEndTimeMs) {
            maxValue = mClipExtent.maxClipEndTimeMs;
        }

        mVideoTrimmer.setBackgroundClip(mVdbImageLoader, mClip, minValue, maxValue);
        mVideoTrimmer.setInitRangeValues(minValue, maxValue);
        mVideoTrimmer.setLeftValue(mClipExtent.clipStartTimeMs);
        mVideoTrimmer.setRightValue(mClipExtent.clipEndTimeMs);

        oldStartValue = mClipExtent.clipStartTimeMs;
        oldEndValue = mClipExtent.clipEndTimeMs;

        final ClipPos clipPos = new ClipPos(null,
                mClipExtent.readCid,
                mClip.clipDate,
                mClipExtent.clipStartTimeMs,
                ClipPos.TYPE_POSTER, false);
        mVideoTrimmer.setOnChangeListener(new VideoTrimmer.OnTrimmerChangeListener() {
            @Override
            public void onStartTrackingTouch(VideoTrimmer trimmer, VideoTrimmer.DraggingFlag flag) {
                //
            }

            @Override
            public void onProgressChanged(VideoTrimmer trimmer, VideoTrimmer.DraggingFlag flag, long start, long end, long progress) {
                if (start != mClipExtent.clipStartTimeMs || end != mClipExtent.clipEndTimeMs) {
                    mBtnUpdateBookMark.getDrawable().setColorFilter(getResources().getColor(R.color.style_color_primary),
                            PorterDuff.Mode.MULTIPLY);
                }
                if (Math.abs(oldStartValue - start) > 1000) {
                    refreshThumbnail(start, clipPos);
                    oldStartValue = start;
                }
                if (Math.abs(oldEndValue - end) > 1000) {
                    refreshThumbnail(end, clipPos);
                    oldEndValue = end;
                }
            }

            @Override
            public void onStopTrackingTouch(VideoTrimmer trimmer) {
                //
            }
        });
        mVideoTrimmer.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.btn_update_bookmark)
    public void updateBookmark() {
        if (mClipExtent == null
                || (mClipExtent.clipStartTimeMs == mVideoTrimmer.getLeftValue()
                && mClipExtent.clipEndTimeMs == mVideoTrimmer.getRightValue())) {
            return;
        }
        mVdbRequestQueue.add(new ClipExtentUpdateRequest(mClip,
                mVideoTrimmer.getLeftValue(),
                mVideoTrimmer.getRightValue(),
                new VdbResponse.Listener<Integer>() {
                    @Override
                    public void onResponse(Integer response) {
                        if (response == 0) {
                            mBtnUpdateBookMark.getDrawable().setColorFilter(null);
                        }
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        Log.e("test", "", error);
                    }
                }
        ));

    }

    private void preparePlayback() {
        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_URL_TYPE, VdbClient.URL_TYPE_TS);
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_STREAM, VdbClient.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, mClip.getStartTime() + 33 * 1000);
        parameters.putInt(ClipPlaybackUrlExRequest.PARAMETER_CLIP_LENGTH_MS, 1000);

        ClipPlaybackUrlExRequest request = new ClipPlaybackUrlExRequest(mClip, parameters,
                new VdbResponse.Listener<PlaybackUrl>() {
                    @Override
                    public void onResponse(PlaybackUrl playbackUrl) {
                        Log.e("test", "URL: " + playbackUrl.url);
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        Log.e("test", "", error);
                    }
                });

        mVdbRequestQueue.add(request);
    }

    private void downloadTs(String url) {
        File dir = ImageUtils.getImageStorageDir(this, "Download");

    }




    private static class DownloadState {
        DownloadInfo downloadInfo;
        int stream;
        Clip.StreamInfo si;
        RawDataBlock.DownloadRawDataBlock mAccData;
        RawDataBlock.DownloadRawDataBlock mGpsData;
        RawDataBlock.DownloadRawDataBlock mObdData;
    }

    private DownloadState mDownloadState;

    private void startDownload(DownloadInfo downloadInfo, int stream, Clip.StreamInfo si) {
        mDownloadState = new DownloadState();
        mDownloadState.downloadInfo = downloadInfo;
        mDownloadState.stream = stream;
        mDownloadState.si = si;

        downloadVideo(mDownloadState);

    }





    private void downloadVideo(DownloadState ds) {
        DownloadInfo.DownloadStreamInfo dsi = ds.stream == 0 ? ds.downloadInfo.main : ds.downloadInfo.sub;

        RemuxerParams params = new RemuxerParams();
        // clip params
        params.setClipDate(dsi.clipDate); // TODO
        params.setClipTimeMs(dsi.clipTimeMs); // TODO
        params.setClipLength(dsi.lengthMs);
        // stream info
        params.setStreamVersion(ds.si.version);
        params.setVideoCoding(ds.si.video_coding);
        params.setVideoFrameRate(ds.si.video_framerate);
        params.setVideoWidth(ds.si.video_width);
        params.setVideoHeight(ds.si.video_height);
        params.setAudioCoding(ds.si.audio_coding);
        params.setAudioNumChannels(ds.si.audio_num_channels);
        params.setAudioSamplingFreq(ds.si.audio_sampling_freq);
        // download params
        params.setInputFile(dsi.url + ",0,-1;");
        params.setInputMime("ts");
        params.setOutputFormat("mp4");
        params.setPosterData(ds.downloadInfo.posterData);
        params.setGpsData(ds.mGpsData != null ? ds.mGpsData.ack_data : null);
        params.setAccData(ds.mAccData != null ? ds.mAccData.ack_data : null);
        params.setObdData(ds.mObdData != null ? ds.mObdData.ack_data : null);
        params.setDurationMs(dsi.lengthMs);
        //params.setDisableAudio(mEditor.bMuteAudio);
        //params.setAudioFileName(mEditor.audioFileName);
        params.setAudioFormat("mp3");
        // add to queue
        RemuxHelper.remux(this, params);

    }
}
