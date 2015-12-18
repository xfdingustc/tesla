package com.waylens.hachi.ui.activities;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.orhanobut.logger.Logger;
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
import com.waylens.hachi.snipe.toolbox.RawDataBlockRequest;
import com.waylens.hachi.ui.services.download.DownloadIntentService;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipExtent;
import com.waylens.hachi.vdb.ClipFragment;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RemoteClip;
import com.waylens.hachi.views.VideoPlayerProgressBar;
import com.waylens.hachi.views.VideoTrimmer;
import com.waylens.hachi.views.dashboard.DashboardLayout;
import com.waylens.hachi.views.dashboard.adapters.BlockAdapter;
import com.waylens.mediatranscoder.MediaTranscoder;
import com.waylens.mediatranscoder.format.MediaFormatStrategyPresets;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2015/8/14.
 */
public class ClipEditActivity extends BaseActivity {
    private static final String TAG = ClipEditActivity.class.getSimpleName();

    private static final int MAX_EXTENSION = 1000 * 30;

    private static Clip mSharedClip;

    private Clip mClip;
    private ClipFragment mSelectedClipFragment;


    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mVdbImageLoader;

    private long oldPosition;

    private long oldStartValue;
    private long oldEndValue;

    private ClipExtent mClipExtent;
    private MediaPlayer mPlayer;

    private BroadcastReceiver mBroadcastReceiver;

    private String mDownloadedFilePath;

    private BlockAdapter mDashboardLayoutAdapter;


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
    ProgressBar mProgressBar;

    @Bind(R.id.dashboardLayout)
    DashboardLayout mDashboardLayout;

    @OnClick(R.id.btnPlay)
    public void onBtnPlayClicked() {
        ClipPlaybackActivity.launch(this, mClip);
        //preparePlayback();
    }

    @OnClick(R.id.btnDownload)
    public void onBtnDownload() {
        mSelectedClipFragment = getSelectedClipFragment();
        requestDownloadVideoClip();


    }

