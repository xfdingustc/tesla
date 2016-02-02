package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.adapters.MusicListAdapter;
import com.waylens.hachi.ui.entities.MusicItem;
import com.waylens.hachi.ui.views.RecyclerViewExt;

import java.util.ArrayList;

import butterknife.Bind;

/**
 * Created by Richard on 2/1/16.
 */
public class MusicFragment extends BaseFragment {

    @Bind(R.id.music_list_view)
    RecyclerViewExt mMusicListView;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @Bind(R.id.refresh_layout)
    SwipeRefreshLayout mRefreshLayout;

    MusicListAdapter mMusicListAdapter;
    ArrayList<MusicItem> mMusicItems;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMusicItems = new ArrayList<>();
        mMusicItems.add(new MusicItem("California (There Is No End to Love)", "", 1000 * 179));
        mMusicItems.add(new MusicItem("Cedarwood Road", "", 1000 * 265));
        mMusicItems.add(new MusicItem("The City (Live)", "", 1000 * 339));
        mMusicItems.add(new MusicItem("Drunk (Live)", "", 1000 * 268));
        mMusicItems.add(new MusicItem("Every Breaking Wave", "", 1000 * 255));
        mMusicItems.add(new MusicItem("The Miracle (Of Joey Ramone)", "", 1000 * 287));
        mMusicListAdapter = new MusicListAdapter();
        mMusicListAdapter.setMusicItems(mMusicItems);
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
        mViewAnimator.setDisplayedChild(1);
    }
}
