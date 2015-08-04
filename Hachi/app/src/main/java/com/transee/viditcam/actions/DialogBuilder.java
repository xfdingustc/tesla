package com.transee.viditcam.actions;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.waylens.hachi.R;


// TODO - ok/yes at left, cancel/no at right

public class DialogBuilder {

	static final boolean LEFT_IS_POSITIVE = true;

	// dialog button set
	public static final int DLG_NO_BUTTONS = 0; // show no buttons
	public static final int DLG_YES_NO = 1; // show Yes & No
	public static final int DLG_OK_CANCEL = 2; // show OK & Cancel
	public static final int DLG_OK = 3; // show OK
	public static final int DLG_CANCEL = 4; // show Cancel

	// individual button
	public static final int BUTTON_POSITIVE = 0;
	public static final int BUTTON_NEGATIVE = 1;
	public static final int BUTTON_NEUTRAL = 2;

	static class Control {
		boolean mbShow;
		int mDrawable;
		int mResId;
		CharSequence mTitleString;

		void setTitle(int resId) {
			mResId = resId;
			mbShow = true;
		}

		void setTitle(CharSequence titleString) {
			mTitleString = titleString;
			mbShow = true;
		}

		void setDrawable(int resId) {
			mDrawable = resId;
			mbShow = true;
		}

		void setupControl(TextView view) {
			if (mbShow) {
				if (mTitleString != null) {
					view.setText(mTitleString);
				} else {
					view.setText(mResId);
				}
				if (mDrawable != 0) {
					view.setCompoundDrawablesWithIntrinsicBounds(mDrawable, 0, 0, 0);
				}
			} else {
				view.setVisibility(View.GONE);
			}
		}
	}

	static class Param {
		// title
		Control mTitle = new Control();
		// content
		int mContentResId;
		Control mMsg = new Control();
		// buttons
		int mButtons;
		Control mNegativeButton = new Control();
		Control mNeutralButton = new Control();
		Control mPositiveButton = new Control();
	}

	protected final Context mContext;
	protected final Param mParam = new Param();
	protected BaseDialog mDialog;

	// inherit
	protected void onDialogCreated(BaseDialog dialog, View layout) {
	}

	// inherit
	protected boolean onButtonClicked(int id) {
		return false;
	}

	// inherit
	protected void onClickPositiveButton() {
	}

	// inherit
	protected void onClickNegativeButton() {
	}

	// inherit
	protected void onClickNeutralButton() {
	}

	// inherit
	protected void onShow() {
	}

	// inherit
	protected void onDismiss() {
	}

	public DialogBuilder(Context context) {
		mContext = context;
	}

	// API
	public Context getContext() {
		return mContext;
	}

	// API
	public void setTitle(int resId) {
		mParam.mTitle.setTitle(resId);
	}

	// API
	public void setTitle(CharSequence titleString) {
		mParam.mTitle.setTitle(titleString);
	}

	// API
	public void setTitleDrawawble(int resId) {
		mParam.mTitle.setDrawable(resId);
	}

	// API
	public void setContent(int contentResId) {
		mParam.mContentResId = contentResId;
	}

	// API
	public void setMsg(int resId) {
		mParam.mMsg.setTitle(resId);
	}

	// API
	public void setMsg(CharSequence titleString) {
		mParam.mMsg.setTitle(titleString);
	}

	// API
	public void setButtons(int buttons) {
		switch (buttons) {
		default:
		case DLG_NO_BUTTONS:
			break;
		case DLG_YES_NO:
			mParam.mPositiveButton.setTitle(android.R.string.yes);
			mParam.mNegativeButton.setTitle(android.R.string.no);
			break;
		case DLG_OK_CANCEL:
			mParam.mPositiveButton.setTitle(android.R.string.ok);
			mParam.mNegativeButton.setTitle(android.R.string.cancel);
			break;
		case DLG_OK:
			mParam.mPositiveButton.setTitle(android.R.string.ok);
			break;
		case DLG_CANCEL:
			mParam.mNegativeButton.setTitle(android.R.string.cancel);
			break;
		}
		mParam.mButtons = buttons;
	}

	// API
	public void setNegativeButton(int resId) {
		mParam.mNegativeButton.setTitle(resId);
	}

	// API
	public void setNegativeButton(CharSequence titleString) {
		mParam.mNegativeButton.setTitle(titleString);
	}

	// API
	public void setNeutralButton(int resId) {
		mParam.mNeutralButton.setTitle(resId);
	}

	// API
	public void setNeutralButton(CharSequence titleString) {
		mParam.mNeutralButton.setTitle(titleString);
	}

	// API
	public void setPositiveButton(int resId) {
		mParam.mPositiveButton.setTitle(resId);
	}

	// API
	public void setPositiveButton(CharSequence titleString) {
		mParam.mPositiveButton.setTitle(titleString);
	}

