package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.entities.BasicUserInfo;
import com.waylens.hachi.utils.ImageUtils;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2015/9/23.
 */
public class UserListRvAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context mContext;
    private List<BasicUserInfo> mUserList;

    public UserListRvAdapter(Context context) {
        this.mContext = context;
    }

    public void setUserList(List<BasicUserInfo> userList) {
        this.mUserList = userList;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_user_list, parent, false);
        return new UserListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        UserListViewHolder viewHolder = (UserListViewHolder)holder;

        BasicUserInfo userInfo = mUserList.get(position);

        viewHolder.mTvUserName.setText(userInfo.userName);

        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.displayImage(userInfo.avatarUrl, viewHolder.mUserAvater, ImageUtils.getAvatarOptions());
    }

    @Override
    public int getItemCount() {
        if (mUserList != null) {
            return mUserList.size();
        }

        return 0;
    }


    public class UserListViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.userAvatar)
        CircleImageView mUserAvater;

        @Bind(R.id.tvUserName)
        TextView mTvUserName;

        public UserListViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
