package com.waylens.hachi.ui.avatar;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;

import android.view.View;
import android.view.Window;
import android.widget.TextView;


import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.avatar.serializables.AlbumInfo;
import com.waylens.hachi.ui.avatar.serializables.AlbumSerializable;
import com.waylens.hachi.ui.avatar.serializables.Photo;
import com.waylens.hachi.ui.avatar.serializables.PhotoSerializable;
import com.waylens.hachi.utils.ThumbnailsUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Xiaofei on 2015/6/29.
 */
public class LocalPhotoActivity extends BaseActivity implements AlbumFragment.OnAlbumClickedListener
    , PhotoPickerFragment.OnPhotoSelectClickListener {

    private static final String TAG = LocalPhotoActivity.class.getSimpleName();

    private TextView titleTextView;
    private TextView tvLeftArrowBtn;
    private TextView tvRightCancelBtn;

    private AlbumFragment photoFolderFragment;
    private PhotoPickerFragment photoPickerFragment;
    private Fragment currentFragment;

    private FragmentManager manager;

    private List<AlbumInfo> listImageInfo = new ArrayList<>();
    private ContentResolver cr;

    private String albumName = null;



    public static void launch(Activity activity, String albumName, int requestCode) {
        Intent intent = new Intent();
        intent.setClass(activity, LocalPhotoActivity.class);
        if (albumName != null && albumName.length() > 0) {
            intent.putExtra("albumName", albumName);
        }
        activity.startActivityForResult(intent, requestCode);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();


        titleTextView = (TextView) findViewById(R.id.tvTitleName);
        titleTextView.setText("选择相册");
        tvLeftArrowBtn = (TextView) findViewById(R.id.tvTitleArrowBtnLeft);
        tvLeftArrowBtn.setText("相册");
        tvLeftArrowBtn.setVisibility(View.GONE);
        tvLeftArrowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showPhotoAlbumFragment();
            }
        });
        tvRightCancelBtn = (TextView) findViewById(R.id.tvTitleBtnRightButton);
        tvRightCancelBtn.setText(getString(android.R.string.cancel));
        tvRightCancelBtn.setVisibility(View.VISIBLE);
        tvRightCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                LocalPhotoActivity.this.finish();
            }
        });

        manager = getFragmentManager();

        photoFolderFragment = new AlbumFragment();
        photoPickerFragment = new PhotoPickerFragment();

        cr = getContentResolver();
        listImageInfo.clear();

        albumName = getIntent().getStringExtra("albumName");

        new ImageLoadAsyncTask().execute();

    }

    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_localphoto);
        setupToolbar();
    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.choose_album);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onAlbumClickedListener(String albumName, List<Photo> list) {
        showPhotoPickerFragment(albumName, list);
    }

    private void showPhotoPickerFragment(String albumName, List<Photo> list) {
//        tvLeftArrowBtn.setVisibility(View.VISIBLE);
        getToolbar().setTitle(albumName);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPhotoAlbumFragment();
            }
        });

        FragmentTransaction transaction = manager.beginTransaction().setTransition
            (FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        if (!photoPickerFragment.isAdded()) {
            Bundle args = new Bundle();
            PhotoSerializable photoSerializable = new PhotoSerializable();
            photoSerializable.setList(list);
            args.putSerializable("list", photoSerializable);
            photoPickerFragment.setArguments(args);
            transaction.hide(photoFolderFragment).add(R.id.fragment_container, photoPickerFragment).commit();
        } else {
            photoPickerFragment.updateDataList(list);
            transaction.hide(photoFolderFragment).show(photoPickerFragment).commit();
        }
        currentFragment = photoPickerFragment;
    }

    private void showPhotoAlbumFragment() {
        tvLeftArrowBtn.setVisibility(View.GONE);
        getToolbar().setTitle(R.string.choose_album);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        FragmentTransaction transaction = manager.beginTransaction().setTransition
            (FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        if (!photoFolderFragment.isAdded()) {
            transaction.hide(photoPickerFragment).add(R.id.fragment_container, photoFolderFragment).commit();
        } else {
            transaction.hide(photoPickerFragment).show(photoFolderFragment).commit();
        }
        currentFragment = photoFolderFragment;
    }


    @Override
    public void onOKClickListener(Photo selectedPhoto) {
        Intent data = new Intent();
        data.putExtra("photoPath", selectedPhoto.getPathAbsolute());
        data.putExtra("albumName", titleTextView.getText());
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {

            finish();
            return;
        }
        // 根据上面发送过去的请求码来区别
        switch (requestCode) {
            case 50001:
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (currentFragment == photoFolderFragment) {
            setResult(RESULT_CANCELED);
            finish();
        } else {
            showPhotoAlbumFragment();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void finish() {
        super.finish();

    }

    private class ImageLoadAsyncTask extends AsyncTask<Void, Void, Object> {
        @Override
        protected Object doInBackground(Void... params) {
            //获取缩略图
            ThumbnailsUtil.clear();
            String[] projection = {MediaStore.Images.Thumbnails._ID, MediaStore.Images.Thumbnails.IMAGE_ID, MediaStore.Images.Thumbnails.DATA};
            Cursor cur = cr.query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, projection, null, null, null);

            if (cur != null && cur.moveToFirst()) {
                int image_id;
                String image_path;
                int image_idColumn = cur.getColumnIndex(MediaStore.Images.Thumbnails.IMAGE_ID);
                int dataColumn = cur.getColumnIndex(MediaStore.Images.Thumbnails.DATA);
                do {
                    image_id = cur.getInt(image_idColumn);
                    image_path = cur.getString(dataColumn);
                    ThumbnailsUtil.put(image_id, "file://" + image_path);
                } while (cur.moveToNext());
            }

            cur.close();
            //获取原图
            Cursor cursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, "date_modified DESC");

            String _path = "_data";
            String _album = "bucket_display_name";

            HashMap<String, AlbumInfo> myhash = new HashMap<String, AlbumInfo>();
            AlbumInfo albumInfo = null;
            Photo photo = null;
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int index = 0;
                    int _id = cursor.getInt(cursor.getColumnIndex("_id"));
                    String path = cursor.getString(cursor.getColumnIndex(_path));
                    String album = cursor.getString(cursor.getColumnIndex(_album));
                    List<Photo> stringList = new ArrayList<>();
                    photo = new Photo();
                    if (myhash.containsKey(album)) {
                        albumInfo = myhash.remove(album);
                        if (listImageInfo.contains(albumInfo))
                            index = listImageInfo.indexOf(albumInfo);
                        photo.setImageId(_id);
                        photo.setUrl("file://" + path);
                        photo.setPathAbsolute(path);
                        albumInfo.getList().add(photo);
                        listImageInfo.set(index, albumInfo);
                        myhash.put(album, albumInfo);
                    } else {
                        albumInfo = new AlbumInfo();
                        stringList.clear();
                        photo.setImageId(_id);
                        photo.setUrl("file://" + path);
                        photo.setPathAbsolute(path);
                        stringList.add(photo);
                        albumInfo.setImage_id(_id);
                        albumInfo.setPath_file("file://" + path);
                        albumInfo.setPath_absolute(path);
                        albumInfo.setName_album(album);
                        albumInfo.setList(stringList);
                        listImageInfo.add(albumInfo);
                        myhash.put(album, albumInfo);
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            super.onPostExecute(result);
            List<Photo> listInit = null;
            if (albumName != null && albumName.length() > 0) {
                for (AlbumInfo ai : listImageInfo) {
                    if (ai.getName_album().equals(albumName)) {
                        listInit = ai.getList();
                        break;
                    }
                }
            }
            Bundle args = new Bundle();
            AlbumSerializable photoSerializable = new AlbumSerializable();
            photoSerializable.setList(listImageInfo);
            args.putSerializable("list", photoSerializable);
            photoFolderFragment.setArguments(args);
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.add(R.id.fragment_container, photoFolderFragment);
            transaction.commit();

            currentFragment = photoFolderFragment;
            if (listInit != null) {
                showPhotoPickerFragment(albumName, listInit);
            }
        }
    }
}
