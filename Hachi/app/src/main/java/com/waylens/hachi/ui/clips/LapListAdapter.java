package com.waylens.hachi.ui.clips;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.snipe.remix.AvrproLapData;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by lshw on 16/12/15.
 */
public class LapListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = LapListAdapter.class.getSimpleName();
    private static final int LAP_TYPE_NORMAL = 0x0001;
    private static final int LAP_TYPE_BEST = 0x0002;

    private List<AvrproLapData> mLapDataList = new ArrayList<>();

    private int longestLapTime = 100;

    private final Context mContext;
    private OnLapClickListener mOnLapClickListener;

    public LapListAdapter(Context context, OnLapClickListener listener) {
        this.mContext = context;
        this.mOnLapClickListener = listener;
    }

    public void setLapDataList(List<AvrproLapData> lapDataList) {
        mLapDataList = lapDataList;
        int minDurationIndex = -1, maxDurationIndex = -1;
        int minDuration = Integer.MAX_VALUE, maxDuration = Integer.MIN_VALUE;
        for (int i = 0; i < lapDataList.size(); i++) {
            AvrproLapData data = lapDataList.get(i);
            if (data.lap_time_ms < minDuration) {
                minDuration = data.lap_time_ms;
                minDurationIndex = i;
            }
            if (data.lap_time_ms > maxDuration){
                maxDuration = data.lap_time_ms;
                maxDurationIndex = i;
            }
        }
        if (minDurationIndex >= 0) {
            mLapDataList.get(minDurationIndex).isBestLap = true;
        }
        if (maxDurationIndex >= 0) {
            longestLapTime = maxDuration;
        }
        Logger.t(TAG).d("set lap data list!" + lapDataList.size());
        Logger.t(TAG).d("max duration index:" + maxDurationIndex);
        Logger.t(TAG).d("max duration:" + longestLapTime);
        notifyDataSetChanged();
    }

    public void clear() {
        mLapDataList.clear();
        longestLapTime = 0;
        notifyDataSetChanged();
    }


    @Override
    public int getItemViewType(int position) {
        if (position < mLapDataList.size()) {
            if (mLapDataList.get(position).isBestLap) {
                return LAP_TYPE_BEST;
            } else {
                return LAP_TYPE_NORMAL;
            }
        }
        return LAP_TYPE_NORMAL;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == LAP_TYPE_BEST) {
            View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_lap_timer_best, parent, false);
            return new BestLapViewHolder(itemView);
        } else {
            View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_lap_timer_normal, parent, false);
            return new NormalLapViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOnLapClickListener != null) {
                    mOnLapClickListener.onLapClicked(position);
                }
            }
        });
        int viewType = getItemViewType(position);
        if (viewType == LAP_TYPE_BEST) {
            onBindBestLapViewHolder((BestLapViewHolder) holder, position);
        } else if (viewType == LAP_TYPE_NORMAL){
            onBindNormalViewHolder((NormalLapViewHolder) holder, position);
        }
    }


    private void onBindBestLapViewHolder(final BestLapViewHolder holder, final int position) {
        AvrproLapData lapData = mLapDataList.get(position);
        holder.lapPbDuration.setProgress((lapData.lap_time_ms * 100) / longestLapTime);
        holder.lapTitle.setText("Lap " + (position + 1));
        holder.lapTvDuration.setText(formatLapTime(lapData.lap_time_ms));
    }


    private void onBindNormalViewHolder(NormalLapViewHolder holder, int position) {
        AvrproLapData lapData = mLapDataList.get(position);
        holder.lapPbDuration.setProgress((lapData.lap_time_ms * 100) / longestLapTime);
        holder.lapTitle.setText("Lap " + (position + 1));
        holder.lapTvDuration.setText(formatLapTime(lapData.lap_time_ms));
    }

    private String formatLapTime(int timeMs) {
        NumberFormat formatter = new DecimalFormat("#0.00");
        return DateUtils.formatElapsedTime(timeMs / 1000) + formatter.format((double)timeMs % 1000 / 1000).substring(1);
    }

    @Override
    public int getItemCount() {
        if(mLapDataList == null) {
            return 0;
        } else {
            return mLapDataList.size();
        }
    }

    public static class NormalLapViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.tv_title)
        TextView lapTitle;

        @BindView(R.id.pb_duration)
        ProgressBar lapPbDuration;

        @BindView(R.id.tv_duration)
        TextView lapTvDuration;

        public NormalLapViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

        }
    }

    public static class BestLapViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.iv_best_lap)
        ImageView lapIvBest;

        @BindView(R.id.tv_title)
        TextView lapTitle;

        @BindView(R.id.pb_duration)
        ProgressBar lapPbDuration;

        @BindView(R.id.tv_duration)
        TextView lapTvDuration;

        public BestLapViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public interface OnLapClickListener {
        void onLapClicked(int lapId);
    }
}
