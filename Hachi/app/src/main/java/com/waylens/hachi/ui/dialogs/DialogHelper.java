package com.waylens.hachi.ui.dialogs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobHelper;
import com.waylens.hachi.bgjob.download.DownloadHelper;

import java.io.File;

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

    public static MaterialDialog showReportUserDialog(Context context, MaterialDialog.SingleButtonCallback positiveListener) {
        return new MaterialDialog.Builder(context)
            .title(R.string.report)
            .titleColorRes(R.color.style_color_accent)
            .iconRes(R.drawable.ic_report)
            .items(R.array.report_reason)
            .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                @Override
                public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                    return true;
                }
            })
            .positiveText(R.string.report)
            .negativeText(R.string.cancel)
            .onPositive(positiveListener)
            .show();
    }

    public static void showReportMomentDialog(final Context context, final long momentId) {
        new MaterialDialog.Builder(context)
            .title(R.string.report)
            .titleColorRes(R.color.style_color_accent)
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

    public static void showUnfollowConfirmDialog(final Context context, final String userName,
                                                 final String userId, final boolean isFollow,
                                                 final onPositiveClickListener listener) {
        new MaterialDialog.Builder(context)
            .content(context.getResources().getString(R.string.unfollow) + " " + userName)
            .positiveText(R.string.ok)
            .negativeText(R.string.cancel)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    BgJobHelper.followUser(userId, isFollow);
                    listener.onPositiveClick();
                }
            }).show();
    }


    public static MaterialDialog showDeleteHighlightConfirmDialog(Context context,
                                                                  MaterialDialog.SingleButtonCallback positiveListener) {
        return new MaterialDialog.Builder(context)
            .title(R.string.delete_highlight)
            .titleColorRes(R.color.style_color_accent)
            .content(R.string.delete_bookmark_confirm)
            .negativeText(R.string.cancel)
            .positiveText(R.string.ok)
            .onPositive(positiveListener)
            .theme(Theme.DARK)
            .show();
    }


    public static MaterialDialog showDeleteFileConfirmDialog(Context context, File file, MaterialDialog.SingleButtonCallback positiveListener) {
        return new MaterialDialog.Builder(context)
            .title(R.string.delete_file)
            .titleColorRes(R.color.style_color_accent)
            .content(context.getString(R.string.delete_file_confirm, file.getName()))
            .positiveText(R.string.delete)
            .negativeText(R.string.cancel)
            .onPositive(positiveListener)
            .show();
    }


    public static MaterialDialog showLeaveEnhanceConfirmDialog(Context context, MaterialDialog.SingleButtonCallback positiveListener) {
        return new MaterialDialog.Builder(context)
            .title(R.string.enhance_leave)
            .titleColorRes(R.color.style_color_accent)
            .content(R.string.discard_enhance_confirm)
            .positiveText(R.string.ok)
            .negativeText(R.string.cancel)
            .onPositive(positiveListener)
            .show();
    }



    public interface onPositiveClickListener {
        void onPositiveClick();
    }
}
