package com.waylens.hachi.ui.settings.myvideo;

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
import com.waylens.hachi.bgjob.upload.IUploadable;
import com.waylens.hachi.bgjob.upload.UploadManager;
import com.waylens.hachi.bgjob.upload.UploadMomentJob;
import com.waylens.hachi.ui.entities.LocalMoment;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/8/15.
 */
public class UploadItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements UploadManager.OnUploadJobStateChangeListener {
    private final Activity mActivity;
    private UploadManager mUploadManager = UploadManager.getManager();
    public UploadItemAdapter(Activity activity) {
        this.mActivity = activity;
        mUploadManager.addOnUploadJobStateChangedListener(this);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_simple, parent, false);
        return new UploadVideoItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final UploadVideoItemViewHolder videoItemViewHolder = (UploadVideoItemViewHolder) holder;
        final IUploadable uploadable = mUploadManager.getUploadJob(position);
        LocalMoment localMoment = uploadable.getLocalMoment();
        if (TextUtils.isEmpty(localMoment.title)) {
            videoItemViewHolder.momentTitle.setText(R.string.no_title);
        } else {
            videoItemViewHolder.momentTitle.setText(localMoment.title);
        }
        videoItemViewHolder.uploadStatus.setVisibility(View.VISIBLE);
        videoItemViewHolder.uploadProgress.setVisibility(View.VISIBLE);
        videoItemViewHolder.uploadProgress.setProgress(uploadable.getUploadProgress());
        if (localMoment.cache) {
            videoItemViewHolder.uploadStatus.setText(mActivity.getString(R.string.downloaded, uploadable.getUploadProgress()));
        } else {
            videoItemViewHolder.uploadStatus.setText("" + uploadable.getUploadProgress() + "% " + mActivity.getString(R.string.uploaded));
        }
        if (localMoment.thumbnailPath != null) {
            Glide.with(mActivity)
                .load(localMoment.thumbnailPath)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontAnimate()
                .placeholder(videoItemViewHolder.videoCover.getDrawable())
                .into(videoItemViewHolder.videoCover);
        }
        if (uploadable.getState() == UploadMomentJob.UPLOAD_STATE_FINISHED) {
            videoItemViewHolder.uploadProgress.setVisibility(View.GONE);
            videoItemViewHolder.uploadStatus.setVisibility(View.GONE);
        } else {
            videoItemViewHolder.uploadProgress.setVisibility(View.VISIBLE);
            videoItemViewHolder.uploadStatus.setVisibility(View.VISIBLE);
        }

        updateUploadStatus(uploadable.getState(), videoItemViewHolder.description, localMoment.cache);

        videoItemViewHolder.videoDuration.setVisibility(View.INVISIBLE);

        videoItemViewHolder.btnMore.setVisibility(View.VISIBLE);

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
        });


    }


    private void updateUploadStatus(int state, TextView description, boolean isCache) {
        switch (state) {
            case IUploadable.UPLOAD_STATE_GET_URL_INFO:
                description.setText(R.string.upload_get_url_info);
                break;
            case IUploadable.UPLOAD_STATE_GET_VIDEO_COVER:
                description.setText(R.string.upload_get_video_cover);
                break;
            case IUploadable.UPLOAD_STATE_STORE_VIDEO_COVER:
                description.setText(R.string.upload_store_video_cover);
                break;
            case IUploadable.UPLOAD_STATE_CREATE_MOMENT:
                description.setText(R.string.upload_create_moment);
                break;
            case IUploadable.UPLOAD_STATE_LOGIN:
                description.setText(R.string.upload_login);
                break;
            case IUploadable.UPLOAD_STATE_LOGIN_SUCCEED:
                description.setText(R.string.upload_login_succeed);
                break;
            case IUploadable.UPLOAD_STATE_START:
            case IUploadable.UPLOAD_STATE_PROGRESS:
                if (isCache) {
                    description.setText(R.string.cache_start);
                } else {
                    description.setText(R.string.upload_start);
                }
                break;
            case IUploadable.UPLOAD_STATE_CANCELLED:
                description.setText(R.string.upload_cancelled);
                break;
            case IUploadable.UPLOAD_STATE_FINISHED:
                description.setText(R.string.upload_finished);
                break;
            case IUploadable.UPLOAD_STATE_ERROR:
                description.setText(R.string.upload_error);
                break;
            case IUploadable.UPLOAD_STATE_WAITING_FOR_NETWORK_AVAILABLE:
                description.setText(R.string.waiting_for_network);
                break;

        }
    }

    @Override
    public int getItemCount() {
        return mUploadManager.getJobCount();
    }

    @Override
    public void onUploadJobStateChanged(IUploadable job, int index) {
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

        public UploadVideoItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
