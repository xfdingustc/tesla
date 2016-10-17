package com.waylens.hachi.ui.community.comment;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;
import com.waylens.hachi.rest.bean.Comment;
import com.waylens.hachi.ui.views.AvatarView;
import com.waylens.hachi.utils.AnimUtils;
import com.waylens.hachi.utils.TransitionUtils;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class CommentsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_COMMENT = 0;
    private static final int VIEW_TYPE_TAIL = 1;

    private static final int EXPAND = 0x1;
    private static final int COLLAPSE = 0x2;
    private static final int COMMENT_LIKE = 0x3;
    private static final int REPLY = 0x4;

    private final RecyclerView mCommentListView;

    private List<Comment> mComments;

    private PrettyTime mPrettyTime;

    private OnCommentClickListener mOnCommentClickListener;

    private OnLoadMoreListener mOnLoadMoreListener;

    private boolean mIsLoadingMore;

    private boolean mHasMore = false;

    private final Transition mExpandCollapse;

    private final Context mContext;

    private int mExpandedCommentPosition = RecyclerView.NO_POSITION;

    private CommentAnimator mCommentAnimator;


    public CommentsAdapter(RecyclerView commentList, Context context, List<Comment> comments) {
        mCommentListView = commentList;
        mCommentAnimator = new CommentAnimator();
        mCommentListView.setItemAnimator(mCommentAnimator);
        mContext = context;
        mComments = comments;
        mPrettyTime = new PrettyTime();
        mExpandCollapse = new AutoTransition();
        mExpandCollapse.setDuration(120);
        mExpandCollapse.setInterpolator(AnimUtils.getFastOutSlowInInterpolator(mContext));
        mExpandCollapse.addListener(new TransitionUtils.TransitionListenerAdapter() {
            @Override
            public void onTransitionStart(Transition transition) {
                super.onTransitionStart(transition);
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                super.onTransitionEnd(transition);
                mCommentAnimator.setAnimateMoves(true);
            }
        });
    }

    public void setComments(List<Comment> comments, boolean hasMore) {
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


        viewHolder.avatarView.loadAvatar(comment.author.avatarUrl, comment.author.userName);
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

                Comment clickedComment = getComment(position);
                TransitionManager.beginDelayedTransition(mCommentListView, mExpandCollapse);
                mCommentAnimator.setAnimateMoves(false);

                if (mExpandedCommentPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(mExpandedCommentPosition, COLLAPSE);
                }

                if (mExpandedCommentPosition != position) {
                    mExpandedCommentPosition = position;
                    notifyItemChanged(position, EXPAND);
                } else {
                    mExpandedCommentPosition = RecyclerView.NO_POSITION;
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
        final boolean isExpanded = position == mExpandedCommentPosition;
        setExpanded((CommentViewHolder) holder, isExpanded);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List<Object> payloads) {
        if (holder instanceof CommentViewHolder) {
            bindPartialCommentChange((CommentViewHolder) holder, position, payloads);
        } else {
            onBindViewHolder(holder, position);
        }
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
                if (!mHasMore) {
                    return;
                }
                if (mIsLoadingMore || mOnLoadMoreListener == null) {
                    return;
                }
                mIsLoadingMore = true;
                mOnLoadMoreListener.loadMore();
                updateTailView(viewHolder);
            }
        });
    }

    private void bindPartialCommentChange(CommentViewHolder holder, int position, List<Object> partialChangePayloads) {
        // for certain changes we don't need to rebind data, just update some view state
        if ((partialChangePayloads.contains(EXPAND)
            || partialChangePayloads.contains(COLLAPSE))
            || partialChangePayloads.contains(REPLY)) {
            setExpanded(holder, position == mExpandedCommentPosition);
        } else if (partialChangePayloads.contains(COMMENT_LIKE)) {
            return; // nothing to do
        } else {
            onBindViewHolder(holder, position);
        }
    }

    private void setExpanded(CommentViewHolder holder, boolean isExpanded) {
        holder.itemView.setActivated(isExpanded);
        holder.btnReply.setVisibility((isExpanded) ? View.VISIBLE : View.GONE);
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

    public void addComments(List<Comment> commentList, boolean hasMore) {
        if (mComments == null) {
            mComments = new ArrayList<>();
        }

        mHasMore = hasMore;

        mComments.addAll(commentList);
        notifyDataSetChanged();
    }

    private Comment getComment(int adapterPosition) {
        return mComments.get(adapterPosition); // description
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

        @BindView(R.id.avatar_view)
        AvatarView avatarView;

        @BindView(R.id.tvUserName)
        TextView tvUserName;

        @BindView(R.id.comment_content)
        TextView commentContentViews;

        @BindView(R.id.comment_time)
        TextView commentTimeView;

        @BindView(R.id.status_container)
        ViewAnimator commentViewAnimator;

        @BindView(R.id.reply)
        ImageButton btnReply;


        public CommentViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

}
