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
    private static final int MAXIMUM_THREAD = 1; // TODO
    private static volatile MediaTranscoder sMediaTranscoder;
    private ThreadPoolExecutor mExecutor;

    private MediaTranscoder() {
        mExecutor = new ThreadPoolExecutor(
            0, MAXIMUM_THREAD, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "MediaTranscoder-Worker");
                }
            });
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


    /**
     * Transcodes video file asynchronously.
     * Audio track will be kept unchanged.
     *
     * @param inPath            File path for input.
     * @param outPath           File path for output.
     * @param outFormatStrategy Strategy for output video format.
     * @param listener          Listener instance for callback.
     * @throws IOException if input file could not be read.
     */
    public void transcodeVideo(final String inPath, final String outPath, final MediaFormatStrategy outFormatStrategy, final Listener listener, final OverlayProvider overlayProvider) throws IOException {
        FileInputStream fileInputStream = null;
        FileDescriptor inFileDescriptor;
        try {
            fileInputStream = new FileInputStream(inPath);
            inFileDescriptor = fileInputStream.getFD();
        } catch (IOException e) {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException eClose) {
                    Log.e(TAG, "Can't close input stream: ", eClose);
                }
            }
            throw e;
        }
        final FileInputStream finalFileInputStream = fileInputStream;
        transcodeVideo(inFileDescriptor, outPath, outFormatStrategy, new Listener() {
            @Override
            public void onTranscodeProgress(double progress, long currentTimeMs) {
                listener.onTranscodeProgress(progress, currentTimeMs);
            }

            @Override
            public void onTranscodeCompleted() {
                listener.onTranscodeCompleted();
                closeStream();
            }

            @Override
            public void onTranscodeFailed(Exception exception) {
                listener.onTranscodeFailed(exception);
                closeStream();
            }

            private void closeStream() {
                try {
                    finalFileInputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Can't close input stream: ", e);
                }
            }
        }, overlayProvider);
    }

    /**
     * Transcodes video file asynchronously.
     * Audio track will be kept unchanged.
     *
     * @param inFileDescriptor  FileDescriptor for input.
     * @param outPath           File path for output.
     * @param outFormatStrategy Strategy for output video format.
     * @param listener          Listener instance for callback.
     */
    public void transcodeVideo(final FileDescriptor inFileDescriptor, final String outPath,
                               final MediaFormatStrategy outFormatStrategy, final Listener listener,
                               final OverlayProvider overlayProvider) {
        Looper looper = Looper.myLooper();
        if (looper == null) {
            looper = Looper.getMainLooper();
        }
        final Handler handler = new Handler(looper);
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Exception caughtException = null;
                try {
                    MediaTranscoderEngine engine = new MediaTranscoderEngine();
                    engine.setProgressCallback(new MediaTranscoderEngine.ProgressCallback() {
                        @Override
                        public void onProgress(final double progress, final long currentTimeMs) {
                            handler.post(new Runnable() { // TODO: reuse instance
                                @Override
                                public void run() {
                                    listener.onTranscodeProgress(progress, currentTimeMs);
                                }
                            });
                        }
                    });
                    engine.setDataSource(inFileDescriptor);
                    engine.transcodeVideo(outPath, outFormatStrategy, overlayProvider);
                } catch (IOException e) {
                    Log.w(TAG, "Transcode failed: input file (fd: " + inFileDescriptor.toString() + ") not found"
                        + " or could not open output file ('" + outPath + "') .", e);
                    caughtException = e;
                } catch (RuntimeException e) {
                    Log.e(TAG, "Fatal error while transcoding, this might be invalid format or bug in engine or Android.", e);
                    caughtException = e;
                }

                final Exception exception = caughtException;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (exception == null) {
                            listener.onTranscodeCompleted();
                        } else {
                            listener.onTranscodeFailed(exception);
                        }
                    }
                });
            }
        });
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
            MediaTranscoderEngine engine = new MediaTranscoderEngine();
            engine.setProgressCallback(new MediaTranscoderEngine.ProgressCallback() {
                @Override
                public void onProgress(final double progress, final long currentTimeMs) {
                    TranscodeProgress transcodeProgress = new TranscodeProgress();
                    transcodeProgress.progress = progress;
                    transcodeProgress.currentTimeMs = currentTimeMs;
                    subscriber.onNext(transcodeProgress);
                }
            });
            engine.setDataSource(inFileDescriptor);
            engine.transcodeVideo(outPath, outFormatStrategy, overlayProvider);
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
