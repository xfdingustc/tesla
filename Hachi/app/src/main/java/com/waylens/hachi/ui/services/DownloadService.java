package com.waylens.hachi.ui.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.orhanobut.logger.Logger;
import com.transee.vdb.HttpRemuxer;
import com.transee.vdb.Mp4Info;
import com.transee.vdb.RemuxHelper;
import com.transee.vdb.RemuxerParams;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.ui.activities.CameraVideoActivity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class DownloadService extends Service {
    private static final String TAG = DownloadService.class.getSimpleName();


    // current downloading state
    public static final int DOWNLOAD_STATE_IDLE = 0;
    public static final int DOWNLOAD_STATE_RUNNING = 1;
    public static final int DOWNLOAD_STATE_FINISHED = 2; // TODO
    public static final int DOWNLOAD_STATE_ERROR = 3; // TODO

    // reasons for broadcasting
    public static final int REASON_NONE = 0; // not used
    public static final int REASON_ITEM_ADDED = 1; // a downloading item is added
    public static final int REASON_ITEM_PROGRESS = 2; //
    public static final int REASON_DOWNLOAD_ERROR = 3; // error downloading an item
    public static final int REASON_DOWNLOAD_STARTED = 4; // downloading item started
    public static final int REASON_DOWNLOAD_FINISHED = 5; // downloading item finished

    public interface Callback {
        void onStateChangedAsync(DownloadService service, int reason, int state, Item item, int progress);
    }

    // items in download list or downloading
    public static class Item {
        public int id; // unique id
        public String outputFile;
        public RemuxerParams params;
    }

    private WorkQueue mWorkQueue;
    private Thread mWorkThread;

    private LocalBinder mBinder = new LocalBinder();
    private ArrayList<Callback> mCallbackList = new ArrayList<Callback>();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends android.os.Binder {
        // API
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

    ;

    public static class DownloadInfo {
        public Item item; // may be null
        public int state;
        public int percent;
        public List<Item> list;
    }

    // API - called by client
    public void getDownloadInfo(DownloadInfo info) {
        mWorkQueue.getDownloadInfo(info);
    }

    // API - called by client
    public void cancelDownload(int id) {
        mWorkQueue.cancelDownload(id);
    }

    // API - called by client
    public void cancelAll() {
        mWorkQueue.cancelAll();
    }

    // API
    synchronized public void addCallback(Callback callback) {
        mCallbackList.add(callback);
    }

    // API
    synchronized public void removeCallback(Callback callback) {
        mCallbackList.remove(callback);
    }

    @Override
    public void onCreate() {
        super.onCreate();


        Logger.t(TAG).d("onCreate");

        mWorkQueue = new WorkQueue();
        mWorkThread = new Thread("DownloadThread") {
            @Override
            public void run() {
                try {
                    threadLoop();
                } catch (InterruptedException e) {
                    Log.d(TAG, "interrupted");
                }
            }
        };

        mWorkThread.start();
    }


    @Override
    public void onStart(Intent intent, int startId) {
        Logger.t(TAG).d("onStart");

        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Item item = new Item();
            item.params = new RemuxerParams(bundle);

            Logger.t(TAG).d("add item to work q: " + item.params.getInputFile());
            mWorkQueue.addItem(item);
        }
    }

    @Override
    public void onDestroy() {
        Logger.t(TAG).d("onDestroy");

        if (mWorkThread != null) {
            mWorkThread.interrupt();
            mWorkThread = null;
        }

        super.onDestroy();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.t(TAG).d("onStartCommand, 0x" + Integer.toHexString(flags) + "," + startId);
        onStart(intent, startId);
        return START_NOT_STICKY;
    }

    // ---------------------------------------------------------------------------------

    private NotificationManager mNotifManager;
    private NotificationCompat.Builder mNotifBuilder;

    private void broadcastInfo(int reason, int state, Item item, int progress, int remain) {
        synchronized (this) {
            for (int i = 0; i < mCallbackList.size(); i++) {
                Callback callback = mCallbackList.get(i);
                callback.onStateChangedAsync(this, reason, state, item, progress);
            }
            if (mNotifBuilder == null) {
                mNotifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotifBuilder = new NotificationCompat.Builder(this);
                // Intent notifIntent = new Intent(new ComponentName(this, CameraVideoActivity.class));
                mNotifBuilder.setContentTitle(getResources().getText(R.string.app_name));
                mNotifBuilder.setSmallIcon(R.drawable.app_icon_small);
                // intent to activate the activity
                Intent intent = new Intent(this, CameraVideoActivity.class);
                Bundle bundle = new Bundle();
                bundle.putBoolean("isLocal", true);
                intent.putExtras(bundle);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
                mNotifBuilder.setContentIntent(pendingIntent);
            }
        }
        switch (state) {
            case DOWNLOAD_STATE_IDLE:
                notifyProgress(state, 0, remain);
                break;
            case DOWNLOAD_STATE_RUNNING:
                notifyProgress(state, progress, remain);
                break;
            case DOWNLOAD_STATE_FINISHED:
                notifyProgress(state, 100, remain);
                break;
            case DOWNLOAD_STATE_ERROR:
                // TODO
                break;
        }
    }

    private void notifyProgress(int state, int percent, int remain) {
        String text;
        if (state == DOWNLOAD_STATE_FINISHED && remain == 0) {
            text = getResources().getString(R.string.lable_download_done);
        } else {
            text = Integer.toString(percent) + "%";
            if (remain > 0) {
                String fmt = getResources().getString(R.string.lable_download_remain);
                text += " " + String.format(Locale.US, fmt, remain);
            }
        }
        mNotifBuilder.setContentText(text);
        mNotifManager.notify(0, mNotifBuilder.build());
    }

    // ---------------------------------------------------------------------------------

    static final int NOTIF_NONE = 0;
    static final int NOTIF_DOWNLOAD_ITEM = 1;
    static final int NOTIF_ERROR_DOWNLOAD = 2;
    static final int NOTIF_FINISH_DOWNLOAD = 3;
    static final int NOTIF_BROADCAST = 4;

    static class Notification {
        int code;

        int reason; // NOTIF_BROADCAST
        int state; //
        int progress; // REASON_PROGRESS
        int remain; // NOTIF_BROADCAST
        Item item;

        void clear() {
            code = NOTIF_NONE;
            reason = REASON_NONE;
            progress = 0;
            item = null;
        }
    }

    void threadLoop() throws InterruptedException {
        Notification notif = new Notification();
        while (true) {
            mWorkQueue.getNotification(notif);
            switch (notif.code) {
                case NOTIF_DOWNLOAD_ITEM:
                    startDownloadItem(notif);
                    break;
                case NOTIF_ERROR_DOWNLOAD:
                    errorDownloadItem(notif);
                    break;
                case NOTIF_FINISH_DOWNLOAD:
                    finishDownloadItem(notif);
                    Hachi.addToMediaStore(this, notif.item.outputFile);
                    break;
                case NOTIF_BROADCAST:
                    broadcastInfo(notif.reason, notif.state, notif.item, notif.progress, notif.remain);
                default:
                    break;
            }
        }
    }

    private void startDownloadItem(Notification notif) {

        Logger.t(TAG).d("start download item " + notif.item.params.getInputFile());

        HttpRemuxer remuxer = new HttpRemuxer(0) {
            @Override
            public void onEventAsync(HttpRemuxer remuxer, int event, int arg1, int arg2) {
                processRemuxerEvent(remuxer, event, arg1, arg2);
            }
        };

        Item item = notif.item;
        int remain = mWorkQueue.initDownload(remuxer, item);
        broadcastInfo(REASON_DOWNLOAD_STARTED, DOWNLOAD_STATE_RUNNING, item, 0, remain);


        Logger.t(TAG).d("run remuxer");

        RemuxerParams params = item.params;
        int clipDate = params.getClipDate();
        long clipTimeMs = params.getClipTimeMs();
        String outputFile = RemuxHelper.genDownloadFileName(clipDate, clipTimeMs);
        if (outputFile == null) {
            Logger.t(TAG).e("Output File is null");
        } else {
            item.outputFile = outputFile;
            remuxer.run(params, outputFile);
            Logger.t(TAG).d("remux is running output file is: " + outputFile);
        }
    }

    private void errorDownloadItem(Notification notif) {
        // TODO - delete the file
        int remain = mWorkQueue.finishDownload();
        // TODO
        broadcastInfo(REASON_DOWNLOAD_ERROR, DOWNLOAD_STATE_ERROR, notif.item, 100, remain);
    }

    private void finishDownloadItem(Notification notif) {
        Item item = notif.item;
        RemuxerParams params = item.params;
        Mp4Info info = new Mp4Info();
        info.clip_date = params.getClipDate();
        info.clip_length_ms = params.getClipLength();
        info.clip_created_date = 0; // TODO
        info.stream_version = 2; // TODO
        info.video_coding = params.getVideoCoding();
        info.video_frame_rate = params.getVideoFrameRate();
        info.video_width = params.getVideoWidth();
        info.video_height = params.getVideoHeight();
        info.audio_coding = params.getAudioCoding();
        info.audio_num_channels = params.getAudioNumChannels();
        info.audio_sampling_freq = params.getAudioSamplingFreq();
        byte[] jpg_data = params.getPosterData();
        byte[] gps_ack_data = params.getGspData();
        byte[] acc_ack_data = params.getAccData();
        byte[] obd_ack_data = params.getObdData();
        info.writeInfo(item.outputFile, jpg_data, gps_ack_data, acc_ack_data, obd_ack_data);
        ///
        int remain = mWorkQueue.finishDownload();
        broadcastInfo(REASON_DOWNLOAD_FINISHED, DOWNLOAD_STATE_FINISHED, item, 100, remain);
    }

    // remuxer callback
    private void processRemuxerEvent(HttpRemuxer remuxer, int event, int arg1, int arg2) {
        switch (event) {
            case HttpRemuxer.EVENT_ERROR:
                mWorkQueue.downloadError(remuxer);
                break;
            case HttpRemuxer.EVENT_FINISHED:
                mWorkQueue.downloadFinished(remuxer);
                break;
            case HttpRemuxer.EVENT_PROGRESS:
                mWorkQueue.downloadProgress(remuxer, arg1);
                break;
            default:
                break;
        }
    }

    // ---------------------------------------------------------------------------------

    // client command
    static class Command {
        public static final int CMD_ADD_ITEM = 0;
        public static final int CMD_CANCEL = 1;
        public static final int CMD_CANCEL_ALL = 2;

        int code;
        int param;
        Item item;

        public Command(int code, int param, Item item) {
            this.code = code;
            this.param = param;
            this.item = item;
        }
    }

    class WorkQueue {

        private LinkedList<Item> mDownloadList = new LinkedList<Item>();
        private LinkedList<Command> mCommandQueue = new LinkedList<Command>();

        private Item mDownloadingItem = null;
        private int mDownloadState = DOWNLOAD_STATE_IDLE;
        private int mDownloadPercent = 0;

        // set by remuxer callback
        private int mDownloadState_remuxer = DOWNLOAD_STATE_IDLE;
        private int mDownloadPercent_remuxer = 0;

        private HttpRemuxer mRemuxer = null;
        private int mLastItemId = 0;

        private boolean mbThreadWaiting = false;

        private final void wakeup() {
            if (mbThreadWaiting) {
                mbThreadWaiting = false;
                notifyAll();
            }
        }

        // called by client
        synchronized public void getDownloadInfo(DownloadInfo info) {
            info.item = mDownloadingItem;
            info.state = mDownloadState;
            info.percent = mDownloadPercent;
            info.list = new ArrayList<Item>();
            for (Item item : mDownloadList) {
                info.list.add(item);
            }
        }

        // called by client
        synchronized final void addItem(Item item) {
            Command cmd = new Command(Command.CMD_ADD_ITEM, 0, item);
            mCommandQueue.addLast(cmd);
            wakeup();
        }

        // called by client
        synchronized public void cancelDownload(int id) {
            Command cmd = new Command(Command.CMD_CANCEL, id, null);
            mCommandQueue.addLast(cmd);
            wakeup();
        }

        // called by client
        synchronized public void cancelAll() {
            Command cmd = new Command(Command.CMD_CANCEL_ALL, 0, null);
            mCommandQueue.addLast(cmd);
            wakeup();
        }

        // called by service thread
        // process internal state changes & return an action to execute on service thread
        synchronized final void getNotification(Notification notif) throws InterruptedException {

            notif.clear();

            while (true) {

                // if have client commands
                if (mCommandQueue.size() > 0) {
                    Command cmd = mCommandQueue.removeFirst();
                    switch (cmd.code) {
                        case Command.CMD_ADD_ITEM:
                            cmd.item.id = mLastItemId++;
                            mDownloadList.addLast(cmd.item);
                            notif.code = NOTIF_BROADCAST;
                            notif.reason = REASON_ITEM_ADDED;
                            notif.state = mDownloadState;
                            notif.progress = mDownloadPercent;
                            notif.remain = mDownloadList.size();
                            notif.item = cmd.item;
                            return;
                        case Command.CMD_CANCEL:
                            // TODO
                            break;
                        case Command.CMD_CANCEL_ALL:
                            // TODO
                            break;
                        default:
                            break;
                    }
                }

                // if download progress changes
                if (mDownloadPercent != mDownloadPercent_remuxer) {
                    mDownloadPercent = mDownloadPercent_remuxer;
                    notif.code = NOTIF_BROADCAST;
                    notif.reason = REASON_ITEM_PROGRESS;
                    notif.state = mDownloadState;
                    notif.progress = mDownloadPercent;
                    notif.remain = mDownloadList.size();
                    notif.item = mDownloadingItem;
                    return;
                }

                // if download state changed
                if (mDownloadState != mDownloadState_remuxer) {
                    mDownloadState = mDownloadState_remuxer;
                    switch (mDownloadState) {
                        case DOWNLOAD_STATE_FINISHED:
                            notif.code = NOTIF_FINISH_DOWNLOAD;
                            notif.item = mDownloadingItem;
                            return;
                        case DOWNLOAD_STATE_ERROR:
                            notif.code = NOTIF_ERROR_DOWNLOAD;
                            notif.item = mDownloadingItem;
                            return;
                        default:
                            break;
                    }
                }

                // if not downloading, check if there's a downloading request in the Q
                if (mDownloadingItem == null) {
                    // can only download one file at a time
                    if (mDownloadList.size() > 0) {
                        notif.code = NOTIF_DOWNLOAD_ITEM;
                        notif.item = mDownloadList.removeFirst();
                        mDownloadingItem = notif.item;
                        return;
                    }
                }

                // none of above; wait
                mbThreadWaiting = true;
                wait();
            }
        }

        // called by service thread
        synchronized private int initDownload(HttpRemuxer remuxer, Item item) {
            mRemuxer = remuxer;
            mDownloadingItem = item;
            mDownloadState = DOWNLOAD_STATE_RUNNING;
            mDownloadState_remuxer = DOWNLOAD_STATE_RUNNING;
            mDownloadPercent = 0;
            mDownloadPercent_remuxer = 0;
            return mDownloadList.size();
        }

        // called by service thread
        synchronized private int finishDownload() {
            mRemuxer = null;
            mDownloadingItem = null;
            mDownloadState = DOWNLOAD_STATE_IDLE;
            mDownloadState_remuxer = DOWNLOAD_STATE_IDLE;
            mDownloadPercent = 0;
            mDownloadPercent_remuxer = 0;
            return mDownloadList.size();
        }

        // called by remuxer callback
        synchronized final void downloadFinished(HttpRemuxer remuxer) {
            if (remuxer == mRemuxer) {
                mDownloadState_remuxer = DOWNLOAD_STATE_FINISHED;
                wakeup();
            }
        }

        // called by remuxer callback
        synchronized final void downloadError(HttpRemuxer remuxer) {
            if (remuxer == mRemuxer) {
                mDownloadState_remuxer = DOWNLOAD_STATE_ERROR;
                wakeup();
            }
        }

        // called by remuxer callback
        synchronized final void downloadProgress(HttpRemuxer remuxer, int percent) {
            if (remuxer == mRemuxer) {
                mDownloadPercent_remuxer = percent;
                wakeup();
            }
        }
    }

}
