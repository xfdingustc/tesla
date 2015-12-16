package com.transee.viditcam.app;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.transee.ccam.BtState;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.transee.viditcam.actions.DialogBuilder;
import com.waylens.hachi.R;

public class CameraBtSetupActivity extends BaseActivity {

    static final boolean DEBUG = false;
    static final String TAG = "CameraBtSetupActivity";

    static final boolean ENABLE_OBD = true;

    static class BtItem {
        int type;
        String mac;
        String name;

        public BtItem(int type, String mac, String name) {
            this.type = type;
            this.mac = mac;
            this.name = name;
        }
    }

    private VdtCamera mVdtCamera;

    private CheckBox mCBEnableBt;
    private ProgressBar mProgressBar1;
    private View mBtViews;

    static class BtDevice {
        TextView mTextName;
        TextView mTextMac;
        TextView mTextState;
    }

    private BtDevice mHidDevice = new BtDevice();
    private BtDevice mObdDevice = new BtDevice();

    private ProgressBar mProgressBar2;

    private ViewGroup mBtDevListView;
    private View mLineDevListView;
    private int mListStartIndex;

    private int mColorHighlight;
    private int mColorDisabled;


    @Override
    protected void requestContentView() {
        setContentView(R.layout.activity_camera_bt_setup);
    }

