package com.waylens.hachi.ui.manualsetup;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

/**
 * Created by Xiaofei on 2016/5/9.
 */
public class UltraQrCodeReaderView extends QRCodeReaderView {
    public UltraQrCodeReaderView(Context context) {
        super(context);
    }

    public UltraQrCodeReaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
       super.surfaceCreated(holder);

        Camera.Parameters parameters = getCameraManager().getCamera().getParameters();
        int minExposure = parameters.getMinExposureCompensation();
//        parameters.getAutoExposureLock();
//        parameters.setAutoExposureLock();

        parameters.setExposureCompensation(minExposure);
        getCameraManager().getCamera().setParameters(parameters);
    }
}
