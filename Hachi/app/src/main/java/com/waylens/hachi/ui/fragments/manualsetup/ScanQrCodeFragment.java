package com.waylens.hachi.ui.fragments.manualsetup;

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

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.fragments.BaseFragment;

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


    @Bind(R.id.cropWindow)
    FrameLayout mCropWindow;

    @Bind(R.id.qrDecoderView)
    QRCodeReaderView mQrCodeReaderView;

    @Bind(R.id.scanLine)
    ImageView mScanLine;


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


    }

    @Override
    public void onPause() {
        super.onPause();
        mQrCodeReaderView.getCameraManager().stopPreview();
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
