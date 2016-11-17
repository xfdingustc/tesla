package com.waylens.hachi.ui.leaderboard;

import com.waylens.hachi.ui.entities.moment.MomentAbstract;

/**
 * Created by Xiaofei on 2016/11/17.
 */

public class MomentRaceTimeHelper {
    public static  double getRaceTime(MomentAbstract moment, int raceType, int testMode) {
        double raceTime = 0.0;
        switch (raceType) {
            case LeaderboardFragment.RACE_TYPE_30MPH:
                if (testMode == LeaderboardFragment.TEST_MODE_AUTO) {
                    raceTime = (double) (moment.momentTimingInfo.t3_2) / 1000;
                } else if (testMode == LeaderboardFragment.TEST_MODE_COUNTDOWN) {
                    raceTime = (double) (moment.momentTimingInfo.t3_1) / 1000;
                }
                break;
            case LeaderboardFragment.RACE_TYPE_50KMH:
                if (testMode == LeaderboardFragment.TEST_MODE_AUTO) {
                    raceTime = (double) (moment.momentTimingInfo.t4_2) / 1000;
                } else if (testMode == LeaderboardFragment.TEST_MODE_COUNTDOWN) {
                    raceTime = (double) (moment.momentTimingInfo.t4_1) / 1000;
                }
                break;
            case LeaderboardFragment.RACE_TYPE_60MPH:
                if (testMode == LeaderboardFragment.TEST_MODE_AUTO) {
                    raceTime = (double) (moment.momentTimingInfo.t5_2) / 1000;
                } else if (testMode == LeaderboardFragment.TEST_MODE_COUNTDOWN) {
                    raceTime = (double) (moment.momentTimingInfo.t5_1) / 1000;
                }
                break;
            case LeaderboardFragment.RACE_TYPE_100KMH:
                if (testMode == LeaderboardFragment.TEST_MODE_AUTO) {
                    raceTime = (double) (moment.momentTimingInfo.t6_2) / 1000;
                } else if (testMode == LeaderboardFragment.TEST_MODE_COUNTDOWN) {
                    raceTime = (double) (moment.momentTimingInfo.t6_1) / 1000;
                }
                break;
            default:
                break;
        }
        return raceTime;
    }

}
