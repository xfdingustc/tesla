package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.orhanobut.logger.Logger;
import com.transee.ccam.Camera;
import com.transee.ccam.CameraManager;
import com.waylens.hachi.R;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2015/8/20.
 */
public class CameraListRvAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = CameraListRvAdapter.class.getSimpleName();

    private final Context mContext;
    private CameraManager mCameraManager = CameraManager.getManager();

    public CameraListRvAdapter(Context context) {
        this.mContext = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_camera_found, parent, false);
        return new CameraListItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        CameraListItemViewHolder viewHolder = (CameraListItemViewHolder)holder;
        //Camera camera = mCameraManager.getItem(position);
        viewHolder.mIvCameraIcon.setImageResource(R.drawable.camera_inactive);
    }

    @Override
    public int getItemCount() {
        return mCameraManager.getTotalItems();
    }


    public static class CameraListItemViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.ivCameraIcon)
        ImageView mIvCameraIcon;

        public CameraListItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
