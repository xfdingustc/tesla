package com.waylens.hachi.gcm;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.ui.activities.NotificationActivity;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

/**
 * Created by lshw on 16/8/2.
 */
public class GcmIntentService extends IntentService {
    public static final String KEY_COMMENT_MOMENT= "@string/comment_moment";

    public static final String KEY_FOLLOW_USER = "@string/follow_user";

    public static final String KEY_LIKE_MOMENT = "@string/like_moment";

    public static final String KEY_REFER_USER = "@string/refer_user";

    public static String TAG = GcmIntentService.class.getSimpleName();

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty() && GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            sendNotification(extras);
            Logger.t(TAG).d("Received: " + extras.toString());
        }
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void sendNotification(Bundle data) {
        Intent intent = new Intent(this, NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);
        Logger.t(TAG).d(data.toString());
        String messageID = data.getString("google.message_id");
        String action = data.getString("gcm.notification.body_loc_key");
        Logger.t(TAG).d(action);
        String stringArray = data.getString("gcm.notification.body_loc_args");
        Logger.t(TAG).d(stringArray);
        String user, moment;
        String msg = null;
        try {
            JSONArray jsonArray = new JSONArray(stringArray);
            switch (action) {
                case KEY_COMMENT_MOMENT:
                    user = jsonArray.optString(0);
                    moment = jsonArray.optString(1);
                    msg = String.format(getResources().getString(R.string.comment_notification), user, moment);
                    break;
                case KEY_LIKE_MOMENT:
                    user =jsonArray.optString(0);
                    moment = jsonArray.optString(1);
                    msg = String.format(getResources().getString(R.string.like_notification), user, moment);
                    break;
                case KEY_FOLLOW_USER:
                    user = jsonArray.optString(0);
                    msg = String.format(getResources().getString(R.string.follow_notification), user);
                    break;
                case KEY_REFER_USER:
                    user = jsonArray.optString(0);
                    moment = jsonArray.optString(1);
                    msg = String.format(getResources().getString(R.string.reply_notification), user, moment);
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Bitmap largeBitmap =  BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_app);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_notification)
                .setColor(getResources().getColor(R.color.material_deep_orange_500))
                .setContentTitle(getString(R.string.app_name))
                .setContentText(msg)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setLargeIcon(largeBitmap)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(messageID, 0, notificationBuilder.build());
    }
}