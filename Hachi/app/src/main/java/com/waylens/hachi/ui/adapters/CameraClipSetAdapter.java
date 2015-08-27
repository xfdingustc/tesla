package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.VdtCamera;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.activities.ClipEditActivity;

import butterknife.Bind;
import butterknife.ButterKnife;


public class CameraClipSetAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {

    private static final String TAG = CameraClipSetAdapter.class.getSimpleName();
    private final VdbRequestQueue mRequestQueue;
    private final VdbImageLoader mVdbImageLoader;
    private final VdtCamera mVdtCamera;
    private Context mContext;

    private ClipSet mClipSet = null;



    public CameraClipSetAdapter(Context context, VdtCamera vdtCamera, VdbRequestQueue queue) {
        this.mContext = context;
        this.mVdtCamera = vdtCamera;
        this.mRequestQueue = queue;
        this.mVdbImageLoader = new VdbImageLoader(mRequestQueue);
    }

    public void setClipSet(ClipSet clipSet) {
        Logger.t(TAG).d("set clip set!!!!!!");
        mClipSet = clipSet;
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
        CameraClipViewHolder holder = (CameraClipViewHolder) viewHolder;
        holder.videoDesc.setText("Mocked description");
        holder.videoTime.setText(clip.getDateTimeString());
        holder.videoDuration.setText(clip.getDurationString());



        ClipPos clipPos = new ClipPos(clip, clip.getStartTime(), ClipPos.TYPE_POSTER, false);
        mVdbImageLoader.displayVdbImage(clipPos, holder.videoCover);



        // set onClickListener
        holder.mBtnVideoEdit.setOnClickListener(this);
        holder.mBtnVideoEdit.setTag(holder);
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnVideoEdit:
                CameraClipViewHolder holder = (CameraClipViewHolder) v.getTag();
                int position = holder.getAdapterPosition();
                Clip clip = mClipSet.getClip(position);
                ClipEditActivity.launch(mContext, mVdtCamera, clip);

                break;
        }
    }


    public class CameraClipViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.video_desc)
        TextView videoDesc;

        @Bind(R.id.video_time)
        TextView videoTime;

        @Bind(R.id.video_duration)
        TextView videoDuration;

        @Bind(R.id.video_cover)
        ImageView videoCover;

        @Bind(R.id.btnVideoEdit)
        ImageButton mBtnVideoEdit;


        public CameraClipViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
