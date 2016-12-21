package com.waylens.hachi.rest.response;

import com.waylens.hachi.rest.bean.LeaderBoardItem;
import com.waylens.hachi.rest.bean.VehicleInfo;
import com.waylens.hachi.snipe.utils.ToStringUtils;


import java.util.List;

/**
 * Created by lshw on 16/9/5.
 */
public class RaceQueryResponse {

    public List<LeaderBoardItem> leaderboard;
    public List<UserRankItem> userRankings;


    public class UserRankItem {
        public VehicleInfo vehicle;
        public MomentInfo.MomentBasicInfo moment;
        public int rank;

        @Override
        public String toString() {
            return ToStringUtils.getString(this);
        }
    }
}
