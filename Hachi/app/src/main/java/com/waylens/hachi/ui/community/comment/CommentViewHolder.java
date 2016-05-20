package com.waylens.hachi.ui.community.comment;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;


import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Richard on 8/26/15.
 */
public class CommentViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.user_avatar)
    CircleImageView avatarView;

    @BindView(R.id.tvUserName)
    TextView tvUserName;

    @BindView(R.id.comment_content)
    TextView commentContentViews;

    @BindView(R.id.comment_time)
    TextView commentTimeView;

    @BindView(R.id.status_container)
    ViewAnimator commentViewAnimator;


    public CommentViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
