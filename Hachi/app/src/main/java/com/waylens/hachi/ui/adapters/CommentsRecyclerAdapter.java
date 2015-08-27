package com.waylens.hachi.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.waylens.hachi.R;
import com.waylens.hachi.utils.ImageUtils;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Richard on 8/26/15.
 */
public class CommentsRecyclerAdapter extends RecyclerView.Adapter<CommentViewHolder> {

    ArrayList<Comment> mComments;

    PrettyTime mPrettyTime;

    public CommentsRecyclerAdapter(ArrayList<Comment> comments) {
        mComments = comments;
        mPrettyTime = new PrettyTime();
    }

    public void setComments(ArrayList<Comment> comments) {
        mComments = comments;
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

    @Override
    public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(CommentViewHolder holder, int position) {
        Comment comment = mComments.get(position);
        ImageLoader.getInstance().displayImage(comment.author.avatarUrl, holder.avatarView, ImageUtils.getAvatarOptions());
        holder.commentContentViews.setText(comment.content);
        holder.commentTimeView.setText(mPrettyTime.formatUnrounded(new Date(comment.createTime)));
        if (comment.commentID == Comment.UNASSIGNED_ID) {
            holder.commentViewAnimator.setVisibility(View.VISIBLE);
            holder.commentViewAnimator.setDisplayedChild(0);
        } else {
            holder.commentViewAnimator.setVisibility(View.GONE);
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
}