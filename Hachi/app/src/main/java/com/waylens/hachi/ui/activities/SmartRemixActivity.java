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
import com.waylens.hachi.snipe.toolbox.ClipSetRequest;
import com.waylens.hachi.snipe.toolbox.RawDataBlockRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipFragment;
import com.waylens.hachi.vdb.ClipSet;
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
        ClipSetRequest request = new ClipSetRequest(Clip.TYPE_MARKED, new VdbResponse.Listener<ClipSet>() {
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
        ClipSetRequest request = new ClipSetRequest(Clip.TYPE_BUFFERED, new VdbResponse.Listener<ClipSet>() {
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
                rawDataBlockAll.accDataBlock = block;
//                saveAccRawData(block);
                loadRawData(RawDataItem.DATA_TYPE_GPS);
                break;
            case RawDataItem.DATA_TYPE_GPS:
                rawDataBlockAll.gpsDataBlock = block;

                if (++mCurrentLoadingIndex == mAllClipSet.getCount()) {
                    Logger.t(TAG).d("load finished!!!!!");


                } else {
                    loadRawData(RawDataItem.DATA_TYPE_OBD);
                }
                mLoadingProgressBar.setProgress(mCurrentLoadingIndex);


                break;
        }
    }

    private void saveAccRawData(RawDataBlock block) {
        List<RawDataItem> items = block.getItemList();
        for (RawDataItem item : items) {
            mRawDataItemDao.addAccRawDataItem(item);
        }
    }

    private class RawDataBlockAll {
        private RawDataBlock accDataBlock = null;
        private RawDataBlock gpsDataBlock = null;
        private RawDataBlock obdDataBlock = null;
    }
}
