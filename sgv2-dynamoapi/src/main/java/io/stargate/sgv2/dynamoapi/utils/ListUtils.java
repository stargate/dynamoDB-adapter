package io.stargate.sgv2.dynamoapi.utils;

import java.util.ArrayList;
import java.util.List;

public class ListUtils {

  public static <E> List<E> createSublist(List<E> oldList, int[] indices) {
    if (indices == null || indices.length == 0) {
      return oldList;
    }
    List<E> newList = new ArrayList<>(indices.length);
    for (int index : indices) {
      newList.add(oldList.get(index));
    }
    return newList;
  }
}
