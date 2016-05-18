package com.waylens.hachi.ui.community;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.waylens.hachi.R;
import com.waylens.hachi.utils.Utils;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/5/16.
 */
public class FeedContextMenu extends LinearLayout {
    private static final int CONTEXT_MENU_WIDTH = (int) Utils.dp2Px(240);

    private int feedItem = -1;

    private OnFeedContextMenuItemClickListener onItemClickListener;

    public FeedContextMenu(Context context) {
        super(context);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_context_menu, this, true);
        setBackgroundResource(R.drawable.bg_container_shadow);
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public void bindToItem(int feedItem) {
        this.feedItem = feedItem;
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.bind(this);
    }

    public void dismiss() {
        ((ViewGroup) getParent()).removeView(FeedContextMenu.this);
    }

    @OnClick(R.id.btnReport)
    public void onReportClick() {
        if (onItemClickListener != null) {
            onItemClickListener.onReportClick(feedItem);
        }
    }

//    @OnClick(R.id.btnSharePhoto)
//    public void onSharePhotoClick() {
//        if (onItemClickListener != null) {
//            onItemClickListener.onSharePhotoClick(feedItem);
//        }
//    }
//
//    @OnClick(R.id.btnCopyShareUrl)
//    public void onCopyShareUrlClick() {
//        if (onItemClickListener != null) {
//            onItemClickListener.onCopyShareUrlClick(feedItem);
//        }
//    }

    @OnClick(R.id.btnCancel)
    public void onCancelClick() {
        if (onItemClickListener != null) {
            onItemClickListener.onCancelClick(feedItem);
        }
    }

    public void setOnFeedMenuItemClickListener(OnFeedContextMenuItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnFeedContextMenuItemClickListener {
        void onReportClick(int feedItem);

        void onCancelClick(int feedItem);
    }
}
