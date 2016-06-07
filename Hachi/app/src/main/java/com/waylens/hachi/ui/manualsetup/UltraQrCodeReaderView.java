package com.waylens.hachi.ui.manualsetup;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2016/5/9.
 */
public class UltraQrCodeReaderView extends QRCodeReaderView {
    private static final String TAG = UltraQrCodeReaderView.class.getSimpleName();

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


//        Rect rect = new Rect(0, 0, 500, 500);
//        Camera.Area area = new Camera.Area(rect, 0);
//        List<Camera.Area> focusArea = new ArrayList<>();
//        focusArea.add(area);
//        parameters.setFocusAreas(focusArea);

        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);


        parameters.setExposureCompensation(minExposure);
        getCameraManager().getCamera().setParameters(parameters);
    }



}
