package com.waylens.hachi.ui.clips.upload;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.UploadManager;
import com.waylens.hachi.bgjob.upload.UploadMomentJob;
import com.waylens.hachi.ui.clips.cliptrimmer.VideoTrimmer;
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

    private final Context mContext;
    private List<Moment> mUploadedMomentList = new ArrayList<>();
    private PrettyTime mPrettyTime = new PrettyTime();
    private UploadManager mUploadManager = UploadManager.getManager();


    public VideoItemAdapter(Context context) {
        this.mContext = context;
        mUploadManager.setOnUploadJobStateChangedListener(this);
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
        VideoItemViewHolder videoItemViewHolder = (VideoItemViewHolder)holder;
        UploadMomentJob uploadMomentJob = mUploadManager.getUploadJob(position);
        LocalMoment localMoment = uploadMomentJob.getLocalMoment();
        videoItemViewHolder.momentTitle.setText(localMoment.title);
        videoItemViewHolder.uploadStatus.setVisibility(View.VISIBLE);
        videoItemViewHolder.uploadProgress.setVisibility(View.VISIBLE);
        videoItemViewHolder.uploadProgress.setProgress(uploadMomentJob.getUploadProgress());
        videoItemViewHolder.uploadStatus.setText("" + uploadMomentJob.getUploadProgress() + "% " + mContext.getString(R.string.uploaded));
        if (localMoment.thumbnailPath != null) {
            Glide.with(mContext)
                .load(localMoment.thumbnailPath)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .crossFade()
                .into(videoItemViewHolder.videoCover);
        }
    }

    private void onBindUploadedViewHolder(RecyclerView.ViewHolder holder, int position) {
        VideoItemViewHolder videoItemViewHolder = (VideoItemViewHolder)holder;
        Moment uploadedMoment = mUploadedMomentList.get(position);
        videoItemViewHolder.uploadProgress.setVisibility(View.GONE);
        videoItemViewHolder.momentTitle.setText(uploadedMoment.title);
        videoItemViewHolder.uploadStatus.setVisibility(View.GONE);
        videoItemViewHolder.description.setText(mPrettyTime.formatUnrounded(new Date(uploadedMoment.uploadTime)));

        Glide.with(mContext)
            .load(uploadedMoment.thumbnail)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .crossFade()
            .into(videoItemViewHolder.videoCover);

        videoItemViewHolder.videoDuration.setText(DateUtils.formatElapsedTime(uploadedMoment.duration / 1000l));
    }

    @Override
    public int getItemCount() {
        return mUploadManager.getJobCount() + mUploadedMomentList.size();
    }

    @Override
    public void onUploadJobStateChanged(UploadMomentJob job, int index) {
        Logger.t(TAG).d("job update: " + index);
        notifyItemChanged(index);

    }

    @Override
    public void onUploadJobAdded() {

    }

    @Override
    public void onUploadJobRemoved() {

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


        public VideoItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
