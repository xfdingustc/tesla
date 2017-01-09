package com.waylens.hachi.ui.clips;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.glide_snipe_integration.SnipeGlideLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipPos;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.utils.SettingHelper;
import com.waylens.hachi.utils.StringUtils;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

/**
 * Created by Xiaofei on 2016/2/26.
 */
public class ClipSetGroupAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = ClipSetGroupAdapter.class.getSimpleName();
    private final int mLayoutRes;
    private List<ClipSet> mClipSetGroup;
    private final Context mContext;
    private final OnClipClickListener mClipClickListener;

    private final static int ITEM_TYPE_HEAD = 0;
    private final static int ITEM_TYPE_CLIPVIEW = 1;

    private VdbRequestQueue mVdbRequestQueue;
    private boolean mMultiSelectedMode = false;

    private ArrayList<Clip> mSelectedClipList = new ArrayList<>();


    public interface OnClipClickListener {
        void onClipClicked(Clip clip, View transitionView);

        void onClipLongClicked(Clip clip);

        void onSelectedClipListChanged(List<Clip> clipList);
    }

    private class ClipGridItem {
        int itemType;
        boolean isItemSelected = false;
        Object itemObject;
    }

    private List<ClipGridItem> mClipGridItemList = new ArrayList<>();


    public ClipSetGroupAdapter(Context context, @LayoutRes int layoutRes, VdbRequestQueue requestQueue, List<ClipSet> clipSetGroup, ClipSetGroupAdapter.OnClipClickListener listener) {
        this.mContext = context;
        this.mClipSetGroup = clipSetGroup;
        recalculateGridItemList();
        this.mClipClickListener = listener;
        this.mVdbRequestQueue = requestQueue;
        this.mLayoutRes = layoutRes;

    }

    public void toggleSelectAll(boolean selectAll) {
        clearSelectedClips(false);
        for (ClipGridItem item : mClipGridItemList) {
            item.isItemSelected = selectAll;
            if (selectAll == true && item.itemType == ITEM_TYPE_CLIPVIEW) {

                add2SelectedList((Clip) item.itemObject);
            }
        }

        notifyDataSetChanged();
    }


    public void setClipSetGroup(List<ClipSet> clipSetGroup) {
        mClipSetGroup = clipSetGroup;
        clearSelectedClips(false);
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
            headItem.itemObject = clipSet.getClip(0).getClipDate();
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
        } else {
            return onCreateClipGridViewHolder(parent);
        }


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
        View view = inflater.inflate(mLayoutRes, parent, false);
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
        ClipGridItem gridItem = mClipGridItemList.get(position);
        Long clipDate = (Long) mClipGridItemList.get(position).itemObject;

        if (mMultiSelectedMode == true) {
            viewHolder.mBtnSelect.setVisibility(View.VISIBLE);
        } else {
            viewHolder.mBtnSelect.setVisibility(View.GONE);
        }

        viewHolder.mClipSetDate.setText(getFormattedDate(clipDate));
        toggleHeaderItemSelectedView(viewHolder, gridItem.isItemSelected);
    }

    private void onBindClipGridViewHolder(RecyclerView.ViewHolder holder, int position) {
        ClipGridViewHolder viewHolder = (ClipGridViewHolder) holder;
        ClipGridItem gridItem = mClipGridItemList.get(position);
        Clip clip = (Clip) gridItem.itemObject;
        ClipPos clipPos = new ClipPos(clip);

        String clipDuration = DateUtils.formatElapsedTime(clip.getDurationMs() / 1000);
        viewHolder.tvDuration.setText(clipDuration);

        Glide.with(mContext)
            .using(new SnipeGlideLoader(mVdbRequestQueue))
            .load(clipPos)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .crossFade()
            .into(viewHolder.ivClipCover);

        if (clip.isRaceClip()) {
            viewHolder.vaVideoTag.setVisibility(View.VISIBLE);
            viewHolder.vaVideoTag.setDisplayedChild(0);

            if (!SettingHelper.isMetricUnit()) {
                if (clip.getRaceTime060() > 0) {
                    viewHolder.performanceIcon.setText("60");
                    viewHolder.performance.setText(StringUtils.getRaceTime(clip.getRaceTime060()));
                } else if (clip.getRaceTime030() > 0) {
                    viewHolder.performanceIcon.setText("30");
                    viewHolder.performance.setText(StringUtils.getRaceTime(clip.getRaceTime030()));
                } else {
                    viewHolder.performanceIcon.setVisibility(View.GONE);
                    viewHolder.performance.setVisibility(View.GONE);
                }
            } else {
                if (clip.getRaceTime100() > 0) {
                    viewHolder.performanceIcon.setText("100");
                    viewHolder.performance.setText(StringUtils.getRaceTime(clip.getRaceTime100()));
                } else if (clip.getRaceTime50() > 0) {
                    viewHolder.performanceIcon.setText("50");
                    viewHolder.performance.setText(StringUtils.getRaceTime(clip.getRaceTime50()));
                } else {
                    viewHolder.performanceIcon.setVisibility(View.GONE);
                    viewHolder.performance.setVisibility(View.GONE);
                }
            }

        } else if(clip.lapTimerData != null) {
            viewHolder.vaVideoTag.setVisibility(View.VISIBLE);
            viewHolder.vaVideoTag.setDisplayedChild(1);
        } else {
                viewHolder.vaVideoTag.setVisibility(View.INVISIBLE);
        }

        if (viewHolder.mBtnSelect != null) {
            if (mMultiSelectedMode == true) {
                viewHolder.mBtnSelect.setVisibility(View.VISIBLE);
            } else {
                viewHolder.mBtnSelect.setVisibility(View.INVISIBLE);
            }
            toggleItemSelectedView(viewHolder, gridItem.isItemSelected);
        }

    }

    public void setMultiSelectedMode(boolean multiSelectedMode) {
        this.mMultiSelectedMode = multiSelectedMode;
        if (mMultiSelectedMode == false) {
            for (ClipGridItem item : mClipGridItemList) {
                item.isItemSelected = false;
            }
            clearSelectedClips(false);

        }
        notifyDataSetChanged();
    }

    private void toggleHeaderItemSelectedView(ClipGroupHeaderViewHolder viewHolder, boolean isSelected) {
        if (viewHolder.mBtnSelect != null) {
            if (isSelected == false) {
                viewHolder.mBtnSelect.setImageResource(R.drawable.edit_unselect);
            } else {
                viewHolder.mBtnSelect.setImageResource(R.drawable.edit_select);
            }
        }
    }

    private void toggleItemSelectedView(ClipGridViewHolder viewHolder, boolean isSelected) {
        if (viewHolder.mBtnSelect != null) {
            if (isSelected == false) {
                viewHolder.mBtnSelect.setImageResource(R.drawable.edit_unselect);
                viewHolder.mSelectedMask.setVisibility(View.GONE);
            } else {
                viewHolder.mBtnSelect.setImageResource(R.drawable.edit_select);
                viewHolder.mSelectedMask.setVisibility(View.VISIBLE);
            }
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
        return mSelectedClipList;
    }

    private void add2SelectedList(Clip clip) {
        if (mSelectedClipList.indexOf(clip) == -1) {
            mSelectedClipList.add(clip);
            if (mClipClickListener != null) {
                mClipClickListener.onSelectedClipListChanged(mSelectedClipList);
            }
        }
    }

    private void removeFromSelectedClip(Clip clip) {
        if (mSelectedClipList.indexOf(clip) != -1) {
            mSelectedClipList.remove(clip);
            if (mClipClickListener != null) {
                mClipClickListener.onSelectedClipListChanged(mSelectedClipList);
            }
        }
    }

    private void clearSelectedClips(boolean notifyObserver) {
        mSelectedClipList.clear();
        if (mClipClickListener != null && notifyObserver) {
            mClipClickListener.onSelectedClipListChanged(mSelectedClipList);
        }
    }

    private void toggleClipSelected(ClipGridItem gridItem, boolean isSelected) {
        gridItem.isItemSelected = isSelected;
        if (isSelected) {
            add2SelectedList((Clip)gridItem.itemObject);
        } else {
            removeFromSelectedClip((Clip)gridItem.itemObject);
        }
    }


    private String getFormattedDate(long date) {
        SimpleDateFormat format = new SimpleDateFormat("MMM dd,yyyy");

        long clipDate = date;
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


    public class ClipGroupHeaderViewHolder extends RecyclerView.ViewHolder {

        @Nullable
        @BindView(R.id.btnSelect)
        ImageButton mBtnSelect;

        @BindView(R.id.clipSetDate)
        TextView mClipSetDate;

        @OnClick(R.id.btnSelect)
        public void onBtnSelectClicked() {
            ClipGroupHeaderViewHolder viewHolder = ClipGroupHeaderViewHolder.this;
            ClipGridItem clipGridItem = mClipGridItemList.get(viewHolder.getPosition());

            clipGridItem.isItemSelected = !clipGridItem.isItemSelected;
            for (int i = viewHolder.getPosition() + 1; i < mClipGridItemList.size(); i++ ) {
                ClipGridItem oneClipGridItem = mClipGridItemList.get(i);
                if (oneClipGridItem.itemType == ITEM_TYPE_HEAD) {
                    break;
                }

                toggleClipSelected(oneClipGridItem, clipGridItem.isItemSelected);


            }
            notifyDataSetChanged();
        }

        public ClipGroupHeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public class ClipGridViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.ivClipCover)
        ImageView ivClipCover;

        @BindView(R.id.tvDuration)
        TextView tvDuration;

        @Nullable
        @BindView(R.id.btnSelect)
        ImageButton mBtnSelect;

        @Nullable
        @BindView(R.id.selectedMask)
        View mSelectedMask;

        @BindView(R.id.performance_icon)
        TextView performanceIcon;

        @BindView(R.id.tag_view_animator)
        ViewAnimator vaVideoTag;

        @BindView(R.id.performance)
        TextView performance;


        @OnClick({R.id.btnSelect, R.id.ivClipCover})
        public void onIvClipCoverClicked() {
            if (mClipClickListener == null) {
                return;
            }

            ClipGridViewHolder holder = ClipGridViewHolder.this;
            ClipGridItem clipGridItem = mClipGridItemList.get(holder.getPosition());

            if (!mMultiSelectedMode) {
                mClipClickListener.onClipClicked((Clip) clipGridItem.itemObject, ivClipCover);
            } else {
                clipGridItem.isItemSelected = !clipGridItem.isItemSelected;
                toggleItemSelectedView(ClipGridViewHolder.this, clipGridItem.isItemSelected);


                toggleClipSelected(clipGridItem, clipGridItem.isItemSelected);

                mClipClickListener.onClipClicked(null, null);
            }

        }

        @OnLongClick({R.id.btnSelect, R.id.ivClipCover})
        public boolean onIvClipCoverLongClicked() {
            if (mClipClickListener == null) {
                return true;
            }

            ClipGridViewHolder holder = ClipGridViewHolder.this;
            ClipGridItem clipGridItem = mClipGridItemList.get(holder.getPosition());
            if (holder.mBtnSelect == null) {
                return true;
            }
            //holder.mBtnSelect.setImageResource(R.drawable.edit_select);
            toggleClipSelected(clipGridItem, true);
            holder.mBtnSelect.setImageResource(R.drawable.edit_select);
            mSelectedMask.setVisibility(View.VISIBLE);

            mClipClickListener.onClipLongClicked((Clip) clipGridItem.itemObject);
            return true;
        }

        public ClipGridViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
