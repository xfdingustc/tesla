package com.waylens.hachi.uploadqueue;

import android.content.Context;
import android.content.Intent;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.uploadqueue.db.UploadQueueDBAdapter;
import com.waylens.hachi.uploadqueue.model.UploadError;
import com.waylens.hachi.uploadqueue.model.UploadQueueActions;
import com.waylens.hachi.uploadqueue.model.UploadRequest;
import com.waylens.hachi.uploadqueue.model.UploadStatus;
import com.waylens.hachi.uploadqueue.utils.UploadQueueUtilityNetwork;
import com.waylens.hachi.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2016/12/29.
 */

public class UploadManager {
    private static final String TAG = UploadManager.class.getSimpleName();

    private static final int MAX_PARALLEL_UPLOAD = 3;

    private List<UploadRequest> mUploadQueue = null;

    private static UploadManager mSharedUploadManager = null;

    private UploadManager() {
        mUploadQueue = new ArrayList();
    }


    public static UploadManager getManager(Context context) {
        if (mSharedUploadManager == null) {
            mSharedUploadManager = new UploadManager();
            mSharedUploadManager.getDataFromDatabase(context);
            mSharedUploadManager.updateManagerForData(context);
            startUploadQueueService(context, UploadQueueActions.START_UPLOAD);
        }

        if (mSharedUploadManager.mUploadQueue.isEmpty()) {
            mSharedUploadManager.getDataFromDatabase(context);
        }

        return mSharedUploadManager;
    }

    public UploadRequest getItem(String key) {
        for (UploadRequest request : mUploadQueue) {
            if (request.getKey().equals(key)) {
                return request;
            }
        }
        return null;
    }

    public int getItemPosition(String key) {
        for (int i = 0; i < mUploadQueue.size(); i++) {
            UploadRequest request = mUploadQueue.get(i);
            if (request.getKey().equals(key)) {
                return i;
            }
        }

        return -1;
    }

    public void updateDBandQueue(Context context, String key, UploadStatus status) {
        UploadRequest itemToBeRemove = getItem(key);
        if (itemToBeRemove == null) {
            return;
        }

        switch (status) {
            case DELETED:
                deleteUploadRequest(itemToBeRemove, context);
                break;
            case COMPLETED:
                itemToBeRemove.setStatus(UploadStatus.COMPLETED);
                uploadedRequestCompleted(context, itemToBeRemove);
                break;
        }

        startUploadQueueService(context, UploadQueueActions.START_UPLOAD);
        updateManagerAndNotify(key, itemToBeRemove);

    }

    private boolean uploadedRequestCompleted(Context context, UploadRequest itemToBeRemove) {
        if (itemToBeRemove != null) {
            itemToBeRemove.setUploading(false);
            return deleteUploadRequest(itemToBeRemove, context);
        }

        return false;
    }

    public void errorOccured(Context context, String key, UploadError error) {
        UploadRequest request = getItem(key);
        if (request != null) {
            request.setStatus(UploadStatus.FAILED);
            updateUploadStatus(request);
        }

        updateDBandQueue(context, key, UploadStatus.FAILED);
    }

    private boolean deleteUploadRequest(UploadRequest itemToBeRemove, Context context) {
        if (itemToBeRemove == null) {
            return false;
        }
        Logger.t(TAG).d("delete upload request");
        itemToBeRemove.setStatus(UploadStatus.DELETED);
        UploadQueueDBAdapter dbAdapter = UploadQueueDBAdapter.getInstance();
        dbAdapter.delete(itemToBeRemove);
        mUploadQueue.remove(itemToBeRemove);
        return true;
    }

    private void getDataFromDatabase(Context context) {
        Logger.t(TAG).d("get data from database'");
        String userId = SessionManager.getInstance().getUserId();
        UploadQueueDBAdapter dbAdapter = UploadQueueDBAdapter.getInstance();
        dbAdapter.updateUploadStatus();
        List<UploadRequest> requestList = dbAdapter.listUploadRequestByUserId(userId);
        if (requestList != null && !requestList.isEmpty()) {
            mUploadQueue.addAll(requestList);
        }

        Logger.t(TAG).d("upload queue size: " + mUploadQueue.size());
        for (UploadRequest request : mUploadQueue) {
            Logger.t(TAG).d("request status: " + request.getStatus());
        }
    }

