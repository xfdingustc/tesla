package com.waylens.hachi.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Richard on 8/21/15.
 */
public class MomentViewHolder extends RecyclerView.ViewHolder {

    @Bind(R.id.user_avatar)
    CircleImageView userAvatar;

    @Bind(R.id.user_name)
    TextView userName;

    @Bind(R.id.user_car)
    TextView userCar;

    @Bind(R.id.like_container)
    View likeContainer;

    @Bind(R.id.like_count)
    TextView likeCount;

    @Bind(R.id.comment_animator)
    ViewAnimator commentAnimator;

    @Bind(R.id.comment_list)
    LinearLayout commentContainer;

    @Bind(R.id.video_time)
    TextView videoTime;

    @Bind(R.id.video_duration)
    TextView videoDuration;

    @Bind(R.id.video_cover)
    ImageView videoCover;

    @Bind(R.id.video_control)
    View videoControl;

    @Bind(R.id.btn_like)
    ImageView btnLike;

    @Bind(R.id.btn_comment)
    ImageView btnComment;

    public MomentViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
