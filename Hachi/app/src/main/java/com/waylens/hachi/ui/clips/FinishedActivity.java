package com.waylens.hachi.ui.clips;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.utils.PrettyTimeUtils;

import java.io.File;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/11/25.
 */

public class FinishedActivity extends BaseActivity {
    private static final String TAG = FinishedActivity.class.getSimpleName();
    private static final String EXTRA_OUTPUT_FILE = "outputfile";

    private String mOutputFile;

    public static void launch(Activity activity, String outputFile) {
        Intent intent = new Intent(activity, FinishedActivity.class);
        intent.putExtra(EXTRA_OUTPUT_FILE, outputFile);
        activity.startActivity(intent);
    }

    @BindView(R.id.clip_cover)
    ImageView clipCover;

    @BindView(R.id.clip_title)
    TextView clipTitle;

    @BindView(R.id.clip_time)
    TextView tvClipTime;

    @BindView(R.id.fab)
    FloatingActionButton mShareFab;

    @OnClick(R.id.fab)
    public void onShareFabClicked() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, (Uri.fromFile(new File(mOutputFile))));
        intent.setType("video/mp4");
        startActivity(Intent.createChooser(intent, getResources().getText(R.string.share)));
    }

    @OnClick(R.id.clip_cover)
    public void onClipCoverClicked() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(mOutputFile)), "video/mp4");
        startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Override
    protected void init() {
        super.init();
        mOutputFile = getIntent().getStringExtra(EXTRA_OUTPUT_FILE);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_finish);
        getToolbar().setTitle(R.string.finished);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        File file = new File(mOutputFile);

        tvClipTime.setText(PrettyTimeUtils.getTimeAgo(file.lastModified()));

        Glide.with(this)
            .loadFromMediaStore(Uri.fromFile(file))
            .crossFade()
            .into(clipCover);

        clipTitle.setText(file.getName());
    }
}
