package com.waylens.hachi.uploadqueue;

import com.waylens.hachi.uploadqueue.interfaces.UploadResponseListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2017/1/3.
 */

public class UploadResponseHolder {
    private static UploadResponseHolder mSharedHolder;

    private List<UploadResponseListener> mListenerList;

    private UploadResponseHolder() {
        mListenerList = new ArrayList<>();
    }


    public static UploadResponseHolder getHolder() {
        if (mSharedHolder == null) {
            synchronized (UploadResponseHolder.class) {
                if (mSharedHolder == null) {
                    mSharedHolder = new UploadResponseHolder();
                }
            }
        }

        return mSharedHolder;
    }

    public void addListener(UploadResponseListener listener) {
        mListenerList.add(listener);
    }

    public void removeListener(UploadResponseListener listener) {
        mListenerList.remove(listener);
    }


    public void onUploadStart(String key) {
        for (UploadResponseListener listener : mListenerList) {
            listener.onUploadStart(key);
        }
    }

    public void onUploadStart(String key, int totalSize) {
        for (UploadResponseListener listener : mListenerList) {
            listener.onUploadStart(key, totalSize);
        }
    }

    public void updateProgress(String key, int progress) {
        for (UploadResponseListener listener : mListenerList) {
            listener.updateProgress(key, progress);
        }
    }

    public void onComplete(String key) {
        for (UploadResponseListener listener : mListenerList) {
            listener.onComplete(key);
        }
    }
}
