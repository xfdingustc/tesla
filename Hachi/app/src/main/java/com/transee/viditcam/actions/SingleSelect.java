package com.transee.viditcam.actions;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.waylens.hachi.R;

import java.util.ArrayList;

abstract public class SingleSelect extends DialogBuilder {

	abstract protected void onSelectItem(int id);

	protected ArrayList<CharSequence> mItems;
	protected ArrayList<Integer> mIDs;
	protected int mSelId = -1;
	protected boolean mbNoAutoDismiss;

	public SingleSelect(Activity activity) {
		super(activity);
		mItems = new ArrayList<CharSequence>();
		mIDs = new ArrayList<Integer>();
	}

	public void addItem(CharSequence item, int id) {
		mItems.add(item);
		mIDs.add(id);
	}

	public void setSelId(int selId) {
		mSelId = selId;
	}

	@Override
	public void show() {
		setContent(R.layout.dialog_single_select);
		super.show();
	}

	private View.OnClickListener mOnClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			int index = v.getId() - 1;
			if (index >= 0 && index < mIDs.size()) {
				int id = mIDs.get(index);
				if (id != mSelId) {
					onSelectItem(id);
				}
			}
			if (!mbNoAutoDismiss) {
				dismiss();
			}
		}
	};

	@Override
	protected void onDialogCreated(BaseDialog dialog, View layout) {
		dialog.requestNoPadding();
		RadioGroup group = (RadioGroup)layout.findViewById(R.id.radioGroup1);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		LayoutInflater inflater = LayoutInflater.from(dialog.getContext());
		for (int i = 0; i < mItems.size(); i++) {
			RadioButton radio = (RadioButton)inflater.inflate(R.layout.item_single_sel, group, false);
			radio.setText(mItems.get(i));
			radio.setId(i + 1);
			radio.setOnClickListener(mOnClick);
			group.addView(radio, params);
			if (mIDs.get(i) == mSelId) {
				radio.setChecked(true);
			}
		}
	}

}