	// API
	public void show() {
		mDialog = new BaseDialog(mContext);
		mDialog.initDialog(mParam);
		mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				DialogBuilder.this.onDismiss();
			}
		});
		onShow();
		mDialog.show();
	}

	// API
	public void dismiss() {
		if (mDialog != null) {
			mDialog.dismiss();
		}
	}

	public class BaseDialog extends Dialog {

		private View mLayout;

		public BaseDialog(Context context) {
			super(context, R.style.BaseDialog);
		}

		@SuppressLint("InflateParams")
		private void initDialog(Param param) {
			Window window = getWindow();
			window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
			window.setWindowAnimations(android.R.style.Animation_Dialog);
			// getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

			LayoutInflater inflater = LayoutInflater.from(getContext());
			mLayout = inflater.inflate(R.layout.dialog_base, null);

			showTitle(mLayout, param);
			showContent(mLayout, param, inflater);
			showButtons(mLayout, param);

			onDialogCreated(mDialog, mDialog.mLayout);

			setContentView(mLayout);
		}

		// API
		public void requestNoPadding() {
			View contentRoot = mLayout.findViewById(R.id.dialogContent);
			contentRoot.setPadding(0, 0, 0, 0);
		}

		private void showTitle(View layout, Param param) {
			TextView title = (TextView)layout.findViewById(R.id.dialogTitle);
			View titleLine = layout.findViewById(R.id.lineTitle);
			param.mTitle.setupControl(title);
			if (!param.mTitle.mbShow) {
				titleLine.setVisibility(View.GONE);
			} else {
				layout.findViewById(R.id.decorLine).setVisibility(View.GONE);
			}
		}

		private void showContent(View layout, Param param, LayoutInflater inflater) {
			TextView textMsg = (TextView)layout.findViewById(R.id.textMsg);
			if (param.mContentResId != 0) {
				ViewGroup contentRoot = (ViewGroup)mLayout.findViewById(R.id.dialogContent);
				inflater.inflate(param.mContentResId, contentRoot);
				textMsg.setVisibility(View.GONE);
			} else {
				param.mMsg.setupControl(textMsg);
			}
		}

		private void showButtons(View layout, Param param) {
			// buttons
			Button buttonLeft = (Button)layout.findViewById(R.id.dlgLeftButton);
			buttonLeft.setOnClickListener(mOnClickButton);
			View lineLeft = layout.findViewById(R.id.lineLeft);

			Button buttonMiddle = (Button)layout.findViewById(R.id.dlgMiddleButton);
			buttonMiddle.setOnClickListener(mOnClickButton);
			View lineRight = layout.findViewById(R.id.lineRight);

			Button buttonRight = (Button)layout.findViewById(R.id.dlgRightButton);
			buttonRight.setOnClickListener(mOnClickButton);

			Control ctrlLeft = LEFT_IS_POSITIVE ? mParam.mPositiveButton : mParam.mNegativeButton;
			ctrlLeft.setupControl(buttonLeft);
			if (!ctrlLeft.mbShow) {
				lineLeft.setVisibility(View.GONE);
			}

			mParam.mNeutralButton.setupControl(buttonMiddle);
			if (!mParam.mNeutralButton.mbShow) {
				lineRight.setVisibility(View.GONE);
			} else {
				buttonMiddle.setOnClickListener(mOnClickButton);
			}

			Control ctrlRight = LEFT_IS_POSITIVE ? mParam.mNegativeButton : mParam.mPositiveButton;
			ctrlRight.setupControl(buttonRight);

			if (!param.mNegativeButton.mbShow && !param.mNeutralButton.mbShow && !param.mPositiveButton.mbShow) {
				View lineContent = mLayout.findViewById(R.id.lineContent);
				lineContent.setVisibility(View.GONE);
			}
		}

		private void onClickButton(View v) {
			if (onButtonClicked(v.getId()))
				return;
			switch (v.getId()) {
			case R.id.dlgLeftButton:
				switch (mParam.mButtons) {
				case DLG_YES_NO:
					if (LEFT_IS_POSITIVE) {
						onClickPositiveButton();
					} else {
						onClickNegativeButton();
					}
					break;
				case DLG_OK_CANCEL:
					if (LEFT_IS_POSITIVE) {
						onClickPositiveButton();
					} else {
						onClickNegativeButton();
					}
					break;
				default:
					break;
				}
				break;
			case R.id.dlgMiddleButton:
				onClickNeutralButton();
				break;
			case R.id.dlgRightButton:
				switch (mParam.mButtons) {
				case DLG_YES_NO:
					if (LEFT_IS_POSITIVE) {
						onClickNegativeButton();
					} else {
						onClickPositiveButton();
					}
					break;
				case DLG_OK_CANCEL:
				case DLG_OK:
					if (LEFT_IS_POSITIVE) {
						onClickNegativeButton();
					} else {
						onClickPositiveButton();
					}
					break;
				default:
					break;
				}
				break;
			default:
				break;
			}
			mDialog.dismiss();
		}

		// API
		public Button getButton(int button) {
			if (mLayout == null) {
				return null;
			}
			switch (button) {
			case DialogBuilder.BUTTON_NEGATIVE:
				if (LEFT_IS_POSITIVE) {
					return (Button)mLayout.findViewById(R.id.dlgRightButton);
				} else {
					return (Button)mLayout.findViewById(R.id.dlgLeftButton);
				}
			case DialogBuilder.BUTTON_POSITIVE:
				if (LEFT_IS_POSITIVE) {
					return (Button)mLayout.findViewById(R.id.dlgLeftButton);
				} else {
					return (Button)mLayout.findViewById(R.id.dlgRightButton);
				}
			case DialogBuilder.BUTTON_NEUTRAL:
				return (Button)mLayout.findViewById(R.id.dlgMiddleButton);
			default:
				return null;
			}
		}

		private View.OnClickListener mOnClickButton = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickButton(v);
			}
		};

	}

}
