package com.waylens.hachi.ui.fragments;

import android.app.DownloadManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRequestQueue = VolleyUtil.newVolleyRequestQueue(getActivity());
        mDownloadHelper = new DownloadHelper(getActivity(), mListener);
        mMusicListAdapter = new MusicListAdapter(this, mDownloadHelper);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createFragmentView(inflater, container, R.layout.fragment_music, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMusicListView.setLayoutManager(new LinearLayoutManager(view.getContext()));
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
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mDownloadHelper.broadcastReceiver);
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
        Intent intent = new Intent("choose-bg-music");
        intent.putExtra("music-id", musicItem.id);
        intent.putExtra("name", musicItem.title);
        intent.putExtra("path", musicItem.localPath);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        getFragmentManager().popBackStack();
    }
}
