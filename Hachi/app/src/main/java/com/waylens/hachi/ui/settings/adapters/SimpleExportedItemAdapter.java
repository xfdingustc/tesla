package com.waylens.hachi.ui.settings.adapters;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
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
import com.waylens.hachi.bgjob.export.ExportManager;
import com.waylens.hachi.bgjob.export.ExportableJob;
import com.waylens.hachi.bgjob.export.event.ExportEvent;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.glide_snipe_integration.SnipeGlideLoader;
import com.waylens.hachi.utils.FileUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/11/14.
 */

public class SimpleExportedItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = DownloadItemAdapter.class.getSimpleName();
    private final Activity mActivity;

    private ExportManager mDownloadManager = ExportManager.getManager();

    private List<File> mDownloadedFileList;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHandleExportJobEvent(ExportEvent event) {
        switch (event.getWhat()) {
            case ExportEvent.EXPORT_WHAT_JOB_ADDED:
                notifyDataSetChanged();
                break;
            case ExportEvent.EXPORT_WHAT_PROGRESS:
                notifyItemChanged(event.getIndex());
                break;
            case ExportEvent.EXPORT_WHAT_FINISHED:
                mDownloadedFileList = FileUtils.getExportedFileList();
                notifyDataSetChanged();
                break;
        }
    }


    public SimpleExportedItemAdapter(Activity activity) {
        this.mActivity = activity;
        mDownloadedFileList = FileUtils.getExportedFileList();
    }


    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        Logger.t(TAG).d("detached from recycler view");
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_simple_lite, parent, false);

        return new SimpleExportedVideoItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position < mDownloadManager.getCount()) {
            onBindDownloadingViewHolder(holder, position);
        } else {
            onBindDownloadedViewHolder(holder, position - mDownloadManager.getCount());
        }

    }


    private void onBindDownloadingViewHolder(RecyclerView.ViewHolder holder, int position) {
        SimpleExportedVideoItemViewHolder videoItemViewHolder = (SimpleExportedVideoItemViewHolder) holder;
        ExportableJob job = ExportManager.getManager().getDownloadJob(position);
        videoItemViewHolder.uploadProgress.setVisibility(View.VISIBLE);
        videoItemViewHolder.uploadProgress.setProgress(job.getExportProgress());
        Glide.with(mActivity)
            .using(new SnipeGlideLoader(VdtCameraManager.getManager().getCurrentVdbRequestQueue()))
            .load(job.getClipStartPos())
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .crossFade()
            .into(videoItemViewHolder.videoCover);


    }

    private void onBindDownloadedViewHolder(RecyclerView.ViewHolder holder, final int position) {

        final SimpleExportedVideoItemViewHolder viewHolder = (SimpleExportedVideoItemViewHolder) holder;

        final File oneDownloadedFile = mDownloadedFileList.get(position);

        Glide.with(mActivity)
            .loadFromMediaStore(Uri.fromFile(oneDownloadedFile))
            .crossFade()
            .placeholder(R.color.placeholder_bg_color)
            .into(viewHolder.videoCover);


        viewHolder.uploadProgress.setVisibility(View.INVISIBLE);

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(oneDownloadedFile), "video/mp4");
                mActivity.startActivity(intent);
            }
        });
        viewHolder.fileName.setText(oneDownloadedFile.getName());

    }


    @Override
    public int getItemCount() {
        return Math.min(mDownloadManager.getCount() + mDownloadedFileList.size(), 4);
    }


    public class SimpleExportedVideoItemViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.video_cover)
        ImageView videoCover;

        @BindView(R.id.file_name)
        TextView fileName;

        @BindView(R.id.upload_progress)
        ProgressBar uploadProgress;


        public SimpleExportedVideoItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
