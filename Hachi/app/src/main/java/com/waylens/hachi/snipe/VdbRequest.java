package com.waylens.hachi.snipe;


import com.transee.vdb.Vdb;

/**
 * Created by Xiaofei on 2015/8/17.
 */
public abstract class VdbRequest<T> implements Comparable<VdbRequest<T>> {
    protected int mMethod;
    private VdbResponse.Listener<T> mListener;

    private VdbResponse.ErrorListener mErrorListener;

    private Integer mSequence;

    private boolean mCanceled = false;
    private boolean mResponseDelivered = false;

    private VdbRequestQueue mVdbRequestQueue;

    private static final int REQUEST_TYPE_NULL = 0;
    private static final int REQUEST_TYPE_GETVERSIONINFO = 1;
    private static final int REQUEST_TYPE_GETCLIPSETINFO = 2;
    private static final int REQUEST_TYPE_GETINDEXPIC = 3;
    private static final int REQUEST_TYPE_GETPLAYBACKURL = 4;
    // private static final int CMD_GetDownloadUrl = 5; // obsolete
    private static final int REQUEST_TYPE_MARKCLIP = 6;
    // private static final int CMD_GetCopyState = 7; // obsolete
    private static final int REQUEST_TYPE_DELETECLIP = 8;
    private static final int REQUEST_TYPE_GETRAWDATA = 9;
    private static final int REQUEST_TYPE_SETRAWDATAOPTION = 10;
    private static final int REQUEST_TYPE_GETRAWDATABLOCK = 11;
    private static final int REQUEST_TYPE_GETDOWNLOADURLEX = 12;

    private static final int REQUEST_TYPE_GETALLPLAYLISTS = 13;
    private static final int REQUEST_TYPE_GETPLAYLISTINDEXPIC = 14;
    private static final int REQUEST_TYPE_CLEARPLAYLIST = 15;
    private static final int REQUEST_TYPE_INSERTCLIP = 16;
    private static final int REQUEST_TYPE_MOVECLIP = 17;
    private static final int REQUEST_TYPE_GETPLAYLISTPLAYBACKURL = 18;
    protected VdbCommand mVdbCommand;

    private boolean mIsIgnorable;

    private Object mTag;

    public VdbRequest(int method, VdbResponse.Listener<T> listener, VdbResponse.ErrorListener errorListener) {
        this.mMethod = method;
        this.mListener = listener;
        this.mErrorListener = errorListener;
    }

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        IMMEDIATE
    }

    public Priority getPriority() {
        return Priority.NORMAL;
    }

    public void addMarker(String tag) {
        // TODO: implement me
    }

    public VdbRequest<T> setTag(Object tag) {
        mTag = tag;
        return this;
    }

    public Object getTag() {
        return mTag;
    }

    public void cancel() {
        mCanceled = true;
    }

    public boolean isCanceled() {
        return mCanceled;
    }


    void finish(final String tag, boolean shouldClean) {
        if (mVdbRequestQueue != null) {
            mVdbRequestQueue.finish(this);
        }
        if (shouldClean) {
            clean();
        }
    }

    public VdbRequest<?> setRequestQueue(VdbRequestQueue vdbRequestQueue) {
        mVdbRequestQueue = vdbRequestQueue;
        return this;
    }

    public VdbConnection getVdbConnection() {
        return Snipe.getVdbConnect();
    }

    public final VdbRequest<?> setSequence(int sequence) {
        mSequence = sequence;
        return this;
    }


    public void markDelivered() {
        mResponseDelivered = true;
    }

    public boolean hasHadResponseDelivered() {
        return mResponseDelivered;
    }


    abstract protected VdbCommand createVdbCommand();

    public VdbCommand getVdbCommand() {
        return mVdbCommand;
    }

    abstract protected VdbResponse<T> parseVdbResponse(VdbAcknowledge response);

    protected SnipeError parseVdbError(SnipeError snipeError) {
        return snipeError;
    }


    final protected void deliverResponse(T response) {
        if (mListener != null) {
            mListener.onResponse(response);
        }
        clean();
    }


    protected void deliverError(SnipeError error) {
        if (mErrorListener != null) {
            mErrorListener.onErrorResponse(error);
        }
        clean();
    }

    @Override
    public int compareTo(VdbRequest<T> another) {
        Priority left = this.getPriority();
        Priority right = another.getPriority();

        return left == right ? this.mSequence - another.mSequence : right.ordinal() - left
                .ordinal();

    }

    public boolean isIgnorable() {
        return mIsIgnorable;
    }

    public void setIgnorable(boolean isIgnorable) {
        mIsIgnorable = isIgnorable;
    }

    void clean() {
        mListener = null;
        mErrorListener = null;
        mVdbCommand = null;
    }
}
