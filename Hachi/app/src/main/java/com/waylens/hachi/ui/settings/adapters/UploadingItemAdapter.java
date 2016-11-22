package com.waylens.hachi.ui.settings.adapters;


import android.app.Activity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.export.statejobqueue.PersistentQueue;
import com.waylens.hachi.bgjob.upload.CacheMomentJob;
import com.waylens.hachi.bgjob.upload.UploadManager;
import com.waylens.hachi.bgjob.export.statejobqueue.UploadMomentJob;
import com.waylens.hachi.bgjob.upload.event.UploadMomentEvent;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/8/15.
 */
public class UploadingItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public final String TAG = UploadingItemAdapter.class.getSimpleName();
    private final Activity mActivity;
    private PersistentQueue mPersistentQueue;
    private int mUploadingCount = 0;

    public UploadingItemAdapter(Activity activity) {
        this.mActivity = activity;
        this.mPersistentQueue = PersistentQueue.getPersistentQueue();
    }

    public void handleEvent(UploadMomentEvent event) {
        Logger.t(TAG).d("event type" + event.getWhat());
        switch (event.getWhat()) {
            case UploadMomentEvent.UPLOAD_JOB_ADDED:
                notifyDataSetChanged();
                break;
            case UploadMomentEvent.UPLOAD_JOB_STATE_CHANGED:
                notifyDataSetChanged();
                break;
            case UploadMomentEvent.UPLOAD_JOB_REMOVED:
                notifyDataSetChanged();
                break;
        }
        UploadMomentJob job = event.getUploadable();
        //Logger.t(TAG).d("in handle event job progress = " + job.getUploadProgress());

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_simple, parent, false);
        return new UploadVideoItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final UploadVideoItemViewHolder videoItemViewHolder = (UploadVideoItemViewHolder) holder;
        final UploadMomentJob uploadable = mPersistentQueue.getUploadingJob(position);
        if (uploadable == null) {
            return;
        }
        if (TextUtils.isEmpty(uploadable.getMomentTitle())) {
            videoItemViewHolder.momentTitle.setText(R.string.no_title);
        } else {
            videoItemViewHolder.momentTitle.setText(uploadable.getMomentTitle());
        }
        if (uploadable.getLocalMoment() == null) {
            videoItemViewHolder.imageMoment.setVisibility(View.VISIBLE);
        } else {
            videoItemViewHolder.imageMoment.setVisibility(View.INVISIBLE);
        }
        videoItemViewHolder.uploadStatus.setVisibility(View.VISIBLE);
        videoItemViewHolder.uploadProgress.setVisibility(View.VISIBLE);
        videoItemViewHolder.uploadProgress.setProgress(uploadable.getUploadProgress());
        Logger.t(TAG).d("job progress in bind view = " + uploadable.getUploadProgress());
        videoItemViewHolder.uploadStatus.setText(uploadable.getProgressStatus());

        if (!TextUtils.isEmpty(uploadable.getThumbnail())) {
            Glide.with(mActivity)
                    .load(uploadable.getThumbnail())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .dontAnimate()
                    .placeholder(videoItemViewHolder.videoCover.getDrawable())
                    .into(videoItemViewHolder.videoCover);
        }
        if (uploadable.getState() == CacheMomentJob.UPLOAD_STATE_FINISHED) {
            videoItemViewHolder.uploadProgress.setVisibility(View.GONE);
            videoItemViewHolder.uploadStatus.setVisibility(View.GONE);
        } else {
            videoItemViewHolder.uploadProgress.setVisibility(View.VISIBLE);
            videoItemViewHolder.uploadStatus.setVisibility(View.VISIBLE);
        }

        //updateUploadStatus(uploadable.getState(), videoItemViewHolder.description, localMoment.cache);

        videoItemViewHolder.description.setText(uploadable.getStateDescription());

        videoItemViewHolder.videoDuration.setVisibility(View.INVISIBLE);

        videoItemViewHolder.btnMore.setVisibility(View.GONE);

        videoItemViewHolder.btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(mActivity, videoItemViewHolder.btnMore, Gravity.END);
                popupMenu.getMenuInflater().inflate(R.menu.menu_upload, popupMenu.getMenu());
                if (uploadable.getState() == CacheMomentJob.UPLOAD_STATE_FINISHED) {
                    popupMenu.getMenu().removeItem(R.id.cancel);
                }
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.cancel:
                                uploadable.cancelUpload();
                                break;
                        }
                        return true;
                    }
                });
                popupMenu.show();
            }
        });
    }

    public int getItemCount() {
        mUploadingCount = mPersistentQueue.getJobCount();
        return mUploadingCount;
    }


    public class UploadVideoItemViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.moment_title)
        TextView momentTitle;

        @BindView(R.id.upload_status)
        TextView uploadStatus;

        @BindView(R.id.description)
        TextView description;

        @BindView(R.id.video_cover)
        ImageView videoCover;

        @BindView(R.id.video_duration)
        TextView videoDuration;

        @BindView(R.id.upload_progress)
        ProgressBar uploadProgress;

        @BindView(R.id.btn_more)
        ImageButton btnMore;

        @BindView(R.id.image_moment)
        ImageView imageMoment;

        public UploadVideoItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
