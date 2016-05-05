package com.waylens.hachi.ui.clips;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.adapters.IconSpinnerAdapter;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.helpers.MomentShareHelper;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.OnClick;


public class ShareFragment extends BaseFragment implements MomentShareHelper.OnShareMomentListener {
    private static final String TAG = ShareFragment.class.getSimpleName();
    private static final int PLAYLIST_SHARE = 0x100;

    private final int mClipSetIndex = ClipSetManager.CLIP_SET_TYPE_SHARE;

    private boolean mIsFacebookShareChecked = false;


    @BindView(R.id.spinner_social_privacy)
    Spinner mPrivacySpinner;

    @BindView(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @BindView(R.id.moment_title)
    TextView mTitleView;

    @BindArray(R.array.social_privacy_text)
    CharSequence[] mPrivacyText;

    @BindView(R.id.btn_facebook)
    ImageView mBtnFaceBook;

    @OnClick(R.id.btn_facebook)
    public void onBtnFackBookChecked() {
        mIsFacebookShareChecked = !mIsFacebookShareChecked;
        if (mIsFacebookShareChecked) {
            mBtnFaceBook.setBackgroundResource(R.drawable.btn_platform_facebook_s);
        } else {
            mBtnFaceBook.setBackgroundResource(R.drawable.btn_platform_facebook_n);
        }
    }


    private ClipSet mClipSet = new ClipSet(Clip.TYPE_TEMP);

    private String[] mSupportedPrivacy;

    private String mSocialPrivacy;

    private MomentShareHelper mShareHelper;

    private RequestQueue mRequestQueue;


    private CallbackManager callbackManager = CallbackManager.Factory.create();

    public static ShareFragment newInstance(ArrayList<Clip> clipList) {
        ShareFragment fragment = new ShareFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("cliplist", clipList);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        ArrayList<Clip> clipList = bundle.getParcelableArrayList("cliplist");
        for (Clip clip : clipList) {
            mClipSet.addClip(clip);
        }
        mRequestQueue = Volley.newRequestQueue(getActivity());
        mRequestQueue.start();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createFragmentView(inflater, container, R.layout.fragment_share, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mShareHelper != null) {
            mShareHelper.cancel(true);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void initViews() {
        TypedArray typedArray = getResources().obtainTypedArray(R.array.social_privacy_icon);
        Drawable[] drawables = new Drawable[typedArray.length()];
        for (int i = 0; i < drawables.length; i++) {
            drawables[i] = typedArray.getDrawable(i);
        }
        typedArray.recycle();
        IconSpinnerAdapter mAdapter = new IconSpinnerAdapter(getActivity(), android.R.layout.simple_spinner_item, mPrivacyText, drawables,
            ViewUtils.dp2px(16));
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPrivacySpinner.setAdapter(mAdapter);

        mSupportedPrivacy = getResources().getStringArray(R.array.social_privacy_value);
        mSocialPrivacy = mSupportedPrivacy[0];
        mPrivacySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSocialPrivacy = mSupportedPrivacy[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mSocialPrivacy = mSupportedPrivacy[0];
            }
        });

        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isLinked()) {
            mBtnFaceBook.setVisibility(View.VISIBLE);
        } else {
            mBtnFaceBook.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.btn_share)
    void shareVideo() {
        ClipSetManager manager = ClipSetManager.getManager();
        manager.updateClipSet(ClipSetManager.CLIP_SET_TYPE_SHARE, mClipSet);

        checkPermission();

    }

    private void checkPermission() {
        Logger.t(TAG).d("send check permission");
        String url = Constants.API_SHARE_ACCOUNTS;
        AuthorizedJsonRequest request = new AuthorizedJsonRequest(url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Logger.t(TAG).json(response.toString());
                boolean hasFacebookPermission = hasFacebookPermission(response);
                if (hasFacebookPermission) {
                    doShare();
                } else {
                    requestPublishPermission();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Logger.t(TAG).d("error");
            }
        });
        mRequestQueue.add(request);
    }

    private boolean hasFacebookPermission(JSONObject response) {
        try {
            JSONArray array = response.getJSONArray("linkedAccounts");
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String provider = object.getString("provider");
                if (provider.equals("facebook")) {
                    return true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void doShare() {
        mViewAnimator.setDisplayedChild(1);
        mShareHelper = new MomentShareHelper(getActivity(), mVdbRequestQueue, ShareFragment.this);
        String title = mTitleView.getText().toString();
        String[] tags = new String[]{"Shanghai", "car"};
        Activity activity = getActivity();
        int audioID = EnhanceFragment.DEFAULT_AUDIO_ID;
        JSONObject gaugeSettings = null;
        if (activity instanceof EnhancementActivity) {
            audioID = ((EnhancementActivity) activity).getAudioID();
            gaugeSettings = ((EnhancementActivity) activity).getGaugeSettings();
        }


        mShareHelper.shareMoment(PLAYLIST_SHARE, title, tags, mSocialPrivacy, audioID, gaugeSettings, mIsFacebookShareChecked);
    }

    @Override
    public void onShareSuccessful(LocalMoment localMoment) {
        Logger.t(TAG).e("onShareSuccessful");
        mViewAnimator.setDisplayedChild(2);
    }

    @Override
    public void onCancelShare() {
        Logger.t(TAG).e("onCancelShare");
        mViewAnimator.setDisplayedChild(0);
    }

    @Override
    public void onShareError(int errorCode, int errorResId) {
        Logger.t(TAG).e("onShareError:" + errorCode);
        mViewAnimator.setDisplayedChild(3);
    }

    @Override
    public void onUploadProgress(int uploadPercentage) {
        //Logger.t(TAG).e("onUploadProgress: "+ uploadPercentage);
    }


    private void requestPublishPermission() {
        Logger.t(TAG).d("request publish permission");
        LoginManager loginManager = LoginManager.getInstance();


        loginManager.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {

            @Override
            public void onSuccess(LoginResult loginResult) {
                Logger.t(TAG).d("on sucess!!!");
                sendShareTokenToServer(loginResult.getAccessToken().getToken());

            }

            @Override
            public void onCancel() {
                Logger.t(TAG).d("on cancel");
            }

            @Override
            public void onError(FacebookException error) {
                Logger.t(TAG).d("on error");
            }
        });
        loginManager.logInWithPublishPermissions(this, Arrays.asList("publish_actions"));



    }

    private void sendShareTokenToServer(String token) {
        String url = Constants.API_SHARE_ACCOUNTS;
        Map<String, String> param = new HashMap<>();
        param.put("provider", "facebook");
        param.put("accessToken", token);

        final JSONObject requestBody = new JSONObject(param);

        AuthorizedJsonRequest request = new AuthorizedJsonRequest(Request.Method.POST, url, requestBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Logger.t(TAG).json(response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        mRequestQueue.add(request);
    }
}
