package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;

import com.waylens.hachi.camera.BtDevice;
import com.waylens.hachi.camera.VdtCamera;
import com.waylens.hachi.camera.events.BluetoothEvent;
import com.waylens.hachi.camera.events.CameraStateChangeEvent;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.utils.rxjava.RxBus;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by Xiaofei on 2016/5/11.
 */
public class BluetoothSettingActivity extends BaseActivity {
    private static final String TAG = BluetoothSettingActivity.class.getSimpleName();
    private EventBus mEventBus = EventBus.getDefault();

    private List<BtDevice> mObdDeviceList = new ArrayList<>();
    private List<BtDevice> mRemoteCtrlDeviceList = new ArrayList<>();

    private BtDeviceListAdapter mObdDeviceListAdapter;
    private BtDeviceListAdapter mRemoteCtrlDeviceListAdapter;

    private Subscription mCameraStateChangeEventSubscription;

    @BindView(R.id.obd_name)
    TextView mObdName;

    @BindView(R.id.obd_mac)
    TextView mObdMac;

    @BindView(R.id.obd_status)
    TextView mObdStatus;

    @BindView(R.id.remote_ctrl_name)
    TextView mRemoteCtrlName;

    @BindView(R.id.remote_ctrl_mac)
    TextView mRemoteCtrlMac;

    @BindView(R.id.remote_ctrl_status)
    TextView mRemoteCtrlStatus;

    @BindView(R.id.scan_mask)
    FrameLayout mScanMask;

    @BindView(R.id.bt_switch)
    Switch mBtSwitch;

    @BindView(R.id.obd_device_list)
    RecyclerView mRvObdDeviceList;

    @BindView(R.id.remote_ctrl_device_list)
    RecyclerView mRvRemoteCtrlList;

    @BindView(R.id.bt_content)
    View mBtContent;

    @BindView(R.id.scan_result)
    LinearLayout mScanrResult;

    @BindView(R.id.obd_scan_result)
    LinearLayout mObdScanResult;

    @BindView(R.id.rc_scan_result)
    LinearLayout mRcScanResult;


