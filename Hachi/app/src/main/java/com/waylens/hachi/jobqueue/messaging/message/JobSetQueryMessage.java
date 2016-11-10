package com.waylens.hachi.jobqueue.messaging.message;

import com.waylens.hachi.jobqueue.JobSetCallback;
import com.waylens.hachi.jobqueue.messaging.Message;
import com.waylens.hachi.jobqueue.messaging.Type;

/**
 * Created by lshw on 16/11/9.
 */

public class JobSetQueryMessage extends Message implements JobSetCallback.MessageWithCallback {
    private JobSetCallback callback;
    private int what = -1;
    private String stringArg;

    public JobSetQueryMessage() {
        super(Type.JOB_SET_QUERY);
    }

    public void set(int what, JobSetCallback callback) {
        this.callback = callback;
        this.what = what;
    }

    public void set(int what, String stringArg, JobSetCallback callback) {
        this.what = what;
        this.stringArg = stringArg;
        this.callback = callback;
    }

    public JobSetCallback getCallback() {
        return callback;
    }

    public int getWhat() {
        return what;
    }

    public String getStringArg() {
        return stringArg;
    }

    public void setCallback(JobSetCallback callback) {
        this.callback = callback;
    }

    @Override
    protected void onRecycled() {
        callback = null;
        what = -1;
    }

    @Override
    public String toString() {
        return "JobSetQuery[" + what + "]";
    }
}