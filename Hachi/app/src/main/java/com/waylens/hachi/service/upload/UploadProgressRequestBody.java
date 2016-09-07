package com.waylens.hachi.service.upload;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ForwardingSink;
import okio.ForwardingSource;
import okio.Okio;
import okio.Sink;
import okio.Source;

/**
 * Created by Xiaofei on 2016/9/9.
 */
public class UploadProgressRequestBody extends RequestBody {

    private final RequestBody mRequestBody;
    private final UploadProgressListener mProgressListener;
    private BufferedSink mBufferedSink;


    public static UploadProgressRequestBody newInstance(File file, UploadProgressListener listener) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/octet-stream;chartset=UTF-8"), file);
        return new UploadProgressRequestBody(requestBody, listener);
    }

    private UploadProgressRequestBody(RequestBody requestBody, UploadProgressListener listener) {
        this.mRequestBody = requestBody;
        this.mProgressListener = listener;
    }

    @Override
    public MediaType contentType() {
        return mRequestBody.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return mRequestBody.contentLength();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
       if (mBufferedSink == null) {
           mBufferedSink = Okio.buffer(sink(sink));
       }

        mRequestBody.writeTo(mBufferedSink);
        mBufferedSink.flush();
    }


    private Sink sink(Sink sink) {
        return new ForwardingSink(sink) {
            //当前写入字节数
            long bytesWritten = 0L;
            //总字节长度，避免多次调用contentLength()方法
            long contentLength = 0L;

            @Override
            public void write(Buffer source, long byteCount) throws IOException {
                super.write(source, byteCount);
                if (contentLength == 0) {
                    //获得contentLength的值，后续不再调用
                    contentLength = contentLength();
                }
                //增加当前写入的字节数
                bytesWritten += byteCount;
                //回调
                mProgressListener.update(bytesWritten, contentLength, bytesWritten == contentLength);
            }
        };
    }
}
