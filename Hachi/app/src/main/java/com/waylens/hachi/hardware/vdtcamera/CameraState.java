package com.waylens.hachi.hardware.vdtcamera;

public class CameraState {
    public static final String TAG = CameraState.class.getSimpleName();




    public interface OnStateChangeListener {
        void onStateChange();
    }

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        this.mListener = listener;
    }

    private OnStateChangeListener mListener = null;


    private boolean mbIsStill = false;
    private int mRecordDuration = -1;
    private boolean mbRecordDurationUpdated; //
    private long mRecordTimeFetchedTime;





    private void notifyStateChanged() {
        if (mListener != null) {
            mListener.onStateChange();
        }
    }







}
