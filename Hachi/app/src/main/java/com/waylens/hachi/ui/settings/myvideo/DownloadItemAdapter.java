package com.waylens.hachi.ui.settings.myvideo;

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
import com.waylens.hachi.R;
import com.waylens.hachi.app.DownloadManager;
import com.waylens.hachi.bgjob.download.Exportable;
import com.waylens.hachi.bgjob.download.DownloadHelper;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.glide_snipe_integration.SnipeGlideLoader;
import com.waylens.hachi.ui.dialogs.DialogHelper;


import org.ocpsoft.prettytime.PrettyTime;

import java.io.File;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/8/15.
 */
public class DownloadItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements DownloadManager.OnDownloadJobStateChangeListener {
    private static final String TAG = DownloadItemAdapter.class.getSimpleName();
    private final Activity mActivity;

    private DownloadManager mDownloadManager = DownloadManager.getManager();

    private PrettyTime mPrettyTime = new PrettyTime();

    private File[] mDownloadedFileList;

    public DownloadItemAdapter(Activity activity) {
        this.mActivity = activity;
        mDownloadManager.addListener(this);
        mDownloadedFileList = DownloadHelper.getDownloadedFileList();
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
    public void onDownloadJobStateChanged(Exportable job, int index) {
        notifyItemChanged(index);
    }

    @Override
    public void onDownloadJobAdded() {
        notifyDataSetChanged();
    }

    @Override
    public void onDownloadJobRemoved() {
        mDownloadedFileList = DownloadHelper.getDownloadedFileList();
        notifyDataSetChanged();
    }

    private void onBindDownloadingViewHolder(RecyclerView.ViewHolder holder, int position) {
        DownloadVideoItemViewHolder videoItemViewHolder = (DownloadVideoItemViewHolder) holder;
        Exportable job = DownloadManager.getManager().getDownloadJob(position);
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
    }

    private void onBindDownloadedViewHolder(RecyclerView.ViewHolder holder, final int position) {

        final DownloadVideoItemViewHolder videoItemViewHolder = (DownloadVideoItemViewHolder) holder;

        final File oneDownloadedFile = mDownloadedFileList[position];

        Glide.with(mActivity)
            .loadFromMediaStore(Uri.fromFile(oneDownloadedFile))
            .crossFade()
            .into(videoItemViewHolder.videoCover);


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
        videoItemViewHolder.videoDuration.setVisibility(View.GONE);

        videoItemViewHolder.btnMore.setVisibility(View.VISIBLE);
        videoItemViewHolder.description.setText(mPrettyTime.formatUnrounded(new Date(oneDownloadedFile.lastModified())));
        videoItemViewHolder.btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(mActivity, videoItemViewHolder.btnMore, Gravity.END);
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
                                        mDownloadedFileList = DownloadHelper.getDownloadedFileList();
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
    }


    @Override
    public int getItemCount() {
        return mDownloadManager.getCount() + mDownloadedFileList.length;
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
