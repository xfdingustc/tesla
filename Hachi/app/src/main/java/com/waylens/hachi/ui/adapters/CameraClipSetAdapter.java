package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.hardware.VdtCamera;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.activities.ClipEditActivity;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;

import butterknife.Bind;
import butterknife.ButterKnife;


public class CameraClipSetAdapter extends RecyclerView.Adapter<CameraClipSetAdapter.CameraClipViewHolder> {

    private static final String TAG = CameraClipSetAdapter.class.getSimpleName();
    private final VdbImageLoader mVdbImageLoader;
    private Context mContext;

    private ClipSet mClipSet = null;

    private ClipActionListener mClipActionListener;

    public CameraClipSetAdapter(Context context, VdbRequestQueue queue) {
        this.mContext = context;
        this.mVdbImageLoader = new VdbImageLoader(queue);
    }

    public void setClipSet(ClipSet clipSet) {
        mClipSet = clipSet;
        notifyDataSetChanged();
    }

    public void setClipActionListener(ClipActionListener listener) {
        mClipActionListener = listener;
    }

    @Override
    public CameraClipViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_camera_video,
                parent, false);
        return new CameraClipViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final CameraClipViewHolder holder, int position) {
        final Clip clip = mClipSet.getClip(position);
        holder.videoDesc.setText("Mocked description");
        holder.videoTime.setText(clip.getDateTimeString());
        holder.videoDuration.setText(clip.getDurationString());
        ClipPos clipPos = new ClipPos(clip, clip.getStartTime(), ClipPos.TYPE_POSTER, false);
        mVdbImageLoader.displayVdbImage(clipPos, holder.videoCover);
        holder.mBtnVideoEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipEditActivity.launch(mContext, clip);
            }
        });
        holder.mBtnVideoPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClipActionListener != null) {
                    mClipActionListener.onRequestVideoPlay(holder, clip);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        if (mClipSet != null) {
            return mClipSet.getCount();
        } else {
            return 0;
        }
    }

    @Override
    public void onViewDetachedFromWindow(CameraClipViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (mClipActionListener != null) {
            mClipActionListener.onRemoveVideoPlayFragment(holder);
        }
    }

    public static class CameraClipViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.video_desc)
        TextView videoDesc;

        @Bind(R.id.video_time)
        TextView videoTime;

        @Bind(R.id.video_duration)
        TextView videoDuration;

        @Bind(R.id.video_cover)
        ImageView videoCover;

        @Bind(R.id.btn_video_play)
        ImageButton mBtnVideoPlay;

        @Bind(R.id.btn_video_edit)
        ImageButton mBtnVideoEdit;

        @Bind(R.id.video_container)
        public FrameLayout videoContainer;

        public CameraClipViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            videoContainer.setId(ViewUtils.generateViewId());
        }
    }

    public interface ClipActionListener {
        void onRequestVideoPlay(CameraClipViewHolder holder, Clip clip);

        void onRemoveVideoPlayFragment(CameraClipViewHolder holder);
    }
}
