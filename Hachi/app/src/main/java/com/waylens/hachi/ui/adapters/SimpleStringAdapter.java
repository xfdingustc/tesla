package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.waylens.hachi.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/5/18.
 */
public class SimpleStringAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private final List<String> mStringList;
    private final OnListItemClickListener mOnListItemClickListener;

    public SimpleStringAdapter(List<String> list, OnListItemClickListener listener ) {
        mStringList = list;
        mOnListItemClickListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_simple_string, parent, false);
        return new SimpleStringViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        SimpleStringViewHolder viewHolder = (SimpleStringViewHolder)holder;
        viewHolder.title.setText(mStringList.get(position));
        viewHolder.rootLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnListItemClickListener != null) {
                    mOnListItemClickListener.onItemClicked(position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mStringList == null ? 0 : mStringList.size();
    }


    public class SimpleStringViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.root_layout)
        LinearLayout rootLayout;

        @BindView(R.id.title)
        TextView title;

        public SimpleStringViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public interface OnListItemClickListener {
        void onItemClicked(int position);
    }
}
