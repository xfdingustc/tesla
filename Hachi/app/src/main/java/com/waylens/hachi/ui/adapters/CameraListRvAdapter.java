package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.transee.ccam.CameraState;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.VdtCameraManager;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2015/8/20.
 */
public class CameraListRvAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {
    private static final String TAG = CameraListRvAdapter.class.getSimpleName();

    private VdtCameraManager mVdtCameraManager;
    private Context mContext;
    private OnCameraActionListener mOnCameraActionListener;

    public CameraListRvAdapter(Context context, VdtCameraManager cameraManager, OnCameraActionListener listener) {
        mVdtCameraManager = cameraManager;
        mContext = context;
        mOnCameraActionListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_camera_found, parent, false);
        return new CameraListItemViewHolder(view);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        CameraListItemViewHolder viewHolder = (CameraListItemViewHolder) holder;
        VdtCamera camera = mVdtCameraManager.getCamera(position);
        if (camera != null) {
            //Logger.t(TAG).d("Server name: " + camera.getServerName() + " SSID: " + camera
            //   .getSSID());
            if (camera.isConnected()) {
                viewHolder.mIvCameraIcon.setImageResource(R.drawable.camera_active);
                viewHolder.mBtnClips.setEnabled(true);
                viewHolder.mBtnPreview.setEnabled(true);
                viewHolder.mBtnClips.setColorFilter(mContext.getResources().getColor(R.color.style_color_primary));
                viewHolder.mBtnPreview.setColorFilter(mContext.getResources().getColor(R.color.style_color_primary));
                viewHolder.mBtnSettings.setColorFilter(mContext.getResources().getColor(R.color.style_color_primary));
            } else {
                viewHolder.mIvCameraIcon.setImageResource(R.drawable.camera_inactive);
                viewHolder.mBtnClips.setEnabled(false);
                viewHolder.mBtnPreview.setEnabled(false);
                viewHolder.mBtnClips.clearColorFilter();
                viewHolder.mBtnPreview.clearColorFilter();
                viewHolder.mBtnClips.setColorFilter(mContext.getResources().getColor(R.color.material_grey_600));
                viewHolder.mBtnPreview.setColorFilter(mContext.getResources().getColor(R.color.material_grey_600));
                viewHolder.mBtnSettings.setColorFilter(mContext.getResources().getColor(R.color.material_grey_600));
            }
            CameraState state = VdtCamera.getState(camera);
            if (TextUtils.isEmpty(state.mCameraName)) {
                viewHolder.mCameraName.setText(camera.getSSID());
            } else {
                viewHolder.mCameraName.setText(state.mCameraName);
            }

        }

        viewHolder.mBtnPreview.setOnClickListener(this);
        viewHolder.mBtnPreview.setTag(viewHolder);

        viewHolder.mBtnClips.setOnClickListener(this);
        viewHolder.mBtnClips.setTag(viewHolder);

        viewHolder.mBtnSettings.setOnClickListener(this);
        viewHolder.mBtnSettings.setTag(viewHolder);
    }

    @Override
    public int getItemCount() {
        return mVdtCameraManager.getTotalItems();
    }

    @Override
    public void onClick(View v) {

        CameraListItemViewHolder viewHolder = (CameraListItemViewHolder) v.getTag();
        int position = viewHolder.getAdapterPosition();
        VdtCamera camera = mVdtCameraManager.getCamera(position);
        if (mOnCameraActionListener == null || camera == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.btnClips:
                mOnCameraActionListener.onBrowseVideo(camera);
                break;
            case R.id.btnPreview:
                mOnCameraActionListener.onPreview(camera);
                break;
            case R.id.btn_settings:
                mOnCameraActionListener.onSetup(camera);
                break;
        }
    }


    public static class CameraListItemViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.ivCameraIcon)
        ImageView mIvCameraIcon;

        @Bind(R.id.tv_camera_name)
        TextView mCameraName;

        @Bind(R.id.btnClips)
        ImageButton mBtnClips;

        @Bind(R.id.btnPreview)
        ImageButton mBtnPreview;

        @Bind(R.id.btn_settings)
        ImageButton mBtnSettings;

        public CameraListItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public interface OnCameraActionListener {
        void onSetup(VdtCamera camera);

        void onBrowseVideo(VdtCamera camera);

        void onPreview(VdtCamera camera);
    }
}
