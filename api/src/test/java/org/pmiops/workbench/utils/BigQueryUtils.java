package org.pmiops.workbench.utils;

import com.google.api.gax.paging.Page;
import com.google.cloud.PageImpl;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import java.util.List;

public class BigQueryUtils {
  public static TableResult newTableResult(
      Schema schema, long totalRows, Page<FieldValueList> pageNoSchema) {
    return TableResult.newBuilder()
        .setSchema(schema)
        .setTotalRows(totalRows)
        .setPageNoSchema(pageNoSchema)
        .build();
  }

  public static TableResult newTableResult(Schema schema, List<FieldValueList> tableRows) {
    return newTableResult(schema, tableRows.size(), new PageImpl<>(() -> null, null, tableRows));
  }

  public static TableResult emptyTableResult() {
    return newTableResult(null, 0L, new PageImpl<>(null, "", null));
  }
}