    @OnClick(R.id.btn_share)
    public void shareVideo() {
        playVideo();
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

        mDashboardLayoutAdapter = new BlockAdapter();
        mDashboardLayout.setAdapter(mDashboardLayoutAdapter);

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


    private void playVideo() {
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

        ClipPlaybackUrlRequest request = new ClipPlaybackUrlRequest(mClip.cid, parameters, new VdbResponse.Listener<PlaybackUrl>() {
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

        mVideoTrimmer.setBackgroundClip(mVdbImageLoader, mClip, minValue, maxValue, ViewUtils.dp2px(64, getResources()));
        mVideoTrimmer.setInitRangeValues(minValue, maxValue);
        mVideoTrimmer.setLeftValue(mClipExtent.clipStartTimeMs);
        mVideoTrimmer.setRightValue(mClipExtent.clipEndTimeMs);

        oldStartValue = mClipExtent.clipStartTimeMs;
        oldEndValue = mClipExtent.clipEndTimeMs;

        final ClipPos clipPos = new ClipPos(null,
            mClipExtent.originalCid,
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


    private ClipFragment getSelectedClipFragment() {
        long startTimeMs = mClip.getStartTimeMs();
        long endTimeMs = mClip.getStartTimeMs() + mClip.getDurationMs() / 256;
        ClipFragment clipFragment = new ClipFragment(mClip, startTimeMs, endTimeMs);
        return clipFragment;
    }

    private void preparePlayback() {
        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_URL_TYPE, VdbClient.URL_TYPE_TS);
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_STREAM, VdbClient.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, mClip.getStartTimeMs() + 33 * 1000);
        parameters.putInt(ClipPlaybackUrlExRequest.PARAMETER_CLIP_LENGTH_MS, 1000);

        ClipPlaybackUrlExRequest request = new ClipPlaybackUrlExRequest(mClip.cid, parameters,
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


    private void handleDownloadBroadcastIntent(Intent intent) {
        int event_what = intent.getIntExtra(DownloadIntentService.EVENT_EXTRA_WHAT, -1);
        switch (event_what) {
            case DownloadIntentService.EVENT_WHAT_DOWNLOAD_PROGRESS:
                int progress = intent.getIntExtra(DownloadIntentService
                    .EVENT_EXTRA_DOWNLOAD_PROGRESS, 0);
                mTvDownloadInfo.setText("Downloading: " + progress + "%");
                mProgressBar.setProgress(progress);
                break;
            case DownloadIntentService.EVENT_WHAT_DOWNLOAD_FINSHED:
                mDownloadedFilePath = intent.getStringExtra(DownloadIntentService
                    .EVENT_EXTRA_DOWNLOAD_FILE_PATH);
                mTvDownloadInfo.setText("Download finished: " + mDownloadedFilePath);
                mProgressBar.setProgress(0);
                requestDownloadRawData();
                break;
        }

    }


    private void requestDownloadVideoClip() {

        DownloadIntentService.startDownload(this, mSelectedClipFragment);
    }

    private void requestDownloadRawData() {
        Bundle params = new Bundle();
        params.putInt(RawDataBlockRequest.PARAM_DATA_TYPE, RawDataBlock.RAW_DATA_ACC);
        params.putLong(RawDataBlockRequest.PARAM_CLIP_TIME, mSelectedClipFragment.getStartTimeMs());
        params.putInt(RawDataBlockRequest.PARAM_CLIP_LENGTH, mSelectedClipFragment.getDurationMs());

        RawDataBlockRequest accRequest = new RawDataBlockRequest(mSelectedClipFragment.getClip().cid, params,
            new VdbResponse.Listener<RawDataBlock>() {
                @Override
                public void onResponse(RawDataBlock response) {
                    mDashboardLayoutAdapter.setAccDataBlock(response);
                    mTvDownloadInfo.setText("ACC data is downloaded!!!");

                }
            }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(accRequest);

        params = new Bundle();
        params.putInt(RawDataBlockRequest.PARAM_DATA_TYPE, RawDataBlock.RAW_DATA_GPS);
        params.putLong(RawDataBlockRequest.PARAM_CLIP_TIME, mSelectedClipFragment.getStartTimeMs());
        params.putInt(RawDataBlockRequest.PARAM_CLIP_LENGTH, mSelectedClipFragment.getDurationMs());
        RawDataBlockRequest gpsRequest = new RawDataBlockRequest(mSelectedClipFragment.getClip().cid,
            params, new VdbResponse.Listener<RawDataBlock>() {
            @Override
            public void onResponse(RawDataBlock response) {
                mTvDownloadInfo.setText("GPS data is downloaded!!!!");
                mDashboardLayoutAdapter.setGpsDataBlock(response);
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(gpsRequest);

        params = new Bundle();
        params.putInt(RawDataBlockRequest.PARAM_DATA_TYPE, RawDataBlock.RAW_DATA_ODB);
        params.putLong(RawDataBlockRequest.PARAM_CLIP_TIME, mSelectedClipFragment.getStartTimeMs());
        params.putInt(RawDataBlockRequest.PARAM_CLIP_LENGTH, mSelectedClipFragment.getDurationMs());
        RawDataBlockRequest obdRequest = new RawDataBlockRequest(mSelectedClipFragment.getClip().cid, params,
            new VdbResponse.Listener<RawDataBlock>() {
                @Override
                public void onResponse(RawDataBlock response) {
                    mTvDownloadInfo.setText("OBD data is downloaded!!!!");
                    mDashboardLayoutAdapter.setObdDataBlock(response);
                    startTranscodeTask();
                }
            }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(obdRequest);
    }

    private void startTranscodeTask() {
        mTvDownloadInfo.setText("Start transcoding task");

        beginTranscode();
    }

    private void beginTranscode() {
        final File file;

        try {
            file = File.createTempFile("transcode_test", ".mp4", getExternalFilesDir(null));
        } catch (IOException e) {
            Logger.t(TAG).e("Failed to create temporary file.");
            Toast.makeText(this, "Failed to create temporary file.", Toast.LENGTH_LONG).show();
            return;
        }

        ContentResolver resolver = getContentResolver();
        final ParcelFileDescriptor parcelFileDescriptor;

        Uri fileUri = Uri.fromFile(new File(mDownloadedFilePath));
        try {
            parcelFileDescriptor = resolver.openFileDescriptor(fileUri, "r");
        } catch (FileNotFoundException e) {
            //Log.w("Could not open '" + data.getDataString() + "'", e);
            Toast.makeText(this, "File not found.", Toast.LENGTH_LONG).show();
            return;
        }
        final FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

        mProgressBar.setMax(1000);
        final long startTime = SystemClock.uptimeMillis();

        MediaTranscoder.Listener listener = new MediaTranscoder.Listener() {
            @Override
            public void onTranscodeProgress(double progress, final long currentTimeMs) {
                if (progress < 0) {
                    mProgressBar.setIndeterminate(true);
                } else {
                    mProgressBar.setIndeterminate(false);
                    mProgressBar.setProgress((int) Math.round(progress * 1000));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDashboardLayout.update(currentTimeMs);
                        }
                    });
                }
            }

            @Override
            public void onTranscodeCompleted() {
                Logger.t(TAG).d("transcoding took " + (SystemClock.uptimeMillis() - startTime) + "ms");
                Toast.makeText(ClipEditActivity.this, "transcoded file placed on " + file, Toast
                    .LENGTH_LONG)
                    .show();

                mProgressBar.setIndeterminate(false);
                mProgressBar.setProgress(1000);
                startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(file), "video/mp4"));
                try {
                    parcelFileDescriptor.close();
                } catch (IOException e) {
                    Log.w("Error while closing", e);
                }
            }

            @Override
            public void onTranscodeFailed(Exception exception) {
                mProgressBar.setIndeterminate(false);
                mProgressBar.setProgress(0);

                Toast.makeText(ClipEditActivity.this, "Transcoder error occurred.", Toast.LENGTH_LONG).show();
                mTvDownloadInfo.setText("Transcoder error");
                try {
                    parcelFileDescriptor.close();
                } catch (IOException e) {
                    Log.w("Error while closing", e);
                }
            }
        };
        Logger.t(TAG).d("transcoding into " + file);
        MediaTranscoder.getInstance().transcodeVideo(fileDescriptor, file.getAbsolutePath(),
            MediaFormatStrategyPresets.createAndroid720pStrategy(), listener, mDashboardLayout);
    }


}
