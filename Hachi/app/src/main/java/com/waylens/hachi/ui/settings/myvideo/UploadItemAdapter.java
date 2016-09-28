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
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.upload.CacheMomentJob;
import com.waylens.hachi.bgjob.upload.UploadManager;
import com.waylens.hachi.bgjob.upload.UploadMomentJob;

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
        final UploadMomentJob uploadable = mUploadManager.getUploadJob(position);
        if (TextUtils.isEmpty(uploadable.getMomentTitle())) {
            videoItemViewHolder.momentTitle.setText(R.string.no_title);
        } else {
            videoItemViewHolder.momentTitle.setText(uploadable.getMomentTitle());
        }
        videoItemViewHolder.uploadStatus.setVisibility(View.VISIBLE);
        videoItemViewHolder.uploadProgress.setVisibility(View.VISIBLE);
        videoItemViewHolder.uploadProgress.setProgress(uploadable.getUploadProgress());
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

//        updateUploadStatus(uploadable.getState(), videoItemViewHolder.description, localMoment.cache);

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


    @Override
    public int getItemCount() {
        return mUploadManager.getJobCount();
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

        public UploadVideoItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
