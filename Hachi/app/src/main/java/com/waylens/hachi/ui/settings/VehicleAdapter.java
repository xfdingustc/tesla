package com.waylens.hachi.ui.settings;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.xfdingustc.snipe.utils.ToStringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/8/4.
 */
public class VehicleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context mContext;
    private final OnVehicleClickListener mOnVehicleClickListener;
    private List<Vehicle> mVehicleList = new ArrayList<>();

    public VehicleAdapter(Context context, OnVehicleClickListener listener) {
        this.mContext = context;
        this.mOnVehicleClickListener = listener;
    }

    public void setVehicleList(String vehicleList) {
        if (vehicleList == null) {
            return;
        }
        try {
            setVehicleList(new JSONObject(vehicleList));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setVehicleList(JSONObject jsonObject) {
        try {
            JSONArray country = jsonObject.getJSONArray("vehicles");
            mVehicleList.clear();
            for (int i = 0; i < country.length(); i++) {
                JSONObject object = country.getJSONObject(i);
                Vehicle oneVehicle = new Vehicle();
                oneVehicle.modelYearID = object.getLong("modelYearID");
                oneVehicle.maker = object.getString("maker");
                oneVehicle.year = object.getInt("year");
                oneVehicle.model = object.getString("model");

                mVehicleList.add(oneVehicle);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vehicle, parent, false);
        return new VehicleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        VehicleViewHolder vehicleViewHolder = (VehicleViewHolder) holder;
        Vehicle vehicle = mVehicleList.get(position);
        Logger.t("TAG").d(vehicle.toString());
        vehicleViewHolder.vehicle.setText(vehicle.maker + "  " + vehicle.model + "  " + vehicle.year);
    }

    @Override
    public int getItemCount() {
        return mVehicleList.size();
    }

    public class VehicleViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.vehicle)
        TextView vehicle;

        @OnClick(R.id.vehicle)
        public void onVehicleClicked() {
            final int position = this.getAdapterPosition();
            new MaterialDialog.Builder(mContext)
                .content(R.string.delete_car_confirm)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {


                        long modelYearId = Long.valueOf(mVehicleList.get(position).modelYearID);
                        if (mOnVehicleClickListener != null) {
                            mOnVehicleClickListener.onVehicleClicked(modelYearId);
                        }

                        mVehicleList.remove(position);
                        notifyItemRemoved(position);


                    }
                })
                .show();
        }

        public VehicleViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public class Vehicle {
        public long modelYearID;
        public String maker;
        public String model;
        public int year;

        @Override
        public String toString() {
            return ToStringUtils.getString(this);
        }
    }

    public interface OnVehicleClickListener {
        void onVehicleClicked(long modelId);
    }

}
