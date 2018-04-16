package org.pmiops.workbench.cohortreview.querybuilder;

import org.pmiops.workbench.api.CohortReviewController;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.model.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * ObservationQueryBuilder is an object that builds sql
 * for the Observation table in BigQuery.
 */
@Service
public class ObservationQueryBuilder implements ReviewQueryBuilder {

    public static final String OBSERVATIONS_SQL_TEMPLATE =
            "select ob.observation_datetime as item_date,\n" +
                    "       c1.vocabulary_id as standard_vocabulary,\n" +
                    "       c1.concept_name as standard_name,\n" +
                    "       ob.value_as_string as source_value,\n" +
                    "       c2.vocabulary_id as source_vocabulary,\n" +
                    "       c2.concept_name as source_name,\n" +
                    "       CAST(FLOOR(DATE_DIFF(observation_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age\n" +
                    "from `${projectId}.${dataSetId}.observation` ob\n" +
                    "left join `${projectId}.${dataSetId}.concept` c1 on ob.observation_concept_id = c1.concept_id\n" +
                    "left join `${projectId}.${dataSetId}.concept` c2 on ob.observation_source_concept_id = c2.concept_id\n" +
                    "join `${projectId}.${dataSetId}.person` p on ob.person_id = p.person_id\n" +
                    "where ob.person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n" +
                    "order by %s %s, observation_id\n" +
                    "limit %d offset %d\n";

    public static final String OBSERVATIONS_SQL_COUNT_TEMPLATE =
            "select count(*) as count\n" +
                    "from `${projectId}.${dataSetId}.observation`\n" +
                    "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n";

    @Override
    public String getQuery() {
        return this.OBSERVATIONS_SQL_TEMPLATE;
    }

    @Override
    public String getCountQuery() {
        return this.OBSERVATIONS_SQL_COUNT_TEMPLATE;
    }

    @Override
    public ParticipantData createParticipantData() {
        return new ParticipantObservation().dataType(DataType.PARTICIPANTOBSERVATION);
    }

    @Override
    public PageRequest createPageRequest(PageFilterRequest request) {
        String sortColumn =  Optional.ofNullable(((ParticipantObservations) request).getSortColumn())
                .orElse(ParticipantObservationsColumns.ITEMDATE).toString();
        int pageParam = Optional.ofNullable(request.getPage()).orElse(CohortReviewController.PAGE);
        int pageSizeParam = Optional.ofNullable(request.getPageSize()).orElse(CohortReviewController.PAGE_SIZE);
        SortOrder sortOrderParam = Optional.ofNullable(request.getSortOrder()).orElse(SortOrder.ASC);
        return new PageRequest(pageParam, pageSizeParam, sortOrderParam, sortColumn);
    }

    @Override
    public PageFilterType getPageFilterType() {
        return PageFilterType.PARTICIPANTOBSERVATIONS;
    }
}
