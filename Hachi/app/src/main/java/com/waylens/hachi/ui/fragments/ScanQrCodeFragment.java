package com.waylens.hachi.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PointF;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.WifiAutoConnectManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/3/14.
 */
public class ScanQrCodeFragment extends BaseFragment {
    private static final String TAG = ScanQrCodeFragment.class.getSimpleName();

    private String mWifiName;
    private String mWifiPassword;
    private WifiManager mWifiManager;

    private BroadcastReceiver mWifiStateReceiver;

    @Bind(R.id.qrDecoderView)
    QRCodeReaderView mQrCodeReaderView;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_qr_code_scan, savedInstanceState);
        initViews();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mWifiStateReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                    Logger.t(TAG).d("Network state changed " + wifiInfo.getSSID());


                } else if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                    Logger.t(TAG).d("WIFI_STATE_CHANGED_ACTION");
                }
            }
        };
        getActivity().registerReceiver(mWifiStateReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        mQrCodeReaderView.getCameraManager().stopPreview();
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mWifiStateReceiver);
    }

    private void init() {
        mWifiManager = (WifiManager)getActivity().getSystemService(Context.WIFI_SERVICE);
    }

    private void initViews() {
        mQrCodeReaderView.getCameraManager().startPreview();
        mQrCodeReaderView.setOnQRCodeReadListener(new QRCodeReaderView.OnQRCodeReadListener() {
            @Override
            public void onQRCodeRead(String text, PointF[] points) {
                if (parseWifiInfo(text) == true) {
                    mQrCodeReaderView.getCameraManager().stopPreview();
                    launchApConnectFragment();
                }
            }

            @Override
            public void cameraNotFound() {

            }

            @Override
            public void QRCodeNotFoundOnCamImage() {

            }
        });
    }

    private void launchApConnectFragment() {
        ApConnectFragment fragment = ApConnectFragment.newInstance(mWifiName, mWifiPassword);
        getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).commit();
//        WifiAutoConnectManager wifiAutoConnectManager = new WifiAutoConnectManager
//            (mWifiManager, new WifiAutoConnectManager.WifiAutoConnectListener() {
//                @Override
//                public void onAudoConnectStarted() {
//
//                }
//            });
//        wifiAutoConnectManager.connect(mWifiName, mWifiPassword, WifiAutoConnectManager
//            .WifiCipherType.WIFICIPHER_WPA);
    }


//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
//        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
//        //registerReceiver(mWifiStateReceiver, filter);
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        //unregisterReceiver(mWifiStateReceiver);
//    }

//    @Override
//    protected void init() {
//        super.init();
//
//        mWifiStateReceiver = new BroadcastReceiver() {
//
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
//
//                    WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
//                    Logger.t(TAG).d("Network state changed " + wifiInfo.getSSID());
//                } else if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
//                    Logger.t(TAG).d("WIFI_STATE_CHANGED_ACTION");
//                }
//            }
//        };
//        initViews();
//    }


    private boolean parseWifiInfo(String result) {
        Pattern pattern = Pattern.compile("<a>(\\w*)</a>.*<p>(\\w*)</?p>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

        Matcher matcher = pattern.matcher(result);
        if (matcher.find() && matcher.groupCount() == 2) {
            mWifiName = matcher.group(1);
            mWifiPassword = matcher.group(2);

            return true;
        }
        return false;
    }


}
