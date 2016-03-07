package com.waylens.hachi.ui.adapters;

import android.content.Intent;
import android.media.MediaPlayer;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.entities.MusicItem;
import com.waylens.hachi.ui.helpers.DownloadHelper;

import java.io.IOException;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Richard on 2/1/16.
 */
public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.ViewHolder> {

    ArrayList<MusicItem> mMusicItems = new ArrayList<>();

    ViewHolder currentHolder;

    MediaPlayer mMediaPlayer;

    OnMusicActionListener mListener;

    DownloadHelper mDownloadHelper;

    public MusicListAdapter(OnMusicActionListener listener, DownloadHelper downloadHelper) {
        mListener = listener;
        mDownloadHelper = downloadHelper;
    }

    public void setMusicItems(ArrayList<MusicItem> musicItems) {
        if (musicItems == null) {
            return;
        }
        mMusicItems = musicItems;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bg_music_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final MusicItem musicItem = mMusicItems.get(position);
        holder.musicTitle.setText(musicItem.title);
        holder.musicLength.setText(DateUtils.formatElapsedTime(musicItem.duration));
        switch (musicItem.status) {
            case MusicItem.STATUS_LOCAL:
                holder.setDownloadStatus(ViewHolder.STATUS_NORMAL);
                holder.itemContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currentHolder != null && currentHolder != holder && currentHolder.isInPreviewMode()) {
                            currentHolder.toggleMode();
                        }
                        holder.toggleMode();
                        currentHolder = holder;
                        previewAudio(musicItem);
                    }
                });
                break;
            case MusicItem.STATUS_REMOTE:
                break;
            case MusicItem.STATUS_DOWNLOADING:
                holder.setDownloadStatus(ViewHolder.STATUS_DOWNLOADING);
                break;
        }

        holder.btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onAddMusic(musicItem);
            }
        });

        holder.btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayMode(holder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mMusicItems.size();
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        releaseMediaPlayer();
    }

    public void updateMusicItem(MusicItem musicItem) {
        int index = mMusicItems.indexOf(musicItem);
        notifyItemChanged(index);
    }

    void previewAudio(MusicItem musicItem) {
        releaseMediaPlayer();
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(musicItem.localPath);
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e("test", "", e);
        }
    }

    boolean isPlaying() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    void togglePlayMode(ViewHolder holder) {
        if (mMediaPlayer == null) {
            return;
        }
        if (isPlaying()) {
            mMediaPlayer.pause();
            holder.btnPlay.setImageResource(R.drawable.ic_play_arrow_white_24dp);
        } else {
            mMediaPlayer.start();
            holder.btnPlay.setImageResource(R.drawable.ic_pause_white_24dp);
        }
    }

    void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.item_container)
        LinearLayout itemContainer;

        @Bind(R.id.music_icon)
        ImageView musicIcon;

        @Bind(R.id.music_title)
        TextView musicTitle;

        @Bind(R.id.music_length)
        TextView musicLength;

        @Bind(R.id.item_view_animator)
        ViewAnimator viewAnimator;

        @Bind(R.id.music_btn_add)
        ImageView btnAdd;

        @Bind(R.id.music_btn_play)
        ImageView btnPlay;

        static final int STATUS_WAITING = 0;
        static final int STATUS_DOWNLOADING = 1;
        static final int STATUS_NORMAL = 2;
        static final int STATUS_PREVIEW = 3;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void toggleMode() {
            if (viewAnimator.getDisplayedChild() == STATUS_NORMAL) {
                viewAnimator.setDisplayedChild(STATUS_PREVIEW);
            } else {
                viewAnimator.setDisplayedChild(STATUS_NORMAL);
            }

        }

        public boolean isInPreviewMode() {
            return viewAnimator.getDisplayedChild() == STATUS_PREVIEW;
        }

        public void setDownloadStatus(int status) {
            viewAnimator.setDisplayedChild(status);
        }
    }

    public interface OnMusicActionListener {
        void onAddMusic(MusicItem musicItem);
    }
}
