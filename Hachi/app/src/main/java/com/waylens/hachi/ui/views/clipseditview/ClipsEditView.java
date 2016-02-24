package com.waylens.hachi.ui.views.clipseditview;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.waylens.hachi.R;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.ui.entities.SharableClip;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;

import java.util.Collections;
import java.util.List;

/**
 * Created by Richard on 2/23/16.
 */
public class ClipsEditView extends RecyclerView {

    RecyclerViewAdapter mAdapter;
    ItemTouchHelper mItemTouchHelper;

    public ClipsEditView(Context context) {
        this(context, null, 0);
    }

    public ClipsEditView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClipsEditView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    void init() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(HORIZONTAL);
        setLayoutManager(layoutManager);
        mAdapter = new RecyclerViewAdapter();
        setAdapter(mAdapter);
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(this);
    }

    public void setSharableClips(List<SharableClip> sharableClips) {
        if (mAdapter != null) {
            mAdapter.setSharableClips(sharableClips);
        }
    }

    static class RecyclerViewAdapter extends RecyclerView.Adapter<VH> implements ItemTouchListener {

        List<SharableClip> mSharableClips;
        VdbImageLoader mImageLoader;
        float HALF_ALPHA = 0.5f;
        float FULL_ALPHA = 1.0f;

        VH selectedVH;
        int selectedPosition = -1;

        RecyclerViewAdapter() {
            mImageLoader = VdbImageLoader.getImageLoader(Snipe.newRequestQueue());
        }

        public void setSharableClips(List<SharableClip> sharableClips) {
            mSharableClips = sharableClips;
            if (mSharableClips != null) {
                notifyDataSetChanged();
            }
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.items_clips_edit, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(final VH holder, final int position) {
            Clip clip = mSharableClips.get(position).clip;
            ClipPos clipPos = new ClipPos(clip);
            mImageLoader.displayVdbImage(clipPos, holder.clipThumbnail);
            holder.itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e("test", "selectedPosition: " + selectedPosition);
                    if (selectedVH == null) {
                        Log.e("test", "selectedVH is null");
                    } else {
                        Log.e("test", "selectedVH.getAdapterPosition: " + selectedVH.getAdapterPosition());
                    }
                    Log.e("test", "position: " + position);
                    Log.e("test", "holder.getAdapterPosition: " + holder.getAdapterPosition());

                    if (selectedPosition == position) {
                        holder.itemView.setAlpha(HALF_ALPHA);
                        selectedVH = null;
                        selectedPosition = -1;
                        return;
                    }

                    if (selectedVH != null) {
                        selectedVH.itemView.setAlpha(HALF_ALPHA);
                    }
                    holder.itemView.setAlpha(FULL_ALPHA);
                    selectedVH = holder;
                    selectedPosition = position;
                }
            });
        }

        @Override
        public int getItemCount() {
            if (mSharableClips == null) {
                return 0;
            } else {
                return mSharableClips.size();
            }
        }

        @Override
        public void onViewAttachedToWindow(VH holder) {
            super.onViewAttachedToWindow(holder);
            if (holder.getAdapterPosition() == selectedPosition) {
                holder.itemView.setAlpha(FULL_ALPHA);
            } else {
                holder.itemView.setAlpha(HALF_ALPHA);
            }
        }

        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
            Log.e("test", "fromPosition: " + fromPosition);
            Log.e("test", "toPosition: " + toPosition);
            Collections.swap(mSharableClips, fromPosition, toPosition);
            if (fromPosition == selectedPosition) {
                selectedPosition = toPosition;
            }
            notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        @Override
        public void onItemDismiss(int position) {
            mSharableClips.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class VH extends RecyclerView.ViewHolder implements ItemViewHolderListener {
        ImageView clipThumbnail;

        float defaultElevation = 6.0f;

        public VH(View itemView) {
            super(itemView);
            clipThumbnail = (ImageView) itemView.findViewById(R.id.clip_thumbnail);
            defaultElevation = ((CardView) itemView).getCardElevation();
        }

        @Override
        public void onItemSelected() {
            ((CardView) itemView).setCardElevation(12.0f);
        }

        @Override
        public void onItemClear() {
            ((CardView) itemView).setCardElevation(defaultElevation);
        }
    }
}
