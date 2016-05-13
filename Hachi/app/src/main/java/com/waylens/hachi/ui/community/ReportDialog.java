package com.waylens.hachi.ui.community;

import android.content.Context;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.waylens.hachi.R;

/**
 * Created by Xiaofei on 2016/5/13.
 */
public class ReportDialog extends MaterialDialog {
    protected ReportDialog(Builder builder) {
        super(builder);
    }

    public static MaterialDialog newInstance(Context context) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context)
            .title(R.string.report)
            .items(R.array.gender_list)
            .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                @Override
                public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                    return true;
                }
            })
            .positiveText(R.string.report)
            .negativeText(android.R.string.cancel);
        return builder.build();

    }

}
