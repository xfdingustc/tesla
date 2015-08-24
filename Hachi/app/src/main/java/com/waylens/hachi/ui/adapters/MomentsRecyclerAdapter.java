package com.waylens.hachi.ui.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.fragments.YouTubeFragment;
import com.waylens.hachi.utils.ImageUtils;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

/**
 * Created by Richard on 8/21/15.
 */
public class MomentsRecyclerAdapter extends RecyclerView.Adapter<MomentViewHolder> {

    ArrayList<Moment> mMoments;

    PrettyTime mPrettyTime;

    FragmentManager mFragmentManager;

    YouTubeFragment videoFragment;

    public MomentsRecyclerAdapter(ArrayList<Moment> moments, FragmentManager fm) {
        mMoments = moments;
        mPrettyTime = new PrettyTime();
        mFragmentManager = fm;
        videoFragment = YouTubeFragment.newInstance();
    }

    public void setMoments(ArrayList<Moment> moments) {
        mMoments = moments;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (mMoments == null) {
            return 0;
        }
        return mMoments.get(position).type;
    }


    @Override
    public MomentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_feed, parent, false);
        ViewStub viewStub = (ViewStub) itemView.findViewById(R.id.video_player_stub);
        if (viewType == Moment.TYPE_WAYLENS) {
            viewStub.setLayoutResource(R.layout.layout_video_play);
            viewStub.inflate();
            return new WaylensMomentVH(itemView);
        } else {
            viewStub.setLayoutResource(R.layout.layout_video_player_youtube);
            FrameLayout view = (FrameLayout) viewStub.inflate();
            FrameLayout fragmentContainer = new FrameLayout(itemView.getContext());
            int viewId = generateViewId();
            fragmentContainer.setId(viewId);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            view.addView(fragmentContainer, layoutParams);
            return new YouTubeMomentVH(itemView, fragmentContainer);
        }
    }

    static int generateViewId() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            return View.generateViewId();
        } else {
            return new Random(System.currentTimeMillis()).nextInt(0x00FFFFFF);
        }
    }

    @Override
    public void onBindViewHolder(MomentViewHolder holder, int position) {
        final Moment moment = mMoments.get(position);

        ImageLoader.getInstance().displayImage(moment.owner.avatarUrl, holder.userAvatar, ImageUtils.getAvatarOptions());
        holder.userName.setText(moment.owner.userName);
        holder.videoTime.setText(mPrettyTime.formatUnrounded(new Date(moment.uploadTime)));
        ImageLoader.getInstance().displayImage(moment.thumbnail, holder.videoCover, ImageUtils.getVideoOptions());
        holder.videoDuration.setText(DateUtils.formatElapsedTime(moment.duration / 1000l));
        holder.likeCount.setText("" + moment.likesCount);

        if (moment.type == Moment.TYPE_WAYLENS) {
            WaylensMomentVH vh = (WaylensMomentVH) holder;
            vh.videoPlayView.setVisibility(View.GONE);
        } else {
            final YouTubeMomentVH vh = (YouTubeMomentVH) holder;
            vh.videoCover.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFragmentManager.beginTransaction().remove(videoFragment).commit();
                    videoFragment = YouTubeFragment.newInstance();
                    videoFragment.setVideoId(moment.videoID);
                    mFragmentManager.beginTransaction().replace(vh.fragmentContainer.getId(), videoFragment).commit();
                }
            });

        }

    }

    @Override
    public int getItemCount() {
        if (mMoments == null) {
            return 0;
        } else {
            return mMoments.size();
        }

    }
}
