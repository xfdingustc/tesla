package com.waylens.hachi.ui.community.feed;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.utils.ViewUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;


public class MomentViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.user_avatar)
    CircleImageView userAvatar;

    @BindView(R.id.user_name)
    TextView userName;

    @BindView(R.id.user_car)
    TextView userCar;

    @BindView(R.id.title)
    TextView title;

    @BindView(R.id.video_time)
    TextView videoTime;

    @BindView(R.id.video_duration)
    TextView videoDuration;

    @BindView(R.id.video_cover)
    ImageView videoCover;


    @BindView(R.id.btn_more)
    ImageButton btnMore;

    @BindView(R.id.video_fragment_container)
    public FrameLayout fragmentContainer;

    public MomentViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        fragmentContainer.setId(ViewUtils.generateViewId());
    }
}
