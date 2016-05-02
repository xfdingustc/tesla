package com.waylens.hachi.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.clips.clipplay2.GaugeInfoItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Richard on 3/2/16.
 */
public class GaugeListAdapter extends RecyclerView.Adapter<GaugeListAdapter.VH> {

    private final OnGaugeItemChangedListener mListener;
    ArrayList<GaugeInfoItem> mGaugeItems;

    public interface OnGaugeItemChangedListener {
        void onGaugeItemChanged(GaugeInfoItem item);
    }

    public GaugeListAdapter(String[] supportedGauges, OnGaugeItemChangedListener listener) {
        if (supportedGauges != null) {
            mGaugeItems = new ArrayList<>();
            int i = 0;
            for (String title : supportedGauges) {
                mGaugeItems.add(new GaugeInfoItem(i, title, GaugeInfoItem.DEFAULT_OPTIONS[i]));
                i++;
            }
        }

        this.mListener = listener;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gauge, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(final VH holder, int position) {
        final GaugeInfoItem gaugeItem = mGaugeItems.get(position);
        holder.titleView.setText(gaugeItem.title);
        if (gaugeItem.isEnabled) {
            holder.radioGroup.setVisibility(View.VISIBLE);
            holder.radioGroup.check(getRadioButtonID(gaugeItem.option));
        } else {
            holder.radioGroup.setVisibility(View.INVISIBLE);
        }

        holder.btnSwitch.setChecked(gaugeItem.isEnabled);
        holder.btnSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableGauge(holder, isChecked);
                if (mListener != null) {
                    mListener.onGaugeItemChanged(gaugeItem);
                }
            }
        });

        holder.radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updateOption(holder, checkedId);
                if (mListener != null) {
                    mListener.onGaugeItemChanged(gaugeItem);
                }
            }
        });
    }

    void updateOption(VH holder, int checkedId) {
        GaugeInfoItem gaugeItem = mGaugeItems.get(holder.getAdapterPosition());
        switch (checkedId) {
            case R.id.btn_small:
                gaugeItem.option = GaugeInfoItem.Option.small;
                break;
            case R.id.btn_medium:
                gaugeItem.option = GaugeInfoItem.Option.middle;
                break;
            case R.id.btn_large:
                gaugeItem.option = GaugeInfoItem.Option.large;
                break;
        }
    }

    void enableGauge(VH holder, boolean isChecked) {
        GaugeInfoItem gaugeItem = mGaugeItems.get(holder.getAdapterPosition());
        gaugeItem.isEnabled = isChecked;
        if (isChecked) {
            holder.radioGroup.setVisibility(View.VISIBLE);
            holder.radioGroup.check(getRadioButtonID(gaugeItem.option));
        } else {
            holder.radioGroup.setVisibility(View.INVISIBLE);
        }
    }

    int getRadioButtonID(GaugeInfoItem.Option option) {
        switch (option) {
            case small:
                return R.id.btn_small;
            case middle:
                return R.id.btn_medium;
            case large:
                return R.id.btn_large;
            default:
                return -1;
        }
    }

    @Override
    public int getItemCount() {
        if (mGaugeItems == null) {
            return 0;
        } else {
            return mGaugeItems.size();
        }
    }

    public JSONObject toJSOptions() {
        JSONObject jsonObject = new JSONObject();
        try {
            for (GaugeInfoItem infoItem : mGaugeItems) {
                jsonObject.put(infoItem.getJSParam(), infoItem.getOption());
            }
        } catch (JSONException e) {
            Logger.t("GaugeListAdapter").e(e, "");
        }
        return jsonObject;
    }

    static class VH extends RecyclerView.ViewHolder {

        @Bind(R.id.title)
        TextView titleView;

        @Bind(R.id.btn_small)
        View btnSmall;

        @Bind(R.id.btn_medium)
        View btnMedium;

        @Bind(R.id.btn_large)
        View btnLarge;

        @Bind(R.id.btn_switch)
        SwitchCompat btnSwitch;

        @Bind(R.id.radio_group)
        RadioGroup radioGroup;

        public VH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }


}
