package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.vdb.ClipSet;

import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/2/18.
 */
public class ClipSetGroupAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context mContext;
    private List<ClipSet> mClipSetGroup;

    public ClipSetGroupAdapter(Context context, List<ClipSet> clipSetGroup) {
        mContext = context;
        mClipSetGroup = clipSetGroup;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_clip_set_group, parent, false);

        return new ClipSetGroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ClipSet clipSet = mClipSetGroup.get(position);
        ClipSetGroupViewHolder viewHolder = (ClipSetGroupViewHolder)holder;

        viewHolder.mClipSetDate.setText(clipSet.getClip(0).getDateString());
    }

    @Override
    public int getItemCount() {
        if (mClipSetGroup == null) {
            return 0;
        } else {
            return mClipSetGroup.size();
        }
    }

    public void setClipSetGroup(List<ClipSet> clipSetGroup) {
        mClipSetGroup = clipSetGroup;
        notifyDataSetChanged();
    }


    public static class ClipSetGroupViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.clipSetDate)
        TextView mClipSetDate;

        public ClipSetGroupViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
