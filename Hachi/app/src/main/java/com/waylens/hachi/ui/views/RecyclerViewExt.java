package com.waylens.hachi.ui.views;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.ui.community.event.ScrollEvent;
import com.waylens.hachi.utils.rxjava.RxBus;


/**
 * Created by Richard on 9/2/15.
 */
public class RecyclerViewExt extends RecyclerView {
    private static final String TAG = RecyclerViewExt.class.getSimpleName();

    private OnScrollListener mOnScrollListener = new InternalOnScrollListener();

    private OnLoadMoreListener mOnLoadMoreListener;

    private OnScrollListener mOnFabScrollListener = new FabScrollListener();

    private int mPreviousVisibleItem;

    private boolean isLoadingMore;

    private boolean isLoadMoreEnabled;

    public RecyclerViewExt(Context context) {
        super(context);
        init();
    }

    public RecyclerViewExt(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RecyclerViewExt(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        addOnScrollListener(mOnFabScrollListener);
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
            if (isEnd && mOnLoadMoreListener != null && !isLoadingMore) {
                Logger.t(TAG).d("Loading more");
                isLoadingMore = true;
                mOnLoadMoreListener.loadMore();
            }

            if (firstVisibleItemPosition > mPreviousVisibleItem) {

                RxBus.getDefault().post(new ScrollEvent(true));
            } else if (firstVisibleItemPosition < mPreviousVisibleItem) {
                RxBus.getDefault().post(new ScrollEvent(false));
            }
            mPreviousVisibleItem = firstVisibleItemPosition;
        }
    }


    class FabScrollListener extends OnScrollListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

            if (firstVisibleItemPosition > mPreviousVisibleItem) {
                RxBus.getDefault().post(new ScrollEvent(true));
            } else if (firstVisibleItemPosition < mPreviousVisibleItem) {
                RxBus.getDefault().post(new ScrollEvent(false));
            }
            mPreviousVisibleItem = firstVisibleItemPosition;
        }
    }

}
