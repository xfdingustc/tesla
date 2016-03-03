package com.waylens.hachi.ui.views.cliptrimmer;

import android.os.Handler;
import android.os.Message;

import com.waylens.hachi.ui.views.Progressive;

/**
 * ProgressHandler
 * Created by Richard on 10/13/15.
 */
class ProgressHandler extends Handler {
    private static final int MSG_SHOW_PROGRESS = 100;
    Progressive mProgressBar;

    public ProgressHandler(Progressive progressBar) {
        mProgressBar = progressBar;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_SHOW_PROGRESS:
                mProgressBar.updateProgress();
                if (mProgressBar.isInProgress()) {
                    sendMessageDelayed(obtainMessage(MSG_SHOW_PROGRESS), 20);
                }
                break;
        }
    }

    public void stop() {
        removeMessages(MSG_SHOW_PROGRESS);
    }

    public void start() {
        sendEmptyMessage(MSG_SHOW_PROGRESS);
    }
}
