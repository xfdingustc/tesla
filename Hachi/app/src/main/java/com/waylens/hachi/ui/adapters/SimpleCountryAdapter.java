package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.rest.bean.Country;
import com.waylens.hachi.ui.settings.CountryActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by lshw on 2016/7/27.
 */
public class SimpleCountryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    private final List<Country> mCountryList;
    private List<Country> mCountryListFiltered;
    private final OnListItemClickListener mOnListItemClickListener;
    private InnerFilter mFilter;

    public SimpleCountryAdapter(List<Country> list, OnListItemClickListener listener) {
        mCountryList = list;
        mCountryListFiltered = list;
        mOnListItemClickListener = listener;
    }

    public Country getCountry(int index) {
        if (index < mCountryListFiltered.size()) {
            return mCountryListFiltered.get(index);
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
        viewHolder.title.setText(mCountryListFiltered.get(position).name);
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
        return mCountryListFiltered == null ? 0 : mCountryListFiltered.size();
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
                results.values = mCountryList;
                results.count = mCountryList.size();
            } else {
                String prefixString = prefix.toString().toLowerCase();
                List<Country> newValues = new ArrayList<>();
                for (Country item : mCountryList) {
                    if (item.name.toLowerCase().startsWith(prefixString)) {
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
            mCountryListFiltered = (List<Country>) results.values;

            if (results.count > 0) {
                notifyDataSetChanged();
            }
        }

    }
}
