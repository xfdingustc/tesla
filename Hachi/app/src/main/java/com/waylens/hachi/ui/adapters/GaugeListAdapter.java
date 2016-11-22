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
import com.waylens.hachi.view.gauge.GaugeSettingManager;
import com.waylens.hachi.view.gauge.GaugeInfoItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Richard on 3/2/16.
 */
public class GaugeListAdapter extends RecyclerView.Adapter<GaugeListAdapter.VH> {
    public static String TAG = GaugeListAdapter.class.getSimpleName();

    private final OnGaugeItemChangedListener mListener;
    List<GaugeInfoItem> mGaugeItems;
    List<Integer> mSupportSetting;

    public interface OnGaugeItemChangedListener {
        void onGaugeItemChanged(GaugeInfoItem item);
    }

    public GaugeListAdapter(OnGaugeItemChangedListener listener) {
        mGaugeItems = GaugeSettingManager.getManager().getSetting();
        mSupportSetting = GaugeSettingManager.getManager().getSupportedSetting();
        this.mListener = listener;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gauge, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(final VH holder, final int position) {
        final GaugeInfoItem gaugeItem = mGaugeItems.get(position);
        final int supportSetting = mSupportSetting.get(position);
        if (supportSetting != 7) {
            if ((supportSetting & GaugeInfoItem.MASK_LARGE_SIZE) == 0) {
                holder.btnLarge.setVisibility(View.GONE);
            }
            if ((supportSetting & GaugeInfoItem.MASK_MEDIUM_SIZE) == 0) {
                holder.btnMedium.setVisibility(View.GONE);
            }
            if ((supportSetting & GaugeInfoItem.MASK_SMALL_SIZE) == 0) {
                holder.btnSmall.setVisibility(View.GONE);
            }
        }
        holder.titleView.setText(gaugeItem.title);
        if (gaugeItem.isEnabled) {
            holder.radioGroup.setVisibility(View.VISIBLE);
            holder.radioGroup.check(getRadioButtonID(gaugeItem.option));
        } else {
            holder.radioGroup.setVisibility(View.INVISIBLE);
        }
        Logger.t(TAG).d("setChecked");
        holder.btnSwitch.setChecked(gaugeItem.isEnabled);
        holder.btnSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (holder.btnSwitch.isPressed()) {
                    gaugeItem.isEnabled = isChecked;
                    gaugeItem.option = isChecked?GaugeSettingManager.getDefaultGaugeSetting(gaugeItem.title):"";
                    enableGauge(holder, isChecked);
                    Logger.t(TAG).d(gaugeItem.isEnabled);
                    Logger.t(TAG).d(mGaugeItems.get(position).isEnabled);
                    if (mListener != null) {
                        mListener.onGaugeItemChanged(gaugeItem);
                    }
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
                gaugeItem.option = "small";
                break;
            case R.id.btn_medium:
                gaugeItem.option = "middle";
                break;
            case R.id.btn_large:
                gaugeItem.option = "large";
                break;
        }
    }

    void enableGauge(VH holder, boolean isChecked) {
        GaugeInfoItem gaugeItem = mGaugeItems.get(holder.getAdapterPosition());
        //gaugeItem.isEnabled = isChecked;
        //gaugeItem.option = isChecked?GaugeSettingManager.getDefaultGaugeSetting(gaugeItem.title):"";
        if (isChecked) {
            holder.radioGroup.setVisibility(View.VISIBLE);
            holder.radioGroup.check(getRadioButtonID(gaugeItem.option));
        } else {
            holder.radioGroup.setVisibility(View.INVISIBLE);
        }
    }

    int getRadioButtonID(String option) {
        if (option.equals("small")) {
            return R.id.btn_small;
        } else if (option.equals("middle")) {
            return R.id.btn_medium;
        } else if (option.equals("large")) {
            return R.id.btn_large;
        } else {
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

        @BindView(R.id.title)
        TextView titleView;

        @BindView(R.id.btn_small)
        View btnSmall;

        @BindView(R.id.btn_medium)
        View btnMedium;

        @BindView(R.id.btn_large)
        View btnLarge;

        @BindView(R.id.btn_switch)
        SwitchCompat btnSwitch;

        @BindView(R.id.radio_group)
        RadioGroup radioGroup;

        public VH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }


}
