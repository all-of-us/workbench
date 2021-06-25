package org.pmiops.workbench.cloudtasks;

import java.util.ArrayList;
import java.util.List;

public class CloudTasksUtils {

  private CloudTasksUtils() {}

  /** Partition the given list into sub-lists of batchSize or smaller. */
  public static <T> List<List<T>> partitionList(List<T> list, int batchSize) {
    final List<List<T>> subLists = new ArrayList<>();
    for (int i = 0; i < list.size(); i += batchSize) {
      subLists.add(list.subList(i, Math.min(list.size(), i + batchSize)));
    }
    return subLists;
  }
}
