package com.waylens.hachi.ui.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.fragments.CommentsFragment;
import com.waylens.hachi.ui.fragments.YouTubeFragment;
import com.waylens.hachi.utils.ImageUtils;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.ViewUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ocpsoft.prettytime.PrettyTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Richard on 8/21/15.
 */
public class MomentsRecyclerAdapter extends RecyclerView.Adapter<MomentViewHolder> {

    public ArrayList<Moment> mMoments;

    PrettyTime mPrettyTime;

    FragmentManager mFragmentManager;

    YouTubeFragment videoFragment;

    RequestQueue mRequestQueue;

    ConcurrentHashMap<Integer, MediaPlayer> mMediaPlayers;

    HandlerThread mThread;

    Handler mHandler;

    Resources mResources;

    public MomentsRecyclerAdapter(ArrayList<Moment> moments, FragmentManager fm, RequestQueue requestQueue, Resources resources) {
        mMoments = moments;
        mPrettyTime = new PrettyTime();
        mFragmentManager = fm;
        videoFragment = YouTubeFragment.newInstance();
        mRequestQueue = requestQueue;
        mMediaPlayers = new ConcurrentHashMap<>();
        mThread = new HandlerThread("cleanup");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mResources = resources;
    }

    public void setMoments(ArrayList<Moment> moments) {
        mMoments = moments;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (mMoments == null) {
            return 0;
        }
        return mMoments.get(position).type;
    }


