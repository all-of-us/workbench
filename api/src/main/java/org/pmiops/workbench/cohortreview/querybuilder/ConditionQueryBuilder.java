package org.pmiops.workbench.cohortreview.querybuilder;

import org.pmiops.workbench.api.CohortReviewController;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.model.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * ConditionQueryBuilder is an object that builds sql
 * for the Condition Occurrences table in BigQuery.
 */
@Service
public class ConditionQueryBuilder implements ReviewQueryBuilder {

    private static final String CONDITIONS_SQL_TEMPLATE =
            "select co.condition_start_datetime as item_date,\n" +
                    "       c1.vocabulary_id as standard_vocabulary,\n" +
                    "       c1.concept_name as standard_name,\n" +
                    "       co.condition_source_value as source_value,\n" +
                    "       c2.vocabulary_id as source_vocabulary,\n" +
                    "       c2.concept_name as source_name,\n" +
                    "       CAST(FLOOR(DATE_DIFF(condition_start_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age\n" +
                    "from `${projectId}.${dataSetId}.condition_occurrence` co\n" +
                    "left join `${projectId}.${dataSetId}.concept` c1 on co.condition_concept_id = c1.concept_id\n" +
                    "left join `${projectId}.${dataSetId}.concept` c2 on co.condition_source_concept_id = c2.concept_id\n" +
                    "join `${projectId}.${dataSetId}.person` p on co.person_id = p.person_id\n" +
                    "where co.person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n" +
                    "order by %s %s, condition_occurrence_id\n" +
                    "limit %d offset %d\n";
    private static final String CONDITIONS_SQL_COUNT_TEMPLATE =
            "select count(*) as count\n" +
                    "from `${projectId}.${dataSetId}.condition_occurrence`\n" +
                    "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n";

    @Override
    public String getQuery() {
        return this.CONDITIONS_SQL_TEMPLATE;
    }

    @Override
    public String getCountQuery() {
        return this.CONDITIONS_SQL_COUNT_TEMPLATE;
    }

    @Override
    public ParticipantData createParticipantData() {
        return new ParticipantCondition().dataType(DataType.PARTICIPANTCONDITION);
    }

    @Override
    public PageRequest createPageRequest(PageFilterRequest request) {
        String sortColumn = Optional.ofNullable(((ParticipantConditions) request).getSortColumn())
                .orElse(ParticipantConditionsColumns.ITEMDATE).toString();
        int pageParam = Optional.ofNullable(request.getPage()).orElse(CohortReviewController.PAGE);
        int pageSizeParam = Optional.ofNullable(request.getPageSize()).orElse(CohortReviewController.PAGE_SIZE);
        SortOrder sortOrderParam = Optional.ofNullable(request.getSortOrder()).orElse(SortOrder.ASC);
        return new PageRequest(pageParam, pageSizeParam, sortOrderParam, sortColumn);
    }

    @Override
    public PageFilterType getPageFilterType() {
        return PageFilterType.PARTICIPANTCONDITIONS;
    }
}
