package com.waylens.hachi.bgjob.export.event;

import com.waylens.hachi.bgjob.export.ExportableJob;

/**
 * Created by Xiaofei on 2016/5/5.
 */
public class ExportEvent {
    public static final int EXPORT_WHAT_JOB_ADDED = 1;
    public static final int EXPORT_WHAT_START = 2;
    public static final int EXPORT_WHAT_PROGRESS = 3;
    public static final int EXPORT_WHAT_FINISHED = 4;
    public static final int EXPORT_WHAT_ERROR = 5;
    public static final int EXPORT_WHAT_CANCELLED = 6;



    private final int mWhat;
    private final ExportableJob mJob;

    private final int mIndex;


    public ExportEvent(int what, ExportableJob job) {
        this(what, job, 0);
    }

    public ExportEvent(int what, ExportableJob job, int index) {
        this.mWhat = what;
        this.mJob = job;
        this.mIndex = index;
    }

    public int getWhat() {
        return mWhat;
    }

    public ExportableJob getJob() {
        return mJob;
    }

    public int getIndex() {
        return mIndex;
    }
}
