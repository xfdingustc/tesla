package com.waylens.hachi.ui.avatar;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.avatar.serializables.AlbumInfo;
import com.waylens.hachi.ui.avatar.serializables.AlbumSerializable;
import com.waylens.hachi.ui.avatar.serializables.Photo;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Xiaofei on 2015/6/29.
 */
public class AlbumFragment extends Fragment {

  public interface OnAlbumClickedListener {
    public void onAlbumClickedListener(String albumName, List<Photo> list);
  }

  private OnAlbumClickedListener onPageLodingClickListener;
  private ListView listView;
  private List<AlbumInfo> listAlbumInfo = new ArrayList<AlbumInfo>();
  private AlbumAdapter listAdapter;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    if (onPageLodingClickListener == null) {
      onPageLodingClickListener = (OnAlbumClickedListener) activity;
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_photo_album, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    Bundle args = getArguments();

    AlbumSerializable photoAlbumSerializable = (AlbumSerializable) args.getSerializable("list");
    listAlbumInfo.clear();
    listAlbumInfo.addAll(photoAlbumSerializable.getList());

    listView = (ListView) getView().findViewById(R.id.lv_album_list);

    if (getActivity() != null) {
      listAdapter = new AlbumAdapter(getActivity(), listAlbumInfo);
      listView.setAdapter(listAdapter);
    }

    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        onPageLodingClickListener.onAlbumClickedListener(listAlbumInfo.get(arg2).getName_album(), listAlbumInfo.get(arg2).getList());
      }
    });
  }


}