    public void addToQueue(UploadRequest request, Context context) {
        String userId = SessionManager.getInstance().getUserId();
        request.setUserId(userId);
        UploadQueueDBAdapter dbAdapter = UploadQueueDBAdapter.getInstance();
        if (!dbAdapter.isInUploadQueue(request.getKey(), userId)) {
            dbAdapter.insert(request);
            mUploadQueue.add(request);
        }
        startUploadQueueService(context, UploadQueueActions.START_UPLOAD);

    }

    public boolean stopUploading(Context context, String key) {
        UploadRequest itemToBeStop = getItem(key);
        if (itemToBeStop == null || itemToBeStop.getStatus() == UploadStatus.DELETE_REQUEST || itemToBeStop.getStatus() == UploadStatus.DELETED) {
            return true;
        }

        itemToBeStop.setStatus(UploadStatus.DELETE_REQUEST);
        updateUploadStatus(itemToBeStop);
        startUploadQueueService(context, UploadQueueActions.DELETE_ITEM);
        return true;
    }

    public List<UploadRequest> getQueuedItemList() {
        return mUploadQueue;
    }




    public boolean isThereAnyItemWithStatus(UploadStatus status) {

        boolean flag = false;
        for (int s = 0; s < mUploadQueue.size(); s++) {
            UploadRequest temp = mUploadQueue.get(s);
            if (temp != null && (temp.getStatus() == status)) {
                flag = true;
                break;
            }
        }
        return flag;
    }


    public boolean updateUploadStatus(UploadRequest request) {
        String userId = SessionManager.getInstance().getUserId();
        return UploadQueueDBAdapter.getInstance().updateRequest(request);
    }

    public void updateManagerAndNotify(String key, UploadRequest uploadRequest) {

    }

    public void updateManagerAndNotify(String key) {

    }

    public boolean canUploadFurthurItems(Context context) {
        boolean flag = false;
        flag = isLimitAvaiable();
        if (flag) {
            UploadRequest request = getItemWithStatus(UploadStatus.UPLOAD_REQUEST);
            if (request == null) {
                request = getItemWithStatus(UploadStatus.WAITING);
                if (request == null) {
                    flag = false;
                }
            }
        }


        return flag;
    }


    public UploadRequest getNextItemToUpload(Context context) {
        if (canUploadFurthurItems(context)) {
            for (UploadRequest request : mUploadQueue) {
                if (request.getStatus() == UploadStatus.UPLOAD_REQUEST) {
                    return request;
                }
            }

            for (UploadRequest request : mUploadQueue) {
                if (request.getStatus() == UploadStatus.WAITING || request.getStatus() == UploadStatus.FAILED) {
                    return request;
                }
            }
        }

        return null;
    }


    public boolean isLimitAvaiable() {
        int limit = getMaxParallelDownloads();
        int curUploading = countCurrentUploading();
        if (curUploading < limit) {
            return true;
        }
        return false;
    }


    public UploadRequest getItemWithStatus(UploadStatus status) {
        if (mUploadQueue == null || mUploadQueue.isEmpty()) {
            return null;
        }
        for (UploadRequest request : mUploadQueue) {
            if (request.getStatus() == status) {
                return request;
            }
        }

        return null;

    }

    private int countCurrentUploading() {
        int curUploading = 0;
        for (int i = 0; i < mUploadQueue.size(); i++) {
            UploadRequest request = mUploadQueue.get(i);
            if (request != null && request.getStatus() == UploadStatus.UPLOADING || request.getStatus() == UploadStatus.PAUSED || request.getStatus() == UploadStatus.PAUSED_REQUEST) {
                curUploading++;
            }
        }
        return curUploading;
    }

    private int getMaxParallelDownloads() {
        return PreferenceUtils.getInt(PreferenceUtils.KEY_MAX_PARALLEL_UPLOAD, MAX_PARALLEL_UPLOAD);
    }

    public static void startUploadQueueService(Context context, UploadQueueActions action) {
        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra("action", action.ordinal());
        context.startService(intent);

    }

    private void updateManagerForData(Context context) {

    }


    public static boolean isDownloadOnlyOnWifi(Context context) {
        return PreferenceUtils.getBoolean(PreferenceUtils.KEY_ONLY_ON_WIFI, true);

    }

    public static boolean isConfiguredNetworkAvailable(Context context) {

        boolean isNetwrokOn = false;
        if (UploadQueueUtilityNetwork.isNetworkAvailable(context)) {
            boolean onlyOnWifi = isDownloadOnlyOnWifi(context);
            if (onlyOnWifi) {
                if (UploadQueueUtilityNetwork.isConnectedToWifi(context)) {
                    isNetwrokOn = true;
                }
            } else {
                isNetwrokOn = true;
            }
        }
        return isNetwrokOn;
    }


}
