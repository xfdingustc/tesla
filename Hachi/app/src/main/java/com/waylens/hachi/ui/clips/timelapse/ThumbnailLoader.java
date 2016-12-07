package com.waylens.hachi.ui.clips.timelapse;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.google.android.exoplayer.upstream.Loader;
import com.orhanobut.logger.Logger;


/**
 * Created by lshw on 16/12/6.
 */

public class ThumbnailLoader {
    public static final String TAG = ThumbnailLoader.class.getSimpleName();

    private static final int MSG_END_OF_SOURCE = 0;
    private static final int MSG_IO_EXCEPTION = 1;
    private static final int MSG_FATAL_ERROR = 2;

    private final ExecutorService downloadExecutorService;
    private boolean loading;
    private LoadTask currentTask;


    public ThumbnailLoader(String name) {
        downloadExecutorService = newSingleThreadExecutor(name);
    }


    public void startLoading(Looper looper, Loadable loadable, Callback callback) {
        if (loading) {
            return;
        }
        loading = true;
        currentTask = new LoadTask(looper, loadable, callback);
        downloadExecutorService.submit(currentTask);
    }

    public static ExecutorService newSingleThreadExecutor(final String threadName) {
        return Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, threadName);
            }
        });
    }
    public interface Loadable {

        /**
         * Cancels the load.
         */
        void cancelLoad();

        /**
         * Whether the load has been canceled.
         *
         * @return True if the load has been canceled. False otherwise.
         */
        boolean isLoadCanceled();

        /**
         * Performs the load, returning on completion or cancelation.
         *
         * @throws IOException
         * @throws InterruptedException
         */
        void load() throws IOException, InterruptedException;

    }

    /**
     * Interface definition for a callback to be notified of {@link Loader} events.
     */
    public interface Callback {

        /**
         * Invoked when loading has been canceled.
         *
         * @param loadable The loadable whose load has been canceled.
         */
        void onLoadCanceled(Loadable loadable);

        /**
         * Invoked when the data source has been fully loaded.
         *
         * @param loadable The loadable whose load has completed.
         */
        void onLoadCompleted(Loadable loadable);

        /**
         * Invoked when the data source is stopped due to an error.
         *
         * @param loadable The loadable whose load has failed.
         */
        void onLoadError(Loadable loadable, IOException exception);

    }

    private final class LoadTask extends Handler implements Runnable {

        private static final String TAG = "LoadTask";

        private final Loadable loadable;
        private final ThumbnailLoader.Callback callback;

        private volatile Thread executorThread;

        public LoadTask(Looper looper, Loadable loadable, ThumbnailLoader.Callback callback) {
            super(looper);
            this.loadable = loadable;
            this.callback = callback;
        }

        public void quit() {
            loadable.cancelLoad();
            if (executorThread != null) {
                executorThread.interrupt();
            }
        }

        @Override
        public void run() {
            try {
                executorThread = Thread.currentThread();
                if (!loadable.isLoadCanceled()) {
                    loadable.load();
                }
                sendEmptyMessage(MSG_END_OF_SOURCE);
            } catch (IOException e) {
                obtainMessage(MSG_IO_EXCEPTION, e).sendToTarget();
            } catch (InterruptedException e) {
                // The load was canceled.
                sendEmptyMessage(MSG_END_OF_SOURCE);
            } catch (Exception e) {
                // This should never happen, but handle it anyway.
                Logger.e(TAG, "Unexpected exception loading stream", e);
                obtainMessage(MSG_IO_EXCEPTION, new Exception(e)).sendToTarget();
            } catch (Error e) {
                // We'd hope that the platform would kill the process if an Error is thrown here, but the
                // executor may catch the error (b/20616433). Throw it here, but also pass and throw it from
                // the handler thread so that the process dies even if the executor behaves in this way.
                Logger.e(TAG, "Unexpected error loading stream", e);
                obtainMessage(MSG_FATAL_ERROR, e).sendToTarget();
                throw e;
            }
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_FATAL_ERROR) {
                throw (Error) msg.obj;
            }
            onFinished();
            if (loadable.isLoadCanceled()) {
                callback.onLoadCanceled(loadable);
                return;
            }
            switch (msg.what) {
                case MSG_END_OF_SOURCE:
                    callback.onLoadCompleted(loadable);
                    break;
                case MSG_IO_EXCEPTION:
                    callback.onLoadError(loadable, (IOException) msg.obj);
                    break;
            }
        }

        private void onFinished() {
            loading = false;
            currentTask = null;
        }

    }

}
