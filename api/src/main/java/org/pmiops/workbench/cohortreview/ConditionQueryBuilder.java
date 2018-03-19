package org.pmiops.workbench.cohortreview;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ConditionQueryBuilder {

    private static final String NAMED_PARTICIPANTID_PARAM = "participantId";
    private static final String NAMED_LIMIT_PARAM = "limit";
    private static final String NAMED_OFFSET_PARAM = "offset";

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
                    "order by item_date, condition_occurrence_id\n";

    public QueryJobConfiguration buildQuery(Long participantId, Integer page, Integer pageSize) {
        Map<String, QueryParameterValue> params = new HashMap<>();
        params.put(NAMED_PARTICIPANTID_PARAM, QueryParameterValue.int64(participantId));
        return QueryJobConfiguration
                .newBuilder(CONDITIONS_SQL_TEMPLATE + "limit " + pageSize + " offset " + (page * pageSize))
                .setNamedParameters(params)
                .setUseLegacySql(false)
                .build();
    }
}
