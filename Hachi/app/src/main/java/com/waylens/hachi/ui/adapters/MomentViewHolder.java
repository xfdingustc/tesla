package com.waylens.hachi.ui.adapters;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;

import butterknife.Bind;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Richard on 8/21/15.
 */
public class MomentViewHolder extends ClipViewHolder {

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

    public MomentViewHolder(View itemView) {
        super(itemView);
    }
}
