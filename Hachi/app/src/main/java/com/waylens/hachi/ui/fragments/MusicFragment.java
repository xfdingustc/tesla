package com.waylens.hachi.ui.fragments;

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
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.ui.activities.MusicDownloadActivity;
import com.waylens.hachi.ui.adapters.MusicListAdapter;
import com.waylens.hachi.ui.entities.MusicItem;
import com.waylens.hachi.ui.helpers.DownloadHelper;
import com.waylens.hachi.ui.views.RecyclerViewExt;
import com.waylens.hachi.utils.VolleyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.Bind;

/**
 * Created by Richard on 2/1/16.
 */
public class MusicFragment extends BaseFragment implements MusicListAdapter.OnMusicActionListener {
    private static final String TAG = MusicFragment.class.getSimpleName();

    @Bind(R.id.music_list_view)
    RecyclerViewExt mMusicListView;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @Bind(R.id.refresh_layout)
    SwipeRefreshLayout mRefreshLayout;

    MusicListAdapter mMusicListAdapter;

    RequestQueue mRequestQueue;

    DownloadHelper mDownloadHelper;

    MusicItem mMusicItem;
    MusicListAdapter.ViewHolder mViewHolder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    void loadMusicList() {
        mViewAnimator.setDisplayedChild(0);
        String url = Constants.HOST_URL + Constants.API_MUSICS;
        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        onLoadMusicsSuccessful(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (mViewAnimator != null) {
                            mViewAnimator.setDisplayedChild(2);
                        }
                    }
                }).setTag(Constants.API_MUSICS));
    }

    void onLoadMusicsSuccessful(JSONObject response) {
        ArrayList<MusicItem> musicItems = new ArrayList<>();
        JSONArray jsonArray = response.optJSONArray("musicList");
        if (jsonArray == null) {
            return;
        }
        for (int i = 0; i < jsonArray.length(); i++) {
            MusicItem musicItem = MusicItem.fromJson(jsonArray.optJSONObject(i), getActivity());
            if (musicItem != null) {
                musicItems.add(musicItem);
                if (!musicItem.isDownloaded()) {
                    Logger.t(TAG).d("Should Download: " + musicItem);
                } else {
                    Logger.t(TAG).d("Exist: " + musicItem);
                }
            }
        }
        mMusicListAdapter.setMusicItems(musicItems);
        mViewAnimator.setDisplayedChild(1);
    }

    DownloadHelper.OnDownloadListener mListener = new DownloadHelper.OnDownloadListener() {
        @Override
        public void onSuccess(DownloadHelper.Downloadable downloadable, String filePath) {
            Log.e("test", "Download: " + downloadable);
            MusicItem musicItem = (MusicItem) downloadable;
            musicItem.localPath = filePath;
            musicItem.status = MusicItem.STATUS_LOCAL;
            mMusicListAdapter.updateMusicItem(musicItem);
        }

        @Override
        public void onError(DownloadHelper.Downloadable downloadable) {
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
                    MusicDownloadActivity.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
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
