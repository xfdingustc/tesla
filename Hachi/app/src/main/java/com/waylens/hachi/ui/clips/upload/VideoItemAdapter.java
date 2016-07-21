package com.waylens.hachi.ui.clips.upload;

import android.app.Activity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
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
import com.waylens.hachi.app.UploadManager;
import com.waylens.hachi.bgjob.upload.UploadMomentJob;
import com.waylens.hachi.ui.community.MomentActivity;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.ui.entities.Moment;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/6/17.
 */
public class VideoItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements UploadManager.OnUploadJobStateChangeListener {
    private static final String TAG = VideoItemAdapter.class.getSimpleName();

    private final Activity mActivity;
    private List<Moment> mUploadedMomentList = new ArrayList<>();
    private PrettyTime mPrettyTime = new PrettyTime();
    private UploadManager mUploadManager = UploadManager.getManager();


    public VideoItemAdapter(Activity activity) {
        this.mActivity = activity;
        mUploadManager.addOnUploadJobStateChangedListener(this);
    }

    public void setUploadedMomentList(List<Moment> momentList) {
        this.mUploadedMomentList = momentList;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_simple, parent, false);
        return new VideoItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position < mUploadManager.getJobCount()) {
            onBindUploadingViewHolder(holder, position);
        } else {
            onBindUploadedViewHolder(holder, position - mUploadManager.getJobCount());
        }

    }

    private void onBindUploadingViewHolder(RecyclerView.ViewHolder holder, int position) {
        final VideoItemViewHolder videoItemViewHolder = (VideoItemViewHolder) holder;
        final UploadMomentJob uploadMomentJob = mUploadManager.getUploadJob(position);
        LocalMoment localMoment = uploadMomentJob.getLocalMoment();
        videoItemViewHolder.momentTitle.setText(localMoment.title);
        videoItemViewHolder.uploadStatus.setVisibility(View.VISIBLE);
        videoItemViewHolder.uploadProgress.setVisibility(View.VISIBLE);
        videoItemViewHolder.uploadProgress.setProgress(uploadMomentJob.getUploadProgress());
        videoItemViewHolder.uploadStatus.setText("" + uploadMomentJob.getUploadProgress() + "% " + mActivity.getString(R.string.uploaded));
        if (localMoment.thumbnailPath != null) {
            Glide.with(mActivity)
                .load(localMoment.thumbnailPath)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontAnimate()
                .placeholder(videoItemViewHolder.videoCover.getDrawable())
                .into(videoItemViewHolder.videoCover);
        }
        if (uploadMomentJob.getState() == UploadMomentJob.UPLOAD_STATE_FINISHED) {
            videoItemViewHolder.uploadProgress.setVisibility(View.GONE);
            videoItemViewHolder.uploadStatus.setVisibility(View.GONE);
        } else {
            videoItemViewHolder.uploadProgress.setVisibility(View.VISIBLE);
            videoItemViewHolder.uploadStatus.setVisibility(View.VISIBLE);
        }

        updateUploadStatus(uploadMomentJob.getState(), videoItemViewHolder.description);

        videoItemViewHolder.videoDuration.setVisibility(View.INVISIBLE);

        videoItemViewHolder.btnMore.setVisibility(View.VISIBLE);

        videoItemViewHolder.btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(mActivity, videoItemViewHolder.btnMore, Gravity.END);
                popupMenu.getMenuInflater().inflate(R.menu.menu_upload, popupMenu.getMenu());
                if (uploadMomentJob.getState() == UploadMomentJob.UPLOAD_STATE_FINISHED) {
                    popupMenu.getMenu().removeItem(R.id.cancel);
                }
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.cancel:
                                uploadMomentJob.cancel();
                                break;
                        }
                        return true;
                    }
                });
                popupMenu.show();
            }
        });
    }

    private void updateUploadStatus(int state, TextView description) {
        switch (state) {
            case UploadMomentJob.UPLOAD_STATE_GET_URL_INFO:
                description.setText(R.string.upload_get_url_info);
                break;
            case UploadMomentJob.UPLOAD_STATE_GET_VIDEO_COVER:
                description.setText(R.string.upload_get_video_cover);
                break;
            case UploadMomentJob.UPLOAD_STATE_STORE_VIDEO_COVER:
                description.setText(R.string.upload_store_video_cover);
                break;
            case UploadMomentJob.UPLOAD_STATE_CREATE_MOMENT:
                description.setText(R.string.upload_create_moment);
                break;
            case UploadMomentJob.UPLOAD_STATE_LOGIN:
                description.setText(R.string.upload_login);
                break;
            case UploadMomentJob.UPLOAD_STATE_LOGIN_SUCCEED:
                description.setText(R.string.upload_login_succeed);
                break;
            case UploadMomentJob.UPLOAD_STATE_START:
            case UploadMomentJob.UPLOAD_STATE_PROGRESS:
                description.setText(R.string.upload_start);
                break;
            case UploadMomentJob.UPLOAD_STATE_CANCELLED:
                description.setText(R.string.upload_cancelled);
                break;
            case UploadMomentJob.UPLOAD_STATE_FINISHED:
                description.setText(R.string.upload_finished);
                break;
            case UploadMomentJob.UPLOAD_STATE_ERROR:
                description.setText(R.string.upload_error);
                break;

        }
    }

    private void onBindUploadedViewHolder(RecyclerView.ViewHolder holder, int position) {
        final VideoItemViewHolder videoItemViewHolder = (VideoItemViewHolder) holder;
        final Moment uploadedMoment = mUploadedMomentList.get(position);
        videoItemViewHolder.uploadProgress.setVisibility(View.GONE);
        videoItemViewHolder.momentTitle.setText(uploadedMoment.title);
        videoItemViewHolder.uploadStatus.setVisibility(View.GONE);
        videoItemViewHolder.description.setText(mPrettyTime.formatUnrounded(new Date(uploadedMoment.uploadTime)));
        videoItemViewHolder.description.setVisibility(View.VISIBLE);
        Glide.with(mActivity)
            .load(uploadedMoment.thumbnail)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .crossFade()
            .into(videoItemViewHolder.videoCover);

        videoItemViewHolder.videoDuration.setText(DateUtils.formatElapsedTime(uploadedMoment.duration / 1000l));
        videoItemViewHolder.btnMore.setVisibility(View.GONE);
        videoItemViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MomentActivity.launch(mActivity, uploadedMoment.id, uploadedMoment.thumbnail, videoItemViewHolder.videoCover);
            }
        });

    }

    @Override
    public int getItemCount() {
        return mUploadManager.getJobCount() + mUploadedMomentList.size();
    }

    @Override
    public void onUploadJobStateChanged(UploadMomentJob job, int index) {
        notifyItemChanged(index);
    }

    @Override
    public void onUploadJobAdded() {

    }

    @Override
    public void onUploadJobRemoved() {
        Logger.t(TAG).d("upload remove!!");
        notifyDataSetChanged();
    }


    public class VideoItemViewHolder extends RecyclerView.ViewHolder {

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




        public VideoItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
