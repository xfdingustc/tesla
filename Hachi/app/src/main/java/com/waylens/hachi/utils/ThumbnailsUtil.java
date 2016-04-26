package com.waylens.hachi.utils;

import android.annotation.SuppressLint;

import java.util.HashMap;

/**
 * Created by Xiaofei on 2015/6/29.
 */
public class ThumbnailsUtil {

  @SuppressLint("UseSparseArrays")
  private static HashMap<Integer,String> hash = new HashMap<>();


  public static String MapgetHashValue(int key,String defalt){
    if(hash==null||!hash.containsKey(key)){
      return defalt;
    }
    return hash.get(key);
  }

  public static void put(Integer key,String value){
    hash.put(key, value);
  }

  public static void clear(){
    hash.clear();
  }
}
