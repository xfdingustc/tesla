package com.waylens.hachi.bgjob.export.statejobqueue;


import java.util.UUID;

/**
 * Created by laina on 16/11/21.
 */

public class StateJobHolder{
    public static String TAG = StateJobHolder.class.getSimpleName();
    public static int INITIAL_STATE = 0x0000;
    public static int MIDDLE_STATE = 0x0001;
    public static int FINISH_STATE = 0x0002;

    public long getInsertId() {
        return insertId;
    }

    public void setInsertId(long insertId) {
        this.insertId = insertId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public int getJobState() {
        return jobState;
    }

    public void setJobState(int jobState) {
        this.jobState = jobState;
    }

    public String getJobTemp() {
        return jobTemp;
    }

    public void setJobTemp(String jobTemp) {
        this.jobTemp = jobTemp;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    private Long insertId;
    private String jobId;
    private int jobState;
    private String jobTemp;
    private Job job;


    public StateJobHolder(String jobId, int jobState, String jobTemp, Job job) {
        this(null, jobId, jobState, jobTemp, job);
    }

    public StateJobHolder(Long insertId, String jobId, int jobState, String jobTemp, Job job) {
        this.insertId = insertId;
        if (jobId == null) {
            this.jobId = UUID.randomUUID().toString();
        } else {
            this.jobId = jobId;
        }
        this.jobState = jobState;
        this.jobTemp = jobTemp;
        this.job = job;
    }

}
