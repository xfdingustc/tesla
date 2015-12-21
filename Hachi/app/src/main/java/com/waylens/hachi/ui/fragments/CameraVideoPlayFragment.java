package com.waylens.hachi.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.annotations.SpriteFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;
import com.orhanobut.logger.Logger;
import com.transee.common.GPSRawData;
import com.transee.vdb.VdbClient;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlRequest;
import com.waylens.hachi.snipe.toolbox.ClipUploadUrlRequest;
import com.waylens.hachi.snipe.toolbox.RawDataBlockRequest;
import com.waylens.hachi.ui.views.OnViewDragListener;
import com.waylens.hachi.utils.DataUploader;
import com.waylens.hachi.utils.ImageUtils;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.utils.VolleyUtil;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipFragment;
import com.waylens.hachi.vdb.OBDData;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;
import com.waylens.hachi.vdb.UploadUrl;
import com.waylens.hachi.views.GaugeView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import crs_svr.ProtocolConstMsg;

/**
 * Created by Richard on 11/4/15.
 */
public class CameraVideoPlayFragment extends VideoPlayFragment {
    private static final String TAG = CameraVideoPlayFragment.class.getSimpleName();

    private VdbRequestQueue mVdbRequestQueue;
    private Clip mClip;

    SparseArray<RawDataBlock> mTypedRawData = new SparseArray<>();
    SparseIntArray mTypedState = new SparseIntArray();
    SparseIntArray mTypedPosition = new SparseIntArray();
    GaugeView mObdView;
    MapView mMapView;

    PlaybackUrl mPlaybackUrl;
    long mInitPosition;

    private MarkerOptions mMarkerOptions;
    private PolylineOptions mPolylineOptions;
    private boolean mIsReplay;

    private RequestQueue mRequestQueue;

    public static CameraVideoPlayFragment newInstance(VdbRequestQueue vdbRequestQueue,
                                                      Clip clip,
                                                      OnViewDragListener listener) {
        Bundle args = new Bundle();
        CameraVideoPlayFragment fragment = new CameraVideoPlayFragment();
        fragment.setArguments(args);
        fragment.mVdbRequestQueue = vdbRequestQueue;
        fragment.mClip = clip;
        fragment.mTypedState.put(RawDataBlock.RAW_DATA_ODB, RAW_DATA_STATE_UNKNOWN);
        fragment.mTypedState.put(RawDataBlock.RAW_DATA_ACC, RAW_DATA_STATE_UNKNOWN);
        fragment.mTypedState.put(RawDataBlock.RAW_DATA_GPS, RAW_DATA_STATE_UNKNOWN);
        fragment.mDragListener = listener;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRequestQueue = VolleyUtil.newVolleyRequestQueue(getActivity());
        mRequestQueue.start();

    }

    @Override
    public void onStart() {
        super.onStart();

        if (!isRawDataReady()) {
            loadRawData();
        } else {
            loadPlayURL();
        }

        //createMoment();
        //getUploadUrl_Video(null);
        if (mMapView != null) {
            mMapView.onStart();
        }
    }

