package com.waylens.hachi.ui.avatar.serializables;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Xiaofei on 2015/6/29.
 */
public class AlbumSerializable implements Serializable {

  /**
   * @fields serialVersionUID
   */

  private static final long serialVersionUID = 1L;

  private List<AlbumInfo> list;

  public List<AlbumInfo> getList() {
    return list;
  }

  public void setList(List<AlbumInfo> list) {
    this.list = list;
  }

}
