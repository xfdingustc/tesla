package com.waylens.hachi.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Richard on 12/18/15.
 */
public class ShareFragment extends Fragment implements FragmentNavigator {
    @Bind(R.id.video_cover)
    ImageView videoCover;

    @Bind(R.id.social_icons)
    RecyclerView mRecyclerView;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @Bind(R.id.share_success_view)
    ImageView mShareSuccessView;

    Clip mClip;
    //long mMinClipStartTimeMs;
    //long mMaxClipEndTimeMs;
    VdbImageLoader mImageLoader;

    CameraVideoPlayFragment mVideoPlayFragment;


    public static ShareFragment newInstance(Clip clip) {
        Bundle args = new Bundle();
        ShareFragment fragment = new ShareFragment();
        fragment.setArguments(args);
        fragment.mClip = clip;
        //fragment.mMinClipStartTimeMs = minClipStartTimeMs;
        //fragment.mMaxClipEndTimeMs = maxClipEndTimeMs;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageLoader = VdbImageLoader.getImageLoader(Snipe.newRequestQueue());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_share, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ClipPos clipPos = new ClipPos(mClip, mClip.getStartTimeMs(), ClipPos.TYPE_POSTER, false);
        mImageLoader.displayVdbImage(clipPos, videoCover);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 4);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(new IconAdapter());
        mShareSuccessView.setColorFilter(getResources().getColor(R.color.style_color_primary));
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mVideoPlayFragment != null) {
            getFragmentManager().beginTransaction().remove(mVideoPlayFragment).commitAllowingStateLoss();
            mVideoPlayFragment = null;
        }
    }

    @Override
    public void onDestroyView() {
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    @OnClick(R.id.btn_play)
    void playVideo() {
        mVideoPlayFragment = CameraVideoPlayFragment.newInstance(Snipe.newRequestQueue(), mClip, null);
        getFragmentManager().beginTransaction().replace(R.id.enhance_fragment_content, mVideoPlayFragment).commit();
        videoCover.setVisibility(View.INVISIBLE);
    }

    @OnClick(R.id.btn_ok)
    void performShare() {
        mViewAnimator.setDisplayedChild(2);
    }

    @OnClick(R.id.btn_cancel)
    void onCancel() {
        close();
    }

    void close() {
        getFragmentManager().beginTransaction().remove(this).commit();
    }

    @Override
    public boolean onInterceptBackPressed() {
        if (VideoPlayFragment.fullScreenPlayer != null) {
            VideoPlayFragment.fullScreenPlayer.setFullScreen(false);
            return true;
        }
        close();
        return true;
    }

    static class IconAdapter extends RecyclerView.Adapter<IconVH> {

        int[] drawables = new int[]{
                R.drawable.toggle_waylens,
                R.drawable.toggle_facebook,
                R.drawable.toggle_twitter,
                R.drawable.toggle_youtube,
                R.drawable.toggle_vimeo,
                R.drawable.toggle_pinterest,
                R.drawable.toggle_instagram,
        };

        @Override
        public IconVH onCreateViewHolder(ViewGroup parent, int viewType) {
            CheckBox checkBox = new CheckBox(parent.getContext());
            return new IconVH(checkBox);
        }

        @Override
        public void onBindViewHolder(IconVH holder, int position) {
            holder.mCheckBox.setButtonDrawable(drawables[position]);
            if (position == 0) {
                holder.mCheckBox.setChecked(true);
            } else {
                holder.mCheckBox.setEnabled(false);
            }
        }

        @Override
        public int getItemCount() {
            return drawables.length;
        }
    }

    static class IconVH extends RecyclerView.ViewHolder {
        public CheckBox mCheckBox;

        public IconVH(View itemView) {
            super(itemView);
            mCheckBox = (CheckBox) itemView;
        }
    }
}