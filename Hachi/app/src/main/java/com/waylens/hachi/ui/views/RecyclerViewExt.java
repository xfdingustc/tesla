package com.waylens.hachi.ui.views;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Created by Richard on 9/2/15.
 */
public class RecyclerViewExt extends RecyclerView {

    private OnScrollListener mOnScrollListener = new InternalOnScrollListener();

    private OnLoadMoreListener mOnLoadMoreListener;

    private boolean isLoadingMore;

    private boolean isLoadMoreEnabled;

    public RecyclerViewExt(Context context) {
        super(context);
    }

    public RecyclerViewExt(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RecyclerViewExt(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public interface OnLoadMoreListener {
        void loadMore();
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        mOnLoadMoreListener = listener;
        if (listener != null) {
            addOnScrollListener(mOnScrollListener);
            isLoadMoreEnabled = true;
        } else {
            removeOnScrollListener(mOnScrollListener);
            isLoadMoreEnabled = false;
        }
    }

    public void setIsLoadingMore(boolean isLoading) {
        isLoadingMore = isLoading;
    }

    public void setEnableLoadMore(boolean enable) {
        if (isLoadMoreEnabled == enable) {
            return;
        }

        isLoadMoreEnabled = enable;
        if (enable) {
            addOnScrollListener(mOnScrollListener);
        } else {
            removeOnScrollListener(mOnScrollListener);
        }
    }

    class InternalOnScrollListener extends OnScrollListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
            int visibleItemCount = layoutManager.getChildCount();
            int totalItemCount = layoutManager.getItemCount();
            boolean isEnd = totalItemCount - visibleItemCount <= firstVisibleItemPosition;
            if (isEnd
                    && mOnLoadMoreListener != null
                    && !isLoadingMore) {
                Log.e("test", "Loading more");
                isLoadingMore = true;
                mOnLoadMoreListener.loadMore();
            }
        }
    }

}