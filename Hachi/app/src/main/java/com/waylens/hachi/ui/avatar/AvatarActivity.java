package com.waylens.hachi.ui.avatar;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;


import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.GlobalVariables;
import com.waylens.hachi.bgjob.BgJobHelper;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.views.ClipImageView;
import com.waylens.hachi.utils.ImageUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.BindView;


/**
 * Created by Xiaofei on 2015/6/17.
 */
public class AvatarActivity extends BaseActivity {
    private final static String TAG = AvatarActivity.class.getSimpleName();
    private final static String PICK_FROM_CAMERA = "pick_from_camera";
    private final String mImgCachePath = GlobalVariables.getAvatarUrl();

    private final static int TAKE_PHOTO = 1;
    private final static int FROM_LOCAL = 2;

    private Uri mAvatarUri;


    private String mCroppedImagePath = null;


    private String mReturnImagePath = null;

    @BindView(R.id.civ_cropper_preview)
    ClipImageView mCivCropperPreview;


    public static void launch(Activity startActivity, boolean fromCamera) {
        Intent intent = new Intent(startActivity, AvatarActivity.class);
        intent.putExtra(PICK_FROM_CAMERA, fromCamera);
        startActivity.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        boolean fromCamera = intent.getBooleanExtra(PICK_FROM_CAMERA, false);
        if (fromCamera) {
            jump2TakePhoto();
        } else {
//            LocalPhotoActivity.launch(this, albumName, FROM_LOCAL);
            jump2Picker();
        }

        init();

    }


    private void jump2TakePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        ContentValues values = new ContentValues(2);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        //mAvatarUri = Uri.fromFile(new File(GlobalVariables.getPictureName()));
        mAvatarUri = GlobalVariables.getPictureUri();
        Logger.t(TAG).d(mAvatarUri.getPath());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mAvatarUri);
        startActivityForResult(intent, TAKE_PHOTO);
    }


    private void jump2Picker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, FROM_LOCAL);
    }


    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_avatar_picker);
        setupToolbar();
    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(getString(R.string.avatar));
        getToolbar().setNavigationIcon(R.drawable.ic_arrow_back);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            finish();
            return;
        }

        if (requestCode == TAKE_PHOTO) {
            Logger.t(TAG).d("Get photo: " + mAvatarUri.getPath());
            mReturnImagePath = mAvatarUri.getPath();
        } else if (requestCode == FROM_LOCAL) {
            Uri imageUri = data.getData();
            Logger.t(TAG).d("image selected path", imageUri.getPath());

            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(imageUri, projection, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            mReturnImagePath = cursor.getString(column_index);
        }

        getToolbar().inflateMenu(R.menu.menu_confirm);
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.confirm:
                        saveCroppedImage();
                        BgJobHelper.uploadAvatar(mCroppedImagePath);
                        finish();
                        break;
                }
                return false;
            }
        });
        new ExtractThumbTask(mReturnImagePath, 1536, 2048).execute();
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void saveCroppedImage() {
        Bitmap croppedImage = mCivCropperPreview.clip();

        long now = System.currentTimeMillis();
        String photoId = String.format("%d.%03d", now / 1000, now % 1000);
        String fileName = photoId + ".jpg";
        mCroppedImagePath = mImgCachePath + "/" + fileName;
        try {
            FileOutputStream out = new FileOutputStream(mCroppedImagePath);
            Logger.t(TAG).d("try to compress file : " + mCroppedImagePath);
            if (croppedImage.compress(Bitmap.CompressFormat.JPEG, 60, out)) {
                out.flush();
            } else {
                ImageUtils.saveBitmap(croppedImage, mCroppedImagePath);
                Logger.t(TAG).d("try to save file : " + mCroppedImagePath);
            }
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ImageUtils.saveBitmap(croppedImage, mCroppedImagePath);

    }

//    private void uploadAvatar() {
//        UploadAvatarJob uploadAvatarJob = new UploadAvatarJob(mCroppedImagePath);
//        JobManager jobManager = BgJobManager.getManager();
//        jobManager.addJobInBackground(uploadAvatarJob);
//
//
//    }

    public class ExtractThumbTask extends AsyncTask<Object, Void, String> {
        String srcImgPath = null, dstImgPath = null;
        int reqWidth = 1536, reqHeight = 2048;
        Bitmap bmp = null;

        public ExtractThumbTask(String srcImgPath, int width, int height) {
            this.reqWidth = width;
            this.reqHeight = height;
            this.srcImgPath = srcImgPath;
        }

        protected void onPreExecute() {

        }

        @Override
        protected String doInBackground(Object... params) {
            long now = System.currentTimeMillis();
            String photoId = String.format("%d.%03d", now / 1000, now % 1000);
            String dstFileName = photoId + ".jpg";
            dstImgPath = mImgCachePath + "/" + dstFileName;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            int orientation = -1;

            try {
                ExifInterface exif = new ExifInterface(srcImgPath);
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                Logger.t(TAG).d("orientation:" + orientation);
            } catch (IOException e) {
                Logger.t(TAG).d(e.getMessage());
            }
            Bitmap bitmap = BitmapFactory.decodeFile(srcImgPath);
            Matrix m = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    m.postRotate(90);
                    bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    m.postRotate(180);
                    bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    m.postRotate(270);
                    bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                    break;
                default:
                    bmp = bitmap;
                    break;
            }

/*            BitmapFactory.decodeFile(srcImgPath, options);
            int w = options.outWidth;
            int h = options.outHeight;*/
            int w = bmp.getWidth();
            int h = bmp.getHeight();
            int newW = w, newH = h;
            float ratio = 1;
            if (w <= h) {
                ratio = Math.max((float) w / reqWidth, (float) h / reqHeight);
            } else {
                ratio = Math.max((float) h / reqWidth, (float) w / reqHeight);
            }

            if (ratio < 1.0f) {
                ratio = 1;
            }
            newW = (int) (w / ratio);
            newH = (int) (h / ratio);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            options.inSampleSize = 1;

            //bmp = BitmapFactory.decodeFile(srcImgPath, options);
            if (ratio > 1.0f) {
                bmp = ImageUtils.zoomBitmap(bmp, newW, newH);
            }

            return dstImgPath;
        }

        @Override
        protected void onPostExecute(String thumbFullPath) {
            mCivCropperPreview.setImageBitmap(bmp);
        }
    }
}
