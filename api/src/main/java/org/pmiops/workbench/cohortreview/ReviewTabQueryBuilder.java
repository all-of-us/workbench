package org.pmiops.workbench.cohortreview;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ImmutableMap;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReviewTabQueryBuilder {

    private static final ImmutableMap<String, String> EXTRA_COLUMNS =
      ImmutableMap.of("Master", ", domain, end_datetime as endDate, signature",
        "Drug", ", signature", "Visit", ", end_datetime as endDate");

    private static final String NAMED_PARTICIPANTID_PARAM = "participantId";
    private static final String NAMED_DATAID_PARAM = "dataId";
    private static final String TABLE_PREFIX = "person_";
    private static final String MASTER_TABLE = "person_all_events";

    private static final String SQL_TEMPLATE =
      "select data_id as dataId,\n" +
        "     start_datetime as startDate,\n" +
        "     standard_vocabulary as standardVocabulary,\n" +
        "     standard_name as standardName,\n" +
        "     source_value as sourceValue,\n" +
        "     source_vocabulary as sourceVocabulary,\n" +
        "     source_name as sourceName,\n" +
        "     age_at_event as ageAtEvent\n" +
        "     %s\n" +
        "from `${projectId}.${dataSetId}.%s`\n";

    private static final String WHERE_TEMPLATE =
        "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n" +
        "order by %s %s, data_id\n" +
        "limit %d offset %d\n";;

    private static final String COUNT_TEMPLATE =
      "select count(*) as count\n" +
        "from `${projectId}.${dataSetId}.%s`\n" +
        "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n";

    private static final String DETAILS_WHERE =
      "where data_id = @" + NAMED_DATAID_PARAM + "\n";


    public QueryJobConfiguration buildQuery(Long participantId,
                                            String domain,
                                            PageRequest pageRequest) {
        String tableName = DomainType.MASTER.toString().equals(domain)
          ? MASTER_TABLE : TABLE_PREFIX + domain.toLowerCase();
        String extraColumns = EXTRA_COLUMNS.get(domain) == null
          ? "" : EXTRA_COLUMNS.get(domain);
        String finalSql = String.format(SQL_TEMPLATE + WHERE_TEMPLATE,
          extraColumns,
          tableName,
          pageRequest.getSortColumn(),
          pageRequest.getSortOrder().toString(),
          pageRequest.getPageSize(),
          pageRequest.getPage() * pageRequest.getPageSize());

        Map<String, QueryParameterValue> params = new HashMap<>();
        params.put(NAMED_PARTICIPANTID_PARAM, QueryParameterValue.int64(participantId));
        return QueryJobConfiguration
          .newBuilder(finalSql)
          .setNamedParameters(params)
          .setUseLegacySql(false)
          .build();
    }

    public QueryJobConfiguration buildCountQuery(Long participantId,
                                                 String domain) {
        String tableName = DomainType.MASTER.toString().equals(domain)
          ? MASTER_TABLE : TABLE_PREFIX + domain.toLowerCase();
        String finalSql = String.format(COUNT_TEMPLATE, tableName);
        Map<String, QueryParameterValue> params = new HashMap<>();
        params.put(NAMED_PARTICIPANTID_PARAM, QueryParameterValue.int64(participantId));
        return QueryJobConfiguration
          .newBuilder(finalSql)
          .setNamedParameters(params)
          .setUseLegacySql(false)
          .build();
    }

    public QueryJobConfiguration buildDetailsQuery(Long dataId, String domain) {
        String tableName = DomainType.MASTER.toString().equals(domain)
          ? MASTER_TABLE : TABLE_PREFIX + domain.toLowerCase();
        String extraColumns = EXTRA_COLUMNS.get(domain) == null
          ? "" : EXTRA_COLUMNS.get(domain);
        String finalSql = String.format(SQL_TEMPLATE + DETAILS_WHERE, extraColumns, tableName);
        Map<String, QueryParameterValue> params = new HashMap<>();
        params.put(NAMED_DATAID_PARAM, QueryParameterValue.int64(dataId));
        return QueryJobConfiguration
          .newBuilder(finalSql)
          .setNamedParameters(params)
          .setUseLegacySql(false)
          .build();
    }
}
