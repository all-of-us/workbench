package org.pmiops.workbench.reporting.insertion;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public interface DmlInsertJobBuilder<T> extends ColumnDrivenBuilder<T> {

  default String getColumnNameList() {
    return Arrays.stream(getQueryParameterColumns())
        .map(QueryParameterColumn::getParameterName)
        .collect(Collectors.joining(", "));
  }

  default QueryJobConfiguration build(
      String tableName, List<T> models, QueryParameterValue snapshotTimestamp) {
    final List<String> columnNames =
        Arrays.stream(getQueryParameterColumns())
            .map(QueryParameterColumn::getParameterName)
            .collect(ImmutableList.toImmutableList());
    final String query =
        "INSERT INTO "
            + tableName
            + " (snapshot_timestamp, "
            + getColumnNameList()
            + ")\n"
            + createNamedParameterValuesListKeys(models.size(), columnNames)
            + ";";
    final Map<String, QueryParameterValue> keyToParameterValue =
        getKeyToParameterValue(models, snapshotTimestamp);

    return QueryJobConfiguration.newBuilder(query).setNamedParameters(keyToParameterValue).build();
  }

  default String createNamedParameterValuesListKeys(int rowCount, List<String> columnNames) {
    return "VALUES\n"
        + IntStream.range(0, rowCount)
            .mapToObj(row -> writeNamedParameterValuesRow(row, columnNames))
            .collect(Collectors.joining(",\n"));
  }

  // column count is number of  fields in the enum (no including timestamp)
  default String writeNamedParameterValuesRow(int rowNum, List<String> columnNames) {
    final String parameterNameList =
        columnNames.stream()
            .map(c -> "@" + parameterName(rowNum, c))
            .collect(Collectors.joining(", "));
    return "(@snapshot_timestamp, " + parameterNameList + ")";
  }

  default Map<String, QueryParameterValue> getKeyToParameterValue(
      List<T> models, QueryParameterValue snapshotTimestamp) {
    final ImmutableMap.Builder<String, QueryParameterValue> builder = ImmutableMap.builder();
    builder.put("snapshot_timestamp", snapshotTimestamp); // same for all rows
    for (int row = 0; row < models.size(); ++row) {
      builder.putAll(toNamedParameterMap(models.get(row), row));
    }
    return builder.build();
  }

  default String parameterName(int rowNum, String columnName) {
    return String.format("%s__%d", columnName, rowNum);
  }

  default Map<String, QueryParameterValue> toNamedParameterMap(T target, int rowIndex) {
    return Arrays.stream(getQueryParameterColumns())
        .collect(
            ImmutableMap.toImmutableMap(
                e -> parameterName(rowIndex, e.getParameterName()),
                e -> e.toParameterValue(target)));
  }
}
