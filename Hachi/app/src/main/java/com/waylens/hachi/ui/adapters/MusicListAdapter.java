package com.waylens.hachi.ui.adapters;

import android.media.MediaPlayer;
import android.support.v7.widget.LinearLayoutManager;
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

    int mPreviewIndex = -1;

    MediaPlayer mMediaPlayer;

    OnMusicActionListener mListener;

    DownloadHelper mDownloadHelper;

    LinearLayoutManager mLayoutManager;

    public MusicListAdapter(OnMusicActionListener listener, DownloadHelper downloadHelper,
                            LinearLayoutManager layoutManager) {
        mListener = listener;
        mDownloadHelper = downloadHelper;
        mLayoutManager = layoutManager;
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
        holder.musicIcon.setImageResource(R.drawable.ic_cloud_queue_white_24dp);

        switch (musicItem.status) {
            case MusicItem.STATUS_LOCAL:
                holder.musicIcon.setImageResource(R.drawable.ic_cloud_done_white_24dp);
                holder.setDownloadStatus(ViewHolder.STATUS_NORMAL);
                holder.itemContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mPreviewIndex == holder.getAdapterPosition()) {
                            holder.toggleMode();
                            releaseMediaPlayer();
                            mPreviewIndex = -1;
                            return;
                        }

                        if (mPreviewIndex >= mLayoutManager.findFirstVisibleItemPosition()
                                && mPreviewIndex <= mLayoutManager.findLastVisibleItemPosition()) {
                            notifyItemChanged(mPreviewIndex);
                        }

                        holder.toggleMode();
                        mPreviewIndex = holder.getAdapterPosition();
                        previewAudio(musicItem);
                    }
                });
                break;
            case MusicItem.STATUS_REMOTE:
                holder.itemContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        musicItem.status = MusicItem.STATUS_DOWNLOADING;
                        mDownloadHelper.download(musicItem);
                        holder.setDownloadStatus(ViewHolder.STATUS_DOWNLOADING);
                    }
                });
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

    void exitPreviewMode() {

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

        static final int STATUS_NORMAL = 0;
        static final int STATUS_DOWNLOADING = 1;
        static final int STATUS_PREVIEW = 2;

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

        public void setDownloadStatus(int status) {
            viewAnimator.setDisplayedChild(status);
        }
    }

    public interface OnMusicActionListener {
        void onAddMusic(MusicItem musicItem);
    }
}
