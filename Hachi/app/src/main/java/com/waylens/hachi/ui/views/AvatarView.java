package com.waylens.hachi.ui.views;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ViewAnimator;

import com.bumptech.glide.Glide;
import com.github.pavlospt.roundedletterview.RoundedLetterView;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.rest.bean.User;
import com.waylens.hachi.utils.AvatarHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2016/10/16.
 */

public class AvatarView extends FrameLayout {

    @BindView(R.id.avatar_va)
    ViewAnimator vaAvatar;

    @BindView(R.id.user_avatar)
    CircleImageView userAvatar;

    @BindView(R.id.rlv_name_view)
    RoundedLetterView rlvNameView;

    public AvatarView(Context context) {
        this(context, null);
    }

    public AvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews(context);
    }

    private void initViews(Context context) {
        View.inflate(context, R.layout.layout_avatar_view, this);
        ButterKnife.bind(this);
    }

    public void loadAvatar(User user) {
        loadAvatar(user.avatarUrl, user.userName);
    }

    public void loadAvatar(String avatarUrl, String userName) {
        if (!TextUtils.isEmpty(avatarUrl) && !avatarUrl.equals(Constants.DEFAULT_AVATAR)) {
            Glide.with(getContext())
                .load(avatarUrl)
                .placeholder(R.drawable.ic_account_circle_placeholder)
                .crossFade()
                .dontAnimate()
                .into(userAvatar);
            vaAvatar.setDisplayedChild(0);
        } else {
            vaAvatar.setDisplayedChild(1);
            rlvNameView.setBackgroundColor(getContext().getResources().getColor(AvatarHelper.getRandomAvatarBackgroundColor()));
            rlvNameView.setTitleText(userName.substring(0, 1).toUpperCase());
        }
    }
}
