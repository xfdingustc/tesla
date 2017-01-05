package com.waylens.hachi.ui.settings.myvideo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.settings.adapters.UploadItemAdapter;
import com.waylens.hachi.uploadqueue.UploadManager;
import com.waylens.hachi.uploadqueue.UploadResponseHolder;
import com.waylens.hachi.uploadqueue.interfaces.UploadResponseListener;
import com.waylens.hachi.uploadqueue.model.UploadError;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/9/12.
 */
public class UploadingMomentActivity extends BaseActivity {
    private static final String TAG = UploadingMomentActivity.class.getSimpleName();
    private UploadItemAdapter mUploadItemAdapter;

    private static final String EXTRA_AUTO_EXIT = "extra.auto.exit";

    private boolean mAutoExit;

    @BindView(R.id.uploading_list)
    RecyclerView mRvUploadingList;

    @BindView(R.id.root_switch)
    ViewAnimator rootAnimator;

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, UploadingMomentActivity.class);
        activity.startActivity(intent);
    }

    public static void launch(Activity activity, boolean autoExit) {
        Intent intent = new Intent(activity, UploadingMomentActivity.class);
        intent.putExtra(EXTRA_AUTO_EXIT, autoExit);
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
        UploadResponseHolder.getHolder().addListener(mUploadListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        UploadResponseHolder.getHolder().removeListener(mUploadListener);
    }


    @Override
    protected void init() {
        super.init();
        mAutoExit = getIntent().getBooleanExtra(EXTRA_AUTO_EXIT, false);
        initViews();
    }


    private void initViews() {
        setContentView(R.layout.activity_uploading);
        setupToolbar();

/*        List<StateJobHolder> uploadJobList = PersistentQueue.getPersistentQueue().getAllJobs();
        Logger.t(TAG).d("uploadJobList size = " + uploadJobList.size());
        for (StateJobHolder jobHolder : uploadJobList) {
            Logger.t(TAG).d("insertId = " + jobHolder.getInsertId());
            Logger.t(TAG).d("jobId = " + jobHolder.getJobId());
            Logger.t(TAG).d("jobState = " + jobHolder.getJobState());
            //Logger.t(TAG).d("moment first segment url = " + ((CacheUploadMomentJob)jobHolder.getJob()).getLocalMoment().mSegments.get(0).uploadURL.url);
        }*/

        mRvUploadingList.setLayoutManager(new LinearLayoutManager(this));
        mUploadItemAdapter = new UploadItemAdapter(this);
        mRvUploadingList.setAdapter(mUploadItemAdapter);

        if (mUploadItemAdapter.getItemCount() == 0) {
            rootAnimator.setDisplayedChild(1);
        }
/*        mUploadingItemAdapter = new UploadingItemAdapter(this);
        mRvUploadingList.setAdapter(mUploadingItemAdapter);

        if (mUploadingItemAdapter.getItemCount() == 0) {
            rootAnimator.setDisplayedChild(1);
        } else {
            CacheUploadMomentService.launch(this);
        }*/
    }

    private UploadResponseListener mUploadListener = new UploadResponseListener() {
        @Override
        public void onUploadStart(String key) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    rootAnimator.setDisplayedChild(0);
                    mUploadItemAdapter.notifyDataSetChanged();
                }
            });

        }



        @Override
        public void updateProgress(final String key, int progress) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mUploadItemAdapter.notifyItemChanged(UploadManager.getManager(UploadingMomentActivity.this).getItemPosition(key));
                }
            });

        }

        @Override
        public void updateDescription(final String key) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mUploadItemAdapter.notifyItemChanged(UploadManager.getManager(UploadingMomentActivity.this).getItemPosition(key));
                }
            });
        }

        @Override
        public void onComplete(String key) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mUploadItemAdapter.notifyDataSetChanged();

                    if (mUploadItemAdapter.getItemCount() == 0) {
                        if (mAutoExit) {
                            finish();
                        } else {
                            rootAnimator.setDisplayedChild(1);
                        }
                    }
                }
            });
        }

        @Override
        public void onError(final String key, UploadError error) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int position = UploadManager.getManager(UploadingMomentActivity.this).getItemPosition(key);
                    if (position >= 0) {
                        mUploadItemAdapter.notifyItemChanged(position);
                    }
                }
            });
        }
    };


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.uploading);
    }
}
