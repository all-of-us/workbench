package org.pmiops.workbench.cohortreview;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.cdm.ParticipantConditionDbInfo;
import org.pmiops.workbench.model.ParticipantConditionsColumns;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ConditionQueryBuilder {

    private static final String NAMED_PARTICIPANTID_PARAM = "participantId";

    private static final String CONDITIONS_SQL_TEMPLATE =
            "select co.condition_start_datetime as item_date,\n" +
                    "       c1.vocabulary_id as standard_vocabulary,\n" +
                    "       c1.concept_name as standard_name,\n" +
                    "       co.condition_source_value as source_value,\n" +
                    "       c2.vocabulary_id as source_vocabulary,\n" +
                    "       c2.concept_name as source_name\n" +
                    "from `${projectId}.${dataSetId}.condition_occurrence` co\n" +
                    "left join `${projectId}.${dataSetId}.concept` c1 on co.condition_concept_id = c1.concept_id\n" +
                    "left join `${projectId}.${dataSetId}.concept` c2 on co.condition_source_concept_id = c2.concept_id\n" +
                    "where co.person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n" +
                    "order by %s %s, condition_occurrence_id\n" +
                    "limit %d offset %d\n";

    private static final String CONDITIONS_SQL_COUNT_TEMPLATE =
            "select count(*) as count\n" +
                    "from `${projectId}.${dataSetId}.condition_occurrence`\n" +
                    "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n";

    public QueryJobConfiguration buildQuery(Long participantId, PageRequest pageRequest) {
        ParticipantConditionsColumns sortColumn = ParticipantConditionsColumns.fromValue(pageRequest.getSortColumn());
        String finalSql = String.format(CONDITIONS_SQL_TEMPLATE,
                ParticipantConditionDbInfo.fromName(sortColumn).getDbName(),
                pageRequest.getSortOrder().toString(),
                pageRequest.getPageSize(),
                pageRequest.getPageNumber() * pageRequest.getPageSize());

        Map<String, QueryParameterValue> params = new HashMap<>();
        params.put(NAMED_PARTICIPANTID_PARAM, QueryParameterValue.int64(participantId));
        return QueryJobConfiguration
                .newBuilder(finalSql)
                .setNamedParameters(params)
                .setUseLegacySql(false)
                .build();
    }

    public QueryJobConfiguration buildCountQuery(Long participantId) {
        Map<String, QueryParameterValue> params = new HashMap<>();
        params.put(NAMED_PARTICIPANTID_PARAM, QueryParameterValue.int64(participantId));
        return QueryJobConfiguration
                .newBuilder(CONDITIONS_SQL_COUNT_TEMPLATE)
                .setNamedParameters(params)
                .setUseLegacySql(false)
                .build();
    }
}
