package com.waylens.hachi.service.upload;

/**
 * Created by Xiaofei on 2016/9/9.
 */
public interface UploadProgressListener {
    void update(long bytesWritten, long contentLength, boolean done);
}
