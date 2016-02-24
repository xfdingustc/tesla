package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.LayoutManager.WrapGridLayoutManager;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/2/18.
 */
public class ClipSetGroupAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context mContext;
    private final OnClipClickListener mClipClickListener;
    private List<ClipSet> mClipSetGroup;

    public interface OnClipClickListener {
        void onClipClicked(Clip clip);
    }

    public ClipSetGroupAdapter(Context context, List<ClipSet> clipSetGroup, ClipSetGroupAdapter
        .OnClipClickListener listener) {
        mContext = context;
        mClipSetGroup = clipSetGroup;
        this.mClipClickListener = listener;
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


        viewHolder.mClipSetDate.setText(getFormattedDate(clipSet.getClip(0).clipDate));

        viewHolder.mRvClipGrid.setLayoutManager(new WrapGridLayoutManager(mContext, 4));
        ClipSetGridAdapter adapter = new ClipSetGridAdapter(mContext, clipSet, new ClipSetGridAdapter.OnClipClickListener() {
            @Override
            public void onClipClicked(Clip clip) {
                if (mClipClickListener != null) {
                    mClipClickListener.onClipClicked(clip);
                }
            }
        });
        viewHolder.mRvClipGrid.setAdapter(adapter);
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

    private String getFormattedDate(int date) {
        SimpleDateFormat format = new SimpleDateFormat("MMM dd,yyyy");

        long clipDate = (long)date * 1000;
        long currentTime = System.currentTimeMillis();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(clipDate);
        int clipDateDay = calendar.get(Calendar.DAY_OF_YEAR);
        int clipDateYear = calendar.get(Calendar.YEAR);

        calendar.setTimeInMillis(currentTime);
        int currentDateDay = calendar.get(Calendar.DAY_OF_YEAR);
        int currentDateYear = calendar.get(Calendar.YEAR);


        String dateString = format.format(clipDate);

        if (clipDateYear == currentDateYear) {
            if ((currentDateDay - clipDateDay) < 1) {
                dateString = mContext.getString(R.string.today);
            } else if ((currentDateDay - clipDateDay) < 2) {
                dateString = mContext.getString(R.string.yesterday);
            }
        }
        return dateString;
    }

    public static class ClipSetGroupViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.clipSetDate)
        TextView mClipSetDate;

        @Bind(R.id.rvClipGrid)
        RecyclerView mRvClipGrid;

        public ClipSetGroupViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
