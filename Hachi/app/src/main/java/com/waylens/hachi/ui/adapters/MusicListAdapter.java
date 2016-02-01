package com.waylens.hachi.ui.adapters;

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

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Richard on 2/1/16.
 */
public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.ViewHolder> {

    ArrayList<MusicItem> mMusicItems = new ArrayList<>();

    ViewHolder currentHolder;

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
        MusicItem musicItem = mMusicItems.get(position);
        holder.musicTitle.setText(musicItem.title);
        holder.musicLength.setText(DateUtils.formatElapsedTime(musicItem.length / 1000l));
        holder.itemContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentHolder != null && currentHolder != holder && currentHolder.isInPreviewMode()) {
                    currentHolder.toggleMode();
                }
                holder.toggleMode();
                currentHolder = holder;
            }
        });

        holder.btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("test", "Click add");
            }
        });

        holder.btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("test", "Click Play");
            }
        });
    }

    @Override
    public int getItemCount() {
        return mMusicItems.size();
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
        View btnAdd;

        @Bind(R.id.music_btn_play)
        View btnPlay;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void toggleMode() {
            viewAnimator.setDisplayedChild(viewAnimator.getDisplayedChild() + 1 % 2);
        }

        public boolean isInPreviewMode() {
            return viewAnimator.getDisplayedChild() == 1;
        }
    }
}
