package com.waylens.hachi.ui.settings.myvideo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.DownloadManager;
import com.waylens.hachi.bgjob.download.DownloadHelper;
import com.waylens.hachi.bgjob.download.DownloadJob;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/8/15.
 */
public class DownloadItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements DownloadManager.OnDownloadJobStateChangeListener {
    private static final String TAG = DownloadItemAdapter.class.getSimpleName();
    private final Activity mActivity;

    private DownloadManager mDownloadManager = DownloadManager.getManager();

    public DownloadItemAdapter(Activity activity) {
        this.mActivity = activity;
        mDownloadManager.addListener(this);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_simple, parent, false);

        return new DownloadVideoItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position < mDownloadManager.getCount()) {
            onBindDownloadingViewHolder(holder, position);
        } else {
            onBindDownloadedViewHolder(holder, position - mDownloadManager.getCount());
        }

    }

    @Override
    public void onDownloadJobStateChanged(DownloadJob job, int index) {
        notifyItemChanged(index);
    }

    @Override
    public void onDownloadJobAdded() {
        notifyDataSetChanged();
    }

    @Override
    public void onDownloadJobRemoved() {
        notifyDataSetChanged();
    }

    private void onBindDownloadingViewHolder(RecyclerView.ViewHolder holder, int position) {
        Logger.t(TAG).d("bind downloading view holder: " + position);
        DownloadVideoItemViewHolder videoItemViewHolder = (DownloadVideoItemViewHolder) holder;
        DownloadJob job = DownloadManager.getManager().getDownloadJob(position);
        videoItemViewHolder.uploadProgress.setVisibility(View.VISIBLE);
        videoItemViewHolder.uploadProgress.setProgress(job.getDownloadProgress());
        if (job.getOutputFile() != null) {
            videoItemViewHolder.momentTitle.setText(new File(job.getOutputFile()).getName());
        }
    }

    private void onBindDownloadedViewHolder(RecyclerView.ViewHolder holder, int position) {
        Logger.t(TAG).d("bind downloaded view holder: " + position);
        final DownloadVideoItemViewHolder videoItemViewHolder = (DownloadVideoItemViewHolder) holder;
        File[] downloadedFiles = DownloadHelper.getDownloadedFileList();
        final File oneDownloadedFile = downloadedFiles[position];

        Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                Bitmap bitmap = getVideoBitmap(oneDownloadedFile.toString(), 384, 216, MediaStore.Images.Thumbnails.MICRO_KIND);
                subscriber.onNext(bitmap);
            }
        })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Bitmap>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(Bitmap bitmap) {
                    videoItemViewHolder.videoCover.setImageBitmap(bitmap);
                }
            });


        videoItemViewHolder.momentTitle.setText(oneDownloadedFile.getName());
        videoItemViewHolder.uploadProgress.setVisibility(View.INVISIBLE);

        videoItemViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(oneDownloadedFile), "video/mp4");
                mActivity.startActivity(intent);
            }
        });


    }

    public Bitmap getVideoBitmap(String filePath, int width, int height, int kind) {

        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(filePath, kind);


        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);

        return bitmap;
    }


    @Override
    public int getItemCount() {
        return mDownloadManager.getCount() + DownloadHelper.getDownloadedFileList().length;
    }


    public class DownloadVideoItemViewHolder extends RecyclerView.ViewHolder {
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

        public DownloadVideoItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
