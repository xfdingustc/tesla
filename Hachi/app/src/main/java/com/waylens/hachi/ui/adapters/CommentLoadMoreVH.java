package com.waylens.hachi.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Richard on 8/26/15.
 */
public class CommentLoadMoreVH extends RecyclerView.ViewHolder {

    @Bind(R.id.load_more)
    View loadMoreView;

    @Bind(R.id.load_more_progressbar)
    ProgressBar loadMoreProgressBar;

    public CommentLoadMoreVH(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
