package com.waylens.hachi.ui.avatar;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.avatar.serializables.AlbumInfo;
import com.waylens.hachi.ui.avatar.serializables.AlbumSerializable;
import com.waylens.hachi.ui.avatar.serializables.Photo;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.utils.ThumbnailsUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;


/**
 * Created by Xiaofei on 2015/6/29.
 */
public class AlbumFragment extends BaseFragment implements View.OnClickListener {



    public interface OnAlbumClickedListener {
        void onAlbumClickedListener(String albumName, List<Photo> list);
    }

    private OnAlbumClickedListener onPageLodingClickListener;
    private ListView listView;
    private List<AlbumInfo> listAlbumInfo = new ArrayList<AlbumInfo>();
    private AlbumAdapter listAdapter;

    private AlbumSerializable mPhotoAlbumSerializable;

    private ImageLoader mImageLoader = ImageLoader.getInstance();

    @Bind(R.id.rvAlbumList)
    RecyclerView mRvAlbumList;

    @Override
    public void onClick(View v) {
        AlbumAdapterViewHolder viewHolder = (AlbumAdapterViewHolder)v.getTag();
        int position = viewHolder.getLayoutPosition();
        AlbumInfo albumInfo = mPhotoAlbumSerializable.getList().get(position);
        onPageLodingClickListener.onAlbumClickedListener(albumInfo.getName_album(), albumInfo.getList());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (onPageLodingClickListener == null) {
            onPageLodingClickListener = (OnAlbumClickedListener) activity;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_photo_album, savedInstanceState);
        initViews();
        return view;
    }

    private void initViews() {
        mRvAlbumList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRvAlbumList.setAdapter(new AlbumAdapter());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = getArguments();

        mPhotoAlbumSerializable = (AlbumSerializable) args.getSerializable("list");


    }


    private class AlbumAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.list_item_album, parent, false);

            return new AlbumAdapterViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            AlbumAdapterViewHolder viewHolder = (AlbumAdapterViewHolder)holder;
            final AlbumInfo item = mPhotoAlbumSerializable.getList().get(position);

            String displayItemUri = ThumbnailsUtil.MapgetHashValue(item.getImage_id(), item.getPath_file());
            mImageLoader.displayImage(displayItemUri, viewHolder.ivCover);

            viewHolder.tvAlbumTitle.setText(item.getName_album());
            viewHolder.tvAlbumItemCount.setText("" + item.getList().size());
        }

        @Override
        public int getItemCount() {
            return mPhotoAlbumSerializable == null ? 0 : mPhotoAlbumSerializable.getList().size();
        }
    }


    class AlbumAdapterViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.album_item_cover)
        ImageView ivCover;

        @Bind(R.id.album_item_title)
        TextView tvAlbumTitle;

        @Bind(R.id.album_item_count)
        TextView tvAlbumItemCount;


        public AlbumAdapterViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setTag(AlbumAdapterViewHolder.this);
            itemView.setOnClickListener(AlbumFragment.this);
        }
    }


}
