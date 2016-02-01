package com.waylens.hachi.ui.entities;

/**
 * Created by Richard on 2/1/16.
 */
public class MusicItem {

    public String title;

    public String url;

    public int length;

    public boolean isLocal;

    public MusicItem(String title, String url, int length) {
        this.title = title;
        this.url = url;
        this.length = length;
    }
}
