package org.pmiops.workbench.utils;

import com.google.api.gax.paging.Page;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;

public class BigQueryUtils {
  public static TableResult newTableResult(
      Schema schema, long totalRows, Page<FieldValueList> pageNoSchema) {
    return TableResult.newBuilder()
        .setSchema(schema)
        .setTotalRows(totalRows)
        .setPageNoSchema(pageNoSchema)
        .build();
  }
}
