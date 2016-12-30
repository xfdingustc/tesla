package com.waylens.hachi.uploadqueue;

import android.content.Context;

import com.waylens.hachi.uploadqueue.model.UploadQueueActions;

/**
 * Created by Xiaofei on 2016/12/29.
 */

public class UploadManager {
    private static final String TAG = UploadManager.class.getSimpleName();

    private static UploadManager mSharedUploadManager = null;

    private UploadManager() {

    }


    public static UploadManager getManager(Context context) {
        if (mSharedUploadManager == null) {
            mSharedUploadManager = new UploadManager();
            mSharedUploadManager.getDataFromDatabase(context);
            mSharedUploadManager.updateManagerForData(context);
            //startUploadQueueService(context, UploadQueueActions.START_UPLOAD);
        }

        return mSharedUploadManager;
    }

    private void updateManagerForData(Context context) {

    }

    private void getDataFromDatabase(Context context) {

    }
}
