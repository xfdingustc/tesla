package com.waylens.hachi.ui.LayoutManager;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Xiaofei on 2015/7/14.
 */
public class WrapGridLayoutManager extends GridLayoutManager {

  private final int mSpanCount;
  private int[] mMeasuredDimension = {0, 0};
  private static final String TAG = WrapGridLayoutManager.class.getSimpleName();

  public WrapGridLayoutManager(Context context, int spanCount) {
    super(context, spanCount);
    this.mSpanCount = spanCount;
  }

  @Override
  public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state, int widthSpec, int heightSpec) {
    if (getItemCount() == 0) {
      setMeasuredDimension(0, 0);
      return;
    }
    final int widthMode = View.MeasureSpec.getMode(widthSpec);
    final int widthSize = View.MeasureSpec.getSize(widthSpec);
    int width = widthSize;
    int height = 0;
    int row = getItemCount() / mSpanCount + 1;

    int childWidthSpec = View.MeasureSpec.makeMeasureSpec(widthSize / mSpanCount,
        widthMode);

    measureScrapChild(recycler, 0, childWidthSpec, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        mMeasuredDimension);

    // TODO: here is a ugly solution, we need refine this.
    height = mMeasuredDimension[1] * row;
    setMeasuredDimension(width, height);
  }

  private void measureScrapChild(RecyclerView.Recycler recycler, int position, int widthSpec, int heightSpec, int[] measuredDimension) {
    View view = recycler.getViewForPosition(position);
    if (view != null) {
      RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) view.getLayoutParams();
      int childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec,
          getPaddingTop() + getPaddingBottom(), p.height);
      view.measure(widthSpec, childHeightSpec);
      measuredDimension[0] = view.getMeasuredWidth() + p.leftMargin + p.rightMargin;
      measuredDimension[1] = view.getMeasuredHeight() + p.bottomMargin + p.topMargin;
      recycler.recycleView(view);
    }
  }
}
