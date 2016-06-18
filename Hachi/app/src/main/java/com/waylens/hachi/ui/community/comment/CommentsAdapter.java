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

    ArrayList<Comment> mComments;

    PrettyTime mPrettyTime;

    OnCommentClickListener mOnCommentClickListener;

    OnLoadMoreListener mOnLoadMoreListener;

    boolean mIsLoadingMore;

    public CommentsAdapter(ArrayList<Comment> comments) {
        mComments = comments;
        mPrettyTime = new PrettyTime();
    }

    public void setComments(ArrayList<Comment> comments, boolean hasMore) {
        mComments = comments;
        if (hasMore) {
            mComments.add(0, Comment.createLoadMoreIndicator());
        }
        notifyDataSetChanged();
    }

    public int addComment(Comment comment) {
        mComments.add(comment);
        int pos = mComments.size() - 1;
        notifyItemInserted(pos);
        return pos;
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
        if (mComments == null || mComments.get(position) == null) {
            return 0;
        }
        return mComments.get(position).type;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        switch (viewType) {
            case Comment.TYPE_NORMAL:
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
                return new CommentViewHolder(itemView);
            case Comment.TYPE_LOAD_MORE_INDICATOR:
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comments_loadmore, parent, false);
                return new CommentLoadMoreVH(itemView);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == Comment.TYPE_NORMAL) {
            CommentViewHolder viewHolder = (CommentViewHolder) holder;
            final Comment comment = mComments.get(position);

            Context context = viewHolder.avatarView.getContext();
            Glide.with(context)
                .load(comment.author.avatarUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.default_avatar)
                .crossFade()
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
                        mOnCommentClickListener.onCommentClicked(comment);
                    }
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mOnCommentClickListener != null) {
                        mOnCommentClickListener.onCommentLongClicked(comment);
                    }
                    return true;
                }
            });
        } else {
            final CommentLoadMoreVH vh = (CommentLoadMoreVH) holder;
            updateLoadMoreStatus(vh);
            vh.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mIsLoadingMore || mOnLoadMoreListener == null) {
                        return;
                    }
                    mIsLoadingMore = true;
                    mOnLoadMoreListener.loadMore();
                    updateLoadMoreStatus(vh);
                }
            });
        }
    }

    void updateLoadMoreStatus(CommentLoadMoreVH vh) {
        if (mIsLoadingMore) {
            vh.loadMoreView.setVisibility(View.INVISIBLE);
            vh.loadMoreProgressBar.setVisibility(View.VISIBLE);
        } else {
            vh.loadMoreView.setVisibility(View.VISIBLE);
            vh.loadMoreProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        if (mComments == null) {
            return 0;
        } else {
            return mComments.size();
        }
    }

    public void addComments(ArrayList<Comment> commentList, boolean hasMore) {
        if (mComments == null) {
            mComments = new ArrayList<>();
        }
        int index = 0;
        if (hasMore) {
            if (mComments.size() == 0 || mComments.get(0).type != Comment.TYPE_LOAD_MORE_INDICATOR) {
                mComments.add(Comment.createLoadMoreIndicator());
            }
            index = 1;
        } else {
            if (mComments.size() > 0 && mComments.get(0).type == Comment.TYPE_LOAD_MORE_INDICATOR) {
                mComments.remove(0);
            }
            index = 0;
        }
        mComments.addAll(index, commentList);
        notifyItemRangeInserted(0, commentList.size() + index);
    }

    public interface OnCommentClickListener {
        void onCommentClicked(Comment comment);
        void onCommentLongClicked(Comment comment);
    }

    public interface OnLoadMoreListener {
        void loadMore();
    }

    public static class CommentLoadMoreVH extends RecyclerView.ViewHolder {

        @BindView(R.id.load_more)
        View loadMoreView;

        @BindView(R.id.load_more_progressbar)
        ProgressBar loadMoreProgressBar;

        public CommentLoadMoreVH(View itemView) {
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
