package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.l4digital.fastscroll.FastScroller;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by lshw on 16/7/27.
 */
public abstract class SimpleCommonAdapter<T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable, FastScroller.SectionIndexer {

    private final List<T> mList;
    private List<T> mListFiltered;
    private final OnListItemClickListener mOnListItemClickListener;
    private InnerFilter mFilter;

    public SimpleCommonAdapter(List<T> list, OnListItemClickListener listener) {
        mList = list;
        mListFiltered = list;
        mOnListItemClickListener = listener;
        Collections.sort(mList, new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                String leftName = getName(o1).toUpperCase();
                String rightName = getName(o2).toUpperCase();
                return leftName.compareTo(rightName);
            }
        });
    }

    public T getItem(int index) {
        if (index < mListFiltered.size()) {
            return mListFiltered.get(index);
        } else {
            return null;
        }
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
        SimpleStringViewHolder viewHolder = (SimpleStringViewHolder) holder;
        viewHolder.title.setText(getName(mListFiltered.get(position)));
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
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
        return mListFiltered == null ? 0 : mListFiltered.size();
    }


    @Override
    public String getSectionText(int position) {
        return String.valueOf(getName(mListFiltered.get(position)).charAt(0));
    }

    public class SimpleStringViewHolder extends RecyclerView.ViewHolder {


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

    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new InnerFilter();
        }
        return mFilter;
    }

    class InnerFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();
            if (prefix == null || prefix.length() == 0) {
                results.values = mList;
                results.count = mList.size();
            } else {
                String prefixString = prefix.toString().toLowerCase();
                List<T> newValues = new ArrayList<>();
                for (T item : mList) {
                    if (getName(item).toLowerCase().startsWith(prefixString)) {
                        newValues.add(item);
                    }
                }
                results.values = newValues;
                results.count = newValues.size();
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mListFiltered = (List<T>) results.values;

            if (results.count > 0) {
                notifyDataSetChanged();
            }
        }


    }

    public abstract String getName(T t);
}