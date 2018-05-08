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
      "select item_date as itemDate,\n" +
        "       standard_vocabulary as standardVocabulary,\n" +
        "       standard_name as standardName,\n" +
        "       source_value as sourceValue,\n" +
        "       source_vocabulary as sourceVocabulary,\n" +
        "       source_name as sourceName,\n" +
        "       age_at_event as age\n" +
        "from `${projectId}.${dataSetId}.participant_review`\n" +
        "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n" +
        "and domain = 'Observation'\n" +
        "order by %s %s, data_id\n";

    private static final String OBSERVATIONS_DETAIL_SQL_TEMPLATE =
      "select ob.observation_datetime as itemDate,\n" +
        "       c1.vocabulary_id as standardVocabulary,\n" +
        "       c1.concept_name as standardName,\n" +
        "       ob.value_as_string as sourceValue,\n" +
        "       c2.vocabulary_id as sourceVocabulary,\n" +
        "       c2.concept_name as sourceName,\n" +
        "       CAST(FLOOR(DATE_DIFF(observation_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age\n" +
        "from `${projectId}.${dataSetId}.observation` ob\n" +
        "left join `${projectId}.${dataSetId}.concept` c1 on ob.observation_concept_id = c1.concept_id\n" +
        "left join `${projectId}.${dataSetId}.concept` c2 on ob.observation_source_concept_id = c2.concept_id\n" +
        "join `${projectId}.${dataSetId}.person` p on ob.person_id = p.person_id\n" +
        "where ob.observation_id = @" + NAMED_DATAID_PARAM + "\n";

    @Override
    public String getQuery() {
        return this.OBSERVATIONS_SQL_TEMPLATE;
    }

    @Override
    public String getDetailsQuery() {
        return this.OBSERVATIONS_DETAIL_SQL_TEMPLATE;
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
