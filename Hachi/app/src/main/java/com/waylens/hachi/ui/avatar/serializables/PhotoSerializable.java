package com.waylens.hachi.ui.avatar.serializables;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Xiaofei on 2015/6/29.
 */
public class PhotoSerializable implements Serializable {

  private List<Photo> list;


  public PhotoSerializable() {
  }

  public PhotoSerializable(List<Photo> list) {
    this.list = list;
  }

  public List<Photo> getList() {
    return list;
  }

  public void setList(List<Photo> list) {
    this.list = list;
  }

}

