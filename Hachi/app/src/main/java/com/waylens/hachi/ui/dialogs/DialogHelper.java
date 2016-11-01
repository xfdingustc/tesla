package com.waylens.hachi.ui.dialogs;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobHelper;
import com.waylens.hachi.rest.bean.Comment;
import com.waylens.hachi.rest.bean.Firmware;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.settings.FirmwareUpdateActivity;

import java.io.File;

/**
 * Created by Xiaofei on 2016/7/22.
 */
public class DialogHelper {

    private static String mReportReason;

    public static void showDeleteMomentConfirmDialog(Context context, final long momentId, final OnPositiveClickListener listener) {
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
                                                 final OnPositiveClickListener listener) {
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
            .content(R.string.delete_bookmark_confirm)
            .negativeText(R.string.cancel)
            .positiveText(R.string.ok)
            .onPositive(positiveListener)
            .show();
    }


    public static MaterialDialog showDeleteFileConfirmDialog(Context context, File file, MaterialDialog.SingleButtonCallback positiveListener) {
        return new MaterialDialog.Builder(context)
            .title(R.string.delete_file)
            .content(context.getString(R.string.delete_file_confirm, file.getName()))
            .positiveText(R.string.delete)
            .negativeText(R.string.cancel)
            .onPositive(positiveListener)
            .show();
    }


    public static MaterialDialog showLeaveEnhanceConfirmDialog(Context context, MaterialDialog.SingleButtonCallback positiveListener) {
        return new MaterialDialog.Builder(context)
            .title(R.string.enhance_leave)
            .content(R.string.discard_enhance_confirm)
            .positiveText(R.string.ok)
            .negativeText(R.string.cancel)
            .onPositive(positiveListener)
            .show();
    }

    public static MaterialDialog showUploadCacheConfirmDialog(Context context, MaterialDialog.SingleButtonCallback positiveListener,
                                                              MaterialDialog.SingleButtonCallback negativeListener) {
        return new MaterialDialog.Builder(context)
            .title(R.string.upload_moment)
            .content(R.string.upload_in_ap)
            .positiveText(R.string.understand)
            .negativeText(R.string.cancel)
            .onPositive(positiveListener)
            .onNegative(negativeListener)
            .show();
    }


    public static MaterialDialog showUpgradFirmwareConfirmDialog(final Context context, final Firmware firmware,
                                                                 MaterialDialog.SingleButtonCallback negativeListener) {
        return new MaterialDialog.Builder(context)
            .title(R.string.found_new_firmware)
            .content(firmware.description.en)
            .positiveText(R.string.upgrade)
            .negativeText(R.string.cancel)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    FirmwareUpdateActivity.launch((Activity)context, firmware);
                }
            })
            .onNegative(negativeListener).show();

    }

    public static MaterialDialog showReportCommentDialog(final Context context, final Comment comment,
                                                         final OnPositiveClickListener positiveClickListener) {
        mReportReason = context.getResources().getStringArray(R.array.report_reason)[0];
        return new MaterialDialog.Builder(context)
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
                    mReportReason = context.getResources().getStringArray(R.array.report_reason)[index];
                    if (positiveClickListener != null) {
                        positiveClickListener.onPositiveClick();
                    }
                    BgJobHelper.reportComent(comment, mReportReason);
                }
            })
            .show();
    }

    public static MaterialDialog showDeleteCommentConfirmDialog(Context context, final Comment comment, final OnPositiveClickListener positiveClickListener) {
        return new MaterialDialog.Builder(context)
            .content(R.string.delete_your_comment)
            .positiveText(R.string.delete)
            .negativeText(R.string.cancel)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    if (positiveClickListener != null) {
                        positiveClickListener.onPositiveClick();
                    }
                    BgJobHelper.deleteComment(comment);
                }
            }).show();
    }

    public static MaterialDialog showSignoutConfirmDialog(Context context, final OnPositiveClickListener positiveClickListener) {
        return new MaterialDialog.Builder(context)
            .content(R.string.logout_confirm)
            .positiveText(R.string.ok)
            .negativeText(R.string.cancel)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    SessionManager.getInstance().logout();
                    if (positiveClickListener != null) {
                        positiveClickListener.onPositiveClick();
                    }
                }
            }).show();
    }


    public interface OnPositiveClickListener {
        void onPositiveClick();
    }


}
