package com.waylens.hachi.ui.entities;

/**
 * Created by Richard on 2/19/16.
 */
public class LocalMoment {

    public String title;

    public String[] tags;

    public String accessLevel;

    public int audioID;

    public SharableClip[] sharableClips;

    public LocalMoment(String title,
                       String[] tags,
                       String accessLevel,
                       int audioID,
                       SharableClip[] sharableClips) {
        this.title = title;
        this.tags = tags;
        this.accessLevel = accessLevel;
        this.audioID = audioID;
        this.sharableClips = sharableClips;
    }
}
