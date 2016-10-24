package com.waylens.hachi.ui.community;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobHelper;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.community.event.MomentModifyEvent;
import com.waylens.hachi.ui.dialogs.DialogHelper;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.entities.moment.MomentAbstract;
import com.waylens.hachi.ui.entities.moment.MomentEx;
import com.xfdingustc.rxutils.library.RxBus;

import butterknife.BindView;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by Xiaofei on 2016/9/26.
 */

public class PhotoViewActivity extends BaseActivity {
    public static final String EXTRA_PHOTO_URL = "extra.photo.url";
    public static final String EXTRA_MOMENT = "extra.momentEx";
    public static final String EXTRA_MOMENT_INDEX = "extra.moment.index";

    public String mPhotoUrl;

    public PhotoViewAttacher mAttacher;

    public MomentEx mMomentEx;

    public int mIndex = -1;

    private boolean isViewShowing = true;

    private RxBus mRxBus;

    @BindView(R.id.iv_photo)
    PhotoView mPhotoView;

    @BindView(R.id.ll_action_bar)
    LinearLayout mAction_bar;

    @BindView(R.id.tv_title)
    TextView mTvTitle;

    @BindView(R.id.tv_like_counter)
    TextView mTvLikeCounter;

    @BindView(R.id.tv_comment_counter)
    TextView mTvCommentCounter;

    @BindView(R.id.btn_more)
    ImageButton mBtnMore;

    @BindView(R.id.btn_like)
    ImageButton mBtnLike;

    @BindView(R.id.btn_comment)
    ImageButton mBtnComment;

    public static void launch(Activity activity, MomentEx momentEx, String url, int index) {
        Intent intent = new Intent(activity, PhotoViewActivity.class);
        intent.putExtra(EXTRA_MOMENT, momentEx);
        intent.putExtra(EXTRA_PHOTO_URL, url);
        intent.putExtra(EXTRA_MOMENT_INDEX, index);
        activity.startActivity(intent);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photoview);
        mRxBus = RxBus.getDefault();
        init();
    }

    @Override
    protected void init() {
        super.init();
        Intent intent = getIntent();
        mPhotoUrl = intent.getStringExtra(EXTRA_PHOTO_URL);
        mMomentEx = (MomentEx)intent.getSerializableExtra(EXTRA_MOMENT);
        mIndex = intent.getIntExtra(EXTRA_MOMENT_INDEX, -1);
        initView();
        setStatusBarColor(Color.BLACK);
    }

    private void initView() {
        mAttacher = new PhotoViewAttacher(mPhotoView);
        if (mMomentEx.moment.title != null) {
            mTvTitle.setText(mMomentEx.moment.title);
        }
        final MomentAbstract moment = mMomentEx.moment;
        if (moment.isLiked) {
            mBtnLike.setImageResource(R.drawable.ic_favorite);
        } else {
            mBtnLike.setImageResource(R.drawable.ic_favorite_border);
        }
        mTvLikeCounter.setText(Integer.toString(moment.likesCount));
        mTvCommentCounter.setText(Integer.toString(moment.commentsCount));
        mBtnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTvLikeCounter.setText(Integer.toString(mMomentEx.moment.likesCount));
                if (!moment.isLiked) {
                    mTvLikeCounter.setText(Integer.toString(moment.likesCount + 1));
                    moment.likesCount++;
                    mBtnLike.setImageResource(R.drawable.ic_favorite);
                    mRxBus.post(new MomentModifyEvent(MomentModifyEvent.LIKE_EVENT, mIndex, Boolean.TRUE));
                } else {
                    mTvLikeCounter.setText(Integer.toString(moment.likesCount - 1));
                    moment.likesCount--;
                    mBtnLike.setImageResource(R.drawable.ic_favorite_border);
                    mRxBus.post(new MomentModifyEvent(MomentModifyEvent.LIKE_EVENT, mIndex, Boolean.FALSE));
                }
                BgJobHelper.addLike(moment.id, moment.isLiked);
                moment.isLiked = !moment.isLiked;
            }
        });
        mBtnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommentActivity.launch(PhotoViewActivity.this, moment.id);
            }
        });
        mAttacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                toggleOtherView();
            }
        });

        Glide.with(this)
            .load(mPhotoUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .crossFade()
            .into(new SimpleTarget<GlideDrawable>() {
                @Override
                public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                    mPhotoView.setImageDrawable(resource);
                    mAttacher.update();
                }
            });

        mBtnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(PhotoViewActivity.this, mBtnMore, Gravity.END);
                popupMenu.getMenuInflater().inflate(R.menu.menu_moment, popupMenu.getMenu());
                if (mMomentEx.owner.userID.equals(SessionManager.getInstance().getUserId())) {
                    popupMenu.getMenu().removeItem(R.id.report);
                } else {
                    popupMenu.getMenu().removeItem(R.id.delete);
                    popupMenu.getMenu().removeItem(R.id.edit);
                }
                popupMenu.getMenu().removeItem(R.id.edit);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.report:
                                onReportClick(moment.id);
                                break;
                            case R.id.delete:
                                onDeleteClick(moment.id, mIndex);
                                break;
                            case R.id.edit:
//                               onEditClick(moment.id, moment.title, holder);
                                break;
                        }
                        return true;
                    }
                });
                popupMenu.show();
            }
        });
    }

    public void setStatusBarColor(int statusBarColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // If both system bars are black, we can remove these from our layout,
            // removing or shrinking the SurfaceFlinger overlay required for our views.
            Window window = getWindow();
            if (statusBarColor == Color.BLACK && window.getNavigationBarColor() == Color.BLACK) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            }
            window.setStatusBarColor(Color.parseColor("#4CAF50"));
        }
    }

    private void toggleOtherView() {
        AnimationSet animationSet = new AnimationSet(true);
        AlphaAnimation alphaAnimation;
        if (isViewShowing) {
            alphaAnimation = new AlphaAnimation(1, 0);
        } else {
            alphaAnimation = new AlphaAnimation(0, 1);
            mAction_bar.setVisibility(View.VISIBLE);
            mTvTitle.setVisibility(View.VISIBLE);
        }
        animationSet.addAnimation(alphaAnimation);
        animationSet.setDuration(200);

        mAction_bar.startAnimation(animationSet);
        mTvTitle.startAnimation(animationSet);

        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (isViewShowing) {
                    mAction_bar.setVisibility(isViewShowing ? View.INVISIBLE : View.VISIBLE);
                    mTvTitle.setVisibility(isViewShowing ? View.INVISIBLE : View.VISIBLE);
                }
                isViewShowing=!isViewShowing;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

    }

    private void onReportClick(final long momentId) {
        if (!SessionManager.getInstance().isLoggedIn()) {
            AuthorizeActivity.launch(this);
            return;
        }
        if (!SessionManager.checkUserVerified(this)) {
            return;
        }
        DialogHelper.showReportMomentDialog(this, momentId);
    }


    private void onDeleteClick(final long momentId, final int position) {
        DialogHelper.showDeleteMomentConfirmDialog(this, momentId, new DialogHelper.OnPositiveClickListener() {
            @Override
            public void onPositiveClick() {
                RxBus.getDefault().post(new MomentModifyEvent(MomentModifyEvent.DELETE_EVENT, position, null));
            }
        });
    }
}
