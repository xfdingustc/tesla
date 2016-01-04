package com.waylens.hachi.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipUploadUrlRequest;
import com.waylens.hachi.utils.DataUploader;
import com.waylens.hachi.utils.VolleyUtil;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.UploadUrl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import crs_svr.ProtocolConstMsg;

/**
 * Created by Richard on 12/18/15.
 */
public class ShareFragment extends Fragment implements FragmentNavigator {
    @Bind(R.id.video_cover)
    ImageView videoCover;

    @Bind(R.id.social_icons)
    RecyclerView mRecyclerView;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @Bind(R.id.share_success_view)
    ImageView mShareSuccessView;

    @Bind(R.id.error_msg_view)
    TextView mErrorMsgView;

    Clip mClip;
    //long mMinClipStartTimeMs;
    //long mMaxClipEndTimeMs;
    VdbImageLoader mImageLoader;

    CameraVideoPlayFragment mVideoPlayFragment;
    VdbRequestQueue mVdbRequestQueue;
    RequestQueue mRequestQueue;

    UploadUrl mUploadUrlVideo;
    UploadUrl mUploadUrlRaw;
    JSONObject mMomentInfo;

    Handler mHandler;


    public static ShareFragment newInstance(Clip clip) {
        Bundle args = new Bundle();
        ShareFragment fragment = new ShareFragment();
        fragment.setArguments(args);
        fragment.mClip = clip;
        //fragment.mMinClipStartTimeMs = minClipStartTimeMs;
        //fragment.mMaxClipEndTimeMs = maxClipEndTimeMs;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVdbRequestQueue = Snipe.newRequestQueue();
        mImageLoader = VdbImageLoader.getImageLoader(mVdbRequestQueue);
        mRequestQueue = VolleyUtil.newVolleyRequestQueue(getActivity());
        mHandler = new Handler();

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_share, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ClipPos clipPos = new ClipPos(mClip, mClip.getStartTimeMs(), ClipPos.TYPE_POSTER, false);
        mImageLoader.displayVdbImage(clipPos, videoCover);
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 4);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(new IconAdapter());
        mShareSuccessView.setColorFilter(getResources().getColor(R.color.style_color_primary));
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mVideoPlayFragment != null) {
            getFragmentManager().beginTransaction().remove(mVideoPlayFragment).commitAllowingStateLoss();
            mVideoPlayFragment = null;
        }
    }

    @Override
    public void onDestroyView() {
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    @OnClick(R.id.btn_play)
    void playVideo() {
        mVideoPlayFragment = CameraVideoPlayFragment.newInstance(Snipe.newRequestQueue(), mClip, null);
        getFragmentManager().beginTransaction().replace(R.id.enhance_fragment_content, mVideoPlayFragment).commit();
        videoCover.setVisibility(View.INVISIBLE);
    }

    @OnClick(R.id.btn_ok)
    void performShare() {
        mViewAnimator.setDisplayedChild(1);
        getUploadUrlVideo();

    }

    @OnClick(R.id.btn_cancel)
    void onCancel() {
        close();
    }

    void close() {
        getFragmentManager().beginTransaction().remove(this).commit();
    }

    @Override
    public boolean onInterceptBackPressed() {
        if (VideoPlayFragment.fullScreenPlayer != null) {
            VideoPlayFragment.fullScreenPlayer.setFullScreen(false);
            return true;
        }
        close();
        return true;
    }

    static class IconAdapter extends RecyclerView.Adapter<IconVH> {

        int[] drawables = new int[]{
                R.drawable.toggle_waylens,
                R.drawable.toggle_facebook,
                R.drawable.toggle_twitter,
                R.drawable.toggle_youtube,
                R.drawable.toggle_vimeo,
                R.drawable.toggle_pinterest,
                R.drawable.toggle_instagram,
        };

        @Override
        public IconVH onCreateViewHolder(ViewGroup parent, int viewType) {
            CheckBox checkBox = new CheckBox(parent.getContext());
            return new IconVH(checkBox);
        }

        @Override
        public void onBindViewHolder(IconVH holder, int position) {
            holder.mCheckBox.setButtonDrawable(drawables[position]);
            if (position == 0) {
                holder.mCheckBox.setChecked(true);
            } else {
                holder.mCheckBox.setEnabled(false);
            }
        }

        @Override
        public int getItemCount() {
            return drawables.length;
        }
    }

    static class IconVH extends RecyclerView.ViewHolder {
        public CheckBox mCheckBox;

        public IconVH(View itemView) {
            super(itemView);
            mCheckBox = (CheckBox) itemView;
        }
    }

    void getUploadUrlVideo() {
        Bundle parameters = new Bundle();
        parameters.putBoolean(ClipUploadUrlRequest.PARAM_IS_PLAY_LIST, false);
        parameters.putLong(ClipUploadUrlRequest.PARAM_CLIP_TIME_MS, mClip.getStartTimeMs());
        parameters.putInt(ClipUploadUrlRequest.PARAM_CLIP_LENGTH_MS, mClip.getDurationMs());
        parameters.putInt(ClipUploadUrlRequest.PARAM_UPLOAD_OPT, VdbCommand.Factory.UPLOAD_GET_V1);

        ClipUploadUrlRequest request = new ClipUploadUrlRequest(mClip, parameters,
                new VdbResponse.Listener<UploadUrl>() {
                    @Override
                    public void onResponse(UploadUrl response) {
                        mUploadUrlVideo = response;
                        getUploadUrlRaw();
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        mErrorMsgView.setText(R.string.share_video_retrieve_error);
                        mViewAnimator.setDisplayedChild(3);
                        Log.e("test", "", error);
                    }
                });

        mVdbRequestQueue.add(request);
    }

    void createMoment() {
        JSONObject params = new JSONObject();
        try {
            params.put("title", "Moment from Android");
            JSONObject raw = new JSONObject();
            raw.put("guid", mClip.getVdbId());
            JSONArray rawArray = new JSONArray();
            rawArray.put(raw);
            params.put("rawData", rawArray);
            JSONObject fragment = new JSONObject();
            fragment.put("guid", mClip.getVdbId());
            fragment.put("clipCaptureTime", mClip.getDateTimeString());
            fragment.put("beginTime", mClip.getStartTimeMs());
            fragment.put("offset", mUploadUrlVideo.realTimeMs - mClip.getStartTimeMs());
            fragment.put("duration", mUploadUrlVideo.lengthMs);
            JSONArray fragments = new JSONArray();
            fragments.put(fragment);
            params.put("fragments", fragments);
            Log.e("test", "params: " + params);
        } catch (JSONException e) {
            Log.e("test", "", e);
        }

        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.POST, Constants.API_MOMENTS, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e("test", "response: " + response);
                        mMomentInfo = response;
                        uploadData();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mErrorMsgView.setText(R.string.create_moment_error);
                        mViewAnimator.setDisplayedChild(3);
                        Log.e("test", "", error);
                    }
                }));
    }

    void uploadData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean done = uploadData(mUploadUrlVideo.url, "video", ProtocolConstMsg.VIDIT_VIDEO_DATA_LOW);
                if (!done) {
                    return;
                }
                done = uploadData(mUploadUrlRaw.url, "raw", ProtocolConstMsg.VIDIT_RAW_DATA);
                if (done) {
                    updateShareStatus(0, 2);
                }
            }
        }).start();

    }

    boolean uploadData(String urlString, String type, int option) {
        InputStream inputStream = null;
        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            inputStream = conn.getInputStream();
            Log.e("test", String.format("type[%s], ContentLength[%d]", type, conn.getContentLength()));
            JSONObject uploadServer = mMomentInfo.optJSONObject("uploadServer");
            String ip = uploadServer.optString("ip");
            int port = uploadServer.optInt("port");
            String privateKey = uploadServer.optString("privateKey");
            String[] tokenAndGuid = findTokenAndGuid(mMomentInfo, type);
            DataUploader uploader = new DataUploader(ip, port, privateKey);
            uploader.setUploaderListener(uploadListener);
            return uploader.uploadStream(inputStream, conn.getContentLength(), option, tokenAndGuid[0], tokenAndGuid[1]);
        } catch (Exception e) {
            updateShareStatus(R.string.share_upload_error, 3);
            return false;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e("test", "", e);
                }
            }
        }
    }

    String[] findTokenAndGuid(JSONObject momentInfo, String type) {
        JSONArray jsonArray = momentInfo.optJSONArray("uploadData");
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.optJSONObject(i);
            if (type.equals(jsonObject.optString("dataType"))) {
                return new String[]{jsonObject.optString("uploadToken"), jsonObject.optString("guid")};
            }
        }
        return null;
    }

    void getUploadUrlRaw() {
        Bundle parameters = new Bundle();
        parameters.putBoolean(ClipUploadUrlRequest.PARAM_IS_PLAY_LIST, false);
        parameters.putLong(ClipUploadUrlRequest.PARAM_CLIP_TIME_MS, mClip.getStartTimeMs());
        parameters.putInt(ClipUploadUrlRequest.PARAM_CLIP_LENGTH_MS, mClip.getDurationMs());
        parameters.putInt(ClipUploadUrlRequest.PARAM_UPLOAD_OPT, VdbCommand.Factory.UPLOAD_GET_RAW);

        ClipUploadUrlRequest request = new ClipUploadUrlRequest(mClip, parameters,
                new VdbResponse.Listener<UploadUrl>() {
                    @Override
                    public void onResponse(UploadUrl response) {
                        mUploadUrlRaw = response;
                        createMoment();
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        Log.e("test", "", error);
                        mErrorMsgView.setText(R.string.share_raw_retrieve_error);
                        mViewAnimator.setDisplayedChild(3);
                    }
                });

        mVdbRequestQueue.add(request);
    }

    void updateShareStatus(final int textResId, final int childId) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (textResId != 0) {
                    mErrorMsgView.setText(textResId);
                }
                mViewAnimator.setDisplayedChild(childId);
            }
        });
    }

    DataUploader.UploadListener uploadListener = new DataUploader.UploadListener() {
        @Override
        public void onUploadStarted() {
            Log.e("test", "onUploadStarted");
        }

        @Override
        public void onUploadProgress(float progress) {
            Log.e("test", "onUploadProgress: " + progress);
        }

        @Override
        public void onUploadFinished() {
            Log.e("test", "onUploadFinished");
        }

        @Override
        public void onUploadError(String error) {
            Log.e("test", "onUploadError: " + error);
        }
    };

}
