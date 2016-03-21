package com.waylens.hachi.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.ui.entities.NetworkItemBean;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/3/21.
 */
public class ClientConnectFragment extends BaseFragment {

    @Bind(R.id.rvWifiList)
    RecyclerView mRvWifiList;
    private List<NetworkItemBean> mNetworkList;

    private NetworkItemAdapter mNetworkItemAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_client_connect, savedInstanceState);
        initViews();
        return view;
    }

    private void initViews() {
        mRvWifiList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mNetworkItemAdapter = new NetworkItemAdapter();
        mRvWifiList.setAdapter(mNetworkItemAdapter);

        mVdtCamera.scanHost(new VdtCamera.OnScanHostListener() {
            @Override
            public void OnScanHostResult(List<NetworkItemBean> networkList) {
                mNetworkList = networkList;
                mNetworkItemAdapter.notifyDataSetChanged();
            }


        });
    }

    public class NetworkItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final Context mContext;

        public NetworkItemAdapter() {
            this.mContext = getActivity();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.item_network, parent, false);
            return new NetworkItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            NetworkItemBean networkItem = mNetworkList.get(position);
            NetworkItemViewHolder viewHolder = (NetworkItemViewHolder)holder;
            viewHolder.tvSsid.setText(networkItem.ssid);
        }


        @Override
        public int getItemCount() {
            return mNetworkList == null ? 0 : mNetworkList.size();
        }

        public class NetworkItemViewHolder extends RecyclerView.ViewHolder {

            @Bind(R.id.tvSsid)
            TextView tvSsid;

            public NetworkItemViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }
        }
    }
}
