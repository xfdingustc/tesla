package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.BtDevice;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.events.BluetoothScanEvent;
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


    @BindView(R.id.scan_mask)
    FrameLayout mScanMask;

    @BindView(R.id.bt_switch)
    Switch mBtSwitch;

    @BindView(R.id.obd_device_list)
    RecyclerView mRvObdDeviceList;

    @BindView(R.id.remote_ctrl_device_list)
    RecyclerView mRvRemoteCtrlList;

    private List<BtDevice> mObdDeviceList = new ArrayList<>();
    private List<BtDevice> mRemoteCtrlDeviceList = new ArrayList<>();

    private BtDeviceListAdapter mObdDeviceListAdapter;
    private BtDeviceListAdapter mRemoteCtrlDeviceListAdapter;

    @OnClick(R.id.bt_switch)
    public void onBtnSwitchClicked() {
//        mBtSwitch.isChecked()
    }


    @OnClick(R.id.scan_bt)
    public void onBtnScanBluetoothClicked() {
        if (mVdtCamera == null) {
            return;
        }

        mVdtCamera.scanBluetoothDevices();
        mScanMask.setVisibility(View.VISIBLE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBluetoothEvent(BluetoothScanEvent event) {
        List<BtDevice> devices = event.getDevices();

        mObdDeviceList.clear();
        mRemoteCtrlDeviceList.clear();
        for (BtDevice device : devices) {
            Logger.t(TAG).d("Devices: " + device.toString());
            if (device.getType() == BtDevice.BtDeviceType.BT_DEVICE_TYPE_OBD) {
                mObdDeviceList.add(device);
            } else if (device.getType() == BtDevice.BtDeviceType.BT_DEVICE_TYPE_REMOTE_CTR) {
                mRemoteCtrlDeviceList.add(device);
            }
        }

        mRemoteCtrlDeviceListAdapter.setDeviceList(mRemoteCtrlDeviceList);
        mObdDeviceListAdapter.setDeviceList(mObdDeviceList);

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
        mBtSwitch.setChecked(mVdtCamera.getBtState() == VdtCamera.BT_STATE_ENABLED ? true : false);

        mRvObdDeviceList.setLayoutManager(new LinearLayoutManager(this));
        mRvRemoteCtrlList.setLayoutManager(new LinearLayoutManager(this));

        mObdDeviceListAdapter = new BtDeviceListAdapter(mObdDeviceList);
        mRemoteCtrlDeviceListAdapter = new BtDeviceListAdapter(mRemoteCtrlDeviceList);

        mRvObdDeviceList.setAdapter(mObdDeviceListAdapter);
        mRvRemoteCtrlList.setAdapter(mRemoteCtrlDeviceListAdapter);

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
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            BtDevice btDevice = mDeviceList.get(position);
            BtDeviceListViewHolder viewHolder = (BtDeviceListViewHolder)holder;
            viewHolder.deviceMac.setText(btDevice.getMac());
            viewHolder.deviceName.setText(btDevice.getName());
        }

        @Override
        public int getItemCount() {
            int ret =  mDeviceList == null ? 0 : mDeviceList.size();
            Logger.t(TAG).d("item count: " + ret);
            return ret;
        }
    }


    public static class BtDeviceListViewHolder extends RecyclerView.ViewHolder {

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
