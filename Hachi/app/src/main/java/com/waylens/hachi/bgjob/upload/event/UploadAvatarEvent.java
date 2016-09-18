package com.waylens.hachi.bgjob.upload.event;

/**
 * Created by Xiaofei on 2016/7/14.
 */
public class UploadAvatarEvent {
    public static final int UPLOAD_WHAT_START = 0;
    public static final int UPLOAD_WHAT_PROGRESS = 1;
    public static final int UPLOAD_WHAT_FINISHED = 2;

    private final int mWhat;
    private final int mExtra;


    public UploadAvatarEvent(int what) {
        this(what, 0);
    }

    public UploadAvatarEvent(int what, int extra) {
        this.mWhat = what;
        this.mExtra = extra;
    }

    public int getWhat() {
        return mWhat;
    }

    public int getExtra() {
        return mExtra;
    }
}
