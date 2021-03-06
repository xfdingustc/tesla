package com.waylens.hachi.ui.entities.moment;

import com.waylens.hachi.rest.bean.Comment;
import com.waylens.hachi.rest.bean.MomentSimple;
import com.waylens.hachi.rest.bean.User;
import com.waylens.hachi.ui.entities.MomentPicture;
import com.waylens.hachi.ui.entities.UserDeprecated;
import java.io.Serializable;
import java.util.List;

/**
 * Created by Xiaofei on 2016/9/13.
 */
public class MomentEx implements Serializable{
    public MomentAbstract moment;

    public User owner;

    public User lastLike;

    public List<Comment> lastComments;

    public List<MomentPicture> pictureUrls;

    public static MomentEx fromMomentSimple(MomentSimple momentSimple, User user) {
        MomentEx momentEx = new MomentEx();
        momentEx.moment = new MomentAbstract(momentSimple);
        momentEx.owner = user;
        return momentEx;
    }
}
