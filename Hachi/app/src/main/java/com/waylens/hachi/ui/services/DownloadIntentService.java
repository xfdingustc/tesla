package com.waylens.hachi.ui.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.waylens.hachi.hardware.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.vdb.Clip;


public class DownloadIntentService extends IntentService {
    private static final String ACTION_DOWNLOAD = "com.waylens.hachi.ui.services.action.DOWNLOAD";
    private static Clip mSharedClip;

    private static VdtCamera mSharedCamera;

    private static final String EXTRA_CLIP = "com.waylens.hachi.ui.services.extra.CLIP";

    private VdbRequestQueue mVdbRequestQueue;
    private Clip mClip;

    public DownloadIntentService() {
        super("DownloadIntentService");
    }

    public static void startDownload(Context context, VdtCamera vdtCamera, Clip clip) {
        Intent intent = new Intent(context, DownloadIntentService.class);
        intent.setAction(ACTION_DOWNLOAD);
        mSharedCamera = vdtCamera;
        mSharedClip = clip;
        context.startService(intent);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DOWNLOAD.equals(action)) {
                mClip = mSharedClip;
                handleDownloadClip(mClip);
            }
        }
    }

    private void init() {
        mVdbRequestQueue = Snipe.newRequestQueue(this);
    }

    private void handleDownloadClip(Clip clip) {

    }

}
