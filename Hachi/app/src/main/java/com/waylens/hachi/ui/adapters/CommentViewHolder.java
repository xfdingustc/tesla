package com.waylens.hachi.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Richard on 8/26/15.
 */
public class CommentViewHolder extends RecyclerView.ViewHolder {

    @Bind(R.id.user_avatar)
    CircleImageView avatarView;

    @Bind(R.id.comment_content)
    TextView commentContentViews;

    @Bind(R.id.comment_time)
    TextView commentTimeView;

    @Bind(R.id.status_container)
    ViewAnimator commentViewAnimator;


    public CommentViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
