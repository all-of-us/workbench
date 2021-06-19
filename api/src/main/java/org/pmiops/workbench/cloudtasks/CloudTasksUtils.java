package org.pmiops.workbench.cloudtasks;

import java.util.ArrayList;
import java.util.List;

public class CloudTasksUtils {

  private CloudTasksUtils() {}

  public static <T> List<List<T>> partitionList(List<T> list, int batchSize) {
    final List<List<T>> out = new ArrayList<>();
    for (int i = 0; i < list.size(); i += batchSize) {
      out.add(list.subList(i, Math.min(list.size(), i + batchSize)));
    }
    return out;
  }
}
