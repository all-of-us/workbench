package org.pmiops.workbench.reporting.insertion;

import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllRequest.RowToInsert;
import com.google.cloud.bigquery.TableId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.pmiops.workbench.utils.RandomUtils;

/**
 * Class with InsertAllRequest payload-specific implementation to change model objects (e.g.
 * ReportingUser) into RowToInsert instances. A different template specialization of this interface
 * with corresponding object (possibly of an anonymous class) should be used for each destination
 * table in the upload. In order to construct such an instance, it's only necessary to override the
 * method getQueryParameterColumns() with a method returning an array of instances. Enum#values() is
 * very useful here.
 *
 * <p>In order to instantiate a table-specific object implementing this instance, just do something
 * like
 *
 * @code{ InsertAllRequestPayloadTransformer<ReportingUser> userTransformer =
 *     UserColumnValueExtractor::values; }
 */
@FunctionalInterface
public interface InsertAllRequestPayloadTransformer<MODEL_T>
    extends BigQueryInsertionPayloadTransformer<MODEL_T> {
  String INSERT_ID_CHARS = "abcdefghijklmnopqrstuvwxyz";
  int INSERT_ID_LENGTH = 16;
  int MAX_ROWS_PER_INSERT_ALL_REQUEST = 1000;

  default List<InsertAllRequest> buildBatchedRequests(
      TableId tableId, List<MODEL_T> models, Map<String, Object> fixedValues, int batchSize) {
    return Lists.partition(models, Math.min(batchSize, MAX_ROWS_PER_INSERT_ALL_REQUEST)).stream()
        .map(batch -> build(tableId, batch, fixedValues))
        .collect(ImmutableList.toImmutableList());
  }
  /**
   * Construct an InsertAllRequest from all of the provided models, one row per model. The
   * fixedValues argument is to allow a value (like snapshot_timestamp) to span all rows in its
   * column.
   */
  default InsertAllRequest build(
      TableId tableId, List<MODEL_T> models, Map<String, Object> fixedValues) {
    return InsertAllRequest.newBuilder(tableId)
        .setIgnoreUnknownValues(false) // consider non-schema-conforming values bad rows.
        .setRows(modelsToRowsToInsert(models, fixedValues))
        .build();
  }

  // Wrap modelToRowToInsert() and apply to the whole input list of models.
  default List<RowToInsert> modelsToRowsToInsert(
      Collection<MODEL_T> models, Map<String, Object> fixedValues) {
    return models.stream()
        .map(m -> modelToRowToInsert(m, fixedValues))
        .collect(ImmutableList.toImmutableList());
  }

  /*
   * Build a RowToInsert object for each model instance, which is basically a poorly typed Map.
   * Null values are supposed to be omitted from the map (or have @Value or @NullValue annotations).
   */
  default RowToInsert modelToRowToInsert(MODEL_T model, Map<String, Object> fixedValues) {
    final ImmutableMap.Builder<String, Object> columnToValueBuilder = ImmutableMap.builder();
    columnToValueBuilder.putAll(fixedValues); // assumed to have non-null values
    Arrays.stream(getQueryParameterColumns())
        .map(
            col ->
                new AbstractMap.SimpleImmutableEntry<>(
                    col.getParameterName(), col.getRowToInsertValue(model)))
        .filter(e -> Objects.nonNull(e.getValue()))
        .forEach(columnToValueBuilder::put);

    return RowToInsert.of(generateInsertId(), columnToValueBuilder.build());
  }

  /*
   * As an aid for best-effort deduplication, we can specify a unique 16-character key for each row.
   */
  static String generateInsertId() {
    return RandomUtils.generateRandomChars(INSERT_ID_CHARS, INSERT_ID_LENGTH);
  }
}
