package com.waylens.hachi.ui.entities.moment;

import com.waylens.hachi.rest.bean.Comment;
import com.waylens.hachi.ui.entities.MomentPicture;
import com.waylens.hachi.ui.entities.UserDeprecated;

import java.util.List;

/**
 * Created by Xiaofei on 2016/9/13.
 */
public class MomentEx {
    public MomentAbstract moment;

    public UserDeprecated owner;

    public UserDeprecated lastLike;

    public List<Comment> lastComments;

    public List<MomentPicture> pictureUrls;
}
