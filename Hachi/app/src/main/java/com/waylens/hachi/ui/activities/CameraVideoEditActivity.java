package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.waylens.hachi.R;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2015/8/14.
 */
public class CameraVideoEditActivity extends BaseActivity {

    @Bind(R.id.ivPreviewPicture)
    ImageView mIvPreviewPicture;


    public static void launch(Context context) {
        Intent intent = new Intent(context, CameraVideoEditActivity.class);
        context.startActivity(intent);
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
        setContentView(R.layout.activity_camera_video_editor);

    }
}
