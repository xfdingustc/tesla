package com.waylens.hachi.jobqueue.messaging.message;


import com.waylens.hachi.jobqueue.JobHolder;
import com.waylens.hachi.jobqueue.messaging.Message;
import com.waylens.hachi.jobqueue.messaging.Type;

public class RunJobMessage extends Message {
    private JobHolder jobHolder;
    public RunJobMessage() {
        super(Type.RUN_JOB);
    }

    public JobHolder getJobHolder() {
        return jobHolder;
    }

    public void setJobHolder(JobHolder jobHolder) {
        this.jobHolder = jobHolder;
    }

    @Override
    protected void onRecycled() {
        jobHolder = null;
    }
}
