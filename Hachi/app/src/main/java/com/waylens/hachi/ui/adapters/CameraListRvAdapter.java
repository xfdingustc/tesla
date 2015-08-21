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
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.VdtCamera;
import com.waylens.hachi.hardware.VdtCameraManager;
import com.waylens.hachi.ui.activities.BrowseCameraActivity;
import com.waylens.hachi.ui.activities.CameraControlActivity;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2015/8/20.
 */
public class CameraListRvAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {
    private static final String TAG = CameraListRvAdapter.class.getSimpleName();

    private final Context mContext;
    private VdtCameraManager mVdtCameraManager = VdtCameraManager.getManager();

    public CameraListRvAdapter(Context context) {
        this.mContext = context;
        mVdtCameraManager.addCallback(new VdtCameraManager.Callback() {
            @Override
            public void onCameraConnecting(VdtCamera vdtCamera) {

            }

            @Override
            public void onCameraConnected(VdtCamera vdtCamera) {
                Logger.t(TAG).d("on Camera Connected");
                notifyDataSetChanged();
            }

            @Override
            public void onCameraVdbConnected(VdtCamera vdtCamera) {
                Logger.t(TAG).d("camera vdb connected");

                notifyDataSetChanged();
            }

            @Override
            public void onCameraDisconnected(VdtCamera vdtCamera) {

            }

            @Override
            public void onCameraStateChanged(VdtCamera vdtCamera) {

            }

            @Override
            public void onWifiListChanged() {

            }
        });
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_camera_found, parent, false);
        return new CameraListItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        CameraListItemViewHolder viewHolder = (CameraListItemViewHolder) holder;
        VdtCamera camera = mVdtCameraManager.getCamera(position);
        if (camera != null) {
            //Logger.t(TAG).d("Server name: " + camera.getServerName() + " SSID: " + camera
             //   .getSSID());
            if (camera.isConnected() == false) {
                viewHolder.mIvCameraIcon.setImageResource(R.drawable.camera_inactive);
                viewHolder.mBtnClips.setEnabled(false);
                viewHolder.mBtnPreview.setEnabled(false);
                viewHolder.mBtnClips.clearColorFilter();
                viewHolder.mBtnPreview.clearColorFilter();
            } else {
                viewHolder.mIvCameraIcon.setImageResource(R.drawable.camera_active);
                viewHolder.mBtnClips.setEnabled(true);
                viewHolder.mBtnPreview.setEnabled(true);
                viewHolder.mBtnClips.setColorFilter(mContext.getResources().getColor(R.color.style_color_primary));
                viewHolder.mBtnPreview.setColorFilter(mContext.getResources().getColor(R.color.style_color_primary));
            }
            viewHolder.mTvServerName.setText(camera.getServerName());
            viewHolder.mTvSsid.setText(camera.getSSID());
        }

        viewHolder.mBtnPreview.setOnClickListener(this);
        viewHolder.mBtnPreview.setTag(viewHolder);

        viewHolder.mBtnClips.setOnClickListener(this);
        viewHolder.mBtnClips.setTag(viewHolder);

    }

    @Override
    public int getItemCount() {
        return mVdtCameraManager.getTotalItems();
    }

    @Override
    public void onClick(View v) {
        CameraListItemViewHolder viewHolder;
        int position;
        VdtCamera camera;
        switch (v.getId()) {
            case R.id.btnClips:
                Logger.t(TAG).d("BtnClips clicked");
                viewHolder = (CameraListItemViewHolder)v.getTag();
                position = viewHolder.getPosition();
                camera = mVdtCameraManager.getCamera(position);
                BrowseCameraActivity.launch(mContext, camera);
                break;
            case R.id.btnPreview:
                Logger.t(TAG).d("BtnPreview clicked");
                viewHolder = (CameraListItemViewHolder)v.getTag();
                position = viewHolder.getPosition();
                camera = mVdtCameraManager.getCamera(position);
                CameraControlActivity.launch(mContext, camera);
                break;
        }
    }


    public static class CameraListItemViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.ivCameraIcon)
        ImageView mIvCameraIcon;

        @Bind(R.id.tvServerName)
        TextView mTvServerName;

        @Bind(R.id.tvSsid)
        TextView mTvSsid;

        @Bind(R.id.btnClips)
        ImageButton mBtnClips;

        @Bind(R.id.btnPreview)
        ImageButton mBtnPreview;

        public CameraListItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
