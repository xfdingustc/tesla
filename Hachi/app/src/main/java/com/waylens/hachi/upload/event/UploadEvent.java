package com.waylens.hachi.upload.event;

/**
 * Created by Xiaofei on 2016/4/28.
 */
public class UploadEvent {

    public static final int UPLOAD_WHAT_START = 0;
    public static final int UPLOAD_WHAT_PROGRESS = 1;
    public static final int UPLOAD_WHAT_FINISHED = 2;
    public static final int UPLOAD_WHAT_ERROR = 3;

    public static final int UPLOAD_ERROR_UNKNOWN = 0x100;
    public static final int UPLOAD_ERROR_LOGIN = 0x101;


    private final int mWhat;
    private final int mExtra;


    public UploadEvent(int what) {
        this(what, 0);
    }

    public UploadEvent(int what, int extra) {
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
