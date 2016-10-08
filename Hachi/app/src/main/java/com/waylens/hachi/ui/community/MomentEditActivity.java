package com.waylens.hachi.ui.community;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.MomentUpdateBody;
import com.waylens.hachi.rest.response.SimpleBoolResponse;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.entities.MomentPicture;
import com.waylens.hachi.utils.TransitionHelper;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Xiaofei on 2016/8/9.
 */
public class MomentEditActivity extends BaseActivity {
    private static final String TAG = MomentEditActivity.class.getSimpleName();
    public static final String EXTRA_MOMENT = "moment";


    private Moment mMoment;


    public static void launch(Activity activity, Moment moment, View transitionView) {
        Intent intent = new Intent(activity, MomentEditActivity.class);
        intent.putExtra(EXTRA_MOMENT, moment);
        final Pair<View, String>[] pairs = TransitionHelper.createSafeTransitionParticipants(activity,
            false, new Pair<>(transitionView, activity.getString(R.string.moment_cover)));
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, pairs);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }



    @BindView(R.id.moment_cover)
    ImageView mMomentCover;

    @BindView(R.id.moment_title)
    TextInputEditText mMomentTitle;

    @BindView(R.id.moment_description)
    TextInputEditText mMomentDescription;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        Intent intent = getIntent();
        mMoment = (Moment)intent.getSerializableExtra(EXTRA_MOMENT);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_moment_edit);
        setupToolbar();
        String thumbnailUrl = mMoment.thumbnail;
        if (!TextUtils.isEmpty(mMoment.momentType) && mMoment.momentType.equals("PICTURE")) {
            final List<MomentPicture> momentPictures = mMoment.pictureUrls;
            if (!momentPictures.isEmpty()) {
                MomentPicture momentPicture = momentPictures.get(0);
                if (!TextUtils.isEmpty(momentPicture.bigThumbnail)) {
                    thumbnailUrl = momentPicture.bigThumbnail;
                } else if (!TextUtils.isEmpty(momentPicture.smallThumbnail)) {
                    thumbnailUrl = momentPicture.smallThumbnail;
                } else {
                    thumbnailUrl = momentPicture.original;
                }
            }
        }
        Logger.t(TAG).d("moment: " + mMoment.toString());
        mMomentTitle.setText(mMoment.title);
        mMomentDescription.setText(mMoment.description);
        Glide.with(this)
            .load(thumbnailUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(mMomentCover);
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.edit_moment);
        getToolbar().inflateMenu(R.menu.menu_share);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.share:
                        doModifyMoment();
                        break;
                }
                return true;
            }
        });
    }

    private void doModifyMoment() {
        HachiApi mHachiApi = HachiService.createHachiApiService();
        final MomentUpdateBody body = new MomentUpdateBody();
        body.title = mMomentTitle.getEditableText().toString();
        body.description = mMomentDescription.getEditableText().toString();
        mHachiApi.updateMoment(mMoment.id, body).enqueue(new Callback<SimpleBoolResponse>() {
            @Override
            public void onResponse(Call<SimpleBoolResponse> call, Response<SimpleBoolResponse> response) {
                Logger.t(TAG).d("result: " + response.body().result);
                EventBus.getDefault().post(new MomentChangeEvent(mMoment.id, body));
                onBackPressed();
            }

            @Override
            public void onFailure(Call<SimpleBoolResponse> call, Throwable t) {

            }
        });
    }
}
