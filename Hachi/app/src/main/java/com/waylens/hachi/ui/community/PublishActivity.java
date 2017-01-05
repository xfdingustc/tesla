package com.waylens.hachi.ui.community;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.settings.myvideo.UploadingMomentActivity;

import butterknife.BindView;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/9/21.
 */
public class PublishActivity extends BaseActivity {
    public static final String EXTRA_PHOTO_URI = "extra.photo.uri";

    private String mPhotoUrl;

    private PictureUploader mPictureUploader;
    private MaterialDialog mUploadDialog;

    public static void launch(Activity activity, String photUri) {
        Intent intent = new Intent(activity, PublishActivity.class);
        intent.putExtra(EXTRA_PHOTO_URI, photUri);
        activity.startActivity(intent);
    }

    @BindView(R.id.ivPhoto)
    ImageView mIvPhoto;

    @BindView(R.id.title)
    EditText mTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();

    }

    @Override
    protected void init() {
        super.init();
        mPhotoUrl = getIntent().getStringExtra(EXTRA_PHOTO_URI);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_publish);
        setupToolbar();
        Glide.with(this)
            .load(mPhotoUrl)
            .crossFade()
            .into(mIvPhoto);
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.publish);
        getToolbar().inflateMenu(R.menu.menu_share);
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.share:
                        showUploadPictureDialog();
                        break;
                }

                return false;
            }
        });
    }

    private void showUploadPictureDialog() {
        mPictureUploader = new PictureUploader(mTitle.getEditableText().toString(), mPhotoUrl);
        mPictureUploader.uploadPictureRx()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Integer>() {
                @Override
                public void onCompleted() {
                    if (mUploadDialog != null && mUploadDialog.isShowing()) {
                        mUploadDialog.dismiss();
                        mUploadDialog = null;
                    }
                    finish();
                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(Integer integer) {
                    if (mUploadDialog != null) {
                        mUploadDialog.setProgress(integer);
                    }
                }
            });

        mUploadDialog = new MaterialDialog.Builder(this)
            .title(R.string.upload_picture)
            .progress(false, 100)
            .negativeText(R.string.cancel)
            .canceledOnTouchOutside(false)
            .onNegative(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    if (mPictureUploader != null) {
                        mPictureUploader.cancel();
                    }
                }
            }).show();


    }
}
