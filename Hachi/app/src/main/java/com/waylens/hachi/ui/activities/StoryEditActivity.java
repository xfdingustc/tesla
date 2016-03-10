package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.ui.entities.story.Story;
import com.waylens.hachi.ui.fragments.clipplay.CameraVideoPlayFragment;
import com.waylens.hachi.ui.helpers.MomentShareHelper;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/2/2.
 */
public class StoryEditActivity extends BaseActivity {
    public static final String TAG = StoryEditActivity.class.getSimpleName();
    private static Story mSharedStory;
    private Story mStory;

    private CameraVideoPlayFragment mVideoPlayFragment;

    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mImageLoader;

    @Bind(R.id.video_cover)
    ImageView mVideoCover;

    @Bind(R.id.titleEditor)
    EditText mTitleEditor;

    @Bind(R.id.uploadProgressBar)
    ProgressBar mUploadProgressBar;

    @Bind(R.id.btnShare)
    ImageButton mBtnShare;

    @OnClick(R.id.btnShare)
    public void onBtnShareClicked() {
        // First check if it is logined
        SessionManager sessionManager = SessionManager.getInstance();
        if (!sessionManager.isLoggedIn()) {
            LoginActivity.launch(this);
            return;
        }

        mUploadProgressBar.setVisibility(View.VISIBLE);
        MomentShareHelper helper = new MomentShareHelper(this, new MomentShareHelper.OnShareMomentListener() {
            @Override
            public void onShareSuccessful(LocalMoment localMoment) {
                Logger.t(TAG).d("Upload done!!!");
                Snackbar snackbar = Snackbar.make(mUploadProgressBar, "UploadDone", Snackbar
                        .LENGTH_SHORT);
                snackbar.show();
                mUploadProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelShare() {

            }

            @Override
            public void onShareError(int errorCode, int errorResId) {

            }

            @Override
            public void onUploadProgress(int uploadPercentage) {
                mUploadProgressBar.setProgress(uploadPercentage);
            }
        });
        String[] tags = new String[]{"Shanghai", "car"};
        //helper.shareMoment(mStory.getPlaylist().getId(), mTitleEditor.getText().toString(), tags, "PUBLIC", 0);
    }


    @OnClick(R.id.btn_play)
    public void onBtnPlayClicked() {
        mVideoPlayFragment = CameraVideoPlayFragment.newInstance(Snipe.newRequestQueue(), mStory
                .getPlaylist(), null);
        getFragmentManager().beginTransaction().replace(R.id.fragment_content, mVideoPlayFragment).commit();
        mVideoCover.setVisibility(View.INVISIBLE);
    }

    @OnClick(R.id.btnEnhance)
    public void onBtnEnhanceClicked() {
//        EnhancementActivity2 fragment = EnhancementActivity2.newInstance(mStory.getPlaylist());
//
//        getFragmentManager().beginTransaction()
//                .add(R.id.root_container, fragment)
//            .addToBackStack(null)
//                .commit();
    }

    public static void launch(Activity startingActivity, Story story) {
        Intent intent = new Intent(startingActivity, StoryEditActivity.class);
        mSharedStory = story;
        startingActivity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        mStory = mSharedStory;
        mVdbRequestQueue = Snipe.newRequestQueue();
        mImageLoader = VdbImageLoader.getImageLoader(mVdbRequestQueue);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_story_edit);
        Clip firstClip = mStory.getPlaylist().getClip(0);
        ClipPos clipPos = new ClipPos(firstClip, firstClip.getStartTimeMs(), ClipPos.TYPE_POSTER,
                false);

        mUploadProgressBar.setMax(100);
        mImageLoader.displayVdbImage(clipPos, mVideoCover);

        mTitleEditor.requestFocus();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            public void run() {
                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(mTitleEditor, 0);
            }

        }, 500);

    }
}
