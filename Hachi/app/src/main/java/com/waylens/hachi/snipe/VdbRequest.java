package com.waylens.hachi.snipe;

/**
 * Created by Xiaofei on 2015/8/17.
 */
public abstract class VdbRequest<T> implements Comparable<VdbRequest<T>> {
    private final int mMethod;
    private final VdbResponse.ErrorListener mErrorListener;

    private Integer mSequence;

    private boolean mCanceled = false;
    private boolean mResponseDelivered = false;

    private VdbRequestQueue mVdbRequestQueue;

    public VdbRequest(int method, VdbResponse.ErrorListener listener) {
        this.mMethod = method;
        this.mErrorListener = listener;
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

    public void cancel() {
        mCanceled = true;
    }

    public boolean isCanceled() {
        return mCanceled;
    }


    void finish(final String tag) {
        if (mVdbRequestQueue != null) {
            mVdbRequestQueue.finish(this);
        }
    }

    public VdbRequest<?> setRequestQueue(VdbRequestQueue vdbRequestQueue) {
        mVdbRequestQueue = vdbRequestQueue;
        return this;
    }

    public final VdbRequest<?> setSequence(int sequence) {
        mSequence = sequence;
        return this;
    }


    public void markDelivered() {
        mResponseDelivered  = true;
    }

    public boolean hasHadResponseDelivered() {
        return mResponseDelivered;
    }


    abstract protected VdbResponse<T> parseVdbResponse(RawResponse response);

    @Override
    public int compareTo(VdbRequest<T> another) {
        Priority left = this.getPriority();
        Priority right = another.getPriority();

        return left == right ? this.mSequence - another.mSequence : right.ordinal() - left
            .ordinal();

    }
}
