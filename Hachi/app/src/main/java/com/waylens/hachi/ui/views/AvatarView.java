package com.waylens.hachi.ui.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ViewAnimator;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.pavlospt.roundedletterview.RoundedLetterView;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.rest.bean.User;
import com.waylens.hachi.utils.AvatarHelper;
import com.waylens.hachi.utils.CircleTransform;
import com.waylens.hachi.utils.VersionHelper;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/10/16.
 */

public class AvatarView extends FrameLayout {
    private CircleTransform mCircleTransform;

   @BindView(R.id.user_avatar_iv)
    ImageView userAvatar;



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
        if (isInEditMode()) {
            return;
        }
        mCircleTransform = new CircleTransform(getContext());
    }

    public void loadAvatar(User user) {
        loadAvatar(user.avatarUrl, user.userName);
    }

    public void loadAvatar(String avatarUrl, String userName) {
        if (!TextUtils.isEmpty(avatarUrl) && !avatarUrl.equals(Constants.DEFAULT_AVATAR)) {
            Drawable placeHolderDrawable;
            if (VersionHelper.isGreaterThanLollipop()) {
                placeHolderDrawable = getContext().getResources().getDrawable(R.drawable.ic_account_circle_placeholder, null);
            } else {
                placeHolderDrawable = getContext().getResources().getDrawable(R.drawable.ic_account_circle_placeholder);
            }
            Glide.with(getContext())
                .load(avatarUrl)
                .transform(mCircleTransform)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(placeHolderDrawable)
                .crossFade()
                .into(userAvatar);
        } else if (!TextUtils.isEmpty(userName)){
            TextDrawable drawable = TextDrawable.builder()
                .beginConfig()
                .bold()
                .endConfig()
                .buildRound(userName.substring(0, 1).toUpperCase(), getContext().getResources().getColor(AvatarHelper.getAvatarBackgroundColor(userName)));


            userAvatar.setImageDrawable(drawable);

        } else {

            setImageResource(R.drawable.ic_account_circle);
        }
    }

    public void setImageResource(int resource) {
        userAvatar.setImageResource(resource);
    }

  
}
