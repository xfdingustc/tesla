package com.waylens.hachi.ui.community.comment;

import android.support.v7.widget.RecyclerView;

import com.waylens.hachi.ui.recyclerview.SlideInItemAnimator;

/**
 * Created by Xiaofei on 2016/10/17.
 */

public class CommentAnimator extends SlideInItemAnimator {

    private boolean animateMoves = false;

    public CommentAnimator() {
        super();
    }

    void setAnimateMoves(boolean animateMoves) {
        this.animateMoves = animateMoves;
    }

    @Override
    public boolean animateMove(
        RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
        if (!animateMoves) {
            dispatchMoveFinished(holder);
            return false;
        }
        return super.animateMove(holder, fromX, fromY, toX, toY);
    }
}
