package org.pmiops.workbench.cohortreview.querybuilder;

import org.pmiops.workbench.api.CohortReviewController;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.model.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VisitQueryBuilder implements ReviewQueryBuilder {

  private static final String VISITS_SQL_TEMPLATE =
    "select item_date as itemDate,\n" +
      "       item_date as endDate,\n" +
      "       standard_vocabulary as standardVocabulary,\n" +
      "       standard_name as standardName,\n" +
      "       source_value as sourceValue,\n" +
      "       source_vocabulary as sourceVocabulary,\n" +
      "       source_name as sourceName,\n" +
      "       age_at_event as age\n" +
      "from `${projectId}.${dataSetId}.participant_review`\n" +
      "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n" +
      "and domain = 'Visit'\n" +
      "order by %s %s, data_id\n";

  private static final String VISITS_DETAIL_SQL_TEMPLATE =
    "select vo.visit_start_datetime as itemDate,\n" +
      "       vo.visit_end_datetime as endDate,\n" +
      "       c1.vocabulary_id as standardVocabulary,\n" +
      "       c1.concept_name as standardName,\n" +
      "       vo.visit_source_value as sourceValue,\n" +
      "       c2.vocabulary_id as sourceVocabulary,\n" +
      "       c2.concept_name as sourceName,\n" +
      "       CAST(FLOOR(DATE_DIFF(visit_start_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age\n" +
      "from `${projectId}.${dataSetId}.visit_occurrence` vo\n" +
      "left join `${projectId}.${dataSetId}.concept` c1 on vo.visit_concept_id = c1.concept_id\n" +
      "left join `${projectId}.${dataSetId}.concept` c2 on vo.visit_source_concept_id = c2.concept_id\n" +
      "join `${projectId}.${dataSetId}.person` p on vo.person_id = p.person_id\n" +
      "where vo.visit_occurrence_id = @" + NAMED_DATAID_PARAM + "\n";

  @Override
  public String getQuery() {
    return this.VISITS_SQL_TEMPLATE;
  }

  @Override
  public String getDetailsQuery() {
    return this.VISITS_DETAIL_SQL_TEMPLATE;
  }

  @Override
  public ParticipantData createParticipantData() {
    return new Visit().domainType(DomainType.VISIT);
  }

  @Override
  public PageRequest createPageRequest(PageFilterRequest request) {
    String sortColumn = Optional.ofNullable(((ParticipantVisits) request).getSortColumn())
      .orElse(ParticipantVisitsColumns.ITEMDATE).toString();
    int pageParam = Optional.ofNullable(request.getPage()).orElse(CohortReviewController.PAGE);
    int pageSizeParam = Optional.ofNullable(request.getPageSize()).orElse(CohortReviewController.PAGE_SIZE);
    SortOrder sortOrderParam = Optional.ofNullable(request.getSortOrder()).orElse(SortOrder.ASC);
    return new PageRequest(pageParam, pageSizeParam, sortOrderParam, sortColumn);
  }

  @Override
  public PageFilterType getPageFilterType() {
    return PageFilterType.PARTICIPANTVISITS;
  }
}
