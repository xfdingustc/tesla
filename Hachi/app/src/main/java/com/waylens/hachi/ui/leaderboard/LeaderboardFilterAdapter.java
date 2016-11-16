package com.waylens.hachi.ui.leaderboard;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.waylens.hachi.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/11/16.
 */

public class LeaderboardFilterAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private final Context mContext;
    private List<String> mFilterList = new ArrayList<>();
    private String mSelected;
    private final OnFilterItemClickListener mListener;
    private int mSelectedIndex;

    public LeaderboardFilterAdapter(Context context, String[] filterList, OnFilterItemClickListener listener) {
        this.mContext = context;
        this.mFilterList = Arrays.asList(filterList);
        mListener = listener;
    }

    public void setSelected(String filter) {
        mSelected = filter;
        for (int i = 0; i < mFilterList.size(); i++) {
            String oneFilter = mFilterList.get(i);
            if (oneFilter.equals(filter)) {
                mSelectedIndex = i;
                break;
            }
        }
        notifyDataSetChanged();
    }

    public int getSelectedIndex() {
        return mSelectedIndex;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_filter, parent, false);
        return new LeaderboardFilterItem(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        LeaderboardFilterItem viewHolder = (LeaderboardFilterItem)holder;
        String oneFilter = mFilterList.get(position);
        viewHolder.itemFilter.setText(oneFilter);
        if (position == mSelectedIndex) {
            viewHolder.itemFilter.setBackgroundResource(R.drawable.item_filter_selected);
            viewHolder.itemFilter.setTextColor(Color.WHITE);
        } else {
            viewHolder.itemFilter.setBackgroundResource(R.drawable.item_filter_unselected);
            viewHolder.itemFilter.setTextColor(mContext.getResources().getColor(R.color.app_text_color_secondary_light));
        }
    }

    @Override
    public int getItemCount() {
        return mFilterList == null ? 0 : mFilterList.size();
    }


    public class LeaderboardFilterItem extends RecyclerView.ViewHolder {
        @BindView(R.id.filter_item)
        TextView itemFilter;

        @OnClick(R.id.filter_item)
        public void onFilterItemClicked() {
            int position = getAdapterPosition();
            String filterName = mFilterList.get(position);
            setSelected(filterName);
            if (mListener != null) {
                mListener.onFilterItemClick(position, filterName);
            }
        }

        public LeaderboardFilterItem(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public interface OnFilterItemClickListener {
        void onFilterItemClick(int position, String filter);
    }
}
