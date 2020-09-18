package org.pmiops.workbench.reporting.insertion;

import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllRequest.RowToInsert;
import com.google.cloud.bigquery.TableId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.pmiops.workbench.utils.RandomUtils;

public interface InsertAllRequestBuilder<T> extends ColumnDrivenBuilder<T> {
  String INSERT_ID_CHARS = "abcdefghijklmnopqrstuvwxyz";
  int INSERT_ID_LENGTH = 16;

  default InsertAllRequest build(TableId tableId, List<T> models, Map<String, Object> fixedValues) {
    return InsertAllRequest.newBuilder(tableId)
        .setIgnoreUnknownValues(true)
        .setRows(dtosToRowsToInsert(models, fixedValues))
        .build();
  }

  default List<RowToInsert> dtosToRowsToInsert(
      Collection<T> models, Map<String, Object> fixedValues) {
    return models.stream()
        .map(m -> dtoToRowToInsert(m, fixedValues))
        .collect(ImmutableList.toImmutableList());
  }

  // Null values are supposed to be omitted from the map (or have @Value or @NullValue annotations).
  default RowToInsert dtoToRowToInsert(T model, Map<String, Object> fixedValues) {
    final ImmutableMap.Builder<String, Object> columnToValueBuilder = ImmutableMap.builder();
    columnToValueBuilder.putAll(fixedValues);
    Arrays.stream(getQueryParameterColumns())
        .map(
            col ->
                new AbstractMap.SimpleImmutableEntry<>(
                    col.getParameterName(), col.getRowToInsertValue(model)))
        .filter(e -> Objects.nonNull(e.getValue()))
        .forEach(columnToValueBuilder::put);

    return RowToInsert.of(generateInsertId(), columnToValueBuilder.build());
  }

  default String generateInsertId() {
    return RandomUtils.generateRandomChars(INSERT_ID_CHARS, INSERT_ID_LENGTH);
  }
}
