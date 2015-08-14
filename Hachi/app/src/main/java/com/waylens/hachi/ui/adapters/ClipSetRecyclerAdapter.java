package com.waylens.hachi.ui.adapters;

import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;

import com.orhanobut.logger.Logger;
import com.transee.vdb.Clip;
import com.transee.vdb.ClipSet;
import com.waylens.hachi.R;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RecyclerView Adapter for ClipSet
 * <p/>
 * Created by Richard on 8/10/15.
 */
public class ClipSetRecyclerAdapter extends RecyclerView.Adapter<ClipViewHolder> {

    static final String TAG = "ClipSetRecyclerAdapter";

    ClipSet mClipSet;
    BitmapDrawable[] mBitmaps;
    String[] mPlayBackURLs;
    ConcurrentHashMap<Integer, Boolean> hasSourceMap;
    ConcurrentHashMap<Integer, Boolean> isPreparedMap;
    ConcurrentHashMap<Integer, MediaPlayer> mMediaPlayers;

    @Override
    public void onViewRecycled(ClipViewHolder holder) {
        super.onViewRecycled(holder);
    }

    public ClipSetRecyclerAdapter(ClipSet clipSet) {
        mClipSet = clipSet;
        if (clipSet != null) {
            mBitmaps = new BitmapDrawable[clipSet.getCount()];
            mPlayBackURLs = new String[mBitmaps.length];
            hasSourceMap = new ConcurrentHashMap<>(mBitmaps.length);
            isPreparedMap = new ConcurrentHashMap<>(mBitmaps.length);
            mMediaPlayers = new ConcurrentHashMap<>(mBitmaps.length);
        }
    }

    public void setClipSet(ClipSet clipSet) {
        mClipSet = clipSet;
        if (clipSet != null) {
            mBitmaps = new BitmapDrawable[clipSet.getCount()];
            mPlayBackURLs = new String[mBitmaps.length];
            hasSourceMap = new ConcurrentHashMap<>(mBitmaps.length);
            isPreparedMap = new ConcurrentHashMap<>(mBitmaps.length);
            mMediaPlayers = new ConcurrentHashMap<>(mBitmaps.length);
        }
        notifyDataSetChanged();
    }

    @Override
    public ClipViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.live_video_list_item, parent, false);
        ClipViewHolder clipViewHolder = new ClipViewHolder(itemView);
        clipViewHolder.videoPlayView.getHolder().addCallback(new SurfaceHolderCallback(clipViewHolder));
        return clipViewHolder;
    }

    @Override
    public void onBindViewHolder(final ClipViewHolder holder, final int position) {
        Clip clip = mClipSet.getClip(position);
        holder.videoDesc.setText("Mocked description " + position);
        holder.videoTime.setText(clip.getDateTimeString());
        holder.videoDuration.setText(clip.getDurationString());
        holder.progressBar.setVisibility(View.GONE);
        if (mBitmaps[position] != null) {
            holder.videoPlayView.setBackground(mBitmaps[position]);
        }

        if (mPlayBackURLs[position] != null) {
            holder.videoControl.setBackgroundResource(R.drawable.ic_play_circle);
            holder.videoControl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playVideo(holder, position);
                }
            });
        } else {
            holder.videoControl.setBackgroundResource(R.drawable.ic_not_ready);
        }
        holder.videoControl.setVisibility(View.VISIBLE);
        holder.videoPlayView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "OnClick.");
            }
        });
    }

    @Override
    public void onViewDetachedFromWindow(ClipViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        int position = holder.getAdapterPosition();
        MediaPlayer mediaPlayer = mMediaPlayers.get(position);
        if (position != RecyclerView.NO_POSITION
                && mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        Log.e(TAG, "onDetachedFromRecyclerView");
    }

    private void playVideo(final ClipViewHolder holder, final int position) {
        MediaPlayer mediaPlayer = mMediaPlayers.get(position);
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                holder.progressBar.setVisibility(View.GONE);
                holder.viewCover.setVisibility(View.GONE);
                mp.setDisplay(holder.videoPlayView.getHolder());
                mp.start();
                holder.videoPlayView.setBackgroundColor(Color.TRANSPARENT);
            }
        });
        mMediaPlayers.put(position, mediaPlayer);

        try {
            mediaPlayer.setDataSource(mPlayBackURLs[position]);
            mediaPlayer.prepareAsync();
            holder.videoControl.setVisibility(View.GONE);
            holder.progressBar.setVisibility(View.VISIBLE);
        } catch (IOException e) {
            Logger.t(TAG).e(e, "");
        }
    }

    @Override
    public int getItemCount() {
        if (mClipSet != null) {
            return mClipSet.getCount();
        } else {
            return 0;
        }
    }

    public void setClipCover(BitmapDrawable bitmapDrawable, int position) {
        if (position < 0 || position > mBitmaps.length) {
            Logger.t(TAG).e("Illegal argument: " + position);
            return;
        }
        mBitmaps[position] = bitmapDrawable;
        notifyItemChanged(position);
    }

    public void setPlaybackURL(String url, int position) {
        if (position < 0 || position > mBitmaps.length) {
            Logger.t(TAG).e("Illegal argument: " + position);
            return;
        }
        mPlayBackURLs[position] = url;
        notifyItemChanged(position);
    }


    class SurfaceHolderCallback implements SurfaceHolder.Callback {

        ClipViewHolder mClipViewHolder;

        public SurfaceHolderCallback(ClipViewHolder holder) {
            mClipViewHolder = holder;
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.e(TAG, "surfaceCreated: " + mClipViewHolder.getAdapterPosition());
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.e(TAG, "surfaceChanged: " + mClipViewHolder.getAdapterPosition());
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.e(TAG, "surfaceDestroyed: " + mClipViewHolder.getAdapterPosition());
        }
    }
}