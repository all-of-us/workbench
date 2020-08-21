package org.pmiops.workbench.reporting.insertion;

import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllRequest.RowToInsert;
import com.google.cloud.bigquery.TableId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
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
    // First N columns are same for all rows (e.g. a partition key column)
    final Map<String, Object> map = Maps.newHashMap(fixedValues);

    // can't stream/collect here because that uses HashMap.merge() which surprisingly does not
    // allow null values although they are valid for HashMap.  We do use null values.
    for (QueryParameterColumn<T> qpc : getQueryParameterColumns()) {
      map.put(qpc.getParameterName(), qpc.getRowToInsertValue(model));
    }

    return RowToInsert.of(generateInsertId(), map);
  }

  default String generateInsertId() {
    return RandomUtils.generateRandomChars(INSERT_ID_CHARS, 16);
  }
}