    @Override
    public void onStop() {
        if (mMapView != null) {
            mMapView.onStop();
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        mVdbRequestQueue.cancelAll(REQUEST_TAG);
        super.onDestroyView();
    }

    protected void setProgress(int currentPosition, int duration) {
        if (mPlaybackUrl.realTimeMs != 0
                && mInitPosition == 0
                && currentPosition != 0
                && Math.abs(mPlaybackUrl.realTimeMs - currentPosition) < 200) {
            mInitPosition = mPlaybackUrl.realTimeMs;
            Log.e("test", "setProgress - deviation: " + Math.abs(mPlaybackUrl.realTimeMs - currentPosition));
        }
        //Log.e("test", "setProgress - duration: " + duration + "; position: " + position + "; real: "
        //        + mPlaybackUrl.realTimeMs + "; duration2: " + mPlaybackUrl.lengthMs);
        int position = currentPosition;
        if (duration > 0) {
            //Log.e("test", "setProgress - position: " + position + "; real: "
            //        + mPlaybackUrl.realTimeMs + "; duration2: " + mPlaybackUrl.lengthMs);
            if (mInitPosition == 0) {
                position = currentPosition + (int) mPlaybackUrl.realTimeMs;
            }
        }

        displayOverlay(position);
        if (mProgressListener != null) {
            mProgressListener.onProgress(position, duration);
        }
    }

    @Override
    protected void displayOverlay(int position) {
        if (mObdView == null) {
            return;
        }
        
        RawDataItem obd = getRawData(RawDataBlock.RAW_DATA_ODB, position);
        if (obd != null && obd.object != null) {
            mObdView.setSpeed(((OBDData) obd.object).speed);
            mObdView.setTargetValue(((OBDData) obd.object).rpm / 1000.0f);
        } else {
            Logger.t(TAG).e("Position: " + position + "; mOBDPosition: " + mTypedPosition
                    .get(RawDataBlock.RAW_DATA_ODB));
        }

        RawDataItem gps = getRawData(RawDataBlock.RAW_DATA_GPS, position);
        if (gps != null) {
            GPSRawData gpsRawData = (GPSRawData) gps.object;
            mMarkerOptions.getMarker().remove();
            LatLng point = new LatLng(gpsRawData.coord.lat_orig, gpsRawData.coord.lng_orig);
            mMarkerOptions.position(point);
            mMapView.addMarker(mMarkerOptions);
            mMapView.setCenterCoordinate(point);
            mMapView.setDirection(-gpsRawData.track);
        }
    }

    RawDataItem getRawData(int dataType, int position) {
        RawDataBlock raw = mTypedRawData.get(dataType);
        int pos = mTypedPosition.get(dataType);
        RawDataItem rawDataItem = null;
        while (pos < raw.dataSize.length) {
            RawDataItem tmp = raw.getRawDataItem(pos);
            long timeOffsetMs = raw.timeOffsetMs[pos] + raw.header.mRequestedTimeMs;
            if (timeOffsetMs == position) {
                rawDataItem = tmp;
                mTypedPosition.put(dataType, pos);
                break;
            } else if (timeOffsetMs < position) {
                rawDataItem = tmp;
                mTypedPosition.put(dataType, pos);
                pos++;
            } else if (timeOffsetMs > position) {
                break;
            }
        }
        return rawDataItem;
    }

    @Override
    protected void onPlayCompletion() {
        mTypedPosition.clear();
        mIsReplay = true;
    }

    boolean isRawDataReady() {
        return mTypedState.get(RawDataBlock.RAW_DATA_ODB) == RAW_DATA_STATE_READY
                && mTypedState.get(RawDataBlock.RAW_DATA_ACC) == RAW_DATA_STATE_READY
                && mTypedState.get(RawDataBlock.RAW_DATA_GPS) == RAW_DATA_STATE_READY;
    }

    void loadRawData() {
        mProgressLoading.setVisibility(View.VISIBLE);
        if (mTypedState.get(RawDataBlock.RAW_DATA_ODB) != RAW_DATA_STATE_READY) {
            loadRawData(RawDataBlock.RAW_DATA_ODB);
        }

        if (mTypedState.get(RawDataBlock.RAW_DATA_ACC) != RAW_DATA_STATE_READY) {
            loadRawData(RawDataBlock.RAW_DATA_ACC);
        }
        if (mTypedState.get(RawDataBlock.RAW_DATA_GPS) != RAW_DATA_STATE_READY) {
            loadRawData(RawDataBlock.RAW_DATA_GPS);
        }
    }

    void loadRawData(final int dataType) {
        if (mClip == null || mVdbRequestQueue == null) {
            mRawDataState = RAW_DATA_STATE_ERROR;
            return;
        }

        Logger.t(TAG).d("DataType[1]: " + dataType);

        ClipFragment clipFragment = new ClipFragment(mClip);
        Bundle params = new Bundle();
        params.putInt(RawDataBlockRequest.PARAM_DATA_TYPE, dataType);
        params.putLong(RawDataBlockRequest.PARAM_CLIP_TIME, clipFragment.getStartTimeMs());
        params.putInt(RawDataBlockRequest.PARAM_CLIP_LENGTH, clipFragment.getDurationMs());

        RawDataBlockRequest obdRequest = new RawDataBlockRequest(clipFragment.getClip().cid, params,
                new VdbResponse.Listener<RawDataBlock>() {
                    @Override
                    public void onResponse(RawDataBlock response) {
                        Logger.t(TAG).d("resoponse datatype: " + dataType);
                        mTypedRawData.put(dataType, response);
                        mTypedState.put(dataType, RAW_DATA_STATE_READY);
                        onLoadRawDataFinished();
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        mTypedState.put(dataType, RAW_DATA_STATE_ERROR);
                        onLoadRawDataFinished();
                        Logger.t(TAG).d("error response:");
                    }
                });
        mVdbRequestQueue.add(obdRequest.setTag(REQUEST_TAG));
    }

    void onLoadRawDataFinished() {
        if (mTypedState.get(RawDataBlock.RAW_DATA_ODB) == RAW_DATA_STATE_UNKNOWN
                || mTypedState.get(RawDataBlock.RAW_DATA_ACC) == RAW_DATA_STATE_UNKNOWN
                || mTypedState.get(RawDataBlock.RAW_DATA_GPS) == RAW_DATA_STATE_UNKNOWN) {
            return;
        }
        mRawDataState = RAW_DATA_STATE_READY;
        loadPlayURL();

        if (mTypedRawData.get(RawDataBlock.RAW_DATA_ODB) != null && mObdView == null) {
            mObdView = new GaugeView(getActivity());
            int defaultSize = ViewUtils.dp2px(64, getResources());
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(defaultSize, defaultSize);
            params.gravity = Gravity.BOTTOM | Gravity.END;
            mVideoContainer.addView(mObdView, params);
        }

        if (mTypedRawData.get(RawDataBlock.RAW_DATA_GPS) != null && mMapView == null) {
            initMapView();
        }
    }

    private void initMapView() {
        mMapView = new MapView(getActivity(), Constants.MAP_BOX_ACCESS_TOKEN);
        mMapView.setStyleUrl(Style.DARK);
        mMapView.setZoomLevel(14);
        mMapView.setLogoVisibility(View.GONE);
        mMapView.setCompassEnabled(false);
        mMapView.onCreate(null);
        GPSRawData firstGPS = (GPSRawData) mTypedRawData.get(RawDataBlock.RAW_DATA_GPS).getRawDataItem(0).object;
        SpriteFactory spriteFactory = new SpriteFactory(mMapView);
        LatLng firstPoint = new LatLng(firstGPS.coord.lat_orig, firstGPS.coord.lng_orig);
        mMarkerOptions = new MarkerOptions().position(firstPoint)
                .icon(spriteFactory.fromResource(R.drawable.map_car_inner_red_triangle));
        mMapView.addMarker(mMarkerOptions);
        mPolylineOptions = new PolylineOptions().color(Color.rgb(252, 219, 12)).width(3).add(firstPoint);
        mMapView.setCenterCoordinate(firstPoint);
        mMapView.setDirection(firstGPS.track);
        mMapView.addPolyline(mPolylineOptions);

        int defaultSize = ViewUtils.dp2px(96, getResources());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(defaultSize, defaultSize);
        mVideoContainer.addView(mMapView, params);
        buildFullPath();
    }

    void buildFullPath() {
        RawDataBlock raw = mTypedRawData.get(RawDataBlock.RAW_DATA_GPS);
        for (int i = 0; i < raw.dataSize.length; i++) {
            RawDataItem item = raw.getRawDataItem(i);
            GPSRawData gpsRawData = (GPSRawData) item.object;
            LatLng point = new LatLng(gpsRawData.coord.lat_orig, gpsRawData.coord.lng_orig);
            mPolylineOptions.add(point);
        }
        mMapView.addPolyline(mPolylineOptions);
    }

    void loadPlayURL() {
        if (mProgressLoading.getVisibility() != View.VISIBLE) {
            mProgressLoading.setVisibility(View.VISIBLE);
        }
        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_URL_TYPE, VdbClient.URL_TYPE_HLS);
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_STREAM, VdbClient.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, mClip.getStartTimeMs());

