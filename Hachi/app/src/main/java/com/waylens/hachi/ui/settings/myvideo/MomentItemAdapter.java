package com.waylens.hachi.ui.settings.myvideo;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
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
import com.waylens.hachi.ui.community.MomentActivity;
import com.waylens.hachi.ui.community.PhotoViewActivity;
import com.waylens.hachi.ui.entities.moment.MomentEx;
import com.waylens.hachi.utils.PrettyTimeUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/6/17.
 */
public class MomentItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = MomentItemAdapter.class.getSimpleName();

    private final Activity mActivity;
    private List<MomentEx> mUploadedMomentList = new ArrayList<>();


    public MomentItemAdapter(Activity activity) {
        this.mActivity = activity;

    }


    public void setUploadedMomentList(List<MomentEx> momentList) {
        this.mUploadedMomentList = momentList;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_simple, parent, false);
        return new VideoItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

        final VideoItemViewHolder viewHolder = (VideoItemViewHolder) holder;
        final MomentEx uploadedMoment = mUploadedMomentList.get(position);
        viewHolder.uploadProgress.setVisibility(View.GONE);
        viewHolder.momentTitle.setText(uploadedMoment.moment.title);
        viewHolder.uploadStatus.setVisibility(View.GONE);
        viewHolder.description.setText(PrettyTimeUtils.getTimeAgo(uploadedMoment.moment.uploadTime));
        viewHolder.description.setVisibility(View.VISIBLE);

        if (uploadedMoment.moment.isPictureMoment()) {
            if (!uploadedMoment.pictureUrls.isEmpty()) {
                final String cover = uploadedMoment.pictureUrls.get(0).getMomentPicturlUrl();
                Glide.with(mActivity)
                    .load(cover)
                    .crossFade()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(viewHolder.videoCover);
                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PhotoViewActivity.launch(mActivity, uploadedMoment, cover, position);
                    }
                });
            } else {
                viewHolder.itemView.setOnClickListener(null);
            }
            viewHolder.videoDuration.setVisibility(View.GONE);
            viewHolder.imageMoment.setVisibility(View.VISIBLE);


        } else {
            Glide.with(mActivity)
                .load(uploadedMoment.moment.thumbnail)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .crossFade()
                .into(viewHolder.videoCover);
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MomentActivity.launch(mActivity, uploadedMoment.moment.id, uploadedMoment.moment.thumbnail, viewHolder.videoCover);
                }
            });
            viewHolder.videoDuration.setText(DateUtils.formatElapsedTime(uploadedMoment.moment.duration / 1000l));
            viewHolder.imageMoment.setVisibility(View.GONE);
            viewHolder.videoDuration.setVisibility(View.VISIBLE);
        }


        viewHolder.btnMore.setVisibility(View.GONE);



    }


    @Override
    public int getItemCount() {
        return mUploadedMomentList.size();
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

        @BindView(R.id.image_moment)
        ImageView imageMoment;


        public VideoItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
