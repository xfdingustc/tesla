package com.waylens.hachi.uploadqueue.interfaces;

import com.waylens.hachi.uploadqueue.model.UploadError;

/**
 * Created by Xiaofei on 2016/12/30.
 */

public interface UploadResponseListener {
    void onUploadStart(String key);

    void onUploadStart(String key, int totalSize);

    void updateProgress(String key, int progress);

    void onComplete(String key);

    void onError(String key, UploadError error);
}
