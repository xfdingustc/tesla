package com.waylens.hachi.ui.avatar.serializables;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Xiaofei on 2015/6/29.
 */
public class AlbumInfo implements Serializable {

  private int image_id;
  private String path_absolute;
  private String path_file;
  private String name_album;
  private List<Photo> list;
  public int getImage_id() {
    return image_id;
  }
  public void setImage_id(int image_id) {
    this.image_id = image_id;
  }
  public String getPath_absolute() {
    return path_absolute;
  }
  public void setPath_absolute(String path_absolute) {
    this.path_absolute = path_absolute;
  }
  public String getPath_file() {
    return path_file;
  }
  public void setPath_file(String path_file) {
    this.path_file = path_file;
  }
  public String getName_album() {
    return name_album;
  }
  public void setName_album(String name_album) {
    this.name_album = name_album;
  }
  public List<Photo> getList() {
    return list;
  }
  public void setList(List<Photo> list) {
    this.list = list;
  }
}
