package com.waylens.hachi.ui.welcome;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.ui.activities.WaylensAgreementActivity;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.manualsetup.StartupActivity;
import com.waylens.hachi.utils.PreferenceUtils;


import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/7/5.
 */
public class Welcome4Fragment extends BaseFragment {
    private static final String TAG = Welcome4Fragment.class.getSimpleName();

    @BindView(R.id.agree_check_box)
    CheckBox mAgreeCheckBox;

    @BindView(R.id.waylens_agreement)
    TextView mWaylensAgreement;

    @BindView(R.id.btnEnter)
    Button mBtnEnter;

    @BindView(R.id.withoutCamera)
    TextView mWithoutCamera;

    @OnClick(R.id.btnEnter)
    public void onBtnEnterClicked() {
        enter();
    }


    @OnClick(R.id.withoutCamera)
    public void OnWithoutCameraClicked() {
        MainActivity.launch(getActivity());
        getActivity().finish();
        writeVersionName();
    }



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_welcome4, savedInstanceState);

        mAgreeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                int visibility = b ? View.VISIBLE : View.INVISIBLE;
                mBtnEnter.setVisibility(visibility);
                mWithoutCamera.setVisibility(visibility);
            }
        });

        SpannableStringBuilder ssb = new SpannableStringBuilder(getString(R.string.agree_line1) + " ");
        int start = ssb.length();
        ssb.append(getString(R.string.agree_line2))
            .setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    WaylensAgreementActivity.launch(getActivity());
                }
            }, start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append(" " + getString(R.string.agree_line3));
        mWaylensAgreement.setText(ssb);
        mWaylensAgreement.setMovementMethod(LinkMovementMethod.getInstance());

        return view;
    }

    @Override
    protected String getRequestTag() {
        return TAG;
    }

    private void enter() {
        Intent intent = new Intent();
        boolean enterSetup = VdtCameraManager.getManager().isConnected();
        if (!enterSetup) {
            intent.setClass(getActivity(), StartupActivity.class);
        } else {
            intent.setClass(getActivity(), MainActivity.class);
        }
        startActivity(intent);
        getActivity().finish();
        writeVersionName();
    }

    private void writeVersionName() {
        PackageInfo pi = null;
        try {
            pi = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            PreferenceUtils.putInt(PreferenceUtils.VERSION_CODE, pi.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


    }
}
