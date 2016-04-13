package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ProgressBar;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.dao.RawDataItemDao;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.snipe.toolbox.RawDataBlockRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipFragment;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.rawdata.GpsData;
import com.waylens.hachi.vdb.rawdata.IioData;
import com.waylens.hachi.vdb.rawdata.ObdData;
import com.waylens.hachi.vdb.rawdata.RawData;
import com.waylens.hachi.vdb.rawdata.RawDataBlock;
import com.waylens.hachi.vdb.rawdata.RawDataItem;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/4/11.
 */
public class SmartRemixActivity extends BaseActivity {
    private static final String TAG = SmartRemixActivity.class.getSimpleName();
    private ClipSet mAllClipSet;


    private VdtCamera mVdtCamera;
    private VdbRequestQueue mVdbRequestQueue;

    private static final String IS_PC_SERVER = "isPcServer";
    private static final String SSID = "ssid";
    private static final String HOST_STRING = "hostString";
    private int mCurrentLoadingIndex;

    private RawDataItemDao mRawDataItemDao;

    private List<RawDataBlockAll> mRawDataBlockList = new ArrayList<>();
    private List<List<RawData>> mRawDataList = new ArrayList<>();

    public static void launch(Activity activity, VdtCamera camera) {
        Intent intent = new Intent(activity, SmartRemixActivity.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean(IS_PC_SERVER, camera.isPcServer());
        bundle.putString(SSID, camera.getSSID());
        bundle.putString(HOST_STRING, camera.getHostString());
        intent.putExtras(bundle);
        activity.startActivity(intent);
    }

    @Bind(R.id.rawdata)
    ProgressBar mLoadingProgressBar;

    @OnClick(R.id.btnCreateRemix)
    public void onBtnSmartRemixClicked() {
        startLoadingRawData();
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        mVdtCamera = getCameraFromIntent(getIntent().getExtras());
        mVdbRequestQueue = Snipe.newRequestQueue(this, mVdtCamera);
        mRawDataItemDao = new RawDataItemDao("rawdata.db");
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_smart_remix);
    }

