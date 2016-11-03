package com.waylens.hachi.ui.clips.music;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.rest.response.MusicCategoryResponse;
import com.waylens.hachi.utils.MusicCategoryHelper;
import com.waylens.hachi.utils.rxjava.RxBus;


import butterknife.BindView;
import butterknife.ButterKnife;

import static com.waylens.hachi.rest.response.MusicCategoryResponse.MusicCategory;

/**
 * Created by Xiaofei on 2016/8/17.
 */
public class MusicCategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = MusicCategoryAdapter.class.getSimpleName();
    private final Activity mActivity;
    private MusicCategoryResponse mCategories;

    public MusicCategoryAdapter(Activity activity) {
        this.mActivity = activity;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music_category, parent, false);
        return new MusicCategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MusicCategoryViewHolder viewHolder = (MusicCategoryViewHolder) holder;
        final MusicCategory category = mCategories.categories.get(position);
        viewHolder.musicCategoryName.setText(category.category);
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RxBus.getDefault().post(new MusicCategorySelectEvent(category));
                MusicFragment fragment = MusicFragment.newInstance(category);
                mActivity.getFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).addToBackStack(TAG).commit();
            }
        });
        viewHolder.contentView.setBackgroundColor(MusicCategoryHelper.getMusicBgColor(position));
    }

    @Override
    public int getItemCount() {
        return mCategories == null ? 0 : mCategories.categories.size();
    }


    public void setCategories(MusicCategoryResponse categories) {
        this.mCategories = categories;
        notifyDataSetChanged();
    }

    public class MusicCategoryViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.music_category_name)
        TextView musicCategoryName;

        @BindView(R.id.content_view)
        FrameLayout contentView;

        public MusicCategoryViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
