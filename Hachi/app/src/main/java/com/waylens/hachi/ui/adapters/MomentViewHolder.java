package com.waylens.hachi.ui.adapters;

import android.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;
import com.waylens.hachi.utils.ViewUtils;

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

    @Bind(R.id.moment_desc_view)
    TextView descView;

    @Bind(R.id.video_time)
    TextView videoTime;

    @Bind(R.id.video_duration)
    TextView videoDuration;

    @Bind(R.id.video_cover)
    ImageView videoCover;

    @Bind(R.id.video_control)
    public View videoControl;

    @Bind(R.id.btnLike)
    ImageButton btnLike;


    @Bind(R.id.btn_comment)
    View btnComment;

    @Bind(R.id.video_fragment_container)
    public FrameLayout fragmentContainer;

    public Fragment videoFragment;

    public MomentViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        fragmentContainer.setId(ViewUtils.generateViewId());
    }
}
