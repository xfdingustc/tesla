package com.waylens.hachi.ui.community.feed;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
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

    public static class FeedContextMenuManager extends RecyclerView.OnScrollListener implements OnAttachStateChangeListener {

        private static FeedContextMenuManager instance;

        private FeedContextMenu contextMenuView;

        private boolean isContextMenuDismissing;
        private boolean isContextMenuShowing;

        public static FeedContextMenuManager getInstance() {
            if (instance == null) {
                instance = new FeedContextMenuManager();
            }
            return instance;
        }

        private FeedContextMenuManager() {

        }

        public void toggleContextMenuFromView(View openingView, int feedItem, OnFeedContextMenuItemClickListener listener) {
            if (contextMenuView == null) {
                showContextMenuFromView(openingView, feedItem, listener);
            } else {
                hideContextMenu();
            }
        }

        private void showContextMenuFromView(final View openingView, int feedItem, OnFeedContextMenuItemClickListener listener) {
            if (!isContextMenuShowing) {
                isContextMenuShowing = true;
                contextMenuView = new FeedContextMenu(openingView.getContext());
                contextMenuView.bindToItem(feedItem);
                contextMenuView.addOnAttachStateChangeListener(this);
                contextMenuView.setOnFeedMenuItemClickListener(listener);

                ((ViewGroup) openingView.getRootView().findViewById(android.R.id.content)).addView(contextMenuView);

                contextMenuView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        contextMenuView.getViewTreeObserver().removeOnPreDrawListener(this);
                        setupContextMenuInitialPosition(openingView);
                        performShowAnimation();
                        return false;
                    }
                });
            }
        }

        private void setupContextMenuInitialPosition(View openingView) {
            final int[] openingViewLocation = new int[2];
            openingView.getLocationOnScreen(openingViewLocation);
            int additionalBottomMargin = (int) Utils.dp2Px(16);
            int menuLeft = openingViewLocation[0] - contextMenuView.getWidth();
            int menuTop = openingViewLocation[1] - contextMenuView.getHeight();
            if (menuTop < (int)Utils.dp2Px(16)) {
                menuTop  = openingViewLocation[1] + openingView.getHeight();
            }
            contextMenuView.setTranslationX(menuLeft);
            contextMenuView.setTranslationY(menuTop);
        }

        private void performShowAnimation() {
            contextMenuView.setPivotX(contextMenuView.getWidth());
            contextMenuView.setPivotY(contextMenuView.getHeight());
            contextMenuView.setScaleX(0.1f);
            contextMenuView.setScaleY(0.1f);
            contextMenuView.animate()
                .scaleX(1f).scaleY(1f)
                .setDuration(150)
                .setInterpolator(new OvershootInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isContextMenuShowing = false;
                    }
                });
        }

        public void hideContextMenu() {
            if (!isContextMenuDismissing) {
                isContextMenuDismissing = true;
                performDismissAnimation();
            }
        }

        private void performDismissAnimation() {
            contextMenuView.setPivotX(contextMenuView.getWidth());
            contextMenuView.setPivotY(contextMenuView.getHeight());
            contextMenuView.animate()
                .scaleX(0.1f).scaleY(0.1f)
                .setDuration(150)
                .setInterpolator(new AccelerateInterpolator())
                .setStartDelay(100)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (contextMenuView != null) {
                            contextMenuView.dismiss();
                        }
                        isContextMenuDismissing = false;
                    }
                });
        }

        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (contextMenuView != null) {
                hideContextMenu();
                contextMenuView.setTranslationY(contextMenuView.getTranslationY() - dy);
            }
        }

        @Override
        public void onViewAttachedToWindow(View v) {

        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            contextMenuView = null;
        }
    }
}
