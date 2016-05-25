package com.waylens.hachi.ui.avatar;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


import com.bumptech.glide.Glide;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.avatar.serializables.Photo;
import com.waylens.hachi.ui.avatar.serializables.PhotoSerializable;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.utils.ImageUtils;
import com.waylens.hachi.utils.ThumbnailsUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2015/6/29.
 */
public class PhotoPickerFragment extends BaseFragment implements View.OnClickListener {
    private static final String TAG = PhotoPickerFragment.class.getSimpleName();


    public interface OnPhotoSelectClickListener {
        void onOKClickListener(Photo selectedPhoto);
    }

    private OnPhotoSelectClickListener onPhotoSelectClickListener;



    private List<Photo> dataList;

    @BindView(R.id.rvPhotoList)
    RecyclerView mRvPhotoList;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (onPhotoSelectClickListener == null) {
            onPhotoSelectClickListener = (OnPhotoSelectClickListener) activity;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_photo_picker, savedInstanceState);
        return view;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = getArguments();

        // 文件夹照片列表;
        PhotoSerializable photoSerializable = (PhotoSerializable) args.getSerializable("list");
        dataList = new ArrayList<Photo>();
        dataList.addAll(photoSerializable.getList());

        init();
    }

    public void updateDataList(List<Photo> newList) {
        if (dataList == newList) {
            return;
        }
        dataList.clear();
        dataList.addAll(newList);
//        gridImageAdapter.notifyDataSetChanged();
    }

    private void init() {
        initViews();
    }

    private void initViews() {
        mRvPhotoList.setLayoutManager(new GridLayoutManager(getActivity(), 4));
        PhotoListAdapter adapter = new PhotoListAdapter();
        mRvPhotoList.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        PhotoListViewHolder viewHolder = (PhotoListViewHolder) v.getTag();
        int position = viewHolder.getLayoutPosition();
        if (onPhotoSelectClickListener != null) {
            onPhotoSelectClickListener.onOKClickListener(dataList.get(position));
        }

    }


    private class PhotoListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.grid_item_img, parent, false);
            return new PhotoListViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            PhotoListViewHolder viewHolder = (PhotoListViewHolder) holder;
            Photo photo = dataList.get(position);
            String displayItemUri = ThumbnailsUtil.MapgetHashValue(photo.getImageId(), photo.getUrl());

            Glide.with(PhotoPickerFragment.this).load(displayItemUri).crossFade().into(viewHolder.imageView);

        }

        @Override
        public int getItemCount() {
            return dataList == null ? 0 : dataList.size();
        }
    }

    class PhotoListViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.image_view)
        ImageView imageView;

        public PhotoListViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            imageView.setTag(this);
            imageView.setOnClickListener(PhotoPickerFragment.this);

        }
    }

}
