package com.waylens.hachi.uploadqueue;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.uploadqueue.interfaces.UploadResponseListener;
import com.waylens.hachi.uploadqueue.model.UploadError;
import com.waylens.hachi.uploadqueue.model.UploadQueueActions;
import com.waylens.hachi.uploadqueue.model.UploadRequest;
import com.waylens.hachi.uploadqueue.model.UploadStatus;
import com.waylens.hachi.uploadqueue.utils.UploadQueueUtilityNetwork;

import java.util.HashMap;

/**
 * Created by Xiaofei on 2016/12/30.
 */

public class UploadService extends Service {
    private static final String TAG = UploadService.class.getSimpleName();
    private Context mContext;
    private HashMap<String, UploadQueueUploader> mUploadThreadMap = new HashMap<>();
    private HashMap<String, UploadRequest> mUploadQueueKeysRequestMap = new HashMap<>();


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        if (mContext == null) {
            mContext = this;
        }
        if (intent == null || intent.getExtras() == null || intent.hasExtra("action") == false) {
            return;
        }

        UploadQueueActions action = UploadQueueActions.get(intent.getIntExtra("action", 0));

        UploadManager uploadManager = UploadManager.getManager(this);
        switch (action) {
            case START_UPLOAD:
                startUpload(uploadManager);
                break;
            case START_UPLOAD_FROM_PAUSE:
                break;
            case STOP_UPLOAD_QUEUE:
                break;
            case UPDATE_UPLOAD_QUEUE:
                break;
            case PAUSE_ITEM:
                break;
            case DELETE_ITEM:
                break;
            case REMOVE_ITEM:
                break;
        }
    }


    public void startUpload(UploadManager uploadManager) {
        Logger.t(TAG).d("start upload");
        if (uploadManager.canUploadFurthurItems(this)) {
            Logger.t(TAG).d("can upload futher item");
            while (uploadManager.canUploadFurthurItems(this)) {
                UploadRequest request = uploadManager.getNextItemToUpload(this);
                if (!UploadQueueUtilityNetwork.isNetworkAvailable(this)) {
                    break;
                }

                if (mUploadQueueKeysRequestMap.containsKey(request.getKey()) == false) {
                    UploadQueueUploader uploader = new UploadQueueUploader(this, request, mUploadListener);
                    uploader.start();
                    mUploadQueueKeysRequestMap.put(request.getKey(), request);
                    request.setStatus(UploadStatus.UPLOADING);
                    uploadManager.updateUploadStatus(request);
                    uploadManager.updateManagerAndNotify(request.getKey(), request);
                } else {
                    request.setStatus(UploadStatus.UPLOADING);
                    uploadManager.updateUploadStatus(request);
                    uploadManager.updateManagerAndNotify(request.getKey(), request);
                }


            }

        }
    }


    private UploadResponseListener mUploadListener = new UploadResponseListener() {
        @Override
        public void onUploadStart(String key) {
            UploadResponseHolder.getHolder().onUploadStart(key);
            UploadManager.getManager(UploadService.this).updateManagerAndNotify(key);
        }

        @Override
        public void onUploadStart(String key, int totalSize) {
            UploadResponseHolder.getHolder().onUploadStart(key, totalSize);
            UploadManager.getManager(UploadService.this).updateManagerAndNotify(key);
        }

        @Override
        public void updateProgress(String key, int progress) {
            UploadResponseHolder.getHolder().updateProgress(key, progress);
            UploadManager uploadManager = UploadManager.getManager(UploadService.this);
            UploadRequest request = uploadManager.getItem(key);
            request.setProgress(progress);

        }

        @Override
        public void onComplete(String key) {
            UploadManager manager = UploadManager.getManager(UploadService.this);
            UploadRequest request = manager.getItem(key);
            if (request != null) {
                request.setStatus(UploadStatus.COMPLETED);
                manager.updateUploadStatus(request);
            }
            manager.updateDBandQueue(mContext, key, UploadStatus.COMPLETED);
            if (!UploadQueueUtilityNetwork.isNetworkAvailable(mContext)) {
                if (!manager.isAnyUploadingInProgressPending()) {
                    stopSelf();
                }
            } else if (UploadQueueUtilityNetwork.isNetworkAvailable(mContext) && manager.canUploadFurthurItems(mContext) == false) {
                stopSelf();
            }

            if (mUploadThreadMap.containsKey(key)) {
                mUploadThreadMap.remove(key);
            }

            UploadResponseHolder.getHolder().onComplete(key);

            if (mUploadQueueKeysRequestMap != null && mUploadQueueKeysRequestMap.containsKey(key)) {
                mUploadQueueKeysRequestMap.remove(key);
            }
        }

        @Override
        public void onError(String key, UploadError error) {
            if (mUploadQueueKeysRequestMap != null && mUploadQueueKeysRequestMap.containsKey(key)) {
                mUploadQueueKeysRequestMap.remove(key);
            }

            if (mUploadThreadMap.containsKey(key)) {
                mUploadThreadMap.remove(key);
            }

            UploadManager.getManager(mContext).errorOccured(mContext, key, error);

            UploadResponseHolder.getHolder().onError(key, error);
        }
    };
}
