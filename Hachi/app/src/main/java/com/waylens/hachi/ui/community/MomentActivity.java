package com.waylens.hachi.ui.community;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.birbit.android.jobqueue.JobManager;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.social.LikeJob;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.views.OnViewDragListener;
import com.waylens.hachi.utils.ImageUtils;

import butterknife.BindView;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2016/5/19.
 */
public class MomentActivity extends BaseActivity {
    private static final String TAG = MomentActivity.class.getSimpleName();
    private Moment mMoment;


    private static final DecelerateInterpolator DECCELERATE_INTERPOLATOR = new DecelerateInterpolator();
    private static final AccelerateInterpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();
    private static final OvershootInterpolator OVERSHOOT_INTERPOLATOR = new OvershootInterpolator(4);

    public static void launch(Context activity, Moment moment) {
        Intent intent = new Intent(activity, MomentActivity.class);
        intent.putExtra("moment", moment);
        activity.startActivity(intent);
    }


    @BindView(R.id.momemt_title)
    TextView mMomentTitle;

    @BindView(R.id.btn_like)
    ImageButton mBtnLike;

    @BindView(R.id.like_count)
    TextSwitcher mTsLikeCount;

    @BindView(R.id.user_avatar)
    CircleImageView mUserAvatar;

    @OnClick(R.id.btn_like)
    public void onBtnLikeClicked() {
        boolean isCancel = mMoment.isLiked;
        JobManager jobManager = BgJobManager.getManager();
        LikeJob job = new LikeJob(mMoment, isCancel);
        jobManager.addJobInBackground(job);
        mMoment.isLiked = !mMoment.isLiked;
        if (mMoment.isLiked) {
            mMoment.likesCount++;
        } else {
            mMoment.likesCount--;
        }
        doUpdateLikeStateAnimator();
        updateLikeCount();
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        Intent intent = getIntent();
        mMoment = (Moment) intent.getSerializableExtra("moment");
        Logger.t(TAG).d("moment: " + mMoment.toString());
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_moment);
        mMomentTitle.setText(mMoment.title);
        updateLikeState();

        mTsLikeCount.setCurrentText(String.valueOf(mMoment.likesCount));

        mImageLoader.displayImage(mMoment.owner.avatarUrl, mUserAvatar, ImageUtils.getAvatarOptions());


        MomentPlayFragment fragment = MomentPlayFragment.newInstance(mMoment, new OnViewDragListener() {
            @Override
            public void onStartDragging() {

            }

            @Override
            public void onStopDragging() {

            }
        });

        getFragmentManager().beginTransaction().replace(R.id.moment_play_container, fragment).commit();
    }

    private void doUpdateLikeStateAnimator() {

        AnimatorSet animatorSet = new AnimatorSet();

        ObjectAnimator rotationAnim = ObjectAnimator.ofFloat(mBtnLike, "rotation", 0f, 360f);
        rotationAnim.setDuration(300);
        rotationAnim.setInterpolator(ACCELERATE_INTERPOLATOR);

        ObjectAnimator bounceAnimX = ObjectAnimator.ofFloat(mBtnLike, "scaleX", 0.2f, 1f);
        bounceAnimX.setDuration(300);
        bounceAnimX.setInterpolator(OVERSHOOT_INTERPOLATOR);

        ObjectAnimator bounceAnimY = ObjectAnimator.ofFloat(mBtnLike, "scaleY", 0.2f, 1f);
        bounceAnimY.setDuration(300);
        bounceAnimY.setInterpolator(OVERSHOOT_INTERPOLATOR);
        bounceAnimY.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                updateLikeState();
            }
        });

        animatorSet.play(rotationAnim);
        animatorSet.play(bounceAnimX).with(bounceAnimY).after(rotationAnim);


        animatorSet.start();

    }

    private void updateLikeState() {
        if (mMoment.isLiked) {
            //vh.btnLike.setImageResource(R.drawable.social_like_click);
            mBtnLike.setImageResource(R.drawable.social_like_click);
        } else {
            mBtnLike.setImageResource(R.drawable.social_like);
        }
    }

    private void updateLikeCount() {
        int fromValue;
        if (mMoment.isLiked) {
            fromValue = mMoment.likesCount - 1;
        } else {
            fromValue = mMoment.likesCount + 1;
        }

        mTsLikeCount.setCurrentText(String.valueOf(fromValue));

        String toValue = String.valueOf(mMoment.likesCount);
        mTsLikeCount.setText(toValue);

    }
}
