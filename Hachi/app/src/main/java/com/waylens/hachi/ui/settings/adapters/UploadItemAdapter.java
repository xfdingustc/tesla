package com.waylens.hachi.ui.settings.adapters;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.upload.UploadManager2;
import com.waylens.hachi.bgjob.upload.UploadMomentJob;
import com.waylens.hachi.uploadqueue.UploadManager;
import com.waylens.hachi.uploadqueue.model.UploadError;
import com.waylens.hachi.uploadqueue.model.UploadRequest;
import com.waylens.hachi.uploadqueue.model.UploadStatus;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/8/15.
 */
public class UploadItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements UploadManager2.OnUploadJobStateChangeListener {
    private final Activity mActivity;
    private UploadManager mUploadManager;

    public UploadItemAdapter(Activity activity) {
        this.mActivity = activity;
        mUploadManager = UploadManager.getManager(activity);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_simple, parent, false);
        return new UploadVideoItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final UploadVideoItemViewHolder videoItemViewHolder = (UploadVideoItemViewHolder) holder;
        UploadRequest request = mUploadManager.getQueuedItemList().get(position);
        if (TextUtils.isEmpty(request.getTitle())) {
            videoItemViewHolder.momentTitle.setText(R.string.no_title);
        } else {
            videoItemViewHolder.momentTitle.setText(request.getTitle());
        }

        if (request.getLocalMoment() == null) {
            videoItemViewHolder.imageMoment.setVisibility(View.VISIBLE);
        } else {
            videoItemViewHolder.imageMoment.setVisibility(View.INVISIBLE);
        }

        if (!TextUtils.isEmpty(request.getLocalMoment().thumbnailPath)) {
            Glide.with(mActivity)
                .load(request.getLocalMoment().thumbnailPath)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontAnimate()
                .placeholder(videoItemViewHolder.videoCover.getDrawable())
                .into(videoItemViewHolder.videoCover);
        }

        videoItemViewHolder.uploadStatus.setVisibility(View.VISIBLE);
        videoItemViewHolder.uploadStatus.setText(request.getStatus().message());
        videoItemViewHolder.uploadProgress.setVisibility(View.VISIBLE);
        videoItemViewHolder.uploadProgress.setProgress(request.getProgress());
        videoItemViewHolder.videoDuration.setVisibility(View.INVISIBLE);

        switch (request.getStatus()) {
            case UPLOADING:
                break;
            case FAILED:
                videoItemViewHolder.description.setVisibility(View.VISIBLE);
                if (request.getCurrentError() != UploadError.NO_ERROR) {
                    videoItemViewHolder.description.setText(request.getCurrentError().getValue());
                }
                break;
        }

        /*


        if (uploadable.getState() == UploadMomentJob.UPLOAD_STATE_FINISHED) {
            videoItemViewHolder.uploadProgress.setVisibility(View.GONE);
            videoItemViewHolder.uploadStatus.setVisibility(View.GONE);
        } else {
            videoItemViewHolder.uploadProgress.setVisibility(View.VISIBLE);
            videoItemViewHolder.uploadStatus.setVisibility(View.VISIBLE);
        }

//        updateUploadStatus(uploadable.getState(), videoItemViewHolder.description, localMoment.cache);

        videoItemViewHolder.description.setText(uploadable.getStateDescription());

        videoItemViewHolder.videoDuration.setVisibility(View.INVISIBLE);

        videoItemViewHolder.btnMore.setVisibility(View.GONE);

        videoItemViewHolder.btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(mActivity, videoItemViewHolder.btnMore, Gravity.END);
                popupMenu.getMenuInflater().inflate(R.menu.menu_upload, popupMenu.getMenu());
                if (uploadable.getState() == UploadMomentJob.UPLOAD_STATE_FINISHED) {
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
        });*/


    }


    @Override
    public int getItemCount() {
        return mUploadManager.getQueuedItemList().size();
    }

    @Override
    public void onUploadJobStateChanged(UploadMomentJob job, int index) {
        notifyItemChanged(index);
    }

    @Override
    public void onUploadJobAdded() {
        notifyDataSetChanged();
    }

    @Override
    public void onUploadJobRemoved() {
        notifyDataSetChanged();
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
