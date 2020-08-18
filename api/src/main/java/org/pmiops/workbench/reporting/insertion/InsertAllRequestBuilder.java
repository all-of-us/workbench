package org.pmiops.workbench.reporting.insertion;

import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllRequest.RowToInsert;
import com.google.cloud.bigquery.TableId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.pmiops.workbench.utils.RandomUtils;

public interface InsertAllRequestBuilder<T> extends ColumnDrivenBuilder<T> {
  String INSERT_ID_CHARS = "abcdefghijklmnopqrstuvwxyz";

  default InsertAllRequest build(TableId tableId, List<T> models, Map<String, Object> fixedValues) {
    return InsertAllRequest.newBuilder(tableId)
        .setIgnoreUnknownValues(true)
        .setRows(modelsToRows(models, fixedValues))
        .build();
  }

  default List<RowToInsert> modelsToRows(Collection<T> models, Map<String, Object> fixedValues) {
    return models.stream()
        .map(m -> modelToRow(m, fixedValues))
        .collect(ImmutableList.toImmutableList());
  }

  default RowToInsert modelToRow(T model, Map<String, Object> fixedValues) {
    final ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<>();
    // First N columns are same for all rows (e.g. a partition key column)
    builder.putAll(fixedValues);
    builder.putAll(
        Arrays.stream(getQueryParameterColumns())
            .collect(
                ImmutableMap.toImmutableMap(
                    QueryParameterColumn::getParameterName, c -> c.getRowToInsertValue(model))));
    return RowToInsert.of(generateInsertId(), builder.build());
  }

  default String generateInsertId() {
    return RandomUtils.generateRandomChars(INSERT_ID_CHARS, 16);
  }
}
