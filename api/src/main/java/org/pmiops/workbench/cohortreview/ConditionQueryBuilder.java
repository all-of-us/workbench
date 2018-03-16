package org.pmiops.workbench.cohortreview;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ConditionQueryBuilder {

    private static final String NAMED_PARTICIPANTID_PARAM = "@participantId";

    private static final String CONDITIONS_SQL_TEMPLATE =
            "select co.condition_start_datetime as item_date,\n" +
                    "       c1.vocabulary_id as standardVocabulary,\n" +
                    "       c1.concept_name as standardName,\n" +
                    "       co.condition_source_value as sourceValue,\n" +
                    "       c2.vocabulary_id as sourceVocabulary,\n" +
                    "       c2.concept_name as sourceName\n" +
                    "from `${projectId}.${dataSetId}.condition_occurrence` co\n" +
                    "left join `${projectId}.${dataSetId}.concept` c1 on co.condition_concept_id = c1.concept_id\n" +
                    "left join `${projectId}.${dataSetId}.concept` c2 on co.condition_source_concept_id = c2.concept_id\n" +
                    "where co.person_id = " + NAMED_PARTICIPANTID_PARAM + "\n" +
                    "order by itemDate;";

    public QueryJobConfiguration buildQuery(Long participantId) {
        Map<String, QueryParameterValue> params = new HashMap<>();
        params.put(NAMED_PARTICIPANTID_PARAM, QueryParameterValue.int64(participantId));
        return QueryJobConfiguration
                .newBuilder(CONDITIONS_SQL_TEMPLATE)
                .setNamedParameters(params)
                .setUseLegacySql(false)
                .build();
    }
}
