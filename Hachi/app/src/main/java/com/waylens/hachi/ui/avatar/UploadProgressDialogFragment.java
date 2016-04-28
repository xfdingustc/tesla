package com.waylens.hachi.ui.avatar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.views.CircularProgressView;
import com.waylens.hachi.upload.event.UploadEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import fr.tvbarthel.lib.blurdialogfragment.BlurDialogFragment;

/**
 * Created by Xiaofei on 2016/4/28.
 */
public class UploadProgressDialogFragment extends BlurDialogFragment {


    /**
     * Bundle key used to start the blur dialog with a given scale factor (float).
     */
    private static final String BUNDLE_KEY_DOWN_SCALE_FACTOR = "bundle_key_down_scale_factor";

    /**
     * Bundle key used to start the blur dialog with a given blur radius (int).
     */
    private static final String BUNDLE_KEY_BLUR_RADIUS = "bundle_key_blur_radius";

    /**
     * Bundle key used to start the blur dialog with a given dimming effect policy.
     */
    private static final String BUNDLE_KEY_DIMMING = "bundle_key_dimming_effect";

    /**
     * Bundle key used to start the blur dialog with a given debug policy.
     */
    private static final String BUNDLE_KEY_DEBUG = "bundle_key_debug_effect";

    /**
     * Bundle key used for blur effect on action bar policy.
     */
    private static final String BUNDLE_KEY_BLURRED_ACTION_BAR = "bundle_key_blurred_action_bar";

    /**
     * Bundle key used for RenderScript
     */
    private static final String BUNDLE_KEY_USE_RENDERSCRIPT = "bundle_key_use_renderscript";

    private int mRadius;
    private float mDownScaleFactor;
    private boolean mDimming;
    private boolean mDebug;
    private boolean mBlurredActionBar;
    private boolean mUseRenderScript;

    private CircularProgressView mProgressView;

    private EventBus mEventBus = EventBus.getDefault();

    /**
     * Retrieve a new instance of the sample fragment.
     *
     * @param radius            blur radius.
     * @param downScaleFactor   down scale factor.
     * @param dimming           dimming effect.
     * @param debug             debug policy.
     * @param mBlurredActionBar blur affect on actionBar policy.
     * @param useRenderScript   use of RenderScript
     * @return well instantiated fragment.
     */
    public static UploadProgressDialogFragment newInstance(int radius,
                                                           float downScaleFactor,
                                                           boolean dimming,
                                                           boolean debug,
                                                           boolean mBlurredActionBar,
                                                           boolean useRenderScript) {
        UploadProgressDialogFragment fragment = new UploadProgressDialogFragment();
        Bundle args = new Bundle();
        args.putInt(BUNDLE_KEY_BLUR_RADIUS, radius);
        args.putFloat(BUNDLE_KEY_DOWN_SCALE_FACTOR, downScaleFactor);
        args.putBoolean(BUNDLE_KEY_DIMMING, dimming);
        args.putBoolean(BUNDLE_KEY_DEBUG, debug);
        args.putBoolean(BUNDLE_KEY_BLURRED_ACTION_BAR, mBlurredActionBar);
        args.putBoolean(BUNDLE_KEY_USE_RENDERSCRIPT, useRenderScript);

        fragment.setArguments(args);

        return fragment;
    }

    @Subscribe
    public void onEventUpload(UploadEvent event) {
        switch (event.getWhat()) {
            case UploadEvent.UPLOAD_WHAT_PROGRESS:
                mProgressView.setCurrentProgress(event.getExtra());
                break;
            case UploadEvent.UPLOAD_WHAT_FINISHED:
                dismiss();
                getActivity().finish();
                break;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Bundle args = getArguments();
        mRadius = args.getInt(BUNDLE_KEY_BLUR_RADIUS);
        mDownScaleFactor = args.getFloat(BUNDLE_KEY_DOWN_SCALE_FACTOR);
        mDimming = args.getBoolean(BUNDLE_KEY_DIMMING);
        mDebug = args.getBoolean(BUNDLE_KEY_DEBUG);
        mBlurredActionBar = args.getBoolean(BUNDLE_KEY_BLURRED_ACTION_BAR);
        mUseRenderScript = args.getBoolean(BUNDLE_KEY_USE_RENDERSCRIPT);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.upload_dialog_fragment, null);
        mProgressView = ((CircularProgressView) view.findViewById(R.id.progressView));

        builder.setView(view);


        return builder.create();
    }


    @Override
    public void onStart() {
        super.onStart();
        Log.i("ddd", "ddd");
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mEventBus.register(this);
    }


    @Override
    public void onStop() {
        super.onStop();
        mEventBus.unregister(this);
    }

    @Override
    protected boolean isDebugEnable() {
        return mDebug;
    }

    @Override
    protected boolean isDimmingEnable() {
        return mDimming;
    }

    @Override
    protected boolean isActionBarBlurred() {
        return mBlurredActionBar;
    }

    @Override
    protected float getDownScaleFactor() {
        return mDownScaleFactor;
    }

    @Override
    protected int getBlurRadius() {
        return mRadius;
    }

    @Override
    protected boolean isRenderScriptEnable() {
        return mUseRenderScript;
    }
}
