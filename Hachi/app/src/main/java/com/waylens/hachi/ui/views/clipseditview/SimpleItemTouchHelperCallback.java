/*
 * Copyright (C) 2015 Paul Burke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.waylens.hachi.ui.views.clipseditview;

import android.graphics.Canvas;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

/**
 * An implementation of {@link ItemTouchHelper.Callback} that enables basic drag & drop and
 * swipe-to-dismiss. Drag events are automatically started by an item long-press.<br/>
 * </br/>
 * Expects the <code>RecyclerView.Adapter</code> to listen for {@link
 * ItemTouchListener} callbacks and the <code>RecyclerView.ViewHolder</code> to implement
 * {@link ItemViewHolderListener}.
 *
 * @author Paul Burke (ipaulpro)
 */
public class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {

    public static final float ALPHA_FULL = 1.0f;

    public static final int POSITION_UNKNOWN = -1;

    private final ItemTouchListener mAdapter;

    private int mFromPosition = POSITION_UNKNOWN;
    private int mToPosition = POSITION_UNKNOWN;

    public SimpleItemTouchHelperCallback(ItemTouchListener adapter) {
        mAdapter = adapter;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        // Set movement flags based on the layout manager
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (!(layoutManager instanceof LinearLayoutManager)) {
            return makeMovementFlags(0, 0);
        }
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
        int dragFlags;
        int swipeFlags;
        if (linearLayoutManager.getOrientation() == LinearLayoutManager.HORIZONTAL) {
            dragFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            swipeFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        } else {
            dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        }

        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
        if (source.getItemViewType() != target.getItemViewType()) {
            return false;
        }
        if (mFromPosition == POSITION_UNKNOWN) {
            mFromPosition = source.getAdapterPosition();
        }
        mToPosition = target.getAdapterPosition();

        mAdapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
        // Notify the adapter of the dismissal
        mAdapter.onItemDismiss(viewHolder.getAdapterPosition());
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            // Fade out the view as it is swiped out of the parent's bounds
            //final float alpha = ALPHA_FULL - Math.abs(dY) / (float) viewHolder.itemView.getHeight();
            //viewHolder.itemView.setAlpha(alpha);
            viewHolder.itemView.setTranslationY(dY);
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        // We only want the active item to change
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder instanceof ItemViewHolderListener) {
                // Let the view holder know that this item is being moved or dragged
                ItemViewHolderListener itemViewHolder = (ItemViewHolderListener) viewHolder;
                itemViewHolder.onItemSelected();
            }
        }

        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        //viewHolder.itemView.setAlpha(ALPHA_FULL);

        if (viewHolder instanceof ItemViewHolderListener) {
            // Tell the view holder it's time to restore the idle state
            ItemViewHolderListener itemViewHolder = (ItemViewHolderListener) viewHolder;
            itemViewHolder.onItemClear();
        }

        if (mFromPosition != POSITION_UNKNOWN) {
            mAdapter.onItemMoved(mFromPosition, mToPosition);
            mFromPosition = POSITION_UNKNOWN;
            mToPosition = POSITION_UNKNOWN;
        }
    }
}