    @Override
    public MomentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_feed, parent, false);
        ViewStub viewStub = (ViewStub) itemView.findViewById(R.id.video_player_stub);
        if (viewType == Moment.TYPE_WAYLENS) {
            viewStub.setLayoutResource(R.layout.layout_video_player_waylens);
            viewStub.inflate();
            return new WaylensMomentVH(itemView);
        } else {
            viewStub.setLayoutResource(R.layout.layout_video_player_youtube);
            FrameLayout view = (FrameLayout) viewStub.inflate();
            FrameLayout fragmentContainer = new FrameLayout(itemView.getContext());
            int viewId = ViewUtils.generateViewId();
            fragmentContainer.setId(viewId);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            view.addView(fragmentContainer, layoutParams);
            return new YouTubeMomentVH(itemView, fragmentContainer);
        }
    }

    @Override
    public void onBindViewHolder(final MomentViewHolder holder, final int position) {
        final Moment moment = mMoments.get(position);

        ImageLoader.getInstance().displayImage(moment.owner.avatarUrl, holder.userAvatar, ImageUtils.getAvatarOptions());
        holder.userName.setText(moment.owner.userName);
        holder.videoTime.setText(mPrettyTime.formatUnrounded(new Date(moment.uploadTime)));
        ImageLoader.getInstance().displayImage(moment.thumbnail, holder.videoCover, ImageUtils.getVideoOptions());
        holder.videoDuration.setText(DateUtils.formatElapsedTime(moment.duration / 1000l));

        updateLikeState(holder, moment);
        updateLikeCount(holder, moment);

        holder.btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likeVideo(holder, moment);
                //like the video at once after posting request to server.
                moment.isLiked = !moment.isLiked;
                updateLikeState(holder, moment);
            }
        });

        holder.btnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommentsFragment fragment = new CommentsFragment();
                Bundle args = new Bundle();
                args.putLong(CommentsFragment.ARG_MOMENT_ID, moment.id);
                fragment.setArguments(args);
                mFragmentManager.beginTransaction().add(R.id.root_container, fragment).commit();
            }
        });

        configureVideoPlay(holder, position, moment);

        updateCommentCount(holder, moment);
        if (moment.comments != null) {
            holder.commentView.setText(moment.comments);
            holder.commentContainer.setVisibility(View.VISIBLE);
        } else {
            holder.commentContainer.setVisibility(View.GONE);
        }
    }

    void updateLikeState(MomentViewHolder vh, Moment moment) {

        if (moment.isLiked) {
            vh.btnLike.setImageResource(R.drawable.feed_button_like_active);
        } else {
            vh.btnLike.setImageResource(R.drawable.feed_button_like);
        }
    }

    void updateLikeCount(MomentViewHolder vh, Moment moment) {
        if (moment.likesCount == 0) {
            vh.likeCount.setText(mResources.getString(R.string.zero_likes));
        } else {
            vh.likeCount.setText(mResources.getQuantityString(R.plurals.number_of_likes,
                    moment.likesCount,
                    moment.likesCount));
        }
    }

    void updateCommentCount(MomentViewHolder vh, Moment moment) {
        if (moment.commentsCount == 0) {
            vh.commentIcon.setVisibility(View.GONE);
            vh.commentCountView.setVisibility(View.GONE);
        } else {
            vh.commentIcon.setVisibility(View.VISIBLE);
            vh.commentCountView.setVisibility(View.VISIBLE);
            vh.commentCountView.setText(mResources.getQuantityString(
                    R.plurals.number_of_comments, moment.commentsCount, moment.commentsCount));
        }
    }

    void likeVideo(final MomentViewHolder vh, final Moment moment) {
        JSONObject params = new JSONObject();
        try {
            params.put("momentID", moment.id);
            params.put("cancel", moment.isLiked);
        } catch (JSONException e) {
            Log.e("test", "", e);
        }

        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.POST, Constants.API_MOMENT_LIKE,
                params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        moment.likesCount = response.optInt("count");
                        Log.e("test", "LikeCount:" + moment.likesCount);
                        updateLikeCount(vh, moment);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        SparseIntArray errorInfo = ServerMessage.parseServerError(error);
                        //showMessage(errorInfo.get(1));
                        Log.e("test", "Error: " + error);
                    }
                }));
    }

    void configureVideoPlay(MomentViewHolder holder, final int position, final Moment moment) {
        if (moment.type == Moment.TYPE_WAYLENS) {
            final WaylensMomentVH vh = (WaylensMomentVH) holder;
            vh.videoControl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playWaylensVideo(vh, moment, position);
                }
            });
            vh.videoControl.setVisibility(View.VISIBLE);
            vh.progressBar.setVisibility(View.GONE);
        } else {
            final YouTubeMomentVH vh = (YouTubeMomentVH) holder;
            vh.videoControl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFragmentManager.beginTransaction().remove(videoFragment).commit();
                    videoFragment = YouTubeFragment.newInstance();
                    videoFragment.setVideoId(moment.videoID);
                    vh.videoFragment = videoFragment;
                    mFragmentManager.beginTransaction().replace(vh.fragmentContainer.getId(), videoFragment).commit();
                    vh.videoControl.setVisibility(View.GONE);
                }
            });

        }
    }

    void playWaylensVideo(final WaylensMomentVH vh, final Moment moment, final int position) {
        long s = System.currentTimeMillis();
        SurfaceView surfaceView = new SurfaceView(vh.videoContainer.getContext());
        long e = System.currentTimeMillis();
        long delta = e - s;
        s = e;
        Log.e("test", "Create Surface:" + delta);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.e("test", "surfaceCreated");

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
                mediaPlayer.setDisplay(holder);
                mMediaPlayers.put(position, mediaPlayer);
                loadVideoInfo(vh, moment, position);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        vh.videoContainer.addView(surfaceView, 2, layoutParams);

        e = System.currentTimeMillis();
        delta = e - s;
        s = e;
        Log.e("test", "Add View:" + delta);
        vh.videoPlayView = surfaceView;
        vh.videoControl.setVisibility(View.GONE);
        vh.progressBar.setVisibility(View.VISIBLE);
    }

    void loadVideoInfo(final WaylensMomentVH vh, Moment moment, final int position) {
        String url = Constants.API_MOMENT_PLAY + moment.id;
        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        JSONArray videoFragments = response.optJSONArray("fragments");
                        if (videoFragments == null) {
                            return;
                        }
                        playVideoFragments(vh, videoFragments, position);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        SparseIntArray errorInfo = ServerMessage.parseServerError(error);
                        //showMessage(errorInfo.get(1));
                        Log.e("test", "Error: " + error);
                    }
                }));
        mRequestQueue.start();
    }

    void playVideoFragments(final WaylensMomentVH vh, JSONArray videoFragments, int position) {
        MediaPlayer mediaPlayer = mMediaPlayers.get(position);
        if (mediaPlayer == null) {
            Log.e("test", "MediaPlayer is null");
            return;
        }
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
                vh.progressBar.setVisibility(View.GONE);
            }
        });
        try {
            String videoURL = videoFragments.optJSONObject(0).optString("videoUrl");
            Log.e("test", "URL: " + videoURL);
            mediaPlayer.setDataSource(videoURL);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e("test", "", e);
        }

    }

    @Override
    public void onViewAttachedToWindow(MomentViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder.getItemViewType() == Moment.TYPE_WAYLENS) {
            WaylensMomentVH vh = (WaylensMomentVH) holder;
            vh.progressBar.setVisibility(View.GONE);
        }
        holder.videoControl.setVisibility(View.VISIBLE);
    }

    @Override
    public void onViewDetachedFromWindow(MomentViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder.getItemViewType() == Moment.TYPE_YOUTUBE) {
            YouTubeMomentVH vh = (YouTubeMomentVH) holder;
            if (vh.videoFragment != null) {
                mFragmentManager.beginTransaction().remove(videoFragment).commit();
            }
        } else if (holder.getItemViewType() == Moment.TYPE_WAYLENS) {
            WaylensMomentVH vh = (WaylensMomentVH) holder;
            final int id = (int) holder.getItemId();
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
            if (vh.videoPlayView != null) {
                vh.videoContainer.removeView(vh.videoPlayView);
                vh.videoPlayView = null;
            }
            mRequestQueue.cancelAll(new RequestQueue.RequestFilter() {
                @Override
                public boolean apply(Request<?> request) {
                    return !Constants.API_MOMENT_LIKE.equals(request.getOriginUrl());
                }
            });
        }
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
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

    @Override
    public int getItemCount() {
        if (mMoments == null) {
            return 0;
        } else {
            return mMoments.size();
        }

    }

    public void updateMoment(Spannable spannedComments, int position) {
        Moment moment = mMoments.get(position);
        moment.comments = spannedComments;
        notifyItemChanged(position);
    }
}
