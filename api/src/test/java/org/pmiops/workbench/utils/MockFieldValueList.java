package org.pmiops.workbench.utils;

import com.google.cloud.bigquery.FieldValueList;
import java.util.ArrayList;
import java.util.List;

public class MockFieldValueList {
  public static final Iterable<FieldValueList> TEST_ITERABLE =
      () -> {
        List<FieldValueList> list = new ArrayList<>();
        list.add(null);
        return list.iterator();
      };
}
