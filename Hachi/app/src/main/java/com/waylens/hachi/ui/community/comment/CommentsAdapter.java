package com.waylens.hachi.ui.community.comment;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewAnimator;


import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.entities.Comment;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;


public class CommentsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_COMMENT = 0;
    private static final int VIEW_TYPE_TAIL = 1;

    private ArrayList<Comment> mComments;

    private PrettyTime mPrettyTime;

    private OnCommentClickListener mOnCommentClickListener;

    private OnLoadMoreListener mOnLoadMoreListener;

    private boolean mIsLoadingMore;

    private boolean mHasMore = false;

    public CommentsAdapter(ArrayList<Comment> comments) {
        mComments = comments;
        mPrettyTime = new PrettyTime();
    }

    public void setComments(ArrayList<Comment> comments, boolean hasMore) {
        mComments = comments;
        mHasMore = hasMore;
        notifyDataSetChanged();
    }

    public int addComment(Comment comment) {
        mComments.add(0, comment);
        int pos = 0;
        notifyItemInserted(0);
        return pos;
    }

    public void removeComment(int position) {
        mComments.remove(position);
        notifyItemRemoved(position);
    }

    public void updateCommentID(int position, long commentID) {
        Comment comment = mComments.get(position);
        if (comment == null) {
            return;
        }
        comment.commentID = commentID;
        notifyItemChanged(position);
    }

    public void setOnCommentClickListener(OnCommentClickListener listener) {
        mOnCommentClickListener = listener;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        mOnLoadMoreListener = listener;
    }

    public void setIsLoadMore(boolean isLoading) {
       mIsLoadingMore = isLoading;
    }

    @Override
    public int getItemViewType(int position) {
        if (mComments == null) {
            return 0;
        }

        if (position < mComments.size()) {
            return VIEW_TYPE_COMMENT;
        } else {
            return VIEW_TYPE_TAIL;
        }

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        switch (viewType) {
            case VIEW_TYPE_COMMENT:
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
                return new CommentViewHolder(itemView);
            case VIEW_TYPE_TAIL:
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comments_tail, parent, false);
                return new CommentTailViewHolder(itemView);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_COMMENT) {
            onBindCommentView(holder, position);
        } else {
            onBindTailView(holder, position);
        }
    }



    private void onBindCommentView(RecyclerView.ViewHolder holder, final int position) {
        CommentViewHolder viewHolder = (CommentViewHolder) holder;
        final Comment comment = mComments.get(position);

        Context context = viewHolder.avatarView.getContext();
        Glide.with(context)
            .load(comment.author.avatarUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.menu_profile_photo_default)
            .dontAnimate()
            .into(viewHolder.avatarView);
        viewHolder.tvUserName.setText(comment.author.userName);
        viewHolder.commentContentViews.setText(comment.toSpannable());
        viewHolder.commentTimeView.setText(mPrettyTime.formatUnrounded(new Date(comment.createTime)));
        if (comment.commentID == Comment.UNASSIGNED_ID) {
            viewHolder.commentViewAnimator.setVisibility(View.VISIBLE);
            viewHolder.commentViewAnimator.setDisplayedChild(0);
        } else {
            viewHolder.commentViewAnimator.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOnCommentClickListener != null) {
                    mOnCommentClickListener.onCommentClicked(comment, position);
                }
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mOnCommentClickListener != null) {
                    mOnCommentClickListener.onCommentLongClicked(comment, position);
                }
                return true;
            }
        });
    }


    private void onBindTailView(RecyclerView.ViewHolder holder, int position) {
        final CommentTailViewHolder viewHolder = (CommentTailViewHolder) holder;

        if (mComments.size() == 0) {
            viewHolder.tailInfo.setText(R.string.no_comments_found);
        } else {
            if (mHasMore) {
                viewHolder.tailInfo.setText(R.string.more);
            } else {
                viewHolder.itemView.setVisibility(View.GONE);
            }
        }

        updateTailView(viewHolder);

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsLoadingMore || mOnLoadMoreListener == null) {
                    return;
                }
                mIsLoadingMore = true;
                mOnLoadMoreListener.loadMore();
                updateTailView(viewHolder);
            }
        });
    }

    private void updateTailView(CommentTailViewHolder viewHolder) {
        if (mIsLoadingMore) {
            viewHolder.tailInfo.setVisibility(View.INVISIBLE);
            viewHolder.loadMoreProgressBar.setVisibility(View.VISIBLE);
        } else {
            viewHolder.tailInfo.setVisibility(View.VISIBLE);
            viewHolder.loadMoreProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        if (mComments == null) {
            return 0;
        } else {
            return mComments.size() + 1;
        }
    }

    public void addComments(ArrayList<Comment> commentList, boolean hasMore) {
        if (mComments == null) {
            mComments = new ArrayList<>();
        }

        mHasMore = hasMore;

        mComments.addAll(commentList);
        notifyDataSetChanged();
    }

    public interface OnCommentClickListener {
        void onCommentClicked(Comment comment, int position);
        void onCommentLongClicked(Comment comment, int position);
    }

    public interface OnLoadMoreListener {
        void loadMore();
    }

    public static class CommentTailViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.tail_info)
        TextView tailInfo;

        @BindView(R.id.load_more_progressbar)
        ProgressBar loadMoreProgressBar;

        public CommentTailViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.user_avatar)
        CircleImageView avatarView;

        @BindView(R.id.tvUserName)
        TextView tvUserName;

        @BindView(R.id.comment_content)
        TextView commentContentViews;

        @BindView(R.id.comment_time)
        TextView commentTimeView;

        @BindView(R.id.status_container)
        ViewAnimator commentViewAnimator;


        public CommentViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

}
