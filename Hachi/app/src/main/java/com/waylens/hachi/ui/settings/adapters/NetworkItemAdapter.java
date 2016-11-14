package com.waylens.hachi.ui.settings.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.camera.entities.NetworkItemBean;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/11/4.
 */

public class NetworkItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context mContext;
    private final OnNetworkItemClickedListener mOnItemClickListener;

    private List<NetworkItemBean> mNetworkList;

    public NetworkItemAdapter(Context context, OnNetworkItemClickedListener listener) {
        this.mContext = context;
        this.mOnItemClickListener = listener;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_network, parent, false);
        return new NetworkItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        NetworkItemBean networkItem = mNetworkList.get(position);
        NetworkItemViewHolder viewHolder = (NetworkItemViewHolder) holder;
//            Logger.t(TAG).d("set ssid: " + networkItem.ssid);
        viewHolder.tvSsid.setText(networkItem.ssid);


        switch (networkItem.status) {
            case NetworkItemBean.CONNECT_STATUS_NONE:
                viewHolder.wifiStatus.setVisibility(View.GONE);
                break;
            case NetworkItemBean.CONNECT_STATUS_SAVED:
                viewHolder.wifiStatus.setVisibility(View.VISIBLE);
                viewHolder.wifiStatus.setText(R.string.saved);
                break;
            case NetworkItemBean.CONNECT_STATUS_AUTHENTICATION:
                viewHolder.wifiStatus.setVisibility(View.VISIBLE);
                viewHolder.wifiStatus.setText(R.string.authenticating);
                break;
            case NetworkItemBean.CONNECT_STATUS_AUTHENTICATION_PROBLEM:
                viewHolder.wifiStatus.setVisibility(View.VISIBLE);
                viewHolder.wifiStatus.setText(R.string.authentication_problem);
                break;
        }

        int wifiSignalImageRes = getWifiSignIcon(networkItem.signalLevel, networkItem.flags != null && !networkItem.flags.isEmpty());
        viewHolder.ivWifiSignal.setImageResource(wifiSignalImageRes);
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NetworkItemBean itemBean = mNetworkList.get(position);
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClicked(itemBean);
                }
            }
        });
    }


    @Override
    public int getItemCount() {
        int size = mNetworkList == null ? 0 : mNetworkList.size();
        return size;
    }

    public void setNetworkList(List<NetworkItemBean> networkList) {
        mNetworkList = networkList;
        notifyDataSetChanged();
    }

    public class NetworkItemViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.wifiContainer)
        LinearLayout mContainer;

        @BindView(R.id.tvSsid)
        TextView tvSsid;


        @BindView(R.id.ivWifiSignal)
        ImageView ivWifiSignal;

        @BindView(R.id.wifi_status)
        TextView wifiStatus;


        public NetworkItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }


    private int getWifiSignIcon(int signalLevel, boolean isLocked) {
        if (isLocked) {
            if (signalLevel >= -55) {
                return R.drawable.ic_signal_wifi_4_bar_lock;
            } else if (signalLevel >= -70) {
                return R.drawable.ic_signal_wifi_3_bar_lock;
            } else if (signalLevel >= -85) {
                return R.drawable.ic_signal_wifi_2_bar_lock;
            } else {
                return R.drawable.ic_signal_wifi_1_bar_lock;
            }
        } else {
            if (signalLevel >= -55) {
                return R.drawable.ic_signal_wifi_4_bar;
            } else if (signalLevel >= -70) {
                return R.drawable.ic_signal_wifi_3_bar;
            } else if (signalLevel >= -85) {
                return R.drawable.ic_signal_wifi_2_bar;
            } else {
                return R.drawable.ic_signal_wifi_1_bar;
            }
        }
    }

    public interface OnNetworkItemClickedListener {
        void onItemClicked(NetworkItemBean itemBean);
    }
}