    @OnClick(R.id.obd_content)
    public void onObdNameClicked() {
        BtDevice obdDevice = mVdtCamera.getObdDevice();
        if (!TextUtils.isEmpty(obdDevice.getMac())) {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .title(R.string.unbind_obd_device)
                    .content(obdDevice.getName() + " " + obdDevice.getMac())
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            doUnbindDevice(mVdtCamera.getObdDevice());
                        }
                    }).show();
        }

    }

    @OnClick(R.id.rc_content)
    public void onRemoteCtrlNameClicked() {
        BtDevice remoteCtrlDevice = mVdtCamera.getRemoteCtrlDevice();
        if (!TextUtils.isEmpty(remoteCtrlDevice.getMac())) {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .title(R.string.unbind_rc_device)
                    .content(remoteCtrlDevice.getName() + remoteCtrlDevice.getMac())
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            doUnbindDevice(mVdtCamera.getRemoteCtrlDevice());
                        }
                    }).show();
        }
    }

    @OnClick(R.id.bt_switch)
    public void onBtnSwitchClicked() {
//      Logger.t(TAG).d("bt switch is : " + mBtSwitch.isChecked());
        mVdtCamera.getRemoteCtrlDevice().setState(BtDevice.BT_DEVICE_STATE_UNKNOWN);
        mVdtCamera.getObdDevice().setState(BtDevice.BT_DEVICE_STATE_UNKNOWN);
        mVdtCamera.setBtEnable(mBtSwitch.isChecked());
        mVdtCamera.getIsBtEnabled();
        updateBtContent();

    }

    private void updateBtContent() {
        if (mBtSwitch.isChecked()) {
            mBtContent.setVisibility(View.VISIBLE);
            refreshBtDevices();
        } else {
            mBtContent.setVisibility(View.GONE);
        }
    }


    @OnClick(R.id.scan_bt)
    public void onBtnScanBluetoothClicked() {
        if (mVdtCamera == null) {
            return;
        }
        mObdScanResult.setVisibility(View.GONE);
        mRcScanResult.setVisibility(View.GONE);
        mVdtCamera.scanBluetoothDevices();
        mScanMask.setVisibility(View.VISIBLE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBluetoothEvent(BluetoothEvent event) {
        switch (event.getWhat()) {
            case BluetoothEvent.BT_SCAN_COMPLETE:
                List<BtDevice> devices = (List<BtDevice>) event.getExtra();
                Logger.t(TAG).d("find " + devices.size() + " bluetooth devices");
                mObdDeviceList.clear();
                mRemoteCtrlDeviceList.clear();
                for (BtDevice device : devices) {
                    Logger.t(TAG).d("Devices: " + device.toString());
                    if (device.getType() == BtDevice.BT_DEVICE_TYPE_OBD) {
                        mObdDeviceList.add(device);
                    } else if (device.getType() == BtDevice.BT_DEVICE_TYPE_REMOTE_CTR) {
                        mRemoteCtrlDeviceList.add(device);
                    }
                }
                if (mObdDeviceList.size() > 0) {
                    mObdScanResult.setVisibility(View.VISIBLE);
                }
                if (mRemoteCtrlDeviceList.size() > 0) {
                    mRcScanResult.setVisibility(View.VISIBLE);
                }
                mRemoteCtrlDeviceListAdapter.setDeviceList(mRemoteCtrlDeviceList);
                mObdDeviceListAdapter.setDeviceList(mObdDeviceList);
                break;
            case BluetoothEvent.BT_DEVICE_BIND_FINISHED:
            case BluetoothEvent.BT_DEVICE_UNBIND_FINISHED:
//            case BluetoothEvent.BT_DEVICE_STATUS_CHANGED:
                refreshBtDevices();
                break;
            default:
                break;
        }


        mScanMask.setVisibility(View.GONE);
    }

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, BluetoothSettingActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mObdScanResult.setVisibility(View.GONE);
        mRcScanResult.setVisibility(View.GONE);
        mEventBus.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mEventBus.unregister(this);
        if (!mCameraStateChangeEventSubscription.isUnsubscribed()) {
            mCameraStateChangeEventSubscription.unsubscribe();
        }
    }

    @Override
    protected void init() {
        super.init();
        initViews();
        mCameraStateChangeEventSubscription = RxBus.getDefault().toObserverable(CameraStateChangeEvent.class)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleSubscribe<CameraStateChangeEvent>() {
                    @Override
                    public void onNext(CameraStateChangeEvent cameraStateChangeEvent) {
                        onHandleCameraStateChangeEvent(cameraStateChangeEvent);
                    }
                });
    }

    private void initViews() {
        setContentView(R.layout.activity_bt_setting);
        setupToolbar();
        mBtSwitch.setChecked(mVdtCamera.getBtState() == VdtCamera.BT_STATE_ENABLED);
        updateBtContent();

        refreshBtDevices();
        mRvObdDeviceList.setLayoutManager(new LinearLayoutManager(this));
        mRvRemoteCtrlList.setLayoutManager(new LinearLayoutManager(this));

        mObdScanResult.setVisibility(View.GONE);
        mRcScanResult.setVisibility(View.GONE);

        mObdDeviceListAdapter = new BtDeviceListAdapter(mObdDeviceList, BtDevice.BT_DEVICE_TYPE_OBD);
        mRemoteCtrlDeviceListAdapter = new BtDeviceListAdapter(mRemoteCtrlDeviceList, BtDevice.BT_DEVICE_TYPE_REMOTE_CTR);

        mRvObdDeviceList.setAdapter(mObdDeviceListAdapter);
        mRvRemoteCtrlList.setAdapter(mRemoteCtrlDeviceListAdapter);

    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        getToolbar().setTitle(R.string.bt);
    }

    private void refreshBtDevices() {

        BtDevice obdDevice = mVdtCamera.getObdDevice();
        if (!TextUtils.isEmpty(obdDevice.getName())) {
            mObdName.setText(obdDevice.getName());
            mObdMac.setText(obdDevice.getMac());
            switch (obdDevice.getState()) {
                case BtDevice.BT_DEVICE_STATE_BUSY:
                    mObdStatus.setText(getResources().getString(R.string.busy));
                    break;
                case BtDevice.BT_DEVICE_STATE_OFF:
                    mObdStatus.setText(getResources().getString(R.string.not_connected));
                    break;
                case BtDevice.BT_DEVICE_STATE_ON:
                    mObdStatus.setText(getResources().getString(R.string.connected));
                    break;
                default:
                    mObdStatus.setText(getResources().getString(R.string.not_connected));
                    break;
            }
        } else {
            mObdName.setText("");
            mObdMac.setText("");
            mObdStatus.setText(R.string.na);
        }

        BtDevice remoteCtrlDevice = mVdtCamera.getRemoteCtrlDevice();
        if (!TextUtils.isEmpty(remoteCtrlDevice.getName())) {
            mRemoteCtrlName.setText(remoteCtrlDevice.getName());
            mRemoteCtrlMac.setText(remoteCtrlDevice.getMac());
            switch (remoteCtrlDevice.getState()) {
                case BtDevice.BT_DEVICE_STATE_BUSY:
                    mRemoteCtrlStatus.setText(getResources().getString(R.string.busy));
                    break;
                case BtDevice.BT_DEVICE_STATE_OFF:
                    mRemoteCtrlStatus.setText(getResources().getString(R.string.not_connected));
                    break;
                case BtDevice.BT_DEVICE_STATE_ON:
                    mRemoteCtrlStatus.setText(getResources().getString(R.string.connected));
                    break;
                default:
                    mRemoteCtrlStatus.setText(getResources().getString(R.string.not_connected));
                    break;
            }
        } else {
            mRemoteCtrlName.setText("");
            mRemoteCtrlMac.setText("");
            mRemoteCtrlStatus.setText(R.string.na);
        }
    }

    private void onHandleCameraStateChangeEvent(CameraStateChangeEvent event) {
        if (event.getWhat() == CameraStateChangeEvent.CAMERA_STATE_BT_DEVICE_STATUS_CHANGED) {
            refreshBtDevices();
        }
    }

    private void doBindBtDevice(BtDevice device) {
        if (device == null) {
            return;
        }
        mVdtCamera.doBind(device.getType(), device.getMac());
        mScanMask.setVisibility(View.VISIBLE);
    }

    private void doUnbindDevice(BtDevice device) {
        if (device == null) {
            return;
        }
        mVdtCamera.doBtUnbind(device.getType(), device.getMac());
        mScanMask.setVisibility(View.VISIBLE);
    }

    private class BtDeviceListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<BtDevice> mDeviceList;

        private int mBtType;

        public BtDeviceListAdapter(List<BtDevice> deviceList, int type) {
            this.mDeviceList = deviceList;
            mBtType = type;
        }

        private void setDeviceList(List<BtDevice> deviceList) {
            mDeviceList = deviceList;
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(BluetoothSettingActivity.this);
            View view = inflater.inflate(R.layout.item_bt_device, parent, false);
            return new BtDeviceListViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
            BtDevice btDevice = mDeviceList.get(position);
            BtDeviceListViewHolder viewHolder = (BtDeviceListViewHolder) holder;
            viewHolder.deviceMac.setText(btDevice.getMac());
            viewHolder.deviceName.setText(btDevice.getName());
            viewHolder.rootView.setTag(holder);
            viewHolder.rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int titleRes;
                    if (mBtType == BtDevice.BT_DEVICE_TYPE_OBD) {
                        titleRes = R.string.bind_obd_device;
                    } else {
                        titleRes = R.string.bind_rc_device;
                    }
                    final BtDevice device = mDeviceList.get(position);
                    new MaterialDialog.Builder(BluetoothSettingActivity.this)
                            .title(titleRes)
                            .content(device.getName() + " " + device.getMac())
                            .positiveText(R.string.ok)
                            .negativeText(R.string.cancel)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {

                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    if (mBtType == BtDevice.BT_DEVICE_TYPE_OBD) {
                                        doUnbindDevice(mVdtCamera.getObdDevice());
                                    } else {
                                        doUnbindDevice(mVdtCamera.getRemoteCtrlDevice());
                                    }
                                    doBindBtDevice(device);
                                }
                            })
                            .show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mDeviceList == null ? 0 : mDeviceList.size();

        }
    }


    public static class BtDeviceListViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.root_view)
        View rootView;

        @BindView(R.id.device_name)
        TextView deviceName;

        @BindView(R.id.device_mac)
        TextView deviceMac;

        public BtDeviceListViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

        }
    }
}
