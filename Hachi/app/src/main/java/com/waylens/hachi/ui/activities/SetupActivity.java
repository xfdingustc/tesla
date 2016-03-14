package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.waylens.hachi.R;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/3/14.
 */
public class SetupActivity extends BaseActivity {

    @Bind(R.id.qrDecoderView)
    QRCodeReaderView mQrCodeReaderView;

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, SetupActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_setup);
        mQrCodeReaderView.getCameraManager().startPreview();
        mQrCodeReaderView.setOnQRCodeReadListener(new QRCodeReaderView.OnQRCodeReadListener() {
            @Override
            public void onQRCodeRead(String text, PointF[] points) {
                Toast.makeText(SetupActivity.this, text, Toast.LENGTH_LONG).show();
            }

            @Override
            public void cameraNotFound() {

            }

            @Override
            public void QRCodeNotFoundOnCamImage() {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mQrCodeReaderView.getCameraManager().stopPreview();
    }
}
