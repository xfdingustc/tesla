package com.waylens.hachi.rest.response;

import com.waylens.hachi.ui.entities.Moment;

import java.util.List;

/**
 * Created by lshw on 16/9/5.
 */
public class RaceQueryResponse {

    public List<LeaderBoardItem> leaderboard;
    public List<UserRankItem> userRankings;

    public class LeaderBoardItem {
        public int cursor;
        public Moment moment;
        public MomentInfo.Owner owner;
    }

    public class UserRankItem {
        public MomentInfo.VehicleInfo vehicle;
        public MomentInfo.MomentBasicInfo moment;
        public int rank;
    }
}