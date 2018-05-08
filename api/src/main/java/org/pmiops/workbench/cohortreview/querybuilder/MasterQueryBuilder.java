package org.pmiops.workbench.cohortreview.querybuilder;

import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.api.CohortReviewController;
import org.pmiops.workbench.cdm.DomainTableEnum;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MasterQueryBuilder is an object that builds sql
 * for all the domain tables in BigQuery.
 */
@Service
public class MasterQueryBuilder implements ReviewQueryBuilder {

  private static final String MASTER1_SQL_TEMPLATE =
    "select data_id as dataId, " +
      "     item_date as itemDate,\n" +
      "     domain,\n" +
      "     standard_vocabulary as standardVocabulary,\n" +
      "     standard_name as standardName,\n" +
      "     source_value as sourceValue,\n" +
      "     source_vocabulary as sourceVocabulary,\n" +
      "     source_name as sourceName\n" +
      "from `${projectId}.${dataSetId}.participant_review`\n" +
      "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n";

  @Override
  public String getQuery() {
    return MASTER1_SQL_TEMPLATE;
  }

  @Override
  public String getDetailsQuery() {
    return "";
  }

  @Override
  public ParticipantData createParticipantData() {
    return new ParticipantMaster().dataType(DataType.PARTICIPANTMASTER);
  }

  @Override
  public PageRequest createPageRequest(PageFilterRequest request) {
    String sortColumn = Optional.ofNullable(((ParticipantMasters) request).getSortColumn())
      .orElse(ParticipantMasterColumns.ITEMDATE).toString();
    int pageParam = Optional.ofNullable(request.getPage()).orElse(CohortReviewController.PAGE);
    int pageSizeParam = Optional.ofNullable(request.getPageSize()).orElse(CohortReviewController.PAGE_SIZE);
    SortOrder sortOrderParam = Optional.ofNullable(request.getSortOrder()).orElse(SortOrder.ASC);
    return new PageRequest(pageParam, pageSizeParam, sortOrderParam, sortColumn);
  }

  @Override
  public PageFilterType getPageFilterType() {
    return PageFilterType.PARTICIPANTMASTERS;
  }
}
