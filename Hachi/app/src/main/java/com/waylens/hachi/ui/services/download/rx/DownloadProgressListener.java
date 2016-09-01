package com.waylens.hachi.ui.services.download.rx;

/**
 * Created by Xiaofei on 2016/9/1.
 */
public interface DownloadProgressListener {
    void update(long bytesRead, long contentLength, boolean done);
}
