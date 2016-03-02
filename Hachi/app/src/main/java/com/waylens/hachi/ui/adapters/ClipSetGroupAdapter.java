package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/2/26.
 */
public class ClipSetGroupAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<ClipSet> mClipSetGroup;
    private final Context mContext;
    private final OnClipClickListener mClipClickListener;

    private final static int ITEM_TYPE_HEAD = 0;
    private final static int ITEM_TYPE_CLIPVIEW = 1;

    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mVdbImageLoader;
    private boolean mMultiSelectedMode = false;

    public interface OnClipClickListener {
        void onClipClicked(Clip clip);

        void onClipLongClicked(Clip clip);
    }

    private class ClipGridItem {
        int itemType;
        boolean isItemSelected = false;
        Object itemObject;
    }

    private List<ClipGridItem> mClipGridItemList = new ArrayList<>();


    public ClipSetGroupAdapter(Context context, List<ClipSet> clipSetGroup, ClipSetGroupAdapter
            .OnClipClickListener listener) {
        this.mContext = context;
        this.mClipSetGroup = clipSetGroup;
        recalculateGridItemList();
        this.mClipClickListener = listener;
        this.mVdbRequestQueue = Snipe.newRequestQueue(mContext);
        this.mVdbImageLoader = VdbImageLoader.getImageLoader(mVdbRequestQueue);
    }


    public void setClipSetGroup(List<ClipSet> clipSetGroup) {
        mClipSetGroup = clipSetGroup;
        recalculateGridItemList();
        notifyDataSetChanged();
    }

    private void recalculateGridItemList() {
        if (mClipSetGroup == null) {
            return;
        }

        mClipGridItemList.clear();
        for (ClipSet clipSet : mClipSetGroup) {
            ClipGridItem headItem = new ClipGridItem();
            headItem.itemType = ITEM_TYPE_HEAD;
            headItem.itemObject = clipSet.getClip(0).clipDate;
            mClipGridItemList.add(headItem);

            for (Clip clip : clipSet.getClipList()) {
                ClipGridItem clipItem = new ClipGridItem();
                clipItem.itemType = ITEM_TYPE_CLIPVIEW;
                clipItem.itemObject = clip;
                mClipGridItemList.add(clipItem);
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_TYPE_HEAD) {
            return onCreateHeaderViewHolder(parent);
        } else if (viewType == ITEM_TYPE_CLIPVIEW) {
            return onCreateClipGridViewHolder(parent);
        }

        return null;


    }

    private RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_clip_set_header, parent, false);
        StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
        layoutParams.setFullSpan(true);
        view.setLayoutParams(layoutParams);
        return new ClipGroupHeaderViewHolder(view);
    }

    private RecyclerView.ViewHolder onCreateClipGridViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_clip_set_grid, parent, false);
        StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
        layoutParams.setFullSpan(false);
        view.setLayoutParams(layoutParams);
        return new ClipGridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        if (ITEM_TYPE_HEAD == viewType) {
            onBindClipSetHeaderViewHolder(holder, position);
        } else if (ITEM_TYPE_CLIPVIEW == viewType) {
            onBindClipGridViewHolder(holder, position);
        }
    }


    private void onBindClipSetHeaderViewHolder(RecyclerView.ViewHolder holder, int position) {
        ClipGroupHeaderViewHolder viewHolder = (ClipGroupHeaderViewHolder) holder;
        Integer clipDate = (Integer) mClipGridItemList.get(position).itemObject;

        viewHolder.mClipSetDate.setText(getFormattedDate(clipDate));
    }

    private void onBindClipGridViewHolder(RecyclerView.ViewHolder holder, int position) {
        ClipGridViewHolder viewHolder = (ClipGridViewHolder) holder;
        ClipGridItem gridItem = mClipGridItemList.get(position);
        Clip clip = (Clip) gridItem.itemObject;
        ClipPos clipPos = new ClipPos(clip);

        String clipDuration = DateUtils.formatElapsedTime(clip.getDurationMs() / 1000);
        viewHolder.tvDuration.setText(clipDuration);

        mVdbImageLoader.displayVdbImage(clipPos, viewHolder.ivClipCover);

        if (mMultiSelectedMode == true) {
            viewHolder.mBtnSelect.setVisibility(View.VISIBLE);
        } else {
            viewHolder.mBtnSelect.setVisibility(View.INVISIBLE);
        }
        toggleItemSelectedView(viewHolder, gridItem.isItemSelected);

    }

    public void setMultiSelectedMode(boolean multiSelectedMode) {
        this.mMultiSelectedMode = multiSelectedMode;
        if (mMultiSelectedMode == false) {
            for (ClipGridItem item : mClipGridItemList) {
                item.isItemSelected = false;
            }
        }
        notifyDataSetChanged();
    }

    private void toggleItemSelectedView(ClipGridViewHolder viewHolder, boolean isSelected) {
        if (isSelected == false) {
            viewHolder.mBtnSelect.setImageResource(R.drawable.edit_unselect);
            viewHolder.mSelectedMask.setVisibility(View.GONE);
        } else {
            viewHolder.mBtnSelect.setImageResource(R.drawable.edit_select);
            viewHolder.mSelectedMask.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return mClipGridItemList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mClipGridItemList.get(position).itemType;
    }

    public ArrayList<Clip> getSelectedClipList() {
        ArrayList<Clip> mSelectedClipList = new ArrayList<>();
        for (ClipGridItem gridItem : mClipGridItemList) {
            if (gridItem.itemType == ITEM_TYPE_CLIPVIEW && gridItem.isItemSelected == true) {
                mSelectedClipList.add((Clip) gridItem.itemObject);
            }
        }
        return mSelectedClipList;
    }


    private String getFormattedDate(int date) {
        SimpleDateFormat format = new SimpleDateFormat("MMM dd,yyyy");

        long clipDate = (long) date * 1000;
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


    public static class ClipGroupHeaderViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.clipSetDate)
        TextView mClipSetDate;

        public ClipGroupHeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public class ClipGridViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.ivClipCover)
        ImageView ivClipCover;

        @Bind(R.id.tvDuration)
        TextView tvDuration;

        @Bind(R.id.btnSelect)
        ImageButton mBtnSelect;

        @Bind(R.id.selectedMask)
        View mSelectedMask;

        public ClipGridViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            ivClipCover.setTag(this);
            ivClipCover.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mClipClickListener == null) {
                        return;
                    }

                    ClipGridViewHolder holder = (ClipGridViewHolder) v.getTag();
                    ClipGridItem clipGridItem = mClipGridItemList.get(holder.getPosition());

                    if (!mMultiSelectedMode) {
                        mClipClickListener.onClipClicked((Clip) clipGridItem.itemObject);
                    } else {
                        clipGridItem.isItemSelected = !clipGridItem.isItemSelected;
                        toggleItemSelectedView(ClipGridViewHolder.this, clipGridItem
                                .isItemSelected);
                    }

                }
            });

            ivClipCover.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mClipClickListener == null) {
                        return true;
                    }

                    ClipGridViewHolder holder = (ClipGridViewHolder) v.getTag();
                    ClipGridItem clipGridItem = mClipGridItemList.get(holder.getPosition());
                    //holder.mBtnSelect.setImageResource(R.drawable.edit_select);
                    clipGridItem.isItemSelected = true;
                    holder.mBtnSelect.setImageResource(R.drawable.edit_select);
                    mSelectedMask.setVisibility(View.VISIBLE);
                    mClipClickListener.onClipLongClicked((Clip) clipGridItem.itemObject);

                    return true;
                }
            });
        }
    }
}
