package com.waylens.hachi.ui.settings.adapters;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.export.ExportManager;
import com.waylens.hachi.bgjob.export.ExportableJob;
import com.waylens.hachi.bgjob.export.event.ExportEvent;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.glide_snipe_integration.SnipeGlideLoader;
import com.waylens.hachi.ui.dialogs.DialogHelper;
import com.waylens.hachi.utils.FileUtils;
import com.waylens.hachi.utils.PrettyTimeUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/8/15.
 */
public class DownloadItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = DownloadItemAdapter.class.getSimpleName();
    private final Activity mActivity;

    private ExportManager mDownloadManager = ExportManager.getManager();

    private File[] mDownloadedFileList;

    private int mDownloadingCount = 0;

    public void handleEvent(ExportEvent event) {
        Logger.t(TAG).d("event type" + event.getWhat());
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


    public DownloadItemAdapter(Activity activity) {
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_simple, parent, false);

        return new DownloadVideoItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position < mDownloadingCount) {
            onBindDownloadingViewHolder(holder, position);
        } else {
            onBindDownloadedViewHolder(holder, position - mDownloadingCount);
        }

    }


    private void onBindDownloadingViewHolder(RecyclerView.ViewHolder holder, int position) {
        DownloadVideoItemViewHolder videoItemViewHolder = (DownloadVideoItemViewHolder) holder;
        ExportableJob job = ExportManager.getManager().getDownloadJob(position);
        if (job == null) {
            return;
        }
        videoItemViewHolder.uploadProgress.setVisibility(View.VISIBLE);
        videoItemViewHolder.uploadProgress.setProgress(job.getExportProgress());
        Glide.with(mActivity)
            .using(new SnipeGlideLoader(VdtCameraManager.getManager().getCurrentVdbRequestQueue()))
            .load(job.getClipStartPos())
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .crossFade()
            .into(videoItemViewHolder.videoCover);
        if (job.getOutputFile() != null) {
            videoItemViewHolder.momentTitle.setText(new File(job.getOutputFile()).getName());
        }
        videoItemViewHolder.videoDuration.setVisibility(View.GONE);
        videoItemViewHolder.btnMore.setVisibility(View.GONE);
        videoItemViewHolder.description.setText(mActivity.getString(R.string.exported, String.valueOf(job.getExportProgress())));
        videoItemViewHolder.imageMoment.setVisibility(View.GONE);
    }

    private void onBindDownloadedViewHolder(RecyclerView.ViewHolder holder, final int position) {

        final DownloadVideoItemViewHolder viewHolder = (DownloadVideoItemViewHolder) holder;

        final File oneDownloadedFile = mDownloadedFileList[position];

        Glide.with(mActivity)
            .loadFromMediaStore(Uri.fromFile(oneDownloadedFile))
            .crossFade()
            .into(viewHolder.videoCover);


        viewHolder.momentTitle.setText(oneDownloadedFile.getName());
        viewHolder.uploadProgress.setVisibility(View.INVISIBLE);

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(oneDownloadedFile), "video/mp4");
                mActivity.startActivity(intent);
            }
        });
        viewHolder.videoDuration.setVisibility(View.GONE);

        viewHolder.btnMore.setVisibility(View.VISIBLE);
        viewHolder.description.setText(PrettyTimeUtils.getTimeAgo(oneDownloadedFile.lastModified()));
        viewHolder.btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(mActivity, viewHolder.btnMore, Gravity.END);
                popupMenu.getMenuInflater().inflate(R.menu.menu_downloaded, popupMenu.getMenu());

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.delete:
                                DialogHelper.showDeleteFileConfirmDialog(mActivity, oneDownloadedFile, new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        oneDownloadedFile.delete();
                                        mDownloadedFileList = FileUtils.getExportedFileList();
                                        notifyDataSetChanged();
                                    }
                                });

                                break;
                        }
                        return true;
                    }
                });
                popupMenu.show();
            }
        });
        viewHolder.imageMoment.setVisibility(View.GONE);
    }


    @Override
    public int getItemCount() {
        mDownloadingCount = mDownloadManager.getCount();
        return mDownloadingCount + mDownloadedFileList.length;
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

        @BindView(R.id.image_moment)
        ImageView imageMoment;

        public DownloadVideoItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
