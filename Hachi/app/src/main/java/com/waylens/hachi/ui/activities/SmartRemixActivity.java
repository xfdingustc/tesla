package com.waylens.hachi.ui.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.appyvet.rangebar.RangeBar;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.cache.DiskCache;
import com.waylens.hachi.snipe.cache.impl.UnlimitedDiskCache;
import com.waylens.hachi.snipe.cache.impl.ext.LruDiskCache;
import com.waylens.hachi.snipe.cache.naming.FileNameGenerator;
import com.waylens.hachi.snipe.cache.naming.HashCodeFileNameGenerator;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.snipe.toolbox.RawDataBlockRequest;
import com.waylens.hachi.snipe.utils.StorageUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipFragment;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.rawdata.GpsData;
import com.waylens.hachi.vdb.rawdata.IioData;
import com.waylens.hachi.vdb.rawdata.ObdData;
import com.waylens.hachi.vdb.rawdata.RawData;
import com.waylens.hachi.vdb.rawdata.RawDataBlock;
import com.waylens.hachi.vdb.rawdata.RawDataItem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/4/11.
 */
public class SmartRemixActivity extends BaseActivity {
    private static final String TAG = SmartRemixActivity.class.getSimpleName();
    private static final String IS_PC_SERVER = "isPcServer";
    private static final String SSID = "ssid";
    private static final String HOST_STRING = "hostString";

    private static final int PERMISSION_REQUEST_STORAGE = 0;

    @Bind(R.id.rawdata)
    ProgressBar mLoadingProgressBar;
    @Bind(R.id.iconSpeed)
    ImageButton mIconSpeed;
    @Bind(R.id.iconAccel)
    ImageButton mIconAccel;
    @Bind(R.id.iconBrake)
    ImageButton mIconBrake;
    @Bind(R.id.iconTurn)
    ImageButton mIconTurn;
    @Bind(R.id.iconBump)
    ImageButton mIconBump;
    @Bind(R.id.rangeBarSpeed)
    RangeBar mRangeBarSpeed;
    @Bind(R.id.rangeBarAccel)
    RangeBar mRangeBarAccel;
    @Bind(R.id.rangeBarBrake)
    RangeBar mRangeBarBrake;
    @Bind(R.id.rangeBarTurn)
    RangeBar mRangeBarTurn;
    @Bind(R.id.rangeBarBump)
    RangeBar mRangeBarBump;

    @Bind(R.id.tvClipSelected)
    TextView mTvClipSelected;

    private ClipSet mAllClipSet;
    private VdtCamera mVdtCamera;
    private VdbRequestQueue mVdbRequestQueue;
    private int mCurrentLoadingIndex;
    private List<RawDataBlockAll> mRawDataBlockList = new ArrayList<>();
    private List<List<RawData>> mRawDataList = new ArrayList<>();

    private List<ClipFragment> mSelectedClipFragment = null;

    private boolean mRawDataLoaded = false;

    private DiskCache mDiskCache;

    public static void launch(Activity activity, VdtCamera camera) {
        Intent intent = new Intent(activity, SmartRemixActivity.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean(IS_PC_SERVER, camera.isPcServer());
        bundle.putString(SSID, camera.getSSID());
        bundle.putString(HOST_STRING, camera.getHostString());
        intent.putExtras(bundle);
        activity.startActivity(intent);
    }

    @OnClick(R.id.iconSpeed)
    public void onIconSpeedClicked() {
        boolean isIconSpeedSelected = !mIconSpeed.isSelected();
        mIconSpeed.setSelected(isIconSpeedSelected);
        mRangeBarSpeed.setEnabled(isIconSpeedSelected);
    }

    @OnClick(R.id.iconAccel)
    public void onIconAccelClicked() {
        boolean isIconAccelSelected = !mIconAccel.isSelected();
        mIconAccel.setSelected(isIconAccelSelected);
        mRangeBarAccel.setEnabled(isIconAccelSelected);
    }

    @OnClick(R.id.iconBrake)
    public void onIconBrakeClicked() {
        boolean isIconBrakeSelected = !mIconBrake.isSelected();
        mIconBrake.setSelected(isIconBrakeSelected);
        mRangeBarBrake.setEnabled(isIconBrakeSelected);
    }

    @OnClick(R.id.iconTurn)
    public void onIconTurnClicked() {
        boolean isIconTurnSelected = !mIconTurn.isSelected();
        mIconTurn.setSelected(isIconTurnSelected);
        mRangeBarTurn.setEnabled(isIconTurnSelected);
    }

    @OnClick(R.id.iconBump)
    public void onIconBumpClicked() {
        boolean isIconBumpSelected = !mIconBump.isSelected();
        mIconBump.setSelected(isIconBumpSelected);
        mRangeBarTurn.setEnabled(isIconBumpSelected);
    }

    @OnClick(R.id.btnCreateRemix)
    public void onBtnSmartRemixClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Logger.t(TAG).d("storage permission not granted");
            startLoadingRawData();
        } else {
            Logger.t(TAG).d("storage permission is already granted");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_STORAGE);
        }

    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLoadingRawData();
                }
                break;
        }
    }

    @Override
    protected void init() {
        super.init();
        mVdtCamera = getCameraFromIntent(getIntent().getExtras());
        mVdbRequestQueue = mVdtCamera.getRequestQueue();//Snipe.newRequestQueue(this, mVdtCamera);
//        mDiskCache = createDiskCache(this, new HashCodeFileNameGenerator(), 1024 * 1024 * 1024, 50 * 1024);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_smart_remix);
        mRangeBarSpeed.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex, String leftPinValue, String rightPinValue) {
                if (mRawDataLoaded) {
                    mSelectedClipFragment = analyseRawData();

                    mTvClipSelected.setText("" + mSelectedClipFragment.size() + " clips is selected");

                }
            }
        });
    }





    @Override
    public void setupToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            mToolbar.setTitle(R.string.smart_remix);
            mToolbar.setNavigationIcon(R.drawable.navbar_back);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
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

