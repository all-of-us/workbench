package org.pmiops.workbench.cohortreview;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.cdm.ParticipantConditionDbInfo;
import org.pmiops.workbench.cohortreview.util.ReviewTabQueries;
import org.pmiops.workbench.model.ParticipantConditionsColumns;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReviewTabQueryBuilder {

    public QueryJobConfiguration buildQuery(ReviewTabQueries queries, Long participantId, PageRequest pageRequest) {
        ParticipantConditionsColumns sortColumn = ParticipantConditionsColumns.fromValue(pageRequest.getSortColumn());
        String finalSql = String.format(queries.getQuery(),
                ParticipantConditionDbInfo.fromName(sortColumn).getDbName(),
                pageRequest.getSortOrder().toString(),
                pageRequest.getPageSize(),
                pageRequest.getPageNumber() * pageRequest.getPageSize());

        Map<String, QueryParameterValue> params = new HashMap<>();
        params.put(queries.NAMED_PARTICIPANTID_PARAM, QueryParameterValue.int64(participantId));
        return QueryJobConfiguration
                .newBuilder(finalSql)
                .setNamedParameters(params)
                .setUseLegacySql(false)
                .build();
    }

    public QueryJobConfiguration buildCountQuery(ReviewTabQueries queries, Long participantId) {
        Map<String, QueryParameterValue> params = new HashMap<>();
        params.put(queries.NAMED_PARTICIPANTID_PARAM, QueryParameterValue.int64(participantId));
        return QueryJobConfiguration
                .newBuilder(queries.getCountQuery())
                .setNamedParameters(params)
                .setUseLegacySql(false)
                .build();
    }
}