    @Override
    protected void onCreateActivity(Bundle savedInstanceState) {
        Resources res = getResources();
        mColorHighlight = res.getColor(R.color.pref_value_text);
        mColorDisabled = res.getColor(R.color.pref_hint_text);

        View layout = findViewById(R.id.layoutEnableBt);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickEnableBt();
            }
        });
        mCBEnableBt = (CheckBox) layout.findViewById(R.id.checkBoxEnableBt);
        mProgressBar1 = (ProgressBar) layout.findViewById(R.id.progressBar1);

        mBtViews = findViewById(R.id.btViews);

        layout = mBtViews.findViewById(R.id.remoteController);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickRemoteController();
            }
        });
        mHidDevice.mTextName = (TextView) layout.findViewById(R.id.textRemoteController);
        mHidDevice.mTextState = (TextView) layout.findViewById(R.id.textRemoteControllerState);
        mHidDevice.mTextMac = (TextView) layout.findViewById(R.id.textRemoteControllerMac);

        layout = mBtViews.findViewById(R.id.obdDevice);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickObdDevice();
            }
        });
        mObdDevice.mTextName = (TextView) layout.findViewById(R.id.textObdDevice);
        mObdDevice.mTextState = (TextView) layout.findViewById(R.id.textObdDeviceState);
        mObdDevice.mTextMac = (TextView) layout.findViewById(R.id.textObdDeviceMac);

        if (!ENABLE_OBD) {
            layout.setVisibility(View.GONE);
            findViewById(R.id.lineObdDevice).setVisibility(View.GONE);
        }

        layout = mBtViews.findViewById(R.id.scanBtDevice);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickScanBtDevice();
            }
        });
        mProgressBar2 = (ProgressBar) layout.findViewById(R.id.progressBar2);

        mBtDevListView = (ViewGroup) mBtViews.findViewById(R.id.btDevList);
        mBtDevListView.setVisibility(View.GONE);

        mLineDevListView = mBtViews.findViewById(R.id.lineBtDevList);
        mLineDevListView.setVisibility(View.GONE);

        mListStartIndex = mBtDevListView.getChildCount();
    }

    @Override
    protected void onStartActivity() {
        mVdtCamera = getCameraFromIntent(null);
        if (mVdtCamera == null) {
            noCamera();
            return;
        }

        //mVdtCamera.addCallback(mCameraCallback);
        updateBtState();
    }

    @Override
    protected void onStopActivity() {
        removeCamera();
    }

    private void onClickEnableBt() {
        if (mVdtCamera != null) {
            BtState states = mVdtCamera.getBtStates();
            if (states.canEnableBt()) {
                boolean bEnable = states.mBtState == BtState.BT_State_Disabled;
                mVdtCamera.setBtEnable(bEnable);
                // force refresh
                states.enableBt(bEnable);
                updateBtEnabled();
            }
        }
    }

    private void onClickRemoteController() {
        BtState states = mVdtCamera.getBtStates();
        unbindDevice(states, BtState.BT_Type_HID, states.mHidState);
    }

    private void onClickObdDevice() {
        BtState states = mVdtCamera.getBtStates();
        unbindDevice(states, BtState.BT_Type_OBD, states.mObdState);
    }

    private void unbindDevice(BtState states, final int type, final BtState.BtDevState devState) {
        if (!states.canOperate() || devState.mMac.length() == 0) {
            return;
        }

        if (mVdtCamera != null) {
            DialogBuilder builder = new DialogBuilder(this) {
                @Override
                protected void onClickPositiveButton() {
                    unbindBtDevice(type, devState.mMac);
                }
            };
            builder.setTitle(R.string.title_unbind_bt);
            String info = devState.mName + " (" + devState.mMac + ")";
            builder.setMsg(info);
            builder.setButtons(DialogBuilder.DLG_OK_CANCEL);
            builder.show();
        }
    }

    private void onClickScanBtDevice() {
        if (mVdtCamera != null) {
            BtState states = mVdtCamera.getBtStates();
            if (states.canOperate()) {
                if (states.mBtState == BtState.BT_State_Disabled) {
                    DialogBuilder action = new DialogBuilder(this);
                    action.setMsg(R.string.msg_hint_enable_bt);
                    action.setButtons(DialogBuilder.DLG_OK);
                    action.show();
                    return;
                }
                clearAllDevices();
                mVdtCamera.doBtScan();
                states.scanBt();
                updateBtScanning();
            }
        }
    }

    private void updateBtEnabled() {
        BtState states = mVdtCamera.getBtStates();
        boolean bEnabled = states.mBtState == BtState.BT_State_Enabled;
        mCBEnableBt.setChecked(bEnabled);
        mProgressBar1.setVisibility(states.mbBtEnabling ? View.VISIBLE : View.GONE);
        mBtViews.setVisibility(bEnabled ? View.VISIBLE : View.GONE);
        if (!bEnabled) {
            clearAllDevices();
        }
    }

    private void setTextAndColor(TextView textView, int text, int color) {
        textView.setText(text);
        textView.setTextColor(color);
    }

    private void setTextAndColor(TextView textView, String text, int color) {
        textView.setText(text);
        textView.setTextColor(color);
    }

    private void updateDeviceState(BtState.BtDevState devState, BtDevice device) {
        switch (devState.mState) {
            default:
            case BtState.BTDEV_State_Off:
                setTextAndColor(device.mTextName, R.string.lable_bt_unbound, mColorDisabled);
                setTextAndColor(device.mTextState, "", mColorDisabled);
                device.mTextMac.setVisibility(View.GONE);
                break;
            case BtState.BTDEV_State_On:
                setTextAndColor(device.mTextName, devState.mName, mColorHighlight);
                setTextAndColor(device.mTextState, R.string.lable_bt_connected, mColorHighlight);
                setTextAndColor(device.mTextMac, devState.mMac, mColorDisabled);
                device.mTextMac.setVisibility(View.VISIBLE);
                break;
            case BtState.BTDEV_State_Busy:
            case BtState.BTDEV_State_Wait:
                setTextAndColor(device.mTextName, devState.mName, mColorHighlight);
                setTextAndColor(device.mTextState, R.string.lable_bt_disconnected, mColorDisabled);
                setTextAndColor(device.mTextMac, devState.mMac, mColorDisabled);
                device.mTextMac.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateBtScanning() {
        BtState states = mVdtCamera.getBtStates();
        mProgressBar2.setVisibility(states.mbBtScanning ? View.VISIBLE : View.GONE);
    }

    private void updateBtState() {
        updateBtEnabled();
        BtState states = mVdtCamera.getBtStates();
        updateDeviceState(states.mHidState, mHidDevice);
        if (ENABLE_OBD) {
            updateDeviceState(states.mObdState, mObdDevice);
        }
        updateBtScanning();
    }

    private void fetchScanResult() {
        if (mVdtCamera != null) {
            mVdtCamera.getBtHostNumber();

        }
    }

    final private void unbindBtDevice(int type, String mac) {
        if (mVdtCamera != null) {
            mVdtCamera.doBtUnbind(type, mac);
        }
    }

    final private void bindBtDevice(BtItem item) {
        if (mVdtCamera != null) {
            mVdtCamera.doBtUnbind(item.type, item.mac);
        }
    }

    private void onClickBtDevItem(View view) {
        if (mVdtCamera == null) {
            return;
        }

        final BtItem item = (BtItem) view.getTag();

        BtState states = mVdtCamera.getBtStates();
        if (states.isDeviceBound(item.type)) {
            DialogBuilder builder = new DialogBuilder(this);
            if (states.isDeviceBound(item.type, item.mac)) {
                // already bound
                builder.setMsg(item.name + "\r\n" + item.mac);
                builder.setTitle(R.string.msg_bt_dev_is_bound);
            } else {
                // should unbind first
                String msg;
                if (item.type == BtState.BT_Type_HID)
                    msg = states.mHidState.mName + "\r\n" + states.mHidState.mMac;
                else
                    msg = states.mObdState.mName + "\r\n" + states.mObdState.mMac;
                builder.setMsg(msg);
                builder.setTitle(R.string.msg_bt_should_unbound);
            }
            builder.setButtons(DialogBuilder.DLG_OK);
            builder.show();
            return;
        }

        DialogBuilder builder = new DialogBuilder(this) {
            @Override
            protected void onClickPositiveButton() {
                bindBtDevice(item);
            }
        };

        builder.setTitle(R.string.title_bind_bt);
        String info = item.name + " (" + item.mac + ")";
        builder.setMsg(info);
        builder.setButtons(DialogBuilder.DLG_OK_CANCEL);
        builder.show();
    }

    @SuppressLint("InflateParams")
    private void addDevice(BtItem item) {
        LayoutInflater lf = LayoutInflater.from(this);
        View view = lf.inflate(R.layout.item_bt_dev, null);
        TextView name = (TextView) view.findViewById(R.id.textView1);
        TextView mac = (TextView) view.findViewById(R.id.textView2);
        name.setText(item.name);
        mac.setText(item.mac);
        view.setTag(item);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickBtDevItem(v);
            }
        });
        mBtDevListView.addView(view);
        if (mBtDevListView.getChildCount() == mListStartIndex + 1) {
            mBtDevListView.setVisibility(View.VISIBLE);
            mLineDevListView.setVisibility(View.VISIBLE);
        }
    }

    private void clearAllDevices() {
        int index = mListStartIndex;
        int count = mBtDevListView.getChildCount() - index;
        mBtDevListView.removeViews(index, count);
        mBtDevListView.setVisibility(View.GONE);
        mLineDevListView.setVisibility(View.GONE);
    }

    private void onBtDevInfo(int type, String mac, String name) {
        int count = mBtDevListView.getChildCount();
        for (int index = mListStartIndex; index < count; index++) {
            View view = mBtDevListView.getChildAt(index);
            BtItem item = (BtItem) view.getTag();
            if (item.mac.equals(mac)) {
                // TODO
                return;
            }
        }
        BtItem item = new BtItem(type, mac, name);
        addDevice(item);
    }

    private void removeCamera() {
        if (mVdtCamera != null) {
            //mVdtCamera.removeCallback(mCameraCallback);
            mVdtCamera = null;
        }
    }

    private void noCamera() {
        if (DEBUG) {
            Log.d(TAG, "camera not found or disconnected");
        }
        performFinish();
    }

    @Override
    public void onBackPressed() {
        performFinish();
    }

    private void performFinish() {
        finish();
    }

    /*

    private final VdtCamera.Callback mCameraCallback = new VdtCamera.Callback() {

        @Override
        public void onBtStateChanged(VdtCamera vdtCamera) {
            if (vdtCamera == mVdtCamera) {
                updateBtState();
            }
        }

        @Override
        public void onGpsStateChanged(VdtCamera vdtCamera) {

        }

        @Override
        public void onWifiStateChanged(VdtCamera vdtCamera) {

        }

        @Override
        public void onStartRecordError(VdtCamera vdtCamera, int error) {

        }

        @Override
        public void onHostSSIDFetched(VdtCamera vdtCamera, String ssid) {

        }

        @Override
        public void onScanBtDone(VdtCamera vdtCamera) {
            if (vdtCamera == mVdtCamera) {
                fetchScanResult();
            }
        }

        @Override
        public void onBtDevInfo(VdtCamera vdtCamera, int type, String mac, String name) {
            if (vdtCamera == mVdtCamera) {
                CameraBtSetupActivity.this.onBtDevInfo(type, mac, name);
            }
        }

        @Override
        public void onStillCaptureStarted(VdtCamera vdtCamera, boolean bOneShot) {

        }

        @Override
        public void onStillPictureInfo(VdtCamera vdtCamera, boolean bCapturing, int numPictures, int burstTicks) {

        }

        @Override
        public void onStillCaptureDone(VdtCamera vdtCamera) {

        }

        @Override
        public void onConnected(VdtCamera vdtCamera) {

        }

        @Override
        public void onDisconnected(VdtCamera vdtCamera) {
            if (vdtCamera == mVdtCamera) {
                removeCamera();
                noCamera();
            }
        }

        @Override
        public void onStateChanged(VdtCamera vdtCamera) {

        }
    }; */
}
