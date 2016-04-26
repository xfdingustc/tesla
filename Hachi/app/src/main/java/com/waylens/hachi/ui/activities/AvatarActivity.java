package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.GlobalVariables;
import com.waylens.hachi.utils.ImageUtils;


import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

import butterknife.Bind;


/**
 * Created by Xiaofei on 2015/6/17.
 */
public class AvatarActivity extends BaseActivity {
    private final static String TAG = AvatarActivity.class.getSimpleName();
    private final static String PICK_FROM_CAMERA = "pick_from_camera";


    private final static int TAKE_PHOTO = 1;
    private final static int FROM_LOCAL = 2;

    private Uri mAvatarUri;
    private RequestQueue mRequestQueue;

    private String mCroppedImagePath = null;

    private String albumName;
    private String mReturnImagePath = null;

//    @Bind(R.id.civ_cropper_preview)
//    ClipImageView mCivCropperPreview;
//
//    @Bind(R.id.mcpb_upload_progress)
//    MaterialCircularProgressBar mUploadProgressBar;

    public static void start(Activity startActivity, boolean fromCamera) {
        Intent intent = new Intent(startActivity, AvatarActivity.class);
        intent.putExtra(PICK_FROM_CAMERA, fromCamera);
        startActivity.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.gc();
        Intent intent = getIntent();
        boolean fromCamera = intent.getBooleanExtra(PICK_FROM_CAMERA, false);
        if (fromCamera) {
            jump2TakePhoto();
        } else {
//            LocalPhotoActivity.launch(this, albumName, FROM_LOCAL);
        }

        initViews();
    }

    private void jump2TakePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        ContentValues values = new ContentValues(1);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        mAvatarUri = Uri.fromFile(new File(GlobalVariables.getPictureName()));
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mAvatarUri);
        startActivityForResult(intent, TAKE_PHOTO);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }


    private void initViews() {
        setContentView(R.layout.activity_avatar_picker);
        setupToolbar();
    }
    @Override
    public void setupToolbar() {
//        mToolbar.setTitle(getString(R.string.avatar_setting));
        super.setupToolbar();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            finish();
            return;
        }

        if (requestCode == TAKE_PHOTO) {
            Logger.d("Avatar uri: " + mAvatarUri.getPath());
            mReturnImagePath = mAvatarUri.getPath();
        } else if (requestCode == FROM_LOCAL) {
            albumName = data.getStringExtra("albumName");
            mReturnImagePath = data.getStringExtra("photoPath");
        }

//        getToolbar().inflateMenu(R.menu.menu_confirm);
//        new ExtractThumbTask(mReturnImagePath, 1536, 2048).execute();
        super.onActivityResult(requestCode, resultCode, data);
    }




//    private void saveCroppedImage() {
//        Bitmap croppedImage = mCivCropperPreview.clip();
//
//        long now = System.currentTimeMillis();
//        String photoId = String.format("%d.%03d", now / 1000, now % 1000);
//        String fileName = photoId + ".jpg";
//        mCroppedImagePath = mImgCachePath + "/" + fileName;
//        try {
//            FileOutputStream out = new FileOutputStream(mCroppedImagePath);
//            Logger.t(TAG).d("try to compress file : " + mCroppedImagePath);
//            if (croppedImage.compress(Bitmap.CompressFormat.JPEG, 60, out)) {
//                out.flush();
//            } else {
//                ImageUtils.SaveBitmap(croppedImage, mCroppedImagePath);
//                Logger.t(TAG).d("try to save file : " + mCroppedImagePath);
//            }
//            out.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        ImageUtils.SaveBitmap(croppedImage, mCroppedImagePath);
//
//    }
//
//    private void uploadAvatar() {
//        String url = Constant.HOST_URL + Constant.UPLOAD_AVATAR;
//        mRequestQueue.add(new TutuJsonObjectRequest(url, new uploadAvatarResponseListener(),
//            new uploadAvatarErrorListener()));
//
//    }

