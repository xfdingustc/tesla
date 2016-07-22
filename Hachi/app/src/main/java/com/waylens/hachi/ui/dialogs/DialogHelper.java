package com.waylens.hachi.ui.dialogs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobHelper;

/**
 * Created by Xiaofei on 2016/7/22.
 */
public class DialogHelper {

    public static void showDeleteMomentConfirmDialog(Context context, final long momentId, final onPositiveClickListener listener) {
        new MaterialDialog.Builder(context)
            .content(R.string.delete_this_video)
            .positiveText(R.string.ok)
            .negativeText(R.string.cancel)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    listener.onPositiveClick();
                    BgJobHelper.deleteMoment(momentId);
                }
            }).show();
    }

    public static void showReportMomentDialog(final Context context, final long momentId) {
        new MaterialDialog.Builder(context)
            .title(R.string.report)
            .items(R.array.report_reason)
            .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                @Override
                public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                    return true;
                }
            })
            .positiveText(R.string.report)
            .negativeText(R.string.cancel)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    int index = dialog.getSelectedIndex();
                    String reportReason = context.getResources().getStringArray(R.array.report_reason)[index];
                    BgJobHelper.reportMoment(momentId, reportReason);
                }
            })
            .show();
    }

    public interface onPositiveClickListener {
        void onPositiveClick();
    }
}
