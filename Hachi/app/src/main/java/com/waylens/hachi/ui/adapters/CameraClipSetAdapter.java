package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.transee.vdb.VdbClient;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.VdtCamera;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlRequest;
import com.waylens.hachi.ui.activities.ClipEditActivity;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.PlaybackUrl;

import java.io.IOException;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import butterknife.Bind;
import butterknife.ButterKnife;


public class CameraClipSetAdapter extends RecyclerView.Adapter<CameraClipSetAdapter.CameraClipViewHolder> implements View.OnClickListener {

    private static final String TAG = CameraClipSetAdapter.class.getSimpleName();
    private final VdbRequestQueue mRequestQueue;
    private final VdbImageLoader mVdbImageLoader;
    private final VdtCamera mVdtCamera;
    private Context mContext;

    private ClipSet mClipSet = null;

    ConcurrentHashMap<Integer, MediaPlayer> mMediaPlayers = new ConcurrentHashMap<>();
    HandlerThread mThread;
    Handler mHandler;

    public CameraClipSetAdapter(Context context, VdtCamera vdtCamera, VdbRequestQueue queue) {
        this.mContext = context;
        this.mVdtCamera = vdtCamera;
        this.mRequestQueue = queue;
        this.mVdbImageLoader = new VdbImageLoader(mRequestQueue);
        mThread = new HandlerThread("cleanup");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());

    }

    public void setClipSet(ClipSet clipSet) {
        mClipSet = clipSet;
        notifyDataSetChanged();
    }

    @Override
    public CameraClipViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_camera_video,
                parent, false);
        return new CameraClipViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(CameraClipViewHolder holder, int position) {
        Clip clip = mClipSet.getClip(position);
        holder.videoDesc.setText("Mocked description");
        holder.videoTime.setText(clip.getDateTimeString());
        holder.videoDuration.setText(clip.getDurationString());
        ClipPos clipPos = new ClipPos(clip, clip.getStartTime(), ClipPos.TYPE_POSTER, false);
        mVdbImageLoader.displayVdbImage(clipPos, holder.videoCover);
        holder.mBtnVideoEdit.setOnClickListener(this);
        holder.mBtnVideoEdit.setTag(holder);
        holder.mBtnVideoPlay.setOnClickListener(this);
        holder.mBtnVideoPlay.setTag(holder);
    }

    @Override
    public int getItemCount() {
        if (mClipSet != null) {
            return mClipSet.getCount();
        } else {
            return 0;
        }
    }


    @Override
    public void onClick(View v) {
        CameraClipViewHolder holder = (CameraClipViewHolder) v.getTag();
        if (holder == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.btn_video_play:
                preparePlayback(holder);
                break;
            case R.id.btn_video_edit:
                int position = holder.getAdapterPosition();
                Clip clip = mClipSet.getClip(position);
                ClipEditActivity.launch(mContext, mVdtCamera, clip);
                break;
        }
    }

    private void preparePlayback(final CameraClipViewHolder holder) {
        int position = holder.getAdapterPosition();
        Clip clip = mClipSet.getClip(position);
        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_URL_TYPE, VdbClient.URL_TYPE_HLS);
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_STREAM, VdbClient.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, clip.getStartTime());

        ClipPlaybackUrlRequest request = new ClipPlaybackUrlRequest(clip, parameters, new VdbResponse.Listener<PlaybackUrl>() {
            @Override
            public void onResponse(PlaybackUrl playbackUrl) {
                playVideo(holder, playbackUrl);
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                //
            }
        });

        mRequestQueue.add(request);
    }

    void playVideo(final CameraClipViewHolder holder, final PlaybackUrl playbackUrl) {
        if (playbackUrl == null) {
            return;
        }

        if (holder.videoPlayer != null) {
            holder.videoContainer.removeView(holder.videoPlayer);
        }

        final int position = holder.getAdapterPosition();
        SurfaceView surfaceView = new SurfaceView(holder.itemView.getContext());
        surfaceView.setTag(holder);
        surfaceView.setOnClickListener(this);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                final MediaPlayer oldMediaPlayer = mMediaPlayers.get(position);
                if (oldMediaPlayer != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            oldMediaPlayer.release();
                            Log.e("test", "Old MediaPlay is released. " + position);
                        }
                    });
                }
                MediaPlayer mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDisplay(surfaceHolder);
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.start();
                    }
                });
                try {
                    mediaPlayer.setDataSource(playbackUrl.url);
                    mediaPlayer.prepareAsync();
                } catch (IOException e) {
                    Log.e("test", "", e);
                }
                mMediaPlayers.put(position, mediaPlayer);
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        });
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        holder.videoContainer.addView(surfaceView, layoutParams);
        holder.videoPlayer = surfaceView;
    }

    @Override
    public void onViewDetachedFromWindow(CameraClipViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        final int id = holder.getAdapterPosition();
        final MediaPlayer mediaPlayer = mMediaPlayers.get(id);
        if (mediaPlayer != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mediaPlayer.release();
                    mMediaPlayers.remove(id);
                    Log.e("test", "MediaPlayer is released: " + id);
                }
            });

        }
        if (holder.videoPlayer != null) {
            holder.videoContainer.removeView(holder.videoPlayer);
            holder.videoPlayer = null;
        }
    }


    public void cleanup() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Enumeration<MediaPlayer> plays = mMediaPlayers.elements();
                while (plays.hasMoreElements()) {
                    plays.nextElement().release();
                    Log.e("test", "Release MediaPlayer.");
                }
                mThread.quit();
                Log.e("test", "Quite handler Thread");
            }
        });
    }

    public static class CameraClipViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.video_desc)
        TextView videoDesc;

        @Bind(R.id.video_time)
        TextView videoTime;

        @Bind(R.id.video_duration)
        TextView videoDuration;

        @Bind(R.id.video_cover)
        ImageView videoCover;

        @Bind(R.id.btn_video_play)
        ImageButton mBtnVideoPlay;

        @Bind(R.id.btn_video_edit)
        ImageButton mBtnVideoEdit;

        @Bind(R.id.video_container)
        FrameLayout videoContainer;

        SurfaceView videoPlayer;

        public CameraClipViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
