package com.waylens.hachi.jobqueue.messaging.message;


import com.waylens.hachi.jobqueue.Job;
import com.waylens.hachi.jobqueue.messaging.Message;
import com.waylens.hachi.jobqueue.messaging.Type;

public class AddJobMessage extends Message {
    private Job job;
    public AddJobMessage() {
        super(Type.ADD_JOB);
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    @Override
    protected void onRecycled() {
        job = null;
    }
}