        ClipPlaybackUrlRequest request = new ClipPlaybackUrlRequest(mClip.cid, parameters, new VdbResponse.Listener<PlaybackUrl>() {
            @Override
            public void onResponse(PlaybackUrl playbackUrl) {
                mPlaybackUrl = playbackUrl;
                setSource(playbackUrl.url);
                mProgressLoading.setVisibility(View.GONE);
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                mProgressLoading.setVisibility(View.GONE);
                Log.e("test", "", error);
            }
        });

        mVdbRequestQueue.add(request.setTag(REQUEST_TAG));
    }

    void getUploadUrl_Video(final JSONObject momentInfo) {
        Bundle parameters = new Bundle();
        parameters.putBoolean(ClipUploadUrlRequest.PARAM_IS_PLAY_LIST, false);
        parameters.putLong(ClipUploadUrlRequest.PARAM_CLIP_TIME_MS, mClip.getStartTimeMs());
        parameters.putInt(ClipUploadUrlRequest.PARAM_CLIP_LENGTH_MS, mClip.getDurationMs());
        parameters.putInt(ClipUploadUrlRequest.PARAM_UPLOAD_OPT, VdbCommand.Factory.UPLOAD_GET_V1);

        ClipUploadUrlRequest request = new ClipUploadUrlRequest(mClip, parameters,
                new VdbResponse.Listener<UploadUrl>() {
                    @Override
                    public void onResponse(UploadUrl response) {
                        readDataVideo(response.url, momentInfo);
                        //saveToSdcardVideo(response.url);
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

    void getUploadUrl_Raw(final JSONObject momentInfo) {
        Bundle parameters = new Bundle();
        parameters.putBoolean(ClipUploadUrlRequest.PARAM_IS_PLAY_LIST, false);
        parameters.putLong(ClipUploadUrlRequest.PARAM_CLIP_TIME_MS, mClip.getStartTimeMs());
        parameters.putInt(ClipUploadUrlRequest.PARAM_CLIP_LENGTH_MS, mClip.getDurationMs());
        parameters.putInt(ClipUploadUrlRequest.PARAM_UPLOAD_OPT, VdbCommand.Factory.UPLOAD_GET_RAW);

        ClipUploadUrlRequest request = new ClipUploadUrlRequest(mClip, parameters,
                new VdbResponse.Listener<UploadUrl>() {
                    @Override
                    public void onResponse(UploadUrl response) {
                        readDataRaw(response.url, momentInfo);
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

    String findToken(JSONObject momentInfo, String type) {
        JSONArray jsonArray = momentInfo.optJSONArray("uploadData");
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.optJSONObject(i);
            if (type.equals(jsonObject.optString("dataType"))) {
                return jsonObject.optString("uploadToken");
            }
        }
        return null;
    }

    String findGuid(JSONObject momentInfo, String type) {
        JSONArray jsonArray = momentInfo.optJSONArray("uploadData");
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.optJSONObject(i);
            if (type.equals(jsonObject.optString("dataType"))) {
                return jsonObject.optString("guid");
            }
        }
        return null;
    }

    private void readDataRaw(final String urlString, final JSONObject momentInfo) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                try {
                    URL url = new URL(urlString);
                    URLConnection conn = url.openConnection();
                    inputStream = conn.getInputStream();
                    Log.e("test", "ContentLength: " + conn.getContentLength());

                    JSONObject uploadServer = momentInfo.optJSONObject("uploadServer");
                    String ip = uploadServer.optString("ip");
                    int port = uploadServer.optInt("port");
                    String privateKey = uploadServer.optString("privateKey");
                    String token = findToken(momentInfo, "raw");
                    String guid = findGuid(momentInfo, "raw");
                    DataUploader uploader = new DataUploader(ip, port, privateKey);
                    //uploader.setUploaderListener(uploadListener);
                    uploader.uploadStream(inputStream, conn.getContentLength(), ProtocolConstMsg.VIDIT_RAW_DATA, token, guid);
                    Log.e("test", "===================== Raw done!");
                } catch (Exception e) {
                    Log.e("test", "", e);
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            Log.e("test", "", e);
                        }
                    }
                }
            }
        }).start();
    }

    private void readDataVideo(final String urlString, final JSONObject momentInfo) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                try {
                    URL url = new URL(urlString);
                    URLConnection conn = url.openConnection();
                    inputStream = conn.getInputStream();
                    Log.e("test", "ContentLength: " + conn.getContentLength());

                    JSONObject uploadServer = momentInfo.optJSONObject("uploadServer");
                    String ip = uploadServer.optString("ip");
                    int port = uploadServer.optInt("port");
                    String privateKey = uploadServer.optString("privateKey");
                    String token = findToken(momentInfo, "video");
                    String guid = findGuid(momentInfo, "video");


                    /*
                    String ip = "192.168.20.160";
                    int port = 35020;
                    String privateKey = "qwertyuiopasdfgh";
                    String token = "220";
                    String guid = "53f37022-43ba-404b-a5f8-389c823c1e0b";
                    */

                    DataUploader uploader = new DataUploader(ip, port, privateKey);
                    //uploader.setUploaderListener(uploadListener);
                    uploader.uploadStream(inputStream, conn.getContentLength(), ProtocolConstMsg.VIDIT_VIDEO_DATA, token, guid);
                    Log.e("test", "================= Video done!");
                    //getUploadUrl_Raw(momentInfo);
                } catch (Exception e) {
                    Log.e("test", "", e);
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            Log.e("test", "", e);
                        }
                    }
                }
            }
        }).start();
    }

    void createMoment() {
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
                        Log.e("test", "response: " + response);
                        getUploadUrl_Video(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("test", "", error);
                    }
                }));
    }

    private void saveToSdcardVideo(final String urlString) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                FileOutputStream outputStream = null;
                try {
                    URL url = new URL(urlString);
                    URLConnection conn = url.openConnection();
                    inputStream = conn.getInputStream();
                    Log.e("test", "ContentLength: " + conn.getContentLength());
                    File dir = ImageUtils.getImageStorageDir(getActivity(), "upload");
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    outputStream = new FileOutputStream(new File(dir, "speedtest.clip"));
                    byte[] buffer = new byte[2 * 1024];
                    int len;
                    long s = System.currentTimeMillis();
                    while ((len = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, len);
                    }
                    long e = System.currentTimeMillis();
                    Log.e("test", "================= Video done after: " + (e - s));
                } catch (Exception e) {
                    Log.e("test", "", e);
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            Log.e("test", "", e);
                        }
                    }
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            Log.e("test", "", e);
                        }
                    }
                }
            }
        }).start();
    }

    private void sendVideoFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                try {
                    File dir = ImageUtils.getImageStorageDir(getActivity(), "upload");
                    File file = new File(dir, "speedtest.clip");
                    inputStream = new FileInputStream(file);
                    Log.e("test", "ContentLength: " + file.length());

                    String ip = "192.168.20.160";
                    int port = 35020;
                    String privateKey = "qwertyuiopasdfgh";
                    String token = "217";
                    String guid = "f3e6be46-f22a-454a-8e3c-8133c7876927";
                    DataUploader uploader = new DataUploader(ip, port, privateKey);
                    uploader.setUploaderListener(uploadListener);
                    long s = System.currentTimeMillis();
                    uploader.uploadStream(inputStream, (int) file.length(), ProtocolConstMsg.VIDIT_VIDEO_DATA, token, guid);
                    long e = System.currentTimeMillis();
                    Log.e("test", "================= Video done after: " + (e - s));
                } catch (Exception e) {
                    Log.e("test", "", e);
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            Log.e("test", "", e);
                        }
                    }
                }
            }
        }).start();
    }

    DataUploader.UploadListener uploadListener = new DataUploader.UploadListener() {
        @Override
        public void onUploadStarted() {
            Log.e("test", "onUploadStarted");
        }

        @Override
        public void onUploadProgress(float progress) {
            Log.e("test", "onUploadProgress: " + progress);
        }

        @Override
        public void onUploadFinished() {
            Log.e("test", "onUploadFinished");
        }

        @Override
        public void onUploadError(String error) {
            Log.e("test", "onUploadError: " + error);
        }
    };
}
