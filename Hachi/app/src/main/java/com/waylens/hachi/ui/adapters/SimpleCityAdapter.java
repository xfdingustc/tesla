package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.rest.bean.City;
import com.waylens.hachi.ui.settings.CityActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/5/18.
 */
public class SimpleCityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>implements Filterable{

    private final List<City> mCityList;
    private List<City> mCityListFiltered;
    private final OnListItemClickListener mOnListItemClickListener;
    private InnerFilter mFilter;

    public SimpleCityAdapter(List<City> list, OnListItemClickListener listener ) {
        mCityList = list;
        mCityListFiltered = list;
        mOnListItemClickListener = listener;
    }

    public City getCity(int index) {
        if (index < mCityListFiltered.size()) {
            return mCityListFiltered.get(index);
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
        SimpleStringViewHolder viewHolder = (SimpleStringViewHolder)holder;
        viewHolder.title.setText(mCityListFiltered.get(position).name);
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
        return mCityListFiltered == null ? 0 : mCityListFiltered.size();
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
                results.values = mCityList;
                results.count = mCityList.size();
            } else {
                String prefixString = prefix.toString().toLowerCase();
                List<City> newValues = new ArrayList<>();
                for (City item : mCityList) {
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
            mCityListFiltered = (List<City>) results.values;

            if (results.count > 0) {
                notifyDataSetChanged();
            }
        }

    }
}
