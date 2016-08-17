package com.waylens.hachi.ui.clips.music;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewSwitcher;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.response.MusicCategoryResponse;
import com.waylens.hachi.ui.fragments.BaseFragment;

import butterknife.BindView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Xiaofei on 2016/8/18.
 */
public class MusicCategoryFragment extends BaseFragment {
    private static final String TAG = MusicCategoryFragment.class.getSimpleName();
    private MusicCategoryAdapter mMusicCategoryAdapter;
    private MusicCategoryResponse mMusicCategoryList;

    @BindView(R.id.vs_root)
    ViewSwitcher mVsRoot;

    @BindView(R.id.music_style_list)
    RecyclerView mMusicStyleList;

    @Override
    protected String getRequestTag() {
        return TAG;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_music_category, savedInstanceState);
        initViews();
        return view;
    }

    private void initViews() {
        mMusicStyleList.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        mMusicCategoryAdapter = new MusicCategoryAdapter(getActivity());
        mMusicStyleList.setAdapter(mMusicCategoryAdapter);
        fetchMusicCategory();
    }

    private void fetchMusicCategory() {
        HachiApi hachiApi = HachiService.createHachiApiService();
        final Call<MusicCategoryResponse> musicCategoryResponseCall = hachiApi.getMusicCategories();
        musicCategoryResponseCall.enqueue(new Callback<MusicCategoryResponse>() {
            @Override
            public void onResponse(Call<MusicCategoryResponse> call, Response<MusicCategoryResponse> response) {
                mVsRoot.showNext();
                mMusicCategoryList = response.body();
                mMusicCategoryAdapter.setCategories(mMusicCategoryList);

                Logger.t(TAG).d("update: " + mMusicCategoryList.lastUpdateTime + " categoies size: " + mMusicCategoryList.categories.size());
            }

            @Override
            public void onFailure(Call<MusicCategoryResponse> call, Throwable t) {
                Logger.t(TAG).d("Get music error");
            }
        });
    }
}
