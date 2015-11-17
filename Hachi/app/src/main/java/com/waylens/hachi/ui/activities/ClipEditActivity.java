package com.waylens.hachi.ui.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.transee.vdb.VdbClient;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipExtentGetRequest;
import com.waylens.hachi.snipe.toolbox.ClipExtentUpdateRequest;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlExRequest;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlRequest;
import com.waylens.hachi.ui.services.download.DownloadIntentService;
import com.waylens.hachi.utils.ImageUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipExtent;
import com.waylens.hachi.vdb.ClipFragment;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.PlaybackUrl;
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

    @Bind(R.id.tvDownloadInfo)
    TextView mTvDownloadInfo;

    @Bind(R.id.downloadProgressBar)
    ProgressBar mDownloadProgressBar;


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

    private BroadcastReceiver mBroadcastReceiver;


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
        long startTimeMs = mClip.getStartTimeMs();
        long endTimeMs = mClip.getStartTimeMs() + mClip.getDurationMs() / 4;
        ClipFragment clipFragment = new ClipFragment(mClip, startTimeMs, endTimeMs);
        DownloadIntentService.startDownload(this, clipFragment);

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
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, mClip.getStartTimeMs());

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
            fragment.put("beginTime", (int) mClip.getStartTimeMs());
            fragment.put("offset", 0);
            fragment.put("duration", mClip.getDurationMs());
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
        initBroadcastReceiver();
    }



    private void initViews() {
        setContentView(R.layout.activity_clip_editor);
        final ClipPos clipPos = new ClipPos(mClip, mClip.getStartTimeMs(), ClipPos.TYPE_POSTER, false);
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
        final ClipPos clipPos = new ClipPos(mClip, mClip.getStartTimeMs(), ClipPos.TYPE_POSTER, false);
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
                    refreshThumbnail(mClip.getStartTimeMs() + progress, clipPos);
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

    private void initBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter(DownloadIntentService
            .INTENT_FILTER_DOWNLOAD_INTENT_SERVICE);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleDownloadBroadcastIntent(intent);
            }
        };

        registerReceiver(mBroadcastReceiver, intentFilter);
    }



    private void refreshThumbnail(long clipTimeMs, ClipPos clipPos) {
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
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, mClip.getStartTimeMs() + 33 * 1000);
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



    private void handleDownloadBroadcastIntent(Intent intent) {
        int event_what = intent.getIntExtra(DownloadIntentService.EVENT_EXTRA_WHAT, -1);
        switch (event_what) {
            case DownloadIntentService.EVENT_WHAT_DOWNLOAD_PROGRESS:
                int progress = intent.getIntExtra(DownloadIntentService
                    .EVENT_EXTRA_DOWNLOAD_PROGRESS, 0);
                mTvDownloadInfo.setText("Downloading: " + progress + "%");
                mDownloadProgressBar.setProgress(progress);
                break;
            case DownloadIntentService.EVENT_WHAT_DOWNLOAD_FINSHED:
                mTvDownloadInfo.setText("Download finished");
                mDownloadProgressBar.setProgress(0);
                break;
        }

    }









}
