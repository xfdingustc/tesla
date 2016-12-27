package com.waylens.hachi.ui.settings.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.waylens.hachi.R;
import com.waylens.hachi.rest.bean.Vehicle;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by lshw on 16/12/26.
 */

public class CarListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context mContext;
    private final OnCarItemClickedListener mOnItemClickListener;

    private List<Vehicle> mCarList;
    private Vehicle mSelectedVehicle = null;

    public CarListAdapter(Context context, OnCarItemClickedListener listener) {
        this.mContext = context;
        this.mOnItemClickListener = listener;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_car, parent, false);
        return new CarItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final Vehicle oneCar = mCarList.get(position);
        CarItemViewHolder viewHolder = (CarItemViewHolder) holder;
        viewHolder.tvCarMode.setText(oneCar.maker + "  " + oneCar.model + "  " + oneCar.year);
        viewHolder.ivChecked.setVisibility(oneCar.equals(mSelectedVehicle) ? View.VISIBLE : View.INVISIBLE);
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelectedVehicle = oneCar;
                Vehicle itemBean = mCarList.get(position);
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClicked(itemBean);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        int size = mCarList == null ? 0 : mCarList.size();
        return size;
    }

    public void setCarList(List<Vehicle> carList, Vehicle selectedVehicle) {
        mCarList = carList;
        mSelectedVehicle = selectedVehicle;
        notifyDataSetChanged();
    }

    public class CarItemViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tv_car_mode)
        TextView tvCarMode;

        @BindView(R.id.iv_checked)
        ImageView ivChecked;

        public CarItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public interface OnCarItemClickedListener {
        void onItemClicked(Vehicle item);
    }
}
