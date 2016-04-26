package com.waylens.hachi.ui.avatar;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.avatar.serializables.Photo;
import com.waylens.hachi.ui.avatar.serializables.PhotoSerializable;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2015/6/29.
 */
public class PhotoPickerFragment extends Fragment {
  private static final String TAG = PhotoPickerFragment.class.getSimpleName();

  public interface OnPhotoSelectClickListener {
    public void onOKClickListener(Photo selectedPhoto);
  }

  private OnPhotoSelectClickListener onPhotoSelectClickListener;

  protected ImageLoader imageLoader = ImageLoader.getInstance();
  protected boolean pauseOnScroll = true;
  protected boolean pauseOnFling = true; // 滑动时不异步取数据;

  private GridView gridView;
  private List<Photo> dataList;
  private PhotoPickerAdapter gridImageAdapter;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (onPhotoSelectClickListener == null) {
      onPhotoSelectClickListener = (OnPhotoSelectClickListener) activity;
    }
  }

  @Override
  public void onStop() {
    super.onStop();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_photo_picker, container, false);
    View unusedView = view.findViewById(R.id.bottom_layout);
    unusedView.setVisibility(View.GONE);
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
    gridImageAdapter.notifyDataSetChanged();
  }

  private void init() {
    View v = getView();

    gridView = (GridView) v.findViewById(R.id.myGrid);
    gridImageAdapter = new PhotoPickerAdapter(getActivity(), dataList);
    initListener();
    gridView.setAdapter(gridImageAdapter);

    gridView.setOnScrollListener(new PauseOnScrollListener(imageLoader, pauseOnScroll, pauseOnFling));
  }

  private void initListener() {
    gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (onPhotoSelectClickListener != null) {
          onPhotoSelectClickListener.onOKClickListener(dataList.get(position));
        }
      }
    });
  }

}