    @Override
    public void setupToolbar() {
        mToolbar.setTitle(R.string.smart_remix);
        mToolbar.setNavigationIcon(R.drawable.navbar_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        super.setupToolbar();
    }

    private void startLoadingRawData() {
        doGetBookmarkClips();
    }

    private void doGetBookmarkClips() {
        ClipSetExRequest request = new ClipSetExRequest(Clip.TYPE_MARKED, ClipSetExRequest.FLAG_CLIP_EXTRA,
            new VdbResponse.Listener<ClipSet>() {
                @Override
                public void onResponse(ClipSet response) {
                    mAllClipSet = response;
                    doGetBufferedClips();
                }
            }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(request);
    }

    private void doGetBufferedClips() {
        ClipSetExRequest request = new ClipSetExRequest(Clip.TYPE_BUFFERED, ClipSetExRequest.FLAG_CLIP_EXTRA,
            new VdbResponse.Listener<ClipSet>() {
                @Override
                public void onResponse(ClipSet response) {
                    for (int i = 0; i < response.getCount(); i++) {
                        Clip clip = response.getClip(i);
                        mAllClipSet.addClip(clip);
                    }

                    doLoadRawData();

                }
            }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });

        mVdbRequestQueue.add(request);
    }

    private void doLoadRawData() {
        mCurrentLoadingIndex = 0;
        mLoadingProgressBar.setMax(mAllClipSet.getCount());
        for (int i = 0; i < mAllClipSet.getCount(); i++) {
            RawDataBlockAll rawDataBlockAll = new RawDataBlockAll();
            mRawDataBlockList.add(rawDataBlockAll);
        }

        loadRawData(RawDataItem.DATA_TYPE_OBD);

    }

    private void loadRawData(final int dataType) {

        Logger.t(TAG).d("clipset count: " + mAllClipSet.getCount() + " loading index: " + mCurrentLoadingIndex);
        Clip clip = mAllClipSet.getClip(mCurrentLoadingIndex);

        ClipFragment clipFragment = new ClipFragment(clip);
        Bundle params = new Bundle();
        params.putInt(RawDataBlockRequest.PARAM_DATA_TYPE, dataType);
        params.putLong(RawDataBlockRequest.PARAM_CLIP_TIME, clipFragment.getStartTimeMs());
        params.putInt(RawDataBlockRequest.PARAM_CLIP_LENGTH, clipFragment.getDurationMs());

        RawDataBlockRequest obdRequest = new RawDataBlockRequest(clipFragment.getClip().cid, params,
            new VdbResponse.Listener<RawDataBlock>() {
                @Override
                public void onResponse(RawDataBlock response) {
                    onLoadRawDataFinished(dataType, response);
                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    onLoadRawDataFinished(dataType, null);
                }
            });
        mVdbRequestQueue.add(obdRequest);
    }

    private void onLoadRawDataFinished(int dataType, RawDataBlock block) {
        RawDataBlockAll rawDataBlockAll = mRawDataBlockList.get(mCurrentLoadingIndex);

        Logger.t(TAG).d("Load finished type: " + dataType + " index: " + mCurrentLoadingIndex);

        switch (dataType) {
            case RawDataItem.DATA_TYPE_OBD:
                rawDataBlockAll.obdDataBlock = block;
                loadRawData(RawDataItem.DATA_TYPE_IIO);
                break;
            case RawDataItem.DATA_TYPE_IIO:
                rawDataBlockAll.iioDataBlock = block;
//                saveAccRawData(block);
                loadRawData(RawDataItem.DATA_TYPE_GPS);
                break;
            case RawDataItem.DATA_TYPE_GPS:
                rawDataBlockAll.gpsDataBlock = block;

                List<RawData> rawDataList = remixAllRawData(rawDataBlockAll);

                mRawDataList.add(rawDataList);

                if (++mCurrentLoadingIndex == mAllClipSet.getCount()) {
                    Logger.t(TAG).d("load finished!!!!!");
                    analyseRawData();

                } else {
                    loadRawData(RawDataItem.DATA_TYPE_OBD);
                }
                mLoadingProgressBar.setProgress(mCurrentLoadingIndex);


                break;
        }
    }

    private List<ClipFragment> analyseRawData() {

        List<ClipFragment> clipFragmentList = new ArrayList<>();


//        for (List<RawData> rawDataList : mRawDataList) {
        for (int i = 0; i < mRawDataList.size(); i++) {

            boolean startFound = false;
            List<RawData> rawDataList = mRawDataList.get(i);

            Clip clip = mAllClipSet.getClip(i);
            ClipFragment clipFragment = null;
            for (RawData rawData : rawDataList) {

                if (!startFound && ifMeetThreshold(rawData)) {
//                    Logger.t(TAG).d("start Hit!!!!!! " + rawData.getObdData().speed + " pts: " + rawData.getPts());
                    startFound = true;
                    clipFragment = new ClipFragment(clip);
                    clipFragment.setStartTime(rawData.getPts());
//                    Logger.t(TAG).d("set start: " + clipFragment.toString());
                }

                if (startFound && !ifMeetThreshold(rawData)) {
//                    Logger.t(TAG).d("end Hit!!!!!! "  );
                    startFound = false;
                    clipFragment.setEndTime(rawData.getPts());
//                    Logger.t(TAG).d("Found one ClipFragment: " + clipFragment.toString());
                    clipFragmentList.add(clipFragment);
                }
            }
        }

        Logger.t(TAG).d("Found clip Fragment: " + clipFragmentList.size());

        return clipFragmentList;
    }


    private boolean ifMeetThreshold(RawData rawData) {
        if (rawData.getObdData() != null && rawData.getObdData().speed >= 90) {
            return true;
        } else {
            return false;
        }
    }

    private List<RawData> remixAllRawData(RawDataBlockAll rawDataBlock) {
        List<RawDataItem> iioItemList = rawDataBlock.iioDataBlock == null ? null : rawDataBlock.iioDataBlock.getItemList();
        List<RawDataItem> gpsItemList = rawDataBlock.gpsDataBlock == null ? null : rawDataBlock.gpsDataBlock.getItemList();
        List<RawDataItem> obdItemList = rawDataBlock.obdDataBlock == null ? null : rawDataBlock.obdDataBlock.getItemList();


        List<RawData> rawDataList = new ArrayList<>();

        int gpsItemIndex = 0;
        int obdItemIndex = 0;

//        Logger.t(TAG).d("iio item list size: " + iioItemList.size());

        for (RawDataItem iioItem : iioItemList) {
            RawData rawData = new RawData();
            long iioPts = iioItem.getPtsMs();
            rawData.setPts(iioPts);
            rawData.setIioData((IioData) iioItem.data);

            if (gpsItemList != null && gpsItemIndex < gpsItemList.size()) {
                RawDataItem gpsItem = gpsItemList.get(gpsItemIndex);
                if (gpsItem.getPtsMs() <= iioPts) {
                    gpsItemIndex++;
                    rawData.setGpsData((GpsData) gpsItem.data);
                }
            }

            if (obdItemList != null && obdItemIndex < obdItemList.size()) {
                RawDataItem obdItem = obdItemList.get(obdItemIndex);
                if (obdItem.getPtsMs() <= iioPts) {
                    obdItemIndex++;
                    rawData.setObdData((ObdData) obdItem.data);
                }
            }

            rawDataList.add(rawData);


        }

        return rawDataList;
    }


    private class RawDataBlockAll {
        private RawDataBlock iioDataBlock = null;
        private RawDataBlock gpsDataBlock = null;
        private RawDataBlock obdDataBlock = null;
    }
}
