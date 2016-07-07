package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.BtDevice;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.events.BluetoothEvent;
import com.waylens.hachi.ui.activities.BaseActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

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

    @BindView(R.id.obd_name)
    TextView mObdName;

    @BindView(R.id.remote_ctrl_name)
    TextView mRemoteCtrlName;

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



    @OnClick(R.id.obd_name)
    public void onObdNameClicked() {
        if (mVdtCamera.getObdDevice().getState() == BtDevice.BT_DEVICE_STATE_ON) {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(R.string.unbind_bt_device)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        doUnbindDevice(mVdtCamera.getObdDevice());
                    }
                }).show();

        }
    }

    @OnClick(R.id.remote_ctrl_name)
    public void onRemoteCtrlNameClicked() {
        if (mVdtCamera.getRemoteCtrlDevice().getState() == BtDevice.BT_DEVICE_STATE_ON) {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .title(R.string.unbind_bt_device)
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
        mScanrResult.setVisibility(View.GONE);
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

                mScanrResult.setVisibility(View.VISIBLE);
                mRemoteCtrlDeviceListAdapter.setDeviceList(mRemoteCtrlDeviceList);
                mObdDeviceListAdapter.setDeviceList(mObdDeviceList);
                break;
            case BluetoothEvent.BT_DEVICE_BIND_FINISHED:
            case BluetoothEvent.BT_DEVICE_UNBIND_FINISHED:
            case BluetoothEvent.BT_DEVICE_STATUS_CHANGED:
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
        mScanrResult.setVisibility(View.GONE);
        mEventBus.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mEventBus.unregister(this);
    }

    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_bt_setting);
        setupToolbar();
        mBtSwitch.setChecked(mVdtCamera.getBtState() == VdtCamera.BT_STATE_ENABLED ? true : false);
        updateBtContent();

        refreshBtDevices();

        mRvObdDeviceList.setLayoutManager(new LinearLayoutManager(this));
        mRvRemoteCtrlList.setLayoutManager(new LinearLayoutManager(this));

        mScanrResult.setVisibility(View.GONE);

        mObdDeviceListAdapter = new BtDeviceListAdapter(mObdDeviceList);
        mRemoteCtrlDeviceListAdapter = new BtDeviceListAdapter(mRemoteCtrlDeviceList);

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
        if (obdDevice.getName() != "") {
            if (obdDevice.getState() == BtDevice.BT_DEVICE_STATE_ON) {

                mObdName.setText(obdDevice.getName() + " - " + getString(R.string.connected));
            } else {
                mObdName.setText(obdDevice.getName() + " - " + getString(R.string.not_connected));
            }
        } else {
            mObdName.setText(getString(R.string.na));
        }

        BtDevice remoteCtrlDevice = mVdtCamera.getRemoteCtrlDevice();
        if (remoteCtrlDevice.getName() != "") {
            if (remoteCtrlDevice.getState() == BtDevice.BT_DEVICE_STATE_ON) {
                mRemoteCtrlName.setText(remoteCtrlDevice.getName() + " - " + getString(R.string.connected));
            } else {
                mRemoteCtrlName.setText(remoteCtrlDevice.getName() + " - " + getString(R.string.not_connected));
            }
        } else {
            mRemoteCtrlName.setText(getString(R.string.na));
        }
    }

    private void doBindBtDevice(BtDevice device) {
        mVdtCamera.doBind(device.getType(), device.getMac());
        mScanMask.setVisibility(View.VISIBLE);
    }

    private void doUnbindDevice(BtDevice device) {
        mVdtCamera.doBtUnbind(device.getType(), device.getMac());
        mScanMask.setVisibility(View.VISIBLE);
    }

    private class BtDeviceListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<BtDevice> mDeviceList;

        public BtDeviceListAdapter(List<BtDevice> deviceList) {
            this.mDeviceList = deviceList;
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
                    BtDevice device = mDeviceList.get(position);
                    doBindBtDevice(device);

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
