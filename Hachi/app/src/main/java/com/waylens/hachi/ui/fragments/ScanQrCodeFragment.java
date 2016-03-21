package com.waylens.hachi.ui.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PointF;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;

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

    @Bind(R.id.qrDecoderView)
    QRCodeReaderView mQrCodeReaderView;
    private String mWifiName;
    private String mWifiPassword;
    private WifiManager mWifiManager;
    private BroadcastReceiver mWifiStateReceiver;

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, ScanQrCodeFragment.class);
        activity.startActivity(intent);
    }

//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        init();
//    }
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
//        this.mWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
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

//    private void initViews() {
//        setContentView(R.layout.activity_setup);
//        mQrCodeReaderView.getCameraManager().startPreview();
//        mQrCodeReaderView.setOnQRCodeReadListener(new QRCodeReaderView.OnQRCodeReadListener() {
//            @Override
//            public void onQRCodeRead(String text, PointF[] points) {
//                if (parseWifiInfo(text) == true) {
//                    mQrCodeReaderView.getCameraManager().stopPreview();
//                }
//            }
//
//            @Override
//            public void cameraNotFound() {
//
//            }
//
//            @Override
//            public void QRCodeNotFoundOnCamImage() {
//
//            }
//        });
//    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        mQrCodeReaderView.getCameraManager().stopPreview();
//    }

    private boolean parseWifiInfo(String result) {
        Pattern pattern = Pattern.compile("<a>(\\w*)</a>.*<p>(\\w*)</?p>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

        Matcher matcher = pattern.matcher(result);
        if (matcher.find() && matcher.groupCount() == 2) {
            mWifiName = matcher.group(1);
            mWifiPassword = matcher.group(2);
            WifiAutoConnectManager wifiAutoConnectManager= new WifiAutoConnectManager
                (mWifiManager, new WifiAutoConnectManager.WifiAutoConnectListener() {
                    @Override
                    public void onAudoConnectStarted() {

                    }
                });
            wifiAutoConnectManager.connect(mWifiName, mWifiPassword, WifiAutoConnectManager
                .WifiCipherType.WIFICIPHER_WPA);
            return true;
        }
        return false;
    }


}