//        Logger.t(TAG).d("Load finished type: " + dataType + " index: " + mCurrentLoadingIndex);

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
                    onHandleLoadRawDataFinished();

                } else {
                    loadRawData(RawDataItem.DATA_TYPE_OBD);
                }
                mLoadingProgressBar.setProgress(mCurrentLoadingIndex);
                break;
        }
    }

    private void onHandleLoadRawDataFinished() {
        mRawDataLoaded = true;
        mTvClipSelected.setVisibility(View.VISIBLE);
        mToolbar.getMenu().clear();
        mToolbar.inflateMenu(R.menu.menu_smart_remix_to_enhance);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.to_enhance:
                        launchEnhanceActivity(mSelectedClipFragment);
                        break;
                }
                return true;
            }
        });

    }

    private void launchEnhanceActivity(List<ClipFragment> clipFragments) {
        ArrayList<Clip> selectedList = new ArrayList<>();
        int total = 0;
        for (ClipFragment clipFragment : clipFragments) {
            Clip clip = new Clip(clipFragment.getClip());

            clip.setStartTime(clipFragment.getStartTimeMs());
            clip.setEndTime(clipFragment.getEndTimeMs());
            selectedList.add(clip);
        }
        EnhancementActivity.launch(this, selectedList, EnhancementActivity.LAUNCH_MODE_ENHANCE);
    }

    private List<ClipFragment> analyseRawData() {

        List<ClipFragment> clipFragmentList = new ArrayList<>();


        for (int i = 0; i < mRawDataList.size(); i++) {

            boolean startFound = false;
            List<RawData> rawDataList = mRawDataList.get(i);

            Clip clip = mAllClipSet.getClip(i);
            ClipFragment clipFragment = null;
            for (int j = 0; j < rawDataList.size(); j++) {
                RawData rawData = rawDataList.get(j);
//            for (RawData rawData : rawDataList) {

                if (!startFound && ifMeetThreshold(rawData)) {
//                    Logger.t(TAG).d("start Hit!!!!!! " + rawData.getObdData().speed + " pts: " + rawData.getPts());
                    startFound = true;
                    clipFragment = new ClipFragment(clip);
                    clipFragment.setStartTime(rawData.getPts());
//                    Logger.t(TAG).d("set start: " + clipFragment.toString());
                }

                if (startFound && (!ifMeetThreshold(rawData) || j == rawDataList.size() - 1)) {
//                    Logger.t(TAG).d("end Hit!!!!!! "  );
                    startFound = false;
                    clipFragment.setEndTime(rawData.getPts());
                    Logger.t(TAG).d("Found one ClipFragment: " + clipFragment.toString());
                    clipFragmentList.add(clipFragment);
                }
            }
        }


        return clipFragmentList;
    }


    private boolean ifMeetThreshold(RawData rawData) {
        // check speed:
        if (mRangeBarSpeed.isEnabled()) {
            if (rawData.getGpsData() != null) {
//                Logger.t(TAG).d("mRangeBarSpeed: left: " + mRangeBarSpeed.getLeftPinValue() + " ~ " + mRangeBarSpeed.getRightPinValue());
                int left = Integer.parseInt(mRangeBarSpeed.getLeftPinValue());
                int right = Integer.parseInt(mRangeBarSpeed.getRightPinValue());
                if (left <= rawData.getGpsData().speed && rawData.getGpsData().speed <= right) {
                    return true;
                }
            }

            return false;
        }

        return false;
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
