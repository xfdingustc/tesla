package com.waylens.hachi.ui.clips.music;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.android.volley.RequestQueue;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.rest.response.MusicCategoryResponse;
import com.waylens.hachi.rest.response.MusicList;
import com.waylens.hachi.ui.entities.MusicItem;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.helpers.DownloadHelper;
import com.waylens.hachi.ui.views.RecyclerViewExt;
import com.waylens.hachi.utils.VolleyUtil;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;


import butterknife.BindView;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static com.waylens.hachi.rest.response.MusicCategoryResponse.MusicCategory;

public class MusicFragment extends BaseFragment implements MusicListAdapter.OnMusicActionListener {
    private static final String TAG = MusicFragment.class.getSimpleName();

    private static final String EXTRA_MUSIC_CATEGORY = "extra.music.category";

    private MusicCategory mMusicCategory;


    MusicListAdapter mMusicListAdapter;

    RequestQueue mRequestQueue;

    DownloadHelper mDownloadHelper;

    MusicItem mMusicItem;
    MusicListAdapter.ViewHolder mViewHolder;

    @BindView(R.id.music_list_view)
    RecyclerViewExt mMusicListView;

    @BindView(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @BindView(R.id.refresh_layout)
    SwipeRefreshLayout mRefreshLayout;

    @Override
    protected String getRequestTag() {
        return TAG;
    }


    public static MusicFragment newInstance(MusicCategory category) {
        MusicFragment fragment = new MusicFragment();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_MUSIC_CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMusicCategory = (MusicCategoryResponse.MusicCategory) getArguments().getSerializable(EXTRA_MUSIC_CATEGORY);
        mRequestQueue = VolleyUtil.newVolleyRequestQueue(getActivity());
        mDownloadHelper = new DownloadHelper(getActivity(), mListener);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createFragmentView(inflater, container, R.layout.fragment_music, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LinearLayoutManager layoutManager = new LinearLayoutManager(view.getContext());
        mMusicListView.setLayoutManager(layoutManager);
        mMusicListAdapter = new MusicListAdapter(this, mDownloadHelper, layoutManager);
        mMusicListView.setAdapter(mMusicListAdapter);
        mRefreshLayout.setEnabled(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        loadMusicList();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mDownloadHelper.broadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public void onStop() {
        getActivity().unregisterReceiver(mDownloadHelper.broadcastReceiver);
        mDownloadHelper.clearListener();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        mMusicListView.setAdapter(null);
        super.onDestroyView();
    }

    private void loadMusicList() {
        mViewAnimator.setDisplayedChild(0);

        mHachi.getMusicList(mMusicCategory.id)
            .subscribeOn(Schedulers.io())
            .map(new Func1<MusicList, MusicList>() {
                @Override
                public MusicList call(MusicList musicList) {
                    for (MusicItem item : musicList.musicList) {
                        item.checkLocalFile(Hachi.getContext());
                    }
                    return musicList;
                }
            })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<MusicList>() {
                @Override
                public void onNext(MusicList musicList) {
                    onLoadMusicsSuccessful(musicList);
                }

                @Override
                public void onError(Throwable e) {
                    super.onError(e);
                    if (mViewAnimator != null) {
                        mViewAnimator.setDisplayedChild(2);
                    }
                }
            });
    }


    private void onLoadMusicsSuccessful(MusicList musicList) {
        mMusicListAdapter.setMusicItems(musicList.musicList);
        mViewAnimator.setDisplayedChild(1);
    }

    DownloadHelper.OnDownloadListener mListener = new DownloadHelper.OnDownloadListener() {
        @Override
        public void onSuccess(DownloadHelper.IDownloadable downloadable, String filePath) {
            Logger.t(TAG).d("Download: " + downloadable);
            MusicItem musicItem = (MusicItem) downloadable;
            musicItem.localPath = filePath;
            musicItem.status = MusicItem.STATUS_LOCAL;
            mMusicListAdapter.updateMusicItem(musicItem);
        }

        @Override
        public void onError(DownloadHelper.IDownloadable downloadable) {
            MusicItem musicItem = (MusicItem) downloadable;
            musicItem.status = MusicItem.STATUS_REMOTE;
            mMusicListAdapter.updateMusicItem(musicItem);
        }
    };

    @Override
    public void onAddMusic(MusicItem musicItem) {
        Intent intent = new Intent();
        intent.putExtra("music.item", musicItem.toBundle());
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
    }

    @Override
    public void onDownloadMusic(MusicItem musicItem, MusicListAdapter.ViewHolder holder) {
        mMusicItem = musicItem;
        mViewHolder = holder;

        int permissionCheck = ContextCompat.checkSelfPermission(getActivity(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            continueDownloadMusic();
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                MusicListSelectActivity.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
    }

    public void continueDownloadMusic() {
        if (mMusicItem == null || mViewHolder == null) {
            return;
        }
        mMusicItem.status = MusicItem.STATUS_DOWNLOADING;
        mDownloadHelper.download(mMusicItem);
        mViewHolder.setDownloadStatus(MusicListAdapter.ViewHolder.STATUS_DOWNLOADING);
        mMusicItem = null;
        mViewHolder = null;
    }
}
