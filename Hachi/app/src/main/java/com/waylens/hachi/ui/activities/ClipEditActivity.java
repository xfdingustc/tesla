package com.waylens.hachi.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
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
import com.waylens.hachi.snipe.toolbox.RawDataBlockRequest;
import com.waylens.hachi.snipe.toolbox.RawDataDumpRequest;
import com.waylens.hachi.snipe.toolbox.RawDataRequest;
import com.waylens.hachi.utils.BufferUtils;
import com.waylens.hachi.utils.DataUploader;
import com.waylens.hachi.utils.ImageUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipExtent;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.vdb.RawData;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RemoteClip;
import com.waylens.hachi.views.VideoTrimmer;

import org.florescu.android.rangeseekbar.RangeSeekBar;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collections;

import butterknife.Bind;
import butterknife.OnClick;
import crs_svr.ProtocolConstMsg;

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
    SeekBar mSeekBar;

    @Bind(R.id.video_trimmer)
    VideoTrimmer mVideoTrimmer;

    @Bind(R.id.btn_update_bookmark)
    ImageButton mBtnUpdateBookMark;

    @Bind(R.id.range_seek_bar_container)
    View mRangeSeekBarContainer;

    @Bind(R.id.btn_share)
    View mBtnShare;

    private static VdtCamera mSharedCamera;
    private static Clip mSharedClip;

    private VdtCamera mVdtCamera;
    private Clip mClip;

    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mVdbImageLoader;

    int oldPosition;

    long startPlayTime;

    long oldStartValue;
    long oldEndValue;

    ClipExtent mClipExtent;

    public static void launch(Context context, VdtCamera vdtCamera, Clip clip) {
        Intent intent = new Intent(context, ClipEditActivity.class);
        mSharedCamera = vdtCamera;
        mSharedClip = clip;
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @OnClick(R.id.btnPlay)
    public void onBtnPlayClicked() {
        ClipPlaybackActivity.launch(this, mVdtCamera, mClip);
        //preparePlayback();
    }

    @OnClick(R.id.btn_share)
    public void shareVideo() {
        long clipTimeMs = 0;
        int lengthMs = mClip.clipLengthMs;
        final DataUploader dataUploader = new DataUploader("115.29.247.213", 42020, "qwertyuiopasdfgh");
        RawDataDumpRequest rawDataBlockRequest = new RawDataDumpRequest(mClip, clipTimeMs, lengthMs,
                RawDataBlock.RAW_DATA_ODB,
                new VdbResponse.Listener<byte[]>() {
                    @Override
                    public void onResponse(final byte[] response) {
                        Log.e("test", "Data size: " + response.length);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                dataUploader.uploadBinary(response, ProtocolConstMsg.VIDIT_RAW_OBD, "18", mClip.getVdbId());
                            }
                        }).start();

                    }
                }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Log.e("test", "", error);
            }
        });
        //mVdbRequestQueue.add(rawDataBlockRequest);
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
        mVdtCamera = mSharedCamera;
        mClip = mSharedClip;
        mVdbRequestQueue = Snipe.newRequestQueue(this, mVdtCamera.getVdbConnection());
        mVdbImageLoader = new VdbImageLoader(mVdbRequestQueue);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_clip_editor);
        final ClipPos clipPos = new ClipPos(mClip, mClip.getStartTime(), ClipPos.TYPE_POSTER, false);
        mVdbImageLoader.displayVdbImage(clipPos, mIvPreviewPicture);

        if (mClip.cid.type == RemoteClip.TYPE_BUFFERED) {
            initSeekBar();
            mSeekBar.setVisibility(View.VISIBLE);
            mRangeSeekBarContainer.setVisibility(View.GONE);
            //mBtnShare.setVisibility(View.GONE);
        } else {
            getClipExtent();
            mRangeSeekBarContainer.setVisibility(View.VISIBLE);
            mVideoTrimmer.setVisibility(View.INVISIBLE);
            mSeekBar.setVisibility(View.GONE);
            //mBtnShare.setVisibility(View.VISIBLE);
        }
    }

    private void initSeekBar() {
        final ClipPos clipPos = new ClipPos(mClip, mClip.getStartTime(), ClipPos.TYPE_POSTER, false);
        mSeekBar.setMax(mClip.clipLengthMs);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Log.e("test", String.format("old[%d], new[%d] ", oldPosition, i));
                if (Math.abs(oldPosition - i) > 1000) {
                    refreshThumbnail(mClip.getStartTime() + i, clipPos);
                    oldPosition = i;
                } else {
                    Log.e("test", "Progress: skipped");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                startPlayTime = seekBar.getProgress();
                Log.e("test", "startPlayTime: " + startPlayTime);
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

        mVideoTrimmer.setInitRangeValues(minValue, maxValue);
        mVideoTrimmer.setLeftValue(mClipExtent.clipStartTimeMs);
        mVideoTrimmer.setRightValue(mClipExtent.clipEndTimeMs);

        //mRangeSeekBar.setNotifyWhileDragging(true);
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
}