//    private class uploadAvatarResponseListener implements Response.Listener<JSONObject> {
//
//        private Content mAvatarContent;
//        private JSONObject mResponse;
//
//        @Override
//        public void onResponse(JSONObject response) {
//            mResponse = response;
//            mAvatarContent = new Content();
//            mAvatarContent.setUri(Uri.fromFile(new File(mCroppedImagePath)).toString());
//            mAvatarContent.setType(Constant.TRIP_CONTENT_TYPE_IMAGE);
//            new Thread(new UploadTask()).start();
//        }
//
//        private class UploadTask implements Runnable {
//
//            @Override
//            public void run() {
//                ContentUploader uploader = new ContentUploader(mAvatarContent, mResponse.toString());
//                uploader.setUploaderListener(new uploadAvatarListener());
//                uploader.upload();
//            }
//        }
//    }


    private class uploadAvatarErrorListener implements Response.ErrorListener {
        @Override
        public void onErrorResponse(VolleyError error) {

        }
    }

//    private class uploadAvatarListener implements ContentUploader.UploadListener {
//        private float mProgress;
//
//        @Override
//        public void onUploadStarted() {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    mUploadProgressBar.setVisibility(View.VISIBLE);
//                }
//            });
//        }
//
//        @Override
//        public void onUploadProgress(Content content, float progress) {
//            mProgress = progress / 100;
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    mUploadProgressBar.setProgress(mProgress);
//                }
//            });
//        }
//
//        @Override
//        public void onUploadFinished(Content content) {
//            dismissProgressBar();
//        }
//
//        @Override
//        public void onUploadError(String error) {
//            dismissProgressBar();
//        }
//
//        private void dismissProgressBar() {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    mUploadProgressBar.setVisibility(View.GONE);
//                }
//            });
//            SessionManager.getInstance().refreshUserProfile();
//            finish();
//        }
//    }

//    public class ExtractThumbTask extends AsyncTask<Object, Void, String> {
//        String srcImgPath = null, dstImgPath = null;
//        int reqWidth = 1536, reqHeight = 2048;
//        Bitmap bmp = null;
//
//        public ExtractThumbTask(String srcImgPath, int width, int height) {
//            this.reqWidth = width;
//            this.reqHeight = height;
//            this.srcImgPath = srcImgPath;
//        }
//
//        protected void onPreExecute() {
//
//        }
//
//        @Override
//        protected String doInBackground(Object... params) {
//            long now = System.currentTimeMillis();
//            String photoId = String.format("%d.%03d", now / 1000, now % 1000);
//            String dstFileName = photoId + ".jpg";
//            dstImgPath = mImgCachePath + "/" + dstFileName;
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inJustDecodeBounds = true;
//
//            BitmapFactory.decodeFile(srcImgPath, options);
//            int w = options.outWidth;
//            int h = options.outHeight;
//            int newW = w, newH = h;
//            float ratio = 1;
//            if (w <= h) {
//                ratio = Math.max((float) w / reqWidth, (float) h / reqHeight);
//            } else {
//                ratio = Math.max((float) h / reqWidth, (float) w / reqHeight);
//            }
//
//            if (ratio < 1.0f) {
//                ratio = 1;
//            }
//            newW = (int) (w / ratio);
//            newH = (int) (h / ratio);
//
//            // Decode bitmap with inSampleSize set
//            options.inJustDecodeBounds = false;
//            options.inSampleSize = 1;
//
//            bmp = BitmapFactory.decodeFile(srcImgPath, options);
//            if (ratio > 1.0f) {
//                bmp = ImageUtils.zoomBitmap(bmp, newW, newH);
//            }
//
//            return dstImgPath;
//        }
//
//        @Override
//        protected void onPostExecute(String thumbFullPath) {
//            mCivCropperPreview.setImageBitmap(bmp);
//        }
//    }
}
