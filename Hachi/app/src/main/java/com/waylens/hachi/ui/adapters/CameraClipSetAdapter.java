package com.waylens.hachi.ui.adapters;

import android.graphics.drawable.BitmapDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.transee.common.ViewHolder;
import com.transee.vdb.Clip;
import com.transee.vdb.ClipSet;
import com.waylens.hachi.R;

import butterknife.Bind;
import butterknife.ButterKnife;


public class CameraClipSetAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ClipSetRecyclerAdapter";

    private ClipSet mClipSet;
    private BitmapDrawable[] mBitmaps;

    public CameraClipSetAdapter(ClipSet clipSet) {
        setClipSet(clipSet);
    }

    public void setClipSet(ClipSet clipSet) {
        mClipSet = clipSet;
        if (clipSet != null) {
            mBitmaps = new BitmapDrawable[clipSet.getCount()];
        }
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_camera_video,
            parent, false);
        return new CameraClipViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        Clip clip = mClipSet.getClip(position);
        CameraClipViewHolder holder = (CameraClipViewHolder)viewHolder;
        holder.videoDesc.setText("Mocked description");
        holder.videoTime.setText(clip.getDateTimeString());
        holder.videoDuration.setText(clip.getDurationString());
        if (mBitmaps[position] != null) {
            holder.videoCover.setBackground(mBitmaps[position]);
        }
    }

    @Override
    public int getItemCount() {
        if (mClipSet != null) {
            return mClipSet.getCount();
        } else {
            return 0;
        }
    }

    public void setClipCover(BitmapDrawable bitmapDrawable, int position) {
        if (position < 0 || position > mBitmaps.length) {
            Logger.t(TAG).e("Illegal argument: " + position);
            return;
        }
        mBitmaps[position] = bitmapDrawable;
        notifyDataSetChanged();
    }


    public class CameraClipViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.video_desc)
        TextView videoDesc;

        @Bind(R.id.video_time)
        TextView videoTime;

        @Bind(R.id.video_duration)
        TextView videoDuration;

        @Bind(R.id.video_cover)
        View videoCover;


        public CameraClipViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
