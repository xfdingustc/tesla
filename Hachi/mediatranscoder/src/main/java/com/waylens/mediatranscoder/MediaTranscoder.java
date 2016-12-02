/*
 * Copyright (C) 2014 Yuya Tanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.waylens.mediatranscoder;


import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.waylens.mediatranscoder.engine.MediaTranscoderEngine;
import com.waylens.mediatranscoder.engine.OverlayProvider;
import com.waylens.mediatranscoder.format.MediaFormatStrategy;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;

public class MediaTranscoder {
    private static final String TAG = MediaTranscoder.class.getSimpleName();

    private static volatile MediaTranscoder sMediaTranscoder;
    private MediaTranscoderEngine mEngine;

    private MediaTranscoder() {

    }

    public static MediaTranscoder getInstance() {
        if (sMediaTranscoder == null) {
            synchronized (MediaTranscoder.class) {
                if (sMediaTranscoder == null) {
                    sMediaTranscoder = new MediaTranscoder();
                }
            }
        }
        return sMediaTranscoder;
    }

    public Observable<TranscodeProgress> transcodeVideoRx(final FileDescriptor inFileDescriptor, final String outPath,
                                                          final MediaFormatStrategy outFormatStrategy,
                                                          final OverlayProvider overlayProvider) {
        return Observable.create(new Observable.OnSubscribe<TranscodeProgress>() {
            @Override
            public void call(Subscriber<? super TranscodeProgress> subscriber) {
                doTranscodeVideo(inFileDescriptor, outPath, outFormatStrategy, overlayProvider, subscriber);
            }
        });


    }

    private void doTranscodeVideo(final FileDescriptor inFileDescriptor, final String outPath,
                                  final MediaFormatStrategy outFormatStrategy,
                                  final OverlayProvider overlayProvider,
                                  final Subscriber<? super TranscodeProgress> subscriber) {
        Exception caughtException = null;
        try {
            mEngine = new MediaTranscoderEngine();
            mEngine.setProgressCallback(new MediaTranscoderEngine.ProgressCallback() {
                @Override
                public void onProgress(final double progress, final long currentTimeMs) {
                    TranscodeProgress transcodeProgress = new TranscodeProgress();
                    transcodeProgress.progress = progress;
                    transcodeProgress.currentTimeMs = currentTimeMs;
                    subscriber.onNext(transcodeProgress);
                }
            });
            mEngine.setDataSource(inFileDescriptor);
            mEngine.transcodeVideo(outPath, outFormatStrategy, overlayProvider);
        } catch (IOException e) {
            Log.w(TAG, "Transcode failed: input file (fd: " + inFileDescriptor.toString() + ") not found"
                + " or could not open output file ('" + outPath + "') .", e);
            caughtException = e;
        } catch (RuntimeException e) {
            Log.e(TAG, "Fatal error while transcoding, this might be invalid format or bug in engine or Android.", e);
            caughtException = e;
        }

        final Exception exception = caughtException;
        if (exception == null) {
            subscriber.onCompleted();
        } else {
            subscriber.onError(exception);
        }
    }

    public void cancel() {
        if (mEngine != null) {
            mEngine.stop();
        }
    }

    private void doTimelaps() {

    }


    public interface Listener {
        /**
         * Called to notify progress.
         *
         * @param progress Progress in [0.0, 1.0] range, or negative value if progress is unknown.
         */
        void onTranscodeProgress(double progress, long currentTimeMs);

        /**
         * Called when transcode completed.
         */
        void onTranscodeCompleted();


        void onTranscodeFailed(Exception exception);
    }

    public static class TranscodeProgress {
        public double progress;
        public long currentTimeMs;
    }
}
