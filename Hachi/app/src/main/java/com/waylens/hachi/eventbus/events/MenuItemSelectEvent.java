package com.waylens.hachi.eventbus.events;

/**
 * Created by Xiaofei on 2016/4/6.
 */
public class MenuItemSelectEvent {
    private final int mMenuItemId;

    public MenuItemSelectEvent(int menuId) {
        this.mMenuItemId = menuId;
    }

    public int getMenuItemId() {
        return mMenuItemId;
    }
}
