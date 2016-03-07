package com.waylens.hachi.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.waylens.hachi.R;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Richard on 3/2/16.
 */
public class GaugeListAdapter extends RecyclerView.Adapter<GaugeListAdapter.VH> {

    ArrayList<GaugeItem> mGaugeItems;

    public GaugeListAdapter(String[] supportedGauges, int[] gaugeDefaultSizes) {
        if (supportedGauges != null && gaugeDefaultSizes != null) {
            mGaugeItems = new ArrayList<>();
            int i = 0;
            for (String title : supportedGauges) {
                mGaugeItems.add(new GaugeItem(title, gaugeDefaultSizes[i]));
                i++;
            }
        }
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gauge, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(final VH holder, int position) {
        final GaugeItem gaugeItem = mGaugeItems.get(position);
        holder.titleView.setText(gaugeItem.title);
        if (gaugeItem.isEnable) {
            holder.radioGroup.setVisibility(View.VISIBLE);
            holder.radioGroup.check(getRadioButtonID(gaugeItem.sizeType));
        } else {
            holder.radioGroup.setVisibility(View.INVISIBLE);
        }

        holder.btnSwitch.setChecked(gaugeItem.isEnable);
        holder.btnSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableGauge(holder, isChecked, gaugeItem);
            }
        });
    }

    void enableGauge(VH holder, boolean isChecked, GaugeItem gaugeItem) {
        if (isChecked) {
            holder.radioGroup.setVisibility(View.VISIBLE);
            holder.radioGroup.check(getRadioButtonID(gaugeItem.sizeType));
        } else {
            holder.radioGroup.setVisibility(View.INVISIBLE);
        }
    }

    int getRadioButtonID(int sizeType) {
        switch (sizeType) {
            case GaugeItem.SIZE_SMALL:
                return R.id.btn_small;
            case GaugeItem.SIZE_MEDIUM:
                return R.id.btn_medium;
            case GaugeItem.SIZE_LARGE:
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

    static class GaugeItem {
        public static final int SIZE_UNKNOWN = -1;
        public static final int SIZE_SMALL = 0;
        public static final int SIZE_MEDIUM = 1;
        public static final int SIZE_LARGE = 2;

        public String title;

        public boolean isEnable;

        public int sizeType;

        public GaugeItem(String title, int defaultSizeType) {
            this.title = title;
            sizeType = defaultSizeType;
        }
    }
}
