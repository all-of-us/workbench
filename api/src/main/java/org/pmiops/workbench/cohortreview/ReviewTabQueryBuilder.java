package org.pmiops.workbench.cohortreview;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReviewTabQueryBuilder {

    private static final String NAMED_PARTICIPANTID_PARAM = "participantId";
    private static final String NAMED_DOMAIN_PARAM = "domain";
    private static final String NAMED_DATAID_PARAM = "dataId";

    private static final String SQL_TEMPLATE =
      "select data_id as dataId,\n" +
        "     domain as domain,\n" +
        "     item_date as itemDate,\n" +
        "     standard_vocabulary as standardVocabulary,\n" +
        "     standard_name as standardName,\n" +
        "     source_value as sourceValue,\n" +
        "     source_vocabulary as sourceVocabulary,\n" +
        "     source_name as sourceName,\n" +
        "     age_at_event as age,\n" +
        "     signature as signature,\n" +
        "     item_end_date as itemEndDate\n" +
        "from `${projectId}.${dataSetId}.participant_review`\n";

    private static final String COUNT_TEMPLATE =
      "select count(*) as count\n" +
        "from `${projectId}.${dataSetId}.participant_review`\n" +
        "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n" +
        "and domain = @" + NAMED_DOMAIN_PARAM + "\n";

    private static final String MASTER_COUNT_TEMPLATE =
      "select count(*) as count\n" +
        "from `${projectId}.${dataSetId}.participant_review`\n" +
        "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n";

    private static final String SQL_WHERE =
      "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n" +
        "and domain = @" + NAMED_DOMAIN_PARAM + "\n" +
        "order by %s %s, data_id\n" +
        "limit %d offset %d\n";

    private static final String MASTER_SQL_WHERE =
      "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n" +
        "order by %s %s, data_id\n" +
        "limit %d offset %d\n";

    private static final String DETAILS_WHERE =
      "where data_id = @" + NAMED_DATAID_PARAM + "\n";


    public QueryJobConfiguration buildQuery(Long participantId,
                                            String domain,
                                            PageRequest pageRequest) {
        String whereSql = DomainType.MASTER.toString().equals(domain) ? MASTER_SQL_WHERE : SQL_WHERE;
        String finalSql = SQL_TEMPLATE + whereSql;
        finalSql = String.format(finalSql,
          pageRequest.getSortColumn(),
          pageRequest.getSortOrder().toString(),
          pageRequest.getPageSize(),
          pageRequest.getPage() * pageRequest.getPageSize());

        Map<String, QueryParameterValue> params = new HashMap<>();
        params.put(NAMED_PARTICIPANTID_PARAM, QueryParameterValue.int64(participantId));
        params.put(NAMED_DOMAIN_PARAM, QueryParameterValue.string(domain));
        return QueryJobConfiguration
          .newBuilder(finalSql)
          .setNamedParameters(params)
          .setUseLegacySql(false)
          .build();
    }

    public QueryJobConfiguration buildCountQuery(Long participantId,
                                                 String domain) {
        String countSql = DomainType.MASTER.toString().equals(domain) ? MASTER_COUNT_TEMPLATE : COUNT_TEMPLATE;
        Map<String, QueryParameterValue> params = new HashMap<>();
        params.put(NAMED_PARTICIPANTID_PARAM, QueryParameterValue.int64(participantId));
        params.put(NAMED_DOMAIN_PARAM, QueryParameterValue.string(domain));
        return QueryJobConfiguration
          .newBuilder(countSql)
          .setNamedParameters(params)
          .setUseLegacySql(false)
          .build();
    }

    public QueryJobConfiguration buildDetailsQuery(Long dataId) {
        Map<String, QueryParameterValue> params = new HashMap<>();
        params.put(NAMED_DATAID_PARAM, QueryParameterValue.int64(dataId));
        return QueryJobConfiguration
          .newBuilder(SQL_TEMPLATE + DETAILS_WHERE)
          .setNamedParameters(params)
          .setUseLegacySql(false)
          .build();
    }
}
