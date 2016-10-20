package com.waylens.hachi.ui.liveview;

import android.support.annotation.DrawableRes;

import com.waylens.hachi.R;
import com.waylens.hachi.camera.VdtCamera;

/**
 * Created by Xiaofei on 2016/10/20.
 */

public class BatteryImageViewResHelper {
    private BatteryImageViewResHelper() {

    }


    public static
    @DrawableRes
    int getBatteryViewRes(int volume, int state) {
        if (state == VdtCamera.STATE_BATTERY_CHARGING) {
            if (volume <= 29) {
                return R.drawable.ic_battery_charging_20;
            } else if (volume <= 39) {
                return R.drawable.ic_battery_charging_30;
            } else if (volume <= 59) {
                return R.drawable.ic_battery_charging_50;
            } else if (volume <= 69) {
                return R.drawable.ic_battery_charging_60;
            } else if (volume <= 89) {
                return R.drawable.ic_battery_charging_80;
            } else if (volume <= 99) {
                return R.drawable.ic_battery_charging_90;
            } else {
                return R.drawable.ic_battery_charging_full;
            }
        } else {
            if (volume <= 29) {
                return R.drawable.ic_battery_20;
            } else if (volume <= 39) {
                return R.drawable.ic_battery_30;
            } else if (volume <= 59) {
                return R.drawable.ic_battery_50;
            } else if (volume <= 69) {
                return R.drawable.ic_battery_60;
            } else if (volume <= 89) {
                return R.drawable.ic_battery_80;
            } else if (volume <= 99) {
                return R.drawable.ic_battery_90;
            } else {
                return R.drawable.ic_battery_full;
            }
        }
    }
}
