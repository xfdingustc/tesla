package com.waylens.hachi.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.entities.Comment;
import com.waylens.hachi.utils.ImageUtils;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Richard on 8/26/15.
 */
public class CommentsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    ArrayList<Comment> mComments;

    PrettyTime mPrettyTime;

    OnCommentClickListener mOnCommentClickListener;

    OnLoadMoreListener mOnLoadMoreListener;

    boolean mIsLoadingMore;

    public CommentsRecyclerAdapter(ArrayList<Comment> comments) {
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
            CommentViewHolder vh = (CommentViewHolder) holder;
            final Comment comment = mComments.get(position);
            ImageLoader.getInstance().displayImage(comment.author.avatarUrl, vh.avatarView, ImageUtils.getAvatarOptions());
            vh.commentContentViews.setText(comment.toSpannable());
            vh.commentTimeView.setText(mPrettyTime.formatUnrounded(new Date(comment.createTime)));
            if (comment.commentID == Comment.UNASSIGNED_ID) {
                vh.commentViewAnimator.setVisibility(View.VISIBLE);
                vh.commentViewAnimator.setDisplayedChild(0);
            } else {
                vh.commentViewAnimator.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mOnCommentClickListener != null) {
                        mOnCommentClickListener.onCommentClicked(comment);
                    }
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
    }

    public interface OnLoadMoreListener {
        void loadMore();
    }
}
