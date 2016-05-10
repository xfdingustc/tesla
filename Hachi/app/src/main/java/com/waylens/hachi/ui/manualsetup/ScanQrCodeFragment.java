package com.waylens.hachi.ui.manualsetup;

import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.fragments.BaseFragment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.OnClick;


/**
 * Created by Xiaofei on 2016/3/14.
 */
public class ScanQrCodeFragment extends BaseFragment {
    private static final String TAG = ScanQrCodeFragment.class.getSimpleName();

    private static final int WIFI_SETTING = 0;

    private String mWifiName;
    private String mWifiPassword;


    @BindView(R.id.cropWindow)
    FrameLayout mCropWindow;

    @BindView(R.id.qrDecoderView)
    UltraQrCodeReaderView mQrCodeReaderView;

    @BindView(R.id.scanLine)
    ImageView mScanLine;

    @BindView(R.id.manualSelectWifi)
    TextView mManualSelectWifi;

    @OnClick(R.id.manualSelectWifi)
    public void onManualSelectWifiClick() {
        startActivityForResult(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS), WIFI_SETTING);
    }


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
    public void onPause() {
        super.onPause();
        mQrCodeReaderView.getCameraManager().stopPreview();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == WIFI_SETTING) {
            launchApConnectFragment();
        }
        Logger.t(TAG).d("requestCode: " + requestCode + " resultCode: " + resultCode + " data: " + data);
    }

    private void init() {

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

        mCropWindow.post(new Runnable() {
            @Override
            public void run() {
                TranslateAnimation animation = new TranslateAnimation(0, 0, 0, mCropWindow.getMeasuredHeight());
                animation.setRepeatCount(-1);
                animation.setRepeatMode(Animation.RESTART);
                animation.setInterpolator(new LinearInterpolator());
                animation.setDuration(1200);
                mScanLine.startAnimation(animation);
            }
        });



    }

    private void launchApConnectFragment() {
        ApConnectFragment fragment = ApConnectFragment.newInstance(mWifiName, mWifiPassword);
        getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).commit();

    }


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
