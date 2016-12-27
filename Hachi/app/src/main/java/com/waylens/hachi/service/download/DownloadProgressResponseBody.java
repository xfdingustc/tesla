package com.waylens.hachi.service.download;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * Created by Xiaofei on 2016/9/1.
 */
public class DownloadProgressResponseBody extends ResponseBody {

    private final ResponseBody mResponseBody;
    private final DownloadProgressListener mProgressListener;
    private BufferedSource mBufferedSource;

    public DownloadProgressResponseBody(ResponseBody responseBody, DownloadProgressListener listener) {
        this.mResponseBody = responseBody;
        this.mProgressListener = listener;
    }

    @Override
    public MediaType contentType() {
        return mResponseBody.contentType();
    }

    @Override
    public long contentLength() {
        return mResponseBody.contentLength();
    }

    @Override
    public BufferedSource source() {
        if (mBufferedSource == null) {
            mBufferedSource = Okio.buffer(source(mResponseBody.source()));
        }

        return mBufferedSource;
    }


    private Source source(Source source) {
        return new ForwardingSource(source) {

            long totalBytesRead = 0L;
            double percentage = 0.0;

            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                long bytesRead = super.read(sink, byteCount);

                totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                double newPercentage = (double)totalBytesRead / mResponseBody.contentLength();
                if (mProgressListener != null && (newPercentage - percentage) > 0.01) {
                    mProgressListener.update(totalBytesRead, mResponseBody.contentLength(), bytesRead == -1);
                    percentage = newPercentage;
                }

                return bytesRead;
            }
        };
    }
}
