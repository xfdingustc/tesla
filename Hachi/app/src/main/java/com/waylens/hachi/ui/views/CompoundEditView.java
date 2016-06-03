package com.waylens.hachi.ui.views;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;

import com.waylens.hachi.R;
import com.waylens.hachi.utils.ViewUtils;

import java.util.ArrayList;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Richard on 3/25/16.
 */
public class CompoundEditView extends FrameLayout {

    private static final int TYPE_EMAIL = 0;
    private static final int TYPE_PASSWORD = 1;
    private static final int TYPE_CODE = 2;

    int mInputType;
    String mInputHint;
    Pattern mValidPattern;
    String mInvalidMsg;

    ArrayAdapter<String> mAccountAdapter;
    PasswordTransformationMethod mTransformationMethod;

    int mControlsBottomMargin;

    @BindView(R.id.cev_text_input)
    TextInputLayout mTextInputLayout;

    @BindView(R.id.cev_edit_text)
    TextInputEditText mInputText;

    @BindView(R.id.cev_input_controls)
    View mControlsContainer;

    @BindView(R.id.cev_btn_show_password)
    View mShowPasswordControl;

    @BindView(R.id.cev_btn_clear_text)
    View mClearTextControl;

    @OnClick(R.id.cev_btn_clear_text)
    public void onCevBtnClearTextClicked() {
        mInputText.getText().clear();
    }

    @OnClick(R.id.cev_btn_show_password)
    public void onCevBtnShowPasswordClicked(View view) {
        view.setSelected(!view.isSelected());
        mInputText.setTransformationMethod(view.isSelected() ? null : mTransformationMethod);
    }



    public CompoundEditView(Context context) {
        this(context, null, 0);
    }

    public CompoundEditView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CompoundEditView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        readAttrs(context, attrs, defStyleAttr);
        initViews(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CompoundEditView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        readAttrs(context, attrs, defStyleAttr);
        initViews(context);
    }

    private void readAttrs(final Context context, final AttributeSet attrs, final int defStyle) {
        final TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.CompoundEditView, defStyle, 0);
        mInputType = attributes.getInt(R.styleable.CompoundEditView_cevType, TYPE_EMAIL);
        mInputHint = attributes.getString(R.styleable.CompoundEditView_cevHint);
        String pattern = attributes.getString(R.styleable.CompoundEditView_cevValidPattern);
        if (!TextUtils.isEmpty(pattern)) {
            mValidPattern = Pattern.compile(pattern);
        } else if (mInputType == TYPE_EMAIL) {
            mValidPattern = Patterns.EMAIL_ADDRESS;
        }
        mInvalidMsg = attributes.getString(R.styleable.CompoundEditView_cevInvalidFormatMessage);
        if (TextUtils.isEmpty(mInvalidMsg)) {
            mInvalidMsg = "Invalid Input";
        }
        attributes.recycle();
    }

    private void initViews(Context context) {
        View.inflate(context, R.layout.layout_compound_edit_text, this);
        ButterKnife.bind(this);

        if (isInEditMode()) {
            return;
        }

        mControlsBottomMargin = ViewUtils.dp2px(12);
        if (mInputHint != null) {
            mTextInputLayout.setHint(mInputHint);
        }
        initConfigure();
        if (mInputType == TYPE_EMAIL) {
            initAccountsView();
        }
        mInputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mTextInputLayout.setError(null);
                mClearTextControl.setVisibility(TextUtils.isEmpty(s) ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mInputText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (mInputText == null || hasFocus) {
                    return;
                }
                isValid();
            }
        });


    }

    private void initConfigure() {
        int visibility = mInputType == TYPE_EMAIL ? GONE : VISIBLE;
        mClearTextControl.setVisibility(GONE);
        mShowPasswordControl.setVisibility(visibility);
        switch (mInputType) {
            case TYPE_EMAIL:
                mInputText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                break;
            case TYPE_PASSWORD:
                mInputText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                mTransformationMethod = new PasswordTransformationMethod();
                mInputText.setTransformationMethod(mTransformationMethod);
                break;
            case TYPE_CODE:
                mInputText.setInputType(InputType.TYPE_CLASS_NUMBER);
                break;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
//        int b = mInputText.getBottom();
//        int h = mControlsContainer.getHeight();
//        MarginLayoutParams layoutParams = (MarginLayoutParams) mControlsContainer.getLayoutParams();
//        layoutParams.topMargin = b - h - mControlsBottomMargin;
//        mControlsContainer.setLayoutParams(layoutParams);
    }

    public Editable getText() {
        return mInputText.getText();
    }

    public void setText(String text) {
        mInputText.setText(text);
    }

    void getAccounts() {
        /*
        mAccountAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, selectedAccounts);
        mTvSignUpEmail.setAdapter(mAccountAdapter);
        if (mAccountAdapter.getCount() > 0) {
            mTvSignUpEmail.setText(mAccountAdapter.getItem(0));
        }
        */
    }


    public boolean isValid() {
        String text = mInputText.getText().toString();
        if (mValidPattern != null) {
            if (!mValidPattern.matcher(text).matches()) {
                mTextInputLayout.setError(mInvalidMsg);
                return false;
            }
        }
        mTextInputLayout.setError(null);
        return true;
    }

    private void initAccountsView() {
        if (isInEditMode()) {
            return;
        }
        AccountManager accountManager = AccountManager.get(getContext());
        Account[] accounts = accountManager.getAccounts();
        ArrayList<String> selectedAccounts = new ArrayList<>();
        for (Account account : accounts) {
            if (account.name.contains("@")) {
                selectedAccounts.add(account.name);
            }
        }
        mAccountAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, selectedAccounts);
//        mInputText.setAdapter(mAccountAdapter);
        if (mAccountAdapter.getCount() > 0) {
            mInputText.setText(mAccountAdapter.getItem(0));
        }
    }

    public void setError(String error) {
        mTextInputLayout.setError(error);
    }
}
