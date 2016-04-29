package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;


import com.nostra13.universalimageloader.core.ImageLoader;
import com.waylens.hachi.R;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.avatar.AvatarActivity;
import com.waylens.hachi.utils.ImageUtils;

import butterknife.Bind;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2016/4/26.
 */
public class AccountActivity extends BaseActivity {

    private ImageLoader mImageLoader = ImageLoader.getInstance();
    private SessionManager mSessionManager = SessionManager.getInstance();

    @Bind(R.id.avatar)
    CircleImageView mAvatar;

    @Bind(R.id.btnAddPhoto)
    ImageButton mBtnAddPhoto;

    @Bind(R.id.email)
    TextView mTvEmail;

    @OnClick(R.id.avatar)
    public void onBtnAvatarClicked() {
        AvatarActivity.start(this, false);
    }

    @OnClick(R.id.btnAddPhoto)
    public void onBtnAddPhotoClick() {
        AvatarActivity.start(this, true);
    }

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, AccountActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_account);
        setupToolbar();
        mImageLoader.displayImage(mSessionManager.getAvatarUrl(), mAvatar, ImageUtils.getAvatarOptions());
//        mTvEmail.setText(mSessionManager.get);
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();

        mToolbar.setNavigationIcon(R.drawable.navbar_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mToolbar.setTitle(R.string.account);
    }
}
